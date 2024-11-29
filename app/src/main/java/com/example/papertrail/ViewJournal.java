package com.example.papertrail;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;

public class ViewJournal extends AppCompatActivity {
    PageCurlView curlview;
    DatabaseHelper dbhelper;

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
        curlview = findViewById(R.id.curlview);
        curlview.setCurlView(dbhelper.getAllPagesAsBitmaps("f"));
    }
}