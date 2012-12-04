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

import cn.mdict.mdx.DictEntry;
import cn.mdict.mdx.DictPref;

import java.util.ArrayList;

/**
 * Class MdxDictBase is the base classing for access mdx files, it could be a single mdx files
 * or multiple mdx files joint to form a union-dictionary
 *
 * @author Rayman Zhang
 *         Created on 2011-12-31
 */
public class MdxDictBase {
    public final static int kZDBSuccess = 0;
    public final static int kZDBOpenFileError = 1;
    public final static int kZDBReadFileError = 2;
    public final static int kZDBKeyNotFound = 3;
    public final static int kZDBInvalidHeader = 4;
    public final static int kZDBInvalidFormat = 5;
    public final static int kZDBInvalidParameter = 6;
    public final static int kZDBParameterOutOfRange = 7;
    public final static int kZDBChecksumError = 8;
    public final static int kZDBDecompressionError = 9;
    public final static int kZDBDatabaseNotInited = 10;
    public final static int kZDBInvalidLicenseKey = 11;
    public final static int kZDBUnsupportedFormat = 12;
    public final static int kZDBMemoryError = 13;
    public final static int kZDBErrorEnd = 14;

    public final static int kMdxSuccess = kZDBSuccess;
    public final static int kMdxOpenFileError = kZDBOpenFileError;
    public final static int kMdxReadFileError = kZDBReadFileError;
    public final static int kMdxKeyNotFound = kZDBKeyNotFound;
    public final static int kMdxInvalidHeader = kZDBInvalidHeader;
    public final static int kMdxInvalidFormat = kZDBInvalidFormat;
    public final static int kMdxInvalidParameter = kZDBInvalidParameter;
    public final static int kMdxParameterOutOfRange = kZDBParameterOutOfRange;
    public final static int kMdxChecksumError = kZDBChecksumError;
    public final static int kMdxDecompressionError = kZDBDecompressionError;
    public final static int kMdxDatabaseNotInited = kZDBDatabaseNotInited;
    public final static int kMdxInvalidLicenseKey = kZDBInvalidLicenseKey;
    public final static int kMdxUnSupportedFormat = kZDBUnsupportedFormat;
    public final static int kMdxMemoryError = kZDBMemoryError;
    public final static int kMdxErrorBegin = kZDBErrorEnd;
    public final static int kMdxInvalidStyleSheet = 15;
    public final static int kMdxUnsupportedFunction = 16;
    public final static int kMdxOpenNonUnionGroup = 17;
    public final static int kMdxInvalidMDT = 18;
    public final static int kMdxUnmatchedMDT = 19;
    public final static int kMdxErrorEnd = 20;

    /**
     * Ptr to JNI cpp object
     */
    private int fInstance;

    /**
     * Dummy Constructor
     */
    public MdxDictBase() {

    }

    /**
     * Finalize function for resource cleanup
     *
     * @throws Throwable when
     */
    protected void finalize() throws Throwable {
        releaseCppObject();
        super.finalize();
    }

    /**
     * Get settings of current dictionary
     *
     * @return the settings (type DictPref) of this MdxDictBase object.
     */
    public synchronized DictPref getDictPref() {
        if (isValid()) {
            int prefPtr = getDictPrefN();
            if (prefPtr != 0)
                return new DictPref(prefPtr);
        }
        return null;
    }

    /**
     * Locates the first entry that match the specified headword
     *
     * @param headword     Headword to be searched for
     * @param convertKey   Convert the given headword into different Chinese form according to dictionary settings?
     * @param partialMatch Match partial of the headword. For example "abc" can be matched with "abd" headword search
     * @param entry        The matched entry if found
     * @return Return kMdxSuccess when succeed, otherwise return error codes.
     */
    public synchronized int locateFirst(String headword, boolean convertKey, boolean partialMatch, boolean startWithMatch, DictEntry entry) {
        if (isValid()) {
            return locateFirstN(headword, convertKey, partialMatch, startWithMatch, entry);
        } else {
            return kMdxDatabaseNotInited;
        }
    }

    /**
     * Fill the headword field for given entryNo
     *
     * @param entry The given entry no and the result
     * @return Return kMdxSuccess when succeed.
     */
    public synchronized int getHeadword(DictEntry entry) {
        if (isValid()) {
            return getHeadwordN(entry);
        } else {
            return kMdxDatabaseNotInited;
        }
    }

