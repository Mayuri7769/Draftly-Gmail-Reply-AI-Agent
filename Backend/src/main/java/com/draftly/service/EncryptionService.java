package com.draftly.service;

import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

@Service
public class EncryptionService {

    // You should store this password/salt in environment variables
    private final TextEncryptor encryptor = Encryptors.text("your-secret-password", "deadbeef");

    /**
     * Requirement: Ensure all stored tokens and user preferences are encrypted.
     */
    public String encrypt(String data) {
        return encryptor.encrypt(data);
    }

    public String decrypt(String encryptedData) {
        return encryptor.decrypt(encryptedData);
    }
}