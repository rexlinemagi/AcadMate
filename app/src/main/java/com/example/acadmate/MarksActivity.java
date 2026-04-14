package com.example.acadmate;

import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class MarksActivity extends AppCompatActivity {

    private DBHelper dbHelper;
    private Spinner spinnerSubject;
    private Spinner spinnerAssessment;
    private EditText etMarks;
    private EditText etSearch;
    private final ArrayList<TwoLineItem> rows = new ArrayList<>();
    private final ArrayList<SubjectMarksGroup> groupedMarks = new ArrayList<>();
    private final ArrayList<SubjectMarksGroup> filteredGroups = new ArrayList<>();
    private TwoLineCardAdapter listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marks);
        setTitle("Internal Marks");
        UiEffects.applyInteractiveAnimations(findViewById(android.R.id.content));

        dbHelper = new DBHelper(this);
        spinnerSubject = findViewById(R.id.spinnerMarkSubject);
        spinnerAssessment = findViewById(R.id.spinnerAssessmentType);
        etMarks = findViewById(R.id.etMarksValue);
        etSearch = findViewById(R.id.etSearchMarks);
        Button btnSave = findViewById(R.id.btnSaveMarks);
        ListView listView = findViewById(R.id.listMarks);
        androidx.cardview.widget.CardView cardEntry = findViewById(R.id.cardMarksEntry);

        List<String> subjects = dbHelper.getSubjects();
        if (subjects.isEmpty()) {
            subjects.add("Add timetable subjects first");
        }
        spinnerSubject.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, subjects));
        spinnerAssessment.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"CA1", "CA2", "Component 1", "Component 2"}
        ));

        listAdapter = new TwoLineCardAdapter(this, rows);
        listView.setAdapter(listAdapter);
        refreshRows();

        btnSave.setOnClickListener(v -> saveMark());
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showMarkActions(position);
            return true;
        });
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyMarksFilter(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        applyOpenMode(cardEntry, listView);
    }

    private void saveMark() {
        String subject = String.valueOf(spinnerSubject.getSelectedItem());
        String assessment = String.valueOf(spinnerAssessment.getSelectedItem());
        String marksText = etMarks.getText().toString().trim();
        if (subject.equals("Add timetable subjects first") || marksText.isEmpty()) {
            Toast.makeText(this, "Enter valid subject and marks", Toast.LENGTH_SHORT).show();
            return;
        }
        float marks;
        try {
            marks = Float.parseFloat(marksText);
        } catch (NumberFormatException ex) {
            Toast.makeText(this, "Marks must be numeric", Toast.LENGTH_SHORT).show();
            return;
        }
        if (marks < 0) {
            Toast.makeText(this, "Marks cannot be negative", Toast.LENGTH_SHORT).show();
            return;
        }
        float maxAllowed = getMaxMarksForAssessment(assessment);
        if (marks > maxAllowed) {
            Toast.makeText(this, assessment + " should be out of " + (int) maxAllowed, Toast.LENGTH_SHORT).show();
            return;
        }
        dbHelper.addMark(subject, assessment, marks);
        Toast.makeText(this, "Marks saved", Toast.LENGTH_SHORT).show();
        etMarks.setText("");
        refreshRows();
    }

    private void refreshRows() {
        groupedMarks.clear();
        Cursor cursor = dbHelper.getAllMarksDetailed();
        if (cursor.moveToFirst()) {
            LinkedHashMap<String, SubjectMarksGroup> groups = new LinkedHashMap<>();
            do {
                int markId = cursor.getInt(0);
                String subject = cursor.getString(1);
                String assessment = cursor.getString(2);
                float marks = cursor.getFloat(3);
                SubjectMarksGroup group = groups.get(subject);
                if (group == null) {
                    group = new SubjectMarksGroup(subject);
                    groups.put(subject, group);
                }
                group.setAssessment(assessment, markId, marks);
            } while (cursor.moveToNext());
            for (Map.Entry<String, SubjectMarksGroup> entry : groups.entrySet()) {
                SubjectMarksGroup group = entry.getValue();
                groupedMarks.add(group);
            }
        }
        cursor.close();
        applyMarksFilter();
    }

    private void showMarkActions(int position) {
        if (position < 0 || position >= filteredGroups.size()) {
            return;
        }
        SubjectMarksGroup group = filteredGroups.get(position);
        List<String> available = group.availableAssessments();
        if (available.isEmpty()) {
            return;
        }
        String[] options = available.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Choose assessment")
                .setItems(options, (dialog, which) -> {
                    String assessment = options[which];
                    showAssessmentActions(group, assessment);
                })
                .show();
    }

    private void showAssessmentActions(SubjectMarksGroup group, String assessment) {
        Integer markId = group.markIds.get(assessment);
        if (markId == null) {
            return;
        }
        String[] actions = {"Edit marks", "Delete entry"};
        new AlertDialog.Builder(this)
                .setTitle(group.subject + " - " + assessment)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        showEditMarksDialog(markId);
                    } else if (which == 1) {
                        confirmDeleteMark(markId);
                    }
                })
                .show();
    }

    private void showEditMarksDialog(int markId) {
        EditText etInput = new EditText(this);
        etInput.setHint("Enter new marks");
        etInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        new AlertDialog.Builder(this)
                .setTitle("Edit Marks")
                .setView(etInput)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String value = etInput.getText().toString().trim();
                    if (value.isEmpty()) {
                        Toast.makeText(this, "Marks required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    float marks;
                    try {
                        marks = Float.parseFloat(value);
                    } catch (NumberFormatException ex) {
                        Toast.makeText(this, "Marks must be numeric", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (marks < 0) {
                        Toast.makeText(this, "Marks cannot be negative", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    float maxAllowed = getMaxMarksForAssessmentByMarkId(markId);
                    if (marks > maxAllowed) {
                        Toast.makeText(this, "Marks should be out of " + (int) maxAllowed, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    dbHelper.updateMarkById(markId, marks);
                    Toast.makeText(this, "Marks updated", Toast.LENGTH_SHORT).show();
                    refreshRows();
                })
                .show();
    }

    private void confirmDeleteMark(int markId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete marks entry?")
                .setMessage("This cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteMarkById(markId);
                    Toast.makeText(this, "Marks deleted", Toast.LENGTH_SHORT).show();
                    refreshRows();
                })
                .show();
    }

    private float getMaxMarksForAssessment(String assessment) {
        if ("Component 1".equals(assessment) || "Component 2".equals(assessment)) {
            return 25f;
        }
        return 50f;
    }

    private float getMaxMarksForAssessmentByMarkId(int markId) {
        Cursor cursor = dbHelper.getAllMarksDetailed();
        float max = 50f;
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                if (id == markId) {
                    String assessment = cursor.getString(2);
                    max = getMaxMarksForAssessment(assessment);
                    break;
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return max;
    }

    private static class SubjectMarksGroup {
        final String subject;
        final LinkedHashMap<String, Float> scores = new LinkedHashMap<>();
        final LinkedHashMap<String, Integer> markIds = new LinkedHashMap<>();

        SubjectMarksGroup(String subject) {
            this.subject = subject;
            scores.put("CA1", 0f);
            scores.put("CA2", 0f);
            scores.put("Component 1", 0f);
            scores.put("Component 2", 0f);
        }

        void setAssessment(String assessment, int markId, float value) {
            scores.put(assessment, value);
            markIds.put(assessment, markId);
        }

        List<String> availableAssessments() {
            return new ArrayList<>(markIds.keySet());
        }

        String buildPrettyBreakdown() {
            float ca1 = scores.get("CA1") == null ? 0f : scores.get("CA1");
            float ca2 = scores.get("CA2") == null ? 0f : scores.get("CA2");
            float c1 = scores.get("Component 1") == null ? 0f : scores.get("Component 1");
            float c2 = scores.get("Component 2") == null ? 0f : scores.get("Component 2");
            float bestCa = Math.max(ca1, ca2);
            float internal = (bestCa + c1 + c2) / 2f;
            return "CA1: " + ca1 + "/50 | CA2: " + ca2 + "/50" +
                    "\nComp 1: " + c1 + "/25 | Comp 2: " + c2 + "/25" +
                    "\n✨ Internal = (Best CA + C1 + C2) / 2 = " +
                    String.format(Locale.getDefault(), "%.2f", internal) + "/50 (long press to edit/delete)";
        }
    }

    private void applyMarksFilter() {
        String query = etSearch.getText().toString().trim().toLowerCase(Locale.getDefault());
        filteredGroups.clear();
        rows.clear();
        for (SubjectMarksGroup group : groupedMarks) {
            if (!query.isEmpty() && !group.subject.toLowerCase(Locale.getDefault()).contains(query)) {
                continue;
            }
            filteredGroups.add(group);
            rows.add(new TwoLineItem("🎓 " + group.subject, group.buildPrettyBreakdown()));
        }
        if (rows.isEmpty()) {
            rows.add(new TwoLineItem("No marks yet", "Add marks to see totals."));
        }
        listAdapter.notifyDataSetChanged();
    }

    private void applyOpenMode(androidx.cardview.widget.CardView cardEntry, ListView listView) {
        String mode = getIntent().getStringExtra("open_mode");
        boolean isView = "view".equalsIgnoreCase(mode);
        boolean isEnter = "enter".equalsIgnoreCase(mode);
        if (isView) {
            setTitle("Internal Marks - View All");
            cardEntry.setVisibility(android.view.View.GONE);
        } else if (isEnter) {
            setTitle("Internal Marks - Enter");
            etSearch.setVisibility(android.view.View.GONE);
            listView.setVisibility(android.view.View.GONE);
        }
    }
}
