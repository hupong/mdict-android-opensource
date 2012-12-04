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

/**
 * 用来记录查询记录的通用类，收藏和历史记录都是使用该类作为存储
 *
 * @author rayman
 *         Created on 11-12-31
 */
public class DictBookmarkRef {

    /**
     * 加入条目
     *
     * @param entry 需要加入的条目
     */
    public native void add(DictEntry entry);

    /**
     * 删掉指定位置的记录
     *
     * @param index 要删除的记录的位置
     */
    public native void remove(int index);

    /**
     * 清除所有记录
     */
    public native void clear();

    /**
     * 获取相对当前位置的下一条记录，并将其作为当前记录
     *
     * @return 如果有下一条记录，则返回记录，否则返回null
     */
    public DictEntry getNext() {
        int instance = getNextN();
        if (instance != 0)
            return new DictEntry(instance);
        else
            return null;
    }

    /**
     * 获取相对当前位置的上一条记录，并将其作为当前记录
     *
     * @return 如果有上一条记录，则返回记录，否则返回null
     */
    public DictEntry getPrev() {
        int instance = getPrevN();
        if (instance != 0)
            return new DictEntry(instance);
        else
            return null;
    }

    /**
     * 是否存在下一条记录
     *
     * @return
     */
    public native boolean hasNext();

    /**
     * 是否存在上一条记录
     *
     * @return
     */
    public native boolean hasPrev();

    /**
     * 获取记录总数
     *
     * @return 记录总数
     */
    public native int getCount();

    /**
     * 获取指定位置的记录
     *
     * @param index 记录位置
     * @return 条目记录
     */
    public native DictEntry getEntryByIndex(int index);

    /**
     * getNext()的JNI实现
     */
    private native int getNextN();

    /**
     * getPrev()的JNI实现
     */
    private native int getPrevN();

    /**
     * JNI对象的句柄
     */
    private int fInstance;

    /**
     * 记录的类型，内部使用
     */
    private int fType;

}
