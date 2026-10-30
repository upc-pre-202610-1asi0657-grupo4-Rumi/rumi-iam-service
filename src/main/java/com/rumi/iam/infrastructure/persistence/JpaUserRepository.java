package com.rumi.iam.infrastructure.persistence;

import com.rumi.iam.application.EmailAlreadyRegisteredException;
import com.rumi.iam.domain.model.EmailAddress;
import com.rumi.iam.domain.model.User;
import com.rumi.iam.domain.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaUserRepository implements UserRepository {

    private final SpringDataUserRepository springDataRepository;

    public JpaUserRepository(SpringDataUserRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return springDataRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(EmailAddress email) {
        return springDataRepository.findByEmail(email.value()).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(EmailAddress email) {
        return springDataRepository.existsByEmail(email.value());
    }

    /**
     * Flushes at once so that two sign-ups racing with the same email fail here, on the unique index, and
     * not later at commit. Ids are random UUIDs, so the email is the only constraint that can be violated.
     */
    @Override
    public User save(User user) {
        try {
            return toDomain(springDataRepository.saveAndFlush(toEntity(user)));
        } catch (DataIntegrityViolationException duplicate) {
            throw new EmailAlreadyRegisteredException();
        }
    }

    private UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(
                user.getId(),
                user.getEmail().value(),
                user.getFullName(),
                user.getPasswordHash(),
                user.getRole(),
                user.getBuildingIds(),
                user.getCreatedAt()
        );
    }

    private User toDomain(UserJpaEntity entity) {
        return new User(
                entity.getId(),
                new EmailAddress(entity.getEmail()),
                entity.getFullName(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.getBuildingIds(),
                entity.getCreatedAt()
        );
    }
}
