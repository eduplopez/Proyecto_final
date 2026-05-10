package com.example.frontend;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient {
    private static final String BASE_URL = "http://10.0.2.2:8000/api/";
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final SharedPreferences prefs;

    public ApiClient(Context context) {
        prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
    }

    public void saveToken(String token) {
        prefs.edit().putString("auth_token", token).apply();
    }

    public String getToken() {
        return prefs.getString("auth_token", null);
    }

    public void clearToken() {
        prefs.edit().remove("auth_token").apply();
    }

    public void post(String endpoint, Object body, Callback callback) {
        String json = gson.toJson(body);
        RequestBody reqBody = RequestBody.create(json, JSON);
        Request.Builder requestBuilder = new Request.Builder()
                .url(BASE_URL + endpoint)
                .post(reqBody);

        String token = getToken();
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer " + token);
        }

        client.newCall(requestBuilder.build()).enqueue(callback);
    }

    public void get(String endpoint, Callback callback) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(BASE_URL + endpoint)
                .get();

        String token = getToken();
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer " + token);
        }

        client.newCall(requestBuilder.build()).enqueue(callback);
    }

    public void put(String endpoint, Object body, Callback callback) {
        String json = gson.toJson(body);
        RequestBody reqBody = RequestBody.create(json, JSON);
        Request.Builder requestBuilder = new Request.Builder()
                .url(BASE_URL + endpoint)
                .put(reqBody);

        String token = getToken();
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer " + token);
        }

        client.newCall(requestBuilder.build()).enqueue(callback);
    }

    public void delete(String endpoint, Callback callback) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(BASE_URL + endpoint)
                .delete();

        String token = getToken();
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer " + token);
        }

        client.newCall(requestBuilder.build()).enqueue(callback);
    }
}
