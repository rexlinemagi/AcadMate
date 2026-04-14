package com.example.acadmate;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.time.LocalDate;

public class AssignmentActivity extends AppCompatActivity {

    private static final int REQ_POST_NOTIFICATIONS = 102;
    private DBHelper dbHelper;
    private Spinner spinnerSubject;
    private Spinner spinnerFilter;
    private EditText etTitle, etDescription, etDueDate;
    private EditText etSearch;
    private CheckBox cbReminder;
    private AssignmentListAdapter adapter;
    private final ArrayList<AssignmentItem> allAssignments = new ArrayList<>();
    private final ArrayList<AssignmentItem> filteredAssignments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignment);
        setTitle("Assignment Tracker");
        UiEffects.applyInteractiveAnimations(findViewById(android.R.id.content));

        dbHelper = new DBHelper(this);
        spinnerSubject = findViewById(R.id.spinnerSubject);
        spinnerFilter = findViewById(R.id.spinnerAssignmentFilter);
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etDueDate = findViewById(R.id.etDueDate);
        etSearch = findViewById(R.id.etSearchAssignments);
        cbReminder = findViewById(R.id.cbReminder);
        Button btnSave = findViewById(R.id.btnSaveAssignment);
        ListView listView = findViewById(R.id.listAssignments);
        androidx.cardview.widget.CardView cardEntry = findViewById(R.id.cardAssignmentEntry);

        spinnerFilter.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"All", "Pending", "Completed", "Overdue", "Due Tomorrow"}
        ));
        adapter = new AssignmentListAdapter(this, filteredAssignments);
        listView.setAdapter(adapter);

        refreshSubjectSpinner();
        loadAssignments();
        checkAndSendDueTomorrowNotifications();
        setupDueDatePicker();

        btnSave.setOnClickListener(v -> saveAssignment());
        listView.setOnItemClickListener((parent, view, position, id) -> toggleAssignmentCompletion(position));
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showAssignmentActions(position);
            return true;
        });
        spinnerFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                applyAssignmentFilters();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyAssignmentFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        applyOpenMode(cardEntry, listView);
    }

    private void setupDueDatePicker() {
        etDueDate.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        String value = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        etDueDate.setText(value);
                    },
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });
    }

    private void refreshSubjectSpinner() {
        List<String> subjects = dbHelper.getSubjects();
        if (subjects.isEmpty()) {
            subjects.add("Add timetable subjects first");
        }
        spinnerSubject.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, subjects));
    }

    private void saveAssignment() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String dueDate = etDueDate.getText().toString().trim();
        String subject = String.valueOf(spinnerSubject.getSelectedItem());
        boolean reminder = cbReminder.isChecked();

        if (title.isEmpty() || dueDate.isEmpty() || subject.equals("Add timetable subjects first")) {
            Toast.makeText(this, "Enter title/date and valid subject", Toast.LENGTH_SHORT).show();
            return;
        }

        dbHelper.addAssignment(title, subject, description, dueDate, reminder);
        Toast.makeText(this, "Assignment saved", Toast.LENGTH_SHORT).show();
        etTitle.setText("");
        etDescription.setText("");
        etDueDate.setText("");
        loadAssignments();
        checkAndSendDueTomorrowNotifications();
    }

    private void loadAssignments() {
        allAssignments.clear();
        Cursor cursor = dbHelper.getAllAssignmentsSorted();
        if (cursor.moveToFirst()) {
            do {
                int assignmentId = cursor.getInt(0);
                String title = cursor.getString(1);
                String subject = cursor.getString(2);
                String description = cursor.getString(3);
                String due = cursor.getString(4);
                boolean reminder = cursor.getInt(5) == 1;
                boolean completed = cursor.getInt(6) == 1;
                allAssignments.add(new AssignmentItem(assignmentId, title, subject, description, due, reminder, completed));
            } while (cursor.moveToNext());
        }
        cursor.close();
        applyAssignmentFilters();
    }

    private void toggleAssignmentCompletion(int position) {
        if (position < 0 || position >= filteredAssignments.size()) {
            return;
        }
        AssignmentItem item = filteredAssignments.get(position);
        int assignmentId = item.id;
        boolean currentlyCompleted = item.completed;
        boolean target = !currentlyCompleted;
        dbHelper.updateAssignmentCompleted(assignmentId, target);
        Toast.makeText(this, target ? "Marked completed" : "Marked pending", Toast.LENGTH_SHORT).show();
        loadAssignments();
    }

    private void showAssignmentActions(int position) {
        if (position < 0 || position >= filteredAssignments.size()) {
            return;
        }
        AssignmentItem item = filteredAssignments.get(position);
        String[] options = {"Edit assignment", "Delete assignment"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(item.title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showEditAssignmentDialog(item);
                    } else if (which == 1) {
                        confirmDeleteAssignment(item.id);
                    }
                }).show();
    }

    private void showEditAssignmentDialog(AssignmentItem item) {
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_edit_assignment, null);
        EditText etDialogTitle = view.findViewById(R.id.etEditAssignmentTitle);
        EditText etDialogDesc = view.findViewById(R.id.etEditAssignmentDescription);
        EditText etDialogDue = view.findViewById(R.id.etEditAssignmentDueDate);
        CheckBox cbDialogReminder = view.findViewById(R.id.cbEditAssignmentReminder);
        etDialogTitle.setText(item.title);
        etDialogDesc.setText(item.description);
        etDialogDue.setText(item.dueDate);
        cbDialogReminder.setChecked(item.reminderEnabled);
        etDialogDue.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (picker, year, month, day) -> etDialogDue.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day)),
                    now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Edit Assignment")
                .setView(view)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = etDialogTitle.getText().toString().trim();
                    String desc = etDialogDesc.getText().toString().trim();
                    String due = etDialogDue.getText().toString().trim();
                    if (title.isEmpty() || due.isEmpty()) {
                        Toast.makeText(this, "Title and due date required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    dbHelper.updateAssignment(item.id, title, item.subject, desc, due, cbDialogReminder.isChecked());
                    Toast.makeText(this, "Assignment updated", Toast.LENGTH_SHORT).show();
                    loadAssignments();
                })
                .show();
    }

    private void confirmDeleteAssignment(int assignmentId) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete assignment?")
                .setMessage("This cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, w) -> {
                    dbHelper.deleteAssignment(assignmentId);
                    Toast.makeText(this, "Assignment deleted", Toast.LENGTH_SHORT).show();
                    loadAssignments();
                }).show();
    }

    private void applyAssignmentFilters() {
        String query = etSearch.getText().toString().trim().toLowerCase(Locale.getDefault());
        String filter = String.valueOf(spinnerFilter.getSelectedItem());
        filteredAssignments.clear();
        for (AssignmentItem item : allAssignments) {
            if (!query.isEmpty()) {
                String hay = (item.title + " " + item.subject + " " + item.dueDate).toLowerCase(Locale.getDefault());
                if (!hay.contains(query)) {
                    continue;
                }
            }
            if (!passesAssignmentFilter(item, filter)) {
                continue;
            }
            filteredAssignments.add(item);
        }
        adapter.notifyDataSetChanged();
    }

    private boolean passesAssignmentFilter(AssignmentItem item, String filter) {
        if ("Pending".equals(filter)) return !item.completed;
        if ("Completed".equals(filter)) return item.completed;
        if ("Overdue".equals(filter)) return !item.completed && dueState(item).equals("Overdue");
        if ("Due Tomorrow".equals(filter)) return !item.completed && dueState(item).equals("Due Tomorrow");
        return true;
    }

    private String dueState(AssignmentItem item) {
        try {
            LocalDate due = LocalDate.parse(item.dueDate);
            LocalDate today = LocalDate.now();
            if (due.isBefore(today)) return "Overdue";
            if (due.equals(today.plusDays(1))) return "Due Tomorrow";
            if (due.equals(today)) return "Due Today";
        } catch (Exception ignored) {
        }
        return "Upcoming";
    }

    private void applyOpenMode(androidx.cardview.widget.CardView cardEntry, ListView listView) {
        String mode = getIntent().getStringExtra("open_mode");
        boolean isView = "view".equalsIgnoreCase(mode);
        boolean isEnter = "enter".equalsIgnoreCase(mode);
        if (isView) {
            setTitle("Assignments - View All");
            cardEntry.setVisibility(android.view.View.GONE);
        } else if (isEnter) {
            setTitle("Assignments - Enter");
            etSearch.setVisibility(android.view.View.GONE);
            spinnerFilter.setVisibility(android.view.View.GONE);
            listView.setVisibility(android.view.View.GONE);
        }
    }

    private void checkAndSendDueTomorrowNotifications() {
        String tomorrow = formatDateOffsetFromToday(1);
        Cursor cursor = dbHelper.getAssignmentsForReminderDate(tomorrow);
        if (cursor.moveToFirst()) {
            do {
                String title = cursor.getString(1);
                String subject = cursor.getString(2);
                String dueDate = cursor.getString(3);
                if (getSharedPreferences("acadmate_prefs", MODE_PRIVATE)
                        .getBoolean("assignment_notifications_enabled", true)) {
                    sendReminderNotification(title, subject, dueDate);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    private String formatDateOffsetFromToday(int offsetDays) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, offsetDays);
        Date date = calendar.getTime();
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date);
    }

    private void sendReminderNotification(String title, String subject, String dueDate) {
        String channelId = "acadmate_assignment";
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            NotificationChannel channel = new NotificationChannel(channelId, "Assignment Alerts", NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_POST_NOTIFICATIONS);
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Assignment Reminder")
                .setContentText(subject + ": " + title + " due " + dueDate)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
    }

}