package com.rumi.iam.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataDeviceTokenRepository extends JpaRepository<DeviceTokenJpaEntity, UUID> {

    Optional<DeviceTokenJpaEntity> findByToken(String token);
}
