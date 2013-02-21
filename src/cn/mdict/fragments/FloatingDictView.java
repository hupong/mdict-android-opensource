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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.util.DisplayMetrics;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import cn.mdict.*;
import cn.mdict.mdx.DictEntry;
import cn.mdict.mdx.DictPref;
import cn.mdict.mdx.MdxDictBase;
import cn.mdict.mdx.MdxEngine;
import cn.mdict.utils.IOUtil;
import cn.mdict.utils.SysUtil;
import cn.mdict.widgets.MdxAdapter;
import cn.mdict.widgets.MdxView;
import cn.mdict.widgets.MdxView.MdxViewListener;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FloatingDictView extends SherlockFragment implements
		MdxViewListener, SimpleActionModeCallbackAgent.ActionItemClickListener,
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
		setCurrentEntry(entry);
		// getActivity().invalidateOptionsMenu();

		if (btnSpeak != null)
			btnSpeak.setEnabled(hasPronunciationForCurrentEntry());
		if (btnAddToFav != null)
			btnAddToFav.setEnabled(currentEntry != null
					&& currentEntry.isValid());

		entry.makeJEntry();
		syncSearchView();

		// getSherlockActivity().invalidateOptionsMenu();
		if (MdxEngine.getSettings().getPrefAutoPlayPronunciation()) {
			contentView.playPronunciationForCurrentEntry();
		}
		return false;
	}

	@Override
	public boolean onPlayAudio(MdxView view, String path) {
		return false;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		DisplayMetrics dm = getResources().getDisplayMetrics();
		screenWidth = dm.widthPixels;
		screenHeight = dm.heightPixels - 50;

		ViewGroup rootView = (ViewGroup) inflater.inflate(
				R.layout.floating_dict_view, container);
		// viewContainer

		setHasOptionsMenu(true);
		rootView.setFocusable(true);

		// Inflate the custom ic_view
		ActionBar actionBar = getSherlockActivity().getSupportActionBar();
		actionBar.hide();

		// customViewForDictView = (ViewGroup)
		// rootView.findViewById(R.layout.search_view);

		dictViewContainer = (FrameLayout) rootView
				.findViewById(R.id.dict_view_container);
		contentView = (MdxView) rootView.findViewById(R.id.mdx_view);
		// contentView = new MdxView(rootView.getContext());
		contentView.setMdxViewListener(this);

		jukuuHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.arg1) {
				case 1:
                    //Lookup online content failed.
				case 0:
                    jukuuWebView.loadDataWithBaseURL(null,
                            msg.obj.toString(), "text/html", "utf-8", null);
					break;

				}
				// if(msg.arg1=100)//search word from web
				// {

				super.handleMessage(msg);
			}
		};

		jukuuWebView = (WebView) rootView.findViewById(R.id.webview_jukuu);

		WebSettings webSettings = jukuuWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
		webSettings.setBuiltInZoomControls(true);
		// webview display rule
		jukuuWebView.setWebViewClient(new WebViewClientEmb());
		jukuuWebView.setWebChromeClient(new WebChromeClient());

		jukuuWebView.addJavascriptInterface(new Object() {
			@SuppressWarnings("unused")
			public void loopDictionary(String x, String Y, String selectedText) {
				Message msg = new Message();
				msg.arg1 = 100;
				msg.obj = selectedText;
				jukuuHandler.sendMessage(msg);// 向Handler发送消息，
			}
		}, "MdxDict");

		btnJukuu = (ImageButton) rootView.findViewById(R.id.float_jukuu_btn);
		if (btnJukuu != null) {
			btnJukuu.setOnLongClickListener(new View.OnLongClickListener() {
				public boolean onLongClick(View v) {
					if (lastJukuuWord != null) {
						dictViewContainer.setVisibility(View.GONE);
						jukuuWebView.setVisibility(ViewGroup.VISIBLE);
					}
					return true;
				}
			});
			btnJukuu.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					showJukuu();
				}
			});
			// btnJukuu.setEnabled(false);
		}
		// contentView.setBackgroundColor(android.R.color.white);
		// rootView.addView(contentView);

		// actionModeAgent=new SimpleActionModeCallbackAgent(R.menu.app_menu,
		// this);
		btnSpeak = (ImageButton) rootView.findViewById(R.id.speak);
		if (btnSpeak != null) {
			btnSpeak.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					contentView.playPronunciationForCurrentEntry();
				}
			});
			btnSpeak.setEnabled(false);
		}

		btnAddToFav = (ImageButton) rootView.findViewById(R.id.add_to_fav);
		if (btnAddToFav != null) {
			btnAddToFav.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					contentView.addCurrentEntryToFav();
				}
			});
			btnAddToFav.setEnabled(false);
		}

		inputBox = (EditText) (rootView
				.findViewById(R.id.floating_search_field));
		originalTextBoxWidth = inputBox.getLayoutParams().width;

		btnClear = (ImageButton) rootView
				.findViewById(R.id.floating_search_clear_btn);
		btnLogoIcon = (ImageButton) rootView
				.findViewById(R.id.float_search_logo_btn);

		btnLogoIcon.setOnTouchListener(this.logoIconTouchListener);

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
				if (entry != null)
					displayByEntry(entry, true);
			}
		});

		btnClear.setOnClickListener(new ImageButton.OnClickListener() {
			@Override
			public void onClick(View view) {
				inputBox.setText("");
				inputBox.requestFocus();
			}

		});

		focusChangeListener = new TextView.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View view, boolean hasFocus) {
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
				// btnClear.setVisibility(inputBox.getText().length() != 0 ?
				// View.VISIBLE
				// : View.INVISIBLE);
				setClearButton();
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
				if (word.compareToIgnoreCase(":aboutapp") == 0)
					displayWelcome();
				else if (MdxDictBase.isMdxCmd(word) || !currentEntry.isValid())
					displayByHeadword(word, false);
				else
					displayByEntry(currentEntry, true);
				return false;
			}
		});

		inputBox.addTextChangedListener(new android.text.TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				// btnClear.setVisibility(inputBox.getText().length() != 0 ?
				// View.VISIBLE
				// : View.INVISIBLE);
				setClearButton();
				if (!skipUpdateEntryList) {
					currentEntry.setEntryNo(-1);
					if (dict != null && dict.isValid()) {
						String headword = s.toString();
						dict.locateFirst(headword, true, true, true,
								currentEntry);
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

	protected void setClearButton() {
		if (inputBox.getText().length() != 0) {
			// inputBox.getLayoutParams().width =
			// originalTextBoxWidth-btnClear.getLayoutParams().width;
			// btnClear.setRight(originalTextBoxWidth-btnClear.getWidth());
			btnClear.setVisibility(View.VISIBLE);
		} else {
			btnClear.setVisibility(View.INVISIBLE);
		}
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

					int left = viewContainer.getLeft() + dx;
					int top = viewContainer.getTop() + dy;
					int right = viewContainer.getRight() + dx;
					int bottom = viewContainer.getBottom() + dy;
					// 设置不能出界
					if (left < 0) {
						left = 0;
						right = left + viewContainer.getWidth();
					}

					// if (right > screenWidth) {
					// right = screenWidth;
					// left = right - viewContainer.getWidth();
					// }

					if (top < 0) {
						top = 0;
						bottom = top + viewContainer.getHeight();
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

					viewContainer.layout(left, top, right, bottom);

					lastX = (int) event.getRawX();
					lastY = (int) event.getRawY();
				}
				break;
			case MotionEvent.ACTION_UP:
				if (Math.abs(initX - (int) event.getRawX()) < 10
						&& Math.abs(initY - (int) event.getRawY()) < 10) {
					viewContainer.setVisibility(View.GONE);
					Intent mIntent = new Intent();
					mIntent.putExtra("HEADWORD", currentEntry.getHeadword());
					mIntent.setClass(viewContainer.getContext(), MainForm.class);
					startActivity(mIntent);
					((FloatingForm) viewContainer.getContext()).finish();
				}
				break;
			}
			return true;
		}
	};

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
			// getSherlockActivity().getSupportActionBar().setCustomView(customViewForDictView);
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
			DictEntry entry = MdxEngine.getHistMgr().getPrev();
			if (entry != null && entry.isValid()) {
				if (entry.getEntryNo() != -1)
					displayByEntry(entry, false);
				else
					switchToListView();
			} else {
				switchToListView();
			}
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
		IOUtil.loadStringFromAsset(getSherlockActivity().getAssets(),
                "Welcome.htm", welcome, true);
		String html = welcome.toString().replace("$version$",
				SysUtil.getVersionName(getSherlockActivity()));
		displayHtml(html);
	}

	public void displayByEntry(DictEntry entry, boolean addToHistory) {
		switchToContentView();
		contentView.displayByEntry(entry, entry.isSysCmd() ? false
				: addToHistory);
	}

	public void displayByHeadword(String headword, boolean addToHistory) {
		switchToContentView();
		contentView.displayByHeadword(headword, addToHistory);
		searchHeadword = headword;
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

	public void setViewContainer(RelativeLayout container) {
		viewContainer = container;
	}

	protected void showJukuu() {
		dictViewContainer.setVisibility(View.GONE);
		jukuuWebView.setVisibility(ViewGroup.VISIBLE);
		if (inputBox.getText().length() > 0) {
			searchHeadword = inputBox.getText().toString();
		}

		// jukuuWebContent = "";
		jukuuWebView.loadDataWithBaseURL(null, "Loading..., please wait!",
				"text/html", "utf-8", null);

	}

	@Override
	public boolean onHeadWordNotFound(MdxView view, String headWord,
			int scrollX, int scrollY) {
		// TODO Auto-generated method stub
            inputBox.setText(headWord);
		return false;
	}

	public WebView getHtmlView() {
		return contentView.getHtmlView();
	}

	private String searchHeadword = "";
	private MdxDictBase dict = null;
	private DictPref dictPref = null;

	private int screenWidth;
	private int screenHeight;

	private int originalTextBoxWidth;
	private EditText inputBox;
	// private ViewGroup customViewForDictView;
	private View.OnFocusChangeListener focusChangeListener;
	// private ViewGroup customViewForAppView;
	private ImageButton btnClear;
	private ImageButton btnJukuu;
	private ImageButton btnLogoIcon;
	private DictEntry currentEntry = new DictEntry();
	private FrameLayout dictViewContainer;
	private MdxAdapter adapter = null;
	private MdxView contentView = null;
	private ListView headwordList;
	private WebView jukuuWebView;
	private View currentView = null;
	private boolean skipUpdateEntryList = false;
	private boolean skipChangeFocus = false;
	// private SimpleActionModeCallbackAgent actionModeAgent;
	private ImageButton btnSpeak = null;
	private ImageButton btnAddToFav = null;

	private boolean lastZoomActionIsZoomIn = false;

	private TextToSpeech ttsEngine = null;
	public static final int kCheckTTSData = 0;
	private RelativeLayout viewContainer = null;

	// private String jukuuWebContent;
	private String lastJukuuWord;
	private Handler jukuuHandler;

}