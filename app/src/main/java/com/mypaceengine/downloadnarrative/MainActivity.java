package com.mypaceengine.downloadnarrative;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via email/password.
 */
public class MainActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    private Button narrativeBtn;
//    private Button googleAuthBtn;
//    private Button localBtn;
    private Button syncBtn;

    private Switch celSwitch;
    private Switch googleSwitch;
    private Switch localSwitch;

    private RadioButton localPathLbl;
    private RadioButton localDCIMPathLbl;
    private RadioButton localPICPathLbl;

    private TextView storageSizeLbl;
    private TextView accountLbl;
    private TextView narrative_error;
    private TextView sizeErrorLbl;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    private String narrativeKey;

   private String local_filepath;
    private String dcim_filepath;
    private String picture_filepath;

    static final int GOOGLE_REQUEST_CODE=00000001;

    DataUtil dataUtil=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataUtil=new DataUtil(this);
        setContentView(com.mypaceengine.downloadnarrative.R.layout.activity_main);

        narrative_error=(TextView) findViewById(com.mypaceengine.downloadnarrative.R.id.desc_narrative_text_error_View);
        if(dataUtil.getNarrativeReauthNeed()){
            narrative_error.setText(getResources().getString(com.mypaceengine.downloadnarrative.R.string.desc_Resync_need));
            narrative_error.setVisibility(View.VISIBLE);
        }else{
            narrative_error.setText("");
            narrative_error.setVisibility(View.GONE);
        }


        narrativeBtn=(Button) findViewById(com.mypaceengine.downloadnarrative.R.id.narrative_authorize_button);

//        googleAuthBtn=(Button) findViewById(com.mypaceengine.downloadnarrative.R.id.google_authorizeBtn);

        syncBtn=(Button) findViewById(com.mypaceengine.downloadnarrative.R.id.startSyncBtn);

        local_filepath=getApplicationContext().getExternalFilesDir("photos").getAbsolutePath();
        dcim_filepath= Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_DCIM + File.separator+Conf.DCIMFolderName+File.separator;
        picture_filepath=Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_PICTURES + File.separator+Conf.DCIMFolderName+File.separator;

        String strageSizeStr=
                "storage Information\nTotal: "+String.format("%,dMB",(int)(Environment.getExternalStorageDirectory().getTotalSpace()/1000000))+
                " Used: "+String.format("%,dMB",(int)(Environment.getExternalStorageDirectory().getUsableSpace()/1000000))+
                " Free: "+String.format("%,dMB",(int)(Environment.getExternalStorageDirectory().getFreeSpace()/1000000));

        localPathLbl=(RadioButton) findViewById(com.mypaceengine.downloadnarrative.R.id.localPrivatePath_lbl);
        localPathLbl.setText(getResources().getString(com.mypaceengine.downloadnarrative.R.string.guide_local_storagepath)+local_filepath);

        localDCIMPathLbl=(RadioButton) findViewById(com.mypaceengine.downloadnarrative.R.id.localDCIMPath_lbl);
        localDCIMPathLbl.setText(getResources().getString(com.mypaceengine.downloadnarrative.R.string.guide_dcim_storagepath)+dcim_filepath);

        localPICPathLbl=(RadioButton) findViewById(com.mypaceengine.downloadnarrative.R.id.localPICPath_lbl);
        localPICPathLbl.setText(getResources().getString(com.mypaceengine.downloadnarrative.R.string.guide_pic_storagepath)+picture_filepath);

        storageSizeLbl=(TextView) findViewById(com.mypaceengine.downloadnarrative.R.id.sizeinformation_lbl);
        storageSizeLbl.setText(strageSizeStr);

        sizeErrorLbl=(TextView) findViewById(com.mypaceengine.downloadnarrative.R.id.desc_size_error_View);
        if(Environment.getExternalStorageDirectory().getFreeSpace()<Conf.MinimumStorage){
            sizeErrorLbl.setText(getResources().getString(com.mypaceengine.downloadnarrative.R.string.desc_StorageFreeSpace_need));
            sizeErrorLbl.setVisibility(View.VISIBLE);
        }else{
            sizeErrorLbl.setText("");
            sizeErrorLbl.setVisibility(View.GONE);
        }

        if(dataUtil.getFolderType()==DataUtil.FOLDER_DCIM){
            localDCIMPathLbl.setChecked(true);
        }else if (dataUtil.getFolderType()==DataUtil.FOLDER_PIC){
            localPICPathLbl.setChecked(true);
        }else{
            localPathLbl.setChecked(true);
        }

        localPathLbl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    dataUtil.setFolderType(DataUtil.FOLDER_LOCAL);
                }
            }

        });
        localDCIMPathLbl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    dataUtil.setFolderType(DataUtil.FOLDER_DCIM);
                }
            }

        });
        localPICPathLbl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    dataUtil.setFolderType(DataUtil.FOLDER_PIC);
                }
            }

        });

        accountLbl=(TextView) findViewById(com.mypaceengine.downloadnarrative.R.id.accountinformation_lbl);
        accountLbl.setText("");

