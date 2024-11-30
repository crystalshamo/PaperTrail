package com.example.papertrail;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.List;

public class ViewJournalAdapter extends RecyclerView.Adapter<ViewJournalAdapter.PageViewHolder> {
    private List<Bitmap> pages;

    public ViewJournalAdapter(List<Bitmap> pages) {
        this.pages = pages;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.page_item, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        // Get the first and second page bitmaps for the given position
        int firstPageIndex = position * 2; // First image of the pair
        int secondPageIndex = firstPageIndex + 1; // Second image of the pair

        if (firstPageIndex < pages.size()) {
            holder.firstImageView.setImageBitmap(pages.get(firstPageIndex));
        }
        if (secondPageIndex < pages.size()) {
            holder.secondImageView.setImageBitmap(pages.get(secondPageIndex));
        }
    }

    @Override
    public int getItemCount() {
        // We are displaying two pages per item, so divide the total number of pages by 2.
        return (int) Math.ceil((double) pages.size() / 2);
    }

    public static class PageViewHolder extends RecyclerView.ViewHolder {
        ImageView firstImageView;
        ImageView secondImageView;

        public PageViewHolder(View itemView) {
            super(itemView);
            firstImageView = itemView.findViewById(R.id.firstImageView);
            secondImageView = itemView.findViewById(R.id.secondImageView);
        }
    }
}
