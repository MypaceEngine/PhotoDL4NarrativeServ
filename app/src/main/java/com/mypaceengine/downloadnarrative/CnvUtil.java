package com.mypaceengine.downloadnarrative;

import android.content.Context;

import com.google.common.io.Files;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by MypaceEngine on 2017/05/13.
 */

public class CnvUtil {
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

    public static File cnvFilePath(Context context, String baseFolderName, Calendar cal, String format) throws Exception{

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
}
