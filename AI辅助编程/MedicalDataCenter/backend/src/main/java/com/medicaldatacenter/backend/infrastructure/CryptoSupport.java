package com.medicaldatacenter.backend.infrastructure;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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

        private final String activeKeyVersion;
        private final byte[] activeKeyBytes;
        private final Map<String, byte[]> knownKeys;
        private final SecureRandom secureRandom = new SecureRandom();

        public AesTextEncryptor(
                @Value("${app.crypto.aes-key}") String secretKey,
                @Value("${app.crypto.active-key-version:v1}") String activeKeyVersion,
                @Value("${app.crypto.previous-keys:}") String previousKeys) throws Exception {
            this.activeKeyVersion = normalizeVersion(activeKeyVersion);
            this.activeKeyBytes = hashKey(secretKey);
            this.knownKeys = buildKnownKeys(this.activeKeyVersion, this.activeKeyBytes, previousKeys);
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
                cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(activeKeyBytes, "AES"),
                        new GCMParameterSpec(GCM_TAG_LENGTH, iv));
                byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
                ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
                buffer.put(iv);
                buffer.put(encrypted);
                return activeKeyVersion + ":" + Base64.getEncoder().encodeToString(buffer.array());
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
                ParsedCipher parsedCipher = parseCipher(cipherText);
                if (parsedCipher.version() != null) {
                    byte[] keyBytes = knownKeys.get(parsedCipher.version());
                    if (keyBytes == null) {
                        throw new IllegalStateException("unknown key version: " + parsedCipher.version());
                    }
                    return decryptWithKey(parsedCipher.cipherBody(), keyBytes);
                }
                for (byte[] keyBytes : knownKeys.values()) {
                    try {
                        return decryptWithKey(parsedCipher.cipherBody(), keyBytes);
                    } catch (Exception ignored) {
                    }
                }
                throw new IllegalStateException("decrypt failed");
            } catch (Exception exception) {
                throw new IllegalStateException("decrypt failed", exception);
            }
        }

        private Map<String, byte[]> buildKnownKeys(String activeVersion, byte[] activeKey, String previousKeys)
                throws Exception {
            Map<String, byte[]> keys = new LinkedHashMap<>();
            keys.put(activeVersion, activeKey);
            if (StringUtils.hasText(previousKeys)) {
                Arrays.stream(previousKeys.split(","))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .forEach(entry -> {
                            int separator = entry.indexOf(':');
                            if (separator <= 0 || separator >= entry.length() - 1) {
                                throw new IllegalArgumentException(
                                        "app.crypto.previous-keys format must be version:key");
                            }
                            String version = normalizeVersion(entry.substring(0, separator));
                            String key = entry.substring(separator + 1).trim();
                            try {
                                keys.putIfAbsent(version, hashKey(key));
                            } catch (Exception exception) {
                                throw new IllegalStateException("invalid previous key", exception);
                            }
                        });
            }
            return Map.copyOf(keys);
        }

        private ParsedCipher parseCipher(String cipherText) {
            int separator = cipherText.indexOf(':');
            if (separator > 0 && separator < cipherText.length() - 1) {
                return new ParsedCipher(normalizeVersion(cipherText.substring(0, separator)),
                        cipherText.substring(separator + 1));
            }
            return new ParsedCipher(null, cipherText);
        }

        private String decryptWithKey(String cipherBody, byte[] keyBytes) throws Exception {
            byte[] combined = Base64.getDecoder().decode(cipherBody);
            ByteBuffer buffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"),
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        }

        private byte[] hashKey(String secretKey) throws Exception {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(secretKey.getBytes(StandardCharsets.UTF_8));
        }

        private String normalizeVersion(String version) {
            if (!StringUtils.hasText(version)) {
                throw new IllegalArgumentException("key version must not be blank");
            }
            return version.trim();
        }

        private record ParsedCipher(String version, String cipherBody) {
        }
    }
}
