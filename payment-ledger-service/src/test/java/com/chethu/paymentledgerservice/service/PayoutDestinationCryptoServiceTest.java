package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.chethu.paymentledgerservice.payment.provider.PaymentProviderException;

class PayoutDestinationCryptoServiceTest {
    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void encryptsAndDecryptsDestinationWithoutStoringPlaintext() {
        PayoutDestinationCryptoService crypto = new PayoutDestinationCryptoService(KEY);
        String encrypted = crypto.encrypt("0123456789");

        assertEquals("0123456789", crypto.decrypt(encrypted));
        org.junit.jupiter.api.Assertions.assertFalse(encrypted.contains("0123456789"));
    }

    @Test
    void encryptionUsesRandomIv() {
        PayoutDestinationCryptoService crypto = new PayoutDestinationCryptoService(KEY);

        assertNotEquals(crypto.encrypt("0123456789"), crypto.encrypt("0123456789"));
    }

    @Test
    void invalidKeyFailsWhenEncryptionIsAttempted() {
        PayoutDestinationCryptoService crypto = new PayoutDestinationCryptoService("");

        assertThrows(PaymentProviderException.class, () -> crypto.encrypt("0123456789"));
    }
}
