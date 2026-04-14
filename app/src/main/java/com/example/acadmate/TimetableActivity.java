package com.example.acadmate;

import android.os.Bundle;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TimetableActivity extends AppCompatActivity {

    private Spinner spinnerDayOrder;
    private Spinner spinnerTime;
    private EditText etSubject;
    private EditText etSearch;
    private ListView listView;
    private DBHelper dbHelper;
    private TimetableTableListAdapter adapter;
    private final ArrayList<TimetableModel> rows = new ArrayList<>();
    private boolean isViewMode = false;
    private int viewDayOrder = 1;

    private final String[] times = {
            "1:00-1:50", "1:50-2:40", "2:40-3:30",
            "3:50-4:40", "4:40-5:30"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable);
        setTitle("Timetable");
        UiEffects.applyInteractiveAnimations(findViewById(android.R.id.content));

        dbHelper = new DBHelper(this);
        spinnerDayOrder = findViewById(R.id.spinnerDayOrder);
        spinnerTime = findViewById(R.id.spinnerTime);
        etSubject = findViewById(R.id.etSubject);
        etSearch = findViewById(R.id.etSearchTimetable);
        Button btnAdd = findViewById(R.id.btnAdd);
        Button btnAutoNextDay = findViewById(R.id.btnAutoNextDay);
        Button btnPrevDay = findViewById(R.id.btnPrevDayOrder);
        Button btnNextDay = findViewById(R.id.btnNextDayOrder);
        TextView tvCurrentDay = findViewById(R.id.tvCurrentDayOrder);
        listView = findViewById(R.id.listTimetable);
        androidx.cardview.widget.CardView cardEntry = findViewById(R.id.cardTimetableEntry);
        androidx.cardview.widget.CardView cardDayNavigator = findViewById(R.id.cardDayNavigator);

        List<Integer> dayOrders = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            dayOrders.add(i);
        }
        spinnerDayOrder.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, dayOrders));
        spinnerTime.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, times));

        adapter = new TimetableTableListAdapter(this, rows);
        listView.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences("acadmate_prefs", MODE_PRIVATE);
        int savedDayOrder = prefs.getInt("day_order_state", 1);
        spinnerDayOrder.setSelection(savedDayOrder - 1);
        spinnerDayOrder.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedDay = position + 1;
                prefs.edit().putInt("day_order_state", selectedDay).apply();
                if (!isViewMode) loadTimetable(selectedDay);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btnAdd.setOnClickListener(v -> saveTimetableRow());
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showTimetableActions(position);
            return true;
        });
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyTimetableSearch(); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
        applyOpenMode(cardEntry);
        if (isViewMode) {
            cardDayNavigator.setVisibility(View.VISIBLE);
            viewDayOrder = savedDayOrder;
            updateDayNavigatorLabel(tvCurrentDay);
            btnPrevDay.setOnClickListener(v -> {
                viewDayOrder = viewDayOrder == 1 ? 6 : viewDayOrder - 1;
                updateDayNavigatorLabel(tvCurrentDay);
                loadTimetable(viewDayOrder);
            });
            btnNextDay.setOnClickListener(v -> {
                viewDayOrder = viewDayOrder == 6 ? 1 : viewDayOrder + 1;
                updateDayNavigatorLabel(tvCurrentDay);
                loadTimetable(viewDayOrder);
            });
        }
        btnAutoNextDay.setOnClickListener(v -> {
            int current = (Integer) spinnerDayOrder.getSelectedItem();
            int next = current == 6 ? 1 : current + 1;
            prefs.edit().putInt("day_order_state", next).apply();
            spinnerDayOrder.setSelection(next - 1);
            loadTimetable(next);
            Toast.makeText(this, "Day order moved to " + next, Toast.LENGTH_SHORT).show();
        });

        if (isViewMode) {
            loadTimetable(viewDayOrder);
        } else {
            loadTimetable(savedDayOrder);
        }
    }

    private void loadTimetable(int dayOrder) {
        rows.clear();
        Cursor cursor = dbHelper.getTimetableByDay(dayOrder);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String subject = cursor.getString(1);
                String timeSlot = cursor.getString(2);
                rows.add(new TimetableModel(id, subject, "Day " + dayOrder, timeSlot));
            } while (cursor.moveToNext());
        } else {
            rows.add(new TimetableModel(-1, "-", "Day " + dayOrder, "No entries"));
        }
        cursor.close();
        applyTimetableSearch();
    }

    private void saveTimetableRow() {
        String subject = etSubject.getText().toString().trim();
        int dayOrder = (Integer) spinnerDayOrder.getSelectedItem();
        String timeSlot = (String) spinnerTime.getSelectedItem();
        if (subject.isEmpty()) {
            Toast.makeText(this, "Subject is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dbHelper.hasTimetableConflict(dayOrder, timeSlot, null)) {
            Toast.makeText(this, "Time slot conflict: another class already exists", Toast.LENGTH_SHORT).show();
            return;
        }
        dbHelper.addTimetable(subject, dayOrder, timeSlot);
        getSharedPreferences("acadmate_prefs", MODE_PRIVATE)
                .edit()
                .putInt("day_order_state", dayOrder)
                .apply();
        etSubject.requestFocus();
        Toast.makeText(this, "Timetable saved", Toast.LENGTH_SHORT).show();
        loadTimetable(dayOrder);
    }

    private void applyTimetableSearch() {
        String query = etSearch.getText().toString().trim().toLowerCase(Locale.getDefault());
        ArrayList<TimetableModel> filtered = new ArrayList<>();
        for (TimetableModel row : rows) {
            if (query.isEmpty() || row.subject.toLowerCase(Locale.getDefault()).contains(query) || row.time.toLowerCase(Locale.getDefault()).contains(query)) {
                filtered.add(row);
            }
        }
        adapter.clear();
        adapter.addAll(filtered);
        adapter.notifyDataSetChanged();
    }

    private void showTimetableActions(int position) {
        if (position < 0 || position >= rows.size()) return;
        TimetableModel row = rows.get(position);
        if (row.id <= 0) return;
        String[] options = {"Edit entry", "Delete entry"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(row.subject + " (" + row.time + ")")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) showEditTimetableDialog(row);
                    else confirmDeleteTimetable(row.id);
                })
                .show();
    }

    private void showEditTimetableDialog(TimetableModel row) {
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_edit_timetable, null);
        EditText etEditSubject = view.findViewById(R.id.etEditTimetableSubject);
        Spinner spinnerEditDay = view.findViewById(R.id.spinnerEditTimetableDay);
        Spinner spinnerEditTime = view.findViewById(R.id.spinnerEditTimetableTime);
        etEditSubject.setText(row.subject);
        List<Integer> dayOrders = new ArrayList<>();
        for (int i = 1; i <= 6; i++) dayOrders.add(i);
        spinnerEditDay.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, dayOrders));
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, times);
        spinnerEditTime.setAdapter(timeAdapter);
        spinnerEditDay.setSelection(Integer.parseInt(row.day.replace("Day ", "")) - 1);
        int timePos = timeAdapter.getPosition(row.time);
        if (timePos >= 0) {
            spinnerEditTime.setSelection(timePos);
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Edit Timetable")
                .setView(view)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String subject = etEditSubject.getText().toString().trim();
                    int dayOrder = (Integer) spinnerEditDay.getSelectedItem();
                    String timeSlot = (String) spinnerEditTime.getSelectedItem();
                    if (subject.isEmpty()) {
                        Toast.makeText(this, "Subject is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (dbHelper.hasTimetableConflict(dayOrder, timeSlot, row.id)) {
                        Toast.makeText(this, "Time slot conflict detected", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    dbHelper.updateTimetable(row.id, subject, dayOrder, timeSlot);
                    Toast.makeText(this, "Timetable updated", Toast.LENGTH_SHORT).show();
                    loadTimetable((Integer) spinnerDayOrder.getSelectedItem());
                })
                .show();
    }

    private void confirmDeleteTimetable(int timetableId) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete timetable entry?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteTimetable(timetableId);
                    Toast.makeText(this, "Entry deleted", Toast.LENGTH_SHORT).show();
                    loadTimetable((Integer) spinnerDayOrder.getSelectedItem());
                })
                .show();
    }

    private void applyOpenMode(androidx.cardview.widget.CardView cardEntry) {
        String mode = getIntent().getStringExtra("open_mode");
        isViewMode = "view".equalsIgnoreCase(mode);
        boolean isEnter = "enter".equalsIgnoreCase(mode);
        if (isViewMode) {
            setTitle("Timetable - View All");
            cardEntry.setVisibility(android.view.View.GONE);
            spinnerDayOrder.setVisibility(View.GONE);
            spinnerTime.setVisibility(View.GONE);
        } else if (isEnter) {
            setTitle("Timetable - Enter");
            etSearch.setVisibility(android.view.View.GONE);
            listView.setVisibility(android.view.View.GONE);
        }
    }

    private void updateDayNavigatorLabel(TextView tvCurrentDay) {
        tvCurrentDay.setText("Day Order " + viewDayOrder);
    }
}