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
import android.content.res.Resources;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.speech.tts.TextToSpeech;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import cn.mdict.mdx.*;
import cn.mdict.utils.IOUtil;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


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

    public static void playMedia(byte[] mediaData) {
        try {
            mediaPlayer = new MediaPlayer();
            String tmpFile = MdxEngine.getTempDir() + "audio.wav";
            IOUtil.saveBytesToFile(tmpFile, mediaData);
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

    public static void replaceViewInLayoutById(ViewGroup container, int id, View view){
        View dummy=container.findViewById(id);
        if (dummy==null || container==null)
            return;
        ViewGroup viewParent=(ViewGroup)dummy.getParent();
        int v_index=viewParent.indexOfChild(dummy);
        if (v_index>=0){
            if (view!=null ){
                view.setId(dummy.getId());
                view.setLayoutParams(dummy.getLayoutParams());
                viewParent.removeViewAt(v_index);
                viewParent.addView(view,v_index);
            }else{
                viewParent.removeViewAt(v_index);
            }
            container.requestLayout();
        }
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
