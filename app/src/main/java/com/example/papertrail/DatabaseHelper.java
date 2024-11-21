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

    private static final String DATABASE_NAME = "testdb.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "PAGE";
    public static final String COLUMN_ID = "PAGE_NUMBER";
    public static final String COLUMN_IMAGE = "PAGE_IMAGE";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY, " +
                COLUMN_IMAGE + " BLOB)";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void saveImageToDatabase(int pageNumber, Bitmap bitmap) {
        byte[] imageByteArray = bitmapToByteArray(bitmap);
        SQLiteDatabase db = this.getWritableDatabase();

        // Check if the page already exists
        Cursor cursor = db.query(TABLE_NAME, null, COLUMN_ID + " = ?",
                new String[]{String.valueOf(pageNumber)}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            // Page already exists, so update it instead of inserting
            ContentValues contentValues = new ContentValues();
            contentValues.put(COLUMN_IMAGE, imageByteArray);
            db.update(TABLE_NAME, contentValues, COLUMN_ID + " = ?", new String[]{String.valueOf(pageNumber)});
        } else {
            // Page doesn't exist, insert new row
            ContentValues contentValues = new ContentValues();
            contentValues.put(COLUMN_ID, pageNumber);  // Use the page number as the ID
            contentValues.put(COLUMN_IMAGE, imageByteArray);
            db.insert(TABLE_NAME, null, contentValues);
        }

        if (cursor != null) {
            cursor.close();  // Don't forget to close the cursor!
        }
    }


    public byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);  // You can use JPEG if needed
        return byteArrayOutputStream.toByteArray();
    }

    public Bitmap getImageFromDatabase(int pageNumber) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_NAME,            // Table name
                new String[]{COLUMN_IMAGE}, // Column to retrieve (image column)
                COLUMN_ID + " = ?",    // WHERE clause: Page number
                new String[]{String.valueOf(pageNumber)}, // Selection args (the page number)
                null,                  // Group by (optional)
                null,                  // Having (optional)
                null                   // Order by (optional)
        );

        if (cursor != null && cursor.moveToFirst()) {
            // Retrieve the column index safely
            int imageColumnIndex = cursor.getColumnIndex("PAGE_IMAGE");

            if (imageColumnIndex != -1) {
                byte[] imageByteArray = cursor.getBlob(imageColumnIndex);
                // Now you can work with the image byte array
            } else {
                Log.e("DatabaseError", "PAGE_IMAGE column not found.");
            }
            cursor.close();
        } else {
            Log.e("DatabaseError", "No records found in the PAGE table.");
        }

        // Return null if the image wasn't found
        return null;
    }



}
