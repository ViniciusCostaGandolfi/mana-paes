package vgandolfi.dev.mana_paes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

/**
 * Criptografia simétrica para segredos em repouso (ex: token da instância
 * Evolution — {@code instance_api_key}).
 *
 * <p>{@link TextEncryptor} via {@link Encryptors#delux}: AES-256/GCM com IV
 * aleatório por operação e saída em hex (derivação de chave PBKDF2-HmacSHA1,
 * 1024 iterações). O salt é FIXO e documentado: hex de {@code "mana-paes-salt"}
 * = {@code 6d616e612d706165732d73616c74}.</p>
 *
 * <p>A chave-mestre vem de {@code app.encryption.master-key}
 * (env {@code APP_ENCRYPTION_MASTER_KEY}): placeholder em dev/test e
 * OBRIGATÓRIA em produção (sem default em application-prod.yaml — o boot falha
 * se ausente). Double-check aqui: master-key em branco também falha.</p>
 */
@Configuration
public class EncryptionConfig {

    /** Hex de "mana-paes-salt" (salt fixo derivado do nome do produto). */
    public static final String MASTER_KEY_SALT_HEX = "6d616e612d706165732d73616c74";

    @Bean
    public TextEncryptor textEncryptor(AppProperties appProperties) {
        String masterKey = appProperties.encryption().masterKey();
        if (masterKey == null || masterKey.isBlank()) {
            throw new IllegalStateException(
                    "app.encryption.master-key não configurada (env APP_ENCRYPTION_MASTER_KEY obrigatória)");
        }
        return Encryptors.delux(masterKey, MASTER_KEY_SALT_HEX);
    }
}