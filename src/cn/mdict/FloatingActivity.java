package cn.mdict;/*
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

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

/**
 * Created with IntelliJ IDEA.
 * User: alexli
 * Date: 10/20/12
 * Time: 8:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class FloatingActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            WindowManager wm = (WindowManager) getApplicationContext().getSystemService("window");
            WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();

            /**
             *以下都是WindowManager.LayoutParams的相关属性
             * 具体用途请参考SDK文档
             */
            /*
            * 这个FIRST_SYSTEM_WINDOW的值就是2000。
            * 2003和2002的区别就在于2003类型的View比2002类型的还要top，
            * 能显示在系统下拉状态栏之上
            */
            wmParams.type = 2002;   //这里是关键，你也可以试试2003
            wmParams.format = 1;
            /**
             *这里的flags也很关键
             *代码实际是wmParams.flags |= FLAG_NOT_FOCUSABLE;
             *40的由来是wmParams的默认属性（32）+ FLAG_NOT_FOCUSABLE（8）
             */
            wmParams.flags = 40;
            wmParams.width = 100;
            wmParams.height = 60;
            //wm.addView(bb, wmParams);  //创建View
        } catch (Exception e) {
        }
    }
}
