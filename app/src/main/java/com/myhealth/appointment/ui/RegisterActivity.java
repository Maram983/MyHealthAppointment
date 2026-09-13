package com.myhealth.appointment.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.myhealth.appointment.data.AppUtils;
import com.myhealth.appointment.data.SessionManager;
import com.myhealth.appointment.data.SupabaseClient;
import com.myhealth.appointment.data.models.User;
import com.myhealth.appointment.databinding.ActivityRegisterBinding;

import java.util.Calendar;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private final Gson gson = new Gson();
    private String selectedDob = "";
    private boolean passwordVisible;
    private boolean confirmPasswordVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.inputDob.setOnClickListener(v -> showDatePicker());
        binding.btnTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            togglePassword(binding.inputPassword, passwordVisible);
        });
        binding.btnToggleConfirm.setOnClickListener(v -> {
            confirmPasswordVisible = !confirmPasswordVisible;
            togglePassword(binding.inputConfirmPassword, confirmPasswordVisible);
        });
        binding.btnCreateAccount.setOnClickListener(v -> submit());
        binding.linkLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void togglePassword(android.widget.EditText field, boolean visible) {
        int start = field.getSelectionStart();
        field.setInputType(visible
                ? android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        field.setSelection(Math.max(start, 0));
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -20);
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDob = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            binding.inputDob.setText(selectedDob);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private void submit() {
        String name = text(binding.inputName);
        String phone = text(binding.inputPhone);
        String email = text(binding.inputEmail).toLowerCase(Locale.US);
        String password = text(binding.inputPassword);
        String confirm = text(binding.inputConfirmPassword);

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(email)
                || TextUtils.isEmpty(selectedDob) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirm)) {
            toast("Please fill in all fields.");
            return;
        }
        if (!AppUtils.isValidEmail(email)) {
            toast("Please enter a valid email address.");
            return;
        }
        if (phone.replaceAll("\\D", "").length() < 8) {
            toast("Please enter a valid phone number.");
            return;
        }
        if (password.length() < 6) {
            toast("Password must be at least 6 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            toast("Passwords do not match.");
            return;
        }
        if (!binding.checkTerms.isChecked()) {
            toast("Please agree to the terms to continue.");
            return;
        }

        setLoading(true);
        SupabaseClient.get("users?email=eq." + AppUtils.encode(email) + "&select=id", new SupabaseClient.ResultCallback() {
            @Override
            public void onSuccess(String json) {
                if (json != null && json.trim().length() > 2) {
                    setLoading(false);
                    toast("This email is already registered. Please log in.");
                    return;
                }
                createUser(name, phone, email, password);
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                toast(message);
            }
        });
    }

    private void createUser(String name, String phone, String email, String password) {
        JsonObject body = new JsonObject();
        body.addProperty("full_name", name);
        body.addProperty("phone", phone);
        body.addProperty("email", email);
        body.addProperty("date_of_birth", selectedDob);
        body.addProperty("password", password);
        body.addProperty("role", "patient");

        SupabaseClient.post("users", body.toString(), new SupabaseClient.ResultCallback() {
            @Override
            public void onSuccess(String json) {
                setLoading(false);
                User[] created = gson.fromJson(json, User[].class);
                if (created == null || created.length == 0) {
                    toast("Account created. Please log in.");
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                    return;
                }
                User user = created[0];
                new SessionManager(RegisterActivity.this)
                        .saveUser(user.id, user.fullName, user.email, user.phone);
                toast("Registration successful.");
                Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
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
        binding.btnCreateAccount.setEnabled(!loading);
    }

    private String text(android.widget.EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
