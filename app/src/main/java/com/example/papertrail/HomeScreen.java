package com.example.papertrail;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import java.io.IOException;
import java.util.List;


public class HomeScreen extends AppCompatActivity {
    private DatabaseHelper myDbHelper;
    private String selectedJournal;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);


        ImageButton addJournalButton = findViewById(R.id.addJournalButton);
        ImageButton deleteJournalButton = findViewById(R.id.deleteJournalButton);
        ImageButton editJournalButton = findViewById(R.id.editJournalButton);


        myDbHelper = new DatabaseHelper(this);
        try {
            myDbHelper.createDataBase();
            myDbHelper.openDataBase(); // Open database for writing
        } catch (IOException ioe) {
            throw new Error("Unable to create or open database");
        }


        // Retrieve journal names from the database
        List<String> journalNames = myDbHelper.getJournalNames();


        // Set up the GridView
        GridView gridView = findViewById(R.id.gridView);
        JournalAdapter adapter = new JournalAdapter(this, journalNames);
        gridView.setAdapter(adapter);


        // Add click listener for grid items (optional)
        gridView.setOnItemClickListener((parent, view, position, id) -> {
            selectedJournal = journalNames.get(position);
            Toast.makeText(this, "Selected: " + selectedJournal, Toast.LENGTH_SHORT).show();
            // Add further action here, such as opening journal details
        });


        addJournalButton.setOnClickListener(v -> {
            showAddJournalPopup();
        });


        deleteJournalButton.setOnClickListener(v -> {
            if (selectedJournal == null) {
                Toast.makeText(HomeScreen.this, "Please select a journal to delete", Toast.LENGTH_SHORT).show();
            } else {
                deleteJournal(selectedJournal);
            }
        });


        editJournalButton.setOnClickListener(v -> {
            if (selectedJournal == null) {
                Toast.makeText(HomeScreen.this, "Please select a journal to edit", Toast.LENGTH_SHORT).show();
            } else {
                editJournal(selectedJournal);
            }
        });
    }










    private void deleteJournal(String selectedJournal) {


        myDbHelper.deleteJournal(selectedJournal);


        // Fetch the updated list of journals from the database
        List<String> updatedJournalNames = myDbHelper.getJournalNames();


        // Update the GridView's adapter
        GridView gridView = findViewById(R.id.gridView);
        JournalAdapter adapter = (JournalAdapter) gridView.getAdapter();
        adapter.updateData(updatedJournalNames);


        // Notify the adapter to refresh the GridView
        adapter.notifyDataSetChanged();


        // Show a confirmation toast
        Toast.makeText(this, "Journal deleted", Toast.LENGTH_SHORT).show();
    }


    private void editJournal(String selectedJournal) {
        Intent intent = new Intent(HomeScreen.this, EditPage.class);
        intent.putExtra("journal_name", selectedJournal);
        startActivity(intent);
    }

    private void showAddJournalPopup() {
        // Create a new dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Journal");


        // Inflate the layout for the dialog
        View popupView = getLayoutInflater().inflate(R.layout.add_toast, null);
        builder.setView(popupView);


        // Get references to the EditText and Button in the popup
        EditText editTextJournalName = popupView.findViewById(R.id.editTextJournalName);
        Button btnAddJournal = popupView.findViewById(R.id.btnAddJournal);


        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();


        // Set the OnClickListener for the Add Journal button
        btnAddJournal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String journalName = editTextJournalName.getText().toString();


                if (journalName.isEmpty()) {
                    Toast.makeText(HomeScreen.this, "Please enter a journal name", Toast.LENGTH_SHORT).show();
                } else {
                    // Add journal to the database
                    addNewJournal(journalName);
                    dialog.dismiss();  // Close the popup
                }
            }
        });
    }






    private void addNewJournal(String name) {
        // Insert new journal into the database
        myDbHelper.addNewJournal(name);


        // Fetch the updated list of journals from the database
        List<String> updatedJournalNames = myDbHelper.getJournalNames();


        // Update the GridView's adapter
        GridView gridView = findViewById(R.id.gridView);
        JournalAdapter adapter = (JournalAdapter) gridView.getAdapter();
        adapter.updateData(updatedJournalNames);


        // Notify the adapter to refresh the GridView
        adapter.notifyDataSetChanged();


        // Show a confirmation toast
        Toast.makeText(this, "Journal added", Toast.LENGTH_SHORT).show();
    }






    // ImageAdapter class to populate GridView with images
    public class ImageAdapter extends BaseAdapter {
        private Context context;
        private List<String> imageResIds;


        public ImageAdapter(Context context, List<String> imageResIds) {
            this.context = context;
            this.imageResIds = imageResIds;
        }


        @Override
        public int getCount() {
            return imageResIds.size();
        }


        @Override
        public Object getItem(int position) {
            return imageResIds.get(position);
        }


        @Override
        public long getItemId(int position) {
            return position;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                // Create new ImageView if needed
                imageView = new ImageView(context);


                // Calculate the dynamic size based on screen density
                int imageSize = (int) (context.getResources().getDisplayMetrics().density * 250); // 150dp


                // Set dynamic size for the ImageView
                imageView.setLayoutParams(new GridView.LayoutParams(imageSize, imageSize));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP); // Crop the image to fit
            } else {
                // Reuse the existing ImageView
                imageView = (ImageView) convertView;
            }


            // Set image resource to the ImageView
            //imageView.setImageResource(imageResIds.get(position));  // Adjust based on your logic
            return imageView;
        }


    }}
