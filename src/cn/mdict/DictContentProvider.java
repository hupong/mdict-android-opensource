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

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.os.MemoryFile;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.util.Log;
import cn.mdict.mdx.DictEntry;
import cn.mdict.mdx.MdxDictBase;
import cn.mdict.mdx.MdxEngine;
import cn.mdict.utils.IOUtil;


public class DictContentProvider extends ContentProvider {
    private static final String ContentHost = "mdict.cn";
    private static final String SEARCH_PATH = "/" + SearchManager.SUGGEST_URI_PATH_QUERY;
    private static final String TAG="MDict.DictContentProvider";

    //private static final String URI_PREFIX = "file://";
    private static MdxDictBase fCurrentDict;
    private static String tmpDir;
    private static AssetManager assets;

    public static void setDict(MdxDictBase dict) {
        fCurrentDict = dict;
    }

    public static void setTmpDir(String dir) {
        tmpDir = dir;
    }

    public static void setAssetManager(AssetManager assetManager) {
        assets = assetManager;
    }

    ParcelFileDescriptor openLocalFile(String filePath) throws FileNotFoundException {
        ParcelFileDescriptor parcel = null;
        try {
            File resFile = new File(MdxEngine.getDataHomeDir()+"/" + filePath);
            parcel = ParcelFileDescriptor.open(resFile, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return parcel;
    }

    public static AssetFileDescriptor makeAssetFileDescriptorFromByteArray(String fileName, byte[] data) {
        MemoryFile memFile;
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                memFile = new MemoryFile(fileName, data.length);
                memFile.writeBytes(data, 0, 0, data.length);
                MemoryFileUtil.deactivate(memFile);
                AssetFileDescriptor afd = MemoryFileUtil.fromMemoryFile(memFile);
                //memFile.close();
                return afd;
            } else {
                ParcelFileDescriptor pfd = MemoryFileUtil.fromData(data, null);
                //memFile.close();
                return new AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    AssetFileDescriptor openMDDMemoryFile(Uri uri) throws FileNotFoundException {
        String tmpFileName = "/mdict" + uri.getPath();
        StringBuffer mimeType = new StringBuffer();
        byte[] data = getDataByUrl(fCurrentDict, uri, mimeType);
        android.os.MemoryFile memFile = null;
        if (data != null && data.length > 0) {
            return makeAssetFileDescriptorFromByteArray(tmpFileName, data);
        }
        return null;
    }

    ParcelFileDescriptor openMDDFile(String filePath) throws FileNotFoundException {
        String tmpFileName = tmpDir + filePath;
        String tmpDirName;
        int namePos = tmpFileName.lastIndexOf('/');
        tmpDirName = tmpFileName.substring(0, namePos);
        tmpFileName = tmpFileName.substring(namePos + 1);
        File dir = new File(tmpDirName);
        File path = new File(dir, tmpFileName);
        if (!path.exists()) {
            byte[] data = fCurrentDict.getDictData(filePath, false);
            if (data.length > 0) {
                OutputStream os = null;
                try {
                    dir.mkdirs();
                    path.createNewFile();
                    path.deleteOnExit();
                    os = new FileOutputStream(path);
                    os.write(data, 0, data.length);
                    os.close();
                } catch (Exception e) {
                    try {
                        if (os != null)
                            os.close();
                        path.delete();
                    } catch (Exception e1) {
                    } finally {
                        return null;
                    }
                }
            }
        } else if (path.isDirectory())
            return null;

        ParcelFileDescriptor parcel = ParcelFileDescriptor.open(path, ParcelFileDescriptor.MODE_READ_ONLY);
        return parcel;
    }

    static HashMap<String, byte[]> cache = new HashMap<String, byte[]>();

    private static final Pattern LocalFilePattern = Pattern.compile("/localfile/(.*)"); //%1=name
    private static final Pattern AssetFilePattern = Pattern.compile("/res/(.*)"); //%1=name

    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        Log.d(TAG, "openAssetFile:"+uri.toString());
        if (uri.getScheme().compareToIgnoreCase("content") == 0 && uri.getHost().compareToIgnoreCase(ContentHost) == 0) {
            String path = uri.getPath();
            Matcher matcher = LocalFilePattern.matcher(path);
            if (matcher.matches() && matcher.groupCount() == 1) {
//                return new AssetFileDescriptor(openLocalFile(MdxEngine.getDataHomeDir()+"/"+matcher.group(1)), 0, AssetFileDescriptor.UNKNOWN_LENGTH);
                return new AssetFileDescriptor(openLocalFile(matcher.group(1)), 0, AssetFileDescriptor.UNKNOWN_LENGTH);
            }
            matcher = AssetFilePattern.matcher(path);
            if (matcher.matches() && matcher.groupCount() == 1) {
                try {
                    String fileName = matcher.group(1);
                    byte[] data;
                    if (cache.containsKey(fileName))
                        data = cache.get(fileName);
                    else {
                        data = IOUtil.loadBinFromAsset(assets, fileName, true);
                        if (data != null)
                            cache.put(fileName, data);
                    }
                    return makeAssetFileDescriptorFromByteArray(fileName, data);
                    //IOUtil.copyAssetToFile(assets, fileName, false, MdxEngine.getDataHomeDir()+"/data", fileName);
                    //return new AssetFileDescriptor(ParcelFileDescriptor.open(new File(MdxEngine.getDataHomeDir()+"/data/"+fileName), ParcelFileDescriptor.MODE_READ_ONLY),0,AssetFileDescriptor.UNKNOWN_LENGTH);
                    //InputStream is=assets.open(fileName);
                    //AssetFileDescriptor afd= assets.openFd(fileName);
                    //return afd;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
//                if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB)
//                    return null;
//                else
                return openMDDMemoryFile(uri);
            }
        }
        return null;
    }

/*
    @Override
	public ParcelFileDescriptor openFile(Uri uri, String mode)
			throws FileNotFoundException {
		String dataName = uri.getPath();
        if ( dataName==null || dataName.length()==0 )
            return null;

        if (!dataName.startsWith("\"") && !dataName.startsWith("/")) {
            dataName = "/" + dataName;
        }
        dataName.replace('\\', '/');

        if ( uri.toString().startsWith(LOCALFILE_URI_PREFIX) ){
            return openLocalFile(dataName);
        }else{
            return openMDDMemoryFile(dataName);
        }
	}
*/

    @Override
    public int delete(Uri arg0, String arg1, String[] arg2) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        if (uri.getPath().startsWith(SEARCH_PATH)) {
            return SearchManager.SUGGEST_MIME_TYPE;
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public boolean onCreate() {
        MDictApp.getInstance().setupAppEnv(getContext());
        Log.d(TAG, "Content Provider Created");
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.d(TAG, "Got query:" + uri.toString());
        if (uri.getPath().startsWith(SEARCH_PATH)) {
            if (fCurrentDict != null && fCurrentDict.isValid()) {
                List<String> pathSegments = uri.getPathSegments();
                if (pathSegments.size() > 0 && pathSegments.get(0).compareToIgnoreCase(SearchManager.SUGGEST_URI_PATH_QUERY) == 0) {
                    String query = null;
                    if (pathSegments.size() > 1)
                        query = pathSegments.get(1);
                    DictEntry entry = new DictEntry(0, "", fCurrentDict.getDictPref().getDictId());
                    if (query != null && query.length() > 0)
                        fCurrentDict.locateFirst(query, true, false, true, entry);
                    if (entry.isValid()) {
                        String limit = uri.getQueryParameter(SearchManager.SUGGEST_PARAMETER_LIMIT);
                        int maxResultCount = 20;
                        if (limit != null && limit.length() > 0) {
                            try {
                                maxResultCount = Integer.parseInt(limit);
                            } catch (NumberFormatException e) {
                            }
                        }
                        ArrayList<DictEntry> entryList = new ArrayList<DictEntry>();
                        fCurrentDict.getEntries(entry, maxResultCount, entryList);
                        String[] columns = new String[]{
                                BaseColumns._ID,
                                SearchManager.SUGGEST_COLUMN_TEXT_1,
                                SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
                                SearchManager.SUGGEST_COLUMN_SHORTCUT_ID};
                        MatrixCursor cursor = new MatrixCursor(columns, maxResultCount);
                        Object[] row;
                        for (int i = 0; i < entryList.size(); ++i) {
                            DictEntry cur_entry = entryList.get(i);
                            String intentDataId = String.format("%d_%d_%s", cur_entry.getDictId(), cur_entry.getEntryNo(), cur_entry.getHeadword());
                            row = new Object[]{cur_entry.toString(), cur_entry.getHeadword(), intentDataId, SearchManager.SUGGEST_NEVER_MAKE_SHORTCUT};
                            cursor.addRow(row);
                        }
                        return cursor;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        return 0;
    }

    private static final Pattern MddDataUrlPattern = Pattern.compile("/mdd/(\\d+)/(.*)"); //%1=dict_id, %2=name
    private static final Pattern IFrameEntryUrlPattern = Pattern.compile("/mdx/_(\\d+)/(-?\\d+)/"); //Used by iframe view mode for sub-entryies, %1=dict_id, %2=entry_no
    private static final Pattern ProgEntryUrlPattern = Pattern.compile("/mdx/(\\d+)/(-?\\d+)/(.*)/"); //Used by single view mode, %1=dict_id, %2=entry_no, %3=headword
    private static final Pattern SearchViewUrlPattern = Pattern.compile("/searchView/(\\d+)_(-?\\d+)_(.*)"); //Used by search suggestion View action


    public static byte[] getDataByUrl(MdxDictBase dict, String urlString, StringBuffer mimeType) {
        return getDataByUrl(dict, Uri.parse(urlString), mimeType);
    }

    public static byte[] getDataByUrl(MdxDictBase dict, Uri uri, StringBuffer mimeType) {
        byte[] data = null;
        if (dict == null || !dict.isValid())
            return null;
        try {
            if (uri.getScheme().compareToIgnoreCase("content") == 0 && uri.getHost().compareToIgnoreCase(ContentHost) == 0) {
                mimeType.setLength(0);
                String url = uri.getPath();
                Matcher matcher = MddDataUrlPattern.matcher(url);
                if (matcher.matches() && matcher.groupCount() == 2) {
                    int dictId = Integer.parseInt(matcher.group(1));
                    String dataName = matcher.group(2);
                    mimeType.append("application/octet-stream");
                    return dict.getDictData(dictId, dataName, false);
                }
                matcher = IFrameEntryUrlPattern.matcher(url);
                if (matcher.matches() && matcher.groupCount() == 2) {
                    Integer dictId = Integer.parseInt(matcher.group(1));
                    Integer entryNo = Integer.parseInt(matcher.group(2));
                    DictEntry entry = new DictEntry(entryNo, "", dictId);
                    mimeType.append("text/html");
                    String htmlBegin = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\r\n" +
                            "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" >" +
                            "<script>function OnTouchStart(event){ top.OnTouchStartImpl(event, document, window);}" +
                            "function OnTouchEnd(event){ top.OnTouchEndImpl(event, document, window);}" +
                            "function SetupTouch(){document.body.addEventListener('touchstart', OnTouchStart, false);\n" +
                            "document.body.addEventListener('touchend', OnTouchEnd,false);}\n</script>" +
                            "<style type=\"text/css\">\n@font-face {font-family: 'Droid Sans IPA'; font-style:  normal; font-weight: normal;\n" +
                            "src: url('content://mdict.cn/assert/droid_sans.ttf');}\n*{font-family:'Droid Sans IPA', DroidSans, arial, sans-serif;}</style>" +
                            "</head><body onload=\"javascript:SetupTouch();\">";

                    data = dict.getDictTextN(entry, false, false, htmlBegin, "</body></html>");
                    return data;
                }
                matcher = ProgEntryUrlPattern.matcher(url);
                boolean isProgEntryUrl = (matcher.matches() && matcher.groupCount() >= 2);
                boolean isSearchEntryUrl = false;
                if (!isProgEntryUrl) {
                    matcher = SearchViewUrlPattern.matcher(url);
                    isSearchEntryUrl = (matcher.matches() && matcher.groupCount() >= 2);
                }

                if (isSearchEntryUrl || isProgEntryUrl) {
                    int dictId = Integer.parseInt(matcher.group(1));
                    int entryNo = Integer.parseInt(matcher.group(2));
                    DictEntry entry = new DictEntry(entryNo, "", dictId);
                    mimeType.append("text/html");
                    String headWord = "";
                    if (matcher.groupCount() == 3) {
                        headWord = matcher.group(3);
                        entry.setHeadword(headWord);
                    }
                    if (entry.isSysCmd()) {
                        data = dict.getDictTextN(entry, true, false, null, null);
                    } else if (entry.isUnionDictEntry()) {
                        if (dict.locateFirst(headWord, isSearchEntryUrl, false, false, entry) == MdxDictBase.kMdxSuccess) {
                            data = dict.getDictTextN(entry, true, true, null, null);
                        }
                    } else {
                        data = dict.getDictTextN(entry, true, false, null, null);
                    }
                    return data;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }
}



/*
    AssetFileDescriptor getFileFromSocket(String dataName){
        Socket socket=null;
        BufferedWriter writer=null;
        try{
            socket=new Socket("127.0.0.1", localPort);
            writer=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            writer.write(dataName);
            writer.newLine();
            writer.flush();
            return new AssetFileDescriptor(ParcelFileDescriptor.fromSocket(socket),0, AssetFileDescriptor.UNKNOWN_LENGTH);
        }
        catch(Exception e){
            if (writer!=null){
                try{
                    writer.close();
                }
                catch(Exception e1){
                }
            }
            if (socket.isConnected() ){
                try{
                    socket.close();
                }
                catch (Exception e1){
                }
            }
        }
        finally {

        }
        return null;
    }

    static {
        try{
            serverSocket=new ServerSocket(localPort);
            serverThread= new Thread( new Runnable() {
                @Override
                public void run() {
                    while(!serverThread.isInterrupted()){
                        try {
                            Socket socket=serverSocket.accept();
                            BufferedReader reader=new BufferedReader( new InputStreamReader(socket.getInputStream()) );
                            BufferedOutputStream os=new BufferedOutputStream( socket.getOutputStream());
                            String cmd=reader.readLine();
                            if ( cmd.length()!=0 ){
                                byte[] data=fCurrentDict.getDictData(cmd, false);
                                if ( data != null ){
                                    os.write(data);
                                }
                                os.flush();
                                os.close();
                                socket.close();
                            }
                        }
                        catch (Exception e){
                        }
                    }

                }
            });
            serverThread.start();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private static ServerSocket serverSocket;
    private static Thread serverThread;
    private static int localPort=8999;
    public static void StopProvider(){
        if ( serverThread!=null ){
            serverThread.interrupt();
            try {
                serverSocket.close();
            }
            catch (Exception e){

            }
        }
    }
 */
