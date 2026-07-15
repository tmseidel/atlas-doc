package com.mdeg.docsportal.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Verifies Gitea HMAC signatures (X-Hub-Signature-256 = sha256=HEX).
 */
public final class GiteaHmacVerifier {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String PREFIX = "sha256=";

    private GiteaHmacVerifier() {}

    public static boolean verify(String signature, String payload, String secret) {
        if (signature == null || !signature.startsWith(PREFIX)) {
            return false;
        }
        try {
            String expectedHex = signature.substring(PREFIX.length());
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] hmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String actualHex = bytesToHex(hmac);
            return expectedHex.equals(actualHex);
        } catch (Exception e) {
            return false;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
