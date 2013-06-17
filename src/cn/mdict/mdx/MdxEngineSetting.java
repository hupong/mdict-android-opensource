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

package cn.mdict.mdx;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import cn.mdict.R;

/**
 * Class MdxEngineSetting ...
 *
 * @author rayman
 *         Created on 11-12-31
 */
public class MdxEngineSetting {
    public static final int kSplitViewModeOff = 0;
    public static final int kSplitViewModeOn = 1;
    public static final int kSplitViewModeAuto = 2;

    public static String preferenceName = null;
    public static String prefLastDictId;
    public static String prefAppOwner;
    public static String prefAutoPlayPronunciation;
    public static String prefAutoLookupClipboard;
    public static String prefLockRotation;
    public static String prefShakeForRandomEntry;
    public static String prefUsePopoverForLookup;
    public static String prefAutoSIP;
    public static String prefMultiDictLookupMode;
    public static String prefChnConversion;
    public static String prefExtraDictDir;
    public static String prefShowSplash;
    public static String prefUseBuiltInIPAFont;
    public static String prefUseTTS;
    public static String prefTTSLocale;
    public static String prefPreferredTTSEngine;
    public static String prefShowToolbar;
    public static String prefMultiDictExpandOnlyOne;
    public static String prefMultiDictDefaultExpandAll;
    public static String prefUseFingerGesture;
    public static String prefHighSpeedMode;
    public static String prefMonitorClipboard;
    public static String prefShowInNotification;
    public static String prefUseLRUForDictOrder;
    public static String prefSplitViewMode;
    public static String prefFloatingWindowHeight;

    public static String prefGlobalClipboardMonitor;//Alex20121207.n
    public static String prefFixedDictTitle;//Alex20121207.n

    //Preference default values
    public static String prefDefaultTTSLocale;
    public static boolean prefDefaultAutoPlayPronunciation;
    public static boolean prefDefaultUseTTS;
    public static boolean prefDefaultShowSplash;
    public static boolean prefDefaultAutoLookupClipboard;
    public static boolean prefDefaultUseBuiltInIPAFont;
    public static boolean prefDefaultLockRotation;
    public static boolean prefDefaultUsePopoverForLookup;
    public static boolean prefDefaultShakeForRandomEntry;
    public static boolean prefDefaultShowToolbar;
    public static boolean prefDefaultAutoSIP;
    public static boolean prefDefaultMultiDictExpandOnlyOne;
    public static boolean prefDefaultMultiDictDefaultExpandAll;
    public static boolean prefDefaultUseFingerGesture;
    public static boolean prefDefaultHighSpeedMode;
    public static boolean prefDefaultMonitorClipboard;
    public static boolean prefDefaultShowInNotification;
    public static boolean prefDefaultUseLRUForDictOrder;
    public static String prefDefaultSplitViewMode;

    public static int prefDefaultFloatingWindowHeight;

    public static boolean prefDefaultGlobalClipboardMonitor;//Alex20121207.n
    public static boolean prefDefaultFixedDictTitle;//Alex20121207.n

