/**
 * Copyright (c) 2018, 1Kosmos Inc. All rights reserved.
 * Licensed under 1Kosmos Open Source Public License version 1.0 (the "License");
 * You may not use this file except in compliance with the License. 
 * You may obtain a copy of this license at 
 *    https://github.com/1Kosmos/1Kosmos_License/blob/main/LICENSE.txt
 */
package com.bidsdk.utils;
import java.util.Base64;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.bouncycastle.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptDecryptLogic {

    private enum EncryptMode {
        ENCRYPT, DECRYPT
    }

    private Cipher _cx;
    private byte[] _key, _iv;

    public EncryptDecryptLogic() throws NoSuchAlgorithmException, NoSuchPaddingException {
        // initialize the cipher with transformation AES/CBC/PKCS5Padding
        _cx = Cipher.getInstance("AES/GCM/NoPadding");
        _key = new byte[32]; //256 bit key space
        _iv = new byte[16]; //128 bit IV
    }

    private byte[] encryptDecrypt(String inputText, String encryptionKey, EncryptMode mode, String initVector) throws UnsupportedEncodingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException  {
        int len = encryptionKey.getBytes("UTF-8").length; // length of the key	provided

        if (encryptionKey.getBytes("UTF-8").length > _key.length)
            len = _key.length;

        int ivlength = initVector.getBytes("UTF-8").length;

        if (initVector.getBytes("UTF-8").length > _iv.length) {
            ivlength = _iv.length;
        }
        System.arraycopy(Base64.getDecoder().decode(encryptionKey.getBytes()), 0, _key, 0, len);
        System.arraycopy(initVector.getBytes("UTF-8"), 0, _iv, 0, ivlength);

        SecretKeySpec keySpec = new SecretKeySpec(_key, "AES"); // Create a new SecretKeySpec for the specified key data and algorithm name.
        IvParameterSpec ivSpec = new IvParameterSpec(_iv); // Create a new IvParameterSpec instance with the bytes from the specified buffer iv used as initialization vector.

        //encryption
        if (mode.equals(EncryptMode.ENCRYPT)) {
            // Potentially insecure random numbers on Android 4.3 and older. Read for more info.
            _cx.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);// Initialize this cipher instance
            return Arrays.concatenate(_iv, _cx.doFinal(inputText.getBytes("UTF-8")));
        } else {
            byte[] decodedValue = Base64.getDecoder().decode(inputText.getBytes());
            _cx.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(Arrays.copyOfRange(decodedValue, 0, 16)));// Initialize this cipher instance
            return _cx.doFinal(Arrays.copyOfRange(decodedValue, 16, decodedValue.length)); // Finish multi-part transformation (decryption)
        }
    }

    private static String SHA256(String text, int length) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String resultString;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(text.getBytes("UTF-8"));
        byte[] digest = md.digest();
        StringBuilder result = new StringBuilder();
        for (byte b : digest) {
            result.append(String.format("%02x", b)); //convert to hex
        }
        if (length > result.toString().length()) {
            resultString = result.toString();
        } else {
            resultString = result.toString().substring(0, length);
        }
        return resultString;
    }

    private String encryptPlainText(String plainText, String key, String iv) throws Exception {
        byte[] bytes = encryptDecrypt(plainText, EncryptDecryptLogic.SHA256(key, 32), EncryptMode.ENCRYPT, iv);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String decryptCipherText(String cipherText, String key, String iv) throws Exception {
        byte[] bytes = encryptDecrypt(cipherText, EncryptDecryptLogic.SHA256(key, 32), EncryptMode.DECRYPT, iv);
        return new String(bytes);
    }


    private String encryptPlainTextWithRandomIV(String plainText, String key) throws Exception {
        byte[] bytes = encryptDecrypt(generateRandomIV16() + plainText, EncryptDecryptLogic.SHA256(key, 32), EncryptMode.ENCRYPT, generateRandomIV16());
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String decryptCipherTextWithRandomIV(String cipherText, String key) throws Exception {
        byte[] bytes = encryptDecrypt(cipherText, EncryptDecryptLogic.SHA256(key, 32), EncryptMode.DECRYPT, generateRandomIV16());
        String out = new String(bytes);
        return out.substring(16, out.length());
    }

    private String encryptPlainTextWithRandomIVNew(String plainText, String key) throws Exception {
        byte[] bytes = encryptDecrypt(plainText, key, EncryptMode.ENCRYPT, generateRandomIV16());
        return Base64.getEncoder().encodeToString(bytes);
    }

    public String ecdsaHelper(String method, String str, String key) {

        try {
            if ("encrypt".equalsIgnoreCase(method)) {
                return encryptPlainTextWithRandomIVNew(str, key);
            }
            else if ("decrypt".equalsIgnoreCase(method)) {
                return decryptCipherTextWithRandomIVNew(str, key);
            }
        }
        catch (Exception ex) {
            //no-op
        }

        return null;
    }

    private String decryptCipherTextWithRandomIVNew(String cipherText, String key) throws Exception {
        byte[] bytes = encryptDecrypt(cipherText, key, EncryptMode.DECRYPT, generateRandomIV16());
        String decryptedString = new String(bytes);
        return decryptedString;
    }

    private String generateRandomIV16() {
        SecureRandom ranGen = new SecureRandom();
        byte[] aesKey = new byte[16];
        ranGen.nextBytes(aesKey);
        StringBuilder result = new StringBuilder();
        for (byte b : aesKey) {
            result.append(String.format("%02x", b)); //convert to hex
        }
        if (16 > result.toString().length()) {
            return result.toString();
        } else {
            return result.toString().substring(0, 16);
        }
    }
}
