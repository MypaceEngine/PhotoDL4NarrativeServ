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

public abstract class Job_Google_Abstract extends AbstractJobN{

    private static final String API_PREFIX
            = "https://picasaweb.google.com/data/feed/api/user/default";
    public AlbumEntry getAlbumAndCreate(String name, String description) throws Exception {
        List<AlbumEntry> albums=GoogleUtil.getAlbum(GoogleUtil.getPicasaService(service));
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
            if (GoogleUtil.getPicasaService(service) != null) {
                result = GoogleUtil.getPicasaService(service).insert(new URL(API_PREFIX), result);
            }
            if (albums != null) {
                albums.add(result);
            }
        }
        return result;
    }
    List<AlbumEntry> albums=null;

    Map<String ,List<PhotoEntry>> photoMap=null;
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
    public void upload(String id, String name, String description, File file, String mediaType, double latitude, double longitude, boolean gpsEnable, Calendar cal)throws Exception {
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
                PhotoEntry photoEntry = GoogleUtil.getPicasaService(service).getEntry(new URL(photoUrl), PhotoEntry.class);
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
                    GoogleUtil.getPicasaService(service).getRequestFactory().setHeader("If-Match", "*");
                    photoEntry.setEtag(null);
                    photoEntry.update();
                }finally {
                    GoogleUtil.getPicasaService(service).getRequestFactory().setHeader("If-Match", null);
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
                PhotoEntry photoEntry = GoogleUtil.getPicasaService(service).getEntry(new URL(photoUrl), PhotoEntry.class);
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
            xpp = Xml.newPullParser();
            xpp.setInput(new StringReader(buf.toString()));


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
}
