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

public class MainActivity extends AppCompatActivity implements INoteClickListener {
    static final String PEBBLE_BINARY_PATH = "WristNote.pbw";

    NoteListFragment m_nlf;
    NoteContentFragment m_ncf;

    /*
    ListView m_lvNotes;
    TextView m_tvMain;
    NoteStruct[] m_nsNotes;
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);


        m_nlf = (NoteListFragment) getSupportFragmentManager().findFragmentById(R.id.list_frag);
        m_ncf = (NoteContentFragment) getSupportFragmentManager().findFragmentById(R.id.content_frag);

        //makeDummyNoteList();
        //loadNoteList();

        //TODO - add settings toggle for this!
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
            m_nlf.loadNoteList();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        PebbleComService.setActiveMainActivity(this);
    }

    @Override
    public void onPause(){
        PebbleComService.setActiveMainActivity(null);
        super.onPause();
    }

    public void OnNoteClicked(NoteStruct note, View noteView, int listIndex){
        if(usingTabletLayout()){
            m_ncf.writeNote();
            m_nlf.loadNoteList();
            m_ncf.setNote(note);
            //TODO - highlight view!
            //TODO - unhighlight old view!
            //TODO - ensure view is scrolled into view if off screen!
        } else {
            Intent noteIntent = new Intent(MainActivity.this, NoteActivity.class);
            noteIntent.putExtra("ID", note.ID);
            noteIntent.putExtra("Title", note.title);
            noteIntent.putExtra("Body", note.body);
            startActivity(noteIntent);
        }

    }

    private boolean usingTabletLayout(){
        boolean bIsTablet =  (getSupportFragmentManager().findFragmentById(R.id.content_frag) != null);

        //Update reference to m_ncf
        if(bIsTablet) {
            m_ncf = (NoteContentFragment) getSupportFragmentManager().findFragmentById(R.id.content_frag);
        }

        return bIsTablet;
    }
    private void newNote(){
        //TODO - separate code paths for tablet and phone. Make relative to fragment instead of activity!
        //TODO - Instead of passing in -1, create new note in DB and pass that!
        if(usingTabletLayout()){
            m_ncf.setNote(new NoteStruct());
            m_ncf.writeNote();
            m_nlf.loadNoteList();
            //Todo - get current Note ID out of the note content view so we can keep track of what should be highlighted
        } else {
            Intent noteIntent = new Intent(MainActivity.this, NoteActivity.class);
            noteIntent.putExtra("ID", -1);
            startActivity(noteIntent);
        }
    }

    private void installWatchApp(){
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

    public void loadNoteList() {
        //Pass through to fragment
        m_nlf.loadNoteList();
    }
}
