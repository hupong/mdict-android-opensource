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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import cn.mdict.mdx.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class AddonFuncUnt implements MediaPlayer.OnBufferingUpdateListener {
    static MediaPlayer mediaPlayer = null;
    static boolean  appInited=false;

    public static Boolean checkNetworkStatus(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null
                && networkInfo.getState() == NetworkInfo.State.CONNECTED;
    }

    public static boolean copyAssetToFile(AssetManager assets, String filename, boolean overWrite, String targetDir, String newFileName) {

        boolean result = false;
        try {
            final String destfilename = targetDir + "/" + ((newFileName != null && newFileName.length() > 0) ? newFileName : filename);

            // Check if the dest exists before copying
            File file = new File(destfilename);
            if (file.exists() && !overWrite)
                return true;

            // Open the source file in your assets directory
            InputStream is = assets.open(filename);
            // Copy the assets into the destination
            OutputStream os = new FileOutputStream(destfilename);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();
            os.close();
            is.close();
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static byte[] loadBinFromAsset(AssetManager am, String fileName, boolean checkDocExist) {
        try {
            InputStream is = null;
            if (checkDocExist) {
                File resFile = new File(MdxEngine.getDocDir() + fileName);
                if (resFile.exists() && resFile.isFile()) {
                    is = new FileInputStream(resFile);
                }
            }
            if (is == null)
                is = am.open(fileName);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[16384];

            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            is.close();
            return buffer.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //AssetFileDescriptor afd=am.openFd(fileName);
        return null;
    }

    public static boolean loadStringFromStream(InputStream inputStream, String encoding, StringBuffer str) {
        boolean result = true;
        int buffer_size = 512;
        char[] char_buf = new char[buffer_size];
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(inputStream, encoding));
            int read_size = 0;
            while ((read_size = in.read(char_buf)) != -1) {
                str.append(char_buf, 0, read_size);
            }
        } catch (UnsupportedEncodingException e) {
            result = false;
            e.printStackTrace();
        } catch (IOException e) {
            result = false;
            e.printStackTrace();
        } finally {
            if (in != null)
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return result;
    }


    public static boolean loadStringFromFile(String filename, StringBuffer str) {
        try {
            return loadStringFromStream(new FileInputStream(filename), "UTF8", str);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean loadStringFromAsset(AssetManager assets, String filename, StringBuffer str, boolean checkDocExist) {
        try {
            if (checkDocExist) {
                if (loadStringFromFile(MdxEngine.getDocDir() + filename, str))
                    return true;
            }
            return loadStringFromStream(assets.open(filename), "UTF8", str);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean saveStringToFile(String filename, String str, String charset) {
        OutputStream os = null;
        boolean res = true;
        try {
            os = new FileOutputStream(filename);
            os.write(str.getBytes(charset));
        } catch (Exception e) {
            e.printStackTrace();
            res = false;
        } finally {
            try {
                if (os != null)
                    os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    public static int getVersionCode(Context context) {
        int versionNumber = -1;
        try {
            versionNumber = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionNumber;
    }

    public static String getVersionName(Context context) {
        String versionName = "";
        try {
            // ---get the package info---
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            versionName = pi.versionName;
            if (versionName == null || versionName.length() <= 0) {
                return "";
            }
        } catch (Exception e) {
            Log.e("VersionInfo", "Exception", e);
        }
        return versionName;
    }

    static boolean checkInstalledVersionNumber(Context context, String versionFileName) {
        boolean sameVersion = false;
        BufferedReader reader = null;
        try {
            int versionNumber = getVersionCode(context);
            File versionFile = new File(versionFileName);
            if (versionFile.exists()) {
                reader = new BufferedReader(new FileReader(versionFile));
                String version = reader.readLine();
                sameVersion = Integer.parseInt(version) == versionNumber;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        return sameVersion;
    }

    private static boolean createDir(String dir) {
        File dirFile = new File(dir);
        return dirFile.mkdir();
    }

    public static Boolean initApp(Context context, AssetManager assets, MdxDictBase dict) {
        if ( appInited )
            return true;
        //dict = new MdxDictBase();
        MdxEngine.initSettings(context.getApplicationContext());
        DictContentProvider.setAssetManager(assets);

        String mdictHome = Environment.getExternalStorageDirectory().getAbsolutePath() + "/mdict";
        //String mdictHome=context.getExternalFilesDir(null).getAbsolutePath();
        String resDir = mdictHome + "/data"; //getFilesDir().getAbsolutePath();
        String docDir = mdictHome + "/doc";
        String tmpDir = mdictHome + "/tmp";
        String mediaDir = MdxEngine.getSettings().getExtraDictDir();
        String fontsDir = mdictHome + "/fonts";
        String audioLibDir = mdictHome + "/audiolib";


        createDir(mdictHome);
        createDir(resDir);
        createDir(docDir);
        createDir(tmpDir);
        createDir(fontsDir);
        createDir(audioLibDir);

        String versionFileName = resDir + "/version";

        //Optimize copy action by write a version file after successful copy.
        //Only overwritten asset files if different version found
        if (!checkInstalledVersionNumber(context, versionFileName)) {
            AddonFuncUnt.copyAssetToFile(assets, "ResDB.dat", true, resDir, null);
            AddonFuncUnt.copyAssetToFile(assets, "html_begin.html", true, resDir, null);
            AddonFuncUnt.copyAssetToFile(assets, "html_end.html", true, resDir, null);
            AddonFuncUnt.copyAssetToFile(assets, "block_begin_h.html", true, resDir, null);
            AddonFuncUnt.copyAssetToFile(assets, "block_end_h.html", true, resDir, null);
            AddonFuncUnt.copyAssetToFile(assets, "union_grp_title.html", true, resDir, null);
            AddonFuncUnt.copyAssetToFile(assets, "code.js", true, resDir, null);
            AddonFuncUnt.copyAssetToFile(assets, "droid_sans.ttf", false, resDir, null);
            AddonFuncUnt.copyAssetToFile(assets, "mdict.css", false, resDir, null);
            saveStringToFile(versionFileName, (new Integer(getVersionCode(context))).toString(), "utf-8");
        }

        ArrayList<String> extraSearchPath = new ArrayList<String>();
        //docDir is in the searchPath by default, so don't need to add it.
        mediaDir = mediaDir.trim();
        if (mediaDir != null && mediaDir.length() != 0 && mediaDir.compareTo(docDir) != 0)
            extraSearchPath.add(mediaDir);

        if (MdxEngine.initMDictEngine(context, mdictHome, resDir, tmpDir, extraSearchPath)) {
            appInited=true;

            int result = MdxDictBase.kMdxSuccess;
            MdxEngineSetting prefs = MdxEngine.getSettings();
            if (prefs.getPrefMultiDictLookkupMode()) {
                DictPref dictPref = MdxEngine.getLibMgr().getRootDictPref();
                dictPref.setUnionGroup(true);
                MdxEngine.getLibMgr().updateDictPref(dictPref);
            }

            if (prefs.getPrefLastDictId() == DictPref.kInvalidDictPrefId
                    || (result = MdxEngine.openDictById(prefs.getPrefLastDictId(), prefs.getPrefsUseLRUForDictOrder(), dict)) != MdxDictBase.kMdxSuccess) {
                if (MdxEngine.getLibMgr().getRootDictPref().getChildCount() > 0) {
                    DictPref dictPref = null;
                    if (prefs.getPrefMultiDictLookkupMode()) {
                        dictPref = MdxEngine.getLibMgr().getRootDictPref();
                    } else
                        dictPref = MdxEngine.getLibMgr().getRootDictPref().getChildDictPrefAtIndex(0);
                    if (dictPref != null) {
                        result = MdxEngine.openDictById(dictPref.getDictId(), prefs.getPrefsUseLRUForDictOrder(), dict);
                    }
                }
            }
            if (!dict.isValid()) {
                Log.d("MDX", "Fail to open dictionary, error code:" + result);
                return false;
            } else {
                MdxEngine.saveEngineSettings();
            }
            DictContentProvider.setTmpDir(MdxEngine.getTempDir());
        }
        return true;
    }


    public static String getAudioFileNameForWord(String word, String extension) {
        StringBuffer audioFileName = new StringBuffer("\\");
        if (word.length() == 0) return audioFileName.toString();

        String stripedWord = word.trim();
        audioFileName.append(stripedWord.replace(".", ""));
        audioFileName.append(extension);
        return audioFileName.toString();
    }

    public static final String getErrorMessage(Throwable e) {
        if (e.getMessage() == null)
            return e.toString();
        else
            return e.getMessage();
    }

    public static void hideSIP(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view != null) {
            //imm.showSoftInput(ic_view, 0); //????????
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);//?????????
        }
    }

    public static void showSIP(Activity activity, View view) {
        ((InputMethodManager) activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).showSoftInput(view, InputMethodManager.SHOW_FORCED);
    }

    public static void showSIP(Activity activity, int control_id) {
        showSIP(activity, activity.findViewById(control_id));
    }

    public static void toggleSIP(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, 0);
    }

    public static void parseLink(String url, StringBuffer scheme, StringBuffer host, StringBuffer fregment) {
        final String seperator = "://";
        if (url.length() > 0 && url.charAt(url.length() - 1) == '/') {
            url = url.substring(0, url.length() - 1);
        }

        int pos = url.indexOf(seperator);
        if (pos >= 0)
            scheme.append(url.substring(0, pos));
        pos += seperator.length();
        int pos1 = url.indexOf("#", pos);
        if (pos1 > 0) {
            host.append(url.substring(pos, pos1));
            fregment.append(url.substring(pos1 + 1));
        } else {
            host.append(url.substring(pos));
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        if (percent == 100) {
            mp.stop();
            mp.release();
            if (mediaPlayer == mp)
                mediaPlayer = null;
        }
    }

    public class WaveInfo {
        WaveInfo(int format, int channels, int rate, int bits, int size, int bodyOffset) {
            this.format = format;
            this.channels = channels;
            this.rate = rate;
            this.bits = bits;
            this.size = size;
            this.bodyOffset = bodyOffset;
        }

        public int format, channels, rate, bits, size, bodyOffset;
    }

    public static WaveInfo parseWaveHeader(byte[] waveData) {
        final int HEADER_SIZE = 48;
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(waveData, 0, buffer.capacity());

        int dataMark = 0;
        int dataSize = 0;

        buffer.rewind();
        dataMark = buffer.getInt(); //dataMark for "RIFF"
        dataSize = buffer.getInt();
        dataMark = buffer.getInt(); //dataMark for "WAVE"
        dataMark = buffer.getInt(); //dataMark for "fmt "
        dataSize = buffer.getInt();
        int dataOffset = buffer.position();
        int format = buffer.getShort();
        int channels = buffer.getShort();
        int rate = buffer.getInt();
        int avgrate = buffer.getInt();
        int blockAlign = buffer.getShort();
        int bitsPerSample = buffer.getShort();

        int waveDataPos = dataOffset + dataSize;

        do {
            buffer.rewind();
            buffer.put(waveData, waveDataPos, 8);
            buffer.rewind();
            dataMark = buffer.getInt();
            dataSize = buffer.getInt();
            waveDataPos += (dataSize + 8);
        } while (dataMark != 0x61746164);

        WaveInfo result = (new AddonFuncUnt()).new WaveInfo(format, channels, rate, bitsPerSample, dataSize, waveDataPos - dataSize);
        return result;
    }

    static boolean saveBytesToFile(String fileName, byte[] data) {
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileName));
            bos.write(data);
            bos.flush();
            bos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void playMedia(byte[] mediaData) {
        try {
            mediaPlayer = new MediaPlayer();
            String tmpFile = MdxEngine.getTempDir() + "audio.wav";
            saveBytesToFile(tmpFile, mediaData);
            mediaPlayer.setDataSource(tmpFile);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public static void playWave(byte[] waveData) {
        WaveInfo info = AddonFuncUnt.parseWaveHeader(waveData);
//        WaveInfo info1= AddonFuncUnt.parseWaveHeader1(waveData);

        int channelConfig, encoding;
        if (info.channels == 1)
            channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
        else if (info.channels == 2)
            channelConfig = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
        else
            channelConfig = AudioFormat.CHANNEL_CONFIGURATION_INVALID;

        if (info.bits == 8)
            encoding = AudioFormat.ENCODING_PCM_8BIT;
        else if (info.bits == 16)
            encoding = AudioFormat.ENCODING_PCM_16BIT;
        else {
            //encoding=AudioFormat.ENCODING_INVALID;
            playMedia(waveData);
            return;
        }

        int sampleRate = info.rate;
        int bufSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, encoding);
        AudioTrack outTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                channelConfig, encoding, bufSize, AudioTrack.MODE_STREAM);
        outTrack.play();
        int audioSize = waveData.length - info.bodyOffset;
        int dataOffset = info.bodyOffset;
        while (audioSize > 0) {
            int curBufSize = bufSize;
            if (audioSize < bufSize) {
                curBufSize = audioSize;
            }
            outTrack.write(waveData, dataOffset, curBufSize);
            dataOffset += curBufSize;
            audioSize -= curBufSize;
        }
        ;
        outTrack.stop();
    }

    private static ByteArrayOutputStream getWaveDataForPath(MdxDictBase dict, String path) {
        byte[] result = null;
        if (dict != null && dict.isValid())
            result = dict.getDictData(path, true);

        if (result == null) {
            result = MdxEngine.getSharedMdxData(path, true);
        }

        if (result != null && result.length > 0) {
            // TODO: Ogg decode?
            if (result.length > 3) {
                if (result[0] == 'O' && result[1] == 'g' && result[2] == 'g') {
                    // For Ogg stream
                    ByteArrayOutputStream oggresult = new ByteArrayOutputStream();
                    if (MdxUtils.decodeSpeex(result, oggresult, true)) {
                        if (oggresult.size() > 0) {
                            return oggresult;
                        }
                    }
                } else {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    try {
                        bos.write(result);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                    return bos;
                }
            }
        }
        return null;
    }

    public static boolean playAudioForWord(MdxDictBase dict, String headword) {
        if (headword.length() > 0) {
            if (!playAudio(dict, getAudioFileNameForWord(headword, ".spx"))) {
                return playAudio(dict, getAudioFileNameForWord(headword, ".wav"));
            }
            return true;
        } else
            return false;
    }

    public static boolean playAudio(MdxDictBase dict, String path) {
        ByteArrayOutputStream waveData = getWaveDataForPath(dict, path);
        if (waveData != null && waveData.size() > 0) {
            try {
                AddonFuncUnt.playWave(waveData.toByteArray());
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static int convertDipToPx(Resources res, float dip) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, res.getDisplayMetrics());
    }

    static boolean findSpeechForWord(MdxDictBase dict, String headword, String extension) {
        String path = getAudioFileNameForWord(headword, extension);
        boolean hasSpeech = false;
        if (dict != null)
            hasSpeech = dict.hasDataEntry(path, true);
        if (!hasSpeech)
            hasSpeech = MdxEngine.hasSharedMdxData(path, true);
        return hasSpeech;
    }

    public static boolean hasSpeechForWord(MdxDictBase dict, String headword) {
        if (headword.length() == 0)
            return false;
        if (findSpeechForWord(dict, headword, ".spx"))
            return true;
        else
            return findSpeechForWord(dict, headword, ".wav");
    }

    public static void showMessageDialog(Context context, String message, String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(true);
        if (message != null)
            builder.setMessage(message);
        if (title != null)
            builder.setTitle(title);
        builder.setPositiveButton(R.string.ok, null);
        builder.show();
    }

    public static AlertDialog buildConfirmDialog(Context context, int messageId, int titleId,
                                                 android.content.DialogInterface.OnClickListener okListener,
                                                 android.content.DialogInterface.OnClickListener cancelListener) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(context)
                        .setCancelable(true)
                        .setPositiveButton(R.string.yes, okListener)
                        .setNegativeButton(R.string.no, cancelListener);
        if (messageId != 0) {
            builder.setMessage(messageId);
        }
        if (titleId != 0) {
            builder.setTitle(titleId);
        }
        return builder.create();
    }


    public static void PromptUserToInstallTTSEngine(Activity activity) {
        // missing data, install it
        Intent installIntent = new Intent();
        installIntent.setAction(
                TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
        activity.startActivity(installIntent);
    }

    public static String DecodeUrl(String url) {
        String decodedUrl = url;
        try {
            decodedUrl = new String(URLUtil.decode(url.getBytes("UTF-8")), "UTF-8");
        } catch (Exception e) {

        }
        return decodedUrl;
    }


    /*
    public static View getItemViewForActionItem(ActionBarImpl actionBar, MenuItem item){
        View actionView=item.getActionView();
        if (actionView==null){
            if ( item.getItemId()== android.R.id.home ){
                actionView= actionBar.mActionView.mHomeLayout;
            }else{
                actionView= actionBar.mActionView.mActionMenuPresenter.findViewForItem(item);
                //MenuItemImpl itemImpl=(MenuItemImpl)item;
                //actionView=itemImpl.getActionView();
            }
        }
        return actionView;
    }
    */

}
