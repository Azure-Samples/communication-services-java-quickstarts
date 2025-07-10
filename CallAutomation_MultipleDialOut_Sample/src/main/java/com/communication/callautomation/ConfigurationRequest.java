package com.communication.callautomation;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "acs")
@Getter
public class ConfigurationRequest {
    private String acsConnectionString;
    private String acsInboundPhoneNumber;
    private String acsOutboundPhoneNumber;
    private String userPhoneNumber;
    private String acsTestIdentity2;
    private String acsTestIdentity3;
    private String callbackUriHost;
    private String pmaEndpoint;

    // Getters and Setters
    public void setAcsConnectionString(String acsConnectionString) {
        this.acsConnectionString = acsConnectionString;
    }

    public void setAcsInboundPhoneNumber(String acsInboundPhoneNumber) {
        this.acsInboundPhoneNumber = acsInboundPhoneNumber;
    }

    public void setAcsTestIdentity2(String acsTestIdentity2) {
        this.acsTestIdentity2 = acsTestIdentity2;
    }

    public void setAcsTestIdentity3(String acsTestIdentity3) {
        this.acsTestIdentity3 = acsTestIdentity3;
    }

    public void setAcsOutboundPhoneNumber(String acsOutboundPhoneNumber) {
        this.acsOutboundPhoneNumber = acsOutboundPhoneNumber;
    }

    public void setUserPhoneNumber(String userPhoneNumber) {
        this.userPhoneNumber = userPhoneNumber;
    }

    public void setCallbackUriHost(String callbackUriHost) {
        this.callbackUriHost = callbackUriHost;
    }

    public void setPmaEndpoint(String pmaEndpoint) {
        this.pmaEndpoint = pmaEndpoint;
    }
}
