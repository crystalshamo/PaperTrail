package com.example.papertrail;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class HomeScreen extends AppCompatActivity {

    ImageView image;
    private DatabaseHelper dbHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_screen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Button goToEditPageBtn = findViewById(R.id.goToEditPageBtn);
        image = findViewById(R.id.imageView);
        dbHelper = new DatabaseHelper(this);
        goToEditPageBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick (View v){
                // Create an Intent to start SecondActivity
                Intent intent = new Intent(HomeScreen.this, EditPage.class);

                // Start the new activity
                startActivity(intent);
            }
        });
        loadImageForPage(1);
    }

    private void loadImageForPage(int pageNumber) {
        Bitmap bitmap = dbHelper.getImageFromDatabase(pageNumber);

        if (bitmap != null) {
            // Set the image on the ImageView
            image.setImageBitmap(bitmap);
        } else {
            // Handle the case when no image is found for this page number
            Toast.makeText(this, "No image found for page " + pageNumber, Toast.LENGTH_SHORT).show();
        }
    }


}