package com.vapilot.service.mobile;

import java.util.Random;

public final class OtpManager {
    private static final long OTP_TTL_MS = 5 * 60 * 1000L;

    private OtpManager() {
    }

    public static String generateOtp() {
        int number = 100000 + new Random().nextInt(900000);
        return String.valueOf(number);
    }

    public static void startOtpFlow(SessionManager session, String email, String purpose, String name) throws Exception {
        String otp = generateOtp();
        EmailJsClient.sendOtpEmail(email, otp, purpose, name);
        long now = System.currentTimeMillis();
        session.saveOtpState(new OtpState(email, purpose, name, otp, now, now + OTP_TTL_MS));
    }

    public static void resendOtpFlow(SessionManager session) throws Exception {
        OtpState state = session.getOtpState();
        if (state == null) {
            throw new Exception("No active OTP request found.");
        }
        startOtpFlow(session, state.email, state.purpose, state.name);
    }

    public static OtpVerifyResult verifyOtp(SessionManager session, String code) {
        OtpState state = session.getOtpState();
        if (state == null) return new OtpVerifyResult(false, "missing", null);
        if (System.currentTimeMillis() > state.expiresAt) return new OtpVerifyResult(false, "expired", state);
        if (!String.valueOf(state.otp).equals(String.valueOf(code))) {
            return new OtpVerifyResult(false, "invalid", state);
        }
        return new OtpVerifyResult(true, "ok", state);
    }
}
