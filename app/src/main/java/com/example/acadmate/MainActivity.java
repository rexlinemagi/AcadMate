package com.example.acadmate;

import android.content.Intent;
import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.appbar.MaterialToolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Locale;
import android.database.Cursor;

public class MainActivity extends AppCompatActivity {

    CardView timetable, assignments, attendance, marks, calendar;
    TextView tvTodaySummary;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        UiEffects.applyInteractiveAnimations(findViewById(android.R.id.content));

        dbHelper = new DBHelper(this);

        timetable = findViewById(R.id.cardTimetable);
        assignments = findViewById(R.id.cardAssignments);
        attendance = findViewById(R.id.cardAttendance);
        marks = findViewById(R.id.cardMarks);
        calendar = findViewById(R.id.cardCalendar);
        tvTodaySummary = findViewById(R.id.tvTodaySummary);
        Button btnEnterTimetable = findViewById(R.id.btnEnterTimetable);
        Button btnViewTimetable = findViewById(R.id.btnViewTimetable);
        Button btnEnterAssignments = findViewById(R.id.btnEnterAssignments);
        Button btnViewAssignments = findViewById(R.id.btnViewAssignments);
        Button btnEnterAttendance = findViewById(R.id.btnEnterAttendance);
        Button btnViewAttendance = findViewById(R.id.btnViewAttendance);
        Button btnEnterMarks = findViewById(R.id.btnEnterMarks);
        Button btnViewMarks = findViewById(R.id.btnViewMarks);

        int subjects = dbHelper.getSubjects().size();
        tvTodaySummary.setText("Subjects tracked: " + subjects);
        maybeSendSummaryNotifications();

        timetable.setOnClickListener(v -> openMode(TimetableActivity.class, "view"));
        assignments.setOnClickListener(v -> openMode(AssignmentActivity.class, "view"));
        attendance.setOnClickListener(v -> openMode(AttendanceActivity.class, "view"));
        marks.setOnClickListener(v -> openMode(MarksActivity.class, "view"));
        calendar.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, CalendarActivity.class)));
        btnEnterTimetable.setOnClickListener(v -> openMode(TimetableActivity.class, "enter"));
        btnViewTimetable.setOnClickListener(v -> openMode(TimetableActivity.class, "view"));
        btnEnterAssignments.setOnClickListener(v -> openMode(AssignmentActivity.class, "enter"));
        btnViewAssignments.setOnClickListener(v -> openMode(AssignmentActivity.class, "view"));
        btnEnterAttendance.setOnClickListener(v -> {
            if (SessionManager.isStudent(this)) {
                Toast.makeText(this, "Students can only view attendance", Toast.LENGTH_SHORT).show();
            } else {
                verifyTeacherPinAndOpenAttendanceEnter();
            }
        });
        btnViewAttendance.setOnClickListener(v -> openMode(AttendanceActivity.class, "view"));
        btnEnterMarks.setOnClickListener(v -> openMode(MarksActivity.class, "enter"));
        btnViewMarks.setOnClickListener(v -> openMode(MarksActivity.class, "view"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.menu_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        } else if (id == R.id.menu_clear_data) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Clear all data?")
                    .setMessage("This will erase all timetable, assignments, attendance and marks for this user only.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Clear", (d, w) -> {
                        dbHelper.clearAllData();
                        tvTodaySummary.setText("Subjects tracked: 0");
                        Toast.makeText(this, "All data cleared for this user", Toast.LENGTH_SHORT).show();
                    })
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void maybeSendSummaryNotifications() {
        if (!getSharedPreferences("acadmate_prefs", MODE_PRIVATE)
                .getBoolean("summary_notifications_enabled", true)) {
            return;
        }
        int dueCount = 0;
        String today = java.time.LocalDate.now().toString();
        Cursor dueCursor = dbHelper.getDueTodayOrOverdueAssignments(today);
        if (dueCursor.moveToFirst()) {
            do { dueCount++; } while (dueCursor.moveToNext());
        }
        dueCursor.close();

        int lowAttendanceCount = 0;
        Cursor lowCursor = dbHelper.getLowAttendanceRows(85f);
        if (lowCursor.moveToFirst()) {
            do { lowAttendanceCount++; } while (lowCursor.moveToNext());
        }
        lowCursor.close();

        if (dueCount == 0 && lowAttendanceCount == 0) {
            return;
        }

        String channelId = "acadmate_summary";
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(new NotificationChannel(channelId, "AcadMate Summary", NotificationManager.IMPORTANCE_DEFAULT));
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        String text = dueCount + " assignment(s) due/overdue, " + lowAttendanceCount + " subject(s) below 85%.";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("AcadMate Daily Summary")
                .setContentText(text)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
    }

    private void openMode(Class<?> cls, String mode) {
        Intent intent = new Intent(MainActivity.this, cls);
        intent.putExtra("open_mode", mode);
        startActivity(intent);
    }

    private void verifyTeacherPinAndOpenAttendanceEnter() {
        String teacherPin = getSharedPreferences("acadmate_prefs", MODE_PRIVATE).getString("teacher_pin", "");
        if (teacherPin.isEmpty()) {
            Toast.makeText(this, "Set Teacher PIN in Settings first", Toast.LENGTH_LONG).show();
            return;
        }
        EditText etPin = new EditText(this);
        etPin.setHint("Enter Teacher PIN");
        etPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Teacher Verification")
                .setView(etPin)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Verify", (dialog, which) -> {
                    String entered = etPin.getText().toString().trim();
                    if (!teacherPin.equals(entered)) {
                        Toast.makeText(this, "Incorrect Teacher PIN", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent intent = new Intent(MainActivity.this, AttendanceActivity.class);
                    intent.putExtra("open_mode", "enter");
                    intent.putExtra("teacher_verified", true);
                    startActivity(intent);
                })
                .show();
    }
}