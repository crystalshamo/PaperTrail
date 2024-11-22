package com.example.papertrail;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.view.View;
import android.view.ViewGroup;


import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class StickerScreen extends AppCompatActivity {
    public static SQLiteDatabase db;
    DBHelper myDbHelper;
    GridView myGrid;
    ArrayList<HashMap<String, Object>> imageDataList;
    String ALLQuery = "SELECT imagename FROM stickers"; // Query to fetch only the `imagename`

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sticker_screen);

        // Initialize the GridView
        myGrid = findViewById(R.id.myList);

        // Initialize the database helper
        myDbHelper = new DBHelper(this);

        // Create or open the database
        createDB();

        // Fetch results from the database
        getResult(ALLQuery);

        // Use SimpleAdapter to map data to the GridView
        SimpleAdapter adapter = new SimpleAdapter(
                this,
                imageDataList,
                android.R.layout.simple_list_item_1, // Android's default layout
                new String[]{"image"},
                new int[]{android.R.id.text1} // Placeholder ID, unused since we'll directly set the image
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ImageView imageView;

                if (convertView == null) {
                    imageView = new ImageView(StickerScreen.this);
                    imageView.setLayoutParams(new GridView.LayoutParams(200, 200)); // Set size
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                } else {
                    imageView = (ImageView) convertView;
                }

                // Set the image resource for the ImageView
                HashMap<String, Object> map = (HashMap<String, Object>) getItem(position);
                imageView.setImageResource((int) map.get("image"));

                return imageView;
            }
        };

        myGrid.setAdapter(adapter);
    }

    // Method to create or open the database
    public void createDB() {
        try {
            myDbHelper.createDataBase();
            Log.d("DBHelper", "Database created successfully.");
        } catch (IOException ioe) {
            Log.e("DBHelper", "Error creating database", ioe);
            throw new Error("Unable to create database", ioe);
        }

        try {
            myDbHelper.openDataBase();
            db = myDbHelper.getWritableDatabase();
            Log.d("DBHelper", "Database opened successfully.");
        } catch (SQLException sqle) {
            Log.e("DBHelper", "Error opening database.", sqle);
            db = myDbHelper.getWritableDatabase(); // Fallback
        }

        if (db == null) {
            throw new Error("Database initialization failed. `db` is null.");
        }
    }

    // Method to execute the query and populate the image data list
    public void getResult(String query) {
        Cursor result = db.rawQuery(query, null);

        imageDataList = new ArrayList<>(); // Initialize the list

        if (result.moveToFirst()) {
            do {
                String imageName = result.getString(0); // Get the `imagename`
                int imageId = getResources().getIdentifier(imageName, "drawable", getPackageName());

                // Add the image resource to the list
                HashMap<String, Object> map = new HashMap<>();
                map.put("image", imageId != 0 ? imageId : R.drawable.s14); // Fallback to default image
                imageDataList.add(map);
            } while (result.moveToNext());
        } else {
            Log.d("DBHelper", "No rows found in the stickers table.");
        }

        result.close(); // Close the cursor
    }
}