    /**
     * Constructor MdxEngineSetting creates a new MdxEngineSetting instance.
     */
    public MdxEngineSetting(Context appContext) {
        Resources res = appContext.getResources();
        if (preferenceName == null) {
            preferenceName = res.getString(R.string.preference_name);
            prefLastDictId = res.getString(R.string.prefLast_dict_id);
            prefAppOwner = res.getString(R.string.pref_app_owner);
            prefAutoPlayPronunciation = res.getString(R.string.pref_auto_play_pronunciation);
            prefAutoLookupClipboard = res.getString(R.string.pref_auto_lookup_clipboard);
            prefLockRotation = res.getString(R.string.pref_lock_rotation);
            prefShakeForRandomEntry = res.getString(R.string.pref_shake_for_random_entry);
            prefUsePopoverForLookup = res.getString(R.string.pref_use_popover_for_lookup);
            prefAutoSIP = res.getString(R.string.pref_auto_sip);
            prefMultiDictLookupMode = res.getString(R.string.pref_multi_dict_lookup_mode);
            prefChnConversion = res.getString(R.string.pref_chn_conversion);
            prefExtraDictDir = res.getString(R.string.pref_extra_dict_dir);
            prefShowSplash = res.getString(R.string.pref_show_splash);
            prefUseBuiltInIPAFont = res.getString(R.string.pref_use_built_in_ipa_font);
            prefUseTTS = res.getString(R.string.pref_use_tts);
            prefTTSLocale = res.getString(R.string.pref_tts_locale);
            prefPreferredTTSEngine = res.getString(R.string.pref_preferred_tts_engine);
            prefShowToolbar = res.getString(R.string.pref_show_toolbar);
            prefMultiDictExpandOnlyOne = res.getString(R.string.pref_multi_dict_expand_only_one);
            prefMultiDictDefaultExpandAll = res.getString(R.string.pref_multi_dict_default_expand_all);
            prefUseFingerGesture = res.getString(R.string.pref_use_finger_gesture);
            prefHighSpeedMode = res.getString(R.string.pref_high_speed_mode);
            prefMonitorClipboard = res.getString(R.string.pref_monitor_clipboard);
            prefShowInNotification = res.getString(R.string.pref_show_in_notification);
            prefUseLRUForDictOrder = res.getString(R.string.pref_use_lru_for_dict_order);
            prefSplitViewMode = res.getString(R.string.pref_split_view_mode);

            prefFloatingWindowHeight = res.getString(R.string.pref_floating_window_height);
            prefGlobalClipboardMonitor = res.getString(R.string.pref_global_clipboard_monitor);//Alex20121207.n
            prefFixedDictTitle = res.getString(R.string.pref_fixed_dict_title);//Alex20121207.n

            prefDefaultTTSLocale = res.getString(R.string.pref_default_tts_locale);
            prefDefaultAutoPlayPronunciation = Boolean.parseBoolean(res.getString(R.string.pref_default_auto_play_pronunciation));
            prefDefaultUseTTS = Boolean.parseBoolean(res.getString(R.string.pref_default_use_tts));
            prefDefaultShowSplash = Boolean.parseBoolean(res.getString(R.string.pref_default_show_splash));
            prefDefaultAutoLookupClipboard = Boolean.parseBoolean(res.getString(R.string.pref_default_auto_lookup_clipboard));
            prefDefaultUseBuiltInIPAFont = Boolean.parseBoolean(res.getString(R.string.pref_default_use_built_in_ipa_font));
            prefDefaultLockRotation = Boolean.parseBoolean(res.getString(R.string.pref_default_lock_rotation));
            prefDefaultUsePopoverForLookup = Boolean.parseBoolean(res.getString(R.string.pref_default_use_popover_for_lookup));
            prefDefaultShakeForRandomEntry = Boolean.parseBoolean(res.getString(R.string.pref_default_shake_for_random_entry));
            prefDefaultShowToolbar = Boolean.parseBoolean(res.getString(R.string.pref_default_show_toolbar));
            prefDefaultAutoSIP = Boolean.parseBoolean(res.getString(R.string.pref_default_auto_sip));
            prefDefaultMultiDictDefaultExpandAll = Boolean.parseBoolean(res.getString(R.string.pref_default_multi_dict_default_expand_all));
            prefDefaultMultiDictExpandOnlyOne = Boolean.parseBoolean(res.getString(R.string.pref_default_multi_dict_expand_only_one));
            prefDefaultUseFingerGesture = Boolean.parseBoolean(res.getString(R.string.pref_default_use_finger_gesture));
            prefDefaultMonitorClipboard = Boolean.parseBoolean(res.getString(R.string.pref_default_monitor_clipboard));
            prefDefaultShowInNotification = Boolean.parseBoolean(res.getString(R.string.pref_default_show_in_notification));
            prefDefaultHighSpeedMode = Boolean.parseBoolean(res.getString(R.string.pref_default_high_speed_mode));
            prefDefaultUseLRUForDictOrder = Boolean.parseBoolean(res.getString(R.string.pref_default_use_lru_for_dict_order));
            prefDefaultSplitViewMode = res.getString(R.string.pref_default_split_view_mode);

            prefDefaultFloatingWindowHeight = Integer.parseInt(res.getString(R.string.pref_default_floating_window_height), 10);
            prefDefaultGlobalClipboardMonitor = Boolean.parseBoolean(res.getString(R.string.pref_default_global_clipboard_monitor));//Alex20121207.n
            prefDefaultFixedDictTitle = Boolean.parseBoolean(res.getString(R.string.pref_default_fixed_dict_title));//Alex20121207.n

        }
        this.appPrefs = appContext.getApplicationContext().getSharedPreferences(preferenceName, 0);
    }

