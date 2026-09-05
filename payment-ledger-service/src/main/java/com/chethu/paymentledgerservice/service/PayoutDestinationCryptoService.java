package com.chethu.paymentledgerservice.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.chethu.paymentledgerservice.payment.provider.PaymentProviderException;

@Service
public class PayoutDestinationCryptoService {
    private static final String VERSION = "v1";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final String encodedKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public PayoutDestinationCryptoService(@Value("${payout.data-encryption-key:}") String encodedKey) {
        this.encodedKey = encodedKey == null ? "" : encodedKey.trim();
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new PaymentProviderException("Payout destination account is required.");
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(decodeKey(), "AES"),
                    new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return VERSION + ":" + Base64.getEncoder().encodeToString(iv) + ":"
                    + Base64.getEncoder().encodeToString(ciphertext);
        } catch (GeneralSecurityException ex) {
            throw new PaymentProviderException("Payout destination could not be encrypted securely.", ex);
        }
    }

    public String decrypt(String encodedCiphertext) {
        if (encodedCiphertext == null || encodedCiphertext.isBlank()) {
            throw new PaymentProviderException("Payout destination ciphertext is missing.");
        }
        try {
            String[] parts = encodedCiphertext.split(":", -1);
            if (parts.length != 3 || !VERSION.equals(parts[0])) {
                throw new PaymentProviderException("Payout destination ciphertext is invalid.");
            }
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] ciphertext = Base64.getDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decodeKey(), "AES"),
                    new GCMParameterSpec(TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (PaymentProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PaymentProviderException("Payout destination ciphertext is invalid.", ex);
        }
    }

    private byte[] decodeKey() {
        try {
            byte[] decoded = Base64.getDecoder().decode(encodedKey);
            if (decoded.length != 32) {
                throw new IllegalArgumentException();
            }
            return decoded;
        } catch (IllegalArgumentException ex) {
            throw new PaymentProviderException(
                    "PAYOUT_DATA_ENCRYPTION_KEY must be a Base64-encoded 256-bit key.", ex);
        }
    }
}
