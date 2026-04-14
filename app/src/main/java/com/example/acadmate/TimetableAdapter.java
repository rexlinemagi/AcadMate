package com.example.acadmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TimetableAdapter extends RecyclerView.Adapter<TimetableAdapter.ViewHolder> {

    List<TimetableModel> list;

    public TimetableAdapter(List<TimetableModel> list) {
        this.list = list;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView subject, details;

        public ViewHolder(View view) {
            super(view);
            subject = view.findViewById(R.id.tvSubject);
            details = view.findViewById(R.id.tvDetails);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timetable, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TimetableModel model = list.get(position);

        holder.subject.setText(model.subject);
        holder.details.setText(model.day + " | " + model.time);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}