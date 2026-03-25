package com.vapilot.service.mobile;

import com.google.gson.JsonObject;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

public final class ApiHelper {
    private ApiHelper() {
    }

    public static JsonObject execute(Call<JsonObject> call) throws Exception {
        Response<JsonObject> response = call.execute();
        if (!response.isSuccessful()) {
            String message = response.errorBody() != null ? response.errorBody().string() : response.message();
            if (message == null || message.trim().isEmpty()) {
                message = "Request failed";
            }
            throw new Exception(message);
        }
        JsonObject body = response.body();
        if (body == null) {
            throw new IOException("Empty response");
        }
        return body;
    }
}
