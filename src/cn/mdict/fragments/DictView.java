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
import android.content.Intent;
import android.graphics.Picture;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
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
            deepth += 1;
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
        inputBox.setText(headWord);
    	if (history == null) //al20121205.sn
            history = new ArrayList<SearchTrack>();//al20121205.en
        deepth += 1;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.dict_view,
                container);

        setHasOptionsMenu(true);
        rootView.setFocusable(true);

        // Inflate the custom ic_view
        ActionBar actionBar = getSherlockActivity().getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setHomeButtonEnabled(true);

        customViewForDictView = (ViewGroup) inflater.inflate(
                R.layout.search_edit, null);
        actionBar.setCustomView(customViewForDictView);

        contentView = (MdxView) rootView.findViewById(R.id.mdx_view);
        // contentView = new MdxView(rootView.getContext());
        contentView.setMdxViewListener(this);
        // contentView.setBackgroundColor(android.R.color.white);
        // rootView.addView(contentView);

        // actionModeAgent=new SimpleActionModeCallbackAgent(R.menu.app_menu,
        // this);
        btnSpeak = (ImageButton) customViewForDictView.findViewById(R.id.speak);
        if (btnSpeak != null) {
            btnSpeak.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    contentView.playPronunciationForCurrentEntry();
                }
            });
            btnSpeak.setEnabled(false);
        }

        btnAddToFav = (ImageButton) customViewForDictView
                .findViewById(R.id.add_to_fav);
        if (btnAddToFav != null) {
            btnAddToFav.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    contentView.addCurrentEntryToFav();
                }
            });
            btnAddToFav.setEnabled(false);
        }

        inputBox = (EditText) (customViewForDictView
                .findViewById(R.id.search_field));
        btnClear = (ImageButton) actionBar.getCustomView().findViewById(
                R.id.search_clear_btn);

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
                imm.hideSoftInputFromWindow(inputBox.getWindowToken(), 0);
                if (entry != null) {
                    displayByEntry(entry, true);
                    deepth = 0; // added by alex
                    initSearchHistory();// added by alex
                }
            }
        });

        btnClear.setOnClickListener(new ImageButton.OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                inputBox.setText("");
                inputBox.requestFocus();
            }

        });

        focusChangeListener = new TextView.OnFocusChangeListener() {
            @Override
            public void onFocusChange(android.view.View view, boolean hasFocus) {
                InputMethodManager imm = (InputMethodManager) getSherlockActivity()
                        .getSystemService(
                                android.content.Context.INPUT_METHOD_SERVICE);
                if (hasFocus) {
                    switchToListView();
                    // inputBox.setText("");
                    if (MdxEngine.getSettings().getPrefAutoSIP())
                        imm.showSoftInput(inputBox, 0);
                    inputBox.selectAll();
                } else {
                    if (MdxEngine.getSettings().getPrefAutoSIP())
                        imm.hideSoftInputFromWindow(inputBox.getWindowToken(),
                                0);
                    // imm.hideSoftInputFromWindow(inputBox.getWindowToken(),
                    // 0);
                }
                btnClear.setVisibility(inputBox.getText().length() != 0 ? View.VISIBLE
                        : View.INVISIBLE);
                getSherlockActivity().invalidateOptionsMenu();

            }

        };

        inputBox.setOnFocusChangeListener(focusChangeListener);
        // Handle the enter key action
        inputBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
                String word = v.getText().toString();
                if (word.compareToIgnoreCase(":aboutapp") == 0) {
                    displayWelcome();
                    deepth = 0;
                    history = new ArrayList<SearchTrack>();// added by alex
                } else if (MdxDictBase.isMdxCmd(word)
                        || !currentEntry.isValid()) {
                    displayByHeadword(word, false);
                    deepth = 0;
                    initSearchHistory();// added by alex
                } else {
                    displayByEntry(currentEntry, true);
                    deepth = 0;
                    initSearchHistory();// added by alex
                }
                return false;
            }
        });

        inputBox.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                btnClear.setVisibility(inputBox.getText().length() != 0 ? View.VISIBLE
                        : View.INVISIBLE);
                if (!skipUpdateEntryList) {
                    currentEntry.setEntryNo(-1);
                    if (dict != null && dict.isValid()) {
                        String headword = s.toString();
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
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
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
        if (item.getItemId() == android.R.id.home) {
            getSherlockActivity().getSupportActionBar()
                    .setDisplayShowCustomEnabled(true);
            getSherlockActivity().getSupportActionBar().setCustomView(
                    customViewForDictView);
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
        inputBox.setText(text);
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

            if (entry != null && entry.isValid() && deepth > 0) {
                SearchTrack st = history.get(history.size() - 1); // added by
                // alex
                deepth -= 1;// added by alex
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
        String currentInput = inputBox.getText().toString();
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
        contentView.setVisibility(View.GONE);
        headwordList.setVisibility(View.VISIBLE);
        if (MdxEngine.getSettings().getPrefAutoSIP() && !inputBox.hasFocus())
            inputBox.requestFocus();
        currentView = headwordList;
    }

    public void switchToContentView() {
        contentView.setVisibility(View.VISIBLE);
        contentView.requestFocus();
        // InputMethodManager imm = (InputMethodManager)
        // getSupportActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        // imm.hideSoftInputFromWindow(inputBox.getWindowToken(), 0);
        headwordList.setVisibility(View.INVISIBLE);
        currentView = contentView;
    }

    public boolean isInputing() {
        return (currentView == headwordList && inputBox.isFocused());
    }

    public void syncSearchView() {
        setInputText(currentEntry.getHeadword(), true);
        // TODO should sync the entry list too
    }

    public void displayWelcome() {
        switchToContentView();
        StringBuffer welcome = new StringBuffer();
        AddonFuncUnt.loadStringFromAsset(getSherlockActivity().getAssets(),
                "Welcome.htm", welcome, true);
        String html = welcome.toString().replace("$version$",
                AddonFuncUnt.getVersionName(getSherlockActivity()));
        displayHtml(html);
    }

    public void displayByEntry(DictEntry entry, boolean addToHistory) {
        switchToContentView();
        contentView.displayByEntry(entry, entry.isSysCmd() ? false
                : addToHistory);
    }

    public void displayByHeadword(String headword, boolean addToHistory) {        /*
		 * if (addToHistory) { deepth += 1; SearchTrack st = new SearchTrack();
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

    private MdxDictBase dict = null;
    private DictPref dictPref = null;

    private EditText inputBox;
    private ViewGroup customViewForDictView;
    private View.OnFocusChangeListener focusChangeListener;
    // private ViewGroup customViewForAppView;
    private ImageButton btnClear;
    private DictEntry currentEntry = new DictEntry();
    private MdxAdapter adapter = null;
    private MdxView contentView = null;
    private ListView headwordList;
    private View currentView = null;
    private boolean skipUpdateEntryList = false;
    private boolean skipChangeFocus = false;
    // private SimpleActionModeCallbackAgent actionModeAgent;
    private ImageButton btnSpeak = null;
    private ImageButton btnAddToFav = null;

    private boolean lastZoomActionIsZoomIn = false;

    private TextToSpeech ttsEngine = null;
    public static final int kCheckTTSData = 0;

    private int deepth = 0;

    private ArrayList<SearchTrack> history;
}