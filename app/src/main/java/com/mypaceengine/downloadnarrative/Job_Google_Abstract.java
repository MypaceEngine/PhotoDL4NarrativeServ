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

    private static final String API_PREFIX
            = "https://picasaweb.google.com/data/feed/api/user/default";
    static  AlbumEntry targetAlbum=null;
    public AlbumEntry getTargetAlbum() throws Exception {
        if(targetAlbum!=null){
            return targetAlbum;
        }
        List<AlbumEntry> albums=GoogleUtil.getAlbum(GoogleUtil.getPicasaService(service));
        if(albums!=null) {
            if(albums.size()>0){
                targetAlbum=albums.get(0);
            }
            for (AlbumEntry entry : albums) {
                if("Auto Backup".equals(entry.getTitle().getPlainText())){
                    targetAlbum=entry;
                    break;
                }
            }
        }
        return targetAlbum;
    }
    List<AlbumEntry> albums=null;

    static Map<String ,List<PhotoEntry>> photoMap=null;
    public List<PhotoEntry> getPhotoList(String id)throws  Exception{
        List<PhotoEntry> result=null;
        if(photoMap!=null){
            result=photoMap.get(id);
        }
        if(result==null) {
            result=new ArrayList<PhotoEntry>();
            String feedUrl = "https://picasaweb.google.com/data/feed/api/user/default/albumid/" + id;

            if (GoogleUtil.getPicasaService(service) != null) {
                Log.d("FeedURL", feedUrl);
                AlbumFeed feed = GoogleUtil.getPicasaService(service).getFeed(new URL(feedUrl), AlbumFeed.class);
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
    public void upload(String id, String name, String description, File file, String mediaType, double latitude, double longitude, boolean gpsEnable, Calendar cal,String format)throws Exception {
        if (!isAlreadyUpload(id, name+format)) {
            String photoUrl = null;
           if ("video/mp4".equals(mediaType)) {
               uploadVideo(file);
            } else {
                addPhoto(id, file, mediaType);
            }
        }
    }

    private void addPhoto(String id, File image, String type) throws Exception {
        OutputStream os=null;
            if (!image.canRead()) {
                System.err.println("File read error.");
                System.exit(0);
            }
            String albumPostUrl = "https://picasaweb.google.com/data/feed/api/user/default/albumid/" + id;
            Log.d("upload","URL:"+albumPostUrl+" BaseFile:"+image.getAbsolutePath());
            // POST header
            con = (HttpURLConnection)
                    new URL(albumPostUrl).openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", "GoogleLogin auth=" + GoogleUtil.getPicasaToken(service));
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
}
