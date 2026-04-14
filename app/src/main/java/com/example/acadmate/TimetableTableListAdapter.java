package com.example.acadmate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class TimetableTableListAdapter extends ArrayAdapter<TimetableModel> {

    private final LayoutInflater inflater;

    public TimetableTableListAdapter(Context context, List<TimetableModel> items) {
        super(context, 0, items);
        this.inflater = LayoutInflater.from(context);
    }

    static class ViewHolder {
        TextView tvTime;
        TextView tvSubject;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        View view = convertView;

        if (view == null) {
            view = inflater.inflate(R.layout.list_item_timetable_table, parent, false);
            holder = new ViewHolder();
            holder.tvTime = view.findViewById(R.id.tvTableTime);
            holder.tvSubject = view.findViewById(R.id.tvTableSubject);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        TimetableModel item = getItem(position);
        if (item != null) {
            holder.tvTime.setText(item.time);
            holder.tvSubject.setText(item.subject);
        }
        return view;
    }
}
