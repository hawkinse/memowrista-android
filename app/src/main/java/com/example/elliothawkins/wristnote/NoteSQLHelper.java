package com.example.elliothawkins.wristnote;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Elliot Hawkins on 7/15/2016.
 */
public class NoteSQLHelper extends SQLiteOpenHelper{
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "WristNote.db";

    public static final String TABLE_NAME = "notes";
    public static final String COLUMN_NAME_ID = "ID";
    public static final String COLUMN_NAME_TITLE = "title";
    public static final String COLUMN_NAME_BODY = "body";
    public static final String COLUMN_NAME_MODIFIED_TIMESTAMP = "modify_date";

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_NAME_ID + " INTEGER PRIMARY KEY NOT NULL," +
                    COLUMN_NAME_TITLE + TEXT_TYPE + COMMA_SEP +
                    COLUMN_NAME_BODY + TEXT_TYPE + COMMA_SEP +
                    COLUMN_NAME_MODIFIED_TIMESTAMP + " INTEGER NOT NULL" +
                    " )";
    private static final String SQL_DROP_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    public NoteSQLHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db){
        db.execSQL(SQL_CREATE_TABLE);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.e("WristNote DB", "Unimplemented db upgrade! From v" + oldVersion + " to " + newVersion);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){
        Log.e("WristNote DB", "Unimplemented db downgrade! From v" + oldVersion + " to " + newVersion);
    }

    public long writeNote(NoteStruct note){
        long toReturn = note.ID;

        if(note.ID > 0){
            updateNote(note);
        } else {
            toReturn = writeNewNote(note);
        }

        return toReturn;
    }

    private long writeNewNote(NoteStruct note){
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_NAME_TITLE, note.title);
        values.put(COLUMN_NAME_BODY, note.body);
        values.put(COLUMN_NAME_MODIFIED_TIMESTAMP, System.currentTimeMillis());

        long id = db.insert(TABLE_NAME, null, values);

        if(id < 1){
            Log.e("WristNote DB", "Database insert failed!");
        }

        Log.w("WristNote DB", "Database insert succeded! Note ID: " + id);

        db.close();

        return id;
    }

    private void updateNote(NoteStruct note){
        SQLiteDatabase db = getReadableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_NAME_TITLE, note.title);
        values.put(COLUMN_NAME_BODY, note.body);
        values.put(COLUMN_NAME_MODIFIED_TIMESTAMP, System.currentTimeMillis());

        String selection = COLUMN_NAME_ID + " LIKE ?";
        String[] selectionArgs = { String.valueOf(note.ID) };

        int count = db.update(TABLE_NAME, values, selection, selectionArgs);

        if(count < 1){
            Log.e("WristNote DB", "Database update failed!");
        } else if (count == 1) {
            Log.w("WristNote DB", "Database update succeeded!");
        } else {
            Log.e("WristNote DB", "Database update updated more than one entry!!");
        }

        db.close();
    }

    public NoteStruct[] getNotes(){
        SQLiteDatabase dbRead = getReadableDatabase();
        ArrayList<NoteStruct> readNotes = new ArrayList<NoteStruct>();
        NoteStruct[] toReturn;

        String[] columns = {
                COLUMN_NAME_ID,
                COLUMN_NAME_TITLE,
                COLUMN_NAME_BODY,
                COLUMN_NAME_MODIFIED_TIMESTAMP
        };

        String sortOrder = COLUMN_NAME_MODIFIED_TIMESTAMP + " DESC";

        Cursor resultCursor = dbRead.query(true, TABLE_NAME, columns, null, null, null, null, sortOrder, null);

        resultCursor.moveToFirst();
        while(!resultCursor.isAfterLast()){
            NoteStruct currentNote = new NoteStruct();
            currentNote.ID = resultCursor.getLong(resultCursor.getColumnIndexOrThrow(COLUMN_NAME_ID));
            currentNote.title = resultCursor.getString(resultCursor.getColumnIndexOrThrow(COLUMN_NAME_TITLE));
            currentNote.body = resultCursor.getString(resultCursor.getColumnIndexOrThrow(COLUMN_NAME_BODY));
            currentNote.timestamp = resultCursor.getLong(resultCursor.getColumnIndexOrThrow(COLUMN_NAME_MODIFIED_TIMESTAMP));
            readNotes.add(currentNote);
            resultCursor.moveToNext();
        }
        resultCursor.close();
        dbRead.close();

        if(readNotes.size() > 0) {
            toReturn = new NoteStruct[readNotes.size()];
            readNotes.toArray(toReturn);
            return toReturn;
        }

        return null;
    }

    public NoteStruct getNote(long getID){
        //TODO - simplify!
        SQLiteDatabase dbRead = getReadableDatabase();
        ArrayList<NoteStruct> readNotes = new ArrayList<NoteStruct>();
        NoteStruct[] toReturn;

        String[] columns = {
                COLUMN_NAME_ID,
                COLUMN_NAME_TITLE,
                COLUMN_NAME_BODY,
                COLUMN_NAME_MODIFIED_TIMESTAMP
        };

        String[] idArg = {
                Long.toString(getID)
        };

        String sortOrder = COLUMN_NAME_MODIFIED_TIMESTAMP + " DESC";

        Cursor resultCursor = dbRead.query(true, TABLE_NAME, columns, COLUMN_NAME_ID + "=?", idArg, null, null, sortOrder, null);

        resultCursor.moveToFirst();
        while(!resultCursor.isAfterLast()){
            NoteStruct currentNote = new NoteStruct();
            currentNote.ID = resultCursor.getLong(resultCursor.getColumnIndexOrThrow(COLUMN_NAME_ID));
            currentNote.title = resultCursor.getString(resultCursor.getColumnIndexOrThrow(COLUMN_NAME_TITLE));
            currentNote.body = resultCursor.getString(resultCursor.getColumnIndexOrThrow(COLUMN_NAME_BODY));
            currentNote.timestamp = resultCursor.getLong(resultCursor.getColumnIndexOrThrow(COLUMN_NAME_MODIFIED_TIMESTAMP));
            readNotes.add(currentNote);
            resultCursor.moveToNext();
        }
        resultCursor.close();
        dbRead.close();

        if(readNotes.size() > 0) {
            toReturn = new NoteStruct[readNotes.size()];
            readNotes.toArray(toReturn);
            return toReturn[0];
        }

        //At this point, we either found nothing or something went wrong
        //Create a new note with an ID of -1 to indicate failiure
        toReturn = new NoteStruct[1];
        toReturn[0] = new NoteStruct();
        toReturn[0].ID = -1;
        return toReturn[0];
    }

    public void deleteNote(long deleteID){
        SQLiteDatabase dbWrite = getWritableDatabase();
        String selection = COLUMN_NAME_ID + " LIKE ?";
        String[] selectionArgs = { String.valueOf(deleteID) };

        dbWrite.delete(TABLE_NAME, selection, selectionArgs);

        dbWrite.close();
    }

    boolean backupDB(String path){
        //TODO - back up database at given path
        boolean bSuccess = false;

        return bSuccess;
    }

    boolean restoreDB(String path){
        //TODO - restore database at given path
        boolean bSuccess = false;
        return bSuccess;
    }
}
