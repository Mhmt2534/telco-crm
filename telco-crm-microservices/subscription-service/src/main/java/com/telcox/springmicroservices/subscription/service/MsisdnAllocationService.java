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

    private static final String LOCK_KEY = "lock:msisdn:allocation";
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
        RLock lock = redissonClient.getLock(LOCK_KEY);
        log.info("[MSISDN] Dağıtık kilit alınmaya çalışılıyor. Anahtar: " + LOCK_KEY);

        try {
            boolean isLocked = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!isLocked) {
                log.warning("[MSISDN] Dağıtık kilit " + LOCK_WAIT_SECONDS + "sn içinde alınamadı.");
                throw new MsisdnAllocationException(
                        "Numara havuzu şu anda meşgul. Lütfen kısa süre sonra tekrar deneyin.");
            }

            log.info("[MSISDN] Kilit başarıyla alındı. Boş numara aranıyor...");

            List<MsisdnPool> freeNumbers = msisdnPoolRepository
                    .findFirstByStatus(MsisdnStatus.FREE, PageRequest.of(0, 1));

            if (freeNumbers.isEmpty()) {
                throw new MsisdnAllocationException("Havuzda tahsis edilebilecek boş numara kalmadı.");
            }

            MsisdnPool msisdnPool = freeNumbers.get(0);
            msisdnPool.setStatus(MsisdnStatus.RESERVED);
            msisdnPoolRepository.save(msisdnPool);

            log.info("[MSISDN] Numara RESERVED olarak işaretlendi: " + msisdnPool.getMsisdn());
            return msisdnPool.getMsisdn();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MsisdnAllocationException("Numara tahsis işlemi kesintiye uğradı.");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("[MSISDN] Dağıtık kilit serbest bırakıldı. Anahtar: " + LOCK_KEY);
            }
        }
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
