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

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.client.ClientProtocolException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cn.mdict.R;
import cn.mdict.utils.IOUtil;

public class DownloadService extends LocalService{

    private static final String TAG = "MDict.DownloadService";

    private static ExecutorService executorService = Executors.newFixedThreadPool(5);
    public static HashMap<String, DownloadTask> downloading = new HashMap<String, DownloadTask>();
    public static Context context;



    class DownloadTask implements IOUtil.StatusReport{
        private Notification notification;
        private String downloadUrl;
        private String targetFile;
        private long contentLength=-1;
        private boolean canceled;
        private Context context;
        private String title;
        private PendingIntent contentIntent;
        private long lastPercent=0;

        private void notify(String msg){
            if ( notification!=null ){
                notification.setLatestEventInfo(context, title, msg, contentIntent);
            }
        }
        private void notify(int id){
            notify(context.getResources().getString(id));
        }
        private void notify(int formatId, String msg){
            notify(String.format(context.getResources().getString(formatId), msg));
        }
        private void notify(int formatId, int msgId){
            notify(formatId, context.getResources().getString(msgId));
        }

        @Override
        public void onProgressUpdate(long count) {
            if (contentLength>0){
                int percent=(int)(count*100/contentLength);
                if(percent-lastPercent>0){
                    lastPercent=percent;
                    notify(percent+"%");
                }else{
                    notify(R.string.content_downloaded, new Long(count).toString());
                }
            }
        }

        @Override
        public void onStart() {
            notify(R.string.content_download_begin);
        }

        @Override
        public void onGetTotal(long total) {
            contentLength=total;
        }

        @Override
        public void onComplete() {
            notification.flags = Notification.FLAG_AUTO_CANCEL;
            notification.icon=android.R.drawable.stat_sys_download_done;
            notify(R.string.content_download_complete);
        }

        @Override
        public void onError(Exception e) {
            if (e!=null){
                notification.flags = Notification.FLAG_AUTO_CANCEL;
                if ( e instanceof ClientProtocolException ){
                    notify(R.string.content_download_error, R.string.error_client_protocol);
                }else if ( e instanceof IOException) {
                    notify(R.string.content_download_error, R.string.error_io);
                } else {
                    notify(R.string.content_download_error, e.getClass().getSimpleName());
                }
                e.printStackTrace();
            }
        }

        @Override
        public void onInterrupted() {
            notification.flags = Notification.FLAG_AUTO_CANCEL;
            notify(R.string.content_download_canceled);
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        void download(Context context, PendingIntent contentIntent, String title, String url, String target){
            this.context=context;
            this.title=title;
            this.downloadUrl=url;
            this.targetFile=target;
            this.contentIntent=contentIntent;
            notification = new Notification();
            notification.icon = android.R.drawable.stat_sys_download;
            notification.tickerText = title;
            notification.when = System.currentTimeMillis();
            notification.defaults = Notification.DEFAULT_LIGHTS;
            notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
            notification.setLatestEventInfo(context, title, "", contentIntent);
            NotificationManager nm=(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.notify(url.hashCode(), notification);

            IOUtil.httpGetFile(url, target, this);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Download service started, Id: " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        //TODO Cancel all notifications
        super.onDestroy();
    }

    public void downloadFile(final String url, final String title, final String targetFile, Class<?> activityClass){
        if(downloading.containsKey(url))
            return;
        final PendingIntent contentIntent = PendingIntent.getActivity(context, url.hashCode(), new Intent(context, activityClass), 0);
        final DownloadTask downloadTask=new DownloadTask();
        downloading.put(url, downloadTask);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                downloadTask.download(context, contentIntent, title, url, targetFile);
                //TODO Need more works to notify caller that the job is done.
                //MiscUtils.installApk(context, targetFile);
            }
        });
    }

}
