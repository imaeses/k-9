package com.fsck.k9.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.imaeses.squeaky.R;
import com.imaeses.squeaky.K9;
import com.fsck.k9.activity.K9ActivityCommon.K9ActivityMagic;
import com.fsck.k9.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;


public class K9ListActivity extends ActionBarActivity implements K9ActivityMagic {

    private K9ActivityCommon mBase;
    private ListView mListView;
    private int mContentView = R.layout.k9_list_fragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR);
        mBase = K9ActivityCommon.newInstance(this);
        setContentView( mContentView );
        mListView = ( ListView )findViewById( android.R.id.list );
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        mBase.preDispatchTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void setupGestureDetector(OnSwipeGestureListener listener) {
        mBase.setupGestureDetector(listener);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Shortcuts that work no matter what is selected
        if (K9.useVolumeKeysForListNavigationEnabled() &&
                (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {

            final ListView listView = getListView();

            int currentPosition = listView.getSelectedItemPosition();
            if (currentPosition == AdapterView.INVALID_POSITION || listView.isInTouchMode()) {
                currentPosition = listView.getFirstVisiblePosition();
            }

            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && currentPosition > 0) {
                listView.setSelection(currentPosition - 1);
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN &&
                    currentPosition < listView.getCount()) {
                listView.setSelection(currentPosition + 1);
            }

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Swallow these events too to avoid the audible notification of a volume change
        if (K9.useVolumeKeysForListNavigationEnabled() &&
                (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }
    
    public void registerContentView( int contentView ) {
    	mContentView = contentView;
    }
    
    public ListView getListView() {
    	return mListView;
    }
    
    public void setListAdapter( ListAdapter listAdapter ) {
    	mListView.setAdapter( listAdapter );
    }
    
}
