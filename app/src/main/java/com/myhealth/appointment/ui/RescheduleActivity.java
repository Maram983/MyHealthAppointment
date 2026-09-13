package com.myhealth.appointment.ui;

import android.os.Bundle;
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
import com.myhealth.appointment.data.SupabaseClient;
import com.myhealth.appointment.data.models.TimeSlot;
import com.myhealth.appointment.databinding.ActivityRescheduleBinding;
import com.myhealth.appointment.databinding.ItemSlotBinding;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RescheduleActivity extends AppCompatActivity {
    public static final String EXTRA_APPOINTMENT_ID = "appointment_id";
    public static final String EXTRA_DOCTOR_ID = "doctor_id";
    public static final String EXTRA_OLD_SLOT_ID = "old_slot_id";
    public static final String EXTRA_DOCTOR_NAME = "doctor_name";

    private ActivityRescheduleBinding binding;
    private final Gson gson = new Gson();
    private final List<TimeSlot> slots = new ArrayList<>();
    private SlotAdapter adapter;
    private TimeSlot selectedSlot;
    private long appointmentId;
    private long doctorId;
    private long oldSlotId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRescheduleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        appointmentId = getIntent().getLongExtra(EXTRA_APPOINTMENT_ID, -1);
        doctorId = getIntent().getLongExtra(EXTRA_DOCTOR_ID, -1);
        oldSlotId = getIntent().getLongExtra(EXTRA_OLD_SLOT_ID, -1);
        String doctorName = getIntent().getStringExtra(EXTRA_DOCTOR_NAME);
        binding.textTitle.setText("Reschedule");
        binding.textSubtitle.setText(doctorName == null
                ? "Pick a new available time"
                : "New time with " + doctorName);

        binding.btnBack.setOnClickListener(v -> finish());
        adapter = new SlotAdapter();
        binding.recyclerSlots.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerSlots.setAdapter(adapter);
        binding.btnSave.setOnClickListener(v -> save());
        loadSlots();
    }

    private void loadSlots() {
        binding.progress.setVisibility(View.VISIBLE);
        String query = "time_slots?doctor_id=eq." + doctorId
                + "&is_booked=eq.false"
                + "&slot_date=gte." + LocalDate.now()
                + "&select=*&order=slot_date.asc,slot_time.asc";
        SupabaseClient.get(query, new SupabaseClient.ResultCallback() {
            @Override
            public void onSuccess(String json) {
                binding.progress.setVisibility(View.GONE);
                TimeSlot[] items = gson.fromJson(json, TimeSlot[].class);
                slots.clear();
                if (items != null) {
                    slots.addAll(Arrays.asList(items));
                }
                adapter.notifyDataSetChanged();
                binding.empty.setVisibility(slots.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                binding.progress.setVisibility(View.GONE);
                Toast.makeText(RescheduleActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void save() {
        if (selectedSlot == null) {
            Toast.makeText(this, "Please select a new appointment time.", Toast.LENGTH_SHORT).show();
            return;
        }
        binding.progress.setVisibility(View.VISIBLE);
        binding.btnSave.setEnabled(false);

        String check = "time_slots?id=eq." + selectedSlot.id + "&is_booked=eq.false&select=*";
        SupabaseClient.get(check, new SupabaseClient.ResultCallback() {
            @Override
            public void onSuccess(String json) {
                TimeSlot[] open = gson.fromJson(json, TimeSlot[].class);
                if (open == null || open.length == 0) {
                    binding.progress.setVisibility(View.GONE);
                    binding.btnSave.setEnabled(true);
                    Toast.makeText(RescheduleActivity.this,
                            "That time was just booked. Please choose another slot.",
                            Toast.LENGTH_LONG).show();
                    loadSlots();
                    return;
                }
                bookNewSlot();
            }

            @Override
            public void onError(String message) {
                fail(message);
            }
        });
    }

    private void bookNewSlot() {
        JsonObject booked = new JsonObject();
        booked.addProperty("is_booked", true);
        SupabaseClient.patch("time_slots?id=eq." + selectedSlot.id, booked.toString(),
                new SupabaseClient.ResultCallback() {
                    @Override
                    public void onSuccess(String json) {
                        updateAppointment();
                    }

                    @Override
                    public void onError(String message) {
                        fail(message);
                    }
                });
    }

    private void updateAppointment() {
        JsonObject body = new JsonObject();
        body.addProperty("slot_id", selectedSlot.id);
        body.addProperty("status", "confirmed");
        SupabaseClient.patch("appointments?id=eq." + appointmentId, body.toString(),
                new SupabaseClient.ResultCallback() {
                    @Override
                    public void onSuccess(String json) {
                        JsonObject freeOld = new JsonObject();
                        freeOld.addProperty("is_booked", false);
                        SupabaseClient.patch("time_slots?id=eq." + oldSlotId, freeOld.toString(),
                                new SupabaseClient.ResultCallback() {
                                    @Override
                                    public void onSuccess(String ignored) {
                                        binding.progress.setVisibility(View.GONE);
                                        Toast.makeText(RescheduleActivity.this,
                                                "Appointment rescheduled.", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }

                                    @Override
                                    public void onError(String message) {
                                        fail(message);
                                    }
                                });
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
                        fail(message);
                    }
                });
    }

    private void fail(String message) {
        binding.progress.setVisibility(View.GONE);
        binding.btnSave.setEnabled(true);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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
            holder.binding.textSlot.setBackgroundColor(ContextCompat.getColor(RescheduleActivity.this,
                    selected ? R.color.teal_primary : R.color.white));
            holder.binding.textSlot.setTextColor(ContextCompat.getColor(RescheduleActivity.this,
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
