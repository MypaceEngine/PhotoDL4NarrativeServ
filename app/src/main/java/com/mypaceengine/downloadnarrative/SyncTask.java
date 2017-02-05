package com.mypaceengine.downloadnarrative;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;

import com.google.common.io.Files;
import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.media.MediaFileSource;
import com.google.gdata.data.media.mediarss.MediaKeywords;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.AlbumFeed;
import com.google.gdata.data.photos.GphotoEntry;
import com.google.gdata.data.photos.GphotoFeed;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.data.photos.PhotoFeed;
import com.google.gdata.data.photos.UserFeed;
import com.google.gdata.util.ServiceException;
import com.google.gdata.util.ServiceForbiddenException;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.StatFs;
import android.util.Log;
import android.util.Xml;
import android.webkit.CookieManager;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.Collections;

/**
 * Created by MypaceEngine on 2016/09/04.
 */
public class SyncTask {

    SyncService service = null;

    static final int INITIALIZE = 0;
    static final int DOWNLOAD_SYNC = 1;

    int seeuence = INITIALIZE;
    int loadingID = 0;

    AccountManager manager =null;
    long startTime=-1;

    public long getStartTime(){
        return startTime;
    }

    public void initializeTask(SyncService _service) {
        service = _service;
        if(startTime==-1){
            startTime=System.currentTimeMillis();
        }
    }

    public void startExecution() {
        Log.d("MainServiceTask","Start");
        if(chkEnableTask()) {
            if (service.getEnableGoogleSync()&&(service.getGoogleAccount()!=null)) {
                Log.d("MainServiceTask","GoogleAuthToken");
                AccountManager manager = AccountManager.get(service);
                manager.getAuthToken(
                        new Account(service.getGoogleAccount(), "com.google"),
                        "lh2",            // Auth scope
                        null,                        // Authenticator-specific options
                        null,                           // Your activity
                        new OnTokenAcquired(),          // Callback called when a token is successfully acquired
                        null);    // Callback called if an error occ
            } else {
                Log.d("MainServiceTask","NotGoogle");
                doSyncronize();
            }
        }else {
            Log.d("MainServiceTask","GoNextTask");
            scheduleNextTask();
        }
    }

    public boolean isDoing(){
        return
                ((downLoadTask!=null)&&(downLoadTask.getStatus().equals(AsyncTask.Status.RUNNING)))
                                ||
                        ((downLoadTask!=null)&&(chkTask.getStatus().equals(AsyncTask.Status.RUNNING)));
    }

    ArrayList<JSONObject> momentArr;
//    ArrayList<PhotoListClass> photoListArr;
    ArrayList<JSONObject> videoArr;
    boolean allTaskCompleteFlg=false;
    int count_momentList=0;
//    int count_moment=0;
    int photo_count=0;
    int count_video=0;
    int parcent=0;

