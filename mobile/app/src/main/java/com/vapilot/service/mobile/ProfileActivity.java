package com.vapilot.service.mobile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends BaseDrawerActivity {
    private String profileImage;
    private String idPhoto;
    private String selfiePhoto;

    private final ActivityResultLauncher<String> pickProfile =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        profileImage = ImageUtils.uriToBase64(this, uri);
                    } catch (Exception ignored) {
                    }
                }
            });

    private final ActivityResultLauncher<String> pickIdPhoto =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        idPhoto = ImageUtils.uriToBase64(this, uri);
                    } catch (Exception ignored) {
                    }
                }
            });

    private final ActivityResultLauncher<String> pickSelfie =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        selfiePhoto = ImageUtils.uriToBase64(this, uri);
                    } catch (Exception ignored) {
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupDrawer(R.layout.activity_profile, "Profile");

        SessionManager session = new SessionManager(this);
        JsonObject user = session.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String name = user.has("fullname") ? user.get("fullname").getAsString()
                : (user.has("name") ? user.get("name").getAsString() : "Customer");
        String email = user.has("email") ? user.get("email").getAsString() : "";
        boolean verified = user.has("verified") && user.get("verified").getAsBoolean();

        TextView nameView = findViewById(R.id.profile_name);
        TextView emailView = findViewById(R.id.profile_email);
        TextView verifiedView = findViewById(R.id.profile_verified);
        TextView errorView = findViewById(R.id.profile_error);

        nameView.setText(name);
        emailView.setText(email);
        verifiedView.setText("Verified: " + (verified ? "Yes" : "No"));

        TextInputEditText verifyName = findViewById(R.id.profile_verify_name);
        TextInputEditText verifyEmail = findViewById(R.id.profile_verify_email);
        verifyName.setText(name);
        verifyEmail.setText(email);

        findViewById(R.id.profile_pick_image).setOnClickListener(v -> pickProfile.launch("image/*"));
        findViewById(R.id.profile_pick_id).setOnClickListener(v -> pickIdPhoto.launch("image/*"));
        findViewById(R.id.profile_pick_selfie).setOnClickListener(v -> pickSelfie.launch("image/*"));

        findViewById(R.id.profile_update_image).setOnClickListener(v -> {
            if (profileImage == null) {
                showError(errorView, "Please choose a profile image.");
                return;
            }
            AppExecutors.io().execute(() -> {
                try {
                    Map<String, String> body = new HashMap<>();
                    body.put("profileImage", profileImage);
                    JsonObject payload = ApiHelper.execute(ApiClient.getService().updateProfileImage(email, body));
                    JsonObject updated = payload.has("user") && payload.get("user").isJsonObject()
                            ? payload.getAsJsonObject("user")
                            : user;
                    session.saveCurrentUser(updated);
                    AppExecutors.runOnMain(() -> showError(errorView, "Profile image updated."));
                } catch (Exception ex) {
                    AppExecutors.runOnMain(() ->
                            showError(errorView, ex.getMessage() == null ? "Unable to update profile image." : ex.getMessage()));
                }
            });
        });

        findViewById(R.id.profile_submit_verification).setOnClickListener(v -> {
            if (verified) {
                showError(errorView, "Account already verified.");
                return;
            }

            String fullName = safeString(verifyName.getText());
            String userEmail = safeLower(verifyEmail.getText());
            String phone = safeString(((TextInputEditText) findViewById(R.id.profile_verify_phone)).getText());
            String idType = safeString(((TextInputEditText) findViewById(R.id.profile_verify_id_type)).getText());
            String idNumber = safeString(((TextInputEditText) findViewById(R.id.profile_verify_id_number)).getText());

            if (fullName.isEmpty() || userEmail.isEmpty() || phone.isEmpty() || idType.isEmpty() || idNumber.isEmpty()) {
                showError(errorView, "All verification fields are required.");
                return;
            }
            if (idPhoto == null || selfiePhoto == null) {
                showError(errorView, "Please upload both ID and selfie images.");
                return;
            }

            AppExecutors.io().execute(() -> {
                try {
                    JsonObject pending = ApiHelper.execute(ApiClient.getService().getVerifications(userEmail, "Pending"));
                    JsonArray requests = pending.has("requests") ? pending.getAsJsonArray("requests") : new JsonArray();
                    if (requests.size() > 0) {
                        AppExecutors.runOnMain(() ->
                                showError(errorView, "You already have a pending verification request."));
                        return;
                    }

                    Map<String, Object> request = new HashMap<>();
                    request.put("id", "VR-" + System.currentTimeMillis());
                    request.put("userEmail", userEmail);
                    request.put("fullName", fullName);
                    request.put("phoneNumber", phone);
                    request.put("govIdType", idType);
                    request.put("govIdNumber", idNumber);
                    request.put("govIdPhoto", idPhoto);
                    request.put("selfieWithId", selfiePhoto);

                    ApiHelper.execute(ApiClient.getService().createVerification(request));
                    AppExecutors.runOnMain(() -> showError(errorView, "Verification request submitted."));
                } catch (Exception ex) {
                    AppExecutors.runOnMain(() ->
                            showError(errorView, ex.getMessage() == null ? "Unable to submit verification request." : ex.getMessage()));
                }
            });
        });
    }

    private String safeString(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private String safeLower(CharSequence value) {
        return value == null ? "" : value.toString().trim().toLowerCase();
    }

    private void showError(TextView view, String message) {
        view.setText(message);
        view.setVisibility(View.VISIBLE);
    }
}
