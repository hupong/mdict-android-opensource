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

package cn.mdict.widgets;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import cn.mdict.MiscUtils;
import cn.mdict.R;
import cn.mdict.WebViewGestureFilter;
import cn.mdict.mdx.DictEntry;
import cn.mdict.mdx.MdxDictBase;
import cn.mdict.mdx.MdxEngine;
import cn.mdict.utils.WordSuggestion;

public class MdxView extends RelativeLayout {

    public interface MdxViewListener {
        // If events listener want to suppress the default action, it should return true.
        // Otherwise should return false.
        boolean onSearchText(MdxView view, String text, int touchPointX, int touchPointY);

        boolean onDisplayEntry(MdxView view, DictEntry entry, boolean addToHistory);

        boolean onHeadWordNotFound(MdxView view, String headWord, int touchPointX, int touchPointY);// added by alex

        boolean onPlayAudio(MdxView view, String path);

        //Default action is "jump to anchor"
        boolean onPageLoadCompleted(WebView view);
    }

    public boolean onPageLoadCompleted(WebView view) {
        return mdxViewListener != null && mdxViewListener.onPageLoadCompleted(view);
    }

    public MdxView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // ViewGroup.LayoutParams lp=new
        // ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
        // ViewGroup.LayoutParams.WRAP_CONTENT);
        // setLayoutParams(lp);
        LayoutInflater.from(context).inflate(R.layout.mdxview, this, true);
        // lp=getLayoutParams();
        setupControls();
    }

    public DictEntry getCurrentEntry() {
        return currentEntry;
    }

    public MdxDictBase getDict() {
        return dict;
    }

    public void setDict(MdxDictBase dict) {
        this.dict = dict;
        currentEntry.invalidate();
    }

    public void setMdxViewListener(MdxViewListener listener) {
        mdxViewListener = listener;
    }

    public MdxViewListener getMdxViewListener() {
        return mdxViewListener;
    }

    public void setGestureListener(WebViewGestureFilter.GestureListener listener) {
        entryViewSingle.setGestureListener(listener);
        entryViewList.setGestureListener(listener);
    }

    public void setTtsEngine(TextToSpeech engine) {
        ttsEngine = engine;
    }

    public void switchViewMode(boolean listMode) {
        // ViewGroup container=(ViewGroup)findViewById(R.id.contentview);
        ViewGroup container = this;
        if (listMode) {
            if (entryViewList.getContainer().getParent() == null) {
                container.addView(entryViewList.getContainer(), 0);
            }
            requestChildFocus(entryViewList.getContainer(), getFocusedChild());
            if (entryViewSingle.getContainer().getParent() != null)
                container.removeView(entryViewSingle.getContainer());
            currentViewer = entryViewList;
        } else {
            if (entryViewSingle.getContainer().getParent() == null) {
                container.addView(entryViewSingle.getContainer(), 0);
            }
            requestChildFocus(entryViewSingle.getContainer(), getFocusedChild());
            if (entryViewList.getContainer() != null
                    && entryViewList.getContainer().getParent() != null)
                container.removeView(entryViewList.getContainer());
            currentViewer = entryViewSingle;
        }

        // entryViewList.setVisibility(listMode?VISIBLE:GONE);
        // htmlView.setVisibility(listMode?GONE:VISIBLE);
    }

    public void displayAssetFile(String filename) {
        switchViewMode(false);
        entryViewSingle.displayAssetFile(filename);
        updateUIStateForEntry(null);
    }

    public void displayHtml(String info) {
        switchViewMode(false);
        entryViewSingle.displayHtml(info);
        updateUIStateForEntry(null);
    }

    protected void displayLocalizedInfo(int stringId) {
        displayHtml(getContext().getString(stringId));
    }

    public void displayByHeadword(String headword, boolean addToHistory) {
        if (dict != null && dict.isValid()) {
            DictEntry entry = new DictEntry(DictEntry.kInvalidEntryNo,
                    headword, dict.getDictPref().getDictId());
            if (MdxDictBase.isMdxCmd(headword)) {
                entry.setEntryNo(DictEntry.kSystemCmdEntryNo);
            } else {
                int r = dict.locateFirst(headword, true, false, false, entry);
                if (r != MdxDictBase.kMdxSuccess
                        //|| (entry.getHeadword().indexOf(" ") != -1 && headword.indexOf(" ") == -1)) {//alex20121205.o
                        || !entry.getHeadword().equals(headword)) {//alex20121205.n
                    String word = WordSuggestion.getMdxSuggestWord(getContext(), dict, headword);
                    if (word.length() > 0) {
                        entry.setHeadword(word);
                        dict.locateFirst(word, true, false, false, entry);
                        // entry.setHeadword(word);
                    } else {
                        if (r != MdxDictBase.kMdxSuccess)
                            entry.setHeadword(headword);
                    }
                }
            }
            displayByEntry(entry, addToHistory);
        }
    }

    public void displayByEntry(DictEntry entry, boolean addToHistory) {
        currentEntry = new DictEntry(entry);
        updateUIStateForEntry(currentEntry);
        currentEntry.dumpEntryInfo();
        if (dict != null && dict.isValid()) {
            if (entry.isValid() || entry.isSysCmd()) { // TODO should handle
                // syscmd here
                if (mdxViewListener != null) {
                    if (mdxViewListener.onDisplayEntry(this, currentEntry,
                            addToHistory))
                        return;
                }
                if (MdxEngine.getSettings().getPrefHighSpeedMode()) {
                    switchViewMode(false);
                    entryViewSingle.displayEntry(currentEntry);
                } else {
                    // htmlView.loadUrl(String.format("content://mdict.cn/mdx/%d/%d/%s/",
                    // currentEntry.getDictId(), currentEntry.getEntryNo(),
                    // currentEntry.getHeadword()));

                    if (currentEntry.isUnionDictEntry()) {
                        //switchViewMode(true);
                        //entryViewList.displayEntry(currentEntry);
                        switchViewMode(false);
                        entryViewSingle.displayEntry(currentEntry);

                        //TODO: should scroll view to webview if "Show only one entry"
                    } else {
                        switchViewMode(false);
                        entryViewSingle.loadUrl(String.format(
                                "content://mdict.cn/mdx/%d/%d/%s/",
                                currentEntry.getDictId(),
                                currentEntry.getEntryNo(),
                                currentEntry.getHeadword()));
                    }
                }
            } else {
                // added by alex started
                if (mdxViewListener != null) {
                    mdxViewListener.onHeadWordNotFound(this, currentEntry
                            .getHeadword(), getHtmlView().getScrollX(),
                            getHtmlView().getScrollY());
                }
                String str = String.format(
                        getContext().getString(R.string.headword_not_found),
                        currentEntry.getHeadword());
                // this.getHtmlView().loadUrl(url);
                String wordList = WordSuggestion
                        .getMdxSuggestWordList(this.getContext(), dict,
                                currentEntry.getHeadword());
                if (wordList.length() > 0) {
                    str = String.format(getContext().getString(R.string.headword_not_found_suggestion), currentEntry.getHeadword(), wordList);
                }
                // String str=String.format(
                // getContext().getString(R.string.headword_not_found),
                // currentEntry.getHeadword());
                displayHtml(str);
            }
            if (!entry.isSysCmd() && addToHistory) {
                MdxEngine.getHistMgr().add(currentEntry);
            }
        } else {
            displayLocalizedInfo(R.string.no_dict_selected);
        }
    }

    private void updateUIStateForEntry(DictEntry entry) {
        if (btnAddToFav != null && btnSpeak != null) {
            if (entry != null) {
                int show = entry.isValid() ? View.VISIBLE : View.INVISIBLE;
                btnAddToFav.setVisibility(show);
                boolean showSpeak = (ttsEngine != null)
                        && MdxEngine.getSettings().getPrefUseTTS();
                showSpeak = showSpeak
                        || MiscUtils.hasSpeechForWord(dict,
                        entry.getHeadword());
                show = showSpeak ? View.VISIBLE : View.INVISIBLE;
                btnSpeak.setVisibility(show);
            } else {
                btnAddToFav.setVisibility(View.INVISIBLE);
                btnSpeak.setVisibility(View.INVISIBLE);
            }
        }
    }

    public void displayHistPrev() {
        if (MdxEngine.getHistMgr().hasPrev()) {
            DictEntry entry = MdxEngine.getHistMgr().getPrev();
            if (entry.isValid()) {
                displayByEntry(entry, false);
            } else {
                displayByHeadword(entry.getHeadword(), false);
            }
        }
    }

    public void displayHistNext() {
        if (MdxEngine.getHistMgr().hasNext()) {
            DictEntry entry = MdxEngine.getHistMgr().getNext();
            if (entry.isValid()) {
                displayByEntry(entry, false);
            } else {
                entry.makeJEntry();
                displayByHeadword(entry.getHeadword(), false);
            }
        }
    }

    public void displayEntryPrev() {
        if (currentEntry.getEntryNo() != 0) {
            currentEntry.setEntryNo(currentEntry.getEntryNo() - 1);
            dict.getHeadword(currentEntry);
            displayByEntry(currentEntry, true);
        }
    }

    public void displayEntryNext() {
        if (currentEntry.getEntryNo() < dict.getEntryCount() - 1) {
            currentEntry.setEntryNo(currentEntry.getEntryNo() + 1);
            dict.getHeadword(currentEntry);
            displayByEntry(currentEntry, true);
        }
    }

    public void toggleZoom() {
        if (!lastZoomActionIsZoomIn)
            zoomIn();
        else
            zoomOut();
    }

    public void zoomIn() {
        currentViewer.zoomIn();
        lastZoomActionIsZoomIn = true;
    }

    public void zoomOut() {
        currentViewer.zoomOut();
        lastZoomActionIsZoomIn = false;
    }

    public void showAllEntries(boolean show) {
        currentViewer.showAllEntries(show);
    }

	/*
     * public void selectAndCopyText() { try { Method m =
	 * WebView.class.getMethod("emulateShiftHeld", null); m.invoke(htmlView,
	 * null); } catch (Exception e) { e.printStackTrace(); // fallback KeyEvent
	 * shiftPressEvent = new KeyEvent(0,0,
	 * KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_SHIFT_LEFT,0,0);
	 * shiftPressEvent.dispatch(this); } }
	 */

    private void setupControls() {
        // htmlView=new WebView(getContext());
        // this.addView(htmlView);
        // TableLayout toolbar=(TableLayout)findViewById(R.id.toolbar);

        btnSpeak = (ImageButton) findViewById(R.id.speak);
        if (btnSpeak != null) {
            btnSpeak.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    playPronunciationForCurrentEntry();
                }
            });
        }
        btnAddToFav = (ImageButton) findViewById(R.id.add_to_fav);
        if (btnAddToFav != null) {
            btnAddToFav.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    addCurrentEntryToFav();
                }
            });
        }

        entryViewList = new EntryViewList(getContext(),
                (ScrollView) findViewById(R.id.entryListView));

        entryViewList.setMdxView(this);
        entryViewSingle = new EntryViewSingle(getContext(),
                (WebView) findViewById(R.id.entrySingleView));
        entryViewSingle.setMdxView(this);
    }

    public void addCurrentEntryToFav() {
        if (currentEntry.isValid()) {
            MdxEngine.getFavMgr().add(currentEntry);
            Toast.makeText(
                    getContext(),
                    String.format(
                            getResources().getString(
                                    R.string.entry_added_to_fav),
                            currentEntry.getHeadword()), Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public void playPronunciationForCurrentEntry() {
        if (currentEntry != null && currentEntry.isValid()
                && currentEntry.getHeadword().length() != 0) {
            if (!MiscUtils
                    .playAudioForWord(dict, currentEntry.getHeadword())
                    && ttsEngine != null
                    && MdxEngine.getSettings().getPrefUseTTS()) {
                String headword = currentEntry.getHeadword().trim();
                StringBuilder hw = new StringBuilder(currentEntry.getHeadword()
                        .length());
                char c;
                for (int i = 0; i < headword.length(); ++i) {
                    c = headword.charAt(i);
                    if (c == ' ' || c >= '1')
                        hw.append(c);
                }
                if (hw.length() > 0)
                    ttsEngine.speak(hw.toString(), TextToSpeech.QUEUE_FLUSH,
                            null);
                else
                    ttsEngine.speak(headword, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    public void playAudio(String path) {
        if (mdxViewListener != null) {
            if (!mdxViewListener.onPlayAudio(this, path)) {
                MiscUtils.playAudio(dict, path);
            }
        }
    }

    private DictEntry currentEntry = new DictEntry();
    private MdxEntryView currentViewer = null;

    private EntryViewSingle entryViewSingle = null;
    private EntryViewList entryViewList = null;

    private MdxDictBase dict = null;
    private MdxViewListener mdxViewListener = null;
    // private TableLayout toolbar=null;
    private ImageButton btnAddToFav = null;
    private ImageButton btnSpeak = null;
    private boolean lastZoomActionIsZoomIn = false;
    private TextToSpeech ttsEngine = null;

    // added by alex
    public WebView getHtmlView() {
        return (WebView) entryViewSingle.getContainer();
    }
}