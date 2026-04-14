package com.example.acadmate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class TwoLineCardAdapter extends ArrayAdapter<TwoLineItem> {

    private final LayoutInflater inflater;

    public TwoLineCardAdapter(Context context, List<TwoLineItem> items) {
        super(context, 0, items);
        this.inflater = LayoutInflater.from(context);
    }

    static class ViewHolder {
        TextView tvTitle;
        TextView tvSubtitle;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        View view = convertView;

        if (view == null) {
            view = inflater.inflate(R.layout.list_item_two_line_card, parent, false);
            holder = new ViewHolder();
            holder.tvTitle = view.findViewById(R.id.tvRowTitle);
            holder.tvSubtitle = view.findViewById(R.id.tvRowSubtitle);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        TwoLineItem item = getItem(position);
        if (item != null) {
            holder.tvTitle.setText(item.title);
            holder.tvSubtitle.setText(item.subtitle);
        }

        return view;
    }
}

