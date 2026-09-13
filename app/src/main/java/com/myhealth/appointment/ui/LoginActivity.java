package com.myhealth.appointment.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.myhealth.appointment.data.AppUtils;
import com.myhealth.appointment.data.SessionManager;
import com.myhealth.appointment.data.SupabaseClient;
import com.myhealth.appointment.data.models.User;
import com.myhealth.appointment.databinding.ActivityLoginBinding;

import java.util.Locale;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private final Gson gson = new Gson();
    private boolean passwordVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            int start = binding.inputPassword.getSelectionStart();
            binding.inputPassword.setInputType(passwordVisible
                    ? android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            binding.inputPassword.setSelection(Math.max(start, 0));
        });
        binding.btnLogin.setOnClickListener(v -> submit());
        binding.linkRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });
    }

    private void submit() {
        String email = text(binding.inputEmail).toLowerCase(Locale.US);
        String password = text(binding.inputPassword);

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            toast("Please enter your email and password.");
            return;
        }
        if (!AppUtils.isValidEmail(email)) {
            toast("Please enter a valid email address.");
            return;
        }

        setLoading(true);
        String query = "users?email=eq." + AppUtils.encode(email)
                + "&password=eq." + AppUtils.encode(password)
                + "&select=*";
        SupabaseClient.get(query, new SupabaseClient.ResultCallback() {
            @Override
            public void onSuccess(String json) {
                setLoading(false);
                User[] users = gson.fromJson(json, User[].class);
                if (users == null || users.length == 0) {
                    toast("Incorrect email or password.");
                    return;
                }
                User user = users[0];
                new SessionManager(LoginActivity.this)
                        .saveUser(user.id, user.fullName, user.email, user.phone);
                toast("Login successful.");
                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                toast(message);
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
    }

    private String text(android.widget.EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
