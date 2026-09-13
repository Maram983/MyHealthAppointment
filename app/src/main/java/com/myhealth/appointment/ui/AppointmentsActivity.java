package com.myhealth.appointment.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.myhealth.appointment.data.AppUtils;
import com.myhealth.appointment.data.SessionManager;
import com.myhealth.appointment.data.SupabaseClient;
import com.myhealth.appointment.data.models.Appointment;
import com.myhealth.appointment.databinding.ActivityAppointmentsBinding;
import com.myhealth.appointment.databinding.ItemAppointmentBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AppointmentsActivity extends AppCompatActivity {
    private ActivityAppointmentsBinding binding;
    private final Gson gson = new Gson();
    private final List<Appointment> appointments = new ArrayList<>();
    private AppointmentAdapter adapter;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);
        binding = ActivityAppointmentsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        adapter = new AppointmentAdapter();
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        binding.recycler.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAppointments();
    }

    private void loadAppointments() {
        binding.progress.setVisibility(View.VISIBLE);
        String query = "appointments?user_id=eq." + session.getUserId()
                + "&select=*,doctors(*),time_slots(*)"
                + "&order=id.desc";
        SupabaseClient.get(query, new SupabaseClient.ResultCallback() {
            @Override
            public void onSuccess(String json) {
                binding.progress.setVisibility(View.GONE);
                Appointment[] items = gson.fromJson(json, Appointment[].class);
                appointments.clear();
                if (items != null) {
                    appointments.addAll(Arrays.asList(items));
                }
                adapter.notifyDataSetChanged();
                binding.empty.setVisibility(appointments.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                binding.progress.setVisibility(View.GONE);
                Toast.makeText(AppointmentsActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void cancelAppointment(Appointment appointment) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel appointment")
                .setMessage("Cancel this appointment with "
                        + (appointment.doctors == null ? "the doctor" : appointment.doctors.name) + "?")
                .setPositiveButton("Cancel appointment", (dialog, which) -> performCancel(appointment))
                .setNegativeButton("Keep it", null)
                .show();
    }

    private void performCancel(Appointment appointment) {
        binding.progress.setVisibility(View.VISIBLE);
        JsonObject status = new JsonObject();
        status.addProperty("status", "cancelled");
        SupabaseClient.patch("appointments?id=eq." + appointment.id, status.toString(),
                new SupabaseClient.ResultCallback() {
                    @Override
                    public void onSuccess(String json) {
                        JsonObject slot = new JsonObject();
                        slot.addProperty("is_booked", false);
                        SupabaseClient.patch("time_slots?id=eq." + appointment.slotId, slot.toString(),
                                new SupabaseClient.ResultCallback() {
                                    @Override
                                    public void onSuccess(String ignored) {
                                        binding.progress.setVisibility(View.GONE);
                                        Toast.makeText(AppointmentsActivity.this,
                                                "Appointment cancelled.", Toast.LENGTH_SHORT).show();
                                        loadAppointments();
                                    }

                                    @Override
                                    public void onError(String message) {
                                        binding.progress.setVisibility(View.GONE);
                                        Toast.makeText(AppointmentsActivity.this, message, Toast.LENGTH_LONG).show();
                                        loadAppointments();
                                    }
                                });
                    }

                    @Override
                    public void onError(String message) {
                        binding.progress.setVisibility(View.GONE);
                        Toast.makeText(AppointmentsActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.Holder> {
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemAppointmentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Appointment item = appointments.get(position);
            String doctorName = item.doctors == null ? "Doctor" : item.doctors.name;
            String room = item.doctors == null || item.doctors.room == null ? "" : item.doctors.room;
            holder.binding.textDoctor.setText(doctorName);
            holder.binding.textRoom.setText(room);
            holder.binding.textStatus.setText(item.status == null ? "" : item.status);
            if (item.timeSlots != null) {
                holder.binding.textWhen.setText(
                        AppUtils.formatDate(item.timeSlots.slotDate) + "  •  "
                                + AppUtils.formatTime(item.timeSlots.slotTime));
            } else {
                holder.binding.textWhen.setText("Time unavailable");
            }
            holder.binding.textReason.setText(item.reason == null || item.reason.isEmpty()
                    ? "No reason recorded" : item.reason);

            boolean active = item.status != null && !"cancelled".equalsIgnoreCase(item.status);
            holder.binding.btnReschedule.setEnabled(active);
            holder.binding.btnCancel.setEnabled(active);
            holder.binding.btnReschedule.setAlpha(active ? 1f : 0.4f);
            holder.binding.btnCancel.setAlpha(active ? 1f : 0.4f);

            holder.binding.btnCancel.setOnClickListener(v -> {
                if (active) {
                    cancelAppointment(item);
                }
            });
            holder.binding.btnReschedule.setOnClickListener(v -> {
                if (!active) {
                    return;
                }
                Intent intent = new Intent(AppointmentsActivity.this, RescheduleActivity.class);
                intent.putExtra(RescheduleActivity.EXTRA_APPOINTMENT_ID, item.id);
                intent.putExtra(RescheduleActivity.EXTRA_DOCTOR_ID, item.doctorId);
                intent.putExtra(RescheduleActivity.EXTRA_OLD_SLOT_ID, item.slotId);
                intent.putExtra(RescheduleActivity.EXTRA_DOCTOR_NAME, doctorName);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return appointments.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final ItemAppointmentBinding binding;

            Holder(ItemAppointmentBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
