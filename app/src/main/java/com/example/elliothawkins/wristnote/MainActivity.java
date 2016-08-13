package com.example.elliothawkins.wristnote;

import android.app.ActionBar;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    static final String PEBBLE_BINARY_PATH = "WristNote.pbw";
    static final UUID PEBBLE_APP_UUID = UUID.fromString("aff8fea2-a0c1-47b9-b16a-26ef53b45eae");
    static final int MSG_PEBBLE_REQUEST_NOTE_COUNT = 0;
    static final int MSG_PEBBLE_REQUEST_NOTE_ID = 1;
    static final int MSG_PEBBLE_REQUEST_NOTE_TITLE = 2;
    static final int MSG_PEBBLE_REQUEST_NOTE_BODY = 3;
    static final int MSG_PEBBLE_REQUEST_NOTE_DATE = 4;
    static final int MSG_PEBBLE_REQUEST_NOTE_TIME = 5;
    static final int MSG_PEBBLE_DELETE_NOTE = 6;
    static final int MSG_PEBBLE_REPLACE_TITLE = 7;
    static final int MSG_PEBBLE_REPLACE_BODY = 8;
    static final int MSG_PEBBLE_APPEND_BODY = 9;
    static final int MSG_PHONE_SEND_NOTE_COUNT = 10;
    static final int MSG_PHONE_SEND_NOTE_ID = 11;
    static final int MSG_PHONE_SEND_NOTE_TITLE = 12;
    static final int MSG_PHONE_SEND_NOTE_BODY = 13;
    static final int MSG_PHONE_SEND_NOTE_DATE = 14;
    static final int MSG_PHONE_SEND_NOTE_TIME = 15;
    static final int MSG_PHONE_UPDATE = 16;
    static final int MSG_PHONE_GENERIC_ERROR = 17;
    static final int MSG_PEBBLE_NEW_NOTE = 18;
    static final int MSG_PEBBLE_SET_EDIT_ID = 19;

    ListView m_lvNotes;
    TextView m_tvMain;
    NoteStruct[] m_nsNotes;
    PebbleKit.PebbleDataReceiver dataReciever;

    static long m_currentPebbleEditID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);
        m_tvMain = (TextView) findViewById(R.id.main_textview);
        m_lvNotes = (ListView) findViewById(R.id.main_lvNotes);

        //makeDummyNoteList();
        loadNoteList();

        /*
        dataReciever = new PebbleKit.PebbleDataReceiver(PEBBLE_APP_UUID){

            @Override
            public void receiveData(Context context, int transaction_id, PebbleDictionary dict){
                System.out.print("Recieved data!");

                //Alert pebble that message was received
                PebbleKit.sendAckToPebble(context, transaction_id);
                PebbleDictionary responseDict = new PebbleDictionary();
                boolean bDataWritten = false;

                if(dict.getInteger(MSG_PEBBLE_REQUEST_NOTE_COUNT) != null){
                    if(m_nsNotes == null) {
                        responseDict.addInt32(MSG_PHONE_SEND_NOTE_COUNT, 0);
                    } else {
                        responseDict.addInt32(MSG_PHONE_SEND_NOTE_COUNT, m_nsNotes.length);
                    }
                    bDataWritten = true;
                }

                Long requestedNoteIDIndex = dict.getInteger(MSG_PEBBLE_REQUEST_NOTE_ID);
                if(requestedNoteIDIndex != null){
                    if(m_nsNotes == null) {
                        responseDict.addInt32(MSG_PHONE_GENERIC_ERROR, 0);
                    } else {
                        responseDict.addInt32(MSG_PHONE_SEND_NOTE_ID, (int)m_nsNotes[requestedNoteIDIndex.intValue()].ID);
                    }
                    bDataWritten = true;
                }

                Long requestedNoteTitleID = dict.getInteger(MSG_PEBBLE_REQUEST_NOTE_TITLE);
                if(requestedNoteTitleID != null){
                    if(m_nsNotes == null) {
                        responseDict.addInt32(MSG_PHONE_GENERIC_ERROR, 0);
                    } else {
                        NoteSQLHelper sqlHelper = new NoteSQLHelper(context);
                        NoteStruct note = sqlHelper.getNote(requestedNoteTitleID);
                        responseDict.addString(MSG_PHONE_SEND_NOTE_TITLE, note.title);
                    }
                    bDataWritten = true;
                }

                Long requestedNoteBodyID = dict.getInteger(MSG_PEBBLE_REQUEST_NOTE_BODY);
                if(requestedNoteBodyID != null){
                    if(m_nsNotes == null) {
                        responseDict.addInt32(MSG_PHONE_GENERIC_ERROR, 0);
                    } else {
                        NoteSQLHelper sqlHelper = new NoteSQLHelper(context);
                        NoteStruct note = sqlHelper.getNote(requestedNoteBodyID);
                        responseDict.addString(MSG_PHONE_SEND_NOTE_BODY, note.body);
                    }
                    bDataWritten = true;
                }

                Long requestedNoteTimeID = dict.getInteger(MSG_PEBBLE_REQUEST_NOTE_TIME);
                if(requestedNoteTimeID != null){
                    if(m_nsNotes == null) {
                        responseDict.addInt32(MSG_PHONE_GENERIC_ERROR, 0);
                    } else {
                        NoteSQLHelper sqlHelper = new NoteSQLHelper(context);
                        NoteStruct note = sqlHelper.getNote(requestedNoteTimeID);
                        //TODO - find better way to send time to pebble!
                        responseDict.addInt32(MSG_PHONE_SEND_NOTE_TIME, (int)note.timestamp);
                    }
                    bDataWritten = true;
                }

                Long requestedNoteDeleteID = dict.getInteger(MSG_PEBBLE_DELETE_NOTE);
                if(requestedNoteDeleteID != null){
                    if(m_nsNotes == null) {
                        responseDict.addInt32(MSG_PHONE_GENERIC_ERROR, 0);
                    } else {
                        NoteSQLHelper sqlHelper = new NoteSQLHelper(context);
                        sqlHelper.deleteNote(requestedNoteDeleteID);
                        loadNoteList();
                        responseDict.addInt32(MSG_PHONE_UPDATE, 0);
                    }
                    bDataWritten = true;
                }

                Long requestedNewNote = dict.getInteger(MSG_PEBBLE_NEW_NOTE);
                if(requestedNewNote != null){
                    NoteStruct note = new NoteStruct();
                    note.title = "New note";
                    note.body = "Use the edit menu options to set title and content!";
                    NoteSQLHelper sqlHelper = new NoteSQLHelper(context);
                    sqlHelper.writeNote(note);
                    loadNoteList();
                    responseDict.addInt32(MSG_PHONE_UPDATE, 0);
                    bDataWritten = true;
                }

                Long requestedEditID = dict.getInteger(MSG_PEBBLE_SET_EDIT_ID);
                if(requestedEditID != null){
                    m_currentPebbleEditID = requestedEditID;
                }

                String replacementNoteTitleText = dict.getString(MSG_PEBBLE_REPLACE_TITLE);
                if(replacementNoteTitleText != null){
                    NoteSQLHelper sqlHelper = new NoteSQLHelper(context);
                    NoteStruct note = sqlHelper.getNote(m_currentPebbleEditID);
                    note.title = replacementNoteTitleText;
                    sqlHelper.writeNote(note);
                    loadNoteList();
                    responseDict.addInt32(MSG_PHONE_UPDATE, 0);
                    bDataWritten = true;
                }

                String replacementNoteBodyText = dict.getString(MSG_PEBBLE_REPLACE_BODY);
                if(replacementNoteBodyText != null){
                    NoteSQLHelper sqlHelper = new NoteSQLHelper(context);
                    NoteStruct note = sqlHelper.getNote(m_currentPebbleEditID);
                    note.body = replacementNoteBodyText;
                    sqlHelper.writeNote(note);
                    loadNoteList();
                    responseDict.addInt32(MSG_PHONE_UPDATE, 0);
                    bDataWritten = true;
                }

                String appendNoteBodyText = dict.getString(MSG_PEBBLE_APPEND_BODY);
                if(appendNoteBodyText != null){
                    NoteSQLHelper sqlHelper = new NoteSQLHelper(context);
                    NoteStruct note = sqlHelper.getNote(m_currentPebbleEditID);
                    note.body = note.body + "\n" + appendNoteBodyText;
                    sqlHelper.writeNote(note);
                    loadNoteList();
                    responseDict.addInt32(MSG_PHONE_UPDATE, 0);
                    bDataWritten = true;
                }

                if(bDataWritten){
                    PebbleKit.sendDataToPebble(context, PEBBLE_APP_UUID, responseDict);
                }
            }
        };
        */
        Intent pebbleComServiceIntent = new Intent(getBaseContext(), PebbleComService.class);
        startService(pebbleComServiceIntent);

        PebbleComService.setActiveMainActivity(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_main_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_new_note:
                newNote();
                return true;

            case R.id.action_install_watchapp:
                installWatchApp();
                return true;

            case R.id.action_show_about:
                showAboutMessage();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean focus){
        if(focus) {
            loadNoteList();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        PebbleComService.setActiveMainActivity(this);
        //PebbleKit.registerReceivedDataHandler(getApplicationContext(), dataReciever);
    }

    @Override
    public void onPause(){
        PebbleComService.setActiveMainActivity(null);
        super.onPause();
        //TODO - find unregisterReceiver()!
    }

    private void newNote(){
        //Toast.makeText(this, "Creating a new note!", Toast.LENGTH_SHORT).show();
        Intent noteIntent = new Intent(MainActivity.this, NoteActivity.class);
        noteIntent.putExtra("ID", -1);
        startActivity(noteIntent);
    }

    private void installWatchApp(){
        //Toast.makeText(this, "Attempting to install pebble watch app!", Toast.LENGTH_SHORT).show();
        boolean isConnected = PebbleKit.isWatchConnected(this);
        Toast.makeText(this, "Pebble " + (isConnected ? "is" : "is not") + " connected!", Toast.LENGTH_LONG).show();
        if(isConnected){
            try {
                // Read .pbw from assets
                Intent intent = new Intent(Intent.ACTION_VIEW);
                File file = new File(getApplicationContext().getExternalFilesDir(null), PEBBLE_BINARY_PATH);
                InputStream is = getApplicationContext().getResources().getAssets().open(PEBBLE_BINARY_PATH);
                OutputStream os = new FileOutputStream(file);
                byte[] pbw = new byte[is.available()];
                is.read(pbw);
                os.write(pbw);
                is.close();
                os.close();

                // Install via Pebble Android app
                intent.setDataAndType(Uri.fromFile(file), "application/pbw");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "App install failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showAboutMessage(){
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.about_title);
        adb.setIcon(R.drawable.ic_action_about);
        adb.setMessage(R.string.about_body);
        adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Do nothing
            }
        });
        adb.show();
    }

    private void makeDummyNoteList(){
        ArrayList<NoteStruct> dummyNotes = new ArrayList<NoteStruct>();

        for(int i = 0; i < 100; i++){
            NoteStruct dummyNote = new NoteStruct();
            dummyNote.ID = i;
            dummyNote.title = "Dummy note #" + i;
            dummyNote.body = "This is the body of a dummy note! Hopefully this will soon be replaced with ACTUAL CONTENT! XD";
            dummyNotes.add(dummyNote);
        }

        if(dummyNotes.size() > 0) {
            //Hide loading message
            m_tvMain.setVisibility(View.INVISIBLE);

            NoteStruct[] convertedNotes = new NoteStruct[dummyNotes.size()];
            dummyNotes.toArray(convertedNotes);
            m_lvNotes.setAdapter(new NoteListAdapter(this, convertedNotes));
        } else {
            m_tvMain.setText(R.string.main_no_notes);
        }
    }

    public void loadNoteList(){
        ArrayList<NoteStruct> dummyNotes = new ArrayList<NoteStruct>();
        NoteSQLHelper sqlHelper = new NoteSQLHelper(this);
        m_nsNotes = sqlHelper.getNotes();
        sqlHelper.close();

        if(m_nsNotes != null) {
            //Hide loading message
            m_tvMain.setVisibility(View.INVISIBLE);
            m_lvNotes.setAdapter(new NoteListAdapter(this, m_nsNotes));
            m_lvNotes.setVisibility(View.VISIBLE);
        } else {
            m_lvNotes.setVisibility(View.INVISIBLE);
            m_tvMain.setText(R.string.main_no_notes);
            m_tvMain.setVisibility(View.VISIBLE);
        }
    }
}
