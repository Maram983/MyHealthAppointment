package com.myhealth.appointment.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.myhealth.appointment.data.AppUtils;
import com.myhealth.appointment.data.SessionManager;
import com.myhealth.appointment.data.SupabaseClient;
import com.myhealth.appointment.data.models.Appointment;
import com.myhealth.appointment.databinding.ActivityHomeBinding;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Comparator;

public class HomeActivity extends AppCompatActivity {
    private ActivityHomeBinding binding;
    private SessionManager session;
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.textGreetingLabel.setText(AppUtils.greetingForNow());
        binding.textPatientName.setText(session.getFullName());
        binding.textAvatar.setText(session.getInitials());

        binding.cardBook.setOnClickListener(v ->
                startActivity(new Intent(this, DepartmentsActivity.class)));
        binding.cardMyAppointments.setOnClickListener(v ->
                startActivity(new Intent(this, AppointmentsActivity.class)));
        binding.textAvatar.setOnClickListener(v -> confirmLogout());
        binding.btnLogout.setOnClickListener(v -> confirmLogout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNextAppointment();
    }

    private void loadNextAppointment() {
        binding.nextEmpty.setVisibility(View.VISIBLE);
        binding.nextCard.setVisibility(View.GONE);
        String query = "appointments?user_id=eq." + session.getUserId()
                + "&status=neq.cancelled"
                + "&select=*,doctors(*),time_slots(*)"
                + "&order=id.desc";
        SupabaseClient.get(query, new SupabaseClient.ResultCallback() {
            @Override
            public void onSuccess(String json) {
                Appointment[] items = gson.fromJson(json, Appointment[].class);
                Appointment next = pickUpcoming(items);
                if (next == null || next.doctors == null || next.timeSlots == null) {
                    binding.nextEmpty.setVisibility(View.VISIBLE);
                    binding.nextCard.setVisibility(View.GONE);
                    return;
                }
                binding.nextEmpty.setVisibility(View.GONE);
                binding.nextCard.setVisibility(View.VISIBLE);
                binding.nextDoctorName.setText(next.doctors.name);
                binding.nextDoctorMeta.setText(next.doctors.room);
                binding.nextStatus.setText(capitalize(next.status));
                binding.nextDate.setText(AppUtils.formatDate(next.timeSlots.slotDate));
                binding.nextTime.setText(AppUtils.formatTime(next.timeSlots.slotTime));
                binding.nextLocation.setText(next.doctors.room);
                binding.nextDoctorInitials.setText(AppUtils.initials(next.doctors.name));
            }

            @Override
            public void onError(String message) {
                binding.nextEmpty.setText("Could not load appointments.\n" + message);
                binding.nextEmpty.setVisibility(View.VISIBLE);
                binding.nextCard.setVisibility(View.GONE);
            }
        });
    }

    private Appointment pickUpcoming(Appointment[] items) {
        if (items == null || items.length == 0) {
            return null;
        }
        return Arrays.stream(items)
                .filter(item -> item.timeSlots != null && item.timeSlots.slotDate != null)
                .filter(item -> !isPast(item))
                .min(Comparator.comparing(item -> dateTime(item)))
                .orElse(null);
    }

    private boolean isPast(Appointment item) {
        try {
            return dateTime(item).isBefore(LocalDateTime.now());
        } catch (Exception ignored) {
            return false;
        }
    }

    private LocalDateTime dateTime(Appointment item) {
        LocalDate date = LocalDate.parse(item.timeSlots.slotDate.substring(0, 10));
        String rawTime = item.timeSlots.slotTime;
        LocalTime time = LocalTime.parse(rawTime.length() >= 8 ? rawTime.substring(0, 8) : rawTime);
        return LocalDateTime.of(date, time);
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "Confirmed";
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Log out")
                .setMessage("Do you want to log out of MyHealth Appointment?")
                .setPositiveButton("Log out", (dialog, which) -> {
                    session.logout();
                    Intent intent = new Intent(this, WelcomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
