package com.decad3nce.aegis;

import java.util.List;

import com.decad3nce.aegis.Fragments.AdvancedSettingsFragment;
import com.decad3nce.aegis.Fragments.InstallToSystemDialogFragment;
import eu.chainfire.libsuperuser.Shell;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.app.SherlockPreferenceActivity;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

public class AdvancedSettingsActivity extends SherlockPreferenceActivity implements InstallToSystemDialogFragment.NoticeDialogListener {
    private static final String TAG = "aeGis";
    private static String accountName;
    private static boolean dropboxAccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         ActionBar actionBar = getSupportActionBar();
         actionBar.setDisplayHomeAsUpEnabled(true);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("google_prefs", MODE_PRIVATE);
        accountName = prefs.getString("chosen_google_account_name", null);
        
        SharedPreferences prefs1 = getSharedPreferences("dropbox_prefs", MODE_PRIVATE);
        String key = prefs1.getString("dropbox_access_key", null);
        dropboxAccess = getKeyHonesty(key);
        
        getFragmentManager().beginTransaction().replace(android.R.id.content,
        new AdvancedSettingsFragment()).commit();
    }
    
    private boolean getKeyHonesty(String key) {
        if(key != null) {
            return true;
        } else {
            return false;
        }
    }

    public static String getAccountName() {
        if(accountName == null) {
            return null;
        }
        return accountName;
    }
    
    public static boolean getDropboxAccess() {
        return dropboxAccess;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent parentActivityIntent = new Intent(this, AegisActivity.class);
                parentActivityIntent.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(parentActivityIntent);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {      
        boolean installedAsSystem = isAppInstalledAsSystem("com.decad3nce.aegis");
        
        if (!installedAsSystem) {
            (new Startup()).setContext(this).execute();
        } else {
            Toast.makeText(this, getResources().getString(R.string.advanced_install_to_system_fail), Toast.LENGTH_LONG).show();
        }
    }

    private boolean isAppInstalledAsSystem(String uri) {
        PackageManager pm = getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(uri, 0);
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return false;
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
    }
    
    private class Startup extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog = null;
        public Dialog dialog1 = null;
        private Context context = null;
        private boolean suAvailable = false;
        private String suVersion = null;
        private String suVersionInternal = null;
        private List<String> suResult = null;

        public Startup setContext(Context context) {
            this.context = context;
            return this;
        }
        
        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(context);
            dialog.setTitle(getResources().getString(R.string.app_name));
            dialog.setMessage(getResources().getString(R.string.advanced_dialog_installing));
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.show();
            
            dialog1 = new Dialog(context);
            dialog1.setTitle(getResources().getString(R.string.app_name));
            dialog1.setContentView(R.layout.dialog_view);
            dialog1.setCancelable(true);
        }

        @Override
        protected Void doInBackground(Void... params) {
            suAvailable = Shell.SU.available();
            if (suAvailable) {
                suVersion = Shell.SU.version(false);
                suVersionInternal = Shell.SU.version(true);
                suResult = Shell.SU.run(new String[] {
                    "id",
                    "busybox mount -o remount,rw /system",
                    "busybox cp /data/app/com.decad3nce.aegis*.apk /system/app/",
                    "busybox chmod 644 /system/app/com.decad3nce.aegis*.apk",
                    "busybox pm uninstall com.decad3nce.aegis",
                    "busybox rmdir /data/app-lib/com.decad3nce*",
                    "busybox rm /data/app/com.decad3nce.aegis*.apk",
                    "busybox mount -o remount,ro /system",
                    "busybox reboot"
                });
            }

            return null;
        }
        
        @Override
        protected void onPostExecute(Void result) {
            dialog.dismiss();
            
            if(!suAvailable) {
                    dialog1.show();
                }
        }
    }
}
