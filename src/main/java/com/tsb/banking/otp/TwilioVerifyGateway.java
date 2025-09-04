package com.tsb.banking.otp;

import com.twilio.Twilio;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TwilioVerifyGateway implements SmsOtpGateway {

    @Value("${app.otp.twilio.accountSid}")       private String accountSid;
    @Value("${app.otp.twilio.authToken}")        private String authToken;
    @Value("${app.otp.twilio.verifyServiceSid}") private String serviceSid;

    @PostConstruct
    void init() { Twilio.init(accountSid, authToken); 
    }


    @Override
    public void sendOtp(String phoneE164) {
        Verification.creator(serviceSid, phoneE164, "sms").create();
    }

    @Override
    public boolean verifyOtp(String phoneE164, String code) {
        var res = VerificationCheck.creator(serviceSid)
                .setTo(phoneE164)
                .setCode(code)
                .create();
        return "approved".equalsIgnoreCase(res.getStatus());
    }
}
