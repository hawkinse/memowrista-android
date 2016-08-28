package com.example.elliothawkins.wristnote;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

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

    private static ArrayList<INotesChangedListener> m_changedListeners = new ArrayList<INotesChangedListener>();

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

        updateNotesChangedListeners();

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

        updateNotesChangedListeners();
    }

    public boolean exportXmlToStream(OutputStream os){
        return exportXmlToStream(os, null);
    }

    public boolean exportXmlToStream(OutputStream os, Vector<Integer> IDs){
        boolean bSuccess = false;

        //Check that external app storage is writable.
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {

            NoteStruct[] notes = getNotes();
            if (notes != null) {
                StringBuilder xmlBuilder = new StringBuilder();
                xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                xmlBuilder.append("<notes>\n");
                for (NoteStruct note : notes) {
                    if(IDs == null || IDs.contains(note.ID)) {
                        xmlBuilder.append("  <note>\n");
                        xmlBuilder.append("    <title>" + note.title + "</title>\n");
                        xmlBuilder.append("    <body>" + note.body + "</body>\n");
                        xmlBuilder.append("    <timestamp>" + note.timestamp + "</timestamp>\n");
                        xmlBuilder.append("  </note>\n");
                    }
                }
                xmlBuilder.append("</notes>\n");

                try {

                    byte[] writeBuffer = xmlBuilder.toString().getBytes();
                    os.write(writeBuffer);

                    bSuccess = true;

                } catch (IOException ex){
                    return bSuccess;
                }
            }
        }

        return bSuccess;
    }

    boolean importXmlFromStream(InputStream is){
        boolean bSuccess = false;

        //Check that external app storage is readable.
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) || Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {

            ArrayList<NoteStruct> readNotes = new ArrayList<>();
            try {
                //TODO - parse xml and insert into database
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(/*fis*/is, null);

                int eventType = parser.getEventType();
                NoteStruct currentNote = null;
                Vector<String> tagStack = new Vector<String>();
                //String currentTag = null;
                String currentText = null;
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    switch (eventType) {
                        case XmlPullParser.START_TAG:
                            //currentTag = parser.getName();
                            tagStack.add(parser.getName());
                            if (/*currentTag.*/tagStack.lastElement().equalsIgnoreCase("note")) {
                                // create a new instance of employee
                                currentNote = new NoteStruct();
                            }
                            break;

                        case XmlPullParser.TEXT:
                            currentText = parser.getText();
                            break;

                        case XmlPullParser.END_TAG:
                            if (/*currentTag*/tagStack.lastElement().equalsIgnoreCase("note")) {
                                readNotes.add(currentNote);
                                currentNote = null;
                            } else if (/*currentTag*/tagStack.lastElement().equalsIgnoreCase("title")){
                                currentNote.title = currentText;
                            } else if (/*currentTag*/tagStack.lastElement().equalsIgnoreCase("body")){
                                currentNote.body = currentText;
                            } else if (/*currentTag*/tagStack.lastElement().equalsIgnoreCase("timestamp")){
                                currentNote.timestamp = Long.parseLong(currentText);
                            }

                            tagStack.remove(tagStack.lastElement());
                            break;
                        default:
                            break;
                    }
                    eventType = parser.next();
                }
            } catch (Exception ex) {
                Log.e("WristNote", ex.toString());
                return false;
            }

            if(readNotes != null){
                for(NoteStruct note : readNotes){
                    writeNote(note);
                }

                bSuccess = true;
            }
        }

        updateNotesChangedListeners();

        return bSuccess;
    }

    public static void registerNotesChangedListener(INotesChangedListener listener){
        m_changedListeners.add(listener);
    }

    public static void unregisterNotesChangedListener(INotesChangedListener listener){
        m_changedListeners.remove(listener);
    }

    private void updateNotesChangedListeners(){
        for(INotesChangedListener listener : m_changedListeners){
            listener.onNotesChanged();
        }
    }
}
