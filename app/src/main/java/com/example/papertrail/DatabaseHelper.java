package com.example.papertrail;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {


    private static String DB_PATH = "/data/data/com.example.papertrail/databases/"; // Make sure this path matches your app's package name
    private static String DB_NAME = "database5";
    private SQLiteDatabase db;
    private Context myContext;
    private static String JOURNAL_TABLE_NAME = "JournalTable";
    private static String JOURNAL_NAME_COL = "name";


    // PageTable Columns
    public static final String PAGE_TABLE = "PageTable";
    public static final String COLUMN_PAGE_NUMBER = "page_number";
    public static final String COLUMN_BACKGROUND_IMAGE = "background_image";
    public static final String COLUMN_JOURNAL_ID = "journal_id"; // journal_id as the foreign key




    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, 1);
        this.myContext = context;
    }


    public void createDataBase() throws IOException {
        boolean dbExist = checkDataBase();


        if (!dbExist) {
            this.getReadableDatabase();
            try {
                copyDataBase();
            } catch (IOException e) {
                throw new Error("Error copying database");
            }
        }
    }


    private boolean checkDataBase() {
        SQLiteDatabase checkDB = null;


        try {
            String myPath = DB_PATH + DB_NAME;
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
        } catch (android.database.SQLException e) {
            // database doesn't exist yet
        }


        if (checkDB != null) {
            checkDB.close();
        }


        return checkDB != null;
    }


    private void copyDataBase() throws IOException {
        InputStream myInput = myContext.getAssets().open(DB_NAME + ".db");
        String outFileName = DB_PATH + DB_NAME;
        OutputStream myOutput = new FileOutputStream(outFileName);


        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer)) > 0) {
            myOutput.write(buffer, 0, length);
        }


        myOutput.flush();
        myOutput.close();
        myInput.close();
    }


    public void openDataBase() {
        try {
            String myPath = DB_PATH + DB_NAME;
            db = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE); // Use READWRITE for insertions
        } catch (android.database.SQLException e) {
            throw new RuntimeException("Error opening database", e);
        }
    }


    @Override
    public synchronized void close() {
        if (db != null) db.close();
        super.close();
    }


    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
    }


    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // Handle upgrading the database if needed
        // sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + JOURNAL_TABLE_NAME);
        // onCreate(sqLiteDatabase);
    }


    public Bitmap getPageImageFromDatabase(String journalName, int pageNumber) {
        SQLiteDatabase db = this.getReadableDatabase();


        // Step 1: Retrieve the journal_id using the journal_name
        int journalId = getJournalIdByName(journalName);  // Use the method to fetch journal_id by name


        // If the journal_id is -1, return null (journal not found)
        if (journalId == -1) {
            Log.e("DatabaseError", "Journal with name " + journalName + " not found.");
            return null;
        }


        // Step 2: Retrieve the page image using the journal_id and page_number
        Cursor cursor = db.query(
                "PageTable",               // Table name
                new String[]{"background_image"}, // Column to retrieve (the image column)
                "journal_id = ? AND page_number = ?", // WHERE clause: match journal_id and page_number
                new String[]{String.valueOf(journalId), String.valueOf(pageNumber)}, // Selection args
                null,                       // Group by
                null,                       // Having
                null                        // Order by
        );


        Bitmap bitmap = null; // Declare a Bitmap to hold the result


        if (cursor != null && cursor.moveToFirst()) {
            // Retrieve the image as a byte array
            int imageColumnIndex = cursor.getColumnIndex("background_image");
            if (imageColumnIndex != -1) {
                byte[] imageByteArray = cursor.getBlob(imageColumnIndex);
                // Convert byte array to Bitmap
                bitmap = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.length);
            } else {
                Log.e("DatabaseError", "background_image column not found in PageTable.");
            }
            cursor.close();
        } else {
            Log.e("DatabaseError", "No page found for journal: " + journalName + " and page number: " + pageNumber);
        }


        return bitmap; // Return the retrieved Bitmap, or null if not found
    }
    public void addNewJournal(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(JOURNAL_NAME_COL, name);
        db.insert(JOURNAL_TABLE_NAME, null, values);
        db.close();
    }


    public void deleteJournal(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(JOURNAL_TABLE_NAME, "name=?", new String[]{name});
        db.close();
    }


    public List<String> getJournalNames() {
        List<String> journalNames = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();


        Cursor cursor = db.query("JournalTable", new String[]{"name"}, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex("name"));
                journalNames.add(name);
            } while (cursor.moveToNext());
        }


        cursor.close();
        db.close();


        return journalNames;
    }


    // Save the image to PageTable
    public void saveImageToPageTable(String journalName, int pageNumber, Bitmap bitmap) {
        // Get the journal id using the journal name
        int journalId = getJournalIdByName(journalName);


        // If the journal does not exist, return or handle the error
        if (journalId == -1) {
            Log.e("DatabaseError", "Journal with name " + journalName + " not found.");
            return;
        }


        // Convert bitmap to byte array
        byte[] imageByteArray = bitmapToByteArray(bitmap);


        // Get writable database
        SQLiteDatabase db = this.getWritableDatabase();


        // Prepare the content values to insert into the PageTable
        ContentValues contentValues = new ContentValues();
        contentValues.put("page_number", pageNumber);  // Save page number
        contentValues.put("journal_id", journalId);    // Save the journal ID (retrieved by name)
        contentValues.put("background_image", imageByteArray);  // Save image as BLOB


        // Insert the record into PageTable
        long result = db.insert("PageTable", null, contentValues);


        if (result == -1) {
            Log.e("DatabaseError", "Failed to insert new page.");
        } else {
            Log.d("Database", "New page added successfully for journal: " + journalName);
        }


        // Close the database
        db.close();
    }




    // Convert Bitmap to byte array (BLOB)
    public byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);  // Compress to PNG (you can use JPG if needed)
        return byteArrayOutputStream.toByteArray();
    }


    public void addNewPage(int journalId, int pageNumber, Bitmap bitmap) {
        // Convert the bitmap to a byte array (BLOB) for storage
        byte[] imageByteArray = bitmapToByteArray(bitmap);


        SQLiteDatabase db = this.getWritableDatabase();


        // Prepare the content values to insert into the PageTable
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_PAGE_NUMBER, pageNumber);  // Save page number
        contentValues.put(COLUMN_JOURNAL_ID, journalId);    // Save the journal ID
        contentValues.put(COLUMN_BACKGROUND_IMAGE, imageByteArray);  // Save image as BLOB


        // Insert into PageTable
        long rowId = db.insert(PAGE_TABLE, null, contentValues);


        if (rowId != -1) {
            Log.d("Database", "New page added successfully with Page Number: " + pageNumber);
        } else {
            Log.e("Database", "Failed to add new page.");
        }
    }


    public void addNewPageByJournalName(String journalName, int pageNumber, Bitmap backgroundImage) {
        // Retrieve the journal ID based on the journal name
        int journalId = getJournalIdByName(journalName);


        if (journalId == -1) {
            // Journal not found, handle the error (e.g., show a message)
            Log.e("DatabaseError", "Journal with name " + journalName + " not found.");
            return;  // Exit the method if the journal doesn't exist
        }


        // Now we can safely add the page since we have a valid journal_id
        addNewPage(journalId, pageNumber, backgroundImage);  // Call the method that adds the page
    }


    @SuppressLint("Range")
    public int getJournalIdByName(String journalName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                "JournalTable",          // Table to query
                new String[]{"id"},      // Columns to return (we only need the "id")
                "name = ?",              // WHERE clause
                new String[]{journalName}, // The journal name to look for
                null,                    // Group by
                null,                    // Having
                null                     // Order by
        );


        int journalId = -1;  // Default value in case the journal is not found


        if (cursor != null && cursor.moveToFirst()) {
            journalId = cursor.getInt(cursor.getColumnIndex("id"));  // Get the journal id
            cursor.close();  // Don't forget to close the cursor!
        }


        return journalId;  // Return the journal id or -1 if not found
    }




    @SuppressLint("Range")
    public boolean isPageExist(int pageNumber, String journalName) {
        SQLiteDatabase db = this.getReadableDatabase();


        // First, get the journal_id from the JournalTable based on journal_name
        Cursor journalCursor = db.query(
                "JournalTable",              // Table to query
                new String[]{"id"},          // Columns to retrieve (we just need the "id" which is the journal_id)
                "name = ?",                 // WHERE clause to filter by journal_name
                new String[]{journalName},  // Arguments for the WHERE clause (the journal_name)
                null,                        // GROUP BY (not needed here)
                null,                        // HAVING (not needed here)
                null                         // ORDER BY (not needed here)
        );


        int journalId = -1;  // Default value to indicate no journal found


        // Check if we got a result for the journal name
        if (journalCursor != null && journalCursor.moveToFirst()) {
            journalId = journalCursor.getInt(journalCursor.getColumnIndex("id")); // Get the journal_id
        }


        if (journalCursor != null) {
            journalCursor.close(); // Close the cursor
        }


        // If journal_id was not found, return false (page can't exist without a valid journal)
        if (journalId == -1) {
            return false;
        }


        // Now, check if a page with the given page_number exists for this journal_id
        Cursor pageCursor = db.query(
                "PageTable",                  // Table to query
                new String[]{"id"},            // We only need the "id" (primary key) of the page
                "page_number = ? AND journal_id = ?", // WHERE clause (filter by page_number and journal_id)
                new String[]{String.valueOf(pageNumber), String.valueOf(journalId)},  // Arguments for WHERE clause
                null,                          // GROUP BY
                null,                          // HAVING
                null                           // ORDER BY
        );


        boolean pageExists = false;


        // Check if we got a result (meaning the page exists)
        if (pageCursor != null && pageCursor.moveToFirst()) {
            pageExists = true;  // If the cursor has a result, the page exists
        }


        if (pageCursor != null) {
            pageCursor.close(); // Close the cursor
        }


        return pageExists;  // Return whether the page exists or not
    }


}


