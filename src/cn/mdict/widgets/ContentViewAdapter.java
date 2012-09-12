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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import cn.mdict.R;
import cn.mdict.mdx.DictEntry;
import cn.mdict.mdx.MdxDictBase;
import cn.mdict.mdx.MdxUtils;

/**
 * Created by IntelliJ IDEA.
 * User: Rayman
 * Date: 12-3-2
 * Time: 上午11:06
 * To change this template use File | Settings | File Templates.
 */
public class ContentViewAdapter extends BaseAdapter {

    /**
     *
     */
    private Context context = null;
    private MdxDictBase dict;
    private DictEntry currentEntry=null;
    private View rowViewCache[]=null;
    public ContentViewAdapter(Context context, MdxDictBase dict ) {
        this.context=context;
        this.dict=dict;
    }

    public void setDict(MdxDictBase dict){
        this.dict=dict;
    }

    public void setCurrentEntry(DictEntry entry){
        currentEntry=new DictEntry(entry);
        if (currentEntry.getSiblingCount()>0)
            rowViewCache =new View[currentEntry.getSiblingCount()];
        else
            rowViewCache =new WebView[1];
        notifyDataSetChanged();
    }

    /* (non-Javadoc)
     * @see android.widget.Adapter#getCount()
     */
    @Override
    public int getCount() {
        if (currentEntry!=null)
            return currentEntry.getSiblingCount();
        else
            return 0;
    }

    /* (non-Javadoc)
     * @see android.widget.Adapter#getItem(int)
     */
    @Override
    public Object getItem(int position) {
        return currentEntry.getSiblingAt(position);
    }

    /* (non-Javadoc)
     * @see android.widget.Adapter#getItemId(int)
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /* (non-Javadoc)
     * @see android.widget.Adapter#getView(int, android.ic_view.View, android.ic_view.ViewGroup)
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = rowViewCache[position];
        if (convertView == null) {
            convertView= LayoutInflater.from(context).inflate(R.layout.content_view, null);
        }
        rowViewCache[position]=convertView;
        WebView htmlView=(WebView)convertView.findViewById(R.id.webview);
        if (htmlView!=null){
            htmlView.getSettings().setJavaScriptEnabled(true);
            //htmlView.getSettings().setBuiltInZoomControls(true);
            htmlView.getSettings().setSupportZoom(true);
            htmlView.clearView();
            MdxUtils.displayEntry(htmlView, dict, (DictEntry)getItem(position));
        }
        return convertView;
    }

}
