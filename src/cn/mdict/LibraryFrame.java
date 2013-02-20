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

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import cn.mdict.mdx.DictPref;
import cn.mdict.mdx.MdxEngine;
import cn.mdict.widgets.LibraryAdapter;
import cn.mdict.widgets.TouchListView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;

public class LibraryFrame extends SherlockFragmentActivity {
    private ListView listview;
    public static String SELECTED_LIB_ID = "SelectedLibId";
    private LibraryAdapter adapter;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.library);
        getSupportActionBar().setTitle(R.string.library);
        listview = (ListView) findViewById(R.id.library_list);
        prepareAdapter();
        listview.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position,
                                    long ID) {
                if (MdxEngine.getSettings().getPrefMultiDictLookkupMode()) {
                    int visibleRow = listview.getFirstVisiblePosition();
                    DictPref dictPref = adapter.getDataSource().getChildDictPrefAtIndex(position);
                    dictPref.setDisabled(!dictPref.isDisabled());
                    adapter.getDataSource().updateChildPref(dictPref);
                    View itemView = listview.getChildAt(position - visibleRow);
                    adapter.updateRowView(itemView, position);
                } else {
                    Intent intent = getIntent().putExtra(SELECTED_LIB_ID, (int) ID);
                    LibraryFrame.this.setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });

        TouchListView tlv = (TouchListView) listview;

        tlv.setDropListener(onDrop);
        //tlv.setRemoveListener(onRemove);

    }

    private TouchListView.DropListener onDrop = new TouchListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            int targetId = (int) adapter.getItemId(from);
            adapter.getDataSource().moveChildToPos(targetId, to);
            adapter.notifyDataSetChanged();
            //        adapter.remove(item);
            //        adapter.insert(item, to);
        }
    };

    private TouchListView.RemoveListener onRemove = new TouchListView.RemoveListener() {
        @Override
        public void remove(int which) {
            int targetId = (int) adapter.getItemId(which);
            adapter.getDataSource().removeChildDictPref(targetId);
            adapter.notifyDataSetChanged();
        }
    };


    @Override
    protected void onPause() {
        MdxEngine.getLibMgr().updateDictPref(adapter.getDataSource());
        MdxEngine.saveEngineSettings();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MdxEngine.refreshDictList();
        prepareAdapter();
        if (MdxEngine.getSettings().getPrefLockRotation())
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

    private void prepareAdapter() {
        adapter = new LibraryAdapter(this,
                MdxEngine.getLibMgr().getRootDictPref(),
                MdxEngine.getSettings().getPrefLastDictId());
        listview.setAdapter(adapter);
        listview.invalidate();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        if (MdxEngine.getSettings().getPrefMultiDictLookkupMode()) {
            if (adapter.getDataSource().hasEnabledChild()) {
                Intent intent = getIntent().putExtra(SELECTED_LIB_ID, DictPref.kRootDictPrefId);
                LibraryFrame.this.setResult(RESULT_OK, intent);
            } else if (adapter.getDataSource().getChildCount() > 0) {
                Toast.makeText(this, R.string.at_least_select_one_dict, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        finish();
        return;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MdxEngine.setupEnv(getApplicationContext());

        getSupportMenuInflater().inflate(R.menu.library_frame_option_menu, menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItem = menu.findItem(R.id.dict_mode);
        if (menuItem != null) {
            SubMenu subMenu = menuItem.getSubMenu();
            if (subMenu != null) {
                int dictModeId = MdxEngine.getSettings().getPrefMultiDictLookkupMode() ? R.id.multi_dict_mode : R.id.single_dict_mode;
                MenuItem dictModeItem = subMenu.findItem(dictModeId);
                if (dictModeItem != null)
                    dictModeItem.setChecked(true);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.multi_dict_mode || item.getItemId() == R.id.single_dict_mode) {
            boolean unionMode = (item.getItemId() == R.id.multi_dict_mode);
            MdxEngine.getSettings().setPrefMultiDictLookupMode(unionMode);
            adapter.getDataSource().setUnionGroup(unionMode);
            adapter.notifyDataSetChanged();
            invalidateOptionsMenu();
            return true;
        }
        return false;
    }
}
