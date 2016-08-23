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

    private NoteContentFragment m_ncf;

    private EditText m_etTitle;
    private EditText m_etBody;

    private long mID = -1;
    private String mTitle = "";
    private String mBody = "";

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
        m_ncf = (NoteContentFragment) getSupportFragmentManager().findFragmentById(R.id.content_frag);

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
            NoteStruct viewedNote = new NoteStruct();
            viewedNote.ID = mID;
            viewedNote.title = mTitle;
            viewedNote.body = mBody;
            m_ncf.setNote(viewedNote);
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
                m_ncf.deleteNote();
                finish();
                return true;

            //Handle back button, since it for some reason is not handled by parent in default case
            case android.R.id.home:
                //Toast.makeText(this, "Returning to main activity (or whatever is UP)...", Toast.LENGTH_SHORT).show();
                m_ncf.writeNote();
                finish();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPause(){
        PebbleComService.setActiveNoteActivity(null);
        super.onPause();
    }

    @Override
    public void onResume(){
        super.onResume();
        PebbleComService.setActiveNoteActivity(this);
    }

    public void updateNoteContent(long updateID){
        //For now, pass through to fragment
        m_ncf.updateNoteContent(updateID);
    }
}
