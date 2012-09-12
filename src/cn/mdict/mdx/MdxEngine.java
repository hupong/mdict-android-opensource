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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import cn.mdict.AddonFuncUnt;
import cn.mdict.MainForm;
import cn.mdict.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Class MdxEngine ...
 *
 * @author rayman
 * Created on 11-12-31
 */
public class MdxEngine {
    static {
        System.loadLibrary("iconv");
        System.loadLibrary("mdx");
    }
    private MdxEngine() {
		fInstance = getAppInstanceN();
	}

	/**
     * Method getSettings returns preference of the application.
     *
     * @return the view preference of the application.
     */
    static public MdxEngineSetting getSettings() {
        return appSetting;
	}


	/**
     * Method getLibMgr returns the libMgr of this MdxEngine object.
     *
     *
     *
     * @return the libMgr (type MdxLibraryMgrRef) of this MdxEngine object.
     */
    static public MdxLibraryMgrRef getLibMgr() {
		return  new MdxLibraryMgrRef(appOne.getLibMgrN());
	}

    /**
     * Method opendDictById opens a dictionary by passing the dictId and a dict object reference.
     * the pass in dict will be closed if it's a opened dict.
     *
     * @param dictId
     * @param dict
     * @return
     */
    static public int openDictById(int dictId, boolean adjustDictOrder, MdxDictBase dict){
        int res= appOne.openDictN(dictId, "", getSettings().getAppOwner().trim(), adjustDictOrder, dict);
        if (res==MdxDictBase.kMdxSuccess){
            rebuildHtmlSetting(dict, MdxEngine.getSettings().getPrefHighSpeedMode());
            MdxEngine.getSettings().setPrefLastDictId(dictId);
        }
        return res;
    }

    static public int openDictByPref(DictPref dictPref, MdxDictBase dict){
        int res= appOne.openDictByPrefN(dictPref, "", getSettings().getAppOwner().trim(), dict);
        if (res==MdxDictBase.kMdxSuccess){
            rebuildHtmlSetting(dict, MdxEngine.getSettings().getPrefHighSpeedMode());
        }
        return res;
    }

    static public int openDictByName(String dictName, MdxDictBase dict){
        DictPref dictPref=DictPref.createDictPref();
        dictPref.setDictName(dictName);
        dictPref.setDictId(1);
        return openDictByPref(dictPref, dict);
    }

    /**
     * Method initSettings ...
     *
     * @param appContext The application context
     */
    static public void initSettings(Context appContext){
        if ( appSetting==null )
            appSetting = new MdxEngineSetting(appContext);
    }

    static public void rebuildHtmlSetting(MdxDictBase dict, boolean highSpeedMode){
        
        String css="";
        if ( MdxEngine.getSettings().getPrefUseBuiltInIPAFont() ){
            css+=baseContext.getResources().getString(R.string.ipa_font_css);
        }

        StringBuffer css_buffer=new StringBuffer();
        String mdictCSS="";
        if ( AddonFuncUnt.loadStringFromFile(appOne.getDocDirN()+"mdict.css", css_buffer) ){
            if ( css_buffer.length()!=0 )
                css+=css_buffer.toString();
        }

        StringBuffer htmlBlock=new StringBuffer();

        htmlBlock.setLength(0);
        AddonFuncUnt.loadStringFromAsset(baseContext.getAssets(), "html_begin.html", htmlBlock, true);
        String htmlBegin =htmlBlock.toString()
                .replace("$start_expand_all$", MdxEngine.getSettings().getPrefMultiDictDefaultExpandAll().toString())
                .replace("$expand_single$", MdxEngine.getSettings().getPrefMultiDictExpandOnlyOne().toString())
                .replace("$extra_header$", css);

        htmlBlock.setLength(0);
        AddonFuncUnt.loadStringFromAsset(baseContext.getAssets(), "html_end.html", htmlBlock, true);
        String htmlEnd =htmlBlock.toString();

        htmlBlock.setLength(0);
        AddonFuncUnt.loadStringFromAsset(baseContext.getAssets(), highSpeedMode? "block_begin_h.html":"block_begin.html", htmlBlock, true);
        String blockBegin=htmlBlock.toString();

        htmlBlock.setLength(0);
        AddonFuncUnt.loadStringFromAsset(baseContext.getAssets(), highSpeedMode?"block_end_h.html":"block_end.html", htmlBlock, true);
        String blockEnd=htmlBlock.toString();

        htmlBlock.setLength(0);
        AddonFuncUnt.loadStringFromAsset(baseContext.getAssets(), "union_grp_title.html", htmlBlock, true);
        String unionGroupTitle=htmlBlock.toString();

        dict.setHtmlHeader(htmlBegin, htmlEnd);
        dict.setHtmlBlockHeader(blockBegin, blockEnd);
        if (!dict.canRandomAccess())
            dict.setUnionGroupTitle(unionGroupTitle);
    }

