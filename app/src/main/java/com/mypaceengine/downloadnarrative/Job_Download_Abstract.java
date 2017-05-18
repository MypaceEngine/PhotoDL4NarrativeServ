package com.mypaceengine.downloadnarrative;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by MypaceEngine on 2017/05/13.
 */

public abstract class Job_Download_Abstract extends AbstractJobN implements Serializable {
    public void saveNarrativeSrv2File(String urlStr, File file) throws Exception {
        Log.d("MainServiceTask","saveNarrativeSrv2File:"+urlStr);
        String seacret = dataUtil.getNarrativeKey();
        if (seacret != null) {
            HttpURLConnection con = null;
            InputStream in=null;
            FileOutputStream fileOutputstream = null;
            try {
// URLの作成
                URL url = new URL(urlStr);

// 接続用HttpURLConnectionオブジェクト作成
                con = (HttpURLConnection) url.openConnection();
/*                ArrayList<String> cookie_list=service.getNarrativeCookie();
                for(String cookie:cookie_list){
                    CookieManager.getInstance().setCookie(WebActivity.DOMAIN,cookie);
                }

                String cookie = CookieManager.getInstance().getCookie(WebActivity.DOMAIN);

                con.addRequestProperty("Cookie", cookie);
*/
// リクエストメソッドの設定
                con.setRequestMethod("GET");
// リダイレクトを自動で許可しない設定
                con.setInstanceFollowRedirects(false);
// ヘッダーの設定(複数設定可能)
//                con.setRequestProperty("Authorization", "Bearer " + seacret);

// 接続
                con.connect();

                int responseCode = con.getResponseCode();
                String responseMessage = con.getResponseMessage();

                Log.d("Narrative Responce", String.format("%s[%d]", responseMessage, responseCode));

                switch (responseCode){
                    case HttpURLConnection.HTTP_UNAUTHORIZED:
                        //TODO handle unauthorized
                        dataUtil.setNarrativeReauthNeed(true);
                        return ;
                    default:
                }
                in = con.getInputStream();

                fileOutputstream = new FileOutputStream(new File(file.getAbsolutePath()));

                byte[] line = new byte[1024];
                int size;
                while (true) {
                    size = in.read(line);
                    if (size <= 0) {
                        break;
                    }
                    fileOutputstream.write(line, 0, size);
                }
            }catch(Exception ex){
                ex.printStackTrace();
                try {
                    file.deleteOnExit();
                }catch (Exception e2){
                    e2.printStackTrace();
                }
     /*           try{
                    Thread.sleep(3000);
                }catch (Exception ex2){}*/
                if (!shutdownFlg) {
                    saveNarrativeSrv2File(urlStr, file);
                }else{
                    throw ex;
                }
            }finally{
                if(con!=null){
                    con.disconnect();
                }
                if(in!=null){
                    in.close();
                }
                if(fileOutputstream!=null){
                    fileOutputstream.close();
                }
            }

        }
        return ;
    }
}
