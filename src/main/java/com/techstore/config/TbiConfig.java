package com.techstore.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "tbi.fusion-pay")
@Data
public class TbiConfig {

    private String  apiBaseUrl     = "https://beta.tbibank.support";
    private String  resellerCode;
    private String  resellerKey;

    /**
     * AES-256-CTR encryption key provided by TBI for the BIVD merchant account.
     * Must be set via TBI_ENCRYPTION_KEY environment variable.
     */
    private String  encryptionKey;

    /**
     * Publicly reachable HTTPS URL of our webhook endpoint.
     * TBI will POST status updates here after RegisterApplication.
     * Example: https://api.caretech.bg/api/tbi/webhook
     */
    private String  webhookUrl;

    /** Set to false to disable all TBI API calls (e.g. in local dev without credentials) */
    private boolean enabled = true;

    // ── Derived endpoint URLs ────────────────────────────────────────────────

    public String getRegisterApplicationUrl() {
        return apiBaseUrl + "/api/RegisterApplication";
    }

    public String getGetCalculationsUrl() {
        return apiBaseUrl + "/api/GetCalculations";
    }

    public String getGetApplicationStatusUrl() {
        return apiBaseUrl + "/api/GetApplicationStatus";
    }

    public String getGetApplicationDataUrl() {
        return apiBaseUrl + "/api/GetApplicationData";
    }
}
