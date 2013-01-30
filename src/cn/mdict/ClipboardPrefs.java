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

/**
 * Define constants like name or keys for application preferences
 */
public class ClipboardPrefs {
   
    private ClipboardPrefs() {}
   
    /** Name of shared preference */
    public static final String NAME = "AppPrefs";
   
    /**
     * Time interval of monitor checking in milliseconds
     * <p>TYPE: int</p>
     */
    public static final String KEY_MONITOR_INTERVAL = "monitor.interval";
   
    /**
     * Id of current operating clipboard
     * <p>TYPE: int</p>
     */
    public static final String KEY_OPERATING_CLIPBOARD = "clipboard";

    public static final int DEF_MONITOR_INTERVAL = 500;

    public static final int DEF_OPERATING_CLIPBOARD = 1; // 1 = default clipboard

    /**
     * The current operating clipboard id
     * <p>
     * This is for cache usage. Every time MyClips creates (in onCreate()), it
     * reads from shared preference to here and writes back as it pauses (in
     * onPause()). When ClipboardMonitor is created (in onCreate()), it also
     * initialize this value.
     */
    public static volatile int operatingClipboardId;
}
