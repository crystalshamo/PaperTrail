package com.example.papertrail;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import java.io.IOException;
import java.util.List;

public class ViewJournal extends AppCompatActivity {
    PageCurlView curlview;
    DatabaseHelper dbhelper;
    String journalName;

    private ViewPager2 viewPager;
    private ViewJournalAdapter adapter;
    private List<Bitmap> pages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_journal);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbhelper = new DatabaseHelper(this);
        try {
            dbhelper.createDataBase();
            dbhelper.openDataBase();
        } catch (IOException ioe) {
            throw new Error("Unable to create or open database");
        }

        journalName = getIntent().getStringExtra("journal_name");
        if (journalName == null) {
            Toast.makeText(this, "No journal selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //curlview = findViewById(R.id.curlview);
        //curlview.setCurlView(dbhelper.getAllPagesAsBitmaps(journalName));
        viewPager = findViewById(R.id.viewPager);
        pages = dbhelper.getAllPagesAsBitmaps(journalName);
        adapter = new ViewJournalAdapter(pages);
        viewPager.setAdapter(adapter);
        //imageView = findViewById(R.id.notebook);

        viewPager.setPadding(0, 0, 0, 0);  // Optional: Padding around the ViewPager
        viewPager.setClipToPadding(false);   // Disable clipping of the views near edges

    }
}