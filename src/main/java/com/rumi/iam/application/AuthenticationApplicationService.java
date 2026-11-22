package com.rumi.iam.application;

import com.rumi.iam.domain.model.EmailAddress;
import com.rumi.iam.domain.model.PasswordPolicy;
import com.rumi.iam.domain.model.PasswordResetToken;
import com.rumi.iam.domain.model.User;
import com.rumi.iam.domain.repository.PasswordResetTokenRepository;
import com.rumi.iam.domain.repository.UserRepository;
import com.rumi.iam.domain.service.GeneratedResetToken;
import com.rumi.iam.domain.service.PasswordHasher;
import com.rumi.iam.domain.service.PasswordResetNotifier;
import com.rumi.iam.domain.service.ResetTokenGenerator;
import com.rumi.iam.domain.service.TokenIssuer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Sign-up (US01), sign-in (US02) and password recovery (US03).
 */
@Service
@Transactional(readOnly = true)
public class AuthenticationApplicationService {

    private static final String DUMMY_PASSWORD = "dummy-password-used-only-to-spend-time";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final ResetTokenGenerator resetTokenGenerator;
    private final PasswordResetNotifier passwordResetNotifier;
    private final PasswordResetPolicy passwordResetPolicy;
    private final Clock clock;

    private volatile String dummyHash;

    public AuthenticationApplicationService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            TokenIssuer tokenIssuer,
            PasswordResetTokenRepository resetTokenRepository,
            ResetTokenGenerator resetTokenGenerator,
            PasswordResetNotifier passwordResetNotifier,
            PasswordResetPolicy passwordResetPolicy,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.resetTokenRepository = resetTokenRepository;
        this.resetTokenGenerator = resetTokenGenerator;
        this.passwordResetNotifier = passwordResetNotifier;
        this.passwordResetPolicy = passwordResetPolicy;
        this.clock = clock;
    }

    /**
     * Registers a new administrator. The password is checked against the policy before it is hashed.
     */
    @Transactional
    public User signUp(String email, String fullName, String password) {
        EmailAddress emailAddress = new EmailAddress(email);
        PasswordPolicy.requireValid(password);
        if (userRepository.existsByEmail(emailAddress)) {
            throw new EmailAlreadyRegisteredException();
        }
        User user = User.registerAdministrator(
                UUID.randomUUID(), emailAddress, fullName, passwordHasher.hash(password), clock.instant());
        return userRepository.save(user);
    }

    /**
     * Checks the credentials and issues a token. An unknown email and a wrong password fail the same way,
     * and both run one hash comparison so that the response time does not tell them apart.
     */
    public AuthenticatedUser signIn(String email, String password) {
        Optional<User> user = findByEmail(email);
        String hash = user.map(User::getPasswordHash).orElseGet(this::dummyHash);
        boolean matches = password != null && passwordHasher.matches(password, hash);
        if (user.isEmpty() || !matches) {
            throw new InvalidCredentialsException();
        }
        return new AuthenticatedUser(user.get(), tokenIssuer.issue(user.get()));
    }

    /**
     * Sends a single-use reset link to the user of {@code email} (US03). Earlier unused links of that user stop
     * working. An unknown or malformed email does nothing and does not fail, so the caller cannot tell which
     * emails are registered.
     */
    @Transactional
    public void requestPasswordReset(String email) {
        findByEmail(email).ifPresent(user -> {
            Instant now = clock.instant();
            resetTokenRepository.invalidateUnusedByUserId(user.getId(), now);
            GeneratedResetToken generated = resetTokenGenerator.generate();
            PasswordResetToken token = resetTokenRepository.save(PasswordResetToken.issue(
                    UUID.randomUUID(), user.getId(), generated.hash(), now, passwordResetPolicy.tokenLifetime()));
            passwordResetNotifier.notify(user, generated.raw(), token.getExpiresAt());
        });
    }

    private Optional<User> findByEmail(String email) {
        try {
            return userRepository.findByEmail(new EmailAddress(email));
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }

    private String dummyHash() {
        if (dummyHash == null) {
            dummyHash = passwordHasher.hash(DUMMY_PASSWORD);
        }
        return dummyHash;
    }
}
