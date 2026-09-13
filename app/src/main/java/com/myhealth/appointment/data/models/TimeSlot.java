package com.myhealth.appointment.data.models;

import com.google.gson.annotations.SerializedName;

public class TimeSlot {
    public long id;
    @SerializedName("doctor_id")
    public long doctorId;
    @SerializedName("slot_date")
    public String slotDate;
    @SerializedName("slot_time")
    public String slotTime;
    @SerializedName("is_booked")
    public boolean isBooked;
}
