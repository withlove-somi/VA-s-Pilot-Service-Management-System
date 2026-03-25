package com.vapilot.service.mobile;

public class OtpState {
    public String email;
    public String purpose;
    public String name;
    public String otp;
    public long createdAt;
    public long expiresAt;

    public OtpState() {
    }

    public OtpState(String email, String purpose, String name, String otp, long createdAt, long expiresAt) {
        this.email = email;
        this.purpose = purpose;
        this.name = name;
        this.otp = otp;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }
}
