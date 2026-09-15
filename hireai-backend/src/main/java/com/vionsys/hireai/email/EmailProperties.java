package com.vionsys.hireai.email;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "hireai.mail")
public class EmailProperties {

    /**
     * Master switch to enable/disable automated emails.
     */
    private boolean enabled = true;

    /**
     * Default from address.
     */
    private String from = "notifications@vionsys.com";

    /**
     * Sender display name.
     */
    private String senderName = "HireAI Recruitment Platform";
}
