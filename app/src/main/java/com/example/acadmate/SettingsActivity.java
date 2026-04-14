package com.example.acadmate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle("Settings");
        UiEffects.applyInteractiveAnimations(findViewById(android.R.id.content));

        EditText etTeacherName = findViewById(R.id.etTeacherName);
        EditText etPhone = findViewById(R.id.etPhoneNumber);
        EditText etTeacherPin = findViewById(R.id.etTeacherPin);
        CheckBox cbAssignmentNotifs = findViewById(R.id.cbEnableAssignmentNotifs);
        CheckBox cbLowAttendanceSms = findViewById(R.id.cbEnableLowAttendanceSms);
        CheckBox cbSummaryNotifs = findViewById(R.id.cbEnableSummaryNotifications);
        Button btnSave = findViewById(R.id.btnSavePhone);
        Button btnLogout = findViewById(R.id.btnLogout);

        String teacherName = getSharedPreferences("acadmate_prefs", MODE_PRIVATE).getString("teacher_name", "");
        String saved = getSharedPreferences("acadmate_prefs", MODE_PRIVATE).getString("phone_number", "");
        String savedPin = getSharedPreferences("acadmate_prefs", MODE_PRIVATE).getString("teacher_pin", "");
        boolean assignmentNotifsEnabled = getSharedPreferences("acadmate_prefs", MODE_PRIVATE)
                .getBoolean("assignment_notifications_enabled", true);
        boolean lowAttendanceSmsEnabled = getSharedPreferences("acadmate_prefs", MODE_PRIVATE)
                .getBoolean("low_attendance_sms_enabled", true);
        boolean summaryNotifsEnabled = getSharedPreferences("acadmate_prefs", MODE_PRIVATE)
                .getBoolean("summary_notifications_enabled", true);
        etTeacherName.setText(teacherName);
        etPhone.setText(saved);
        etTeacherPin.setText(savedPin);
        cbAssignmentNotifs.setChecked(assignmentNotifsEnabled);
        cbLowAttendanceSms.setChecked(lowAttendanceSmsEnabled);
        cbSummaryNotifs.setChecked(summaryNotifsEnabled);

        btnSave.setOnClickListener(v -> {
            String teacher = etTeacherName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String teacherPin = etTeacherPin.getText().toString().trim();
            if (!teacherPin.isEmpty() && teacherPin.length() < 4) {
                Toast.makeText(this, "Teacher PIN must be at least 4 digits", Toast.LENGTH_SHORT).show();
                return;
            }
            getSharedPreferences("acadmate_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("teacher_name", teacher)
                    .putString("phone_number", phone)
                    .putString("teacher_pin", teacherPin)
                    .putBoolean("assignment_notifications_enabled", cbAssignmentNotifs.isChecked())
                    .putBoolean("low_attendance_sms_enabled", cbLowAttendanceSms.isChecked())
                    .putBoolean("summary_notifications_enabled", cbSummaryNotifs.isChecked())
                    .apply();
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            SessionManager.logout(this);
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
