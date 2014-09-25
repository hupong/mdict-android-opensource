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
 * Class MdxLibraryMgrRef ...
 *
 * @author rayman
 *         Created on 11-12-31
 */
public class MdxLibraryMgrRef {
    /**
     * Field fInstance
     */
    private int fInstance;


    public DictPref createDictPref() {
        return new DictPref(createDictPrefN());
    }

    /**
     * Constructor MdxLibraryMgrRef creates a new MdxLibraryMgrRef instance.
     *
     * @param instance of type int
     */
    public MdxLibraryMgrRef(int instance) {
        fInstance = instance;
    }

    /**
     * Method getRootDictPref returns the rootDictPref of this MdxLibraryMgrRef object.
     *
     * @return the rootDictPref (type DictPref) of this MdxLibraryMgrRef object.
     */
    public DictPref getRootDictPref() {
        return new DictPref(getRootDictPrefN());
    }

    /**
     * Method updateDictPref ...
     *
     * @param dictPref of type DictPref
     */
    public native void updateDictPref(DictPref dictPref);

    /**
     * Method moveTo ...
     *
     * @param dictId   of type int
     * @param position of type int
     * @return boolean
     */
    public native boolean moveTo(int dictId, int position);

    /**
     * Method removeDictPref ...
     *
     * @param dictId of type int
     */
    public native void removeDictPref(int dictId);

    /**
     * Remove all dictPref with the same name
     * @param dictName
     */
    public native void removeDict(String dictName);


    public native void setDefaultViewSetting(DictPref dictPref);

    public DictPref getDictPref(int dictId ){
        int dictInst=getDictPrefN(dictId);
        if (dictInst!=0){
            return new DictPref(dictInst);
        }else
            return null;
    }

    public native void addDictPref(int parentGroupId, DictPref dictPref);

    public native void updateAllLibViewSetting(DictPref dictPref);

    public native void searchDir( int parentGroupId, String searchDir, String filenamePattern, boolean userDirNameAsGroup );

    public native int createNewDictId();

    private native int createDictPrefN();
    private native int getDictPrefN(int dictId );

    /**
     * Method getRootDictPrefN returns the rootDictPrefN of this MdxLibraryMgrRef object.
     *
     * @return the rootDictPrefN (type int) of this MdxLibraryMgrRef object.
     */
    // Native declarations
    private native int getRootDictPrefN();


}