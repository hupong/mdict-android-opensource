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

import java.util.ArrayList;
import java.util.HashMap;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import cn.mdict.R;

/**
 * @author Rayman
 *
 */
public class GridViewMenu {

	public GridViewMenu() {
	}

	public int getSelectedMenuItem(){
		return selectedMenuItem;
	}

	public void show(){
		gridViewDialog.show();
	}
	
	public void setOnDismissListener( android.content.DialogInterface.OnDismissListener listener){
		gridViewDialog.setOnDismissListener(listener);
	}
	public void createMenu(Context context, String[] menuNameArray, int[] imageResourceArray, int[] menuIdArray){
		if (menuNameArray!=null){
			this.menuNameArray=new String[menuNameArray.length];
			System.arraycopy(menuNameArray, 0, this.menuNameArray, 0, menuNameArray.length);
		}else
			this.menuNameArray=null;
		
		if ( imageResourceArray!=null ){
			this.imageResourceArray= new int[imageResourceArray.length];		
			System.arraycopy(imageResourceArray, 0, this.imageResourceArray, 0, imageResourceArray.length);
		}else
			this.imageResourceArray=null;
		
		if (menuIdArray!=null){
			this.menuIdArray= new int[menuIdArray.length];		
			System.arraycopy(menuIdArray, 0, this.menuIdArray, 0, menuIdArray.length);			
		}else
			this.menuIdArray=null;

		View gridLayout = View.inflate(context, R.layout.grid_view_menu, null);
		// 创建AlertDialog
		gridViewDialog = new AlertDialog.Builder(context).create();
		gridViewDialog.setView(gridLayout);

		gridMenu=(GridView) gridLayout.findViewById(R.id.grid_view_menu);
		if (menuNameArray.length>0){
			setupMenuAdapter(context);
		}
		//FrameLayout fl = (FrameLayout) findViewById(android.R.id.custom);
		// fl.addView(gridLayout, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

		gridMenu.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if ( GridViewMenu.this.menuIdArray!=null ){
					selectedMenuItem=GridViewMenu.this.menuIdArray[position];
				}else
					selectedMenuItem=position;
				gridViewDialog.dismiss();
			}
			
		});
		gridViewDialog.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode,
                    KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_MENU){// 监听按键
                	selectedMenuItem=-1;
                    dialog.dismiss();
                }else if ( keyCode==KeyEvent.KEYCODE_BACK){
                	selectedMenuItem=-1;
                }
                return false;
            }
        });

	}
	
	private void setupMenuAdapter(Context context) {

		if (gridMenu!=null){
			ArrayList<HashMap<String, Object>> data = new ArrayList<HashMap<String, Object>>();
			for (int i = 0; i < menuNameArray.length; i++) {
				HashMap<String, Object> map = new HashMap<String, Object>();
				map.put("itemImage", imageResourceArray[i]);
				map.put("itemText", menuNameArray[i]);
				data.add(map);
			}
			gridAdapter = new SimpleAdapter(context, data,
					R.layout.grid_view_menu_item, new String[] { "itemImage", "itemText" },
					new int[] { R.id.grid_menu_item_image, R.id.grid_menu_item_text });

			gridMenu.setAdapter(gridAdapter);			
		}
	}

	private AlertDialog gridViewDialog=null;
	private GridView gridMenu=null;
	private String[] menuNameArray=null;
	private int[] imageResourceArray=null;
	private int[] menuIdArray=null;
	private BaseAdapter gridAdapter=null;
	private int selectedMenuItem=-1;
}
