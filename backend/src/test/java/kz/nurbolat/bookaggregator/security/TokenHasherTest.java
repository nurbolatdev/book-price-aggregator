package kz.nurbolat.bookaggregator.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenHasherTest {

    @Test
    void shouldIssueUrlSafeTokensOf32RandomBytes() {
        String first = TokenHasher.newToken();
        String second = TokenHasher.newToken();

        assertThat(first).hasSize(43).matches("[A-Za-z0-9_-]+");
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void shouldStoreSha256HexInsteadOfTheToken() {
        assertThat(TokenHasher.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void shouldMatchOnlyTheTokenThatWasHashed() {
        String token = TokenHasher.newToken();
        String hash = TokenHasher.hash(token);

        assertThat(TokenHasher.matches(token, hash)).isTrue();
        assertThat(TokenHasher.matches(TokenHasher.newToken(), hash)).isFalse();
    }
}
