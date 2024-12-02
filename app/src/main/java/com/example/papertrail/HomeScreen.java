package com.example.papertrail;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import com.example.papertrail.DatabaseHelper;

import java.io.IOException;
import java.util.List;


public class HomeScreen extends AppCompatActivity {
    private DatabaseHelper myDbHelper;
    private String selectedJournal;
    private List<String> journalNames;  // Move this to a member variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        myDbHelper = new DatabaseHelper(this);

        try {
            // Initialize and open the database before accessing any data
            myDbHelper.createDataBase();
            myDbHelper.openDataBase(); // Open database for reading and writing
        } catch (IOException ioe) {
            throw new Error("Unable to create or open database", ioe);
        }

        // Retrieve journal names after the database is properly initialized
        journalNames = myDbHelper.getJournalNames(); // Now this works because the DB is open

        ImageButton addJournalButton = findViewById(R.id.addJournalButton);
        ImageButton deleteJournalButton = findViewById(R.id.deleteJournalButton);

        // Set up the GridView
        GridView gridView = findViewById(R.id.gridView);
        JournalAdapter adapter = new JournalAdapter(this, journalNames, myDbHelper);
        gridView.setAdapter(adapter);

        // double tap on GridView
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                int position = gridView.pointToPosition((int) e.getX(), (int) e.getY());
                if (position != GridView.INVALID_POSITION) {
                    String selectedJournal = journalNames.get(position);
                    editJournal(selectedJournal);
                }
                return true;
            }
        });

        gridView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            selectedJournal = journalNames.get(position);
            Toast.makeText(this, "Selected: " + selectedJournal, Toast.LENGTH_SHORT).show();
        });

        addJournalButton.setOnClickListener(v -> showAddJournalPopup());
        deleteJournalButton.setOnClickListener(v -> showDeleteJournalPopup());

    }


    private void deleteJournal(String selectedJournal) {
        myDbHelper.deleteJournal(selectedJournal);
        journalNames = myDbHelper.getJournalNames();

        // Update the GridView
        GridView gridView = findViewById(R.id.gridView);
        JournalAdapter adapter = (JournalAdapter) gridView.getAdapter();
        adapter.updateData(journalNames);
        adapter.notifyDataSetChanged();

        // Show toast
        Toast.makeText(this, "Journal: " + selectedJournal + " deleted", Toast.LENGTH_SHORT).show();
    }

    private void editJournal(String selectedJournal) {
        Intent intent = new Intent(HomeScreen.this, EditPage.class);
        intent.putExtra("journal_name", selectedJournal);
        startActivity(intent);
    }

    private void showAddJournalPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Journal");

        View popupView = getLayoutInflater().inflate(R.layout.add_toast, null);
        builder.setView(popupView);

        EditText editTextJournalName = popupView.findViewById(R.id.editTextJournalName);
        Button btnAddJournal = popupView.findViewById(R.id.btnAddJournal);

        AlertDialog dialog = builder.create();
        dialog.show();

        btnAddJournal.setOnClickListener(v -> {
            String journalName = editTextJournalName.getText().toString();

            if (journalName.isEmpty()) {
                Toast.makeText(HomeScreen.this, "Please enter a journal name", Toast.LENGTH_SHORT).show();
            } else if (journalNames.contains(journalName)) {
                Toast.makeText(HomeScreen.this, "Journal already exists", Toast.LENGTH_SHORT).show();
            } else {
                addNewJournal(journalName);
                dialog.dismiss();
            }
        });
    }

    private void showDeleteJournalPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Journal");

        View popupView = getLayoutInflater().inflate(R.layout.delete_pop, null);
        builder.setView(popupView);

        TextView deleteText = popupView.findViewById(R.id.deleteText);
        Button buttonNo = popupView.findViewById(R.id.button_no);
        Button buttonYes = popupView.findViewById(R.id.button_yes);

        AlertDialog dialog = builder.create();
        dialog.show();

        if (selectedJournal != null) {
            deleteText.setText("Are you sure you want to delete " + selectedJournal);
        } else {
            deleteText.setText("Please select a journal to delete.");
        }


        buttonYes.setOnClickListener(v -> {
            if (selectedJournal == null) {
                Toast.makeText(HomeScreen.this, "Please select a journal to delete", Toast.LENGTH_SHORT).show();
            } else {
                deleteJournal(selectedJournal);
                dialog.dismiss();
            }
        });


        buttonNo.setOnClickListener(v -> dialog.dismiss()); // Close
    }

    private void addNewJournal(String name) {
        myDbHelper.addNewJournal(name);

        journalNames = myDbHelper.getJournalNames(); // Update journal list after adding a new journal

        // Update the GridView's adapter
        GridView gridView = findViewById(R.id.gridView);
        JournalAdapter adapter = (JournalAdapter) gridView.getAdapter();
        adapter.updateData(journalNames);
        adapter.notifyDataSetChanged();

        Toast.makeText(this, "Journal added", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences sharedPreferences = getSharedPreferences("cover_update", MODE_PRIVATE);
        boolean isCoverUpdated = sharedPreferences.getBoolean("cover_updated", false);

        if (isCoverUpdated) {
            // Refresh the journal list from the database
            journalNames = myDbHelper.getJournalNames(); // Update journalNames

            // Get the GridView and adapter, then notify the adapter to refresh the view
            GridView gridView = findViewById(R.id.gridView);
            JournalAdapter adapter = (JournalAdapter) gridView.getAdapter();
            if (adapter != null) {
                adapter.updateData(journalNames);  // Update the data source
                adapter.notifyDataSetChanged();  // Notify the adapter that data has changed
            }

            // Reset the flag in SharedPreferences
            sharedPreferences.edit().putBoolean("cover_updated", false).apply();
        }
    }


}