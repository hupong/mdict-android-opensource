/*
 * Copyright (C) 2012. Rayman Zhang <raymanzhang@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package cn.mdict;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;
import cn.mdict.mdx.DictBookmarkRef;
import cn.mdict.mdx.DictEntry;
import cn.mdict.mdx.MdxEngine;
import cn.mdict.widgets.BookmarkAdapter;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public abstract class BookmarkActivity extends SherlockListActivity implements BookmarkAdapter.ItemCheckedChangeListener, SimpleActionModeCallbackAgent.ActionItemClickListener {
    public final static String headwordName="Headword";
    public final static String entryNoName="EntryNo";
    public final static String dictIdName="LibId";

    private SimpleActionModeCallbackAgent actionModeAgent;

    public abstract DictBookmarkRef getBookmarkMgr();
    public abstract int getLayoutResId();
    public abstract int getOptionMenuResId();
    public abstract int getItemOptionMenuResId();
    public abstract  BookmarkAdapter getBookmarkAdapter();

    @Override
    public void onConfigurationChanged (Configuration newConfig){
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (MdxEngine.getSettings().getPrefLockRotation())
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResId());
        setupEnv(getIntent());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(getOptionMenuResId(), menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    private void deleteSelectedItem(){
        SparseBooleanArray states= getBookmarkAdapter().getCheckStates();
        DictBookmarkRef bookmarkMgr=getBookmarkMgr();
        for( int i=states.size()-1; i>=0; --i){
            int pos=states.keyAt(i);
            boolean value=states.valueAt(i);
            if ( value ){
                bookmarkMgr.remove(pos);
            }
        }
        Toast.makeText(this, String.format(getResources().getString(R.string.entries_deleted), states.size()), Toast.LENGTH_LONG).show();
        getBookmarkAdapter().notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.clear_all:
                AlertDialog dialog=AddonFuncUnt.buildConfirmDialog(this,
                        R.string.confirm_clear_all_record, 0,
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public  void onClick(android.content.DialogInterface dialogInterface, int i) {
                                getBookmarkMgr().clear();
                                getBookmarkAdapter().notifyDataSetChanged();
                            }}, null);
                dialog.show();
                return true;
            case R.id.delete:
                deleteSelectedItem();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override
    protected void onListItemClick (ListView l, View v, int position, long id){
        if (position < 0 || position >= MdxEngine.getHistMgr().getCount()) {
            return;
        }
        DictEntry entry = getBookmarkMgr().getEntryByIndex((int) id);
        Intent intent=getIntent();
        intent.putExtra(headwordName, entry.getHeadword());
        intent.putExtra(entryNoName, entry.getEntryNo());
        intent.putExtra(dictIdName, entry.getDictId());
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
        getBookmarkAdapter().notifyDataSetChanged();
    }

    @Override
    public void OnItemCheckedChanged(int position, boolean checked) {
        if ( getBookmarkAdapter().getCheckItemCount()>0 ){
            if ( actionModeAgent.getActionMode()==null ){
                startActionMode(actionModeAgent);
            }
            actionModeAgent.getActionMode().setTitle(String.format(getResources().getString(R.string.entries_selected), getBookmarkAdapter().getCheckItemCount()));
        }else{
            if ( actionModeAgent.getActionMode()!=null )
                actionModeAgent.getActionMode().finish();
        }
    }


    protected void setupEnv(Intent intent){
        ListView listView = getListView();
        //listView.setChoiceMode(ListView.);
        getBookmarkAdapter().setItemCheckedChangeListener(this);
        listView.setAdapter(getBookmarkAdapter());
        //adapter.notifyDataSetChanged();

        actionModeAgent=new SimpleActionModeCallbackAgent(getItemOptionMenuResId(), this);

    }

    @Override
    protected void onPause(){
    	MdxEngine.saveEngineSettings();
    	super.onPause();
    }

    @Override
    public void onBackPressed() {
        finish();
        return;
    }

}

