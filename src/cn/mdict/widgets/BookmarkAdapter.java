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

/**
 *
 */
package cn.mdict.widgets;

import android.content.Context;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import cn.mdict.R;
import cn.mdict.mdx.DictBookmarkRef;
import cn.mdict.mdx.DictEntry;

/**
 * @author rayman
 */
public class BookmarkAdapter extends BaseAdapter {

    /**
     *
     */
    public interface ItemCheckedChangeListener {
        public void OnItemCheckedChanged(int position, boolean checked);
    }

    private DictBookmarkRef bookmark = null;
    private Context context = null;
    private boolean reverseOrder;
    private SparseBooleanArray checkState = null;
    private ItemCheckedChangeListener itemCheckedChangedListener;


    class CheckableItemListener implements View.OnClickListener {
        private int position;

        public CheckableItemListener(int position) {
            this.position = position;
        }

        @Override
        public void onClick(View view) {
            boolean state = !checkState.get(position, false);
            if (state)
                checkState.put(position, state);
            else
                checkState.delete(position);

            if (itemCheckedChangedListener != null) {
                itemCheckedChangedListener.OnItemCheckedChanged(position, state);
            }
        }

    }

    public BookmarkAdapter(Context context, DictBookmarkRef bookmark, boolean reverseOrder) {
        this.bookmark = bookmark;
        this.context = context;
        this.reverseOrder = reverseOrder;
        checkState = new SparseBooleanArray(getCount());
    }

    public void setItemCheckedChangeListener(ItemCheckedChangeListener listener) {
        itemCheckedChangedListener = listener;
    }

    public SparseBooleanArray getCheckStates() {
        return checkState;
    }

    public int getCheckItemCount() {
        return checkState.size();
    }

    public int getRealIndex(int position) {
        int index;
        if (reverseOrder)
            index = bookmark.getCount() - 1 - position;
        else
            index = position;
        return index;
    }

    /* (non-Javadoc)
     * @see android.widget.Adapter#getCount()
     */
    @Override
    public int getCount() {
        return bookmark.getCount();
    }

    /* (non-Javadoc)
     * @see android.widget.Adapter#getItem(int)
     */
    @Override
    public Object getItem(int position) {
        DictEntry entry = bookmark.getEntryByIndex(getRealIndex(position));
        return entry.getHeadword();
    }

    /* (non-Javadoc)
     * @see android.widget.Adapter#getItemId(int)
     */
    @Override
    public long getItemId(int position) {
        return getRealIndex(position);
    }


    void dumpViewIds(ViewGroup vg, int parentId) {
        for (int i = 0; i < vg.getChildCount(); ++i) {
            if (vg.getChildAt(i) instanceof ViewGroup) {
                dumpViewIds((ViewGroup) vg.getChildAt(i), vg.getId());
            } else {
                Log.d("ViewId", String.format("view id=%d, parent id=%d", vg.getChildAt(i).getId(), parentId));
            }
        }

    }

    /* (non-Javadoc)
     * @see android.widget.Adapter#getView(int, android.ic_view.View, android.ic_view.ViewGroup)
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CheckableItemListener listener = new CheckableItemListener(getRealIndex(position));
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.simple_list_item_checked, null);
        }

        if (convertView != null) {

            TextView textView = (TextView) convertView.findViewById(R.id.item_title);
            String text = (String) getItem(position);
            textView.setText(text);

            CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checkbox);
            if (checkBox != null && checkState != null) {
                checkBox.setChecked(checkState.get(getRealIndex(position), false));
                checkBox.setOnClickListener(listener);
            }
            //checkBox.setOnCheckedChangeListener(listener);
        }
        return convertView;
    }

    @Override
    public void notifyDataSetChanged() {
        checkState = new SparseBooleanArray(getCount());
        super.notifyDataSetChanged();
    }

}
