package com.docflow.ai.common.config;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 配置加密服务 —— 敏感配置加密存储
 * <p>
 * 加密策略：
 * <ul>
 *   <li>AES-256-GCM 加密算法</li>
 *   <li>每次加密生成随机 IV（初始化向量）</li>
 *   <li>GCM 模式提供认证加密（AEAD）</li>
 * </ul>
 * <p>
 * 面试考点：
 * <ul>
 *   <li>对称加密 vs 非对称加密</li>
 *   <li>AES-GCM vs AES-CBC 的安全性</li>
 *   <li>密钥管理最佳实践</li>
 *   <li>什么是 AEAD（Authenticated Encryption with Associated Data）</li>
 * </ul>
 * <p>
 * 使用场景：
 * <ul>
 *   <li>数据库密码加密</li>
 *   <li>API Key 加密</li>
 *   <li>第三方服务凭证加密</li>
 * </ul>
 */
public class ConfigEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final SecretKey secretKey;

    public ConfigEncryptionService(String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 生成新的密钥
     */
    public static String generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey key = keyGen.generateKey();
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * 加密配置值
     * @param plaintext 明文
     * @return Base64 编码的密文（IV + 密文）
     */
    public String encrypt(String plaintext) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

        byte[] encrypted = cipher.doFinal(plaintext.getBytes());

        // Combine IV + encrypted data
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * 解密配置值
     * @param ciphertext Base64 编码的密文
     * @return 明文
     */
    public String decrypt(String ciphertext) throws Exception {
        byte[] combined = Base64.getDecoder().decode(ciphertext);

        byte[] iv = new byte[IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, IV_LENGTH);

        byte[] encrypted = new byte[combined.length - IV_LENGTH];
        System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted);
    }

    /**
     * 检查值是否已加密
     */
    public boolean isEncrypted(String value) {
        if (value == null || !value.startsWith("ENC(") || !value.endsWith(")")) {
            return false;
        }
        try {
            Base64.getDecoder().decode(value.substring(4, value.length() - 1));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 包装加密值
     */
    public String wrapEncrypted(String ciphertext) {
        return "ENC(" + ciphertext + ")";
    }

    /**
     * 解包加密值
     */
    public String unwrapEncrypted(String wrapped) {
        return wrapped.substring(4, wrapped.length() - 1);
    }
}
