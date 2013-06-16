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
package cn.mdict;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import cn.mdict.mdx.MdxEngine;
import cn.mdict.mdx.MdxEngineSetting;

public class SettingFrame extends SherlockPreferenceActivity implements TextToSpeech.OnInitListener {
    public final static String prefChanged = "PrefChanged";

    private TextToSpeech ttsEngine = null;
    private static final int kCheckTTS = 1;
    private static final int kInstallTTS = 2;

    private String oldTTSLocale = "";
    private boolean oldUseTTS = false;
    private String oldTTSEngineName = "";
    private String oldExtraDictPath = "";
    private boolean oldUseFingerGesture = true;

    static String prefExtraDictDirTitle = null;

    void updateExtraDictDir() {
        Preference pref = findPreference(MdxEngineSetting.prefExtraDictDir);
        if (pref != null) {
            if (prefExtraDictDirTitle == null)
                prefExtraDictDirTitle = pref.getTitle().toString();
            pref.setTitle(prefExtraDictDirTitle + "  " + MdxEngine.getSettings().getExtraDictDir());
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MdxEngine.setupEnv(getApplicationContext());

        getPreferenceManager().setSharedPreferencesName(MdxEngineSetting.preferenceName);
        addPreferencesFromResource(R.xml.settings);
        oldTTSEngineName = MdxEngine.getSettings().getPrefPreferedTTSEngine();
        oldUseTTS = MdxEngine.getSettings().getPrefUseTTS();
        oldTTSLocale = MdxEngine.getSettings().getPrefTTSLocale();
        oldExtraDictPath = MdxEngine.getSettings().getExtraDictDir();
        oldUseFingerGesture = MdxEngine.getSettings().getPrefUseFingerGesture();

        initTTS();

        /* fill the language list */
        ListPreference ttsSuportedLocale = (ListPreference) findPreference(MdxEngineSetting.prefTTSLocale);
        String[] tts_locales = getResources().getStringArray(R.array.tts_supported_language);
        ttsSuportedLocale.setEntryValues(tts_locales);
        String[] tts_locales_name = getResources().getStringArray(R.array.tts_supported_language_name);
        ttsSuportedLocale.setEntries(tts_locales_name);
        PreferenceGroup prefGrp = (PreferenceGroup) findPreference(getResources().getString(R.string.pref_category_sound));

        updateExtraDictDir();
        if (prefGrp != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                Preference prefTtsEngine = prefGrp.findPreference(getResources().getString(R.string.pref_preferred_tts_engine));
                if (prefTtsEngine != null)
                    prefGrp.removePreference(prefTtsEngine);
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                PreferenceGroup basicGrp = (PreferenceGroup) findPreference(getResources().getString(R.string.pref_category_basic));
                Preference prefMonitorClipboard = basicGrp.findPreference(getResources().getString(R.string.pref_global_clipboard_monitor));
                if (prefMonitorClipboard != null)
                    basicGrp.removePreference(prefMonitorClipboard);
            }

            if (ttsSuportedLocale.getEntries() == null || ttsSuportedLocale.getEntries().length == 0) {
                prefGrp.removePreference(ttsSuportedLocale);
            }
        }

/*
        for(int i=0; i<tts_locales.length; ++i){
            if (tts_locales[i].compareToIgnoreCase("en_US")==0){
                tts_locales_name[i]=getResources().getString(R.string.locale_en_us);
            }else if (tts_locales[i].compareToIgnoreCase("en_GB")==0){
                tts_locales_name[i]=getResources().getString(R.string.locale_en_gb);
            }else{
                Locale locale=new Locale(tts_locales[i]);
                tts_locales_name[i]=locale.getDisplayLanguage();
            }
        }
*/
    }

    @Override
    public void onResume() {
        super.onResume();
        MiscUtils.setOrientationSensorBySetting(this);
    }

