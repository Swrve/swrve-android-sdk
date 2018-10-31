package com.swrve.sdk;

public interface SwrveIdentityResponse {
    void onSuccess(String status, String swrveId);
    void onError(int responseCode, String errorMessage);
}
