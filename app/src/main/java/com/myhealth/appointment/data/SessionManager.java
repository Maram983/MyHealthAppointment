package com.myhealth.appointment.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREFS = "myhealth_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_NAME = "full_name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void saveUser(long userId, String fullName, String email, String phone) {
        prefs.edit()
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_NAME, fullName)
                .putString(KEY_EMAIL, email)
                .putString(KEY_PHONE, phone)
                .apply();
    }

    public boolean isLoggedIn() {
        return prefs.getLong(KEY_USER_ID, -1) > 0;
    }

    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1);
    }

    public String getFullName() {
        return prefs.getString(KEY_NAME, "");
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public String getPhone() {
        return prefs.getString(KEY_PHONE, "");
    }

    public String getInitials() {
        String name = getFullName();
        if (name == null || name.trim().isEmpty()) {
            return "P";
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        initials.append(Character.toUpperCase(parts[0].charAt(0)));
        if (parts.length > 1 && !parts[parts.length - 1].isEmpty()) {
            initials.append(Character.toUpperCase(parts[parts.length - 1].charAt(0)));
        }
        return initials.toString();
    }

    public void logout() {
        prefs.edit().clear().apply();
    }
}
