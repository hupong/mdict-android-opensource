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
import android.os.Message;
import cn.mdict.MiscUtils;
import cn.mdict.utils.IOUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: rayman
 * Date: 13-2-20
 * Time: 下午4:33
 * To change this template use File | Settings | File Templates.
 */
public class Jukuu implements OnlineReference{
    public Jukuu(Context context){
        template = new StringBuffer();
        IOUtil.loadStringFromAsset(context.getAssets(),
                "JukuuTemplate.html", template, false);
    }
    @Override
    public void lookup(String headword, Context context, final Handler resultHandler) {
        String result;
        final String search=headword;
        if (!MiscUtils.checkNetworkStatus(context)) {
            result="<font color=red><b>Network unavailable</b></font>";
        }
        new Thread(new Runnable() {
            public void run() {
                StringBuffer resultText=new StringBuffer();
                Message msg = new Message();
                msg.arg1 =captureJukuu(search, resultText)?1:0;
                msg.obj = template.toString().replace("{DICT_CONTENT}", resultText.toString()); //TODO very low efficiency
                resultHandler.sendMessage(msg);// 向Handler发送消息，
            }
        }).start();
    }

    private boolean captureJukuu(String headWord, StringBuffer resultText) {
        //TODO Output is empty when there is no matched headword, need more works
        boolean result=false;
        Pattern imgPattern = Pattern
                .compile("<tr class=e>(.|\n)*?<tr class=s>");
        String searchStr = URLEncoder.encode(headWord.replace(" ", "+"));
        HttpURLConnection connection = null;
        InputStream is = null;
        String content = "";
        try {

            for (int i = 0; i < 5; i++) {
                String surl = "http://www.jukuu.com/show-" + searchStr + "-"
                        + String.valueOf(i) + ".html";
                if (i == 0)
                    surl = "http://www.jukuu.com/search.php?q=" + searchStr;
                URL url = new URL(surl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10 * 1000); // set time out to 5
                // mins
                connection.setReadTimeout(20 * 1000); // set read time out
                int code = connection.getResponseCode();
                if (HttpURLConnection.HTTP_OK == code) {
                    connection.connect();
                    is = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(is, "utf-8"));
                    StringBuilder buffer = new StringBuilder();
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                    }

                    content = buffer.toString();
                    Matcher matcher = imgPattern.matcher(content);

                    while (matcher.find()) {
                        resultText.append(matcher.group().replace("<tr class=s>", "")).append("\n");
                    }
                    if (content.indexOf("\u003e\u4e0b\u4e00\u9875\u003c\u002f\u0061\u003e") == -1){
                        result=true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            resultText.append(MiscUtils.getErrorMessage(e)); //TODO: should promote user about error instead of displaying it as normal result?
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return result;
    }

    private StringBuffer template=null;
}
