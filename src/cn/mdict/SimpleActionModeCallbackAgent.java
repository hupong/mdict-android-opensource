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


import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * User: Rayman
 * Date: 12-2-2
 * Time: 下午4:24
 */
public class SimpleActionModeCallbackAgent implements ActionMode.Callback {
    public interface ActionItemClickListener {
        public boolean onActionItemClicked(ActionMode mode, MenuItem item);
    }

    private ActionItemClickListener listener = null;
    private int menuResId;
    private ActionMode actionMode;

    public SimpleActionModeCallbackAgent(int menuResId, ActionItemClickListener listener) {
        this.menuResId = menuResId;
        this.listener = listener;
    }

    ActionMode getActionMode() {
        return actionMode;
    }

    // Called when the action mode is created; startActionMode() was called
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        // Inflate a menu resource providing context menu items
        mode.setCustomView(null);
        mode.getMenuInflater().inflate(menuResId, menu);
        actionMode = mode;
        return true;
    }

    // Called each time the action mode is shown. Always called after onCreateActionMode, but
    // may be called multiple times if the mode is invalidated.
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false; // Return false if nothing is done
    }

    // Called when the user selects a contextual menu item
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (listener != null && listener.onActionItemClicked(mode, item))
            mode.finish(); // Action picked, so close the CAB
        return true;
    }

    // Called when the user exits the action mode
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        actionMode = null;
    }
}
