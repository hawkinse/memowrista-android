package com.elliothawkins.wristnote;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class NoteContentFragment extends Fragment {
    private EditText m_etTitle;
    private EditText m_etBody;

    private long mID = -1;
    private String mTitle = "";
    private String mBody = "";
    private boolean mDeleted = false;

    private OnFragmentInteractionListener mListener;

    public NoteContentFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragmentView = inflater.inflate(R.layout.fragment_note_content, container, false);

        m_etTitle = (EditText) fragmentView.findViewById(R.id.note_edit_text_title);
        m_etBody = (EditText) fragmentView.findViewById(R.id.note_edit_text_body);

        //Update note list when focus is changed on either text box. This will allow side panel to more quickly show title/position updates in tablet UI
        m_etTitle.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus) {
                    writeNote();
                }
            }
        });

        m_etBody.setOnFocusChangeListener(new View.OnFocusChangeListener(){
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus) {
                    writeNote();
                }
            }
        });

        return fragmentView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            /*
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
                    */
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public void onPause(){
        //If this note hasn't been deleted, ensure it gets saved!
        if(!mDeleted) {
            writeNote();
        }
        super.onPause();
    }

    @Override
    public void onResume(){
        super.onResume();
        updateNoteContent();
    }

    public void setNote(NoteStruct note){
        mID = note.ID;
        mTitle = note.title;
        mBody = note.body;
        m_etTitle.setText(mTitle);
        m_etBody.setText(mBody);
    }

    //Was private before moving to fragment
    public void writeNote(){
        if(mID > 0 && m_etTitle.getText().toString().equals(mTitle) && m_etBody.getText().toString().equals(mBody)){
            //Toast.makeText(getContext(), "Note is unchanged! Not updating or inserting.", Toast.LENGTH_SHORT).show();
        } else {
            NoteSQLHelper sqlHelper = new NoteSQLHelper(getActivity());
            NoteStruct newNote = new NoteStruct();
            newNote.ID = mID;
            newNote.title = m_etTitle.getText().toString();
            newNote.body = m_etBody.getText().toString();

            //Toast.makeText(this, "Writing note!", Toast.LENGTH_SHORT).show();
            mID = sqlHelper.writeNote(newNote);
            sqlHelper.close();

            //Send update to pebble app
            PebbleComService.sendPebbleUpdate();
        }
    }

    //Was private before moving to fragment
    public void deleteNote(){
        if(mID > 0) {
            NoteSQLHelper sqlHelper = new NoteSQLHelper(getActivity());
            sqlHelper.deleteNote(mID);
            sqlHelper.close();
        }
        mDeleted = true;

        //Send update to pebble app
        PebbleComService.sendPebbleUpdate();
    }

    public void updateNoteContent(long updateID){
        if(updateID == mID){
            updateNoteContent();
        }
    }

    //Was private before moving to fragment
    public void updateNoteContent(){
        if(mID > 0) {
            NoteSQLHelper sqlHelper = new NoteSQLHelper(getActivity());
            NoteStruct note = sqlHelper.getNote(mID);

            if (note != null && note.ID > -1) {
                if ((!m_etTitle.getText().equals(note.title)) || (!m_etBody.getText().equals(note.body))) {
                    mTitle = note.title;
                    mBody = note.body;
                    m_etTitle.setText(note.title);
                    m_etBody.setText(note.body);
                }
            }

            sqlHelper.close();
        }
    }
}
