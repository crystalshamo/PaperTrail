package com.example.papertrail;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ViewJournal extends AppCompatActivity {
    PageCurlView curlview;
    DatabaseHelper dbhelper;
    String journalName;
    TextView journaltv;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_journal);

        curlview = findViewById(R.id.curlview);

        // Apply window insets listener
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

        journaltv = findViewById(R.id.journaltitle);
        journaltv.setText("⋆ " + journalName + " ⋆");

        // Use ViewTreeObserver to wait for the layout pass to complete
        curlview.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                curlview.getViewTreeObserver().removeOnGlobalLayoutListener(this); // Avoid multiple calls

                // Get the dimensions now that they are available
                int viewWidth = curlview.getWidth();
                int viewHeight = curlview.getHeight();

                if (viewWidth > 0 && viewHeight > 0) {
                    // Scale and set pages
                    List<Bitmap> pages = dbhelper.getAllPagesAsBitmaps(journalName);
                    List<Bitmap> scaledPages = scaleBitmapsToViewSize(pages, viewWidth, viewHeight);
                    curlview.setCurlView(scaledPages);
                }
            }
        });
    }

    private List<Bitmap> scaleBitmapsToViewSize(List<Bitmap> bitmaps, int viewWidth, int viewHeight) {
        List<Bitmap> scaledBitmaps = new ArrayList<>();
        for (Bitmap bitmap : bitmaps) {
            scaledBitmaps.add(scaleBitmapToFit(bitmap, viewWidth, viewHeight));
        }
        return scaledBitmaps;
    }

    private Bitmap scaleBitmapToFit(Bitmap bitmap, int viewWidth, int viewHeight) {
        float widthRatio = (float) viewWidth / bitmap.getWidth();
        float heightRatio = (float) viewHeight / bitmap.getHeight();
        float scaleFactor = Math.min(widthRatio, heightRatio);

        int newWidth = Math.round(bitmap.getWidth() * scaleFactor);
        int newHeight = Math.round(bitmap.getHeight() * scaleFactor);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

}