//        localBtn=(Button) findViewById(com.mypaceengine.downloadnarrative.R.id.localfolder_btn);
//        localBtn.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                try {
//                    File sharedfolder = null;
//                    if(localDCIMPathLbl.isChecked()){
//                        sharedfolder = new File(getApplicationContext().getExternalFilesDir("DCIM"),Conf.DCIMFolderName+File.separator);
//                    }else if(localPICPathLbl.isChecked()){
//                        sharedfolder = new File(getApplicationContext().getExternalFilesDir("Pictures"),Conf.DCIMFolderName+File.separator);
//                    }else{
//                        sharedfolder = new File(getApplicationContext().getFilesDir(), "photos"+File.separator);
//                    }
//                    sharedfolder=new File(Environment.getExternalStorageDirectory(),"DCIM");
//                    Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), "com.mypaceengine.downloadnarrative", sharedfolder);
//                    Intent intent = new Intent(Intent.ACTION_VIEW);
////                    intent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
//                    intent.setDataAndType(contentUri,"resource/folder");
//                    intent.addFlags(
//                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
//                    startActivityForResult(intent, 0);
//                }catch(Exception ex){
//                    Toast.makeText(getApplicationContext(), "File Manager is not found!", Toast.LENGTH_LONG).show();
//                }
//            }
//        });

        syncBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
                if(scheduler.getPendingJob(1)==null){
                    registerJobSchedule();
                }else{
                    stopJobSchedule();
                }
                treatBtn();
            }
        });

        celSwitch=(Switch) findViewById(com.mypaceengine.downloadnarrative.R.id.enable_cell_switch);

        googleSwitch=(Switch) findViewById(com.mypaceengine.downloadnarrative.R.id.enable_google_switch);

        localSwitch=(Switch) findViewById(com.mypaceengine.downloadnarrative.R.id.enable_local_swicth);

        mLoginFormView = findViewById(com.mypaceengine.downloadnarrative.R.id.login_form);
        mProgressView = findViewById(com.mypaceengine.downloadnarrative.R.id.login_progress);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        if(narrativeKey!=null){
            dataUtil.setNarrativeKey(narrativeKey);
        }
        celSwitch.setChecked(dataUtil.getEnableCelSync());
        googleSwitch.setChecked(dataUtil.getEnableGoogleSync());
        localSwitch.setChecked(dataUtil.getEnableLocalSync());



        narrativeBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getApplicationContext(), WebActivity.class);
                intent.putExtra("URL", "https://narrativeapp.com/oauth2/authorize/?response_type=code&client_id="+Conf.NarrativeClient_ID);
                startActivity(intent);

            }
        });
//        googleAuthBtn.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent =AccountPicker.newChooseAccountIntent(null, null, new String[]{"com.google"},
//                        false, null, null, null, null);
//                startActivityForResult(intent, MainActivity.GOOGLE_REQUEST_CODE);
//            }
//        });

        localSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                dataUtil.setEnableLocalSync(isChecked);
                treatBtn();
            }

        });
        googleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[]{"com.google"},
                            false, null, null, null, null);
                    startActivityForResult(intent, MainActivity.GOOGLE_REQUEST_CODE);
                }else {
                    dataUtil.setEnableGoogleSync(isChecked);
                    treatBtn();
                }
            }

        });
        celSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                dataUtil.setEnableCelSync(isChecked);
            }

        });
        treatBtn();

    }

    public void treatBtn(){
        JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);

        //JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        //JobInfo jobInfo=scheduler.getPendingJob(1);
        boolean isAlreadySyncTask=(scheduler.getPendingJob(1)!=null);
        String key=dataUtil.getNarrativeKey();
        String googleAccount = dataUtil.getGoogleAccount();
        syncBtn.setEnabled(((key!=null)&&(!dataUtil.getNarrativeReauthNeed()))&&(((dataUtil.getEnableGoogleSync())&&(googleAccount!=null))||(dataUtil.getEnableLocalSync())));
