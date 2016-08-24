package com.example.elliothawkins.wristnote;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link NoteListFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link NoteListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NoteListFragment extends Fragment {
    ListView m_lvNotes;
    TextView m_tvMain;
    NoteStruct[] m_nsNotes;
    private OnFragmentInteractionListener mListener;

    private boolean m_enableHighlight = false;
    private int m_highlightedIndex = 0;

    public NoteListFragment() {
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
        View fragmentView = inflater.inflate(R.layout.fragment_note_list, container, false);
        m_tvMain = (TextView) fragmentView.findViewById(R.id.main_textview);
        m_lvNotes = (ListView) fragmentView.findViewById(R.id.main_lvNotes);

        loadNoteList();

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
            return;
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

    public void loadNoteList(){
        Context loadContext = getActivity();
        if(loadContext == null){
            //Can't load notes if context doesn't exist.
            return;
        }

        NoteSQLHelper sqlHelper = new NoteSQLHelper(loadContext);
        m_nsNotes = sqlHelper.getNotes();
        sqlHelper.close();

        if(m_nsNotes != null) {
            //Hide loading message
            m_tvMain.setVisibility(View.INVISIBLE);
            //TODO - cleaner way of assigning click listener
            NoteListAdapter adapter = new NoteListAdapter(getActivity(), m_nsNotes, (INoteClickListener)getActivity());
            m_lvNotes.setAdapter(adapter);
            adapter.setHighlightEnabled(m_enableHighlight);
            adapter.setHighlighted(m_highlightedIndex);
            m_lvNotes.setVisibility(View.VISIBLE);
        } else {
            m_lvNotes.setVisibility(View.INVISIBLE);
            m_tvMain.setText(R.string.main_no_notes);
            m_tvMain.setVisibility(View.VISIBLE);
        }
    }

    public NoteStruct getNoteAtIndex(int index){
        if(m_lvNotes.getAdapter() != null){
           return (NoteStruct) m_lvNotes.getAdapter().getItem(index);
        }

        return null;
    }

    public void setHighlightedNote(int index){
        m_highlightedIndex = index;
        if(m_lvNotes.getAdapter() != null){
            ((NoteListAdapter) m_lvNotes.getAdapter()).setHighlighted(index);
        }
    }

    public void setHighlightEnabled(boolean enabled){
        m_enableHighlight = enabled;
        if(m_lvNotes.getAdapter() != null){
            ((NoteListAdapter) m_lvNotes.getAdapter()).setHighlightEnabled(enabled);
        }
    }
}