    public SharedPreferences getSharedPreferences() {
        return appPrefs;
    }

    /**
     * Method getAppOwner returns the appOwner of this MdxEngineSetting object.
     *
     * @return the appOwner (type String) of this MdxEngineSetting object.
     */
    // Native declarations
    //获取和设置使用者的名字(邮件地址)
    public String getAppOwner() {
        return appPrefs.getString(prefAppOwner, "");
    }

    /**
     * Method setAppOwner sets the appOwner of this MdxEngineSetting object.
     *
     * @param ownerName the appOwner of this MdxEngineSetting object.
     */
    public void setAppOwner(String ownerName) {
        appPrefs.edit().putString(prefAppOwner, ownerName).commit();
    }

    /**
     * Method getPrefAutoPlayPronunciation returns the prefAutoPlayPronunciation of this MdxEngineSetting object.
     *
     * @return the prefAutoPlayPronunciation (type boolean) of this MdxEngineSetting object.
     */
    //在显示查询的单词解释后，自动播放发音
    public boolean getPrefAutoPlayPronunciation() {
        return appPrefs.getBoolean(prefAutoPlayPronunciation, prefDefaultAutoPlayPronunciation);
    }

    /**
     * Method setPrefAutoPlayPronunciation sets the prefAutoPlayPronunciation of this MdxEngineSetting object.
     *
     * @param on the prefAutoPlayPronunciation of this MdxEngineSetting object.
     */
    public void setPrefAutoPlayPronunciation(boolean on) {
        appPrefs.edit().putBoolean(prefAutoPlayPronunciation, on).commit();
    }

    /**
     * Method getPrefAutoLookupClipboard returns the prefAutoLookupClipboard of this MdxEngineSetting object.
     *
     * @return the prefAutoLookupClipboard (type boolean) of this MdxEngineSetting object.
     */
    //启动或者激活到前台时，自动查找剪贴板的内容
    public boolean getPrefAutoLookupClipboard() {
        return appPrefs.getBoolean(prefAutoLookupClipboard, prefDefaultAutoLookupClipboard);
    }

    /**
     * Method setPrefAutoLookupClipboard sets the prefAutoLookupClipboard of this MdxEngineSetting object.
     *
     * @param enable the prefAutoLookupClipboard of this MdxEngineSetting object.
     */
    public void setPrefAutoLookupClipboard(boolean enable) {
        appPrefs.edit().putBoolean(prefAutoLookupClipboard, enable).commit();
    }

    /**
     * Method getPrefLockRotation returns the prefLockRotation of this MdxEngineSetting object.
     *
     * @return the prefLockRotation (type boolean) of this MdxEngineSetting object.
     */
    //禁用重力旋屏
    public boolean getPrefLockRotation() {
        return appPrefs.getBoolean(prefLockRotation, prefDefaultLockRotation);
    }

    /**
     * Method setPrefLockRotation sets the prefLockRotation of this MdxEngineSetting object.
     *
     * @param enable the prefLockRotation of this MdxEngineSetting object.
     */
    public void setPrefLockRotation(boolean enable) {
        appPrefs.edit().putBoolean(prefLockRotation, enable).commit();
    }

