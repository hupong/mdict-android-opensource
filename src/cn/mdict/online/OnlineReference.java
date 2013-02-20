/*
 * Copyright (C) 2013. Rayman Zhang <raymanzhang@gmail.com>
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

package cn.mdict.online;

import android.content.Context;
import android.os.Handler;

/**
 * Created with IntelliJ IDEA.
 * User: rayman
 * Date: 13-2-20
 * Time: 下午4:33
 * To change this template use File | Settings | File Templates.
 */
public interface OnlineReference {
    public void lookup(String headword, Context context, final Handler resultHandler);
}
