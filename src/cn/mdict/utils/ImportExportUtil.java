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

package cn.mdict.utils;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

import cn.mdict.MDictApp;
import cn.mdict.MiscUtils;
import cn.mdict.R;
import cn.mdict.mdx.DictPref;
import cn.mdict.mdx.MdxEngine;

/**
 * Created by Alex on 10/11/2014.
 */
public class ImportExportUtil {
    public static boolean exportDictSettings(Context context) {
        boolean success = false;
        try {
            DictPref rootDictRef = MdxEngine.getLibMgr().getRootDictPref();
            JSONObject rootDictJson = new JSONObject();
            rootDictJson.put("ExtraDictDir", MdxEngine.getSettings().getExtraDictDir());
            rootDictJson.put("MultiDictLookkupMode", MdxEngine.getSettings().getPrefMultiDictLookkupMode());
            rootDictJson.put("MultiDictDefaultExpandAll", MdxEngine.getSettings().getPrefMultiDictDefaultExpandAll());
            rootDictJson.put("MultiDictExpandOnlyOne", MdxEngine.getSettings().getPrefMultiDictExpandOnlyOne());
            rootDictJson.put("LockRotation", MdxEngine.getSettings().getPrefLockRotation());
            rootDictJson.put("MonitorClipboard", MdxEngine.getSettings().getPrefMonitorClipboard());
            JSONArray dictGroupJsonArray = new JSONArray();
            for (int i = 0; i < rootDictRef.getChildCount(); i++) {
                DictPref dictGroup = rootDictRef.getChildDictPrefAtIndex(i);
                //Dict Group
                JSONObject dictGroupJson = new JSONObject();
                dictGroupJson.put("DictId", dictGroup.getDictId());
                dictGroupJson.put("DictName", dictGroup.getDictName());
                dictGroupJson.put("Disabled", dictGroup.isDisabled());
                JSONArray childDictJsonArray = new JSONArray();
                for (int j = 0; j < dictGroup.getChildCount(); j++) {
                    DictPref childDictPref = dictGroup.getChildDictPrefAtIndex(j);
                    //Child Dict
                    JSONObject childDictJson = new JSONObject();
                    childDictJson.put("DictId", childDictPref.getDictId());
                    childDictJson.put("DictName", childDictPref.getDictName());
                    childDictJson.put("Disabled", childDictPref.isDisabled());
                    childDictJsonArray.put(childDictJson);
                }
                dictGroupJson.put("ChildDicts", childDictJsonArray);
                if (dictGroup.getDictId() == DictPref.kDefaultGrpId) {
                    rootDictJson.put("DefaultGroup", dictGroupJson);
                } else {

                    dictGroupJsonArray.put(dictGroupJson);
                }
            }
            rootDictJson.put("ChildGroups", dictGroupJsonArray);
            //String saveFile = MdxEngine.getDocDir() + "";
            IOUtil.saveStringToFile(MdxEngine.getDocDir() + "/LibrariesSettings.json",
                    rootDictJson.toString(2), "utf-8");
            MiscUtils.showMessageDialog(context, context.getText(R.string.msg_success).toString(), context.getText(R.string.title_information).toString());
            success = true;
        } catch (Exception e) {
            MiscUtils.showMessageDialog(context, MiscUtils.getErrorMessage(e), context.getText(R.string.warning).toString());
            success = false;
            e.printStackTrace();
        }
        return success;
    }

