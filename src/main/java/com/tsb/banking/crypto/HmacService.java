

package com.tsb.banking.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class HmacService {
    private final ThreadLocal<Mac> macTL;

    public HmacService(@Value("${app.crypto.hmacKeyB64}") String keyB64) {
        byte[] key = Base64.getDecoder().decode(keyB64);
        this.macTL = ThreadLocal.withInitial(() -> {
            try {
                Mac m = Mac.getInstance("HmacSHA256");
                m.init(new SecretKeySpec(key, "HmacSHA256"));
                return m;
            } catch (Exception e) {
                throw new IllegalStateException("Cannot init HMAC", e);
            }
        });
    }

    public String hmacHex(String input) {
        Mac m = macTL.get();
        m.reset();
        byte[] out = m.doFinal(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(out.length * 2);
        for (byte b : out) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
