package com.techstore.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * TBI Fusion Pay encryption utility.
 *
 * Algorithm : AES-256-CTR  (matches PHP Cryptor.php — cipher_algo = 'aes-256-ctr')
 * Key        : SHA-256 hash of the raw key string  (hash_algo = 'sha256')
 * IV         : 16 random bytes, prepended to the ciphertext before Base64 encoding
 * Format     : Base64(IV || ciphertext)
 */
public final class TbiEncryptionUtil {

    private static final String ALGORITHM      = "AES";
    private static final String TRANSFORMATION = "AES/CTR/NoPadding";
    private static final int    IV_LENGTH      = 16;

    private TbiEncryptionUtil() {}

    /**
     * Encrypts a plain-text JSON string using the TBI encryption scheme.
     *
     * @param plaintext the JSON to encrypt
     * @param key       the merchant encryption key provided by TBI
     * @return Base64-encoded string: IV (16 bytes) || AES-CTR ciphertext
     */
    public static String encrypt(String plaintext, String key) {
        try {
            byte[] keyHash  = sha256(key);
            byte[] iv       = generateIv();

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(keyHash, ALGORITHM),
                    new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] result = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv,        0, result, 0,         IV_LENGTH);
            System.arraycopy(encrypted, 0, result, IV_LENGTH, encrypted.length);

            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new IllegalStateException("TBI encryption failed", e);
        }
    }

    /**
     * Decrypts a TBI-encrypted Base64 string back to plain-text JSON.
     * Used for Express Buy callbacks where TBI sends encrypted customer data.
     *
     * @param encryptedBase64 Base64-encoded IV || ciphertext
     * @param key             the merchant encryption key provided by TBI
     * @return decrypted plain-text JSON
     */
    public static String decrypt(String encryptedBase64, String key) {
        try {
            byte[] raw = Base64.getDecoder().decode(encryptedBase64);

            if (raw.length < IV_LENGTH) {
                throw new IllegalArgumentException(
                        "Encrypted payload too short (length=" + raw.length + ")");
            }

            byte[] iv         = Arrays.copyOfRange(raw, 0,         IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(raw, IV_LENGTH, raw.length);
            byte[] keyHash    = sha256(key);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(keyHash, ALGORITHM),
                    new IvParameterSpec(iv));

            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("TBI decryption failed", e);
        }
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private static byte[] sha256(String input) throws Exception {
        return MessageDigest.getInstance("SHA-256")
                .digest(input.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] generateIv() {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}
