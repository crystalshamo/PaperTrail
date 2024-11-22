package com.example.papertrail;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DBHelper extends SQLiteOpenHelper {

    private static String DB_PATH = "/data/data/com.example.papertrail/databases/";
    private static String DB_NAME = "PaperTrail";
    private SQLiteDatabase db;
    private Context myContext;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, 1);
        this.myContext = context;
    }

    public void createDataBase() throws IOException {
        boolean dbExist = checkDataBase();

        if(dbExist) {
            // do nothing
        } else {
            this.getReadableDatabase();
            try {
                copyDataBase();
            } catch (IOException e) {
                throw new Error("Error copying database");
            }
        }
    }

    private boolean checkDataBase () {
        SQLiteDatabase checkDB = null;
        try {
            String myPath = DB_PATH + DB_NAME;
            checkDB = SQLiteDatabase.openDatabase(myPath,null, SQLiteDatabase.OPEN_READONLY);
        } catch (SQLException e){
            // database doesnt exist yet.
        }
        if (checkDB != null) {
            checkDB.close();
        }
        return checkDB != null ? true : false;
    }
    private void copyDataBase() throws IOException {
        // Open the input stream to your database file in the assets folder
        InputStream myInput = myContext.getAssets().open(DB_NAME + ".db");

        // Path to the newly created empty database in the device's file system
        String outFileName = DB_PATH + DB_NAME;

        // Open the output stream to write the database file
        OutputStream myOutput = new FileOutputStream(outFileName);

        // Buffer for data transfer
        byte[] buffer = new byte[1024];
        int length;

        // Read from the input and write to the output
        while ((length = myInput.read(buffer)) > 0) {
            myOutput.write(buffer, 0, length);
        }

        // Ensure all data is flushed and streams are closed
        myOutput.flush();
        myOutput.close();
        myInput.close();
    }
    public void openDataBase() throws SQLException {
        String myPath = DB_PATH + DB_NAME;
        db = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY );
    }

    public synchronized void close() {
        if (db != null) db.close();
        super.close();

    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
