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

package cn.mdict.services;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;

/**
 * Created by rayman on 13-7-5.
 */
public class LocalService extends Service {
    private final IBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        LocalService getService() {
            return LocalService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    static ServiceConnection serviceConnection;
    static LocalService localService;
    public static boolean startDownloadService(Context context){
        if (localService ==null){
            serviceConnection =new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    localService =((DownloadService.LocalBinder)service).getService();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    localService =null;
                }
            };
        }
        if (localService ==null)
            return context.bindService(new Intent(context, DownloadService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        else
            return true;
    }

    public LocalService getService(){
        return localService;
    }

    public static void stopDownloadService(Context context){
        if (serviceConnection !=null)
            context.unbindService(serviceConnection);
        serviceConnection =null;
    }
}