    /**
     * Method getPrefRandomEntry returns the prefRandomEntry of this MdxEngineSetting object.
     *
     * @return the prefRandomEntry (type boolean) of this MdxEngineSetting object.
     */
    //摇晃设备时随机显示一个单词
    public boolean getPrefShakeForRandomEntry() {
        return appPrefs.getBoolean(prefShakeForRandomEntry, prefDefaultShakeForRandomEntry);
    }

    /**
     * Method setPrefRandomEntry sets the prefRandomEntry of this MdxEngineSetting object.
     *
     * @param enable the prefRandomEntry of this MdxEngineSetting object.
     */
    public void setPrefShakeForRandomEntry(boolean enable) {
        appPrefs.edit().putBoolean(prefShakeForRandomEntry, enable).commit();
    }

    /**
     * Method getPrefUsePopoverForLookup returns the prefUsePopoverForLookup of this MdxEngineSetting object.
     *
     * @return the prefUsePopoverForLookup (type boolean) of this MdxEngineSetting object.
     */
    //使用弹出窗口来显示内部查找
    public boolean getPrefUsePopoverForLookup() {
        return appPrefs.getBoolean(prefUsePopoverForLookup, prefDefaultUsePopoverForLookup);
    }

    /**
     * Method setPrefUsePopoverForLookup sets the prefUsePopoverForLookup of this MdxEngineSetting object.
     *
     * @param enable the prefUsePopoverForLookup of this MdxEngineSetting object.
     */
    public void setPrefUsePopoverForLookup(boolean enable) {
        appPrefs.edit().putBoolean(prefUsePopoverForLookup, enable).commit();
    }

    /**
     * Method getPrefAutoSIP returns the prefAutoSIP of this MdxEngineSetting object.
     *
     * @return the prefAutoSIP (type boolean) of this MdxEngineSetting object.
     */
    //是否在进入查找界面时将输入焦点设置为输入框(自动弹出输入法)
    public boolean getPrefAutoSIP() {
        return true;
        //return appPrefs.getBoolean(prefAutoSIP, true);
    }

    /**
     * Method setPrefAutoSIP sets the prefAutoSIP of this MdxEngineSetting object.
     *
     * @param enable the prefAutoSIP of this MdxEngineSetting object.
     */
    public void setPrefAutoSIP(boolean enable) {
        appPrefs.edit().putBoolean(prefAutoSIP, enable).commit();
    }

    /**
     * Method getPrefLookupMultiDict returns the prefLookupMultiDict of this MdxEngineSetting object.
     *
     * @return the prefLookupMultiDict (type boolean) of this MdxEngineSetting object.
     */
    //是否为多辞典同时查询模式
    public boolean getPrefMultiDictLookkupMode() {
        return appPrefs.getBoolean(prefMultiDictLookupMode, false);
    }

    /**
     * Method setPrefLookupMultiDict sets the prefLookupMultiDict of this MdxEngineSetting object.
     *
     * @param on the prefLookupMultiDict of this MdxEngineSetting object.
     */
    public void setPrefMultiDictLookupMode(boolean on) {
        appPrefs.edit().putBoolean(prefMultiDictLookupMode, on).commit();
    }

    /**
     * Method getPrefChnConversion returns the prefChnConversion of this MdxEngineSetting object.
     *
     * @return the prefChnConversion (type int) of this MdxEngineSetting object.
     */
    //设置默认中文转换类型(常见dict_pref.h)
    public int getPrefChnConversion() {
        return appPrefs.getInt(prefChnConversion, DictPref.kChnConvNone);
    }

    /**
     * Method setPrefChnConversion sets the prefChnConversion of this MdxEngineSetting object.
     *
     * @param toChn the prefChnConversion of this MdxEngineSetting object.
     */
    public void setPrefChnConversion(int toChn) {
        appPrefs.edit().putInt(prefChnConversion, toChn).commit();
    }

