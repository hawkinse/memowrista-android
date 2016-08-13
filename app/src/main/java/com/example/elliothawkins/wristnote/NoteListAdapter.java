package com.example.elliothawkins.wristnote;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by Elliot Hawkins on 7/15/2016.
 */
public class NoteListAdapter extends BaseAdapter {
    private Context mContext;
    private LayoutInflater mInflater;

    //TODO - replace with struct?
    private NoteStruct[] mNotes;

    public NoteListAdapter(Activity parentActivity, NoteStruct[] notes){
        mNotes = notes;
        mContext = parentActivity;
        mInflater = (LayoutInflater)mContext.getSystemService(mContext.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount(){
        return mNotes.length;
    }

    @Override
    public Object getItem(int position){
        return mNotes[position];
    }

    @Override
    public long getItemId(int position){
        //Should this return the note ID, or the index?
        return mNotes[position].ID;
    }

    public View getView(final int position, View contentView, ViewGroup parent){
        View rowView = mInflater.inflate(R.layout.listitem_note, null);
        TextView tvNoteTitle = (TextView)rowView.findViewById(R.id.note_listitem_name);
        TextView tvNoteTimestamp = (TextView)rowView.findViewById(R.id.note_listitem_timestamp);

        Calendar modifiedDateCal = Calendar.getInstance();
        modifiedDateCal.setTimeInMillis(mNotes[position].timestamp);
        String modifiedDateString = "Last modified on " + modifiedDateCal.get(Calendar.YEAR) + "/" + modifiedDateCal.get(Calendar.MONTH) + "/" + modifiedDateCal.get(Calendar.DAY_OF_MONTH) +
                                    " at " + modifiedDateCal.get(Calendar.HOUR) + ":" + modifiedDateCal.get(Calendar.MINUTE) + ":" + modifiedDateCal.get(Calendar.SECOND) + " " +
                                    (modifiedDateCal.get(Calendar.AM_PM) == 1 ? "PM" : "AM");
        //Test of functionality
        tvNoteTitle.setText(mNotes[position].title);
        tvNoteTimestamp.setText(modifiedDateString + " ID: " + mNotes[position].ID);

        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                //Toast.makeText(mContext, "Selected note ID: " + mNotes[position].ID, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(mContext, NoteActivity.class);
                intent.putExtra("ID", mNotes[position].ID);
                intent.putExtra("Title", mNotes[position].title);
                intent.putExtra("Body", mNotes[position].body);
                mContext.startActivity(intent);
            }
        });

        return rowView;
    }
}