    /**
     * Method getDictData ...
     *
     * @param dataName   of type String
     * @param convertKey of type boolean
     * @return byte[]
     */
    public synchronized byte[] getDictData(String dataName, boolean convertKey) {
        if (isValid()) {
            return getDictDataN(dataName, convertKey);
        } else {
            return null;
        }
    }

    public synchronized byte[] getDictData(int dictId, String dataName, boolean convertKey) {
        if (isValid()) {
            return getDictDataWithDictIdN(dictId, dataName, convertKey);
        } else {
            return null;
        }
    }

    /**
     * Method getEntries ...
     *
     * @param startEntry    of type DictEntry
     * @param maxEntryCount of type int
     * @param entries       of type ArrayList<DictEntry>
     * @return int
     */
    public synchronized int getEntries(DictEntry startEntry, int maxEntryCount, ArrayList<DictEntry> entries) {
        if (isValid()) {
            return getEntriesN(startEntry, maxEntryCount, entries);
        } else {
            return kMdxDatabaseNotInited;
        }
    }

    /**
     * Method isValid returns the valid of this MdxDictBase object.
     *
     * @return the valid (type boolean) of this MdxDictBase object.
     */
    public synchronized boolean isValid() {
        if (fInstance == 0)
            return false;
        else
            return isValidN();
    }

    /**
     * Method releaseCppObject ...
     *
     * @return int
     */
    public native int releaseCppObject();

    /**
     * Method getEntryCount returns the entryCount of this MdxDictBase object.
     *
     * @return the entryCount (type int) of this MdxDictBase object.
     */
    public native int getEntryCount();

    /**
     * Method canRandomAccess ...
     *
     * @return boolean
     */

    public native boolean canRandomAccess();

    /**
     * Method hasDataEntry ...
     *
     * @param dataName   of type String
     * @param convertKey of type boolean
     * @return boolean
     */
    public native boolean hasDataEntry(String dataName, boolean convertKey);


    public native byte[] getDictTextN(DictEntry entry, boolean addHtmlHeader, boolean headerOnly, String htmlBegin, String htmlEnd);

    public native int readDictTextN(DictEntry entry, boolean addHtmlHeader, boolean headerOnly, StringBuffer html);

    /**
     * Method getDictDataN ...
     *
     * @param dataName   of type String
     * @param convertKey of type boolean
     * @return byte[]
     */
    private native byte[] getDictDataN(String dataName, boolean convertKey);

    private native byte[] getDictDataWithDictIdN(int dictId, String dataName, boolean convertKey);


    /**
     * Method getEntriesN ...
     *
     * @param startEntry    of type DictEntry
     * @param maxEntryCount of type int
     * @param entries       of type ArrayList<DictEntry>
     * @return int
     */
    private native int getEntriesN(DictEntry startEntry, int maxEntryCount, ArrayList<DictEntry> entries);

    /**
     * Method getHeadwordN ...
     *
     * @param entry of type DictEntry
     * @return int
     */
    private native int getHeadwordN(DictEntry entry);

    /**
     * Method locateFirstN ...
     *
     * @param headword     of type String
     * @param convertKey   of type boolean
     * @param partialMatch of type boolean
     * @param entry        of type DictEntry
     * @return int
     */
    private native int locateFirstN(String headword, boolean convertKey, boolean partialMatch, boolean startWithMatch, DictEntry entry);

    /**
     * Method getDictPrefN returns the dictPrefN of this MdxDictBase object.
     *
     * @return the dictPrefN (type int) of this MdxDictBase object.
     */
    private native int getDictPrefN();

    /**
     * Method isValidN returns the validN of this MdxDictBase object.
     *
     * @return the validN (type boolean) of this MdxDictBase object.
     */
    private native boolean isValidN();

    public native void setHtmlBlockHeader(String blockBegin, String blockEnd);

    public native void setHtmlHeader(String htmlBegin, String htmlEnd);

    /*
        public native void setExtraHeader(String extraHeader);
        public native void setCSS(String css);
        public native void setJavaScript(String javaScript);
         */
    public native void setUnionGroupTitle(String unionGroupTitle);

    public native void setChnConversion(int chnConversion);

    public static native boolean isMdxCmd(String word);

    public native void setViewSetting(DictPref dictPref);

    public native String getTitle(int dictId);
}
