package com.mdeg.docsportal.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GiteaHmacVerifierTest {

    private static final String SECRET = "test-secret-123";

    @Test
    void shouldVerifyValidHmacSignature() {
        String payload = "{\"repository\":{\"full_name\":\"org/repo\"}}";
        String signature = computeHmac(payload, SECRET);

        assertThat(GiteaHmacVerifier.verify(signature, payload, SECRET)).isTrue();
    }

    @Test
    void shouldRejectInvalidSignature() {
        String payload = "{\"repository\":{\"full_name\":\"org/repo\"}}";

        assertThat(GiteaHmacVerifier.verify("sha256=deadbeef", payload, SECRET)).isFalse();
    }

    @Test
    void shouldRejectNullSignature() {
        assertThat(GiteaHmacVerifier.verify(null, "payload", SECRET)).isFalse();
    }

    @Test
    void shouldRejectSignatureWithoutPrefix() {
        assertThat(GiteaHmacVerifier.verify("deadbeef", "payload", SECRET)).isFalse();
    }

    @Test
    void shouldRejectWrongSecret() {
        String payload = "{\"repository\":{\"full_name\":\"org/repo\"}}";
        String signature = computeHmac(payload, SECRET);

        assertThat(GiteaHmacVerifier.verify(signature, payload, "wrong-secret")).isFalse();
    }

    private String computeHmac(String payload, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hmac = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hmac) {
                sb.append(String.format("%02x", b));
            }
            return "sha256=" + sb;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
