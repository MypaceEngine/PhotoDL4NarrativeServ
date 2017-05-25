package com.mypaceengine.downloadnarrative;


import java.io.DataOutputStream;
import java.lang.Thread.UncaughtExceptionHandler;

import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

public class UncoughtExceptionHandler  implements UncaughtExceptionHandler {

    private File BUG_REPORT_FILE = null;

    private UncaughtExceptionHandler mDefaultUEH;
    public UncoughtExceptionHandler(Context context) {
        sContext = context;
        BUG_REPORT_FILE = new File(context.getExternalFilesDir("log"),"bug.txt");

        mDefaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    }

    public void uncaughtException(Thread th, Throwable t) {

        try {
            saveState(t);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if( sContext.getMainLooper().getThread().equals(th)){
            mDefaultUEH.uncaughtException(th, t);
        }
    }

    private void saveState(Throwable e) throws FileNotFoundException {
        File file = BUG_REPORT_FILE;
        PrintWriter pw = null;
        pw = new PrintWriter(new FileOutputStream(file),true);
        e.printStackTrace(pw);
        pw.close();
    }

    private static Context sContext;
    private static PackageInfo sPackInfo;
    public static void postBugReportInBackground(Context context) {
        sContext=context;
        try {
            //パッケージ情報
            sPackInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        new Thread(new Runnable(){
            public void run() {
                postBugReport(sContext);
                File file  = new File(sContext.getExternalFilesDir("log"),"bug.txt");;
                if (file != null && file.exists()) {
                    file.delete();
                }
            }}).start();
    }

    private static void postBugReport(Context context) {
        HttpURLConnection con = null;
        DataOutputStream os = null;
        File bugFile=new File(context.getExternalFilesDir("log"),"bug.txt");
        try {
            if(bugFile.exists()) {
                String bug = getFileBody(bugFile);
                bug = URLEncoder.encode(bug, "UTF-8");
                String requestBody =
                        "name=NarrativeServiceSync&" +
                                "dev=" + Build.DEVICE + "&" +
                                "mod=" + Build.MODEL + "&" +
                                "sdk=" + Build.VERSION.SDK + "&" +
                                "ver=" + sPackInfo.versionName + "&" +
                                "bug=" + bug;

                URL url = new URL("http://mypace-engine.appspot.com/BugReportReceiver");


                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");

                con.setInstanceFollowRedirects(false);

                // データを送信する
                os = new DataOutputStream(con.getOutputStream());
                os.writeBytes(requestBody);
                os.flush();

                int responseCode = con.getResponseCode();
                String responseMessage = con.getResponseMessage();

                Log.d("Response Code:", String.format("%s[%d]", responseMessage, responseCode));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            try {
                if (os != null) {
                    os.close();
                }
                if (con != null) con.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String getFileBody(File file) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while((line = br.readLine()) != null) {
                sb.append(line).append("\r\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

}
