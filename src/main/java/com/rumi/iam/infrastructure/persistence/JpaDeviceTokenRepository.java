package com.rumi.iam.infrastructure.persistence;

import com.rumi.iam.domain.model.DeviceToken;
import com.rumi.iam.domain.repository.DeviceTokenRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaDeviceTokenRepository implements DeviceTokenRepository {

    private final SpringDataDeviceTokenRepository springDataRepository;

    public JpaDeviceTokenRepository(SpringDataDeviceTokenRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<DeviceToken> findByToken(String token) {
        return springDataRepository.findByToken(token).map(this::toDomain);
    }

    /**
     * Flushes at once so that a token registered twice at the same time fails here, on the unique index.
     */
    @Override
    public DeviceToken save(DeviceToken deviceToken) {
        return toDomain(springDataRepository.saveAndFlush(toEntity(deviceToken)));
    }

    private DeviceTokenJpaEntity toEntity(DeviceToken deviceToken) {
        return new DeviceTokenJpaEntity(
                deviceToken.getId(),
                deviceToken.getUserId(),
                deviceToken.getToken(),
                deviceToken.getPlatform(),
                deviceToken.getRegisteredAt(),
                deviceToken.getUpdatedAt()
        );
    }

    private DeviceToken toDomain(DeviceTokenJpaEntity entity) {
        return new DeviceToken(
                entity.getId(),
                entity.getUserId(),
                entity.getToken(),
                entity.getPlatform(),
                entity.getRegisteredAt(),
                entity.getUpdatedAt()
        );
    }
}
