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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.TextView;
import cn.mdict.R;
import cn.mdict.mdx.DictPref;

/**
 * @author rayman
 *
 */
public class LibraryAdapter extends BaseAdapter {

	/**
	 * 
	 */
	private DictPref rootDictPref = null;
    private Context context = null; 
    private int currentDictId;
	public LibraryAdapter(Context context, DictPref rootDictPref, int currentDictId ) {
		this.rootDictPref=rootDictPref;
		this.context=context;
		this.currentDictId=currentDictId;
	}

    public void setDataSource(DictPref rootDictPref){
        this.rootDictPref=rootDictPref;
    }

    public DictPref getDataSource(){
        return rootDictPref;
    }

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getCount()
	 */
	@Override
	public int getCount() {
		return rootDictPref.getChildCount();
	}

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getItem(int)
	 */
	@Override
	public Object getItem(int position) {
		return rootDictPref.getChildDictPrefAtIndex(position).getDictName();
	}

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getItemId(int)
	 */
	@Override
	public long getItemId(int position) {
		return rootDictPref.getChildDictPrefAtIndex(position).getDictId();
	}

    public void updateRowView(View rowView, int position){
        if (rowView!=null) {
            CheckedTextView textView=(CheckedTextView)rowView.findViewById(android.R.id.text1);
    		DictPref item= rootDictPref.getChildDictPrefAtIndex(position);
    		String file_path=item.getDictName();
            int slash_pos=file_path.lastIndexOf('/');
            int dot_pos=file_path.lastIndexOf('.');
            if (dot_pos<0)
                dot_pos=file_path.length();
    		textView.setText(file_path.substring(slash_pos+1, dot_pos));

            //CheckBox cb=(CheckBox)convertView.findViewById(R.id.checkbox);
            CheckedTextView cb=textView;
            if ( cb!=null ){
                if (rootDictPref.isUnionGroup()){
                    boolean state=!item.isDisabled();
              		cb.setChecked(state);
                } else
              			cb.setChecked(currentDictId==item.getDictId());
            }
        }

    }

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getView(int, android.ic_view.View, android.ic_view.ViewGroup)
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
        	convertView= LayoutInflater.from(context).inflate(R.layout.library_list_item_view, null);
        }
        updateRowView(convertView, position);
        return convertView;
	}
}
