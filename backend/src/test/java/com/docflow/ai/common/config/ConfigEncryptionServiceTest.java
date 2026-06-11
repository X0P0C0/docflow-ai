package com.docflow.ai.common.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("配置加密服务测试")
class ConfigEncryptionServiceTest {

    private ConfigEncryptionService encryptionService;

    @BeforeEach
    void setUp() throws Exception {
        String key = ConfigEncryptionService.generateKey();
        encryptionService = new ConfigEncryptionService(key);
    }

    @Test
    @DisplayName("加密解密往返测试")
    void shouldEncryptAndDecrypt() throws Exception {
        String plaintext = "my-secret-password-123";
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        assertThat(encrypted).isNotEqualTo(plaintext);
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("加密后密文不同（随机 IV）")
    void shouldProduceDifferentCiphertext() throws Exception {
        String plaintext = "same-plaintext";
        String encrypted1 = encryptionService.encrypt(plaintext);
        String encrypted2 = encryptionService.encrypt(plaintext);

        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    @Test
    @DisplayName("包装和解包加密值")
    void shouldWrapAndUnwrap() throws Exception {
        String plaintext = "database-password";
        String encrypted = encryptionService.encrypt(plaintext);
        String wrapped = encryptionService.wrapEncrypted(encrypted);

        assertThat(wrapped).startsWith("ENC(");
        assertThat(wrapped).endsWith(")");
        assertThat(encryptionService.isEncrypted(wrapped)).isTrue();

        String unwrapped = encryptionService.unwrapEncrypted(wrapped);
        String decrypted = encryptionService.decrypt(unwrapped);
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("非加密值应返回 false")
    void shouldDetectNonEncrypted() {
        assertThat(encryptionService.isEncrypted(null)).isFalse();
        assertThat(encryptionService.isEncrypted("")).isFalse();
        assertThat(encryptionService.isEncrypted("plain-text")).isFalse();
        assertThat(encryptionService.isEncrypted("ENC(!!!)")).isFalse();
    }

    @Test
    @DisplayName("加密中文内容")
    void shouldEncryptChineseContent() throws Exception {
        String plaintext = "数据库密码：测试123！@#";
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(plaintext);
    }
}
