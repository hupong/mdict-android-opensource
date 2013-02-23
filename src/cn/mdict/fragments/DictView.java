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
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Picture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.util.AttributeSet;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebView.PictureListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import cn.mdict.*;
import cn.mdict.mdx.DictEntry;
import cn.mdict.mdx.DictPref;
import cn.mdict.mdx.MdxDictBase;
import cn.mdict.mdx.MdxEngine;
import cn.mdict.online.Jukuu;
import cn.mdict.online.OnlineReference;
import cn.mdict.utils.IOUtil;
import cn.mdict.utils.SysUtil;
import cn.mdict.widgets.MdxAdapter;
import cn.mdict.widgets.MdxView;
import cn.mdict.widgets.MdxView.MdxViewListener;
import cn.mdict.widgets.SearchTrack;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.actionbarsherlock.widget.SearchView;

import java.util.ArrayList;
import java.util.Locale;

public class DictView extends SherlockFragment implements MdxViewListener,
        SimpleActionModeCallbackAgent.ActionItemClickListener,
        TextToSpeech.OnInitListener, WebViewGestureFilter.GestureListener {
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
            contentView.getHtmlView().setPictureListener(null);
        }// added by alex end
        int entryNo = entry.getEntryNo();

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
        if (!hasToolbar){
            ActionBar actionBar = getSherlockActivity().getSupportActionBar();
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            //actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setHomeButtonEnabled(true);
            searchView = new com.actionbarsherlock.widget.SearchView(getSherlockActivity().getSupportActionBar().getThemedContext());
            searchView.setIconifiedByDefault(false);
            actionBar.setCustomView(searchView);
            AddonFuncUnt.replaceViewInLayoutById(rootView, R.id.toolbar_container, null);

        }else{
            ActionBar actionBar = getSherlockActivity().getSupportActionBar();
            actionBar.hide();
            ViewGroup searchViewContainer=(ViewGroup)rootView.findViewById(R.id.toolbar_container);
            AddonFuncUnt.replaceViewInLayoutById(searchViewContainer, R.id.search_view, searchView);
        }
        //Create the search view and replace the dummy view.

        //Hide search hint icon
        View searchHintIcon=searchView.findViewById(R.id.abs__search_mag_icon);
        ViewGroup.MarginLayoutParams params=(ViewGroup.MarginLayoutParams)searchHintIcon.getLayoutParams();
        params.width=0;
        params.height=0;
        params.leftMargin=0;
        params.rightMargin=0;
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
        ImageButton btnHome = (ImageButton) rootView
                .findViewById(R.id.home_btn);
        if ( btnHome!=null ) {
            if (showHomeButtonInToolbar)
                btnHome.setOnTouchListener(logoIconTouchListener);
            else
                btnHome.setVisibility(View.GONE);
        }

        onlineReference=new Jukuu(getSherlockActivity());
        onlineReferenceHandler=new Handler(){
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
                    String searchHeadword="";
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
                if (query.compareToIgnoreCase(":aboutapp") == 0) {
                    displayWelcome();
                    depth = 0;
                    history = new ArrayList<SearchTrack>();// added by alex
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
                        String headword = newText;
                        DictEntry entry = new DictEntry();
                        dict.locateFirst(headword, true, true, true, entry);
                        currentEntry=entry;
                        if (!currentEntry.isValid())
                            adapter.setCurrentEntry(new DictEntry(0, "",
                                    dictPref != null ? dictPref.getDictId()
                                            : DictPref.kInvalidDictPrefId));
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
                Context context=getSherlockActivity();
                InputMethodManager imm=null;
                if ( context!=null )
                    imm= (InputMethodManager) context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm==null)
                    return;
                if (hasFocus) {
                    switchToListView();
                    // inputBox.setText("");
                    if (MdxEngine.getSettings().getPrefAutoSIP())
                        imm.showSoftInput(searchView, 0);
                    AutoCompleteTextView editField= (AutoCompleteTextView)(searchView.findViewById(R.id.abs__search_src_text));
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
        SubMenu submenu = null;

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
                if (dictPref != null) {
                    chnConvType = dictPref.getChnConversion();
                }
                MenuItem menuItem;
                int itemId = R.id.no_chn_conv;
                switch (chnConvType) {
                    case DictPref.kChnConvToSimplified:
                        itemId = R.id.to_simp_chn;
                        break;
                    case DictPref.kChnConvToTraditional:
                        itemId = R.id.to_trad_chn;
                        break;
                    default:
                        itemId = R.id.no_chn_conv;
                        break;
                }
                menuItem = submenu.findItem(itemId);
                if (menuItem != null)
                    menuItem.setChecked(true);

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
        View view = item.getActionView();
        // Handle item selection
        switch (item.getItemId()) {
            // case R.id.search:
            // inputBox.requestFocus();
            // return true;
            case android.R.id.home:
                // this.toggleView();
                toggleToolbarVisible();
                break;
            case R.id.no_chn_conv:
                setChnConv(DictPref.kChnConvNone);
                break;
            case R.id.to_simp_chn:
                setChnConv(DictPref.kChnConvToSimplified);
                break;
            case R.id.to_trad_chn:
                setChnConv(DictPref.kChnConvToTraditional);
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
            default:
                // return super.onOptionsItemSelected(item);
                return false;
        }
        getSherlockActivity().invalidateOptionsMenu();
        return true;
    }

    public boolean hasPronunciationForCurrentEntry() {
        boolean hasSound = dict != null && dict.isValid()
                && currentEntry != null && currentEntry.isValid()
                && currentEntry.getHeadword().length() > 0;
        if (hasSound)
            hasSound = (ttsEngine != null && MdxEngine.getSettings()
                    .getPrefUseTTS())
                    || AddonFuncUnt.hasSpeechForWord(dict,
                    currentEntry.getHeadword());
        return hasSound;
    }

    public void setDict(MdxDictBase dict) {
        this.dict = dict;
        lastZoomActionIsZoomIn = false;

        if (contentView != null)
            contentView.setDict(dict);
        if (adapter != null)
            adapter.setDict(dict, getString(R.string.empty_entry_list),
                    getString(R.string.invalid_dict));
        DictContentProvider.setDict(dict);
        if (dict != null && dict.isValid()) {
            dictPref = dict.getDictPref();
            currentEntry = new DictEntry(0, "", dictPref.getDictId());
            dict.getHeadword(currentEntry);
            if (adapter != null)
                adapter.setCurrentEntry(currentEntry);
        } else {
            dictPref = null;
            currentEntry = new DictEntry(-1, "", DictPref.kInvalidDictPrefId);
        }
        getSherlockActivity().invalidateOptionsMenu();
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
        fragmentContainer= container;
    }

    void updateDictWithRefresh(DictPref pref) {
        MdxEngine.getLibMgr().updateDictPref(pref);
        currentEntry.makeJEntry();
        DictEntry entry = new DictEntry(currentEntry);
        // setDict(dict); //setDict may change the value of currentEntry.
        displayByEntry(entry, false);
    }

    public void setChnConv(int convertType) {
        if (dict.isValid()) {
            dict.setChnConversion(convertType);
            updateDictWithRefresh(dict.getDictPref());
            dictPref = dict.getDictPref();
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
                dict.setViewSetting(pref);
                updateDictWithRefresh(pref);
            }
            dictPref = dict.getDictPref();
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
                SearchTrack st = history.get(history.size() - 1); // added by
                // alex
                depth -= 1;// added by alex
                displayByEntry(entry, false);
                final int sX = st.getScrollX();
                final int sY = st.getScrollY();

                contentView.getHtmlView().setPictureListener(
                        new PictureListener() {
                            @Override
                            public void onNewPicture(WebView view,
                                                     Picture picture) {
                                view.scrollTo(sX, sY);
                                view.setPictureListener(null);
                            }
                        });

				/*
                 * contentView.getHtmlView().post(new Runnable() {
				 * 
				 * @Override public void run() {
				 * contentView.getHtmlView().scrollTo(sX, sY); } });
				 */
                // contentView.getHtmlView().scrollTo(st.getScrollX(),
                // st.getScrollY()); //added by alex
                history.remove(history.size() - 1); // added by alex
            } else {
                switchToListView();
            }
            // }
            return true;
        }
        return false;
    }

    public void selectDict(int dictId) {
        String currentInput = searchView.getQuery().toString();
        int result = MdxEngine.openDictById(dictId, MdxEngine.getSettings()
                .getPrefsUseLRUForDictOrder(), dict);
        setDict(dict);
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
            AddonFuncUnt.showMessageDialog(getSherlockActivity(), info,
                    getString(R.string.error));
        }
    }

    public void toggleView() {
        if (currentView == headwordList)
            switchToContentView();
        else
            switchToListView();
    }

    public void switchToListView() {
        //no action for tablet
        if (!getResources().getBoolean(R.bool.screen_xlarge))
        {
            contentView.setVisibility(View.GONE);
            headwordList.setVisibility(View.VISIBLE);
        }
        if (MdxEngine.getSettings().getPrefAutoSIP() && !searchView.hasFocus())
            searchView.requestFocus();
        currentView = headwordList;
    }

    public void switchToContentView() {
        //no action for tablet
        if (!getResources().getBoolean(R.bool.screen_xlarge)){
            contentView.setVisibility(View.VISIBLE);
        }
        contentView.requestFocus();
        // InputMethodManager imm = (InputMethodManager)
        // getSupportActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        // imm.hideSoftInputFromWindow(inputBox.getWindowToken(), 0);
        //no action for tablet
        if (!getResources().getBoolean(R.bool.screen_xlarge)){
            headwordList.setVisibility(View.INVISIBLE);
        }
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
                SysUtil.getVersionName(getSherlockActivity()));
        displayHtml(html);
    }

    public void displayByEntry(DictEntry entry, boolean addToHistory) {
        switchToContentView();
        contentView.displayByEntry(entry, entry.isSysCmd() ? false : addToHistory);
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
        contentView.getHtmlView().setPictureListener(null);
    }

    private View.OnTouchListener logoIconTouchListener = new View.OnTouchListener() {
        int lastX, lastY;
        int initX, initY;

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
    private DictPref dictPref = null;
    private SearchView searchView;
    private DictEntry currentEntry = new DictEntry();
    private MdxAdapter adapter = null;
    private MdxView contentView = null;
    private ListView headwordList;
    private View currentView = null;
    private ViewGroup fragmentContainer=null;

    private ImageButton btnSpeak = null;
    private ImageButton btnAddToFav = null;
    private ImageButton btnJukuu = null;

    private boolean lastZoomActionIsZoomIn = false;
    private boolean hasToolbar =false;
    private boolean showHomeButtonInToolbar=true;
    private boolean skipUpdateEntryList = false;

    private TextToSpeech ttsEngine = null;
    public static final int kCheckTTSData = 0;

    private int depth = 0;

    private OnlineReference onlineReference=null;
    private Handler onlineReferenceHandler=null;

    private ArrayList<SearchTrack> history;
}