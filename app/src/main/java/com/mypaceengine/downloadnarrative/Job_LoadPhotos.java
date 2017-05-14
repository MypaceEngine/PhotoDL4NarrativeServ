package com.mypaceengine.downloadnarrative;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by MypaceEngine on 2017/05/13.
 */

public class Job_LoadPhotos extends Job_LoadNarrativeInfo_Abstract {
    String momentInfo=null;
    void setInfo(String url,String _momentInfo){
        super.setInfo(url);
        momentInfo=_momentInfo;
    }

    List<AbstractJobN> createJobInfo(JSONObject jsonObj){

        ArrayList<AbstractJobN> jobs=new ArrayList<AbstractJobN>();
        try {
            GSPContainer gpsData = cnvGPSContainer(new JSONObject(momentInfo));
            this.service.addGPSData(gpsData);

            JSONArray itemArray = jsonObj.getJSONArray("results");
            int count = itemArray.length();
            for (int i = 0; i < count; i++) {
                JSONObject obj = itemArray.getJSONObject(i);
                String photos_url = obj.getString("photos_url");


            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return jobs;
    }
    /**
     *
     *
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
     */
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
}
