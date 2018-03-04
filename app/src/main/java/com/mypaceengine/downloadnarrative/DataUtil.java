package com.mypaceengine.downloadnarrative;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by MypaceEngine on 2017/05/13.
 */

public class DataUtil {

    static Object syncObject = new Object();
    Context cnx = null;

    public DataUtil(Context _cnx) {
        cnx = _cnx;
    }

    final static String NARRATIVE_KEY_CODE = "NARRATIVE_KEY_CODE";
    final static String NARRATIVE_KEY_CREATE_TIME = "NARRATIVE_KEY_CREATE_TIME";
    final static String NARRATIVE_REAUTH_REQUIRE = "NARRATIVE_REAUTH_REQUIRE";
    final static String GOOGLE_ACCOUNT = "GOOGLE_ACCOUNT";
    final static String GOOGLE_VALIDATION_TIME = "GOOGLE_VALIDATION_TIME";
    final static String GOOGLE_SYNC_FLAG = "GOOGLE_SYNC_FLAG";
    final static String LOCAL_SYNC_FLAG = "LOCAL_SYNC_FLAG";
    final static String NAS_SYNC_FLAG = "NAS_SYNC_FLAG";
    final static String CEL_SYNC_FLAG = "CEL_SYNC_FLAG";

    final static String FOLDER_TYPE ="FOLDER_TYPE";
    final static int FOLDER_LOCAL=0;
    final static int FOLDER_DCIM=1;
    final static int FOLDER_PIC=2;

    public final static String NARRATIVE_COOKIE = "NARRATIVE_COOKIE";

    public void setNarrativeKey(String key) {
        saveString(NARRATIVE_KEY_CODE, key);
        saveLong(NARRATIVE_KEY_CREATE_TIME, System.currentTimeMillis());
    }

    public String getNarrativeKey() {
        return loadString(NARRATIVE_KEY_CODE, null);
    }

    public long getNarrativeKey_CreateTime() {
        return loadLong(NARRATIVE_KEY_CREATE_TIME, -1);
    }

    public void setGoogleAccounty(String account) {
        saveString(GOOGLE_ACCOUNT, account);
        saveLong(GOOGLE_VALIDATION_TIME, System.currentTimeMillis());
    }

    public String getGoogleAccount() {
        return loadString(GOOGLE_ACCOUNT, null);
    }

    public long getGoogleValidationTime() {
        return loadLong(GOOGLE_VALIDATION_TIME, -1);
    }

    public void setEnableGoogleSync(boolean flag) {
        saveBoolean(GOOGLE_SYNC_FLAG, flag);
    }

    public void setEnableLocalSync(boolean flag) {
        saveBoolean(LOCAL_SYNC_FLAG, flag);
    }

    public void setEnableNasSync(boolean flag) {
        saveBoolean(NAS_SYNC_FLAG, flag);
    }

    public void setEnableCelSync(boolean flag) {
        saveBoolean(CEL_SYNC_FLAG, flag);
    }

    public void setNarrativeReauthNeed(boolean flag) {
        saveBoolean(NARRATIVE_REAUTH_REQUIRE, flag);
    }
    public void setFolderType(int type){
        saveInt(FOLDER_TYPE, type);
    }
    public void setPreviousFolderType(int type){
        saveInt(FOLDER_TYPE, type);
    }
    public void setNarrativeCookie(ArrayList<String> list) {
        saveList(NARRATIVE_COOKIE, list);
    }

    public boolean getEnableGoogleSync() {
        return loadBoolean(GOOGLE_SYNC_FLAG, false);
    }

    public boolean getEnableLocalSync() {
        return loadBoolean(LOCAL_SYNC_FLAG, false);
    }

    public boolean getEnableNasSync() {
        return loadBoolean(NAS_SYNC_FLAG, false);
    }

    public boolean getEnableCelSync() {
        return loadBoolean(CEL_SYNC_FLAG, false);
    }

    public boolean getNarrativeReauthNeed() {
        return loadBoolean(NARRATIVE_REAUTH_REQUIRE, true);
    }

    public int getFolderType() {
        return loadInt(FOLDER_TYPE, FOLDER_LOCAL);
    }

    public int getPreviousFolderType() {
        return loadInt(FOLDER_TYPE, FOLDER_LOCAL);
    }

    public ArrayList<String> getNarrativeCookie() {
        return loadList(NARRATIVE_COOKIE);
    }

    SharedPreferences pref;

    public SharedPreferences getPreference() {
        if (pref == null) {
            pref = cnx.getSharedPreferences("settings", Context.MODE_PRIVATE);
        }
        return pref;
    }

    public void saveString(String key, String value) {
        synchronized (this) {
            SharedPreferences.Editor editor = getPreference().edit();
            editor.putString(key, value);
            editor.apply();
        }
    }

    public void saveLong(String key, long value) {
        synchronized (this) {
            SharedPreferences.Editor editor = getPreference().edit();
            editor.putLong(key, value);
            editor.apply();
        }
    }

    public void saveBoolean(String key, boolean value) {
        synchronized (this) {
            SharedPreferences.Editor editor = getPreference().edit();
            editor.putBoolean(key, value);
            editor.apply();
        }
    }

    public void saveInt(String key, int value) {
        synchronized (this) {
            SharedPreferences.Editor editor = getPreference().edit();
            editor.putInt(key, value);
            editor.apply();
        }
    }

    public void saveList(String key, List<String> list) {
        synchronized (this) {
            JSONArray array = new JSONArray();
            for (int i = 0, length = list.size(); i < length; i++) {
                try {
                    array.put(i, list.get(i));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            saveString(key, array.toString());
        }
    }

    public boolean loadBoolean(String key, boolean defaultValue) {
        synchronized (this) {
            return getPreference().getBoolean(key, defaultValue);
        }
    }

    public String loadString(String key, String defaultValue) {
        synchronized (this) {
            return getPreference().getString(key, defaultValue);
        }
    }

    public long loadLong(String key, long defaultValue) {
        synchronized (this) {
            return getPreference().getLong(key, defaultValue);
        }
    }

    public int loadInt(String key, int defaultValue) {
        synchronized (this) {
            return getPreference().getInt(key, defaultValue);
        }
    }

    public ArrayList<String> loadList(String key) {
        synchronized (this) {
            String stringList = loadString(key, null);
            ArrayList<String> list = new ArrayList<String>();
            try {
                if (stringList != null) {
                    JSONArray array = new JSONArray(stringList);
                    for (int i = 0, length = array.length(); i < length; i++) {
                        list.add(array.optString(i));
                    }
                }
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
            return list;
        }
    }

    SharedPreferences prefHistory;

    public SharedPreferences getPreferenceHistory() {
        if (prefHistory == null) {
            prefHistory = cnx.getSharedPreferences("historys", Context.MODE_PRIVATE);
        }
        return prefHistory;
    }

    public boolean loadBooleanHistory(String key) {
        synchronized (this) {
            return getPreferenceHistory().getBoolean(key, false);
        }
    }

    public void saveBooleanHistory(String key, boolean value) {
        synchronized (this) {
            SharedPreferences.Editor editor = getPreferenceHistory().edit();
            editor.putBoolean(key, value);
            editor.apply();

        }
    }
}
