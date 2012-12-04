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

/**
 * 本类用来表示普通辞典或者联合模式辞典中的一个条目入口
 * 当表示的是普通辞典的条目入口时, entryNo, headword, dictId都有效
 *
 * @author rayman
 *         Created on 11-12-31
 */
public class DictEntry {
    public static final int kSystemCmdEntryNo = -2, kUnionDictEntryNo = 0x7fffffff, kInvalidEntryNo = -1;

    /**
     * Constructor DictEntry creates a new DictEntry instance.
     */
    public DictEntry() {
        fInstance = constructDictEntry(-1, "", DictPref.kInvalidDictPrefId);
    }

    /**
     * Constructor DictEntry creates a new DictEntry instance.
     *
     * @param entryNo  of type int
     * @param headword of type String
     * @param dictId   of type int
     */
    public DictEntry(int entryNo, String headword, int dictId) {
        fInstance = constructDictEntry(entryNo, headword, dictId);
    }

    /**
     * Constructor DictEntry creates a new DictEntry instance.
     *
     * @param entry of type DictEntry
     */
    public DictEntry(DictEntry entry) {
        fInstance = copyDictEntry(entry);
    }

    /**
     * Constructor DictEntry creates a new DictEntry instance.
     *
     * @param instance of type int
     */
    public DictEntry(int instance) {
        fInstance = instance;
    }

    /**
     * Method invalidate ...
     */
    public void invalidate() {
        releaseCppObject();
        fInstance = constructDictEntry(-1, "", DictPref.kInvalidDictPrefId);
    }

    /**
     * Method makeJEntry ...
     */
    public void makeJEntry() {
        headword = getHeadword();
        entryNo = getEntryNo();
        dictId = getDictId();
        siblingCount = getSiblingCount();
    }

    /**
     * Method finalize ...
     *
     * @throws Throwable when
     */
    protected void finalize() throws Throwable {
        releaseCppObject();
        super.finalize();
    }

    /**
     * Method getHeadword returns the headword of this DictEntry object.
     * <p/>
     * Field headword
     *
     * @return the headword (type String) of this DictEntry object.
     */
    // Native declarations
    public native String getHeadword();

    /**
     * Method setHeadword sets the headword of this DictEntry object.
     * <p/>
     * Field headword
     *
     * @param headwordStr the headword of this DictEntry object.
     */
    public native void setHeadword(String headwordStr);

    /**
     * Method getEntryNo returns the entryNo of this DictEntry object.
     * <p/>
     * Field entryNo
     *
     * @return the entryNo (type int) of this DictEntry object.
     */
    public native int getEntryNo();

    /**
     * Method setEntryNo sets the entryNo of this DictEntry object.
     * <p/>
     * Field entryNo
     *
     * @param entryNo the entryNo of this DictEntry object.
     */
    public native void setEntryNo(int entryNo);

    /**
     * Method getDictId returns the dictId of this DictEntry object.
     * <p/>
     * Field dictId
     *
     * @return the dictId (type int) of this DictEntry object.
     */
    public native int getDictId();

    /**
     * Method setDictId sets the dictId of this DictEntry object.
     * <p/>
     * Field dictId
     *
     * @param dictId the dictId of this DictEntry object.
     */
    public native void setDictId(int dictId);

    /**
     * Method isValid returns the valid of this DictEntry object.
     *
     * @return the valid (type boolean) of this DictEntry object.
     */
    public native boolean isValid();

    /**
     * Method isSysCmd returns the sysCmd of this DictEntry object.
     *
     * @return the sysCmd (type boolean) of this DictEntry object.
     */
    public native boolean isSysCmd();

    public native boolean isUnionDictEntry();

    /**
     * Method getSiblingCount returns the siblingCount of this DictEntry object.
     * <p/>
     * Field siblingCount
     *
     * @return the siblingCount (type int) of this DictEntry object.
     */
    public native int getSiblingCount();

    public DictEntry getSiblingAt(int index) {
        return new DictEntry(getSiblingAtN(index));
    }

    private native int getSiblingAtN(int index);

    /**
     * 构筑一个简单条目
     *
     * @param entryNo  记录号
     * @param headword 词头
     * @param dictId   辞典id
     * @return JNI对象的句柄
     */
    private native static int constructDictEntry(int entryNo, String headword, int dictId);

    /**
     * 完全复制一个条目入口
     *
     * @param entry of type DictEntry
     * @return int
     */
    private native int copyDictEntry(DictEntry entry);

    /**
     * Method releaseCppObject ...
     *
     * @return int
     */
    private native int releaseCppObject();

    /* 调试用,无实际用途 */
    private String headword;

    /* 调试用,无实际用途 */
    private int entryNo;

    /* 调试用,无实际用途 */
    private int dictId;

    /* 调试用,无实际用途 */
    private int siblingCount;

    /* JNI 对象的句柄 */
    private int fInstance = 0;

}
