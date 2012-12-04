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

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import cn.mdict.R;
import cn.mdict.WebViewGestureFilter;
import cn.mdict.mdx.DictEntry;
import cn.mdict.mdx.MdxDictBase;
import cn.mdict.mdx.MdxEngine;
import cn.mdict.mdx.MdxUtils;

/**
 * Created with IntelliJ IDEA.
 * User: Rayman
 * Date: 12-5-18
 * Time: 下午3:19
 * To change this template use File | Settings | File Templates.
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