    AsyncTask downLoadTask;
    AsyncTask chkTask;
    public void doSyncronize() {
        downLoadTask=new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... voids) {

                try {
                    service.showNotification();

                    if (momentArr == null) {
                        momentArr = getMomentExec(false);
                    }
                    if(shutdownFlg){
                        return null;
                    }
                    service.showNotification();

                    if (videoArr == null) {
                        videoArr = getVideoExec(false);
                    }
                    if(shutdownFlg){
                        return null;
                    }
                    service.showNotification();
                    ArrayList<GSPContainer> gpsContainerList=new ArrayList<GSPContainer>();
                    if ((momentArr != null) && (momentArr.size() > 0) ) {
                        count_momentList=0;
                        photo_count=0;
 //                       ArrayList<PhotoListClass> _photoListArr = new ArrayList<PhotoListClass>();
                        for (JSONObject jsonObj : momentArr) {
                            JSONArray itemArray = jsonObj.getJSONArray("results");
                            parcent=0;
                            int count = itemArray.length();
                            for (int i = 0; i < count; i++) {
                                JSONObject obj = itemArray.getJSONObject(i);
  //                              PhotoListClass plc = new PhotoListClass();
 //                               plc.moment = obj;
                                String photos_url = obj.getString("photos_url");
                                ArrayList<JSONObject> photoList= getNarrativetExec(photos_url, null, false);

 //                               PhotoListClass moment = photoListArr.get(count_moment);
                                //                               JSONObject obj = moment.moment;
//                                ArrayList<JSONObject> photoList = moment.photoArr;
                                GSPContainer gpsData = cnvGPSContainer(obj);
                                gpsContainerList.add(gpsData);
                                for (JSONObject photoEle : photoList) {
                                    JSONArray itemArrayPhotoList = photoEle.getJSONArray("results");
 //                                   parcent=0;
                                    int countPhotoList = itemArrayPhotoList.length();
                                    for (int iPhotoList = 0; iPhotoList < countPhotoList; iPhotoList++) {
                                        JSONObject photo = itemArrayPhotoList.getJSONObject(iPhotoList);
                                        downLoadPhoto(photo, gpsData);
                                        photo_count++;
                                        service.showNotification();
                                        if (shutdownFlg) {
                                            return null;
                                        }
//                                        parcent=i*100/count;
//                                        service.showNotification();
                                    }
                                }

  //                              count_moment++;
                                //                            service.showNotification();
                                if(shutdownFlg){
                                    return null;
                                }
                                parcent=i*100/count;
                                service.showNotification();
                            }
                            count_momentList++;
                            service.showNotification();
                        }
  //                      photoListArr = _photoListArr;
                        service.showNotification();
                        Collections.sort(gpsContainerList);
                        Collections.reverse(gpsContainerList);
                    }

 //                   if ((photoListArr != null) && (photoListArr.size() > 0)) {
 //                       while (photoListArr.size() > count_moment) {

//                        }

//                    }
                    if((videoArr!=null)&&(videoArr.size()>0)){

                        while(videoArr.size()>count_video) {

                            JSONArray itemArray = videoArr.get(count_video).getJSONArray("results");
                            parcent=0;
                            int count = itemArray.length();
                            for (int i = 0; i < count; i++) {
                                JSONObject video = itemArray.getJSONObject(i);
                                downloadVideo(video,gpsContainerList);
                                if(shutdownFlg){
                                    return null;
                                }
                                parcent=i*100/count;
                                service.showNotification();
                            }
                            count_video++;
                            service.showNotification();
                        }
                    }
                    allTaskCompleteFlg=true;
                } catch (ServiceForbiddenException e) {
//                                LOG.error("Token expired, invalidating");
                    manager.invalidateAuthToken("com.google", googleauthToken);
                    startExecution();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ServiceException e) {
                    e.printStackTrace();
                }catch (Exception e) {
                    e.printStackTrace();
                }
                Log.d("DownLoadTask","End");
                return null;
            }
            protected void onPostExecute(Bitmap result) {

                Log.d("DownLoadTask","CallNextTask");
                if(shutdownFlg){
                    Log.d("DownLoadTask","Task is shutdowned!");
                }else{
                    scheduleNextTask();
                }

            }
        }.execute(null, null, null);
    }

    public GSPContainer cnvGPSContainer(JSONObject obj)throws Exception{
        String start_timestamp = obj.getString("start_timestamp_local");
        String end_timestamp = obj.getString("end_timestamp_local");
        String uuid_moment = obj.getString("uuid");

        String city = "";
        String street = "";
        String country = "";
        JSONObject address = obj.getJSONObject("address");
        if (address != null) {
            city = address.getString("city");
            street = address.getString("street");
            country = address.getString("country");
        }

        double lat = 0;
        double lon = 0;
        boolean positionFlg=false;
        try {
            JSONObject position = obj.getJSONObject("keyframe").getJSONObject("position");
            if (position != null) {
                if (position.has("lat")) {
                    lat = position.getDouble("lat");
                }
                if (position.has("lon")) {
                    lon = position
                            .getDouble("lon");
                    positionFlg=true;
                }
            }
        }catch (Exception ex){
            Log.d("LocationInformation","It is not Exist!");
        }
        return new GSPContainer(service,start_timestamp,end_timestamp,country,city,street,lat,lon,positionFlg,uuid_moment);
    }

    public void downLoadPhoto(JSONObject photo ,GSPContainer gpsData)throws Exception{
        String uuid_photo = photo.getString("uuid");
        double photo_quarity = photo.getDouble("quality_score");
        String takeTime = photo.getString("taken_at_local");
        boolean favorite=photo.getBoolean("favorite");
        Calendar cal=cnvCalender(takeTime);

        JSONObject photo_objs=photo.getJSONObject("renders");

        if(photo_objs!=null) {
            Iterator ite = photo_objs.keys();
            int width = 0;
            int height = 0;
            String photoUrl = null;
            String format = ".jpg";
            while (ite.hasNext()) {
                String size_key = (String) ite.next();
                JSONObject eachObj = photo_objs.getJSONObject(size_key);
                JSONObject sizeObj = eachObj.getJSONObject("size");
                if (sizeObj != null) {
                    int photoWidth = sizeObj.getInt("width");
                    int photoHeight = sizeObj.getInt("height");
                    if (
                            (width < photoWidth) ||
                                    ((width == photoWidth) && (height < photoHeight))
                            ) {
                        width = photoWidth;
                        height = photoHeight;
                        photoUrl = eachObj.getString("url");
                        if (eachObj.has("format")) {
                            format = "." + eachObj.getString("format");
                        }
                    }
                }
            }
            Log.d("SelectSize", "Width:" + width + " Height:" + height);

            if (photoUrl != null) {
                boolean download = false;
                SimpleDateFormat sdf_Album = new SimpleDateFormat("yyyyMMdd");
                SimpleDateFormat sdf_Date = new SimpleDateFormat("yyyy/MM/dd");
                String gAlbumName = "NarrativeClip_" + sdf_Album.format(cal.getTime());
                String gFileName = "NarrativeClip_Photo_" + uuid_photo;
                AlbumEntry album =null;
                if (service.getEnableGoogleSync()) {
                    album = getAlbumAndCreate(gAlbumName, sdf_Date.format(cal.getTime()));
                    download = !isAlreadyUpload(album.getGphotoId(), gFileName);
                }
                File movetoTarget = cnvFilePath(service.getApplicationContext(), Conf.PhotoFolderName, cal, format);
                if(!download) {

                    if (service.getEnableLocalSync()) {
                        download = !movetoTarget.exists();
                    }
                }
                if (download) {
                    File tmp_BASEDIR = service.getApplicationContext().getExternalFilesDir(Conf.TempolaryFolderName);
                    Files.createParentDirs(tmp_BASEDIR);
                    File tmpFile = new File(tmp_BASEDIR.getAbsolutePath() + File.separator + uuid_photo + format);
                    tmpFile.deleteOnExit();
                    Log.d("PhotodownLoad", "TmpFilePath:" + tmpFile);
                    saveNarrativeSrv2File(photoUrl, tmpFile);


                    if (gpsData.gpsAvailable) {
                        ExifInterface exifInterface = new ExifInterface(tmpFile.getAbsolutePath());
                        try {
                            String ns = "N";
                            String we = "E";

                            if (gpsData.getLatitude() < 0) {
                                ns = "S";
                            }
                            if (gpsData.getLongitude() < 0) {
                                we = "W";
                            }

                            exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE, latlong2GeoFormat(Math.abs(gpsData.getLatitude())));
                            exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, ns);
                            exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, latlong2GeoFormat(Math.abs(gpsData.getLongitude())));
                            exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, we);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        exifInterface.saveAttributes();
                    }

                    if (service.getEnableGoogleSync()&&(!this.isAlreadyUpload(album.getGphotoId(),gFileName))) {
                        String description=
                                "Narrative_UUID: "+ uuid_photo+"\n"+
                                        "Narrative_Moment_UUID: "+ gpsData.uuid+"\n"+
                                        "Narrative_Quality: " + photo_quarity+"\n"+
                                        "Narrative_Address_Country: "+ gpsData.getCountry()+"\n"+
                                        "Narrative_Address_City: "+ gpsData.getCity()+"\n"+
                                        "Narrative_Address_Street: "+ gpsData.getStreet()+"\n"+
                                        "Narrative_Favorite: " + favorite+"\n";
                        this.upload(album.getGphotoId(),gFileName,description,tmpFile,"image/jpeg",gpsData.getLatitude(),gpsData.getLongitude(),gpsData.gpsAvailable,cal);
                    }

                    Log.d("PhotodownLoad", "DataFilePath:" + movetoTarget);
                    if (service.getEnableLocalSync()&&(!movetoTarget.exists())) {
                        tmpFile.renameTo(movetoTarget);
                    }
                    try {
                        tmpFile.deleteOnExit();
                    } catch (Exception ex) {
                    }
                }
            }
        }
    }
    public void downloadVideo(JSONObject video,List<GSPContainer>gpsList)throws Exception{


        String uuid_video = video.getString("uuid");
        String startTime = video.getString("start_timestamp_local");
        String endTime = video.getString("end_timestamp_local");
        boolean favorite=video.getBoolean("favorite");
        String caption=video.getString("caption");

        Calendar cal=cnvCalender(startTime);

        GSPContainer con=null;
        for(int i=0;i<gpsList.size();i++){
            if(gpsList.get(i).isIncluded(cal)){
                con=gpsList.get(i);
                break;
            }else if (gpsList.get(i).isAfterEnd(cal)){
                con=gpsList.get(i);
                break;
            }else{
                con=gpsList.get(i);
            }
        }
        JSONObject video_objs = video.getJSONObject("renders");

        if (video_objs != null) {
            Iterator ite = video_objs.keys();
            int width = 0;
            int height = 0;
            String photoUrl = null;
            String format = ".mp4";
            while (ite.hasNext()) {
                String size_key = (String) ite.next();
                JSONObject eachObj = video_objs.getJSONObject(size_key);
                JSONObject sizeObj = eachObj.getJSONObject("size");

                if (sizeObj != null) {
                    int photoWidth = sizeObj.getInt("width");
                    int photoHeight = sizeObj.getInt("height");
                    if (
                            (width < photoWidth) ||
                                    ((width == photoWidth) && (height < photoHeight))
                            ) {
                        width = photoWidth;
                        height = photoHeight;
                        photoUrl = eachObj.getString("url");
                        if (eachObj.has("format")) {
                            format = "." + eachObj.getString("format");
                        }
                    }
                }
            }
            Log.d("SelectSize", "Width:" + width + " Height:" + height);
            String thumbnailUrl=video.getJSONArray("video_thumbs").getJSONObject(0).getJSONObject("renders").getJSONObject("g1_hd").getString("url");
            if(thumbnailUrl!=null) {
                Log.d("Thumbnail", thumbnailUrl);
            }

            if (photoUrl != null) {
                boolean download = false;
                SimpleDateFormat sdf_Album = new SimpleDateFormat("yyyyMMdd");
                SimpleDateFormat sdf_Date = new SimpleDateFormat("yyyy/MM/dd");
                String gAlbumName = "NarrativeClip_" + sdf_Album.format(cal.getTime());
                String gFileName = "NarrativeClip_Video_" + uuid_video;
                AlbumEntry album =null;
                if (service.getEnableGoogleSync()) {
                    album = getAlbumAndCreate(gAlbumName, sdf_Date.format(cal.getTime()));
                    download = !isAlreadyUpload(album.getGphotoId(), gFileName);
                }

                File movetoTarget = cnvFilePath(service.getApplicationContext(), Conf.PhotoFolderName, cal, format);
                if(!download) {

                    if (service.getEnableLocalSync()) {
                        download = !movetoTarget.exists();
                    }
                }

                if (download) {
                    File tmp_BASEDIR = service.getApplicationContext().getExternalFilesDir(Conf.TempolaryFolderName);
                    Files.createParentDirs(tmp_BASEDIR);
                    File tmpFile = new File(tmp_BASEDIR.getAbsolutePath() + File.separator + uuid_video + format);
                    tmpFile.deleteOnExit();
                    Log.d("PhotodownLoad", "TmpFilePath:" + tmpFile);
                    saveNarrativeSrv2File(photoUrl, tmpFile);


//                    File tmpThumbnailFile =null;
//                    if(thumbnailUrl!=null) {
//                        tmpThumbnailFile=new File(tmp_BASEDIR.getAbsolutePath() + File.separator + "thumbnail.jpg");
//                        saveNarrativeSrv2File(thumbnailUrl, tmpThumbnailFile);
//                    }
                    Log.d("PhotodownLoad", "DataFilePath:" + movetoTarget);
                    if (service.getEnableGoogleSync()&&(!this.isAlreadyUpload(album.getGphotoId(),gFileName))) {
                        double lat=0;
                        double lon=0;
                        boolean available=false;
                        String uuid="";
                        if(con!=null){
                            lat=con.getLatitude();
                            lon=con.getLongitude();
                            available=con.gpsAvailable;
                            uuid=con.uuid;
                        }
                        String description=
                                "Narrative_UUID: "+ uuid_video+"\n"+
                                        "Narrative_Moment_UUID: "+ uuid+"\n"+
                                        "Narrative_StartTime: " + startTime+"\n"+
                                        "Narrative_EndTime: "+ endTime+"\n"+
                                        "Narrative_Caption: "+ caption+"\n"+
                                        "Narrative_Favorite: " + favorite+"\n";

                        long millis1 = cal.getTimeInMillis();
                        tmpFile.setLastModified(millis1);
                        this.upload(album.getGphotoId(),gFileName,description,tmpFile,"video/mp4",lat,lon,available,cal);
                    }

                    Log.d("PhotodownLoad", "DataFilePath:" + movetoTarget);
                    if (service.getEnableLocalSync()&&(!movetoTarget.exists())) {
                        tmpFile.renameTo(movetoTarget);
                    }
                    try {
                        tmpFile.deleteOnExit();
                    } catch (Exception ex) {
                    }
                }
            }
        }
    }
    public static Calendar cnvCalender(String dateStr){
        String regex = "([0-9]+)-([0-9]+)-([0-9]+)T([0-9]+):([0-9]+):([0-9]+)";
        Pattern p = Pattern.compile(regex);
        String year = "";
        String month = "";
        String day = "";
        String hour = "";
        String min = "";
        String sec = "";
        Matcher m = p.matcher(dateStr);
        if (m.find()) {
            year = m.group(1);
            month = m.group(2);
            day = m.group(3);
            hour = m.group(4);
            min = m.group(5);
            sec = m.group(6);
        }
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day), Integer.parseInt(hour), Integer.parseInt(min),Integer.parseInt(sec));
        return calendar;
    }

    public static File cnvFilePath(Context context,String baseFolderName,Calendar cal,String format) throws Exception{

        SimpleDateFormat sdf_Year = new SimpleDateFormat("yyyy");
        SimpleDateFormat sdf_Month = new SimpleDateFormat("MM");
        SimpleDateFormat sdf_Day= new SimpleDateFormat("dd");
        SimpleDateFormat sdf_File= new SimpleDateFormat("hhmmss");

        File result=context.getExternalFilesDir(baseFolderName+ File.separator + sdf_Year.format(cal.getTime())+File.separator+sdf_Month.format(cal.getTime()) + File.separator + sdf_Day.format(cal.getTime()) + File.separator);
        Files.createParentDirs(result);
        return new File(result.getAbsolutePath()+File.separator+ sdf_File.format(cal.getTime()) + format);
    }


    public static String latlong2GeoFormat (double latlong) {
        // doubleからintへ変換
        Double _latlong = latlong;
        int num1 = _latlong.intValue();
        double num2d = ((_latlong - (double)num1) * 60);
        int num2 = (int)num2d;
        double num3d = ((num2d - (double)num2) * 60 * 100000);
        int num3 = (int)num3d;
        // フォーマット num1/denom1,num2/denom2,num3,denom3
        return String.format("%d/1,%d/1,%d/100000", num1, num2, num3);
    }

    public void scheduleNextTask() {
        service.registerNextTask();
    }
    boolean shutdownFlg=false;
    public void setShutdown(){
        shutdownFlg=true;
    }
    public boolean chkEnableTask(){
        Intent batteryInfo = service.registerReceiver(
                null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryInfo.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int max = batteryInfo.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        Log.d("Battery : ", "Charged = " + level + "   Max" + max);
        int batteryPercent=(level*100)/max;

        ConnectivityManager cm = (ConnectivityManager)service.getSystemService(service.CONNECTIVITY_SERVICE);
        NetworkInfo nInfo = cm.getActiveNetworkInfo();

        StatFs sf = new StatFs(service.getApplicationContext().getExternalFilesDir(Conf.TempolaryFolderName).getAbsolutePath());
        long kb = sf.getFreeBytes();
        Log.d("Storage Free Capacity",  kb+"byte");
        if(nInfo!=null)
        Log.d("Network", "Connected:"+nInfo.isConnected()+" TYPE:"+ nInfo.getTypeName()+" CELLER_AVAILABLE:"+service.getEnableCelSync());
        String key=service.getNarrativeKey();
        boolean reauthNeed=service.getNarrativeReauthNeed();
        Log.d("Narrative","ID:"+key+" ReauthNeed:"+ reauthNeed);
        reauthNeed=false;
        return
                (key!=null)&&
                        (!reauthNeed)&&
        (batteryPercent>0)&&
                        (nInfo!=null)&&(nInfo.isConnected())&&
                ((nInfo.getType()==ConnectivityManager.TYPE_WIFI)||service.getEnableCelSync())&&
                (kb>1024*1024*1024)&&
                        (service.getEnableLocalSync()||service.getEnableGoogleSync());
    }


    public ArrayList<JSONObject> getMomentExec(boolean onetime) throws Exception {
        Log.d("MainServiceTask","getMomentExec");
        return getNarrativetExec("https://narrativeapp.com/api/v2/moments/", null,onetime);
    }
    public ArrayList<JSONObject> getVideoExec(boolean onetime) throws Exception {
        Log.d("MainServiceTask","getVideoExec");
        return getNarrativetExec("https://narrativeapp.com/api/v2/videos/", null,onetime);
    }
    public ArrayList<JSONObject> getNarrativetExec(String urlStr, ArrayList<JSONObject> list,boolean onetime) throws Exception {
        Log.d("MainServiceTask","GetInfo_FromNarrative:"+urlStr);
        if (list == null) {
            list = new ArrayList<JSONObject>();
        }
        String seacret = service.getNarrativeKey();
        if (seacret != null) {
            HttpURLConnection con = null;
            InputStream in=null;
            InputStreamReader inReader=null;
            BufferedReader bufReader=null;
            StringBuffer buf = new StringBuffer();
            try {
// URLの作成
                URL url = new URL(urlStr);

// 接続用HttpURLConnectionオブジェクト作成
                con = (HttpURLConnection) url.openConnection();
                ArrayList<String> cookie_list=service.getNarrativeCookie();
                for(String cookie:cookie_list){
                    CookieManager.getInstance().setCookie(WebActivity.DOMAIN,cookie);
                }

                String cookie = CookieManager.getInstance().getCookie(WebActivity.DOMAIN);

                con.addRequestProperty("Cookie", cookie);

// リクエストメソッドの設定
                con.setRequestMethod("GET");
// リダイレクトを自動で許可しない設定
                con.setInstanceFollowRedirects(true);
// ヘッダーの設定(複数設定可能)
                con.setRequestProperty("Authorization", "Bearer " + seacret);
// 接続
                con.connect();

                int responseCode = con.getResponseCode();
                String responseMessage = con.getResponseMessage();

                Log.d("Narrative Responce", String.format("%s[%d]", responseMessage, responseCode));

                switch (responseCode){
                    case HttpURLConnection.HTTP_UNAUTHORIZED:
                        //TODO handle unauthorized
                        service.setNarrativeReauthNeed(true);
                        throw new Exception();
 //                       return null;
 //                   case 503:
 //                       //TODO handle unauthorized
 //                       service.setNarrativeReauthNeed(true);
 //                       return null;
                    default:
                }
                in = con.getInputStream();
                // HTMLソースを読み出す
                // String src = new String();

/*                byte[] line = new byte[1024];
                int size;
                while (true) {
                    size = in.read(line);
                    if (size <= 0) {
                        break;
                    }
                    buf.append(new String(line,"UTF-8"));
                }*/
                inReader = new InputStreamReader(in);
                bufReader = new BufferedReader(inReader);
                String line = null;
                // 1行ずつテキストを読み込む
                while((line = bufReader.readLine()) != null) {
                    buf.append(line);
                }
                bufReader.close();
            }catch(Exception ex){
                if(!onetime) {
                  /*  try{
                        Thread.sleep(3000);
                    }catch (Exception ex2){}*/
                    getNarrativetExec(urlStr, list, onetime);
                }else {
                    throw ex;
                }
            }finally{
                if(bufReader!=null){
                    bufReader.close();
                }
                if(inReader!=null){
                    inReader.close();
                }

                if(con!=null){
                    con.disconnect();
                }
                if(in!=null){
                    in.close();
                }
            }
            String jsonStr=buf.toString();
            while(true){
                int c = jsonStr.charAt(0);
                if(c==-1){
                    jsonStr=jsonStr.substring(1);
                }else{
                    break;
                }
            }

            Log.d("XMLDEBUG",jsonStr);
            JSONObject json = new JSONObject(jsonStr);
            list.add(json);
            service.showNotification();
            String nextUrlStr=json.getString("next");
            if((!onetime)&&(nextUrlStr!=null)&&(nextUrlStr.length()>0)&&(!urlStr.equals(nextUrlStr))&&(!nextUrlStr.equals("null"))){
                Log.d("Next","URL:"+nextUrlStr);
                //Thread.sleep(3000);
                list = getNarrativetExec(nextUrlStr, list,onetime);
            }
        }
        return list;
    }
    public void saveNarrativeSrv2File(String urlStr, File file) throws Exception {
        Log.d("MainServiceTask","saveNarrativeSrv2File:"+urlStr);
        String seacret = service.getNarrativeKey();
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
                        service.setNarrativeReauthNeed(true);
                        return ;
                    default:
                }
                in = con.getInputStream();

//                fileOutputstream = service.openFileOutput(file.getAbsolutePath(), Context.MODE_PRIVATE);
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
     /*           try{
                    Thread.sleep(3000);
                }catch (Exception ex2){}*/
                saveNarrativeSrv2File(urlStr, file);
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
    private static final String API_PREFIX
            = "https://picasaweb.google.com/data/feed/api/user/default";
    public AlbumEntry getAlbumAndCreate(String name,String description) throws Exception {
        List<AlbumEntry> albums=getAlbum(picasaService);
        AlbumEntry result=null;
        if(albums!=null) {
            for (AlbumEntry entry : albums) {
                if(name.equals(entry.getTitle().getPlainText())){
                    result=entry;
                    break;
                }
            }
        }
        if(result==null) {
            result = new AlbumEntry();
            result.setName(name);
            result.setTitle(new PlainTextConstruct(name));
            result.setDescription(new PlainTextConstruct(description));
            if (picasaService != null) {
                result = picasaService.insert(new URL(API_PREFIX), result);
            }
            if (albums != null) {
                albums.add(result);
            }
        }
        return result;
    }
    List<AlbumEntry> albums=null;
    public List<AlbumEntry> getAlbum(PicasawebService _picasaService) throws IOException,ServiceException
    {
        if(albums==null) {
            URL feedUrl = new URL("https://picasaweb.google.com/data/feed/api/user/default/");
            UserFeed myUserFeed = picasaService.getFeed(feedUrl, UserFeed.class);
            if(albums==null){
                albums=new ArrayList<AlbumEntry>();
            }
            List<GphotoEntry> albumG=myUserFeed.getEntries();
            for (GphotoEntry entry:albumG){
                albums.add(new AlbumEntry(entry));
            }
        }
        return albums;
    }
    Map<String ,List<PhotoEntry>> photoMap=null;
    public List<PhotoEntry> getPhotoList(String id)throws  Exception{
        List<PhotoEntry> result=null;
        if(photoMap!=null){
            result=photoMap.get(id);
        }
        if(result==null) {
            result=new ArrayList<PhotoEntry>();
            String feedUrl = "https://picasaweb.google.com/data/feed/api/user/default/albumid/" + id;

            if (picasaService != null) {
                Log.d("FeedURL", feedUrl);
                AlbumFeed feed = picasaService.getFeed(new URL(feedUrl), AlbumFeed.class);
                List<GphotoEntry> resultpre = feed.getEntries();
                for(GphotoEntry entry:resultpre){
                    result.add(new PhotoEntry(entry));
                }
                if(photoMap==null){
                    photoMap=new HashMap<String ,List<PhotoEntry>>();
                }
                photoMap.put(id,result);
            }
        }
        return result;
    }

    public boolean isAlreadyUpload(String albumID,String name)throws Exception{
        boolean result=false;
        List<PhotoEntry> list=getPhotoList(albumID);
        if(list!=null){
            for(PhotoEntry entry:list){
                if(name.equals(entry.getTitle().getPlainText())){
                    result=true;
                    break;
                }
            }
        }
        return result;
    }
    public void upload(String id,String name,String description,File file,String mediaType,double latitude,double longitude,boolean gpsEnable,Calendar cal)throws Exception {
        if (!isAlreadyUpload(id, name)) {
        /*    URL albumPostUrl = new URL("https://picasaweb.google.com/data/feed/api/user/default/albumid/"+id);

            PhotoEntry myPhoto = new PhotoEntry();
            myPhoto.setTitle(new PlainTextConstruct(name));
            myPhoto.setDescription(new PlainTextConstruct(description));
            if(gpsEnable){
                myPhoto.setGeoLocation(latitude,longitude);
            }

            MediaFileSource myMedia = new MediaFileSource(file, mediaType);
            myPhoto.setMediaSource(myMedia);
            if(picasaService!=null) {
                PhotoEntry returnedPhoto = picasaService.insert(albumPostUrl, myPhoto);
            }
            */
            boolean photoidFlg = false;
            boolean albumidFlag = false;
            boolean photourlFlg = false;
            String photoid = null;
            String albumid = null;
            String photoUrl = null;
            if ("video/mp4".equals(mediaType)) {
                photoUrl = uploadVideo(file);
//                String defalutID = getAlbum(picasaService).get(0).getId();
//                photoUrl = "https://picasaweb.google.com/data/feed/api/user/default/albumid/" + defalutID + "/photoid/" + videoID;
//                PhotoFeed photoEntry = picasaService.getFeed(new URL(photoUrl), PhotoFeed.class);
                PhotoEntry photoEntry = picasaService.getEntry(new URL(photoUrl), PhotoEntry.class);
                photoEntry.setTitle(new PlainTextConstruct(name));
                photoEntry.setDescription(new PlainTextConstruct(description));
                photoEntry.setAlbumId(id);
                photoEntry.setTimestamp(cal.getTime());
                if (gpsEnable) {
                    photoEntry.setGeoLocation(latitude, longitude);
                }
//                List<GphotoEntry> list=photoEntry.getEntries();
//                if(list!=null && list.size()>0){

//                }
                try {
                    picasaService.getRequestFactory().setHeader("If-Match", "*");
                    photoEntry.setEtag(null);
                    photoEntry.update();
                }finally {
                    picasaService.getRequestFactory().setHeader("If-Match", null);
                }

            } else {
                XmlPullParser xpp = addPhoto(id, file, mediaType);
                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        if (xpp.getName().equals("id") && ("gphoto".equals(xpp.getPrefix()))) {
                            photoidFlg = true;
                        } else if (xpp.getName().equals("albumid") && ("gphoto".equals(xpp.getPrefix()))) {
                            albumidFlag = true;
                        } else if (xpp.getName().equals("id")) {

                            photourlFlg = true;
                        }
//                    Log.d("NameSpace",xpp.getNamespace());
//                    Log.d("Name",xpp.getName());
//                    if((xpp.getPrefix())!=null){
//                        Log.d("NamePrefix", xpp.getPrefix());
//                    }
                    } else if (eventType == XmlPullParser.TEXT) {
                        if (photoidFlg) {
                            photoid = xpp.getText();
                            photoidFlg = false;
                        } else if (albumidFlag) {
                            albumid = xpp.getText();
                            albumidFlag = false;
                        } else if (photourlFlg) {
                            photoUrl = xpp.getText();
                            photourlFlg = false;
                        }
                    }
                    if ((photoid != null) && (albumid != null) && (photoUrl != null)) {
                        break;
                    }
                    eventType = xpp.next();
                }
                PhotoEntry photoEntry = picasaService.getEntry(new URL(photoUrl), PhotoEntry.class);
                photoEntry.setTitle(new PlainTextConstruct(name));
                photoEntry.setDescription(new PlainTextConstruct(description));
                photoEntry.setAlbumId(id);
                photoEntry.setTimestamp(cal.getTime());
                if (gpsEnable) {
                    photoEntry.setGeoLocation(latitude, longitude);
                }
                photoEntry.update();
            }

        }
    }
