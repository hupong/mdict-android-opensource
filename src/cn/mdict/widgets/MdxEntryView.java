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

package cn.mdict.widgets;

import android.view.View;
import cn.mdict.WebViewGestureFilter;
import cn.mdict.mdx.DictEntry;

/**
 * User: Rayman
 * Date: 12-5-18
 * Time: 下午3:19
 */
public interface MdxEntryView {
    public void setMdxView(MdxView mdxView);

    public void setGestureListener(WebViewGestureFilter.GestureListener listener);

    public void showAllEntries(boolean show);

    public void zoomIn();

    public void zoomOut();

    public void displayEntry(DictEntry entry);

    public View getContainer();
}
