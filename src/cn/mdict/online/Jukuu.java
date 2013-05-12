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
import cn.mdict.R;
import cn.mdict.utils.IOUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: rayman
 * Date: 13-2-20
 * Time: 下午4:33
 */
public class Jukuu implements OnlineReference {
    public Jukuu(Context context) {
        StringBuffer temp = new StringBuffer();
        IOUtil.loadStringFromAsset(context.getAssets(),
                "JukuuTemplate.html", temp, false);
        template = temp.toString();
    }

    private void notifyResult(Handler handler, int retCode, String resultMsg) {
        Message msg = new Message();
        msg.arg1 = retCode;
        msg.obj = resultMsg;
        handler.sendMessage(msg);

    }

    @Override
    public void lookup(String headword, Context context, final Handler resultHandler) {
        final String search = headword;
        if (!MiscUtils.checkNetworkStatus(context)) {
            notifyResult(resultHandler, 1, context.getResources().getString(R.string.network_not_available));
        }
        new Thread(new Runnable() {
            public void run() {
                StringBuffer resultText = new StringBuffer();
                int retCode = captureJukuu(search, resultText) ? 1 : 0;
                notifyResult(resultHandler, retCode, template.replace("{DICT_CONTENT}", resultText.toString()));
            }
        }).start();
    }

    private boolean captureJukuu(String headWord, StringBuffer resultText) {
        //TODO Output is empty when there is no matched headword, need more works
        boolean result = false;
        Pattern imgPattern = Pattern
                .compile("<tr class=e>(.|\n)*?<tr class=s>");
        String searchStr = URLEncoder.encode(headWord.replace(" ", "+"));
        HttpURLConnection connection = null;
        InputStream is;
        String content;
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
                    String line;
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                    }

                    content = buffer.toString();
                    Matcher matcher = imgPattern.matcher(content);

                    while (matcher.find()) {
                        resultText.append(matcher.group().replace("<tr class=s>", "")).append("\n");
                    }
                    if (!content.contains("\u003e\u4e0b\u4e00\u9875\u003c\u002f\u0061\u003e")) {
                        result = true;
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

    private String template;
}