    @Override
    protected void onDestroy() {
        if (ttsEngine != null) {
            ttsEngine.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        ArrayList<String> changedPrefs = new ArrayList<String>();
        if (oldUseTTS != MdxEngine.getSettings().getPrefUseTTS())
            changedPrefs.add(MdxEngineSetting.prefUseTTS);
        if (oldTTSEngineName.compareToIgnoreCase(MdxEngine.getSettings().getPrefPreferedTTSEngine()) != 0) {
            changedPrefs.add(MdxEngineSetting.prefPreferredTTSEngine);
        }
        if (oldTTSLocale.compareToIgnoreCase(MdxEngine.getSettings().getPrefTTSLocale()) != 0) {
            changedPrefs.add(MdxEngineSetting.prefTTSLocale);
        }
        if (oldExtraDictPath.compareToIgnoreCase(MdxEngine.getSettings().getExtraDictDir()) != 0) {
            changedPrefs.add(MdxEngineSetting.prefExtraDictDir);
        }

        if (oldUseFingerGesture != MdxEngine.getSettings().getPrefUseFingerGesture())
            changedPrefs.add(MdxEngineSetting.prefUseFingerGesture);

        if (changedPrefs.size() != 0) {
            Intent intent = getIntent();
            intent.putStringArrayListExtra(prefChanged, changedPrefs);
            setResult(RESULT_OK, intent);
        }
        finish();
    }

    private void initTTS() {
        try {
            Intent checkIntent = new Intent();
            checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
            startActivityForResult(checkIntent, kCheckTTS);
        } catch (Exception e) {
            onInit(TextToSpeech.ERROR);
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (requestCode == kCheckTTS) {
                if (resultCode != TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL) {
                    // success, create the TTS instance
                    ttsEngine = new TextToSpeech(this, this);
                }else{
                    onInit(TextToSpeech.ERROR);
                }
            }
            else if (requestCode == kInstallTTS) {
                //如果是用户选择了安装TTS，安装完成后需要重新初始化一次
                initTTS();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().compareToIgnoreCase(getString(R.string.pref_use_tts)) == 0) {
            final CheckBoxPreference useTTS = (CheckBoxPreference) preference;
            if (useTTS.isChecked() && ttsEngine == null) {
                AlertDialog dialog = MiscUtils.buildConfirmDialog(this,
                        R.string.confirm_install_tts, R.string.install_tts,
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface dialogInterface, int i) {
                                try {
                                    Intent installIntent = new Intent();
                                    installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                                    startActivityForResult(installIntent, kInstallTTS);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(SettingFrame.this, R.string.fail_to_install_tts_engine, 3000).show();
                                }
                            }
                        }, new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface dialogInterface, int i) {
                                useTTS.setChecked(false);
                            }
                        });
                dialog.show();
            }
        } else if (preference.getKey().compareToIgnoreCase(getString(R.string.pref_extra_dict_dir)) == 0) {
            String dir = MdxEngine.getSettings().getExtraDictDir();
            if (dir == null || dir.length() == 0)
                selectFolder(this, MdxEngine.getDocDir());
            else
                selectFolder(this, dir);

        }
        return false;
    }

    @Override
    public void onInit(int i) {
        try {
            PreferenceGroup prefGrp = (PreferenceGroup) findPreference(getResources().getString(R.string.pref_category_sound));
            if (prefGrp != null) {
                ListPreference ttsEngineName = (ListPreference) prefGrp.findPreference(MdxEngineSetting.prefPreferredTTSEngine);
                if (i == TextToSpeech.ERROR) {
                    if (ttsEngine != null) {
                        ttsEngine.shutdown();
                        ttsEngine = null;
                    }
                    CheckBoxPreference useTTS = (CheckBoxPreference) findPreference(MdxEngineSetting.prefUseTTS);
                    if (useTTS != null)
                        useTTS.setChecked(false); //No TTS engine installed, so we turn it off.
                    if (ttsEngineName != null) {
                        prefGrp.removePreference(ttsEngineName);
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && ttsEngine != null && ttsEngineName != null) {
                        //noinspection AndroidLintNewApi
                        List<TextToSpeech.EngineInfo> engines = ttsEngine.getEngines();
                        if (!engines.isEmpty()) {
                            String[] enginePackageName = new String[engines.size()];
                            String[] engineLabel = new String[engines.size()];
                            int n = 0;
                            for (TextToSpeech.EngineInfo ei : engines) {
                                enginePackageName[n] = ei.name;
                                engineLabel[n++] = ei.label;
                            }
                            ttsEngineName.setEntries(enginePackageName);
                            ttsEngineName.setEntryValues(engineLabel);
                        }
                    }
                    if (ttsEngineName == null || ttsEngineName.getEntries() == null || ttsEngineName.getEntries().length == 0) {
                        prefGrp.removePreference(ttsEngineName);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String[] loadDirList(String rootPath) {
        File path = new File(rootPath);
        String[] dirList = null;
        if (path.exists()) {
            FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    return sel.isDirectory();
                }
            };
            String[] list = path.list(filter);
            if (rootPath.compareTo("/") != 0) {
                if (list != null && list.length > 0) {
                    dirList = new String[list.length + 1];
                    System.arraycopy(list, 0, dirList, 1, list.length);
                }
            } else
                dirList = list;
            if (dirList == null)
                dirList = new String[1];
            dirList[0] = "..";
        }
        java.util.Arrays.sort(dirList, java.text.Collator.getInstance());
        return dirList;
    }

    AlertDialog.Builder dialogBuilder = null;

    protected void selectFolder(final Context context, final String currentDir) {
        final String[] dirList = loadDirList(currentDir);

        DialogInterface.OnClickListener itemListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String targetFolder;
                if (dirList[which].compareTo("..") == 0) {
                    File folder = new File(currentDir);
                    targetFolder = folder.getParent();
                } else if (currentDir.compareTo("/") == 0) {
                    targetFolder = currentDir + dirList[which];
                } else
                    targetFolder = currentDir + "/" + dirList[which];
                if (targetFolder != null && targetFolder.length() > 0)
                    selectFolder(context, targetFolder);
            }
        };

        dialogBuilder = new AlertDialog.Builder(context)
                .setCancelable(true)
                .setTitle(context.getString(R.string.current_folder) + " " + currentDir)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Preference pref = findPreference(MdxEngineSetting.prefExtraDictDir);
                        if (pref != null) {
                            String extraLibPath = currentDir;
                            if (currentDir.compareTo(MdxEngine.getDocDir()) == 0) {
                                extraLibPath = "";
                            }
                            pref.getEditor().putString(MdxEngineSetting.prefExtraDictDir, extraLibPath).commit();
                            updateExtraDictDir();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .setItems(dirList, itemListener);
        dialogBuilder.show();
    }
}


