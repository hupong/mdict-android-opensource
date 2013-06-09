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

package cn.mdict.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.speech.tts.TextToSpeech;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.actionbarsherlock.widget.SearchView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import cn.mdict.DictContentProvider;
import cn.mdict.FloatingForm;
import cn.mdict.MainForm;
import cn.mdict.MiscUtils;
import cn.mdict.R;
import cn.mdict.SimpleActionModeCallbackAgent;
import cn.mdict.WebViewGestureFilter;
import cn.mdict.mdx.DictEntry;
import cn.mdict.mdx.DictPref;
import cn.mdict.mdx.MdxDictBase;
import cn.mdict.mdx.MdxEngine;
import cn.mdict.mdx.MdxEngineSetting;
import cn.mdict.online.Jukuu;
import cn.mdict.online.OnlineReference;
import cn.mdict.utils.IOUtil;
import cn.mdict.utils.SysUtil;
import cn.mdict.widgets.MdxAdapter;
import cn.mdict.widgets.MdxView;
import cn.mdict.widgets.MdxView.MdxViewListener;
import cn.mdict.widgets.SearchTrack;

public class DictView extends SherlockFragment implements MdxViewListener,
        SimpleActionModeCallbackAgent.ActionItemClickListener,
        TextToSpeech.OnInitListener, WebViewGestureFilter.GestureListener{
    @Override
    public boolean onSearchText(MdxView view, String text, int touchPointX,
                                int touchPointY) {
        // TODO handle in dict lookup by popup window
        return false;
    }

    @Override
    public boolean onDisplayEntry(MdxView view, DictEntry entry,
                                  boolean addToHistory) {
        // added by alex start
        if (addToHistory) {
            depth += 1;
            if (history == null)
                history = new ArrayList<SearchTrack>();
            SearchTrack st = new SearchTrack();
            st.setScrollX(view.getHtmlView().getScrollX());
            st.setScrollY(view.getHtmlView().getScrollY());
            history.add(st);
        }// added by alex end

        setCurrentEntry(entry);
        // getActivity().invalidateOptionsMenu();

        if (btnSpeak != null)
            btnSpeak.setEnabled(hasPronunciationForCurrentEntry());
        if (btnAddToFav != null)
            btnAddToFav.setEnabled(currentEntry != null
                    && currentEntry.isValid());

        entry.makeJEntry();
        syncSearchView();
        getSherlockActivity().invalidateOptionsMenu();
        if (MdxEngine.getSettings().getPrefAutoPlayPronunciation()) {
            contentView.playPronunciationForCurrentEntry();
        }
        return false;
    }

    @Override
    public boolean onHeadWordNotFound(MdxView view, String headWord,
                                      int scrollX, int scrollY) {
        searchView.setQuery(headWord, false);
        if (history == null) //al20121205.sn
            history = new ArrayList<SearchTrack>();//al20121205.en
        depth += 1;
        SearchTrack st = new SearchTrack();
        st.setScrollX(scrollX);
        st.setScrollY(scrollY);
        history.add(new SearchTrack());
        return false;
    }

    @Override
    public boolean onPlayAudio(MdxView view, String path) {
        return false;
    }

    @Override
    public boolean onPageLoadCompleted(WebView view) {
        //TODO: should add support for non-highspeed view mode of MdxView
        if (lastTrack != null) {
            view.scrollTo(lastTrack.getScrollX(), lastTrack.getScrollY());
            lastTrack = null;
            return true; //return true to suppress "jump to anchor"
        }
        return false;
    }

    @Override
    public void onInflate(Activity activity, AttributeSet attrs,
                          Bundle savedInstanceState) {
        super.onInflate(activity, attrs, savedInstanceState);
        TypedArray a = activity.obtainStyledAttributes(attrs, R.styleable.DictView);
        hasToolbar = a.getBoolean(R.styleable.DictView_has_toolbar, false);
        showHomeButtonInToolbar = a.getBoolean(R.styleable.DictView_show_home_button_in_toolbar, true);
        a.recycle();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.dict_view, container);

        setHasOptionsMenu(true);
        rootView.setFocusable(true);
        updateViewMode(rootView);
        searchView = new com.actionbarsherlock.widget.SearchView(getSherlockActivity().getSupportActionBar().getThemedContext());
        searchView.setIconifiedByDefault(false);
        searchView.setInputType(searchView.getInputType() & (~android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE));
        searchView.setImeOptions(searchView.getImeOptions() | EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN);
        if (!hasToolbar) {
            ActionBar actionBar = getSherlockActivity().getSupportActionBar();
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            //actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setCustomView(searchView);
            MiscUtils.replaceViewInLayoutById(rootView, R.id.toolbar_container, null);

        } else {
            ActionBar actionBar = getSherlockActivity().getSupportActionBar();
            actionBar.hide();
            ViewGroup searchViewContainer = (ViewGroup) rootView.findViewById(R.id.toolbar_container);
            MiscUtils.replaceViewInLayoutById(searchViewContainer, R.id.search_view, searchView);
        }
        //Create the search view and replace the dummy view.

        //Hide search hint icon
        View searchHintIcon = searchView.findViewById(R.id.abs__search_mag_icon);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) searchHintIcon.getLayoutParams();
        params.width = 0;
        params.height = 0;
        params.leftMargin = 0;
        params.rightMargin = 0;
        searchHintIcon.setLayoutParams(params);

        contentView = (MdxView) rootView.findViewById(R.id.mdx_view);
        // contentView = new MdxView(rootView.getContext());
        contentView.setMdxViewListener(this);
        // contentView.setBackgroundColor(android.R.color.white);
        // rootView.addView(contentView);

        // actionModeAgent=new SimpleActionModeCallbackAgent(R.menu.app_menu,
        // this);
        /*
        btnSpeak = (ImageButton) searchViewContainer.findViewById(R.id.speak);
        if (btnSpeak != null) {
            btnSpeak.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    contentView.playPronunciationForCurrentEntry();
                }
            });
            btnSpeak.setEnabled(false);
        }

        btnAddToFav = (ImageButton) searchViewContainer.findViewById(R.id.add_to_fav);
        if (btnAddToFav != null) {
            btnAddToFav.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    contentView.addCurrentEntryToFav();
                }
            });
            btnAddToFav.setEnabled(false);
        }
        */
        ImageButton btnHome = (ImageButton) rootView.findViewById(R.id.home_btn);
        if (btnHome != null) {
            if (showHomeButtonInToolbar)
                btnHome.setOnTouchListener(logoIconTouchListener);
            else
                btnHome.setVisibility(View.GONE);
        }

        onlineReference = new Jukuu(getSherlockActivity());
        onlineReferenceHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.arg1) {
                    case 1:
                        //Lookup online content failed.
                    case 0:
                        displayHtml(msg.obj.toString());
                        break;

                }
                super.handleMessage(msg);
            }
        };

        btnJukuu = (ImageButton) rootView.findViewById(R.id.jukuu_btn);
        if (btnJukuu != null) {
            btnJukuu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String searchHeadword = "";
                    if (searchView.getQuery().length() > 0) {
                        searchHeadword = searchView.getQuery().toString();
                    }
                    displayHtml(getString(R.string.online_reference_loading));
                    onlineReference.lookup(searchHeadword, getSherlockActivity(), onlineReferenceHandler);
                }
            });
            // btnJukuu.setEnabled(false);
        }
        //searchView = (SearchView) (searchViewContainer.findViewById(R.id.search_view));


        //btnClear = (ImageButton) actionBar.getCustomView().findViewById(R.id.search_clear_btn);

        enableFingerGesture(MdxEngine.getSettings().getPrefUseFingerGesture());

        headwordList = (ListView) rootView.findViewById(R.id.headword_list);

        adapter = new MdxAdapter(getSherlockActivity());
        adapter.setDict(dict, getString(R.string.empty_entry_list),
                getString(R.string.invalid_dict));
        headwordList.setAdapter(adapter);

        rootView.requestFocus();
        // switchToListView();

        headwordList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                                    int position, long ID) {
                DictEntry entry = adapter.getEntryByPosition(position);
                InputMethodManager imm = (InputMethodManager) getSherlockActivity()
                        .getSystemService(
                                android.content.Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                if (entry != null) {
                    displayByEntry(entry, true);
                    depth = 0; // added by alex
                    initSearchHistory();// added by alex
                }
            }
        });
        /*
        btnClear.setOnClickListener(new ImageButton.OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                inputBox.setText("");
                inputBox.requestFocus();
            }

        });
        */
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query.compareToIgnoreCase(":aboutApp") == 0) {
                    displayWelcome();
                    depth = 0;
                    history = new ArrayList<SearchTrack>();// added by alex
                } else if (query.compareToIgnoreCase(":verify") == 0) {
                    DictEntry entryTest = new DictEntry(-1, "", dict.getDictPref().getDictId());
                    for (int i = 0; i < dict.getEntryCount(); ++i) {
                        entryTest.setEntryNo(i);
                        dict.getDictTextN(entryTest, false, false, "", "");
                    }
                    displayHtml("Dict verified!");
                } else if (MdxDictBase.isMdxCmd(query)
                        || !currentEntry.isValid()) {
                    displayByHeadword(query, false);
                    depth = 0;
                    initSearchHistory();// added by alex
                } else {
                    displayByEntry(currentEntry, true);
                    depth = 0;
                    initSearchHistory();// added by alex
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!skipUpdateEntryList) {
                    currentEntry.setEntryNo(-1);
                    if (dict != null && dict.isValid()) {
                        if (MdxDictBase.isMdxCmd(newText)) {
                            currentEntry.setEntryNo(DictEntry.kSystemCmdEntryNo);
                        } else {
                            dict.locateFirst(newText, true, true, true, currentEntry);
                            if (currentEntry.isValid())
                                dict.getHeadword(currentEntry);
                        }
                        adapter.setCurrentEntry(currentEntry);
                        if (dict.canRandomAccess() && currentEntry.isValid()) {
                            headwordList.setSelection(currentEntry.getEntryNo());
                        } else {
                            headwordList.setSelection(0);
                        }
                    }
                }
                skipUpdateEntryList = false;
                return true; //return false if want the system provide a suggestion list?
            }
        });
        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Context context = getSherlockActivity();
                InputMethodManager imm = null;
                if (context != null)
                    imm = (InputMethodManager) context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm == null)
                    return;
                if (hasFocus) {
                    switchToListView();
                    // inputBox.setText("");
                    if (MdxEngine.getSettings().getPrefAutoSIP())
                        imm.showSoftInput(searchView, 0);
                    AutoCompleteTextView editField = (AutoCompleteTextView) (searchView.findViewById(R.id.abs__search_src_text));
                    editField.selectAll(); //TODO This line has no effect, need more works.
                } else {
                    if (MdxEngine.getSettings().getPrefAutoSIP())
                        imm.hideSoftInputFromWindow(searchView.getWindowToken(),
                                0);
                    // imm.hideSoftInputFromWindow(inputBox.getWindowToken(),
                    // 0);
                }
                getSherlockActivity().invalidateOptionsMenu();

            }
        });
        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == kCheckTTSData) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                doTTSInit(resultCode);
            } else {
                shutDownTTS();
            }
        }
    }

    @Override
    public void onDestroy() {
        shutDownTTS();
        super.onDestroy();
    }

    private void shutDownTTS() {
        if (ttsEngine != null)
            ttsEngine.shutdown();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.dict_view_option_menu, menu);
        MenuItem item = menu.findItem(R.id.view);
        if (item != null) {
            SubMenu submenu = item.getSubMenu();
            ArrayList<String> fonts=new ArrayList<String>();
            MdxEngine.findExternalFonts(fonts);
            if (fonts.isEmpty())
                submenu.removeItem(R.id.font_face);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void enableMenuItem(Menu menu, int itemId, boolean enable) {
        MenuItem menuItem = menu.findItem(itemId);
        if (menuItem != null) {
            menuItem.setEnabled(enable);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // rebuildOptionMenuOnDemand(menu,
        // getSupportActivity().getMenuInflater(), false);

        boolean show_toolbar = MdxEngine.getSettings().getPrefShowToolbar();
        for (int i = 0; i < menu.size(); ++i) {
            menu.getItem(i).setShowAsAction(
                    show_toolbar ? MenuItem.SHOW_AS_ACTION_IF_ROOM
                            : MenuItem.SHOW_AS_ACTION_NEVER);
        }
        MenuItem item;
        SubMenu submenu;

        int chnConvType = -1;
        boolean dictIsValid = (dict != null && dict.isValid());

        enableMenuItem(menu, R.id.speak, hasPronunciationForCurrentEntry());
        enableMenuItem(menu, R.id.add_to_fav, currentEntry != null
                && currentEntry.isValid());

        item = menu.findItem(R.id.view);
        if (item != null) {
            item.setEnabled(dictIsValid && currentView == contentView);
            submenu = item.getSubMenu();
            if (submenu != null) {
                MenuItem menuItem;
                menuItem = submenu.findItem(R.id.chn_conv);
                if (menuItem != null)
                    menuItem.setEnabled(dictIsValid);

                boolean showEntryCtrl = dictIsValid && !dict.canRandomAccess();
                menuItem = submenu.findItem(R.id.expand_all);
                if (menuItem != null)
                    menuItem.setEnabled(showEntryCtrl);
                menuItem = submenu.findItem(R.id.collapse_all);
                if (menuItem != null)
                    menuItem.setEnabled(showEntryCtrl);

                int zoomLevel = -1;
                if (dictIsValid)
                    zoomLevel = dict.getDictPref().zoomLevel();
                menuItem = submenu.findItem(R.id.zoom_in);
                if (menuItem != null)
                    menuItem.setEnabled(zoomLevel >= 0
                            && zoomLevel < DictPref.kZoomLargest);
                menuItem = submenu.findItem(R.id.zoom_out);
                if (menuItem != null)
                    menuItem.setEnabled(zoomLevel > DictPref.kZoomSmallest);
                menuItem = submenu.findItem(R.id.toggle_toolbar);
                if (menuItem != null){
                    menuItem.setTitle(show_toolbar?R.string.hide_toolbar:R.string.show_toolbar);
                }
            }
        }

        item = menu.findItem(R.id.history_back);
        if (item != null) {
            item.setEnabled(MdxEngine.getHistMgr().hasPrev());
        }

        item = menu.findItem(R.id.history_forward);
        if (item != null) {
            item.setEnabled(MdxEngine.getHistMgr().hasNext());
        }

    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (item.getItemId() == android.R.id.home && !hasToolbar) {
            getSherlockActivity().getSupportActionBar().setDisplayShowCustomEnabled(true);
            //getSherlockActivity().getSupportActionBar().setCustomView(searchView);
            return true;
        } else {
            getSherlockActivity().onOptionsItemSelected(item);
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //View view = item.getActionView();
        // Handle item selection
        switch (item.getItemId()) {
            // case R.id.search:
            // inputBox.requestFocus();
            // return true;
            case android.R.id.home:
                // this.toggleView();
                toggleToolbarVisible();
                break;
            case R.id.chn_conv:
                selectChnConv();
                break;
            case R.id.history_back:
                contentView.displayHistPrev();
                break;
            case R.id.history_forward:
                contentView.displayHistNext();
                break;
            case R.id.entry_prev:
                contentView.displayEntryPrev();
                break;
            case R.id.entry_next:
                contentView.displayEntryNext();
                break;
            case R.id.zoom_in:
                zoomView(true);
                break;
            case R.id.zoom_out:
                zoomView(false);
                break;
            case R.id.add_to_fav:
                contentView.addCurrentEntryToFav();
                break;
            case R.id.speak:
                contentView.playPronunciationForCurrentEntry();
                break;
            case R.id.expand_all:
                contentView.showAllEntries(true);
                break;
            case R.id.collapse_all:
                contentView.showAllEntries(false);
                break;
            case R.id.aboutapp:
                displayWelcome();
                break;
            case R.id.aboutdict:
                displayByHeadword(":about", false);
                break;
            case R.id.font_face:
                selectFont();
                break;
            default:
                // return super.onOptionsItemSelected(item);
                return false;
        }
        getSherlockActivity().invalidateOptionsMenu();
        return true;
    }

    AlertDialog.Builder dialogBuilder = null;
    private void selectFont() {
        String selectedFont=dict.getDictPref().getFontFace();
        int selectedFontPos=0;

        fontList.clear();
        fontList.add(getSherlockActivity().getResources().getString(R.string.system_default));
        MdxEngine.findExternalFonts(fontList);
        String[] fontNames=new String[fontList.size()];
        int count=0;
        for(String fontPath:fontList){
            if (fontPath.compareTo(selectedFont)==0)
                selectedFontPos=count;
            fontNames[count]=MiscUtils.getFileNameMainPart(fontPath);
            ++count;
        }

        dialogBuilder = new AlertDialog.Builder(getSherlockActivity())
                .setCancelable(true)
                .setTitle(getSherlockActivity().getString(R.string.font_face))
                .setSingleChoiceItems(fontNames, selectedFontPos, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (dict==null || !dict.isValid())
                            return;
                        DictPref dictPref=dict.getDictPref();
                        if (which==0){
                            dictPref.setFontFace("");
                        }else{
                            dictPref.setFontFace(fontList.get(which));
                        }
                        DictView.this.updateDictWithRefresh(dictPref);
                        dialog.dismiss();
                    }
                });
        dialog=dialogBuilder.show();
    }

    private void selectChnConv(){
        if (dict==null || dict.getDictPref()==null )
            return;
        int currentSelection=0;
        switch (dict.getDictPref().getChnConversion()) {
            case DictPref.kChnConvToSimplified:
                currentSelection = 1;
                break;
            case DictPref.kChnConvToTraditional:
                currentSelection= 2;
                break;
            default:
                currentSelection = 0;
                break;
        }
        dialogBuilder = new AlertDialog.Builder(getSherlockActivity())
                .setCancelable(true)
                .setTitle(getSherlockActivity().getString(R.string.chn_conv))
                .setSingleChoiceItems(R.array.chn_conv_choices, currentSelection, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DictPref dictPref=dict.getDictPref();
                        int chnType=DictPref.kChnConvNone;
                        switch(which){
                            case 0:
                                chnType=DictPref.kChnConvNone;
                                break;
                            case 1:
                                chnType=DictPref.kChnConvToSimplified;
                                break;
                            case 2:
                                chnType=DictPref.kChnConvToTraditional;
                                break;
                        }
                        dictPref.setChnConversion(chnType);
                        DictView.this.updateDictWithRefresh(dictPref);
                        dialog.dismiss();
                    }
                });
        dialog=dialogBuilder.show();
    }
    public boolean hasPronunciationForCurrentEntry() {
        boolean hasSound = dict != null && dict.isValid()
                && currentEntry != null && currentEntry.isValid()
                && currentEntry.getHeadword().length() > 0;
        if (hasSound)
            hasSound = (ttsEngine != null && MdxEngine.getSettings()
                    .getPrefUseTTS()) || MiscUtils.hasSpeechForWord(dict, currentEntry.getHeadword());
        return hasSound;
    }

    public void updateViewMode(ViewGroup rootView) {
        useSplitViewMode = MiscUtils.shouldUseSplitViewMode(getSherlockActivity());
        Log.d(TAG, "Update view mode, use split view mode:" + String.valueOf(useSplitViewMode));
        if (rootView == null)
            rootView = (ViewGroup) getView();
        ViewGroup newContainer;
        ViewGroup.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        if (useSplitViewMode) {
            LinearLayout layout = new LinearLayout(getSherlockActivity());
            layout.setLayoutParams(layoutParams);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            //layout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
            newContainer = layout;
        } else {
            FrameLayout layout = new FrameLayout(getSherlockActivity());
            layout.setLayoutParams(layoutParams);
            newContainer = layout;
        }
        View divider = rootView.findViewById(R.id.split_view_divider);
        if (divider != null)
            divider.setVisibility(useSplitViewMode ? View.VISIBLE : View.INVISIBLE);
        newContainer.setId(R.id.dict_view_container);
        MiscUtils.changeContainer(rootView, R.id.dict_view_container, newContainer);
        if (useSplitViewMode && headwordList != null && contentView != null) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) headwordList.getLayoutParams();
            params.weight = 2;
            headwordList.setLayoutParams(params);
            params = (LinearLayout.LayoutParams) contentView.getLayoutParams();
            params.weight = 1;
            contentView.setLayoutParams(params);
        }
        if ((contentView != null) && (headwordList != null)) {
            if (currentView == contentView)
                switchToContentView();
            else
                switchToListView();
        }
        //rootView.requestLayout();
    }

    public void changeDict(MdxDictBase dict, boolean showDictAbout) {
        String currentInput = searchView.getQuery().toString();
        this.dict = dict;
        lastZoomActionIsZoomIn = false;

        if (contentView != null)
            contentView.setDict(dict);
        if (adapter != null)
            adapter.setDict(dict, getString(R.string.empty_entry_list),
                    getString(R.string.invalid_dict));
        DictContentProvider.setDict(dict);
        if (dict != null && dict.isValid()) {
            if (currentInput != null && currentInput.length() != 0) {
                DictEntry entry = new DictEntry();
                if (dict.locateFirst(currentInput, true, false, false, entry) == MdxDictBase.kMdxSuccess) {
                    displayByEntry(entry, true);
                } else {
                    setInputText(currentInput, false);
                    switchToListView();
                }
            } else if (showDictAbout) {
                displayByHeadword(":about", false);
            } else {
                setInputText("", true);
                displayHtml("");
            }
        } else {
            currentEntry = new DictEntry(-1, "", DictPref.kInvalidDictPrefId);
            getSherlockActivity().invalidateOptionsMenu();
        }
    }

    //
    public void setCurrentEntry(DictEntry entry) {
        currentEntry = new DictEntry(entry);
        currentEntry.makeJEntry();
    }

    public WebView getHtmlView() {
        return contentView.getHtmlView();
    }

    public void setFragmentContainer(ViewGroup container) {
        fragmentContainer = container;
    }

    void updateDictWithRefresh(DictPref pref) {
        if (dict!=null){
            dict.setViewSetting(pref);
            MdxEngine.rebuildHtmlSetting(dict,MdxEngine.getSettings().getPrefHighSpeedMode());
        }
        MdxEngine.getLibMgr().updateDictPref(pref);
        currentEntry.makeJEntry();
        DictEntry entry = new DictEntry(currentEntry);
        // changeDict(dict); //changeDict may change the value of currentEntry.
        displayByEntry(entry, false);
    }

    public void setChnConv(int convertType) {
        if (dict.isValid()) {
            dict.setChnConversion(convertType);
            updateDictWithRefresh(dict.getDictPref());
        }
    }

    public void toggleZoom() {
        if (lastZoomActionIsZoomIn)
            zoomView(false);
        else
            zoomView(true);
    }

    public void zoomView(boolean zoomIn) {
        if (dict != null && dict.isValid() && currentEntry.isValid()) {
            DictPref pref = dict.getDictPref();
            int zoomLevel = pref.zoomLevel();
            if (zoomIn && zoomLevel < DictPref.kZoomLargest) {
                zoomLevel++;
                lastZoomActionIsZoomIn = true;
            } else if (!zoomIn && zoomLevel > DictPref.kZoomSmallest) {
                lastZoomActionIsZoomIn = false;
                zoomLevel--;
            } else
                zoomLevel = -1;
            if (zoomLevel >= 0) {
                pref.setZoomLevel(zoomLevel);
                updateDictWithRefresh(pref);
            }
        }
    }

    public void setInputText(String text, boolean updateEntryList) {
        skipUpdateEntryList = !updateEntryList;
        searchView.setQuery(text, false);
    }

    public void enableFingerGesture(boolean enable) {
        if (contentView != null)
            contentView.setGestureListener(enable ? this : null);
    }

    public boolean onBackPressed() {
        if (currentView == contentView) {
            // if (contentView.getHtmlView().canGoBack()) {
            // contentView.getHtmlView().goBack();
            // } else {
            DictEntry entry = MdxEngine.getHistMgr().getPrev();

            if (entry != null && entry.isValid() && depth > 0) {
                lastTrack = history.get(history.size() - 1); // added by alex
                depth -= 1;// added by alex
                displayByEntry(entry, false);
                history.remove(history.size() - 1); // added by alex
            } else {
                switchToListView();
            }
            // }
            return true;
        }
        return false;
    }

    /*
    public void selectDict(int dictId) {
        String currentInput = searchView.getQuery().toString();
        int result = MdxEngine.openDictById(dictId, MdxEngine.getSettings()
                .getPrefsUseLRUForDictOrder(), dict);
        changeDict(dict);
        if (result == MdxDictBase.kMdxSuccess) {
            if (currentInput != null && currentInput.length() != 0) {
                DictEntry entry = new DictEntry();
                if (dict.locateFirst(currentInput, true, false, false, entry) == MdxDictBase.kMdxSuccess) {
                    displayByEntry(entry, true);
                } else {
                    setInputText(currentInput, false);
                    switchToListView();
                }
            } else {
                DictEntry entry = new DictEntry(-2, ":about", dictId);
                displayByEntry(entry, false);
            }
        } else {
            String info = String.format(getString(R.string.fail_to_open_dict),
                    result);
            MiscUtils.showMessageDialog(getSherlockActivity(), info,
                    getString(R.string.error));
        }
    }
    */
    public void toggleView() {
        if (currentView == headwordList)
            switchToContentView();
        else
            switchToListView();
    }

    public void switchToListView() {
        headwordList.setVisibility(View.VISIBLE);
        contentView.setVisibility(useSplitViewMode ? View.VISIBLE : View.INVISIBLE);
        if (MdxEngine.getSettings().getPrefAutoSIP() && !searchView.hasFocus())
            searchView.requestFocus();
        currentView = headwordList;
    }

    public void switchToContentView() {
        headwordList.setVisibility(useSplitViewMode ? View.VISIBLE : View.INVISIBLE);
        contentView.setVisibility(View.VISIBLE);
        contentView.requestFocus();
        // InputMethodManager imm = (InputMethodManager)
        // getSupportActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        // imm.hideSoftInputFromWindow(inputBox.getWindowToken(), 0);
        currentView = contentView;
    }

    public boolean isInputing() {
        return (currentView == headwordList && searchView.isFocused());
    }

    public void syncSearchView() {
        setInputText(currentEntry.getHeadword(), true);
        // TODO should sync the entry list too
    }

    public void displayWelcome() {
        switchToContentView();
        StringBuffer welcome = new StringBuffer();
        IOUtil.loadStringFromAsset(getSherlockActivity().getAssets(),
                "Welcome.htm", welcome, true);
        String html = welcome.toString().replace("$version$",
                SysUtil.getVersionName(getSherlockActivity(), true));
        displayHtml(html);
    }

    public void displayByEntry(DictEntry entry, boolean addToHistory) {
        switchToContentView();
        contentView.displayByEntry(entry, !entry.isSysCmd() && addToHistory);
    }

    public void displayByHeadword(String headword, boolean addToHistory) {        /*
         * if (addToHistory) { depth += 1; SearchTrack st = new SearchTrack();
		 * st.setScrollX(0); st.setScrollY(0); history.add(new SearchTrack()); }
		 */
        switchToContentView();
        contentView.displayByHeadword(headword, addToHistory);
    }

    public void displayAssetFile(String assetFile) {
        switchToContentView();
        contentView.displayAssetFile(assetFile);
    }

    public void displayHtml(String html) {
        switchToContentView();
        contentView.displayHtml(html);
    }

    @Override
    public void onSwipe(View view, int direction, int touchPointCount,
                        MotionEvent motionEvent) {
        if (touchPointCount == 1) {
            if (direction == WebViewGestureFilter.GestureListener.swipeLeft) {
                contentView.displayHistNext();
            } else {
                contentView.displayHistPrev();
            }
        } else if (touchPointCount == 2) {
            if (dict != null && dict.isValid() && dict.canRandomAccess()
                    && currentEntry.isValid()) {
                if (direction == WebViewGestureFilter.GestureListener.swipeLeft) {
                    contentView.displayEntryNext();
                } else {
                    contentView.displayEntryPrev();
                }
            }
        }
    }

    @Override
    public void onDoubleTap(View view, int touchPointCount,
                            MotionEvent motionEvent) {
        if (touchPointCount == 1) {
            int screenY = (int) motionEvent.getRawY();

            int[] loc = new int[2];
            loc[0] = contentView.getLeft();
            loc[1] = contentView.getTop();
            contentView.getLocationOnScreen(loc);
            screenY -= loc[1];
            int viewHeight = contentView.getHeight();
            if (screenY > (viewHeight * 3 / 4)) {
                toggleToolbarVisible();
            } else {
                toggleZoom();
            }
        }
    }

    private void toggleToolbarVisible() {
        MdxEngine.getSettings().setPrefShowToolbar(
                !MdxEngine.getSettings().getPrefShowToolbar());
        getSherlockActivity().invalidateOptionsMenu();
    }

    public void initTTSEngine() {
        try {
            contentView.setTtsEngine(null);
            Intent checkIntent = new Intent();
            checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
            startActivityForResult(checkIntent, kCheckTTSData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("NewApi")
    @Override
    // For tts init
    public void onInit(int i) {
        if (i == TextToSpeech.ERROR) {
            if (ttsEngine != null) {
                ttsEngine.shutdown();
                ttsEngine = null;
            }
        } else {
            if (MdxEngine.getSettings().getPrefPreferedTTSEngine().length() != 0)
                ttsEngine.setEngineByPackageName(MdxEngine.getSettings()
                        .getPrefPreferedTTSEngine());
            String ttsLocaleName = MdxEngine.getSettings().getPrefTTSLocale();
            if (ttsLocaleName.length() != 0) {
                ttsEngine.setLanguage(new Locale(ttsLocaleName));
            }
        }
        contentView.setTtsEngine(ttsEngine);
    }

    public void doTTSInit(int resultCode) {
        if (ttsEngine != null) {
            ttsEngine.shutdown();
            ttsEngine = null;
        }
        if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
            // success, create the TTS instance
            ttsEngine = new TextToSpeech(getSherlockActivity(), this);
        }
    }

    public void initSearchHistory() {
        if (history == null) //al20121205.sn
            history = new ArrayList<SearchTrack>();
        else//al20121205.en
            history.clear();
        history = new ArrayList<SearchTrack>();
    }

    private View.OnTouchListener logoIconTouchListener = new View.OnTouchListener() {
        int lastX
                ,
                lastY;
        int initX
                ,
                initY;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = (int) event.getRawX();
                    lastY = (int) event.getRawY();
                    initX = lastX;
                    initY = lastY;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(initX - (int) event.getRawX()) > 10
                            || Math.abs(initY - (int) event.getRawY()) > 10) {
                        int dx = (int) event.getRawX() - lastX;
                        int dy = (int) event.getRawY() - lastY;

                        int left = fragmentContainer.getLeft() + dx;
                        int top = fragmentContainer.getTop() + dy;
                        int right = fragmentContainer.getRight() + dx;
                        int bottom = fragmentContainer.getBottom() + dy;
                        // 设置不能出界
                        if (left < 0) {
                            left = 0;
                            right = left + fragmentContainer.getWidth();
                        }

                        // if (right > screenWidth) {
                        // right = screenWidth;
                        // left = right - viewContainer.getWidth();
                        // }

                        if (top < 0) {
                            top = 0;
                            bottom = top + fragmentContainer.getHeight();
                        }

                        // if (bottom > screenHeight) {
                        // bottom = screenHeight;
                        // top = bottom - viewContainer.getHeight();
                        // }
                        // RelativeLayout layout = (RelativeLayout) viewContainer;
                        // Gets the layout params that will allow you to resize the
                        // layout
                        // FrameLayout.LayoutParams params =
                        // (FrameLayout.LayoutParams) viewContainer
                        // .getLayoutParams();
                        // Changes the height and width to the specified *pixels*
                        // params.gravity = Gravity.LEFT;
                        // viewContainer.setLayoutParams(params);

                        fragmentContainer.layout(left, top, right, bottom);

                        lastX = (int) event.getRawX();
                        lastY = (int) event.getRawY();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (Math.abs(initX - (int) event.getRawX()) < 10
                            && Math.abs(initY - (int) event.getRawY()) < 10) {
                        fragmentContainer.setVisibility(View.GONE);
                        Intent mIntent = new Intent();
                        mIntent.putExtra("HEADWORD", currentEntry.getHeadword());
                        mIntent.setClass(fragmentContainer.getContext(), MainForm.class);
                        startActivity(mIntent);
                        ((FloatingForm) fragmentContainer.getContext()).finish();
                    }
                    break;
            }
            return true;
        }
    };

    private MdxDictBase dict = null;
    private SearchView searchView;
    private DictEntry currentEntry = new DictEntry();
    private MdxAdapter adapter = null;
    private MdxView contentView = null;
    private ListView headwordList;
    private View currentView = null;
    private ViewGroup fragmentContainer = null;
    private AlertDialog dialog=null;

    private ImageButton btnSpeak = null;
    private ImageButton btnAddToFav = null;
    private ImageButton btnJukuu = null;

    private boolean lastZoomActionIsZoomIn = false;
    private boolean hasToolbar = false;
    private boolean showHomeButtonInToolbar = true;
    private boolean useSplitViewMode = false;
    private boolean skipUpdateEntryList = false;

    private TextToSpeech ttsEngine = null;
    private static final int kCheckTTSData = 0;

    private int depth = 0;

    private OnlineReference onlineReference = null;
    private Handler onlineReferenceHandler = null;

    private ArrayList<SearchTrack> history;
    private SearchTrack lastTrack = null;

    ArrayList<String> fontList=new ArrayList<String>();

    private static final String TAG = "MDict.DictView";

}