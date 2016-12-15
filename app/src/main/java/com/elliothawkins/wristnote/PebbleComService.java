package com.elliothawkins.wristnote;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.UUID;

public class PebbleComService extends Service {
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
    static final int MSG_PEBBLE_REQUEST_COM_VERSION = 20;
    static final int MSG_PHONE_SEND_COM_VERSION = 21;

    static final int PEBBLE_COM_VERSION = 0;

    PebbleKit.PebbleDataReceiver dataReciever;
    static long m_currentPebbleEditID = 0;
    NoteStruct[] m_nsNotes;

    private static PebbleComService pebbleComServiceInstance;
    private static MainActivity currentMainActivityInstance;
    private static NoteActivity currentNoteActivityInstance;

    public PebbleComService() {

    }

    @Override
    public void onCreate(){
        super.onCreate();

        dataReciever = new PebbleKit.PebbleDataReceiver(PEBBLE_APP_UUID) {

            @Override
            public void receiveData(Context context, int transaction_id, PebbleDictionary dict) {
                System.out.print("Recieved data!");

                //Alert pebble that message was received
                PebbleKit.sendAckToPebble(context, transaction_id);
                PebbleDictionary responseDict = new PebbleDictionary();
                NoteSQLHelper sqlHelper = null;
                boolean bDataWritten = false;

                if(m_nsNotes == null){
                    loadNoteList();
                }

                if(dict.getInteger(MSG_PEBBLE_REQUEST_COM_VERSION) != null){
                    responseDict.addInt32(MSG_PHONE_SEND_COM_VERSION, PEBBLE_COM_VERSION);
                    bDataWritten = true;
                }

                if (dict.getInteger(MSG_PEBBLE_REQUEST_NOTE_COUNT) != null) {
                    if (m_nsNotes == null) {
                        responseDict.addInt32(MSG_PHONE_SEND_NOTE_COUNT, 0);
                    } else {
                        responseDict.addInt32(MSG_PHONE_SEND_NOTE_COUNT, m_nsNotes.length);
                    }
                    bDataWritten = true;
                }

                Long requestedNoteIDIndex = dict.getInteger(MSG_PEBBLE_REQUEST_NOTE_ID);
                if (requestedNoteIDIndex != null) {
                    if (m_nsNotes == null) {
                        responseDict.addInt32(MSG_PHONE_GENERIC_ERROR, 0);
                    } else {
                        responseDict.addInt32(MSG_PHONE_SEND_NOTE_ID, (int) m_nsNotes[requestedNoteIDIndex.intValue()].ID);
                    }
                    bDataWritten = true;
                }

                Long requestedNoteTitleID = dict.getInteger(MSG_PEBBLE_REQUEST_NOTE_TITLE);
                if (requestedNoteTitleID != null) {
                    if (m_nsNotes == null) {
                        responseDict.addInt32(MSG_PHONE_GENERIC_ERROR, 0);
                    } else {
                        if(sqlHelper == null){
                            sqlHelper = new NoteSQLHelper(context);
                        }
                        NoteStruct note = sqlHelper.getNote(requestedNoteTitleID);
                        responseDict.addString(MSG_PHONE_SEND_NOTE_TITLE, note.title);
                    }
                    bDataWritten = true;
                }

                Long requestedNoteBodyID = dict.getInteger(MSG_PEBBLE_REQUEST_NOTE_BODY);
                if (requestedNoteBodyID != null) {
                    if (m_nsNotes == null) {
                        responseDict.addInt32(MSG_PHONE_GENERIC_ERROR, 0);
                    } else {
                        if(sqlHelper == null){
                            sqlHelper = new NoteSQLHelper(context);
                        }
                        NoteStruct note = sqlHelper.getNote(requestedNoteBodyID);
                        responseDict.addString(MSG_PHONE_SEND_NOTE_BODY, note.body);
                    }
                    bDataWritten = true;
                }

                Long requestedNoteTimeID = dict.getInteger(MSG_PEBBLE_REQUEST_NOTE_TIME);
                if (requestedNoteTimeID != null) {
                    if (m_nsNotes == null) {
                        responseDict.addInt32(MSG_PHONE_GENERIC_ERROR, 0);
                    } else {
                        if(sqlHelper == null){
                            sqlHelper = new NoteSQLHelper(context);
                        }
                        NoteStruct note = sqlHelper.getNote(requestedNoteTimeID);
                        //TODO - find better way to send time to pebble!
                        responseDict.addInt32(MSG_PHONE_SEND_NOTE_TIME, (int) note.timestamp);
                    }
                    bDataWritten = true;
                }

                Long requestedNoteDeleteID = dict.getInteger(MSG_PEBBLE_DELETE_NOTE);
                if (requestedNoteDeleteID != null) {
                    if (m_nsNotes == null) {
                        responseDict.addInt32(MSG_PHONE_GENERIC_ERROR, 0);
                    } else {

                        //Close the current note activity if we're deleting the note it's showing before actually deleting
                        if(currentNoteActivityInstance != null){
                            currentNoteActivityInstance.finish();
                        }

                        if(sqlHelper == null){
                            sqlHelper = new NoteSQLHelper(context);
                        }
                        sqlHelper.deleteNote(requestedNoteDeleteID);
                        loadNoteList();
                        responseDict.addInt32(MSG_PHONE_UPDATE, 0);

                        if(currentMainActivityInstance != null){
                            currentMainActivityInstance.loadNoteList();
                        }
                    }
                    bDataWritten = true;
                }

                Long requestedNewNote = dict.getInteger(MSG_PEBBLE_NEW_NOTE);
                if (requestedNewNote != null) {
                    NoteStruct note = new NoteStruct();
                    note.title = "New note";
                    note.body = "Use the edit menu options to set title and content!";
                    if(sqlHelper == null){
                        sqlHelper = new NoteSQLHelper(context);
                    }
                    sqlHelper.writeNote(note);
                    loadNoteList();
                    responseDict.addInt32(MSG_PHONE_UPDATE, 0);
                    bDataWritten = true;

                    if(currentMainActivityInstance != null){
                        currentMainActivityInstance.loadNoteList();
                    }
                }

                Long requestedEditID = dict.getInteger(MSG_PEBBLE_SET_EDIT_ID);
                if (requestedEditID != null) {
                    m_currentPebbleEditID = requestedEditID;
                }

                String replacementNoteTitleText = dict.getString(MSG_PEBBLE_REPLACE_TITLE);
                if (replacementNoteTitleText != null) {
                    if(sqlHelper == null){
                        sqlHelper = new NoteSQLHelper(context);
                    }
                    NoteStruct note = sqlHelper.getNote(m_currentPebbleEditID);
                    note.title = replacementNoteTitleText;
                    sqlHelper.writeNote(note);
                    loadNoteList();
                    responseDict.addInt32(MSG_PHONE_UPDATE, 0);
                    bDataWritten = true;

                    if(currentMainActivityInstance != null){
                        currentMainActivityInstance.loadNoteList();
                    }

                    if(currentNoteActivityInstance != null){
                        currentNoteActivityInstance.updateNoteContent(m_currentPebbleEditID);
                    }
                }

                String replacementNoteBodyText = dict.getString(MSG_PEBBLE_REPLACE_BODY);
                if (replacementNoteBodyText != null) {
                    if(sqlHelper == null){
                        sqlHelper = new NoteSQLHelper(context);
                    }
                    NoteStruct note = sqlHelper.getNote(m_currentPebbleEditID);
                    note.body = replacementNoteBodyText;
                    sqlHelper.writeNote(note);
                    loadNoteList();
                    responseDict.addInt32(MSG_PHONE_UPDATE, 0);
                    bDataWritten = true;

                    //Update main to bump edited note to top
                    if(currentMainActivityInstance != null){
                        currentMainActivityInstance.loadNoteList();
                    }

                    if(currentNoteActivityInstance != null){
                        currentNoteActivityInstance.updateNoteContent(m_currentPebbleEditID);
                    }
                }

                String appendNoteBodyText = dict.getString(MSG_PEBBLE_APPEND_BODY);
                if (appendNoteBodyText != null) {
                    if(sqlHelper == null){
                        sqlHelper = new NoteSQLHelper(context);
                    }
                    NoteStruct note = sqlHelper.getNote(m_currentPebbleEditID);
                    note.body = note.body + "\n" + appendNoteBodyText;
                    sqlHelper.writeNote(note);
                    loadNoteList();
                    responseDict.addInt32(MSG_PHONE_UPDATE, 0);
                    bDataWritten = true;

                    //Update main to bump edited note to top
                    if(currentMainActivityInstance != null){
                        currentMainActivityInstance.loadNoteList();
                    }

                    if(currentNoteActivityInstance != null){
                        currentNoteActivityInstance.updateNoteContent(m_currentPebbleEditID);
                    }
                }

                if (bDataWritten) {
                    PebbleKit.sendDataToPebble(context, PEBBLE_APP_UUID, responseDict);
                }

                if(sqlHelper != null) {
                    sqlHelper.close();
                }
            }
        };
        PebbleKit.registerReceivedDataHandler(getApplicationContext(), dataReciever);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        //Update current instance
        pebbleComServiceInstance = this;

        PebbleKit.registerReceivedDataHandler(getApplicationContext(), dataReciever);

        //Return sticky to signify that this service will run continuously
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy(){
    }

    @Override
    public void onTrimMemory(int level){
        //Only take action if memory is actually running low
        if(level > TRIM_MEMORY_UI_HIDDEN) {
            //Free memory by releasing loaded notes
            m_nsNotes = null;
        }
    }

    private void loadNoteList(){
        NoteSQLHelper sqlHelper = new NoteSQLHelper(this);
        m_nsNotes = sqlHelper.getNotes();
        sqlHelper.close();
    }

    //Only one instance of an Android service can exist at once, so static update methods to pebble are safe

    //Tells the pebble to reload its note list data
    public static void sendPebbleUpdate(){
        if(pebbleComServiceInstance != null) {
            pebbleComServiceInstance.loadNoteList();
            PebbleDictionary responseDict = new PebbleDictionary();
            responseDict.addInt32(MSG_PHONE_UPDATE, 0);
            PebbleKit.sendDataToPebble(pebbleComServiceInstance.getBaseContext(), PEBBLE_APP_UUID, responseDict);
        }
    }

    public static void setActiveMainActivity(MainActivity current){
        currentMainActivityInstance = current;
    }

    public static void setActiveNoteActivity(NoteActivity current){
        currentNoteActivityInstance = current;
    }
}
