package com.mypaceengine.downloadnarrative;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.ArrayList;

public class WebActivity extends AppCompatActivity {

    WebView webView;
    public static final String DOMAIN="narrativeapp.com";

    DataUtil dataUtil=null;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // タイトルバー非表示
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        dataUtil=new DataUtil(this);

        setContentView(com.mypaceengine.downloadnarrative.R.layout.activity_web);
        Intent intent = getIntent();
        String url = intent.getStringExtra("URL"); // someDataがStringの場合

        webView = (WebView) findViewById(com.mypaceengine.downloadnarrative.R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setWebViewClient(new WebViewClient(){

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);

            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d("FinishedURL", url);
                // 自ドメインの時CookieをCheckする
                //               if(url.indexOf(DOMAIN) > -1){
                String cookie = CookieManager.getInstance().getCookie(DOMAIN);// 文字列でCookieを取得
                String[] oneCookie = cookie.split(";");
                ArrayList<String> list = new ArrayList<String>();
                for (String pair : oneCookie) {
                    list.add(pair.trim());
                }
                dataUtil.setNarrativeCookie(list);
                //               }else{
                String urlHeader = "com.mypaceengine.narrativeclip.photodawnloadfornarrativeservice://seacret?code=";
                if (url.toLowerCase().indexOf(urlHeader) == 0) {
                    String secret = url.toLowerCase().replace(urlHeader, "");
                    //if (syncService != null) {
                        dataUtil.setNarrativeKey(secret);
                        dataUtil.setNarrativeReauthNeed(false);
                    //}
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                }
                //               }
            }
        });
        webView.loadUrl(url);

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unbind
       // unbindService(mSampleServiceConnection);
    }
}
