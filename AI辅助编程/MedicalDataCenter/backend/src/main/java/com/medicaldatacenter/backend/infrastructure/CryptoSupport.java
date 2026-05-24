package com.medicaldatacenter.backend.infrastructure;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

public final class CryptoSupport {

    private CryptoSupport() {
    }

    public interface TextEncryptor {
        String encrypt(String plainText);

        String decrypt(String cipherText);
    }

    @Component
    public static class AesTextEncryptor implements TextEncryptor {
        private static final int GCM_TAG_LENGTH = 128;
        private static final int IV_LENGTH = 12;

        private final byte[] keyBytes;
        private final SecureRandom secureRandom = new SecureRandom();

        public AesTextEncryptor(@Value("${app.crypto.aes-key}") String secretKey) throws Exception {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            this.keyBytes = digest.digest(secretKey.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public String encrypt(String plainText) {
            try {
                if (plainText == null || plainText.isBlank()) {
                    return "";
                }
                byte[] iv = new byte[IV_LENGTH];
                secureRandom.nextBytes(iv);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"),
                        new GCMParameterSpec(GCM_TAG_LENGTH, iv));
                byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
                ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
                buffer.put(iv);
                buffer.put(encrypted);
                return Base64.getEncoder().encodeToString(buffer.array());
            } catch (Exception exception) {
                throw new IllegalStateException("encrypt failed", exception);
            }
        }

        @Override
        public String decrypt(String cipherText) {
            try {
                if (cipherText == null || cipherText.isBlank()) {
                    return "";
                }
                byte[] combined = Base64.getDecoder().decode(cipherText);
                ByteBuffer buffer = ByteBuffer.wrap(combined);
                byte[] iv = new byte[IV_LENGTH];
                buffer.get(iv);
                byte[] encrypted = new byte[buffer.remaining()];
                buffer.get(encrypted);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"),
                        new GCMParameterSpec(GCM_TAG_LENGTH, iv));
                return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
            } catch (Exception exception) {
                throw new IllegalStateException("decrypt failed", exception);
            }
        }
    }
}