//            if("video/mp4".equals(mediaType)) {
//                if(thumbnailFile!=null) {
//                    MediaFileSource myThumb = new MediaFileSource(thumbnailFile, "image/jpeg");
//                    photoEntry.setMediaSource(myThumb);
//                    photoEntry.setEtag("Narrative_Sumbnail");
//                    photoEntry = photoEntry.updateMedia(true);
 //               }
//                photoEntry.updateMedia(false);
//                String videoStatus = null;
//                while ((videoStatus = photoEntry.getVideoStatus()).equals("pending")) {
//                    if (videoStatus != null)
//                        Log.d("VideoUpdateStatus", "" + videoStatus);
            //        Thread.sleep(1000);
//                }

//            }
//            photoEntry.update();
//        }


    private XmlPullParser addPhoto(String id, File image, String type) throws Exception {
        XmlPullParser xpp=null;
        OutputStream os=null;
        try {
            if (!image.canRead()) {
                System.err.println("File read error.");
                System.exit(0);
            }
            String albumPostUrl = "https://picasaweb.google.com/data/feed/api/user/default/albumid/" + id;
            Log.d("upload","URL:"+image.getAbsolutePath());
            // POST header
            HttpURLConnection con = (HttpURLConnection)
                    new URL(albumPostUrl).openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", "GoogleLogin auth=" + googleauthToken);
//        con.setRequestProperty("Content-type", type);
            con.setRequestProperty("Content-Type", "multipart/related; boundary=\"END_OF_PART\"");
//        con.setRequestProperty("Content-Length",""+image.length()+385);
            con.setRequestProperty("MIME-version", "1.0");

            con.setRequestProperty("Slug", image.getName());

            // POST body (copied from file stream)
            int i;
            byte[] buff = new byte[8192];
            FileInputStream fis = new FileInputStream(image);
            os = con.getOutputStream();
            os.write("Media multipart posting\r\n".getBytes());
            os.write("--END_OF_PART\r\n".getBytes());
            os.write("Content-Type: application/atom+xml\r\n\r\n".getBytes());

            os.write("<entry xmlns='http://www.w3.org/2005/Atom'>\r\n".getBytes());
//        os.write("<title>plz-to-love-realcat.jpg</title>\r\n".getBytes());
//        os.write("<summary>Real cat wants attention too.</summary>\r\n".getBytes());
            os.write("<category scheme=\"http://schemas.google.com/g/2005#kind\"\r\n".getBytes());
            os.write("term=\"http://schemas.google.com/photos/2007#photo\"/>\r\n".getBytes());
            os.write("</entry>\r\n".getBytes());
            os.write("--END_OF_PART\r\n".getBytes());
            os.write(("Content-Type: " + type + "\r\n\r\n").getBytes());


            while ((i = fis.read(buff)) > 0)
                os.write(buff, 0, i);
//        os.write(buff, 0, i);

            os.write("\r\n--END_OF_PART--".getBytes());
            os.flush();
            os.close();
            Log.d("ResponseCode",""+con.getResponseCode());




        // Read response (still needed)
        String text = null;
        BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        StringBuilder buf = new StringBuilder();
        while ((text = br.readLine()) != null) {
      //      Log.d("Data:", text);
            buf.append(text);
        }
            xpp = Xml.newPullParser();
        xpp.setInput(new StringReader (buf.toString()));


        br.close();
        }catch (Exception ex ){
            ex.printStackTrace();
/*            try{
                Thread.sleep(3000);
            }catch(Exception ex2){}：
            */
            xpp=addPhoto(id, image, type);
        }
        return xpp;
    }
    public String uploadVideo(File file) throws Exception {
        try {
            String sessionUrl = "https://photos.google.com/_/upload/photos/resumable?authuser=0";
            // POST header
            HttpURLConnection con = (HttpURLConnection)
                    new URL(sessionUrl).openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", "GoogleLogin auth=" + googleauthToken);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

            String body =
                    "{\"protocolVersion\":\"0.8\",\"createSessionRequest\":{\"fields\":[{\"external\":{\"name\":\"file\",\"filename\":\"" +
                            file.getName() +
                            "\",\"put\":{},\"size\":" +
                            file.length() +
                            "}},{\"inlined\":{\"name\":\"auto_create_album\",\"content\":\"camera_sync.active\",\"contentType\":\"text/plain\"}}," +
                            "{\"inlined\":{\"name\":\"auto_downsize\",\"content\":\"true\",\"contentType\":\"text/plain\"}}," +
                            "{\"inlined\":{\"name\":\"storage_policy\",\"content\":\"use_manual_setting\",\"contentType\":\"text/plain\"}}," +
                            "{\"inlined\":{\"name\":\"disable_asbe_notification\",\"content\":\"true\",\"contentType\":\"text/plain\"}}," +
                            "{\"inlined\":{\"name\":\"client\",\"content\":\"photosweb\",\"contentType\":\"text/plain\"}}," +
                            "]}}";
            OutputStream os = con.getOutputStream();
            os.write(body.getBytes());
            os.flush();
            os.close();


            Log.d("ResponseCode", "" + con.getResponseCode());

            // Read response (still needed)
            String text = null;
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            StringBuilder buf = new StringBuilder();
            while ((text = br.readLine()) != null) {
                //      Log.d("Data:", text);
                buf.append(text);
            }
            br.close();

            Log.d("ResponseBody", "" + buf.toString());

            String uploadUrl = con.getHeaderField("location");
            String uploadID = con.getHeaderField("x-guploader-uploadid");

            // POST header
            HttpURLConnection con2 = (HttpURLConnection)
                    new URL(uploadUrl).openConnection();
            con2.setDoInput(true);
            con2.setDoOutput(true);
            con2.setRequestMethod("POST");
            con2.setRequestProperty("Authorization", "GoogleLogin auth=" + googleauthToken);
            con2.setRequestProperty("Content-Type", "application/octet-stream");

            int i;
            byte[] buff = new byte[8192];
            FileInputStream fis = new FileInputStream(file);
            OutputStream os2 = con2.getOutputStream();

            while ((i = fis.read(buff)) > 0)
                os2.write(buff, 0, i);
            os2.flush();
            os2.close();

            Log.d("ResponseCode", "" + con.getResponseCode());

            // Read response (still needed)
            String text2 = null;
            BufferedReader br2 = new BufferedReader(
                    new InputStreamReader(con2.getInputStream()));
            StringBuilder buf2 = new StringBuilder();
            while ((text2 = br2.readLine()) != null) {
                buf2.append(text2);
            }
            Log.d("ResponseBody", "" + buf2.toString());
            JSONObject json = new JSONObject(buf2.toString());
            JSONObject addInfo = json.getJSONObject("sessionStatus").getJSONObject("additionalInfo").getJSONObject("uploader_service.GoogleRupioAdditionalInfo").getJSONObject("completionInfo").getJSONObject("customerSpecificInfo");
            String albumID = addInfo.getString("albumid");
            String photoID = addInfo.getString("photoid");
            String videoID = addInfo.getString("videoId");

//        return "https://picasaweb.google.com/data/feed/api/user/default/albumid/" + albumID + "/photoid/" + photoID;
            return "https://picasaweb.google.com/data/entry/api/user/default/albumid/" + albumID + "/photoid/" + photoID;
        } catch (Exception ex) {
            ex.printStackTrace();
          /*  try {
                Thread.sleep(3000);
            } catch (Exception ex2) {
            }*/
            return uploadVideo(file);
        }
    }


    public <T extends GphotoFeed> T getFeed(PicasawebService _picasaService,String feedHref,
                                            Class<T> feedClass) throws IOException, ServiceException {
//        LOG.debug("Get Feed URL: " + feedHref);
        return picasaService.getFeed(new URL(feedHref), feedClass);
    }
    PicasawebService picasaService;
    String googleAccountName=null;
    String googleauthToken=null;
    private class OnTokenAcquired implements AccountManagerCallback<Bundle> {
        @Override
        public void run(AccountManagerFuture<Bundle> result) {
            try {
                Bundle bundle = result.getResult();

                if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN)) {
                    googleAccountName = bundle.getString(AccountManager.KEY_ACCOUNT_NAME);
                    googleauthToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
//                    LOG.debug("Auth token {}", authToken);
                    picasaService = new PicasawebService(service.getPackageName());
                    picasaService.setUserToken(googleauthToken);

                    chkTask=new AsyncTask<Void, Void, Bitmap>() {
                        @Override
                        protected Bitmap doInBackground(Void... voids) {

                            try {
                                getAlbum(picasaService);
                                doSyncronize();
                            } catch (ServiceForbiddenException e) {
//                                LOG.error("Token expired, invalidating");
                                manager.invalidateAuthToken("com.google", googleauthToken);
                                startExecution();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (ServiceException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                        protected void onPostExecute(Bitmap result) {

                        }
                    }.execute(null, null, null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private class PhotoListClass{
        JSONObject moment;
         ArrayList<JSONObject> photoArr;
    }
}
