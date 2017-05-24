package com.mypaceengine.downloadnarrative;

import android.media.ExifInterface;
import android.util.Log;
import android.util.Xml;

import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.util.ServiceForbiddenException;

import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;

/**
 * Created by MypaceEngine on 2017/05/13.
 */

public class Job_UploadPhoto_Google extends Job_Google_Abstract implements Serializable {
    private static final long serialVersionUID = 000000000000000000001L;

    String tmpPath=null;
    String photoInfo=null;
    GSPContainer gpsData=null;
    public void setInfo(String _tmpPath,String _photoInfo,GSPContainer _gpsData){
        tmpPath=_tmpPath;
        photoInfo=_photoInfo;
        gpsData=_gpsData;
    }

    void run(){
        try {
            JSONObject photoObj=new JSONObject(photoInfo);
            String uuid_photo = photoObj.getString("uuid");
            double photo_quarity = photoObj.getDouble("quality_score");

            boolean favorite=photoObj.getBoolean("favorite");
            String takeTime = photoObj.getString("taken_at_local");
            Calendar cal = CnvUtil.cnvCalender(takeTime);

//            JSONObject photo_objs=photoObj.getJSONObject("renders");
            String format = ".jpg";
            Iterator ite = photoObj.getJSONObject("renders").keys();
            int width=0;
            int height=0;
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

            File tmpFile = new File(tmpPath);
/*            if (gpsData.gpsAvailable) {

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
            }*/
 //           SimpleDateFormat sdf_Album = new SimpleDateFormat("yyyyMMdd");
 //           SimpleDateFormat sdf_Date = new SimpleDateFormat("yyyy/MM/dd");
 //           String gAlbumName = "NarrativeClip_" + sdf_Album.format(cal.getTime());
            String gFileName = "NarrativeClip_Photo_" + uuid_photo;

            File movetoTarget = CnvUtil.cnvFilePath(service.getApplicationContext(), Conf.PhotoFolderName, cal, format);
            if (dataUtil.getEnableGoogleSync() && (!this.isAlreadyUpload(gFileName+format))) {
                String description =
                        "Narrative_UUID: " + uuid_photo + "\n" +
                                "Narrative_Moment_UUID: " + gpsData.uuid + "\n" +
                                "Narrative_Quality: " + photo_quarity + "\n" +
                                "Narrative_Address_Country: " + gpsData.getCountry() + "\n" +
                                "Narrative_Address_City: " + gpsData.getCity() + "\n" +
                                "Narrative_Address_Street: " + gpsData.getStreet() + "\n" +
                                "Narrative_Favorite: " + favorite + "\n";
                this.upload(gFileName, description, tmpFile, "image/jpeg", gpsData.getLatitude(), gpsData.getLongitude(), gpsData.gpsAvailable, cal,format);
            }

            Log.d("PhotodownLoad", "DataFilePath:" + movetoTarget);
            if (dataUtil.getEnableLocalSync() && (!movetoTarget.exists())) {
                tmpFile.renameTo(movetoTarget);
            }
            try {
                tmpFile.deleteOnExit();
            } catch (Exception ex) {
            }
            this.service.removeJob(this);
        }catch(ServiceForbiddenException e){
            GoogleUtil.invalidateToken();
        }catch(Exception e){e.printStackTrace();}
    }

}
