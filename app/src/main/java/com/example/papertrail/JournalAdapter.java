package com.example.papertrail;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class JournalAdapter extends BaseAdapter {
    private Context context;
    private List<String> journalNames;
    private List<Bitmap> journalImages;  // List to store the journal cover images
    private DatabaseHelper databaseHelper; // Instance of DatabaseHelper

    public JournalAdapter(Context context, List<String> journalNames, DatabaseHelper databaseHelper) {
        this.context = context;
        this.journalNames = journalNames;
        this.databaseHelper = databaseHelper;
        this.journalImages = new ArrayList<>();  // Initialize the list of images
        // Populate journalImages when the adapter is first created
        populateJournalImages();
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
        String journalName = journalNames.get(position);
        holder.textView.setText(journalName);

        // Fetch the image dynamically
        Bitmap firstPageImage = databaseHelper.getPageImageFromDatabase(journalName, 1);
        if (firstPageImage != null) {
            holder.imageView.setImageBitmap(firstPageImage);
        } else {
            holder.imageView.setImageResource(R.drawable.white_background);
        }

        return convertView;
    }

    private static class ViewHolder {
        ImageView imageView;
        TextView textView;
    }

    // Modified updateData method to accept both journal names and their corresponding images
    public void updateData(List<String> newJournalNames) {
        this.journalNames.clear();
        this.journalNames.addAll(newJournalNames);

        // Clear and repopulate images
        this.journalImages.clear();
        populateJournalImages();

        // Notify the adapter to refresh the UI
        notifyDataSetChanged();
    }

    private void populateJournalImages() {
        // Make sure the images list is populated correctly
        for (String journalName : journalNames) {
            Bitmap firstPageImage = databaseHelper.getPageImageFromDatabase(journalName, 1);
            if (firstPageImage != null) {
                this.journalImages.add(firstPageImage);
            } else {
                this.journalImages.add(null);
            }
        }
    }
}