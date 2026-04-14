package com.example.acadmate;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private DBHelper dbHelper;
    private final ArrayList<TwoLineItem> events = new ArrayList<>();
    private CalendarEventAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        setTitle("Academic Calendar");
        UiEffects.applyInteractiveAnimations(findViewById(android.R.id.content));

        dbHelper = new DBHelper(this);
        CalendarView calendarView = findViewById(R.id.calendarView);
        TextView tvCalendarHighlights = findViewById(R.id.tvCalendarHighlights);
        ListView listView = findViewById(R.id.listCalendarEvents);
        adapter = new CalendarEventAdapter(this, events);
        listView.setAdapter(adapter);

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        int currentYear = Integer.parseInt(today.substring(0, 4));
        int currentMonth = Integer.parseInt(today.substring(5, 7));
        loadMonthlyHighlights(currentYear, currentMonth, tvCalendarHighlights);
        loadEvents(today);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            loadMonthlyHighlights(year, month + 1, tvCalendarHighlights);
            loadEvents(date);
        });
    }

    private void loadEvents(String date) {
        events.clear();
        Cursor cursor = dbHelper.getAssignmentEventsForDate(date);
        if (cursor.moveToFirst()) {
            do {
                String title = cursor.getString(0);
                String subject = cursor.getString(1);
                events.add(new TwoLineItem(title, subject));
            } while (cursor.moveToNext());
        }
        cursor.close();
        if (events.isEmpty()) {
            events.add(new TwoLineItem("No events", "No deadlines on " + date));
        } else {
            events.add(0, new TwoLineItem("Due Date Highlight", "Assignments are due on this selected date."));
            Toast.makeText(this, "Due assignments shown for " + date, Toast.LENGTH_SHORT).show();
        }
        adapter.notifyDataSetChanged();
    }

    private void loadMonthlyHighlights(int year, int month, TextView tvCalendarHighlights) {
        List<Integer> dueDays = dbHelper.getAssignmentDaysForMonth(year, month);
        if (dueDays.isEmpty()) {
            tvCalendarHighlights.setText("Highlighted due days: none in this month");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Highlighted due days: ");
        for (int i = 0; i < dueDays.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(String.format(Locale.US, "%02d", dueDays.get(i)));
        }
        tvCalendarHighlights.setText(sb.toString());
    }
}
