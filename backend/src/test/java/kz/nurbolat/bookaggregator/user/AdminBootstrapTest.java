package kz.nurbolat.bookaggregator.user;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBootstrapTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    @Test
    void shouldCreateVerifiedAdminWhenEmailAndPasswordAreSet() {
        when(passwordEncoder.encode("s3cret-pass")).thenReturn("hashed");

        run(new AdminProperties(" admin@example.com ", "s3cret-pass"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("admin@example.com");
        assertThat(saved.getValue().getPassword()).isEqualTo("hashed");
        assertThat(saved.getValue().getRole()).isEqualTo(User.Role.ADMIN);
        assertThat(saved.getValue().isEmailVerified()).isTrue();
    }

    @Test
    void shouldLeaveAccountUnchangedWhenEmailIsAlreadyRegistered() {
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(true);

        run(new AdminProperties("admin@example.com", "s3cret-pass"));

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void shouldCreateNothingWhenVariablesAreMissingOrHalfSet() {
        run(new AdminProperties(null, null));
        run(new AdminProperties("admin@example.com", ""));
        run(new AdminProperties("", "s3cret-pass"));

        verify(userRepository, never()).save(any());
    }

    private void run(AdminProperties properties) {
        new AdminBootstrap(properties, userRepository, passwordEncoder).run(new DefaultApplicationArguments());
    }
}