    static public void registerNotification(){
        NotificationManager nm = (NotificationManager) baseContext.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.app_icon_medium, baseContext.getString(R.string.app_name), System.currentTimeMillis());
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        Intent intent = new Intent(baseContext, MainForm.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(
                baseContext, R.string.app_name, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        notification.setLatestEventInfo( baseContext, baseContext.getString(R.string.app_name), "", contentIntent);
        nm.notify(R.string.app_name, notification);
    }

    static public void unregisterNotification(){
        NotificationManager nm = (NotificationManager) baseContext.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(R.string.app_name);
    }
    /**
     * Method initApp ...
     *
     * @return boolean
     */
    static public boolean initMDictEngine(Context context, String appHomeDir, String resDir, String tmpDir, ArrayList<String> extraSearchPath){
        boolean res=appOne.initAppN(appHomeDir, resDir, tmpDir, extraSearchPath);
        if (res){
            baseContext=context;
            if (getSettings().getPrefShowInNotification()){
                registerNotification();
            }
        }
        return res;
    }

    /**
     * Method saveEngineSettings ...
     * @return int
     */
    static public int saveEngineSettings(){
        return appOne.saveAllPreferenceN();
    }

    /**
     * Method getHistMgr returns the history of this MdxEngine object.
     *
     * @return the favorites (type DictBookmarkRef) of this MdxEngine object.
     */
    static public DictBookmarkRef getHistMgr(){
        return appOne.getHistoryN();
    }

    /**
     * Method getFavMgr returns the favorites of this MdxEngine object.
     *
     * @return the favorites (type DictBookmarkRef) of this MdxEngine object.
     */
    static public DictBookmarkRef getFavMgr(){
        return appOne.getFavoritesN();
    }

    /**
     * Method getSharedMdxData ...
     *
     * @param dataName of type String
     * @param convertKey of type boolean
     * @return byte[]
     */
    static public byte[] getSharedMdxData(String dataName, boolean convertKey){
        return appOne.getGlobalDataN(dataName, convertKey);
    }

    /**
     * Method hasSharedMdxData ...
     *
     * @param dataName of type String
     * @param convertKey of type boolean
     * @return boolean
     */
    static public boolean hasSharedMdxData(String dataName, boolean convertKey){
        return appOne.hasGlobalDataN(dataName, convertKey);
    }

    /**
     * Method refreshDictList ...
     */
    static public void refreshDictList(){
        appOne.refreshLibListN();
    }

    static public String getTempDir(){
        return appOne.getTmpDirN();
    }
    
    static public String getDataHomeDir(){
        return appOne.getDataHomeDirN();
    }

    static public String getDocDir(){
        return appOne.getDocDirN();
    }

    static public String getResDir(){
        return appOne.getResDirN();
    }

    // Native declarations
    private native int openDictN(int dictID, String deviceId, String email, boolean adjustDictOrder, MdxDictBase dict); // dict is java MdxDictBase
    private native int openDictByPrefN( DictPref dictPref, String deviceId, String email, MdxDictBase dict );

    private native int getAppInstanceN();
    private native int getLibMgrN();
    private native boolean initAppN(String appHomeDir, String resDir, String tmpDir, List<String> extraSearchPath);
    private native int saveAllPreferenceN();
    private native DictBookmarkRef getHistoryN();
    private native DictBookmarkRef getFavoritesN();
    private native byte[] getGlobalDataN(String dataName, boolean convertKey);
    private native boolean hasGlobalDataN(String dataName, boolean convertKey);
    private native void refreshLibListN();
    private native String getTmpDirN();
    private native String getDocDirN();
    private native String getDataHomeDirN();
    private native String getResDirN();

    private int fInstance;
    
    private static Context baseContext=null;
    private static MdxEngine appOne=new MdxEngine();
    private static MdxEngineSetting appSetting=null;


}
