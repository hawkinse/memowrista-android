package com.example.elliothawkins.wristnote;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements INoteClickListener, INotesChangedListener {
    static final int PERMISSION_GRANTED_EXPORT_NOTES = 0;
    static final int PERMISSION_GRANTED_IMPORT_NOTES = 1;
    static final int BACKUP_DESTINATION_REQUEST_CODE = 2;
    static final int RESTORE_DESTINATION_REQUEST_CODE = 3;

    static final String PEBBLE_BINARY_PATH = "WristNote.pbw";

    NoteListFragment m_nlf;
    NoteContentFragment m_ncf;

    int m_lastHighlightedNote = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);


        m_nlf = (NoteListFragment) getSupportFragmentManager().findFragmentById(R.id.list_frag);
        m_ncf = (NoteContentFragment) getSupportFragmentManager().findFragmentById(R.id.content_frag);

        if(usingTabletLayout()){
            m_nlf.setHighlightEnabled(true);

            //Auto-load the first entry if present
            NoteStruct firstNote = m_nlf.getNoteAtIndex(0);
            if(firstNote != null){
                m_ncf.setNote(firstNote);
            }
        }

        Intent pebbleComServiceIntent = new Intent(getBaseContext(), PebbleComService.class);
        startService(pebbleComServiceIntent);

        PebbleComService.setActiveMainActivity(this);

        NoteSQLHelper.registerNotesChangedListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_main_toolbar_menu, menu);

        MenuItem deleteMenuItem = menu.findItem(R.id.action_note_delete);
        if(deleteMenuItem != null){
            deleteMenuItem.setVisible(usingTabletLayout());
        }

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

            case R.id.action_export:
                Toast.makeText(this, "Attempting to export notes!", Toast.LENGTH_SHORT).show();
                //attemptExportNotes();
                openFileForExport();
                return true;

            case R.id.action_import:
                Toast.makeText(this, "Attempting to import notes!", Toast.LENGTH_LONG).show();
                openFileForImport();
                return true;

            case R.id.action_note_delete:
                if(usingTabletLayout()){
                    m_ncf.deleteNote();
                    m_nlf.loadNoteList();
                    //Auto-load the first entry if present
                    NoteStruct firstNote = m_nlf.getNoteAtIndex(0);
                    if(firstNote != null){
                        m_ncf.setNote(firstNote);
                    }
                }
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
        } else {
            NoteSQLHelper.unregisterNotesChangedListener(this);
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        PebbleComService.setActiveMainActivity(this);
        NoteSQLHelper.registerNotesChangedListener(this);
    }

    @Override
    public void onPause(){
        PebbleComService.setActiveMainActivity(null);
        NoteSQLHelper.unregisterNotesChangedListener(this);
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putInt("highlightedIndex", m_lastHighlightedNote);
        // etc.
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        Toast.makeText(this, "Restoring index state!", Toast.LENGTH_LONG).show();
        super.onRestoreInstanceState(savedInstanceState);

        //Restore index from last state
        m_lastHighlightedNote = savedInstanceState.getInt("highlightedIndex");
        if(usingTabletLayout()) {
            NoteStruct note = m_nlf.getNoteAtIndex(m_lastHighlightedNote);
            if(note != null) {
                OnNoteClicked(note, null, m_lastHighlightedNote);
            }
        }
    }

    public void OnNoteClicked(NoteStruct note, View noteView, int listIndex){
        if(usingTabletLayout()){
            m_ncf.writeNote();
            m_nlf.loadNoteList();
            m_ncf.setNote(note);
            //TODO - highlight view!
            //TODO - unhighlight old view!
            //TODO - ensure view is scrolled into view if off screen!
            m_nlf.setHighlightedNote(listIndex);
        } else {
            Intent noteIntent = new Intent(MainActivity.this, NoteActivity.class);
            noteIntent.putExtra("ID", note.ID);
            noteIntent.putExtra("Title", note.title);
            noteIntent.putExtra("Body", note.body);
            startActivity(noteIntent);
        }

        m_lastHighlightedNote = listIndex;
    }

    public void onNotesChanged(){
        m_nlf.loadNoteList();
        m_nlf.setHighlightedNote(0);
        m_lastHighlightedNote = 0;
    }

    private void openFileForExport(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_GRANTED_EXPORT_NOTES);
        } else {
            //Open system folder picker on Lollipop and up.
            //Lower versions of Android have no system file picker. Instead use path /sdcard/Documents/WristNote/backup.xml
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Intent fileIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(fileIntent, BACKUP_DESTINATION_REQUEST_CODE);
            } else {
                if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    File file = new File(Environment.getExternalStorageDirectory().toString() + "/Documents/WristNote", "backup.xml");
                    file.mkdirs();

                    if(file.exists()) {
                        file.delete();
                    } else {
                        try {
                            OutputStream os = new FileOutputStream(file);
                            attemptExportNotes(os);
                            os.close();
                        } catch (Exception ex){
                            Toast.makeText(this, "Something went wrong opening the file!", Toast.LENGTH_LONG);
                            Log.e("WristNote", ex.toString());
                        }
                    }
                }
            }
        }
    }
    //TODO - change to something like "openFileForExport", reuse attemptExportNotes for the actual call to NoteSQLHelper!
    //Attempts to export notes, if we have permission.
    private void attemptExportNotes(OutputStream os){
        /*
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_GRANTED_EXPORT_NOTES);
        } else {
            NoteSQLHelper sqlHelper = new NoteSQLHelper(this);
            boolean bSuccess = sqlHelper.backupDB("backup.xml");
            Toast.makeText(this, "Export notes " + (bSuccess ? "successful" : "failed!"), Toast.LENGTH_LONG).show();
        }
        */
        NoteSQLHelper sqlHelper = new NoteSQLHelper(this);
        //boolean bSuccess = sqlHelper.backupDB(os);
        //Toast.makeText(this, "Export notes " + (bSuccess ? "successful" : "failed!"), Toast.LENGTH_LONG).show();
    }

    //Opens a file picker to select a note to import. If we dont have sufficient permissions, will ask for permission first.
    private void openFileForImport(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_GRANTED_IMPORT_NOTES);
        } else {
            //Open system file picker on Kitkat and up.
            //Lower versions of Android have no system file picker. Instead use path /sdcard/Documents/WristNote/backup.xml
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Intent fileIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                fileIntent.addCategory(Intent.CATEGORY_OPENABLE);
                fileIntent.setType("text/xml");
                startActivityForResult(fileIntent, RESTORE_DESTINATION_REQUEST_CODE);
            } else {
                if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) || Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
                    File file = new File(Environment.getExternalStorageDirectory().toString() + "/Documents/WristNote", "backup.xml");

                    if(file.exists()){
                        try {
                            InputStream is = new FileInputStream(file);
                            attemptImportNotes(is);
                            is.close();
                        } catch (Exception ex){
                            Toast.makeText(this, "Something went wrong opening the file!", Toast.LENGTH_LONG);
                            Log.e("WristNote", ex.toString());
                        }
                    }
                }
            }
        }
    }

    private void attemptImportNotes(InputStream is){
        NoteSQLHelper sqlHelper = new NoteSQLHelper(this);
        boolean bSuccess = sqlHelper.importXmlFromStream(is);
        Toast.makeText(this, "Import notes " + (bSuccess ? "successful" : "failed!"), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        switch (requestCode) {
            case PERMISSION_GRANTED_EXPORT_NOTES:
                if((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    openFileForExport();
                } else {
                    Toast.makeText(this, "WristNote requires storage permissions to store backup files on user storage!", Toast.LENGTH_LONG).show();
                }
                break;
            case PERMISSION_GRANTED_IMPORT_NOTES:
                if((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    openFileForImport();
                } else {
                    Toast.makeText(this, "WristNote requires storage permissions to read backup files on user storage!", Toast.LENGTH_LONG).show();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData){
        if(requestCode == RESTORE_DESTINATION_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            Uri uri = null;
            if(resultData != null){
                uri = resultData.getData();
                try {
                    InputStream is = getContentResolver().openInputStream(uri);
                    attemptImportNotes(is);
                    is.close();
                } catch (Exception ex){
                    Toast.makeText(this, "Something went wrong opening the file!", Toast.LENGTH_LONG);
                    Log.e("WristNote", ex.toString());
                }
            }
        } else if (requestCode == BACKUP_DESTINATION_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            Uri uri = null;
            if(resultData != null){
                uri = resultData.getData();
                try {
                    //Todo - prompt for file name and write file.
                } catch(Exception ex){

                }
            }
        }
    }

    private boolean usingTabletLayout(){
        //Need to explicitly check for a missing view. Checking for the fragment containing the view will fail with rotation!
        boolean bIsTablet = (findViewById(R.id.note_edit_text_title) != null);

        //Update reference to m_ncf
        if(bIsTablet) {
            m_ncf = (NoteContentFragment) getSupportFragmentManager().findFragmentById(R.id.content_frag);
        }

        return bIsTablet;
    }
    private void newNote(){
        //Create a new note
        NoteSQLHelper sqlHelper = new NoteSQLHelper(this);
        NoteStruct newNote = new NoteStruct();
        long id = sqlHelper.writeNote(newNote);
        newNote = sqlHelper.getNote(id);
        sqlHelper.close();

        m_nlf.loadNoteList();

        if(usingTabletLayout()){
            m_ncf.setNote(newNote);
            //m_ncf.writeNote();
            //m_nlf.loadNoteList();
        } else {
            //TODO - find way to directly pass note struct instead of id to reload
            Intent noteIntent = new Intent(MainActivity.this, NoteActivity.class);
            noteIntent.putExtra("ID", newNote.ID);
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
