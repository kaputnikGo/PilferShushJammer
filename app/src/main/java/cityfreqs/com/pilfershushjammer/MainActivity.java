package cityfreqs.com.pilfershushjammer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import cityfreqs.com.pilfershushjammer.ui.SectionsPagerAdapter;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = "PilferShush_Jammer-ACT";
    public static final String VERSION = "4.5.3";
    //public static final boolean DEBUG = true;
    private static final int REQUEST_AUDIO_PERMISSION = 1;
    private AlertDialog alertDialog;

    SectionsPagerAdapter sectionsPagerAdapter;
    ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        viewPager = findViewById(R.id.view_pager);

        viewPager.setAdapter(sectionsPagerAdapter);
        // maintain fragment state for num of pages
        viewPager.setOffscreenPageLimit(sectionsPagerAdapter.getCount() - 1);
        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        // permissions ask:
        // check API version, above 23 permissions are asked at runtime
        // if API version < 23 (6.x) fallback is manifest.xml file permission declares
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.LOLLIPOP) {

            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                        Manifest.permission.RECORD_AUDIO)) {
                    String message = getResources().getString(R.string.perms_state_2_1) + "\n\n";
                    message += getResources().getString(R.string.perms_state_2_2) + "\n\n";
                    message += Manifest.permission.RECORD_AUDIO;

                    showPermissionsDialog(message, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.RECORD_AUDIO},
                                    REQUEST_AUDIO_PERMISSION);
                        }
                    });
                }
                else {
                    // no reasoning, show perms request
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            REQUEST_AUDIO_PERMISSION);
                }
            }
            else {
                //initApp
                sectionsPagerAdapter.permissionCheckPassed(true);
            }
        }
        else {
            // pre API 23, check permissions anyway
            if (!(PermissionChecker.checkSelfPermission(MainActivity.this,
                    Manifest.permission.RECORD_AUDIO) == PermissionChecker.PERMISSION_GRANTED)) {
                closeApp();
            }
            else {
                //initApp
                sectionsPagerAdapter.permissionCheckPassed(true);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_about:
                aboutDialog();
                return true;
            case R.id.action_readme:
                readmeDialog();
                return true;
            case R.id.action_power:
                powerDialog();
                return true;
            case R.id.action_debug:
                debugDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void readmeDialog() {
        String readmeString =
                (getResources().getString(R.string.readme_dialog_2) + "\n\n")
                        + (getResources().getString(R.string.readme_dialog_3) + "\n\n")
                        + (getResources().getString(R.string.readme_dialog_4) + "\n\n")
                        + (getResources().getString(R.string.readme_dialog_5) + "\n\n")
                        + (getResources().getString(R.string.readme_dialog_6) + "\n\n")
                        + (getResources().getString(R.string.readme_dialog_7) + "\n\n")
                        + (getResources().getString(R.string.readme_dialog_8));

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
        dialogBuilder.setTitle(getResources().getString(R.string.readme_dialog_1));
        dialogBuilder.setMessage(readmeString);
        dialogBuilder.setCancelable(true);
        dialogBuilder
                .setPositiveButton(R.string.dialog_button_moreinfo, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.cityfreqs.com.au/pilfer.php"));
                        try {
                            if (browserIntent.resolveActivity(getPackageManager()) != null) {
                                startActivity(browserIntent);
                            }
                        } catch (ActivityNotFoundException ex) {
                            Toast.makeText(getApplicationContext(), "Open default browser failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_button_dismiss, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // dismissed
                        alertDialog.cancel();
                    }
                });

        alertDialog = dialogBuilder.create();
        if(!isFinishing())
            alertDialog.show();
    }

    private void aboutDialog() {
        String aboutString =
                (getResources().getString(R.string.about_dialog_2) + "\n\n")
                        + (getResources().getString(R.string.about_dialog_3) + "\n\n")
                        + (getResources().getString(R.string.about_dialog_4) + "\n\n")
                        + (getResources().getString(R.string.about_dialog_5));

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
        dialogBuilder.setTitle(getResources().getString(R.string.about_version) + VERSION);
        dialogBuilder.setMessage(aboutString);
        dialogBuilder.setCancelable(true);
        dialogBuilder
                .setPositiveButton(R.string.dialog_button_source, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/kaputnikGo/PilferShushJammer"));
                        try {
                            if (browserIntent.resolveActivity(getPackageManager()) != null) {
                                startActivity(browserIntent);
                            }
                        } catch (ActivityNotFoundException ex) {
                            Toast.makeText(getApplicationContext(), "Open default browser failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_button_dismiss, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // dismissed
                        alertDialog.cancel();
                    }
                });

        alertDialog = dialogBuilder.create();
        if(!isFinishing())
            alertDialog.show();
    }

    private void powerDialog(){
        // check for API 23, Marshmallow up
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            assert powerManager != null;
            if (!powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                // user has not set PS to ignore batt savings, pop dialog
                checkDozeDialog();
            }
        }
        else {
            // nodoze for you
            Toast toast;
            toast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.action_power_state), Toast.LENGTH_LONG);
            toast.show();
        }
    }

    @TargetApi(23)
    private void checkDozeDialog() {
        AlertDialog.Builder dialogBuilder;
        String aboutString =
                (getResources().getString(R.string.doze_dialog_1) + "\n\n")
                        + (getResources().getString(R.string.doze_dialog_2) + "\n\n")
                        + (getResources().getString(R.string.doze_dialog_3) + "\n\n")
                        + (getResources().getString(R.string.doze_dialog_4) + "\n\n")
                        + (getResources().getString(R.string.doze_dialog_5));

        dialogBuilder = new AlertDialog.Builder(MainActivity.this);
        dialogBuilder.setTitle(getResources().getString(R.string.doze_dialog_title));
        dialogBuilder.setMessage(aboutString);
        dialogBuilder.setCancelable(true);
        dialogBuilder
                .setPositiveButton(R.string.dialog_button_continue, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        MainActivity.this.startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.dialog_button_dismiss, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // dismissed
                        alertDialog.cancel();
                    }
                });
        alertDialog = dialogBuilder.create();
        if(!isFinishing())
            alertDialog.show();
    }

    private void debugDialog() {
        String debugString =
                (getResources().getString(R.string.debug_dialog_2));

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
        dialogBuilder.setTitle(getResources().getString(R.string.debug_dialog_1));
        dialogBuilder.setMessage(debugString);
        dialogBuilder.setCancelable(false);
        dialogBuilder
                .setPositiveButton(R.string.dialog_button_on, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        sectionsPagerAdapter.setDebugBoolean(true);
                        Log.d(TAG, "Debug mode on TRUE");
                    }
                })
                .setNegativeButton(R.string.dialog_button_off, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        sectionsPagerAdapter.setDebugBoolean(false);
                        Log.d(TAG, "Debug mode on FALSE");
                    }
                });

        alertDialog = dialogBuilder.create();
        if(!isFinishing())
            alertDialog.show();
    }

    private void showPermissionsDialog(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.dialog_button_continue), okListener)
                .create()
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_AUDIO_PERMISSION) {
            // Check for RECORD_AUDIO
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission Denied
                Toast toast;
                toast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.perms_state_3), Toast.LENGTH_LONG);
                toast.show();
                closeApp();
            }
            else {
                //initApp
                sectionsPagerAdapter.permissionCheckPassed(true);
            }
        }
        else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void closeApp() {
        Log.d(TAG, getResources().getString(R.string.perms_state_4));
        finishAffinity();
    }

}
