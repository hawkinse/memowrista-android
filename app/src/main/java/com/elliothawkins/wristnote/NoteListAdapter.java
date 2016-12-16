package com.elliothawkins.wristnote;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Calendar;

/**
 * Created by Elliot Hawkins on 7/15/2016.
 */
public class NoteListAdapter extends BaseAdapter {
    private Context mContext;
    private LayoutInflater mInflater;

    private INoteClickListener m_clickedListener;
    private NoteStruct[] mNotes;

    private boolean m_enableHighlight = false;
    private int m_highlightedIndex = 0;

    public NoteListAdapter(Activity parentActivity, NoteStruct[] notes, INoteClickListener listener){
        mNotes = notes;
        mContext = parentActivity;
        mInflater = (LayoutInflater)mContext.getSystemService(mContext.LAYOUT_INFLATER_SERVICE);
        m_clickedListener = listener;
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
        final View rowView = mInflater.inflate(R.layout.listitem_note, null);

        TextView tvNoteTitle = (TextView)rowView.findViewById(R.id.note_listitem_name);
        TextView tvNoteTimestamp = (TextView)rowView.findViewById(R.id.note_listitem_timestamp);

        Calendar modifiedDateCal = Calendar.getInstance();
        modifiedDateCal.setTimeInMillis(mNotes[position].timestamp);
        String modifiedDateString = "Last modified on " + modifiedDateCal.get(Calendar.YEAR) + "/" + modifiedDateCal.get(Calendar.MONTH) + "/" + modifiedDateCal.get(Calendar.DAY_OF_MONTH) +
                                    " at " + modifiedDateCal.get(Calendar.HOUR) + ":" + modifiedDateCal.get(Calendar.MINUTE) + ":" + modifiedDateCal.get(Calendar.SECOND) + " " +
                                    (modifiedDateCal.get(Calendar.AM_PM) == 1 ? "PM" : "AM");

        tvNoteTitle.setText(mNotes[position].title.isEmpty() ? mContext.getString(R.string.default_note_title) : mNotes[position].title);
        tvNoteTimestamp.setText(modifiedDateString/* + " ID: " + mNotes[position].ID*/);

        if((m_enableHighlight) && (position == m_highlightedIndex)){
            //Set highlight
            rowView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.colorMenuHighlight));

            //Add height if material design is supported
            if(Build.VERSION.SDK_INT >= 21) {
                rowView.setElevation(mContext.getResources().getDimension(R.dimen.note_fragment_elevation));
            }
        }

        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                m_clickedListener.OnNoteClicked(mNotes[position], rowView, position);
            }
        });

        return rowView;
    }

    public void setHighlighted(int index){
        m_highlightedIndex = index;
    }

    public void setHighlightEnabled(boolean highlight){
        m_enableHighlight = highlight;
    }
}
