package com.example.papertrail;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static String DB_PATH = "/data/data/com.example.papertrail/databases/";
    private static String DB_NAME = "database5";
    private SQLiteDatabase db;
    private Context myContext;
    private static String JOURNAL_TABLE_NAME = "JournalTable";
    private static String JOURNAL_NAME_COL = "name";

    public static final String PAGE_TABLE = "PageTable";
    public static final String COLUMN_PAGE_NUMBER = "page_number";
    public static final String COLUMN_BACKGROUND_IMAGE = "background_image";
    public static final String COLUMN_JOURNAL_ID = "journal_id";

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
            db = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE);
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
    }

    public Bitmap getPageImageFromDatabase(String journalName, int pageNumber) {
        SQLiteDatabase db = this.getReadableDatabase();
        int journalId = getJournalIdByName(journalName);

        if (journalId == -1) {
            return null;
        }

        Cursor cursor = db.query(
                "PageTable",
                new String[]{"background_image"},
                "journal_id = ? AND page_number = ?",
                new String[]{String.valueOf(journalId), String.valueOf(pageNumber)},
                null,
                null,
                null
        );

        Bitmap bitmap = null;

        if (cursor != null && cursor.moveToFirst()) {
            int imageColumnIndex = cursor.getColumnIndex("background_image");
            if (imageColumnIndex != -1) {
                byte[] imageByteArray = cursor.getBlob(imageColumnIndex);
                bitmap = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.length);
            }
            cursor.close();
        }

        return bitmap;
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

    public void saveImageToPageTable(String journalName, int pageNumber, Bitmap bitmap) {
        int journalId = getJournalIdByName(journalName);

        if (journalId == -1) {
            return;
        }

        byte[] imageByteArray = bitmapToByteArray(bitmap);

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put("page_number", pageNumber);
        contentValues.put("journal_id", journalId);
        contentValues.put("background_image", imageByteArray);

        if (isPageExist(pageNumber, journalName)) {
            int rowsUpdated = db.update(
                    "PageTable",
                    contentValues,
                    "journal_id = ? AND page_number = ?",
                    new String[]{String.valueOf(journalId), String.valueOf(pageNumber)}
            );
        } else {
            long result = db.insert("PageTable", null, contentValues);
        }

        db.close();
    }

    public byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    public void addNewPage(int journalId, int pageNumber, Bitmap bitmap) {
        byte[] imageByteArray = bitmapToByteArray(bitmap);

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_PAGE_NUMBER, pageNumber);
        contentValues.put(COLUMN_JOURNAL_ID, journalId);
        contentValues.put(COLUMN_BACKGROUND_IMAGE, imageByteArray);

        long rowId = db.insert(PAGE_TABLE, null, contentValues);

        db.close();
    }

    public void addNewPageByJournalName(String journalName, int pageNumber, Bitmap backgroundImage) {
        int journalId = getJournalIdByName(journalName);

        if (journalId == -1) {
            return;
        }

        addNewPage(journalId, pageNumber, backgroundImage);
    }

    @SuppressLint("Range")
    public int getJournalIdByName(String journalName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                "JournalTable",
                new String[]{"id"},
                "name = ?",
                new String[]{journalName},
                null,
                null,
                null
        );

        int journalId = -1;

        if (cursor != null && cursor.moveToFirst()) {
            journalId = cursor.getInt(cursor.getColumnIndex("id"));
            cursor.close();
        }

        return journalId;
    }

    @SuppressLint("Range")
    public boolean isPageExist(int pageNumber, String journalName) {
        SQLiteDatabase db = this.getReadableDatabase();

        int journalId = getJournalIdByName(journalName);

        Cursor pageCursor = db.query(
                "PageTable",
                new String[]{"id"},
                "page_number = ? AND journal_id = ?",
                new String[]{String.valueOf(pageNumber), String.valueOf(journalId)},
                null,
                null,
                null
        );

        boolean pageExists = false;

        // If the cursor contains results, the page exists
        if (pageCursor != null && pageCursor.moveToFirst()) {
            pageExists = true;
        }

        // Close the cursor to avoid memory leaks
        if (pageCursor != null) {
            pageCursor.close();
        }

        return pageExists;  // Return true if the page exists, false otherwise
    }

    public boolean hasPagesGreaterThan(int currentPageNumber, String journalName) {
        SQLiteDatabase db = this.getReadableDatabase();

        int journalIdByName = getJournalIdByName(journalName);
        String query = "SELECT COUNT(*) FROM PageTable WHERE journal_id = ? AND page_number > ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(journalIdByName), String.valueOf(currentPageNumber)});

        if (cursor != null) {
            cursor.moveToFirst();
            int count = cursor.getInt(0);
            cursor.close();

            return count > 0;
        }

        return false;
    }

    public boolean hasPagesLessThan(int currentPageNumber, String journalName) {
        SQLiteDatabase db = this.getReadableDatabase();

        int journalIdByName = getJournalIdByName(journalName);
        String query = "SELECT COUNT(*) FROM PageTable WHERE journal_id = ? AND page_number < ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(journalIdByName), String.valueOf(currentPageNumber)});

        if (cursor != null) {
            cursor.moveToFirst();
            int count = cursor.getInt(0);
            cursor.close();

            return count > 0;
        }

        return false;
    }
}

