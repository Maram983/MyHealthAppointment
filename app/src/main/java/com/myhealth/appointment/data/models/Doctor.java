package com.myhealth.appointment.data.models;

import com.google.gson.annotations.SerializedName;

public class Doctor {
    public long id;
    public String name;
    @SerializedName("department_id")
    public long departmentId;
    public String room;
    public double rating;
}
