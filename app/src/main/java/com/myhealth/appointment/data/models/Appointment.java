package com.myhealth.appointment.data.models;

import com.google.gson.annotations.SerializedName;

public class Appointment {
    public long id;
    @SerializedName("user_id")
    public long userId;
    @SerializedName("doctor_id")
    public long doctorId;
    @SerializedName("slot_id")
    public long slotId;
    public String reason;
    public String status;
    public Doctor doctors;
    @SerializedName("time_slots")
    public TimeSlot timeSlots;
}
