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

package cn.mdict.utils;

import android.app.ProgressDialog;
import android.content.res.AssetManager;
import android.os.Environment;

import cn.mdict.mdx.MdxEngine;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * User: rayman
 * Date: 13-2-20
 * Time: 上午11:20
 */
public class IOUtil {
    public static boolean createDir(String dir) {
        File dirFile = new File(dir);
        return dirFile.mkdir();
    }

    public static void forceClose(InputStream is){
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void forceClose(OutputStream os){
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean streamDuplicate(InputStream is, OutputStream os, int bufferSize){
        is= new BufferedInputStream(is);
        os= new BufferedOutputStream(os);
        byte[] buffer = new byte[bufferSize];
        int length;
        boolean result=false;
        try{
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            forceClose(is);
            forceClose(os);
        }
        return result;
    }

    public static boolean streamDuplicate(InputStream is, OutputStream os){
        return streamDuplicate(is, os, 8*1024);
    }

    public static boolean streamToFile(InputStream is, String fileName, boolean overwrite){
        // Check if the dest exists before copying
        File file = new File(fileName);
        if (file.exists() && !overwrite)
            return true;
        try {
            OutputStream os = new FileOutputStream(fileName);
            return streamDuplicate(is,os);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean copyAssetToFile(AssetManager assets, String filename, boolean overwrite, String targetDir, String newFileName) {

        boolean result = false;
        try {
            final String destfilename = targetDir + "/" + ((newFileName != null && newFileName.length() > 0) ? newFileName : filename);
            // Open the source file in your assets directory
            InputStream is = new BufferedInputStream(assets.open(filename));
            result=streamToFile(is, destfilename, overwrite);
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
            if ( streamDuplicate(is, buffer) ){
                return buffer.toByteArray();
            }
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
            int read_size;
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

    public static boolean loadStringFromAsset(AssetManager assets, String filename, StringBuffer str, boolean overwriteByFile) {
        try {
            if (overwriteByFile) {
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
        try {
            InputStream is= new ByteArrayInputStream(str.getBytes(charset));
            return streamToFile(is, filename, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean saveBytesToFile(String fileName, byte[] data) {
        return streamToFile(new ByteArrayInputStream(data), fileName, true);
    }
}
