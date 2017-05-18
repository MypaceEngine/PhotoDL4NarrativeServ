package com.mypaceengine.downloadnarrative;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.GphotoEntry;
import com.google.gdata.data.photos.UserFeed;
import com.google.gdata.data.spreadsheet.Data;
import com.google.gdata.util.ServiceException;
import com.google.gdata.util.ServiceForbiddenException;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by MypaceEngine on 2017/05/13.
 */

public class GoogleUtil {
    static PicasawebService picasaService = null;
    static String googleAccountName = null;
    static String googleauthToken = null;

    static public PicasawebService getPicasaService(SyncJobService service) {
        if (picasaService != null) {
            return picasaService;
        } else {
            getToken(service);
        }
        return picasaService;
    }

    static public String getPicasaAccountName(SyncJobService service) {
        if (googleAccountName != null) {
            return googleAccountName;
        } else {
            getToken(service);
        }
        return googleAccountName;
    }

    static public String getPicasaToken(SyncJobService service) {
        if (picasaService != null) {
            return googleauthToken;
        } else {
            getToken(service);
        }
        return googleauthToken;
    }

    static void invalidateToken() {
        picasaService = null;
        googleAccountName = null;
        googleauthToken = null;
        if(manager!=null) {
            manager.invalidateAuthToken("com.google", googleauthToken);
        }
    }

    static AccountManager manager=null;
    static private void getToken(SyncJobService service){
        Log.d("MainServiceTask", "GoogleAuthToken");
        DataUtil dataUtil=new DataUtil(service);
        manager = AccountManager.get(service);
        OnTokenAcquired tokenAcquired=new OnTokenAcquired(service);
        manager.getAuthToken(
                new Account(dataUtil.getGoogleAccount(), "com.google"),
                "lh2",            // Auth scope
                null,                        // Authenticator-specific options
                null,                           // Your activity
                tokenAcquired,         // Callback called when a token is successfully acquired
                null);               // Callback called if an error occ
        do{
            try {
                Thread.sleep(100);
            }catch (Exception ex){}
            if(tokenAcquired.finishFlg){
               break;
            }
        }while (true);
        try {
            getAlbum(picasaService);
        } catch (ServiceForbiddenException e) {
            manager.invalidateAuthToken("com.google", googleauthToken);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ServiceException e) {
            e.printStackTrace();
        }
        return;
    }

    static private class OnTokenAcquired implements AccountManagerCallback<Bundle> {
        SyncJobService service=null;
        boolean finishFlg=false;
        OnTokenAcquired(SyncJobService _service){
            service=_service;
        }
        @Override
        public void run(AccountManagerFuture<Bundle> result) {
            try {
                Bundle bundle = result.getResult();

                if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN)) {
                    googleAccountName = bundle.getString(AccountManager.KEY_ACCOUNT_NAME);
                    googleauthToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
//                    LOG.debug("Auth token {}", authToken);
                    picasaService = new PicasawebService(service.getPackageName());
                    picasaService.setUserToken(googleauthToken);
                }
                finishFlg=true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    static public List<AlbumEntry> getAlbum(PicasawebService _picasaService) throws IOException,ServiceException
    {
        List<AlbumEntry> albums =new ArrayList<AlbumEntry>();
            URL feedUrl = new URL("https://picasaweb.google.com/data/feed/api/user/default/");
            UserFeed myUserFeed = picasaService.getFeed(feedUrl, UserFeed.class);
            if(albums==null){
                albums=new ArrayList<AlbumEntry>();
            }
            List<GphotoEntry> albumG=myUserFeed.getEntries();
            for (GphotoEntry entry:albumG){
                albums.add(new AlbumEntry(entry));
            }

        return albums;
    }
}
