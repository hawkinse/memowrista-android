package com.example.elliothawkins.wristnote;

import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

public class NoteActivity extends AppCompatActivity {

    private EditText m_etTitle;
    private EditText m_etBody;

    private long mID = -1;
    private String mTitle = "";
    private String mBody = "";
    private boolean mDeleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.note_toolbar);
        setSupportActionBar(myToolbar);

        //Add a back button to the toolbar
        final ActionBar toolbarAsActionBar = getSupportActionBar();
        toolbarAsActionBar.setDisplayHomeAsUpEnabled(true);

        m_etTitle = (EditText) findViewById(R.id.note_edit_text_title);
        m_etBody = (EditText) findViewById(R.id.note_edit_text_body);

        //Hides the action bar during copy/paste if running below Android 6
        //Otherwise it gets pushed down, looks ugly
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            //TODO - Replace with custom copy/paste actionbar. There's still a brief period where both bars exist after onDestroyActionMode
            m_etBody.setCustomSelectionActionModeCallback(new android.view.ActionMode.Callback() {

                @Override
                public boolean onPrepareActionMode(android.view.ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public void onDestroyActionMode(android.view.ActionMode mode) {

                    toolbarAsActionBar.show();
                }

                @Override
                public boolean onCreateActionMode(android.view.ActionMode mode, Menu menu) {
                    //Returning false from this method prevents creation of the copy/paste menu
                    toolbarAsActionBar.hide();
                    return true;
                }

                @Override
                public boolean onActionItemClicked(android.view.ActionMode mode, MenuItem item) {
                    return false;
                }
            });

            m_etTitle.setCustomSelectionActionModeCallback(new android.view.ActionMode.Callback() {

                @Override
                public boolean onPrepareActionMode(android.view.ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public void onDestroyActionMode(android.view.ActionMode mode) {

                    toolbarAsActionBar.show();
                }

                @Override
                public boolean onCreateActionMode(android.view.ActionMode mode, Menu menu) {
                    //Returning false from this method prevents creation of the copy/paste menu
                    toolbarAsActionBar.hide();
                    return true;
                }

                @Override
                public boolean onActionItemClicked(android.view.ActionMode mode, MenuItem item) {
                    return false;
                }
            });
        }

        //Check if ID is valid
        mID = getIntent().getLongExtra("ID", -1);
        if(mID >= 0){
            //ID is valid. Load title and body
            mTitle = getIntent().getStringExtra("Title");
            mBody = getIntent().getStringExtra("Body");
            m_etTitle.setText(mTitle);
            m_etBody.setText(mBody);
        }

        PebbleComService.setActiveNoteActivity(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_note_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_note_delete:
                //Toast.makeText(this, "Deleting note!", Toast.LENGTH_SHORT).show();
                deleteNote();
                finish();
                return true;

            //Handle back button, since it for some reason is not handled by parent in default case
            case android.R.id.home:
                //Toast.makeText(this, "Returning to main activity (or whatever is UP)...", Toast.LENGTH_SHORT).show();
                writeNote();
                finish();
                return true;

            case R.id.action_note_debug_open_on_pebble:
                debug_SendToPebble();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPause(){
        //If this note hasn't been deleted, ensure it gets saved!
        if(!mDeleted) {
            //Toast.makeText(this, "Note activity paused. Saving note!", Toast.LENGTH_LONG).show();
            writeNote();
        }
        PebbleComService.setActiveNoteActivity(null);
        super.onPause();
    }

    @Override
    public void onResume(){
        super.onResume();
        PebbleComService.setActiveNoteActivity(this);
        updateNoteContent();
    }

    private void writeNote(){
        if(mID > 0 && m_etTitle.getText().toString().equals(mTitle) && m_etBody.getText().toString().equals(mBody)){
            //Toast.makeText(this, "Note is unchanged! Not updating or inserting.", Toast.LENGTH_SHORT).show();
        } else {
            NoteSQLHelper sqlHelper = new NoteSQLHelper(this);
            NoteStruct newNote = new NoteStruct();
            newNote.ID = mID;
            newNote.title = m_etTitle.getText().toString();
            newNote.body = m_etBody.getText().toString();

            if(newNote.title.isEmpty()){
                newNote.title = getString(R.string.default_note_title);
            }
            //Toast.makeText(this, "Writing note!", Toast.LENGTH_SHORT).show();
            mID = sqlHelper.writeNote(newNote);
            sqlHelper.close();

            //Send update to pebble app
            PebbleComService.sendPebbleUpdate();
        }
    }

    private void deleteNote(){
        if(mID > 0) {
            NoteSQLHelper sqlHelper = new NoteSQLHelper(this);
            sqlHelper.deleteNote(mID);
            sqlHelper.close();
        }
        mDeleted = true;

        //Send update to pebble app
        PebbleComService.sendPebbleUpdate();
    }

    private void debug_SendToPebble(){
        final Intent i = new Intent("com.getpebble.action.SEND_NOTIFICATION");
        final Map data = new HashMap();
        data.put("title", m_etTitle.getText().toString());
        data.put("body", m_etBody.getText().toString());
        final JSONObject jsonData = new JSONObject(data);
        final String notificationData = new JSONArray().put(jsonData).toString();

        i.putExtra("messageType", "PEBBLE_ALERT");
        i.putExtra("sender", "PebbleKit Android");
        i.putExtra("notificationData", notificationData);
        sendBroadcast(i);
    }

    public void updateNoteContent(long updateID){
        if(updateID == mID){
            updateNoteContent();
        }
    }
    private void updateNoteContent(){
        if(mID > 0) {
            NoteSQLHelper sqlHelper = new NoteSQLHelper(this);
            NoteStruct note = sqlHelper.getNote(mID);

            if (note != null && note.ID > -1) {
                if ((!m_etTitle.getText().equals(note.title)) || (!m_etBody.getText().equals(note.body))) {
                    m_etTitle.setText(note.title);
                    m_etBody.setText(note.body);
                }
            }

            sqlHelper.close();
        }
    }
}
