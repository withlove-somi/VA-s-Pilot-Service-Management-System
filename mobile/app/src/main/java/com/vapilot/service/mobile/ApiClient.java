package com.vapilot.service.mobile;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {
    public static final String BASE_URL = "https://va-s-pilot-service-management-system-production-d6a0.up.railway.app/";

    private static ApiService service;

    private ApiClient() {
    }

    public static ApiService getService() {
        if (service == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            service = retrofit.create(ApiService.class);
        }
        return service;
    }
}
