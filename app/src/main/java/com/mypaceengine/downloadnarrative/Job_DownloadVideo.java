package com.mypaceengine.downloadnarrative;

import android.util.Log;

import com.google.common.io.Files;
import com.google.gdata.data.photos.AlbumEntry;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

/**
 * Created by MypaceEngine on 2017/05/13.
 */

public class Job_DownloadVideo extends Job_Download_Abstract implements Serializable {
    private static final long serialVersionUID = 000000000000000000001L;

    String videoInfo=null;
    void setInfo(String _videoInfo){
        videoInfo=_videoInfo;
    }
    void run(){
        boolean flag = true;
        try {
            List<GSPContainer> gpsList=service.getGPSList();
            JSONObject videoObj = new JSONObject(videoInfo);
            downloadVideo(videoObj, gpsList);
            this.service.removeJob(this);
        }catch (Exception ex){
            ex.printStackTrace();
            flag=false;
        }
        if ((flag) && (!shutdownFlg)) {
            this.service.removeJob(this);
        }
    }

    public void downloadVideo(JSONObject video,List<GSPContainer> gpsList)throws Exception{


        String uuid_video = video.getString("uuid");
        String startTime = video.getString("start_timestamp_local");
        String endTime = video.getString("end_timestamp_local");
        boolean favorite=video.getBoolean("favorite");
        String caption=video.getString("caption");

        Calendar cal=CnvUtil.cnvCalender(startTime);

        service.showExecutingNotification_DownLoad(cal);

        GSPContainer gpsData=null;
        for(int i=0;i<gpsList.size();i++){
            if(gpsList.get(i).isIncluded(cal)){
                gpsData=gpsList.get(i);
                break;
            }else if (gpsList.get(i).isAfterEnd(cal)){
                gpsData=gpsList.get(i);
                break;
            }else{
                gpsData=gpsList.get(i);
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
                    File tmp_BASEDIR = service.getApplicationContext().getExternalFilesDir(Conf.TempolaryFolderName);
                    Files.createParentDirs(tmp_BASEDIR);
                    File tmpFile = new File(tmp_BASEDIR.getAbsolutePath() + File.separator + uuid_video + format);
                    tmpFile.delete();
                do{
                    Thread.sleep(50);
                }while(tmpFile.exists());
                    Log.d("PhotodownLoad", "TmpFilePath:" + tmpFile);
                    saveNarrativeSrv2File(photoUrl, tmpFile);

                tmpFile.setLastModified(cal.getTimeInMillis());

                Job_UploadVideo_Google job=new Job_UploadVideo_Google();
                job.setInfo(tmpFile.getAbsolutePath(),videoInfo,gpsData);
                this.service.addJobFirst(job);

            }
        }
    }

}