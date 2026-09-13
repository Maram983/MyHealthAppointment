package com.myhealth.appointment.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.myhealth.appointment.data.SessionManager;
import com.myhealth.appointment.data.SupabaseConfig;
import com.myhealth.appointment.databinding.ActivityWelcomeBinding;

public class WelcomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager session = new SessionManager(this);
        if (session.isLoggedIn()) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        ActivityWelcomeBinding binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!SupabaseConfig.isConfigured()) {
            Toast.makeText(this,
                    "Add SUPABASE_URL and SUPABASE_ANON_KEY to local.properties, then Sync Project with Gradle.",
                    Toast.LENGTH_LONG).show();
        }

        binding.btnLogin.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
        binding.btnRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }
}
