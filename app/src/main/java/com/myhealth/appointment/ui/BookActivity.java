package com.myhealth.appointment.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.myhealth.appointment.R;
import com.myhealth.appointment.data.AppUtils;
import com.myhealth.appointment.data.SessionManager;
import com.myhealth.appointment.data.SupabaseClient;
import com.myhealth.appointment.data.models.Doctor;
import com.myhealth.appointment.data.models.TimeSlot;
import com.myhealth.appointment.databinding.ActivityBookBinding;
import com.myhealth.appointment.databinding.ItemDoctorBinding;
import com.myhealth.appointment.databinding.ItemSlotBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BookActivity extends AppCompatActivity {
    public static final String EXTRA_DEPARTMENT_ID = "department_id";
    public static final String EXTRA_DEPARTMENT_NAME = "department_name";

    private ActivityBookBinding binding;
    private final Gson gson = new Gson();
    private final List<Doctor> doctors = new ArrayList<>();
    private final List<TimeSlot> slots = new ArrayList<>();
    private DoctorAdapter doctorAdapter;
    private SlotAdapter slotAdapter;
    private Doctor selectedDoctor;
    private TimeSlot selectedSlot;
    private long departmentId;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);
        binding = ActivityBookBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        departmentId = getIntent().getLongExtra(EXTRA_DEPARTMENT_ID, -1);
        String departmentName = getIntent().getStringExtra(EXTRA_DEPARTMENT_NAME);
        binding.textTitle.setText(departmentName == null ? "Book Appointment" : departmentName);
        binding.btnBack.setOnClickListener(v -> finish());

        doctorAdapter = new DoctorAdapter();
        binding.recyclerDoctors.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerDoctors.setAdapter(doctorAdapter);

        slotAdapter = new SlotAdapter();
        binding.recyclerSlots.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerSlots.setAdapter(slotAdapter);

        binding.btnConfirm.setOnClickListener(v -> confirmBooking());
        loadDoctors();
    }

    private void loadDoctors() {
        binding.progress.setVisibility(View.VISIBLE);
        SupabaseClient.get("doctors?department_id=eq." + departmentId + "&select=*&order=id.asc",
                new SupabaseClient.ResultCallback() {
                    @Override
                    public void onSuccess(String json) {
                        binding.progress.setVisibility(View.GONE);
                        Doctor[] items = gson.fromJson(json, Doctor[].class);
                        doctors.clear();
                        if (items != null) {
                            doctors.addAll(Arrays.asList(items));
                        }
                        doctorAdapter.notifyDataSetChanged();
                        if (doctors.isEmpty()) {
                            toast("No doctors found for this department. Run supabase/schema.sql.");
                        }
                    }

                    @Override
                    public void onError(String message) {
                        binding.progress.setVisibility(View.GONE);
                        toast(message);
                    }
                });
    }

    private void selectDoctor(Doctor doctor) {
        selectedDoctor = doctor;
        selectedSlot = null;
        doctorAdapter.notifyDataSetChanged();
        binding.slotsSection.setVisibility(View.VISIBLE);
        binding.confirmSection.setVisibility(View.VISIBLE);
        binding.textSelectedDoctor.setText("Available appointments for " + doctor.name);
        loadSlots(doctor.id);
    }

    private void loadSlots(long doctorId) {
        binding.progress.setVisibility(View.VISIBLE);
        String query = "time_slots?doctor_id=eq." + doctorId
                + "&is_booked=eq.false"
                + "&slot_date=gte." + java.time.LocalDate.now()
                + "&select=*"
                + "&order=slot_date.asc,slot_time.asc";
        SupabaseClient.get(query, new SupabaseClient.ResultCallback() {
            @Override
            public void onSuccess(String json) {
                binding.progress.setVisibility(View.GONE);
                TimeSlot[] items = gson.fromJson(json, TimeSlot[].class);
                slots.clear();
                if (items != null) {
                    slots.addAll(Arrays.asList(items));
                }
                slotAdapter.notifyDataSetChanged();
                binding.textNoSlots.setVisibility(slots.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                binding.progress.setVisibility(View.GONE);
                toast(message);
            }
        });
    }

    private void confirmBooking() {
        if (selectedDoctor == null) {
            toast("Please select a doctor.");
            return;
        }
        if (selectedSlot == null) {
            toast("Please select an available appointment time.");
            return;
        }
        String reason = binding.inputReason.getText() == null ? "" : binding.inputReason.getText().toString().trim();
        if (TextUtils.isEmpty(reason)) {
            toast("Please enter the reason for your visit.");
            return;
        }

        binding.progress.setVisibility(View.VISIBLE);
        binding.btnConfirm.setEnabled(false);
        String slotQuery = "time_slots?id=eq." + selectedSlot.id + "&is_booked=eq.false&select=*";
        SupabaseClient.get(slotQuery, new SupabaseClient.ResultCallback() {
            @Override
            public void onSuccess(String json) {
                TimeSlot[] open = gson.fromJson(json, TimeSlot[].class);
                if (open == null || open.length == 0) {
                    binding.progress.setVisibility(View.GONE);
                    binding.btnConfirm.setEnabled(true);
                    toast("That time was just booked. Please choose another slot.");
                    loadSlots(selectedDoctor.id);
                    return;
                }
                markSlotBooked(reason);
            }

            @Override
            public void onError(String message) {
                binding.progress.setVisibility(View.GONE);
                binding.btnConfirm.setEnabled(true);
                toast(message);
            }
        });
    }

    private void markSlotBooked(String reason) {
        JsonObject body = new JsonObject();
        body.addProperty("is_booked", true);
        SupabaseClient.patch("time_slots?id=eq." + selectedSlot.id, body.toString(),
                new SupabaseClient.ResultCallback() {
                    @Override
                    public void onSuccess(String json) {
                        insertAppointment(reason);
                    }

                    @Override
                    public void onError(String message) {
                        binding.progress.setVisibility(View.GONE);
                        binding.btnConfirm.setEnabled(true);
                        toast(message);
                    }
                });
    }

    private void insertAppointment(String reason) {
        JsonObject body = new JsonObject();
        body.addProperty("user_id", session.getUserId());
        body.addProperty("doctor_id", selectedDoctor.id);
        body.addProperty("slot_id", selectedSlot.id);
        body.addProperty("reason", reason);
        body.addProperty("status", "confirmed");
        SupabaseClient.post("appointments", body.toString(), new SupabaseClient.ResultCallback() {
            @Override
            public void onSuccess(String json) {
                binding.progress.setVisibility(View.GONE);
                toast("Appointment confirmed with " + selectedDoctor.name + ".");
                Intent intent = new Intent(BookActivity.this, AppointmentsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                JsonObject revert = new JsonObject();
                revert.addProperty("is_booked", false);
                SupabaseClient.patch("time_slots?id=eq." + selectedSlot.id, revert.toString(),
                        new SupabaseClient.ResultCallback() {
                            @Override
                            public void onSuccess(String json) {
                            }

                            @Override
                            public void onError(String ignored) {
                            }
                        });
                binding.progress.setVisibility(View.GONE);
                binding.btnConfirm.setEnabled(true);
                toast(message);
            }
        });
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private class DoctorAdapter extends RecyclerView.Adapter<DoctorAdapter.Holder> {
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemDoctorBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Doctor doctor = doctors.get(position);
            holder.binding.textName.setText(doctor.name);
            holder.binding.textMeta.setText(doctor.room + "  •  " + doctor.rating + " rating");
            holder.binding.textInitials.setText(AppUtils.initials(doctor.name));
            boolean selected = selectedDoctor != null && selectedDoctor.id == doctor.id;
            holder.binding.doctorCard.setBackgroundResource(
                    selected ? R.drawable.bg_selected_card : R.drawable.bg_logo_card);
            holder.binding.textName.setTextColor(ContextCompat.getColor(BookActivity.this,
                    selected ? R.color.white : R.color.text_primary));
            holder.binding.textMeta.setTextColor(ContextCompat.getColor(BookActivity.this,
                    selected ? R.color.white : R.color.text_muted));
            holder.binding.getRoot().setOnClickListener(v -> selectDoctor(doctor));
        }

        @Override
        public int getItemCount() {
            return doctors.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final ItemDoctorBinding binding;

            Holder(ItemDoctorBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    private class SlotAdapter extends RecyclerView.Adapter<SlotAdapter.Holder> {
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemSlotBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            TimeSlot slot = slots.get(position);
            holder.binding.textSlot.setText(
                    AppUtils.formatDate(slot.slotDate) + "  •  " + AppUtils.formatTime(slot.slotTime));
            boolean selected = selectedSlot != null && selectedSlot.id == slot.id;
            holder.binding.textSlot.setBackgroundColor(ContextCompat.getColor(BookActivity.this,
                    selected ? R.color.teal_primary : R.color.white));
            holder.binding.textSlot.setTextColor(ContextCompat.getColor(BookActivity.this,
                    selected ? R.color.white : R.color.text_primary));
            holder.binding.textSlot.setOnClickListener(v -> {
                selectedSlot = slot;
                notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() {
            return slots.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final ItemSlotBinding binding;

            Holder(ItemSlotBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
