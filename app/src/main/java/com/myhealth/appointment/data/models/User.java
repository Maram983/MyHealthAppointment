package com.myhealth.appointment.data.models;

import com.google.gson.annotations.SerializedName;

public class User {
    public long id;
    @SerializedName("full_name")
    public String fullName;
    public String phone;
    public String email;
    @SerializedName("date_of_birth")
    public String dateOfBirth;
    public String password;
    public String role;
}
