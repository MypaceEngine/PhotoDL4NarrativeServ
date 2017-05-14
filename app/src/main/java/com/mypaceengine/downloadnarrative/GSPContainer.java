package com.mypaceengine.downloadnarrative;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by MypaceEngine on 2016/10/01.
 */

public class GSPContainer implements Comparable{
    Calendar startCal;
    Calendar endCal;
    String country;
    String city;
    String street;
    double latitude;
    double longitude;
    String uuid;
    boolean gpsAvailable=false;

    public GSPContainer(Context _context,String startDateStr, String endDateStr, String _country, String _city, String _street, double _latitude, double _longitude,boolean _gpsAvailable,String _uuid)throws Exception{
        init( _context, startDateStr,  endDateStr,  _country,  _city,  _street, _latitude, _longitude,_gpsAvailable,_uuid);
    }

    public void init(Context context,String startDateStr, String endDateStr, String _country, String _city, String _street, double _latitude, double _longitude,boolean _gpsAvailable,String _uuid)throws Exception {
        startCal = CnvUtil.cnvCalender(startDateStr);
        endCal = CnvUtil.cnvCalender(endDateStr);
        country = _country;
        city = _city;
        street = _street;
        uuid=_uuid;
        if(_gpsAvailable){
            Log.d("GPS Position","Defined Latitude:"+_latitude+" Longitude:"+_longitude);
        }else if((!_city.isEmpty())||
                        (!_street.isEmpty())||
                        (!_country.isEmpty())
                ) {
            String[] posArr = {_street, _city, _country};
            for (int i = 0; i < posArr.length; i++) {
                StringBuffer search_key = new StringBuffer();
                for (int j = i; j < posArr.length; j++) {
                    if (search_key.length() != 0) {
                        search_key.append(" ");
                    }
                    search_key.append(posArr[j]);
                }
                Log.d("Addres", search_key.toString());
                Geocoder gcoder = new Geocoder(context.getApplicationContext(), Locale.getDefault());
                int maxResults = 1;
                List<Address> lstAddr=null;
                try {
                    lstAddr = gcoder.getFromLocationName(search_key.toString(), maxResults);
                }catch(Exception ex){
                    ex.printStackTrace();
                }
                if (lstAddr != null && lstAddr.size() > 0) {
                    // 緯度・経度取得
                    Address addr = lstAddr.get(0);
                    _latitude = addr.getLatitude();
                    _longitude = addr.getLongitude();
                    Log.d("GPS Position From Addr", "Latitude:" + _latitude + " Longitude:" + _longitude);
                    _gpsAvailable = true;
                    break;
                }
            }
        }else{
            Log.d("GPS Position & Address","Not Defined.");
        }

        latitude = _latitude;
        longitude = _longitude;

        gpsAvailable = _gpsAvailable;

    }
    public Calendar getStartCal() {
        return startCal;
    }

    public Calendar getEndCal() {
        return endCal;
    }

    public String getCountry() {
        return country;
    }

    public String getCity() {
        return city;
    }

    public String getStreet() {
        return street;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
    @Override
    public int compareTo(Object target){
        return compareToExec((GSPContainer)target);
    }
    public int compareToExec(GSPContainer target){
        if(endCal.getTimeInMillis()<target.getEndCal().getTimeInMillis()){
            return -1;
        }else if (endCal.getTimeInMillis()==target.getEndCal().getTimeInMillis()){
            return 0;
        }else{
            return 1;
        }
    }
    public boolean isIncluded(Calendar target){
        if(
                (startCal.getTimeInMillis()<target.getTimeInMillis())&&
                        (endCal.getTimeInMillis()>target.getTimeInMillis())
                ) {
            return true;
        }else if(
                (startCal.getTimeInMillis()>target.getTimeInMillis())&&
                        (endCal.getTimeInMillis()<target.getTimeInMillis())
                ){
                return true;
        }else{
            return false;
        }
    }
    public boolean isAfterEnd(Calendar target){
        if(endCal.getTimeInMillis()<target.getTimeInMillis()){
            return true;
        }else{
            return false;
        }
    }
}