    /**
     * Method getPrefLastDictId returns the prefLastDictId of this MdxEngineSetting object.
     *
     * @return the prefLastDictId (type int) of this MdxEngineSetting object.
     */
    //最后使用的辞典id
    public int getPrefLastDictId() {
        return appPrefs.getInt(prefLastDictId, DictPref.kInvalidDictPrefId);
    }

    /**
     * Method setPrefLastDictId sets the prefLastDictId of this MdxEngineSetting object.
     *
     * @param lastId the prefLastDictId of this MdxEngineSetting object.
     */
    public void setPrefLastDictId(int lastId) {
        appPrefs.edit().putInt(prefLastDictId, lastId).commit();
    }

    public String getExtraDictDir() {
        return appPrefs.getString(prefExtraDictDir, "");
    }

    public void setExtraDictDir(String mediaDir) {
        appPrefs.edit().putString(prefExtraDictDir, mediaDir).commit();
    }

    public boolean getPrefShowSplash() {
        return appPrefs.getBoolean(prefShowSplash, prefDefaultShowSplash);
    }

    public void setPrefShowSplash(boolean showSplash) {
        appPrefs.edit().putBoolean(prefShowSplash, showSplash).commit();
    }

    public boolean getPrefUseBuiltInIPAFont() {
        return appPrefs.getBoolean(prefUseBuiltInIPAFont, prefDefaultUseBuiltInIPAFont);
    }

    public void setPrefUseBuiltInIPAFont(boolean useBuiltInIPAFont) {
        appPrefs.edit().putBoolean(prefUseBuiltInIPAFont, useBuiltInIPAFont).commit();
    }

    public boolean getPrefUseTTS() {
        return appPrefs.getBoolean(prefUseTTS, prefDefaultUseTTS);
    }

    public void setPrefUseTTS(boolean useTTS) {
        appPrefs.edit().putBoolean(prefUseTTS, useTTS).commit();
    }

    public String getPrefTTSLocale() {
        return appPrefs.getString(prefTTSLocale, prefDefaultTTSLocale);
    }

    public void setPrefTTSLocale(String locale) {
        appPrefs.edit().putString(prefTTSLocale, locale).commit();
    }

    public String getPrefPreferedTTSEngine() {
        return appPrefs.getString(prefPreferredTTSEngine, "");
    }

    public void setPrefPreferedTTSEngine(String engineName) {
        appPrefs.edit().putString(prefPreferredTTSEngine, engineName).commit();
    }

    public Boolean getPrefShowToolbar() {
        return appPrefs.getBoolean(prefShowToolbar, prefDefaultShowToolbar);
    }

    public void setPrefShowToolbar(boolean show) {
        appPrefs.edit().putBoolean(prefShowToolbar, show).commit();
    }

    public Boolean getPrefMultiDictExpandOnlyOne() {
        return appPrefs.getBoolean(prefMultiDictExpandOnlyOne, prefDefaultMultiDictExpandOnlyOne);
    }

    public void setPrefMultiDictExpandOnlyOne(boolean expandOnlyOne) {
        appPrefs.edit().putBoolean(prefMultiDictExpandOnlyOne, expandOnlyOne).commit();
    }

    public Boolean getPrefMultiDictDefaultExpandAll() {
        return appPrefs.getBoolean(prefMultiDictDefaultExpandAll, prefDefaultMultiDictDefaultExpandAll);
    }

    public void setPrefMultiDictDefaultExpandAll(boolean defaultExpandAll) {
        appPrefs.edit().putBoolean(prefMultiDictDefaultExpandAll, defaultExpandAll).commit();
    }

    public Boolean getPrefUseFingerGesture() {
        return appPrefs.getBoolean(prefUseFingerGesture, prefDefaultUseFingerGesture);
    }

    public void setPrefUseFingerGesture(boolean useFingerGesture) {
        appPrefs.edit().putBoolean(prefUseFingerGesture, useFingerGesture).commit();
    }