    public static boolean importDictSettings(Context context, MDictApp theApp) {
        boolean success = false;

        try {
            File file = new File(MdxEngine.getDocDir() + "/LibrariesSettings.json");
            if (!file.exists()) {
                MiscUtils.showMessageDialog(context, context.getText(R.string.no_exported_file).toString(), context.getText(R.string.warning).toString());
                return false;
            }
            DictPref rootDictRef = MdxEngine.getLibMgr().getRootDictPref();
            file = new File(MdxEngine.getDocDir() + "/MDict.cfg");
            if (file.exists()) {
                file.delete();
            }

            StringBuffer sbJson = new StringBuffer();
            boolean fileLoaded = IOUtil.loadStringFromFile(MdxEngine.getDocDir() + "/LibrariesSettings.json", sbJson);
            if (fileLoaded) {
                JSONObject rootDictJson = new JSONObject(sbJson.toString());
                MdxEngine.getSettings().setExtraDictDir(rootDictJson.getString("ExtraDictDir"));


                MdxEngine.getSettings().setPrefMultiDictDefaultExpandAll(rootDictJson.getBoolean("MultiDictDefaultExpandAll"));
                MdxEngine.getSettings().setPrefMultiDictExpandOnlyOne(rootDictJson.getBoolean("MultiDictExpandOnlyOne"));
                MdxEngine.getSettings().setPrefLockRotation(rootDictJson.getBoolean("LockRotation"));
                MdxEngine.getSettings().setPrefMonitorClipboard(rootDictJson.getBoolean("MonitorClipboard"));

                MdxEngine.saveEngineSettings();

                //Remove old groups
                int count = rootDictRef.getChildCount();

                while (count > 1) {
                    for (int i = 0; i < rootDictRef.getChildCount(); i++) {
                        DictPref dictGroup = rootDictRef.getChildDictPrefAtIndex(i);
                        if (dictGroup.getDictId() != DictPref.kDefaultGrpId) {
                            for (int j = 0; j < dictGroup.getChildCount(); j++) {
                                DictPref childDictPref = dictGroup.getChildDictPrefAtIndex(j);
                                dictGroup.removeChildDictPref(childDictPref.getDictId());
                                dictGroup.updateChildPref(childDictPref);
                                rootDictRef.updateChildPref(dictGroup);
                            }
                            rootDictRef.removeChildDictPref(dictGroup.getDictId());
                            rootDictRef.updateChildPref(dictGroup);
                            count--;
                        }
                    }
                }
                MdxEngine.getLibMgr().updateDictPref(rootDictRef);

                DictPref defaultDictGroup = null;
                //Add libraries to default dict group
                for (int i = 0; i < rootDictRef.getChildCount(); i++) {
                    DictPref childPref = rootDictRef.getChildDictPrefAtIndex(i);
                    if (childPref.getDictId() == DictPref.kDefaultGrpId) {
                        JSONObject dictGroupJson = rootDictJson.getJSONObject("DefaultGroup");
                        defaultDictGroup = childPref;
                        JSONArray childDictJsonArray = dictGroupJson.getJSONArray("ChildDicts");
                        for (int j = 0; j < childDictJsonArray.length(); j++) {
                            JSONObject childDictJson = childDictJsonArray.getJSONObject(j);
                            DictPref dictPref = defaultDictGroup.getDictByName(childDictJson.getString("DictName"));
                            if (dictPref!=null) {
                                dictPref.setDisabled(childDictJson.getBoolean("Disabled"));
                                defaultDictGroup.updateChildPref(dictPref);
                            }
                        }
                        rootDictRef.updateChildPref(defaultDictGroup);
                        MdxEngine.getLibMgr().updateDictPref(rootDictRef);
                        break;
                    }
                }
                MdxEngine.refreshDictList();
                //Add other dict groups
                JSONArray dictGroupJsonArray = rootDictJson.getJSONArray("ChildGroups");
                for (int i = 0; i < dictGroupJsonArray.length(); i++) {
                    JSONObject dictGroupJson = dictGroupJsonArray.getJSONObject(i);
                    DictPref dictGroup = MdxEngine.getLibMgr().createDictPref();
                    dictGroup.setDictGroup(true);
                    dictGroup.setUnionGroup(true);
                    //dictGroup.setDictId(dictGroupJson.getInt("DictId"));
                    dictGroup.setDisabled(dictGroupJson.getBoolean("Disabled"));
                    dictGroup.setDictName(dictGroupJson.getString("DictName"));
                    JSONArray childDictJsonArray = dictGroupJson.getJSONArray("ChildDicts");
                    for (int j = 0; j < childDictJsonArray.length(); j++) {
                        JSONObject childDictJson = childDictJsonArray.getJSONObject(j);
                        DictPref childDictPref = defaultDictGroup.getDictByName(childDictJson.getString("DictName"));
                        if(childDictPref!=null) {
                            //childDictPref.setDictId(defaultDictGroup.getDictIdByName(childDictJson.getString("DictName")));
                            childDictPref.setDisabled(childDictJson.getBoolean("Disabled"));
                            //childDictPref.setDictName(childDictJson.getString("DictName"));
                            //Child Dict
                            dictGroup.addChildPref(childDictPref);
                            dictGroup.updateChildPref(childDictPref);
                        }
                    }
                    rootDictRef.addChildPref(dictGroup);
                    rootDictRef.updateChildPref(dictGroup);
                    MdxEngine.getLibMgr().updateDictPref(rootDictRef);
                }


                //MdxEngine.getLibMgr().updateDictPrefRoot(rootDictPref)
                MdxEngine.saveEngineSettings();
                MdxEngine.refreshDictList();
                success = true;
                MiscUtils.showMessageDialog(context, context.getText(R.string.msg_success).toString(), context.getText(R.string.title_information).toString());
            }
        } catch (Exception e) {
            MiscUtils.showMessageDialog(context, MiscUtils.getErrorMessage(e), context.getText(R.string.warning).toString());
            success = false;
            e.printStackTrace();
        }
        return success;

    }
}