//        googleAuthBtn.setEnabled((!isAlreadySyncTask)&&(dataUtil.getEnableGoogleSync()));
        celSwitch.setEnabled(!isAlreadySyncTask);
        googleSwitch.setEnabled(!isAlreadySyncTask);
        String accountName=dataUtil.getGoogleAccount();
        if((googleSwitch.isChecked()&&accountName.length()>0)){
            accountLbl.setText(getResources().getString(com.mypaceengine.downloadnarrative.R.string.guide_account_name)+" "+accountName);
            accountLbl.setVisibility(View.VISIBLE);
        }else{
            accountLbl.setText("");
            accountLbl.setVisibility(View.GONE);
        }
        localSwitch.setEnabled(!isAlreadySyncTask);
        localDCIMPathLbl.setEnabled((!isAlreadySyncTask)&&(localSwitch.isChecked()));
        localPICPathLbl.setEnabled((!isAlreadySyncTask)&&(localSwitch.isChecked()));;
        localPathLbl.setEnabled((!isAlreadySyncTask)&&(localSwitch.isChecked()));

        narrativeBtn.setEnabled(!isAlreadySyncTask);
        if(isAlreadySyncTask){
            syncBtn.setText(getResources().getString(R.string.desc_StopSync_Btn));
        }else{
            syncBtn.setText(getResources().getString(R.string.desc_Sync_Btn));

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void stopJobSchedule(){
        JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        scheduler.cancel(1);
        try {
            this.deleteFile(SyncJobService.DataFile);
        }catch (Exception ex){}
    }
    public void registerJobSchedule(){
        Toast.makeText(getApplicationContext(), "Please wait for starting sync.", Toast.LENGTH_LONG).show();
        SyncJobService.registJobSchedule(this);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        {
            if (requestCode == GOOGLE_REQUEST_CODE ) {
                if(resultCode == Activity.RESULT_OK) {
                    AccountManager manager = AccountManager.get(this);
                    manager.getAuthToken(new Account(data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME), "com.google"), "lh2", null,
                            this, new AccountManagerCallback<Bundle>() {
                                @Override
                                public void run(AccountManagerFuture<Bundle> future) {
                                    Bundle bundle = null;
                                    try {
                                        bundle = future.getResult();
                                        String accountName = bundle.getString(AccountManager.KEY_ACCOUNT_NAME);
                                        String accountType = bundle.getString(AccountManager.KEY_ACCOUNT_TYPE);
                                        String authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                                        dataUtil.setGoogleAccounty(accountName);
                                        // Log.d(accountName, accountType);
                                        // Log.d("Auth!", authToken);
                                    } catch (OperationCanceledException e) {
                                        googleSwitch.setChecked(false);
                                        Toast.makeText(getApplicationContext(), getResources().getString(com.mypaceengine.downloadnarrative.R.string.desc_cancel_Auth_google), Toast.LENGTH_LONG).show();
                                    } catch (IOException e) {
                                        googleSwitch.setChecked(false);
                                        e.printStackTrace();
                                    } catch (AuthenticatorException e) {
                                        googleSwitch.setChecked(false);
                                        e.printStackTrace();
                                    }
                                    dataUtil.setEnableGoogleSync(true);
                                    treatBtn();
                                }
                            }, null);
                }else{
                    googleSwitch.setChecked(false);
                    treatBtn();
                }
            }
        }
    }

    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mEmailView, com.mypaceengine.downloadnarrative.R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(com.mypaceengine.downloadnarrative.R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(com.mypaceengine.downloadnarrative.R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(com.mypaceengine.downloadnarrative.R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(email, password);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(MainActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();

        Intent intent = getIntent();
        String action = intent.getAction();
//        if (Intent.ACTION_VIEW.equals(action)) {
//            Uri uri = intent.getData();
//            if (uri!=null) {
//                String code = uri.getQueryParameter("code");
//                Log.d("Code",code);
//                narrativeKey=code;
//                if(syncService!=null){
//                    syncService.setNarrativeKey(code);
//                    syncService.setNarrativeReauthNeed(false);
//                }

//            }
  //      }

        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW,
                "Photo DownLoader from Narrative Service",
                Uri.parse("https://play.google.com/store/apps/details?id=com.mypaceengine.downloadnarrative"),
                Uri.parse("android-app://com.mypaceengine.downloadnarrative/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);



        this.getApplicationContext().getExternalFilesDir("photos");
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW,
                "Photo DownLoader from Narrative Service",
                Uri.parse("https://play.google.com/store/apps/details?id=com.mypaceengine.downloadnarrative"),
                Uri.parse("android-app://com.mypaceengine.downloadnarrative/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return false;
            }

            for (String credential : DUMMY_CREDENTIALS) {
                String[] pieces = credential.split(":");
                if (pieces[0].equals(mEmail)) {
                    // Account exists, return true if the password matches.
                    return pieces[1].equals(mPassword);
                }
            }

            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                finish();
            } else {
                mPasswordView.setError(getString(com.mypaceengine.downloadnarrative.R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

