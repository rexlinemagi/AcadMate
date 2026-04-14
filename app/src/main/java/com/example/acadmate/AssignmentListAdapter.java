package com.example.acadmate;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import java.time.LocalDate;
import java.util.List;

public class AssignmentListAdapter extends ArrayAdapter<AssignmentItem> {

    private final LayoutInflater inflater;

    public AssignmentListAdapter(Context context, List<AssignmentItem> items) {
        super(context, 0, items);
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.list_item_assignment, parent, false);
        }
        CardView card = view.findViewById(R.id.cardAssignmentRow);
        TextView tvTitle = view.findViewById(R.id.tvAssignmentTitle);
        TextView tvSubtitle = view.findViewById(R.id.tvAssignmentSubtitle);
        AssignmentItem item = getItem(position);
        if (item != null) {
            tvTitle.setText(item.title);
            String state = dueState(item);
            String status = item.completed ? "Completed" : "Pending";
            tvSubtitle.setText(item.subject + " | Due: " + item.dueDate + " | " + state + " | " + status);
            if (item.completed) {
                card.setCardBackgroundColor(Color.parseColor("#DCFCE7"));
            } else if ("Overdue".equals(state)) {
                card.setCardBackgroundColor(Color.parseColor("#FEE2E2"));
            } else if ("Due Tomorrow".equals(state)) {
                card.setCardBackgroundColor(Color.parseColor("#FEF3C7"));
            } else {
                card.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
            }
        }
        return view;
    }

    private String dueState(AssignmentItem item) {
        try {
            LocalDate due = LocalDate.parse(item.dueDate);
            LocalDate today = LocalDate.now();
            if (due.isBefore(today)) {
                return "Overdue";
            }
            if (due.equals(today.plusDays(1))) {
                return "Due Tomorrow";
            }
            if (due.equals(today)) {
                return "Due Today";
            }
            return "Upcoming";
        } catch (Exception ignored) {
            return "Upcoming";
        }
    }
}
