package com.elliothawkins.wristnote;

import android.view.View;

/**
 * Created by hawkins on 8/23/16.
 */
public interface INoteClickListener {
    void OnNoteClicked(NoteStruct note, View noteView, int listIndex);
}
