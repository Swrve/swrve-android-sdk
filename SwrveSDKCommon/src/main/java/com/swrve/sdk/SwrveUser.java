package com.swrve.sdk;

public class SwrveUser  {

    private String swrveUserId;
    private String externalUserId;
    private boolean verified;

    public SwrveUser(String swrveUserId, String externalUserId, boolean verified) {
        this.swrveUserId = swrveUserId;
        this.externalUserId = externalUserId;
        this.verified = verified;
    }

    public String getSwrveUserId() {
        return swrveUserId;
    }

    public void setSwrveUserId(String swrveUserId) {
        this.swrveUserId = swrveUserId;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public void setExternalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}
