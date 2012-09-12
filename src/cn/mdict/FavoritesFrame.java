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

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import cn.mdict.mdx.DictBookmarkRef;
import cn.mdict.mdx.DictEntry;
import cn.mdict.mdx.MdxEngine;
import cn.mdict.widgets.BookmarkAdapter;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class FavoritesFrame extends BookmarkActivity {
    private BookmarkAdapter adapter=null;

    @Override
    public DictBookmarkRef getBookmarkMgr() {
        return MdxEngine.getFavMgr();
    }

    @Override
    public int getLayoutResId() {
        return R.layout.favorites_frame;
    }

    @Override
    public int getOptionMenuResId() {
        return R.menu.favorites_frame_optioin_menu;
    }

    @Override
    public int getItemOptionMenuResId() {
        return R.menu.favorites_frame_item_option_menu;
    }

    @Override
    public BookmarkAdapter getBookmarkAdapter() {
        if (adapter==null)
            adapter=new BookmarkAdapter(this, getBookmarkMgr(), true);
        return adapter;
    }

}
