package com.example.papertrail;

import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.BaseAdapter;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class StickerScreen extends AppCompatActivity {
    private static SQLiteDatabase db;
    private DBHelper myDbHelper;
    private GridView myGrid;
    private ArrayList<HashMap<String, Object>> imageDataList;

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

        // Set up the button to show the popup
        ImageView showPopupButton = findViewById(R.id.show_popup_button);
        showPopupButton.setOnClickListener(this::showPopup);

        // Default category load (load all stickers)
        loadCategory("ALL");
    }

    private void createDB() {
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

    private void showPopup(View anchorView) {
        // Inflate the popup layout
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_layout, null);

        // Create the PopupWindow
        PopupWindow popupWindow = new PopupWindow(popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true);

        // Find the RadioGroup
        RadioGroup radioGroup = popupView.findViewById(R.id.radio_group);

        // Set up the button
        ImageView  popupButton = popupView.findViewById(R.id.popup_button);
        popupButton.setOnClickListener(v -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();

            // Determine the selected category
            if (selectedId == R.id.radio_all) {
                loadCategory("ALL");
            } else if (selectedId == R.id.radio_glam) {
                loadCategory("GG");
            } else if (selectedId == R.id.radio_food) {
                loadCategory("FD");
            } else if (selectedId == R.id.radio_winter) {
                loadCategory("W");
            } else if (selectedId == R.id.radio_spring) {
                loadCategory("SP");
            } else if (selectedId == R.id.radio_fall) {
                loadCategory("F");
            } else if (selectedId == R.id.radio_summer) {
                loadCategory("S");
            } else if (selectedId == R.id.radio_celebrations) {
                loadCategory("C");
            } else if (selectedId == R.id.radio_phrases) {
                loadCategory("P");
            } else if (selectedId == R.id.radio_nature) {
                loadCategory("N");
            } else if (selectedId == R.id.radio_other) {
                loadCategory("O");
            }

            popupWindow.dismiss();
        });

        // Show the popup at the center of the anchor view
        popupWindow.showAtLocation(anchorView, Gravity.CENTER, 0, 0);
    }

    private void loadCategory(String categoryCode) {
        String query;
        if (categoryCode.equals("ALL")) {
            query = "SELECT imagename FROM stickers";
        } else {
            query = "SELECT imagename FROM stickers WHERE category = '" + categoryCode + "'";
        }

        getResult(query);

        // Use a custom adapter to map data to the GridView
        StickerAdapter adapter = new StickerAdapter(imageDataList);
        myGrid.setAdapter(adapter);
    }

    private void getResult(String query) {
        Cursor result = db.rawQuery(query, null);

        imageDataList = new ArrayList<>();

        if (result.moveToFirst()) {
            do {
                String imageName = result.getString(0); // Get the `imagename`
                int imageId = getResources().getIdentifier(imageName, "drawable", getPackageName());

                HashMap<String, Object> map = new HashMap<>();
                map.put("image", imageId != 0 ? imageId : R.drawable.s14); // Default fallback image
                imageDataList.add(map);
            } while (result.moveToNext());
        } else {
            Log.d("DBHelper", "No rows found in the stickers table.");
        }

        result.close();
    }

    // Custom Adapter for GridView
    private class StickerAdapter extends BaseAdapter {
        private final ArrayList<HashMap<String, Object>> data;

        public StickerAdapter(ArrayList<HashMap<String, Object>> data) {
            this.data = data;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;

            if (convertView == null) {
                imageView = new ImageView(StickerScreen.this);
                imageView.setLayoutParams(new GridView.LayoutParams(200, 200)); // Fixed size
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            } else {
                imageView = (ImageView) convertView;
            }

            // Set the image resource
            HashMap<String, Object> map = (HashMap<String, Object>) getItem(position);
            int imageId = (int) map.get("image");
            imageView.setImageResource(imageId);

            // Set click listener to return the selected sticker
            imageView.setOnClickListener(v -> {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selected_sticker", imageId); // Pass the resource ID
                setResult(RESULT_OK, resultIntent);
                finish(); // Close the activity
            });

            return imageView;
        }
    }
}