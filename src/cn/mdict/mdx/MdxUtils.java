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

import android.webkit.WebView;

import java.io.ByteArrayOutputStream;

import cn.mdict.utils.IOUtil;

/**
 * User: rayman
 * Date: 11-12-17
 * Time: 下午10:52
 */
public class MdxUtils {
    /**
     * Method decodeSpeex ...
     *
     * @param input           of type byte[]
     * @param output          of type ByteArrayOutputStream
     * @param appendWAVHeader of type boolean
     * @return boolean
     */
    // Native declarations
    public native static boolean decodeSpeex(byte[] input, ByteArrayOutputStream output, boolean appendWAVHeader);

    /**
     * Method punyCodeToUnicode ...
     *
     * @param input of type String
     * @return String
     */
    public native static String punyCodeToUnicode(String input);

    /**
     * Method displayEntry ...
     *
     * @param webView of type WebView
     * @param dict    of type MdxDictBase
     * @param entry   of type DictEntry
     * @return int
     */
    public native static int displayEntry(WebView webView, MdxDictBase dict, DictEntry entry, boolean headerOnly);


    public static void displayEntryHtml(MdxDictBase dict, DictEntry entry, WebView wv) {
        byte[] data = dict.getDictTextN(entry, true, false, "", "");
        if (data != null) {
            try {
                String html = new String(data, "utf-8");
                wv.loadDataWithBaseURL("", html, "text/html", "utf-8", "");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
