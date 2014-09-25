/*
 * Copyright (C) 2013. Rayman Zhang <raymanzhang@gmail.com>
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
import android.util.Log;
import cn.mdict.mdx.DictPref;
import cn.mdict.mdx.MdxDictBase;
import cn.mdict.mdx.MdxEngine;

/**
 * Created with IntelliJ IDEA.
 * User: rayman
 * Date: 13-2-25
 * Time: 上午9:40
 */
public class MDictApp {
    static {
        System.loadLibrary("iconv");
        System.loadLibrary("mdx");
    }

    private static MDictApp instance=null;
    private static MdxDictBase mainDict=null;
    private static MdxDictBase popupDict=null;

    private static final String TAG="MDict.MDictApp";

    private MDictApp(){
    }

    public static MDictApp getInstance(){
        if (instance==null){
            instance=new MDictApp();
        }
        return instance;
    }

    public boolean setupAppEnv(Context context)
    {
        return setupAppEnv(context, false);
    }
    public boolean setupAppEnv(Context context, boolean reInit){
        Log.d(TAG, "Setup App, Pid:"+android.os.Process.myPid());
        boolean res=MdxEngine.setupEnv(context, reInit);
        if (res ){
            mainDict=new MdxDictBase();
            popupDict=mainDict;
        }
        return res;
    }

    public MdxDictBase getMainDict(){
        return mainDict;
    }

    public int openMainDictById(int dictId){
        int retCode;
        if (dictId!= DictPref.kInvalidDictPrefId)
            retCode=MdxEngine.openDictById(dictId, MdxEngine.getSettings().getPrefsUseLRUForDictOrder(), mainDict);
        else
            retCode=MdxEngine.openLastDict(mainDict);
        return retCode;
    }

    public MdxDictBase getPopupDict(){
        return popupDict;
    }

    public int openPopupDictById(int dictId){
        int retCode=MdxDictBase.kMdxDatabaseNotInited;
        if (!popupDict.isValid()){
            retCode=openMainDictById(dictId);
            popupDict=mainDict;
        }
        return retCode;
    }

    public void rebuildAllDictSetting(){
        if (mainDict != null)
            MdxEngine.rebuildHtmlSetting(mainDict, MdxEngine.getSettings().getPrefHighSpeedMode());
        if (popupDict!=mainDict && popupDict!=null){
            MdxEngine.rebuildHtmlSetting(popupDict, MdxEngine.getSettings().getPrefHighSpeedMode());
        }
    }
}