    public Boolean getPrefHighSpeedMode() {
        return appPrefs.getBoolean(prefHighSpeedMode, prefDefaultHighSpeedMode);
    }

    public void setPrefHighSpeedMode(boolean highSpeedMode) {
        appPrefs.edit().putBoolean(prefHighSpeedMode, highSpeedMode).commit();
    }

    public Boolean getPrefMonitorClipboard() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            return false;
        }else
            return appPrefs.getBoolean(prefMonitorClipboard, prefDefaultMonitorClipboard);
    }

    public void setPrefMonitorClipboard(boolean monitorClipboard) {
        appPrefs.edit().putBoolean(prefMonitorClipboard, monitorClipboard).commit();
    }

    public Boolean getPrefShowInNotification() {
        return appPrefs.getBoolean(prefShowInNotification, prefDefaultShowInNotification);
    }

    public void setPrefShowInNotification(boolean showInNotification) {
        appPrefs.edit().putBoolean(prefShowInNotification, showInNotification).commit();
    }

    public Boolean getPrefsUseLRUForDictOrder() {
        return appPrefs.getBoolean(prefUseLRUForDictOrder, prefDefaultUseLRUForDictOrder);
    }

    public void setPrefUseLRUForDictOrder(boolean useLRU) {
        appPrefs.edit().putBoolean(prefUseLRUForDictOrder, useLRU).commit();
    }

    public int getPrefSplitViewMode() {
        return Integer.parseInt(appPrefs.getString(prefSplitViewMode, prefDefaultSplitViewMode));
    }

    public void setPrefSplitViewMode(int splitViewMode) {
        if (splitViewMode > kSplitViewModeAuto)
            splitViewMode = kSplitViewModeAuto;
        appPrefs.edit().putString(prefSplitViewMode, Integer.toString(splitViewMode)).commit();
    }

    public int getPrefFloatingWindowHeight() {
        return appPrefs.getInt(prefFloatingWindowHeight, prefDefaultFloatingWindowHeight);
    }

    public void setPrefFloatingWindowHeight(int floatingWindowHeight) {
        appPrefs.edit().putInt(prefFloatingWindowHeight, floatingWindowHeight).commit();
    }

    //alex20121207.sn

    /**
     * Method getPrefGlobalClipboardMonitor returns the prefGlobalClipboardMonitor of this MdxEngineSetting object.
     *
     * @return the prefGlobalClipboardMonitor (type boolean) of this MdxEngineSetting object.
     */
    public Boolean getPrefGlobalClipboardMonitor() {
        return appPrefs.getBoolean(prefGlobalClipboardMonitor, prefDefaultGlobalClipboardMonitor);
    }

    /**
     * Method setPrefGlobalClipboardMonitor sets the prefGlobalClipboardMonitor of this MdxEngineSetting object.
     *
     * @param enable the prefGlobalClipboardMonitor of this MdxEngineSetting object.
     */
    public void setPrefGlobalClipboardMonitor(boolean enable) {
        appPrefs.edit().putBoolean(prefGlobalClipboardMonitor, enable).commit();
    }

    /**
     * Method getPrefFixedDictTitle returns the prefFixedDictTitle of this MdxEngineSetting object.
     *
     * @return the prefFixedDictTitle (type boolean) of this MdxEngineSetting object.
     */
    public Boolean getPrefFixedDictTitle() {
        return appPrefs.getBoolean(prefFixedDictTitle, prefDefaultFixedDictTitle);
    }

    /**
     * Method setPrefFixedDictTitle sets the prefFixedDictTitle of this MdxEngineSetting object.
     *
     * @param enable the prefFixedDictTitle of this MdxEngineSetting object.
     */
    public void setPrefFixedDictTitle(boolean enable) {
        appPrefs.edit().putBoolean(prefFixedDictTitle, enable).commit();
    }
    //alex20121207.en 


    private SharedPreferences appPrefs = null;
}
