package com.myhealth.appointment.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.myhealth.appointment.data.SupabaseClient;
import com.myhealth.appointment.data.models.Department;
import com.myhealth.appointment.databinding.ActivityDepartmentsBinding;
import com.myhealth.appointment.databinding.ItemDepartmentBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DepartmentsActivity extends AppCompatActivity {
    private ActivityDepartmentsBinding binding;
    private final Gson gson = new Gson();
    private final List<Department> departments = new ArrayList<>();
    private DepartmentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDepartmentsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        adapter = new DepartmentAdapter();
        binding.recycler.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recycler.setAdapter(adapter);
        loadDepartments();
    }

    private void loadDepartments() {
        binding.progress.setVisibility(View.VISIBLE);
        SupabaseClient.get("departments?select=*&order=id.asc", new SupabaseClient.ResultCallback() {
            @Override
            public void onSuccess(String json) {
                binding.progress.setVisibility(View.GONE);
                Department[] items = gson.fromJson(json, Department[].class);
                departments.clear();
                if (items != null) {
                    departments.addAll(Arrays.asList(items));
                }
                adapter.notifyDataSetChanged();
                if (departments.isEmpty()) {
                    Toast.makeText(DepartmentsActivity.this,
                            "No departments found. Run supabase/schema.sql in the SQL Editor.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String message) {
                binding.progress.setVisibility(View.GONE);
                Toast.makeText(DepartmentsActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private class DepartmentAdapter extends RecyclerView.Adapter<DepartmentAdapter.Holder> {
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemDepartmentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Department department = departments.get(position);
            holder.binding.textName.setText(department.name);
            holder.binding.getRoot().setOnClickListener(v -> {
                Intent intent = new Intent(DepartmentsActivity.this, BookActivity.class);
                intent.putExtra(BookActivity.EXTRA_DEPARTMENT_ID, department.id);
                intent.putExtra(BookActivity.EXTRA_DEPARTMENT_NAME, department.name);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return departments.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final ItemDepartmentBinding binding;

            Holder(ItemDepartmentBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
