package com.telcox.springmicroservices.notification.repository;

import com.telcox.springmicroservices.notification.entity.CommunicationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommunicationPreferenceRepository extends JpaRepository<CommunicationPreference, UUID> {
}
