package com.vapilot.service.mobile;

public class OtpVerifyResult {
    public boolean ok;
    public String reason;
    public OtpState state;

    public OtpVerifyResult(boolean ok, String reason, OtpState state) {
        this.ok = ok;
        this.reason = reason;
        this.state = state;
    }
}
