package com.example.acadmate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class CalendarEventAdapter extends ArrayAdapter<TwoLineItem> {

    private static final int TYPE_HIGHLIGHT = 0;
    private static final int TYPE_ASSIGNMENT = 1;
    private static final String HIGHLIGHT_TITLE = "Due Date Highlight";
    private final LayoutInflater inflater;

    public CalendarEventAdapter(Context context, List<TwoLineItem> items) {
        super(context, 0, items);
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        TwoLineItem item = getItem(position);
        if (item != null && HIGHLIGHT_TITLE.equals(item.title)) {
            return TYPE_HIGHLIGHT;
        }
        return TYPE_ASSIGNMENT;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int viewType = getItemViewType(position);
        View view = convertView;
        if (view == null) {
            int layout = viewType == TYPE_HIGHLIGHT
                    ? R.layout.list_item_calendar_highlight
                    : R.layout.list_item_calendar_assignment;
            view = inflater.inflate(layout, parent, false);
        }

        TextView tvTitle = view.findViewById(R.id.tvRowTitle);
        TextView tvSubtitle = view.findViewById(R.id.tvRowSubtitle);
        TwoLineItem item = getItem(position);
        if (item != null) {
            tvTitle.setText(item.title);
            tvSubtitle.setText(item.subtitle);
        }
        return view;
    }
}
