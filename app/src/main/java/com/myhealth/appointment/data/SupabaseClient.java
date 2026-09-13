package com.myhealth.appointment.data;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class SupabaseClient {
    public interface ResultCallback {
        void onSuccess(String json);

        void onError(String message);
    }

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private SupabaseClient() {
    }

    public static void get(String tableAndQuery, ResultCallback callback) {
        enqueue(new Request.Builder().url(restUrl(tableAndQuery)).get().headers(commonHeaders()).build(), callback);
    }

    public static void post(String table, String jsonBody, ResultCallback callback) {
        Request request = new Request.Builder()
                .url(restUrl(table))
                .post(RequestBody.create(jsonBody, JSON))
                .headers(commonHeaders())
                .addHeader("Prefer", "return=representation")
                .build();
        enqueue(request, callback);
    }

    public static void patch(String tableAndQuery, String jsonBody, ResultCallback callback) {
        Request request = new Request.Builder()
                .url(restUrl(tableAndQuery))
                .patch(RequestBody.create(jsonBody, JSON))
                .headers(commonHeaders())
                .addHeader("Prefer", "return=representation")
                .build();
        enqueue(request, callback);
    }

    private static String restUrl(String tableAndQuery) {
        return SupabaseConfig.url() + "/rest/v1/" + tableAndQuery;
    }

    private static okhttp3.Headers commonHeaders() {
        String key = SupabaseConfig.anonKey();
        return new okhttp3.Headers.Builder()
                .add("apikey", key)
                .add("Authorization", "Bearer " + key)
                .add("Content-Type", "application/json")
                .add("Accept", "application/json")
                .build();
    }

    private static void enqueue(Request request, ResultCallback callback) {
        if (!SupabaseConfig.isConfigured()) {
            MAIN.post(() -> callback.onError(
                    "Supabase is not configured. Add SUPABASE_URL and SUPABASE_ANON_KEY to local.properties, then Sync Gradle."));
            return;
        }
        CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                MAIN.post(() -> callback.onError("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody body = response.body()) {
                    String text = body != null ? body.string() : "";
                    if (!response.isSuccessful()) {
                        MAIN.post(() -> callback.onError(formatHttpError(response.code(), text)));
                        return;
                    }
                    MAIN.post(() -> callback.onSuccess(text == null ? "[]" : text));
                } catch (IOException e) {
                    MAIN.post(() -> callback.onError("Could not read server response."));
                }
            }
        });
    }

    private static String formatHttpError(int code, String body) {
        if (body != null && !body.isEmpty()) {
            return "Server error " + code + ": " + body;
        }
        return "Server error " + code;
    }
}
