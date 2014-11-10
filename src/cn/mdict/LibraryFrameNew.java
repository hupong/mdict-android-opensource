/*
 * Copyright (C) 2014. Rayman Zhang <raymanzhang@gmail.com>
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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cn.mdict.mdx.DictPref;
import cn.mdict.mdx.MdxEngine;
import cn.mdict.utils.IOUtil;
import cn.mdict.views.DraggableGridView;
import cn.mdict.views.OnDraggedOutOfContainerListener;
import cn.mdict.views.OnGroupListener;
import cn.mdict.views.OnItemCheckedListener;
import cn.mdict.views.OnRearrangeListener;

public class LibraryFrameNew extends SherlockFragmentActivity {
    DraggableGridView dgv;
    private LinearLayout layoutDictGroup;
    private DraggableGridView dgvDictGroup;
    private EditText txtDictGroupName;
    private DictPref openedDictGroup = null;
    private View openedDictGroupView = null;
    static Random random = new Random();
    public static String SELECTED_LIB_ID = "SelectedLibId";
    private DictPref rootDictRef = MdxEngine.getLibMgr().getRootDictPref();
    private DictPref defaultDictGroup = null;//rootDictRef.getChildDictPrefAtIndex(0);
    List<DictPref> dictPrefList = new ArrayList<DictPref>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.library_new);
        layoutDictGroup = (LinearLayout) findViewById(R.id.layoutDictGroup);
        dgvDictGroup = (DraggableGridView) findViewById(R.id.dgvDictGroup);
        txtDictGroupName = (EditText) findViewById(R.id.txtDictGroupName);
        for (int i = 0; i < rootDictRef.getChildCount(); i++) {
            DictPref childPref = rootDictRef.getChildDictPrefAtIndex(i);
            if (childPref.getDictId() == DictPref.kDefaultGrpId) {
                defaultDictGroup = childPref;
                break;
            }
        }

        dgv = (DraggableGridView) findViewById(R.id.dgv);
        dgv.setGroupingAllowed(true);
        getSupportActionBar().setTitle(R.string.library);
        layoutDictGroup.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                exitDictGrupView();
                return false;
            }
        });
        buildDictListView(true);
        setListeners();

    }


    private void setListeners() {
        dgv.setOnItemCheckedListener(new OnItemCheckedListener() {
            @Override
            public void OnItemChecked(int position) {
                DictPref dictPref = dictPrefList.get(position);
                dictPref.setDisabled(!dictPref.isDisabled());
                if (dictPref.isDictGroup()) {
                    for (int i = 0; i < dictPref.getChildCount(); i++) {
                        DictPref childPref = dictPref.getChildDictPrefAtIndex(i);
                        childPref.setDisabled(dictPref.isDisabled());
                        dictPref.updateChildPref(childPref);
                    }
                }
                if (dictPref.isDictGroup())
                    rootDictRef.updateChildPref(dictPref);
                else {
                    defaultDictGroup.updateChildPref(dictPref);
                    rootDictRef.updateChildPref(defaultDictGroup);
                    if (!MdxEngine.getSettings().getPrefMultiDictLookkupMode()) {
                        Intent intent = getIntent().putExtra(SELECTED_LIB_ID, dictPref.getDictId());
                        LibraryFrameNew.this.setResult(RESULT_OK, intent);
                        finish();
                    }
                }

            }
        });
        dgv.setOnRearrangeListener(new OnRearrangeListener() {
            public void onRearrange(int oldIndex, int newIndex) {
                DictPref dictPref = dictPrefList.get(oldIndex);
                //Calculate new dict index
                int newDictIndex = calculateNewDictIndex(newIndex, dictPref.isDictGroup());
                dictPrefList.remove(oldIndex);
                dictPrefList.add(newIndex, dictPref);
                if (dictPref.isDictGroup()) {
                    rootDictRef.moveChildToPos(dictPref.getDictId(), newDictIndex);
                } else {
                    defaultDictGroup.moveChildToPos(dictPref.getDictId(), newDictIndex);
                    rootDictRef.updateChildPref(defaultDictGroup);
                }
            }
        });
        dgv.setOnGroupListener(new OnGroupListener() {
            @Override
            public void onGroup(int srcIndex, int targetIndex) {
                DictPref srcPref = dictPrefList.get(srcIndex);
                if (srcPref.isDictGroup()) {
                    MiscUtils.showMessageDialog(LibraryFrameNew.this, getText(R.string.msg_cannot_add_group).toString(), "Warning");
                    return;
                } else {
                    DictPref targetPref = dictPrefList.get(targetIndex);
                    if (targetPref.isDictGroup()) {
                        if (isDictExistsInGroup(srcPref.getDictId(), targetPref)) {
                            MiscUtils.showMessageDialog(LibraryFrameNew.this, getText(R.string.msg_dict_exists_in_group).toString(), "Warning");
                            return;
                        }
                        dictPrefList.remove(srcIndex);
                        //rootDictRef.removeChildDictPref(srcPref.getDictId());
                        targetPref.addChildPref(srcPref);
                        rootDictRef.updateChildPref(targetPref);
                        srcPref.setDisabled(true);
                        defaultDictGroup.updateChildPref(srcPref);
                        rootDictRef.updateChildPref(defaultDictGroup);
                    } else {
                        DictPref dictPref = MdxEngine.getLibMgr().createDictPref();
                        int newDictIndex = calculateNewDictIndex(targetIndex, dictPref.isDictGroup());
                        //dictPref.setDictId(random.nextInt(DictPref.kMaxDictPrefId - 100));
                        dictPref.setDisabled(false);
                        dictPref.setDictName(getText(R.string.un_named_dict_group).toString());

                        dictPrefList.remove(srcPref);
                        dictPrefList.remove(targetPref);
                        dictPrefList.add(targetIndex, dictPref);
                        //rootDictRef.removeChildDictPref(srcPref.getDictId());
                        //rootDictRef.removeChildDictPref(targetPref.getDictId());

                        dictPref.setDictGroup(true);
                        dictPref.setUnionGroup(true);
                        dictPref.addChildPref(targetPref);
                        dictPref.addChildPref(srcPref);
                        dictPref.updateChildPref(targetPref);
                        dictPref.updateChildPref(srcPref);

                        rootDictRef.addChildPref(dictPref);
                        rootDictRef.moveChildToPos(dictPref.getDictId(), newDictIndex);
                        rootDictRef.updateChildPref(dictPref);
                        //disable dict in default group
                        srcPref.setDisabled(true);
                        defaultDictGroup.updateChildPref(srcPref);
                        targetPref.setDisabled(true);
                        defaultDictGroup.updateChildPref(targetPref);
                        rootDictRef.updateChildPref(defaultDictGroup);
                        //MiscUtils.showMessageDialog(LibraryFrameNew.this, String.valueOf(dictPref.getDictId()) + "-" + dictPref.getDictName(), "Info");
                    }
                    buildDictListView(true);
                }
            }
        });
        dgv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long ID) {
                DictPref dictPref = dictPrefList.get(position);
                if (dictPref.isDictGroup()) {
                    showDictsByGroup(view, dictPref);
                }
            }
        });

        dgvDictGroup.setOnItemCheckedListener(new OnItemCheckedListener() {
            @Override
            public void OnItemChecked(int position) {
                DictPref dictPref = openedDictGroup.getChildDictPrefAtIndex(position);
                dictPref.setDisabled(!dictPref.isDisabled());
                openedDictGroup.updateChildPref(dictPref);
                //defaultDictGroup.updateChildPref(dictPref);
                rootDictRef.updateChildPref(openedDictGroup);
                //rootDictRef.updateChildPref(defaultDictGroup);
            }
        });

        dgvDictGroup.setOnRearrangeListener(new OnRearrangeListener() {
            public void onRearrange(int oldIndex, int newIndex) {
                DictPref dictPref = openedDictGroup.getChildDictPrefAtIndex(oldIndex);
                openedDictGroup.moveChildToPos(dictPref.getDictId(), newIndex);
                rootDictRef.updateChildPref(openedDictGroup);
            }
        });

        dgvDictGroup.setOnDraggedOutOfContainerListener(new OnDraggedOutOfContainerListener() {
            @Override
            public void onDraggedOutOFContainer(int position) {
                DictPref dictPref = openedDictGroup.getChildDictPrefAtIndex(position);
                openedDictGroup.removeChildDictPref(dictPref.getDictId());
                rootDictRef.updateChildPref(openedDictGroup);
                //rootDictRef.addChildPref(dictPref);
                if (openedDictGroup.getChildCount() > 0)
                    showDictsByGroup(null, openedDictGroup);
                else {
                    rootDictRef.removeChildDictPref(openedDictGroup.getDictId());
                    exitDictGrupView();
                    buildDictListView(true);
                }
            }
        });
    }

    private void buildDictListView(boolean refresh) {
        if (refresh) {
            MdxEngine.getLibMgr().updateDictPref(rootDictRef);
            dictPrefList.clear();
            for (int i = 0; i < rootDictRef.getChildCount(); i++) {
                DictPref childPref = rootDictRef.getChildDictPrefAtIndex(i);
                if (childPref.getDictId() != DictPref.kDefaultGrpId) //Default group
                    dictPrefList.add(childPref);
            }
            for (int i = 0; i < defaultDictGroup.getChildCount(); i++) {
                DictPref childPref = defaultDictGroup.getChildDictPrefAtIndex(i);
                if (!isDictExistsInAnyGroup(childPref.getDictId()))
                    dictPrefList.add(childPref);
            }

        }
        dgv.removeAllViews();
        for (DictPref childPref : dictPrefList) {
            dgv.addView(new DictEntryView(this, childPref));
        }
    }

    private void showDictsByGroup(View view, DictPref dictGroup) {
        txtDictGroupName.setText(dictGroup.getActualDictName());
        dgvDictGroup.removeAllViews();
        for (int i = 0; i < dictGroup.getChildCount(); i++) {
            DictPref childPref = dictGroup.getChildDictPrefAtIndex(i);
            dgvDictGroup.addView(new DictEntryView(this, childPref, 110));
        }
        if (view != null) {
            layoutDictGroup.setVisibility(View.VISIBLE);
            dgv.setTouchAllowed(false);
            openedDictGroup = dictGroup;
            openedDictGroupView = view;
        }
    }

    private void exitDictGrupView() {
        layoutDictGroup.setVisibility(View.GONE);
        if (!txtDictGroupName.getText().toString().equals(openedDictGroup.getActualDictName())) {
            openedDictGroup.setDictName(txtDictGroupName.getText().toString());
            rootDictRef.updateChildPref(openedDictGroup);
            ((TextView) openedDictGroupView.findViewById(R.id.dict_name)).setText(txtDictGroupName.getText());
        }
        dgv.setTouchAllowed(true);
        openedDictGroup = null;
        openedDictGroupView = null;
    }

    @Override
    protected void onPause() {

        MdxEngine.getLibMgr().updateDictPref(rootDictRef);
        //MdxEngine.getLibMgr().updateDictPrefRoot(rootDictPref)
        MdxEngine.saveEngineSettings();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MiscUtils.setOrientationSensorBySetting(this);
        MdxEngine.refreshDictList();
        buildDictListView(true);
    }

    @Override
    public void onBackPressed() {
        if (openedDictGroup != null) {
            exitDictGrupView();
        } else {
            //exportDictSettings();
            if (MdxEngine.getSettings().getPrefMultiDictLookkupMode()) {
                if (rootDictRef.hasEnabledChild()) {
                    Intent intent = getIntent().putExtra(SELECTED_LIB_ID, DictPref.kRootDictPrefId);
                    LibraryFrameNew.this.setResult(RESULT_OK, intent);
                } else if (rootDictRef.getChildCount() > 0) {
                    Toast.makeText(this, R.string.at_least_select_one_dict, Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            finish();
        }
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
            rootDictRef.setUnionGroup(unionMode);
            buildDictListView(false);
            invalidateOptionsMenu();
            return true;
        }
        return false;
    }

    private boolean isDictExistsInAnyGroup(int dictId) {
        for (int i = 0; i < rootDictRef.getChildCount(); i++) {
            DictPref dictGroup = rootDictRef.getChildDictPrefAtIndex(i);
            if (dictGroup.isDictGroup() && dictGroup.getDictId() != DictPref.kDefaultGrpId) {
                for (int j = 0; j < dictGroup.getChildCount(); j++) {
                    DictPref childDictPref = dictGroup.getChildDictPrefAtIndex(j);
                    if (childDictPref.getDictId() == dictId)
                        return true;
                }
            }
        }
        return false;
    }

    private boolean isDictExistsInGroup(int dictId, DictPref dictGroup) {
        for (int i = 0; i < dictGroup.getChildCount(); i++) {
            DictPref childDictPref = dictGroup.getChildDictPrefAtIndex(i);
            if (childDictPref.getDictId() == dictId)
                return true;
        }
        return false;
    }

    private int calculateNewDictIndex(int lastIndex, boolean checkForGroup) {
        int index = 0;
        for (int i = 0; i <= lastIndex; i++) {
            DictPref childDictPref = dictPrefList.get(i);
            if (childDictPref.isDictGroup() && checkForGroup) {
                if (i != lastIndex)
                    index++;
            } else {
                if (!childDictPref.isDictGroup() && !checkForGroup)
                    if (i != lastIndex)
                        index++;
            }
        }
        return index;
    }

    public class DictEntryView extends LinearLayout {
        private TextView dictName;
        private TextView childCount;
        private CheckBox itemCheckBox;
        private DictPref dictPrefEntry;

        private int viewSize = -1;

        public DictEntryView(Context context, DictPref dictPref) {
            super(context);
            dictPrefEntry = dictPref;
            init();

        }


        public DictEntryView(Context context, DictPref dictPref, int sizeInDp) {
            super(context);
            dictPrefEntry = dictPref;
            viewSize = sizeInDp;
            init();

        }


        private void init() {
            //LayoutInflater layoutInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //View view = layoutInflater.inflate(R.layout.example, this);
            LayoutInflater.from(getContext()).inflate(R.layout.dict_pref_view, this);
            if (viewSize != -1) {
                viewSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, viewSize, getResources().getDisplayMetrics());
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) findViewById(R.id.dict_pref_view_container).getLayoutParams();
                params.width = viewSize;
                params.height = viewSize;
                findViewById(R.id.dict_pref_view_container).setLayoutParams(params);
            }
            //this.setTag(dictPrefEntry.getDictId());
            this.dictName = (TextView) findViewById(R.id.dict_name);
            this.childCount = (TextView) findViewById(R.id.child_count);
            this.itemCheckBox = (CheckBox) findViewById(R.id.item_cb);
            this.itemCheckBox.setTag(dictPrefEntry.getDictId());

            if (dictPrefEntry.isDictGroup()) {
                this.childCount.setVisibility(View.VISIBLE);
                this.childCount.setText(String.valueOf(dictPrefEntry.getChildCount()));
                ((ImageView) findViewById(R.id.dict_img)).setImageResource(R.drawable.ic_dict_group);
                this.setBackgroundResource(R.drawable.bg_dictgroup);
                this.setTag(R.drawable.bg_dictgroup);
            } else {
                this.childCount.setVisibility(View.GONE);
                if(dictPrefEntry.getDictCoverImage()!=null)
                    ((ImageView) findViewById(R.id.dict_img)).setImageBitmap(IOUtil.decodeBitmapFile(
                            new File(dictPrefEntry.getDictCoverImage()), 30));
                    else
                ((ImageView) findViewById(R.id.dict_img)).setImageResource(R.drawable.ic_dict_pref);
                this.setBackgroundResource(R.drawable.bg_dictpref);
                this.setTag(R.drawable.bg_dictpref);
            }
            dictName.setText(dictPrefEntry.getActualDictName());

            if (MdxEngine.getSettings().getPrefMultiDictLookkupMode()) {
                this.itemCheckBox.setChecked(!dictPrefEntry.isDisabled());
            } else {
                this.itemCheckBox.setChecked(MdxEngine.getSettings().getPrefLastDictId() == dictPrefEntry.getDictId());
            }
        }
    }
}
