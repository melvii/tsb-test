package com.tsb.banking.otp;

public interface SmsOtpGateway {
    void sendOtp(String phoneE164);
    boolean verifyOtp(String phoneE164, String code);
}
