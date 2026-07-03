package com.telcox.springmicroservices.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "communication_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunicationPreference {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "allow_sms")
    @Builder.Default
    private boolean allowSms = true;

    @Column(name = "allow_email")
    @Builder.Default
    private boolean allowEmail = true;

    @Column(name = "allow_push")
    @Builder.Default
    private boolean allowPush = true;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
