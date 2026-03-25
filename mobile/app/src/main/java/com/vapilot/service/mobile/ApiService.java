package com.vapilot.service.mobile;

import com.google.gson.JsonObject;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @POST("api/auth/login")
    Call<JsonObject> login(@Body Map<String, String> body);

    @POST("api/auth/register")
    Call<JsonObject> register(@Body Map<String, String> body);

    @PATCH("api/auth/password")
    Call<JsonObject> resetPassword(@Body Map<String, String> body);

    @GET("api/users/{email}")
    Call<JsonObject> getUser(@Path("email") String email);

    @PATCH("api/users/{email}/profile-image")
    Call<JsonObject> updateProfileImage(
            @Path("email") String email,
            @Body Map<String, String> body
    );

    @GET("api/orders")
    Call<JsonObject> getOrders(@Query("customerEmail") String email);

    @GET("api/orders")
    Call<JsonObject> getAllOrders();

    @POST("api/orders")
    Call<JsonObject> createOrder(@Body Map<String, Object> body);

    @PATCH("api/orders/{id}/status")
    Call<JsonObject> updateOrderStatus(
            @Path("id") String id,
            @Body Map<String, String> body
    );

    @PATCH("api/orders/{id}/payment")
    Call<JsonObject> updateOrderPayment(
            @Path("id") String id,
            @Body Map<String, String> body
    );

    @GET("api/notifications")
    Call<JsonObject> getNotifications(@Query("userEmail") String email);

    @POST("api/feedback")
    Call<JsonObject> sendFeedback(@Body Map<String, Object> body);

    @GET("api/verifications")
    Call<JsonObject> getVerifications(
            @Query("userEmail") String email,
            @Query("status") String status
    );

    @POST("api/verifications")
    Call<JsonObject> createVerification(@Body Map<String, Object> body);

    @POST("api/transactions")
    Call<JsonObject> createTransaction(@Body Map<String, Object> body);
}
