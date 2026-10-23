package kz.nurbolat.bookaggregator.user;

import kz.nurbolat.bookaggregator.common.text.Emails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Creates the first administrator from the environment on startup, so that nobody has to edit the database.
 * An existing account with that email is left as it is: the password in the environment never overwrites it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final AdminProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean hasEmail = StringUtils.hasText(properties.email());
        boolean hasPassword = StringUtils.hasText(properties.password());
        if (!hasEmail && !hasPassword) {
            return;
        }
        if (!hasEmail || !hasPassword) {
            log.warn("Admin account not created: set both APP_ADMIN_EMAIL and APP_ADMIN_PASSWORD");
            return;
        }
        String email = properties.email().trim();
        if (userRepository.existsByEmail(email)) {
            log.info("Admin account {} already exists, left unchanged", Emails.mask(email));
            return;
        }
        User admin = new User();
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(properties.password()));
        admin.setName("Администратор");
        admin.setRole(User.Role.ADMIN);
        admin.setEmailVerified(true);
        userRepository.save(admin);
        log.info("Admin account {} created", Emails.mask(email));
    }
}
