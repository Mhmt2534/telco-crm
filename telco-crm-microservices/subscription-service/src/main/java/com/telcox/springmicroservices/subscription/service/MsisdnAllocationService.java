package com.telcox.springmicroservices.subscription.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.telcox.springmicroservices.subscription.entity.MsisdnPool;
import com.telcox.springmicroservices.subscription.entity.MsisdnStatus;
import com.telcox.springmicroservices.subscription.exception.MsisdnAllocationException;
import com.telcox.springmicroservices.subscription.repository.MsisdnPoolRepository;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Havuzdan boş bir MSISDN numarası tahsis eder.
 *
 * <p>Dağıtık ortamda (birden fazla pod) aynı numaranın aynı anda iki farklı
 * müşteriye satılmasını önlemek için Redisson RLock kullanılır.
 *
 * <p>Kilit anahtarı: {@code lock:msisdn:allocation}
 * Kilit bekleme süresi: 5 saniye | Kilit yaşam süresi: 10 saniye
 */
@Service
public class MsisdnAllocationService {

    private static final Logger log = Logger.getLogger(MsisdnAllocationService.class.getName());

    private static final long LOCK_WAIT_SECONDS = 5L;
    private static final long LOCK_LEASE_SECONDS = 10L;

    private final RedissonClient redissonClient;
    private final MsisdnPoolRepository msisdnPoolRepository;

    public MsisdnAllocationService(RedissonClient redissonClient,
                                   MsisdnPoolRepository msisdnPoolRepository) {
        this.redissonClient = redissonClient;
        this.msisdnPoolRepository = msisdnPoolRepository;
    }

    /**
     * Havuzdan FREE statüsünde bir MSISDN seçer ve RESERVED olarak işaretler.
     * Tüm işlem Redisson distributed lock altında gerçekleşir.
     *
     * @return Tahsis edilen MSISDN numarası
     * @throws MsisdnAllocationException Havuzda boş numara yoksa veya kilit alınamazsa
     */
    @Transactional
    public String allocateFreeMsisdn() {
        int maxRetries = 5;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            List<MsisdnPool> freeNumbers = msisdnPoolRepository
                    .findFirstByStatus(MsisdnStatus.FREE, PageRequest.of(0, 1));

            if (freeNumbers.isEmpty()) {
                throw new MsisdnAllocationException("Havuzda tahsis edilebilecek boş numara kalmadı.");
            }

            String msisdn = freeNumbers.get(0).getMsisdn();
            String lockKey = "lock:msisdn:" + msisdn;
            RLock lock = redissonClient.getLock(lockKey);
            log.info("[MSISDN] " + msisdn + " için dağıtık kilit alınmaya çalışılıyor. Anahtar: " + lockKey);

            try {
                boolean isLocked = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
                if (isLocked) {
                    try {
                        // Kilit altındayken numaranın hala FREE olduğunu veritabanından teyit et
                        Optional<MsisdnPool> verifiedPoolOpt = msisdnPoolRepository
                                .findByMsisdnAndStatus(msisdn, MsisdnStatus.FREE);

                        if (verifiedPoolOpt.isPresent()) {
                            MsisdnPool msisdnPool = verifiedPoolOpt.get();
                            msisdnPool.setStatus(MsisdnStatus.RESERVED);
                            msisdnPoolRepository.save(msisdnPool);
                            log.info("[MSISDN] Numara " + msisdn + " başarıyla RESERVED olarak işaretlendi.");
                            return msisdn;
                        } else {
                            log.warning("[MSISDN] Numara " + msisdn + " başka bir işlem tarafından kapılmış. Tekrar deneniyor...");
                        }
                    } finally {
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                            log.info("[MSISDN] Dağıtık kilit serbest bırakıldı. Anahtar: " + lockKey);
                        }
                    }
                } else {
                    log.warning("[MSISDN] " + msisdn + " için kilit alınamadı. Tekrar deneniyor...");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MsisdnAllocationException("Numara tahsis işlemi kesintiye uğradı.");
            }
        }
        throw new MsisdnAllocationException("Numara tahsisi sırasında üst üste çakışma yaşandı. Lütfen tekrar deneyin.");
    }

    /**
     * RESERVED bir MSISDN'i ALLOCATED (aktif aboneliğe atanmış) olarak işaretler.
     *
     * @param msisdn Aktive edilecek numara
     */
    @Transactional
    public void confirmAllocation(String msisdn) {
        MsisdnPool msisdnPool = msisdnPoolRepository
                .findByMsisdnAndStatus(msisdn, MsisdnStatus.RESERVED)
                .orElseThrow(() -> new MsisdnAllocationException(
                        "Numara bulunamadı veya RESERVED statüsünde değil: " + msisdn));

        msisdnPool.setStatus(MsisdnStatus.ALLOCATED);
        msisdnPool.setReservedUntil(null);
        msisdnPoolRepository.save(msisdnPool);
        log.info("[MSISDN] Numara ALLOCATED olarak teyit edildi: " + msisdn);
    }

    /**
     * Bir MSISDN'i serbest bırakır (abonelik sonlandırıldığında).
     *
     * @param msisdn Serbest bırakılacak numara
     */
    @Transactional
    public void releaseMsisdn(String msisdn) {
        Optional<MsisdnPool> poolOpt = msisdnPoolRepository.findById(msisdn);
        if (poolOpt.isPresent()) {
            MsisdnPool pool = poolOpt.get();
            pool.setStatus(MsisdnStatus.FREE);
            pool.setReservedUntil(null);
            msisdnPoolRepository.save(pool);
            log.info("[MSISDN] Numara havuza geri döndürüldü: " + msisdn);
        }
    }
}
