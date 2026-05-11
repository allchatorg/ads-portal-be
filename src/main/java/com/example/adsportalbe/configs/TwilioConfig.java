package com.example.adsportalbe.configs;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConfigurationProperties(prefix = "twilio")
@Getter
@Setter
public class TwilioConfig {

    private String accountSid;
    private String authToken;
    private String phoneNumber;
    private String messagingServiceSid;

    @PostConstruct
    public void init() {
        if (StringUtils.hasText(accountSid) && StringUtils.hasText(authToken)) {
            Twilio.init(accountSid, authToken);
        }
    }
}
