package com.example.papertrail;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class JournalAdapter extends BaseAdapter {
    private Context context;
    private List<String> journalNames;

    public JournalAdapter(Context context, List<String> journalNames) {
        this.context = context;
        this.journalNames = journalNames;
    }

    @Override
    public int getCount() {
        return journalNames.size();
    }

    @Override
    public Object getItem(int position) {
        return journalNames.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false);
            holder = new ViewHolder();
            holder.imageView = convertView.findViewById(R.id.item_image);
            holder.textView = convertView.findViewById(R.id.item_text);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Set journal name
        holder.textView.setText(journalNames.get(position));

        // Placeholder image
        holder.imageView.setImageResource(R.drawable.white_background);

        return convertView;
    }

    private static class ViewHolder {
        ImageView imageView;
        TextView textView;
    }

    public void updateData(List<String> newJournalNames) {
        this.journalNames.clear();  // Clear the current list
        this.journalNames.addAll(newJournalNames);  // Add the new list of journals
        notifyDataSetChanged();  // Notify the adapter to refresh the GridView
    }
}