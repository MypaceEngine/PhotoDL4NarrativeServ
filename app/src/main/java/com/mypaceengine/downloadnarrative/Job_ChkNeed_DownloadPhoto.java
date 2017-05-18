package com.mypaceengine.downloadnarrative;

import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.util.ServiceForbiddenException;

import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;

/**
 * Created by MypaceEngine on 2017/05/13.
 */

public class Job_ChkNeed_DownloadPhoto extends Job_Google_Abstract implements Serializable {
    private static final long serialVersionUID = 000000000000000000001L;

    String photoInfo=null;
    GSPContainer gpsData;
    void setInfo(String _photoInfo,GSPContainer _gpsData){
        photoInfo=_photoInfo;
        gpsData=_gpsData;
    }

    void run(){
        boolean download=false;
        try {
            JSONObject photoObj = new JSONObject(photoInfo);
            String uuid_photo = photoObj.getString("uuid");
            String takeTime = photoObj.getString("taken_at_local");
            Calendar cal = CnvUtil.cnvCalender(takeTime);
            String format = ".jpg";
            Iterator ite = photoObj.getJSONObject("renders").keys();
            int width = 0;
            int height = 0;
            while (ite.hasNext()) {
                String size_key = (String) ite.next();
                JSONObject eachObj = photoObj.getJSONObject("renders").getJSONObject(size_key);
                JSONObject sizeObj = eachObj.getJSONObject("size");
                if (sizeObj != null) {
                    int photoWidth = sizeObj.getInt("width");
                    int photoHeight = sizeObj.getInt("height");
                    if (
                            (width < photoWidth) ||
                                    ((width == photoWidth) && (height < photoHeight))
                            ) {
                        if (eachObj.has("format")) {
                            format = "." + eachObj.getString("format");
                        }
                    }
                }
            }

            //           SimpleDateFormat sdf_Album = new SimpleDateFormat("yyyyMMdd");
            //           SimpleDateFormat sdf_Date = new SimpleDateFormat("yyyy/MM/dd");
            //           String gAlbumName = "NarrativeClip_" + sdf_Album.format(cal.getTime());
//            String gFileName = "NarrativeClip_Photo_" + uuid_photo;
            AlbumEntry album = null;
            if (this.dataUtil.getEnableGoogleSync()) {
                album = getTargetAlbum();
                download = !isAlreadyUpload(album.getGphotoId(), uuid_photo + format);
            }
            File movetoTarget = CnvUtil.cnvFilePath(service.getApplicationContext(), Conf.PhotoFolderName, cal, format);
            if (!download) {

                if (dataUtil.getEnableLocalSync()) {
                    download = !movetoTarget.exists();
                }
            }
            if (download) {
                Job_DownLoadPhoto job = new Job_DownLoadPhoto();
                job.setInfo(photoInfo, gpsData);
                service.addJobFirst(job);
            }
            service.removeJob(this);
        }catch(ServiceForbiddenException e){
            GoogleUtil.invalidateToken();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
}
