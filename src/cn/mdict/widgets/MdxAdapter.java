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
import android.widget.BaseAdapter;
import android.widget.TextView;
import cn.mdict.R;
import cn.mdict.mdx.DictEntry;
import cn.mdict.mdx.DictPref;
import cn.mdict.mdx.MdxDictBase;

import java.util.ArrayList;

/**
 * @author rayman
 */
public class MdxAdapter extends BaseAdapter {

    /**
     *
     */
    private MdxDictBase dict = null;
    private int dictId = DictPref.kInvalidDictPrefId;
    private String invalidDictPrompt = null;
    private String emptyListPrompt = null;
    private Context context = null;
    private ArrayList<DictEntry> currentPage = new ArrayList<DictEntry>();
    //    private DictEntry currentEntry=null;
    private final int pageSize = 30;

    public MdxAdapter(Context context) {
        this.context = context;
    }

    public void setDict(MdxDictBase dict, String emptyListPrompt, String invalidDictPrompt) {
        this.dict = dict;
        if (dict != null && dict.isValid())
            dictId = dict.getDictPref().getDictId();
        else
            dictId = DictPref.kInvalidDictPrefId;
        this.invalidDictPrompt = invalidDictPrompt;
        this.emptyListPrompt = emptyListPrompt;

        setCurrentEntry(null);
        this.notifyDataSetChanged();
    }

    public void setCurrentEntry(DictEntry currentEntry) {
//		this.currentEntry=currentEntry;
        currentPage.clear();
        if (currentEntry != null && currentEntry.isValid() && dict != null && dict.isValid() && !dict.canRandomAccess()) {
            dict.getEntries(currentEntry, pageSize, currentPage);
        }
        notifyDataSetChanged();
    }

    public DictEntry getEntryByPosition(int position) {
        if (dict.canRandomAccess()) {
            DictEntry res = new DictEntry(position, "", dictId);
            dict.getHeadword(res);
            return res;
        } else {
            if (currentPage.size() > position)
                return currentPage.get(position);
            else
                return null;
        }
    }

    /* (non-Javadoc)
      * @see android.widget.Adapter#getCount()
      */
    @Override
    public int getCount() {
        if (dict != null && dict.isValid()) {
            if (dict.canRandomAccess())
                return dict.getEntryCount();
            else if (currentPage.size() > 0)
                return currentPage.size();
            else
                return 1;
        } else
            return 1;
    }

    /* (non-Javadoc)
      * @see android.widget.Adapter#getItem(int)
      */
    @Override
    public Object getItem(int position) {
        if (dict == null || !dict.isValid())
            return null;
        DictEntry entry;

        if (dict.canRandomAccess()) {
            entry = new DictEntry(position, "", dictId);
            dict.getHeadword(entry);
            return entry;
        } else {
            if (currentPage.size() > 0) {
                entry = currentPage.get(position);
                return entry;
            } else {
                return null;
            }
        }
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
        if (convertView == null) {
            /**
             * �ڳ����ж�̬�������ϲ���: 
             * LayoutInflater flater = LayoutInflater.from(this); 
             * View ic_view = flater.inflate(R.layout.example, null);
             */
            convertView = LayoutInflater.from(context).inflate(R.layout.entry_list_item_view, null);
        }
        if (convertView != null) {
            TextView textView = (TextView) convertView.findViewById(R.id.entry_headword);
            TextView entryCount = (TextView) convertView.findViewById(R.id.item_count);
            Object obj = getItem(position);
            if (obj != null) {
                DictEntry entry = (DictEntry) obj;
                textView.setText(entry.getHeadword());
                int itemCount = entry.getSiblingCount();
                if (itemCount != 0) {
                    entryCount.setText("(" + itemCount + ")");
                } else {
                    entryCount.setText("");
                }
            } else {
                if (dict == null || !dict.isValid())
                    textView.setText(invalidDictPrompt);
                else
                    textView.setText(emptyListPrompt);
                entryCount.setText("");
            }
        }
        return convertView;
    }

}
