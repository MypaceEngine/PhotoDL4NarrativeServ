package com.mypaceengine.downloadnarrative;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;

import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.media.MediaFileSource;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.AlbumFeed;
import com.google.gdata.data.photos.GphotoEntry;
import com.google.gdata.data.photos.GphotoFeed;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.data.photos.UserFeed;
import com.google.gdata.util.ServiceException;
import com.google.gdata.util.ServiceForbiddenException;

import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by MypaceEngine on 2017/05/13.
 */

public abstract class Job_Google_Abstract extends AbstractJobN implements Serializable {


    public boolean isAlreadyUpload(String name)throws Exception{
        return dataUtil.loadBooleanHistory(name);
    }

    public <T extends GphotoFeed> T getFeed(PicasawebService _picasaService, String feedHref,
                                            Class<T> feedClass) throws IOException, ServiceException {
//        LOG.debug("Get Feed URL: " + feedHref);
        return GoogleUtil.getPicasaService(service).getFeed(new URL(feedHref), feedClass);
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
    public void upload(String uuid,String name, String description, File file, String mediaType, double latitude, double longitude, boolean gpsEnable, Calendar cal,String format)throws Exception {
        if (!isAlreadyUpload(uuid+format)) {
            String photoUrl = null;
           if ("video/mp4".equals(mediaType)) {
               uploadVideo(file);
            } else {
                addPhoto(file, mediaType,name,description, latitude,  longitude,  gpsEnable,  cal);
            }
            dataUtil.saveBooleanHistory(uuid+format,true);
        }
    }

    private void addPhoto( File image, String type,String name,String description,double latitude, double longitude, boolean gpsEnable, Calendar cal) throws Exception {
        OutputStream os=null;
/*            if (!image.canRead()) {
                System.err.println("File read error.");
                System.exit(0);
            }*/

//            String albumPostUrl = "https://picasaweb.google.com/data/feed/api/user/default/albumid/" + id;
//            String albumPostUrl = "https://picasaweb.google.com/data/feed/api/user/default/albumid/default";
        String albumPostUrl =getAlbumURL();
        if(albumPostUrl==null){
            throw new Exception();
        }
            Log.d("upload","URL:"+albumPostUrl+" BaseFile:"+image.getAbsolutePath());
 /*       PhotoEntry myPhoto = new PhotoEntry();
        myPhoto.setTitle(new PlainTextConstruct(name));
       myPhoto.setDescription(new PlainTextConstruct(description));
        if(gpsEnable) {
            myPhoto.setGeoLocation(latitude,longitude);
        }
        myPhoto.setTimestamp(cal.getTime());
//        myPhoto.setClient("myClientName");

        MediaFileSource myMedia = new MediaFileSource(image, type);
        myPhoto.setMediaSource(myMedia);

        PhotoEntry returnedPhoto = GoogleUtil.getPicasaService(service).insert(new URL(albumPostUrl), myPhoto);*/
        FileInputStream fis = new FileInputStream(image);
        int i;
        byte[] buff = new byte[8192];
        ByteArrayOutputStream outBuf=new ByteArrayOutputStream();

        outBuf.write("Media multipart posting\r\n".getBytes());
        outBuf.write("--END_OF_PART\r\n".getBytes());
        outBuf.write("Content-Type: application/atom+xml\r\n\r\n".getBytes());
        outBuf.write("<entry xmlns='http://www.w3.org/2005/Atom'>\r\n".getBytes());
        outBuf.write(("<title>"+name+"</title>\r\n").getBytes());
        outBuf.write(("<summary>"+description+"</summary>\r\n").getBytes());
        outBuf.write("<category scheme=\"http://schemas.google.com/g/2005#kind\"\r\n".getBytes());
        outBuf.write("term=\"http://schemas.google.com/photos/2007#photo\"/>\r\n".getBytes());
        outBuf.write("</entry>\r\n".getBytes());
        outBuf.write("--END_OF_PART\r\n".getBytes());
        outBuf.write(("Content-Type: " + type + "\r\n\r\n").getBytes());


        while ((i = fis.read(buff)) > 0)
            outBuf.write(buff, 0, i);
        //        os.write(buff, 0, i);

        outBuf.write("\r\n--END_OF_PART--".getBytes());

        byte[] data=outBuf.toByteArray();

            // POST header
            con = (HttpURLConnection)
                    new URL(albumPostUrl).openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", "GoogleLogin auth=" + GoogleUtil.getPicasaToken(service));
//        con.setRequestProperty("Content-type", type);
            con.setRequestProperty("Content-Type", "multipart/related; boundary=\"END_OF_PART\"");
            con.setRequestProperty("Content-Length",""+data.length);
            con.setRequestProperty("MIME-version", "1.0");

//            con.setRequestProperty("Slug", image.getName());

            // POST body (copied from file stream)

            os = con.getOutputStream();

            os.write(data);

            os.flush();
            os.close();
            Log.d("ResponseCode",""+con.getResponseCode());

            fis.close();
            outBuf.close();


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

    }
    HttpURLConnection con=null;
    public void uploadVideo(File file) throws Exception {
            String sessionUrl = "https://photos.google.com/_/upload/photos/resumable?authuser=0";
            Log.d("upload","URL:"+sessionUrl+" BaseFile:"+file.getAbsolutePath());
            // POST header
            con = (HttpURLConnection)
                    new URL(sessionUrl).openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", "GoogleLogin auth=" + GoogleUtil.getPicasaToken(service));
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
            con2.setRequestProperty("Authorization", "GoogleLogin auth=" + GoogleUtil.getPicasaToken(service));
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
    }
    static String albumURL=null;
    public String getAlbumURL(){
        try {
            if(albumURL==null) {
                List<AlbumEntry> list = GoogleUtil.getAlbum(GoogleUtil.getPicasaService(service));
                albumURL=list.get(0).getId();
                albumURL=albumURL.replace("entry","feed/api");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return albumURL;
    }
}
