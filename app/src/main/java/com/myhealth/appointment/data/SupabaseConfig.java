package com.myhealth.appointment.data;

import android.text.TextUtils;

import com.myhealth.appointment.BuildConfig;

public final class SupabaseConfig {
    private SupabaseConfig() {
    }

    public static String url() {
        String value = BuildConfig.SUPABASE_URL;
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("/+$", "");
    }

    public static String anonKey() {
        String value = BuildConfig.SUPABASE_ANON_KEY;
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    public static boolean isConfigured() {
        String configuredUrl = url();
        String configuredKey = anonKey();
        return !TextUtils.isEmpty(configuredUrl)
                && configuredUrl.startsWith("http")
                && !configuredUrl.contains("YOUR_PROJECT")
                && !TextUtils.isEmpty(configuredKey)
                && !configuredKey.contains("YOUR_ANON");
    }
}
