package com.example.acadmate;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;

public class AttendanceActivity extends AppCompatActivity {
    private static final int REQ_SMS = 201;

    private Spinner spinnerSubject;
    private Spinner spinnerFilter;
    private EditText etTotalClasses;
    private EditText etAttendedClasses;
    private EditText etSearch;
    private DBHelper dbHelper;
    private final ArrayList<TwoLineItem> rows = new ArrayList<>();
    private final ArrayList<AttendanceRow> allRows = new ArrayList<>();
    private final ArrayList<AttendanceRow> filteredRows = new ArrayList<>();
    private TwoLineCardAdapter adapter;
    private boolean teacherVerified = false;
    private String pendingLowAttendanceMessage;
    private boolean isViewMode = false;
    private static final String EXTRA_EDIT_SUBJECT = "edit_subject";
    private static final String EXTRA_EDIT_TOTAL = "edit_total";
    private static final String EXTRA_EDIT_ATTENDED = "edit_attended";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);
        setTitle("Attendance");
        UiEffects.applyInteractiveAnimations(findViewById(android.R.id.content));

        dbHelper = new DBHelper(this);
        spinnerSubject = findViewById(R.id.spinnerAttendanceSubject);
        spinnerFilter = findViewById(R.id.spinnerAttendanceFilter);
        etTotalClasses = findViewById(R.id.etTotalClasses);
        etAttendedClasses = findViewById(R.id.etAttendedClasses);
        etSearch = findViewById(R.id.etSearchAttendance);
        Button btnSave = findViewById(R.id.btnSaveAttendance);
        Button btnMarkPresent = findViewById(R.id.btnMarkPresent);
        Button btnMarkAbsent = findViewById(R.id.btnMarkAbsent);
        Button btnLock = findViewById(R.id.btnLockTeacherMode);
        ListView listView = findViewById(R.id.listAttendance);
        androidx.cardview.widget.CardView cardEntry = findViewById(R.id.cardAttendanceEntry);

        adapter = new TwoLineCardAdapter(this, rows);
        listView.setAdapter(adapter);
        spinnerFilter.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"All", "Below 85%", "Below 75%"}
        ));

        loadSubjects();
        loadAttendanceRows();
        boolean preVerified = getIntent().getBooleanExtra("teacher_verified", false);
        teacherVerified = preVerified;
        setPostingEnabled(preVerified);
        btnSave.setOnClickListener(v -> {
            if (!teacherVerified) {
                verifyTeacherAccess();
            } else {
                saveAttendance();
            }
        });
        btnMarkPresent.setOnClickListener(v -> incrementAttendance(true));
        btnMarkAbsent.setOnClickListener(v -> incrementAttendance(false));
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showAttendanceActions(position);
            return true;
        });
        spinnerFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                applyAttendanceFilters();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyAttendanceFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        applyOpenMode(cardEntry, listView);
        applyPrefillFromIntent();

        if (SessionManager.isStudent(this)) {
            // Force view-only for students
            setPostingEnabled(false);
            cardEntry.setVisibility(View.GONE);
        }

        btnLock.setOnClickListener(v -> {
            teacherVerified = false;
            setPostingEnabled(false);
            Toast.makeText(this, "Teacher mode locked", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadSubjects() {
        List<String> subjects = dbHelper.getSubjects();
        if (subjects.isEmpty()) {
            subjects.add("Add timetable subjects first");
        }
        spinnerSubject.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, subjects));
    }

    private void saveAttendance() {
        String subject = String.valueOf(spinnerSubject.getSelectedItem());
        if (subject.equals("Add timetable subjects first")) {
            Toast.makeText(this, "No valid subject found", Toast.LENGTH_SHORT).show();
            return;
        }
        String totalText = etTotalClasses.getText().toString().trim();
        String attendedText = etAttendedClasses.getText().toString().trim();
        if (totalText.isEmpty() || attendedText.isEmpty()) {
            Toast.makeText(this, "Enter all attendance values", Toast.LENGTH_SHORT).show();
            return;
        }

        int total;
        int attended;
        try {
            total = Integer.parseInt(totalText);
            attended = Integer.parseInt(attendedText);
        } catch (NumberFormatException ex) {
            Toast.makeText(this, "Use whole numbers only", Toast.LENGTH_SHORT).show();
            return;
        }
        if (total <= 0 || attended < 0 || attended > total) {
            Toast.makeText(this, "Attended classes cannot exceed total classes", Toast.LENGTH_SHORT).show();
            return;
        }

        String updatedBy = getSharedPreferences("acadmate_prefs", MODE_PRIVATE)
                .getString("teacher_name", "")
                .trim();
        if (updatedBy.isEmpty()) {
            updatedBy = "Teacher";
        }
        dbHelper.upsertAttendance(subject, total, attended, updatedBy, System.currentTimeMillis());
        float percent = (attended * 100f) / total;
        Toast.makeText(this, "Attendance saved: " + String.format("%.2f", percent) + "%", Toast.LENGTH_SHORT).show();
        if (percent < 75f) {
            new AlertDialog.Builder(this)
                    .setTitle("Attendance Warning")
                    .setMessage("Attendance is below 75% for " + subject)
                    .setPositiveButton("OK", null)
                    .show();
            sendLowAttendanceSms(subject, percent);
        }
        loadAttendanceRows();
    }

    private void loadAttendanceRows() {
        allRows.clear();
        Cursor cursor = dbHelper.getAttendanceRows();
        if (cursor.moveToFirst()) {
            do {
                String subject = cursor.getString(1);
                int total = cursor.getInt(2);
                int attended = cursor.getInt(3);
                String updatedBy = cursor.getString(4);
                long updatedAt = cursor.getLong(5);
                allRows.add(new AttendanceRow(subject, total, attended, updatedBy, updatedAt));
            } while (cursor.moveToNext());
        }
        cursor.close();
        applyAttendanceFilters();
    }

    private void verifyTeacherAccess() {
        verifyTeacherAccess(null);
    }

    private void verifyTeacherAccess(Runnable onVerified) {
        String teacherPin = getSharedPreferences("acadmate_prefs", MODE_PRIVATE)
                .getString("teacher_pin", "");
        if (teacherPin.isEmpty()) {
            Toast.makeText(this, "Set Teacher PIN in Settings first", Toast.LENGTH_LONG).show();
            return;
        }

        EditText etPin = new EditText(this);
        etPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        etPin.setHint("Enter Teacher PIN");

        new AlertDialog.Builder(this)
                .setTitle("Teacher Verification")
                .setView(etPin)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Verify", (dialog, which) -> {
                    String enteredPin = etPin.getText().toString().trim();
                    if (!teacherPin.equals(enteredPin)) {
                        Toast.makeText(this, "Incorrect Teacher PIN", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    teacherVerified = true;
                    setPostingEnabled(true);
                    Toast.makeText(this, "Teacher mode enabled", Toast.LENGTH_SHORT).show();
                    if (onVerified != null) {
                        onVerified.run();
                    }
                })
                .show();
    }

    private void setPostingEnabled(boolean enabled) {
        spinnerSubject.setEnabled(enabled);
        etTotalClasses.setEnabled(enabled);
        etAttendedClasses.setEnabled(enabled);
        Button btnSave = findViewById(R.id.btnSaveAttendance);
        Button btnLock = findViewById(R.id.btnLockTeacherMode);
        Button btnMarkPresent = findViewById(R.id.btnMarkPresent);
        Button btnMarkAbsent = findViewById(R.id.btnMarkAbsent);
        btnSave.setText(enabled ? "Post Attendance (Teacher)" : "Unlock Teacher Mode");
        btnLock.setVisibility(enabled ? android.view.View.VISIBLE : android.view.View.GONE);
        btnMarkPresent.setEnabled(enabled);
        btnMarkAbsent.setEnabled(enabled);
    }

    private void incrementAttendance(boolean present) {
        int total = parseIntOrZero(etTotalClasses.getText().toString().trim());
        int attended = parseIntOrZero(etAttendedClasses.getText().toString().trim());
        total += 1;
        if (present) {
            attended += 1;
        }
        etTotalClasses.setText(String.valueOf(total));
        etAttendedClasses.setText(String.valueOf(attended));
    }

    private int parseIntOrZero(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void sendLowAttendanceSms(String subject, float percent) {
        if (!getSharedPreferences("acadmate_prefs", MODE_PRIVATE).getBoolean("low_attendance_sms_enabled", true)) {
            return;
        }
        String phone = getSharedPreferences("acadmate_prefs", MODE_PRIVATE).getString("phone_number", "").trim();
        if (phone.isEmpty()) {
            Toast.makeText(this, "Set phone number in Settings for SMS alerts", Toast.LENGTH_SHORT).show();
            return;
        }
        String message = "Low attendance alert: " + subject + " is at " + String.format(Locale.getDefault(), "%.1f", percent) + "%.";
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            pendingLowAttendanceMessage = message;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQ_SMS);
            return;
        }
        SmsManager.getDefault().sendTextMessage(phone, null, message, null, null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_SMS && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            String msg = pendingLowAttendanceMessage;
            pendingLowAttendanceMessage = null;
            if (msg != null) {
                String phone = getSharedPreferences("acadmate_prefs", MODE_PRIVATE).getString("phone_number", "").trim();
                if (!phone.isEmpty()) {
                    SmsManager.getDefault().sendTextMessage(phone, null, msg, null, null);
                }
            }
        }
    }

    private String formatAudit(String updatedBy, long updatedAt) {
        String who = (updatedBy == null || updatedBy.trim().isEmpty()) ? "Teacher" : updatedBy.trim();
        if (updatedAt <= 0) {
            return "by " + who;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
        return "by " + who + " on " + sdf.format(new Date(updatedAt));
    }

    private int classesNeededFor85(int total, int attended) {
        int extra = 0;
        while (((attended + extra) * 100f) / (total + extra) < 85f) {
            extra++;
            if (extra > 10000) break;
        }
        return extra;
    }

    private void applyAttendanceFilters() {
        String query = etSearch.getText().toString().trim().toLowerCase(Locale.getDefault());
        String filter = String.valueOf(spinnerFilter.getSelectedItem());
        filteredRows.clear();
        for (AttendanceRow row : allRows) {
            if (!query.isEmpty() && !row.subject.toLowerCase(Locale.getDefault()).contains(query)) {
                continue;
            }
            float pct = row.percent();
            if ("Below 85%".equals(filter) && pct >= 85f) continue;
            if ("Below 75%".equals(filter) && pct >= 75f) continue;
            filteredRows.add(row);
        }
        rows.clear();
        if (filteredRows.isEmpty()) {
            rows.add(new TwoLineItem("No attendance data", "No entries match current filters."));
        } else {
            for (AttendanceRow row : filteredRows) {
                String audit = formatAudit(row.updatedBy, row.updatedAt);
                int needed = classesNeededFor85(row.total, row.attended);
                String needText = needed == 0 ? "At/above 85%" : "Need " + needed + " present class(es) for 85%";
                rows.add(new TwoLineItem(
                        row.subject,
                        row.attended + "/" + row.total + " (" + String.format("%.1f", row.percent()) + "%) • " + needText + " • " + audit
                ));
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showAttendanceActions(int position) {
        if (position < 0 || position >= filteredRows.size()) return;
        AttendanceRow row = filteredRows.get(position);
        String[] options = {"Edit this subject attendance", "Delete this subject attendance"};
        new AlertDialog.Builder(this)
                .setTitle(row.subject)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openEditAttendanceDialog(row);
                    } else {
                        if (!teacherVerified) {
                            verifyTeacherAccess(() -> {
                                dbHelper.deleteAttendanceBySubject(row.subject);
                                Toast.makeText(this, "Attendance deleted", Toast.LENGTH_SHORT).show();
                                loadAttendanceRows();
                            });
                        } else {
                            dbHelper.deleteAttendanceBySubject(row.subject);
                            Toast.makeText(this, "Attendance deleted", Toast.LENGTH_SHORT).show();
                            loadAttendanceRows();
                        }
                    }
                })
                .show();
    }

    private void openEditAttendanceDialog(AttendanceRow row) {
        Runnable openEditor = () -> {
            if (isViewMode) {
                android.content.Intent intent = new android.content.Intent(this, AttendanceActivity.class);
                intent.putExtra("open_mode", "enter");
                intent.putExtra("teacher_verified", true);
                intent.putExtra(EXTRA_EDIT_SUBJECT, row.subject);
                intent.putExtra(EXTRA_EDIT_TOTAL, row.total);
                intent.putExtra(EXTRA_EDIT_ATTENDED, row.attended);
                startActivity(intent);
                return;
            }
            etTotalClasses.setText(String.valueOf(row.total));
            etAttendedClasses.setText(String.valueOf(row.attended));
            @SuppressWarnings("unchecked")
            ArrayAdapter<String> subAdapter = (ArrayAdapter<String>) spinnerSubject.getAdapter();
            int spinnerIndex = subAdapter.getPosition(row.subject);
            if (spinnerIndex >= 0) spinnerSubject.setSelection(spinnerIndex);
            Toast.makeText(this, "Loaded into form. Edit and post.", Toast.LENGTH_SHORT).show();
        };

        if (!teacherVerified) {
            verifyTeacherAccess(openEditor);
        } else {
            openEditor.run();
        }
    }

    private void applyPrefillFromIntent() {
        String subject = getIntent().getStringExtra(EXTRA_EDIT_SUBJECT);
        if (subject == null || subject.trim().isEmpty()) {
            return;
        }
        int total = getIntent().getIntExtra(EXTRA_EDIT_TOTAL, -1);
        int attended = getIntent().getIntExtra(EXTRA_EDIT_ATTENDED, -1);
        if (total >= 0) {
            etTotalClasses.setText(String.valueOf(total));
        }
        if (attended >= 0) {
            etAttendedClasses.setText(String.valueOf(attended));
        }
        @SuppressWarnings("unchecked")
        ArrayAdapter<String> subAdapter = (ArrayAdapter<String>) spinnerSubject.getAdapter();
        int spinnerIndex = subAdapter.getPosition(subject);
        if (spinnerIndex >= 0) {
            spinnerSubject.setSelection(spinnerIndex);
        }
        Toast.makeText(this, "Edit mode: update and post attendance.", Toast.LENGTH_SHORT).show();
    }

    private static class AttendanceRow {
        final String subject;
        final int total;
        final int attended;
        final String updatedBy;
        final long updatedAt;

        AttendanceRow(String subject, int total, int attended, String updatedBy, long updatedAt) {
            this.subject = subject;
            this.total = total;
            this.attended = attended;
            this.updatedBy = updatedBy;
            this.updatedAt = updatedAt;
        }

        float percent() {
            return total == 0 ? 0f : (attended * 100f) / total;
        }
    }

    private void applyOpenMode(androidx.cardview.widget.CardView cardEntry, ListView listView) {
        String mode = getIntent().getStringExtra("open_mode");
        boolean isView = "view".equalsIgnoreCase(mode);
        boolean isEnter = "enter".equalsIgnoreCase(mode);
        isViewMode = isView;
        if (isView) {
            setTitle("Attendance - View All");
            cardEntry.setVisibility(View.GONE);
        } else if (isEnter) {
            setTitle("Attendance - Enter");
            etSearch.setVisibility(View.GONE);
            spinnerFilter.setVisibility(View.GONE);
            listView.setVisibility(View.GONE);
        }
    }
}