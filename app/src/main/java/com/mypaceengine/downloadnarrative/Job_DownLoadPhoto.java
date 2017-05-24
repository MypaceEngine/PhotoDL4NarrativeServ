package com.mypaceengine.downloadnarrative;

import android.media.ExifInterface;
import android.util.Log;

import com.google.common.io.Files;
import com.google.gdata.data.photos.AlbumEntry;

import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.TimeZone;

/**
 * Created by MypaceEngine on 2017/05/13.
 */

public class Job_DownLoadPhoto extends Job_Download_Abstract  implements Serializable {
    private static final long serialVersionUID = 000000000000000000001L;

    String photoInfo;
    GSPContainer gpsData;

    void setInfo(String _photoInfo, GSPContainer _gpsData) {
        photoInfo = _photoInfo;
        gpsData = _gpsData;
    }

    void run() {
        boolean flag = true;
        try {
            JSONObject photoObj = new JSONObject(photoInfo);
            downLoadPhoto(photoObj, gpsData);
        } catch (Exception ex) {
            ex.printStackTrace();
            flag = false;
        }
        if ((flag) && (!shutdownFlg)) {
            this.service.removeJob(this);
        }
    }

    public void downLoadPhoto(JSONObject photo, GSPContainer gpsData) throws Exception {
        String uuid_photo = photo.getString("uuid");
        double photo_quarity = photo.getDouble("quality_score");

        boolean favorite = photo.getBoolean("favorite");

        JSONObject photo_objs = photo.getJSONObject("renders");
        String takeTime = photo.getString("taken_at_local");
        Calendar cal = CnvUtil.cnvCalender(takeTime);

        if (photo_objs != null) {
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

                File tmp_BASEDIR = service.getApplicationContext().getExternalFilesDir(Conf.TempolaryFolderName);
                Files.createParentDirs(tmp_BASEDIR);
                File tmpFile = new File(tmp_BASEDIR.getAbsolutePath() + File.separator + uuid_photo + format);
                tmpFile.deleteOnExit();
                Log.d("PhotodownLoad", "TmpFilePath:" + tmpFile);
                saveNarrativeSrv2File(photoUrl, tmpFile);
                String description =
                "Narrative_UUID: " + uuid_photo + "\n" +
                        "Narrative_Moment_UUID: " + gpsData.uuid + "\n" +
                        "Narrative_Quality: " + photo_quarity + "\n" +
                        "Narrative_Address_Country: " + gpsData.getCountry() + "\n" +
                        "Narrative_Address_City: " + gpsData.getCity() + "\n" +
                        "Narrative_Address_Street: " + gpsData.getStreet() + "\n" +
                        "Narrative_Favorite: " + favorite + "\n";

                ExifInterface exifInterface = new ExifInterface(tmpFile.getAbsolutePath());
                if (gpsData.gpsAvailable) {

                    try {
                        String ns = "N";
                        String we = "E";

                        if (gpsData.getLatitude() < 0) {
                            ns = "S";
                        }
                        if (gpsData.getLongitude() < 0) {
                            we = "W";
                        }
                        exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE, CnvUtil.latlong2GeoFormat(Math.abs(gpsData.getLatitude())));
                        exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, ns);
                        exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, CnvUtil.latlong2GeoFormat(Math.abs(gpsData.getLongitude())));
                        exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, we);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                }
                cal.setTimeZone(TimeZone.getTimeZone("GMT"));
                String exifDate = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss").format(cal.getTime());
                exifInterface.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL,exifDate);
                exifInterface.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, description);
                //exifInterface.setAttribute(ExifInterface.TAG_MAKER_NOTE, "");
                exifInterface.saveAttributes();
                tmpFile.setLastModified(cal.getTimeInMillis());
                Job_UploadPhoto_Google job=new Job_UploadPhoto_Google();
                job.setInfo(tmpFile.getAbsolutePath(),photoInfo,gpsData);
                this.service.addJobFirst(job);
            }
        }
    }
}
