package com.vapilot.service.mobile;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;

import java.io.InputStream;

public final class ImageUtils {
    private ImageUtils() {
    }

    public static String uriToBase64(Context context, Uri uri) throws Exception {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new Exception("Unable to read image.");
        }
        byte[] bytes;
        try {
            bytes = inputStream.readAllBytes();
        } finally {
            inputStream.close();
        }
        String encoded = Base64.encodeToString(bytes, Base64.NO_WRAP);
        return "data:image/*;base64," + encoded;
    }
}
