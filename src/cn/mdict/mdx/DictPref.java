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
 * Class DictPref ...
 *
 * @author rayman
 *         Created on 11-12-31
 */
public class DictPref {
    /**
     * Field fInstance
     */
    private int fInstance = 0;

    /**
     * Constructor DictPref creates a new DictPref instance.
     *
     * @param instance of type int
     */
    //Construct object by a Cpp object pointer
    public DictPref(int instance) {
        fInstance = instance;
    }

    public static DictPref createDictPref() {
        return new DictPref(createNativeDictPref());
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
     * Method getChildDictPrefAtIndex ...
     *
     * @param index of type int
     * @return DictPref
     */
    public DictPref getChildDictPrefAtIndex(int index) {
        int resInst = getChildDictPrefAtIndexN(index);
        if (resInst != 0) {
            return new DictPref(resInst);
        } else
            return null;
    }

    /**
     * Field kRootDictPrefId
     */
    public final static int kRootDictPrefId = 0;
    public final static int kDefaultGrpId=1;

    /**
     * Field kInvalidDictPrefId
     */
    public final static int kInvalidDictPrefId = (-1);
    /**
     * Field kMaxDictPrefId
     */
    public final static int kMaxDictPrefId = (0x7fffffff);

    public final static int kZoomSmallest = 2;
    public final static int kZoomMedium = 2;
    public final static int kZoomLargest = 10;

    /**
     * Field kChnConvNone
     */
    public final static int kChnConvNone = 0;
    /**
     * Field kChnConvToSimplified
     */
    public final static int kChnConvToSimplified = 1;
    /**
     * Field kChnConvToTraditional
     */
    public final static int kChnConvToTraditional = 2;

    /**
     * Method releaseCppObject ...
     *
     * @return int
     */
    // Native declarations
    public native int releaseCppObject();

    /**
     * Method getChildCount returns the childCount of this DictPref object.
     *
     * @return the childCount (type int) of this DictPref object.
     */
    public native int getChildCount(); // size_t

    /**
     * Method getChildDictPrefAtIndexN ...
     *
     * @param index of type int
     * @return int
     */
    public native int getChildDictPrefAtIndexN(int index); // size_t

    /**
     * Method getDictId returns the dictId of this DictPref object.
     *
     * @return the dictId (type int) of this DictPref object.
     */
    public native int getDictId();

    public native void setDictId(int dictId);

    /**
     * Method getDictName returns the dictName of this DictPref object.
     *
     * @return the dictName (type String) of this DictPref object.
     */
    public native String getDictName();


    public native void setDictName(String fileName);


    public native String getTextColor();

    public native void setTextColor(String textColor);

    public native String getBackgroundColor();

    public native void setBackgroundColor(String backgroundColor);

    public native String getFontFace();

    public native void setFontFace(String fontFace);
    /**
     * Method isDisabled returns the disabled of this DictPref object.
     *
     * @return the disabled (type boolean) of this DictPref object.
     */
    public native boolean isDisabled();

    /**
     * Method setDisabled sets the disabled of this DictPref object.
     *
     * @param disabled the disabled of this DictPref object.
     */
    public native void setDisabled(boolean disabled);

    /**
     * Method isDictGroup returns the dictGroup of this DictPref object.
     *
     * @return the dictGroup (type boolean) of this DictPref object.
     */
    public native boolean isDictGroup();

    /**
     * Method setDictGroup sets the dictGroup of this DictPref object.
     *
     * @param isDictGroup the dictGroup of this DictPref object.
     */
    public native void setDictGroup(boolean isDictGroup);

    /**
     * Method isUnionGroup returns the unionGroup of this DictPref object.
     *
     * @return the unionGroup (type boolean) of this DictPref object.
     */
    public native boolean isUnionGroup();

    /**
     * Method setUnionGroup sets the unionGroup of this DictPref object.
     *
     * @param isUnionGroup the unionGroup of this DictPref object.
     */
    public native void setUnionGroup(boolean isUnionGroup);

    /**
     * Method setChnConversion sets the chnConversion of this DictPref object.
     *
     * @param chnConv the chnConversion of this DictPref object.
     */
    public native void setChnConversion(int chnConv);

    /**
     * Method getChnConversion returns the chnConversion of this DictPref object.
     *
     * @return the chnConversion (type int) of this DictPref object.
     */
    public native int getChnConversion();

    public native int zoomLevel();

    public native void setZoomLevel(int zoomLevel);

    /**
     * Method updateChildPref ...
     *
     * @param childPref of type DictPref
     * @return boolean
     */
    public native boolean updateChildPref(DictPref childPref);

    public native boolean moveChildToPos(int childDictId, int position);

    public native boolean removeChildDictPref(int childDictId);

    public native void addChildPref(DictPref childPref);

    private static native int createNativeDictPref();

    public boolean hasEnabledChild() {
        boolean hasEnabledChild = false;
        for (int i = 0; i < getChildCount(); ++i) {
            if (!getChildDictPrefAtIndex(i).isDisabled()) {
                hasEnabledChild = true;
                break;
            }
        }
        return hasEnabledChild;
    }

}
