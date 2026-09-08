package vgandolfi.dev.mana_paes.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Bean {@link TextEncryptor} (AES-256/GCM via {@code Encryptors.delux}):
 * round-trip de criptografia e fail-fast com master-key em branco.
 */
class EncryptionConfigTest {

    private AppProperties props(String masterKey) {
        return new AppProperties(
                new AppProperties.Jwt("test-secret-test-secret-test-secret-test-secret-1234", 3600000L, 86400000L),
                new AppProperties.Encryption(masterKey),
                new AppProperties.Evolution("", "", 0L),
                new AppProperties.Backend(""),
                new AppProperties.Frontend("http://localhost"),
                new AppProperties.Mail(false),
                new AppProperties.Notifications(false, 2),
                new AppProperties.Scheduler(false));
    }

    @Test
    void encryptDecryptRoundTripWithFixedSalt() {
        TextEncryptor encryptor = new EncryptionConfig().textEncryptor(props("mana-paes-test-master-key-32chars!"));

        String encrypted = encryptor.encrypt("instance-token-secret");

        assertThat(encrypted).isNotEqualTo("instance-token-secret");
        assertThat(encryptor.decrypt(encrypted)).isEqualTo("instance-token-secret");
    }

    @Test
    void ciphertextIsDifferentForSamePlaintext() {
        TextEncryptor encryptor = new EncryptionConfig().textEncryptor(props("mana-paes-test-master-key-32chars!"));

        assertThat(encryptor.encrypt("same")).isNotEqualTo(encryptor.encrypt("same"));
    }

    @Test
    void blankMasterKeyFailsFast() {
        assertThatThrownBy(() -> new EncryptionConfig().textEncryptor(props("  ")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("master-key");
    }
}