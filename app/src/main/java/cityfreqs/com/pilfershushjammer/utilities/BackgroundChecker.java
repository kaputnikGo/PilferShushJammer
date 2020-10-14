package cityfreqs.com.pilfershushjammer.utilities;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;


public class BackgroundChecker {
    private static final String TAG = "PilferShush_BACKCHK";
    private final FileProcessor fileProcessor;
    private PackageManager packageManager;
    private final ArrayList<AppEntry> appEntries;
    private int audioBeaconCount;
    private final boolean DEBUG;

    // needs to check for :
    // " android.Manifest.permission.* "
    // " android.permission.* "
    //
    // Manifest.permission can be requested at runtime
    //

    private static final String RECORD_PERMISSION = "RECORD_AUDIO";
    private static final String BOOT_PERMISSION = "RECEIVE_BOOT_COMPLETED";
    private static final String ACCESS_PERMISSION = "BIND_ACCESSIBILITY_SERVICE";
    private static String[] AUDIO_SDK_NAMES; // fixed as per raw/file

    public BackgroundChecker(Context context, boolean debug) {
        fileProcessor = new FileProcessor(context);
        appEntries = new ArrayList<>();
        audioBeaconCount = 0;
        DEBUG = debug;
    }

    public boolean initChecker(PackageManager packageManager) {
        // need a user updatable SDK_NAMES list insert...
        this.packageManager = packageManager;

        // populate audio_sdk_names
        AUDIO_SDK_NAMES = fileProcessor.getAudioSdkArray();
        return (AUDIO_SDK_NAMES != null && AUDIO_SDK_NAMES.length > 0);
    }

    /********************************************************************/

    public String displayAudioSdkNames() {
        // return a string of names + \n
        if (AUDIO_SDK_NAMES != null && AUDIO_SDK_NAMES.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (String name : AUDIO_SDK_NAMES) {
                sb.append(name).append("\n");
            }
            return sb.toString();
        }
        return "error: none found \n";
    }

    public int getUserRecordNumApps() {
        // count number with getRecordable == true
        int count = 0;
        if (appEntries != null) {
            for (AppEntry appEntry : appEntries) {
                if (appEntry.getRecordable()) count ++;
            }
            return count;
        }
        else
            return 0;
    }

    /*
    // a debug method
    public void audioAppEntryLog() {
        if (appEntries.size() > 0) {
            for (AppEntry appEntry : appEntries) {
                debugLogger(appEntry.entryPrint(), appEntry.checkForCaution());
            }
        }
        else {
            debugLogger(fileProcessor.context.getResources().getString(R.string.userapp_scan_12), false);
        }
    }
    */

    public boolean hasAudioBeaconApps() {
        return audioBeaconCount > 0;
    }

    public void checkAudioBeaconApps() {
        audioBeaconCount = 0;
        int indexCount = 0;
        boolean audioBeaconFound = false;
        if (appEntries.size() > 0) {
            for (AppEntry appEntry : appEntries) {
                if (appEntry.getServices()) {
                    // have services, check for audioBeacon names
                    if (checkForAudioBeaconService(appEntry)) {
                        // have a substring match, set original
                        audioBeaconFound = true;
                    }
                }
                if (appEntry.getReceivers()) {
                    // have services, check for audioBeacon names
                    if (checkForAudioBeaconReceivers(appEntry)) {
                        // have a substring match, set original
                        audioBeaconFound = true;
                    }
                }
                if (audioBeaconFound)
                    audioBeaconCount++;
                appEntries.get(indexCount).setAudioSdk(audioBeaconFound);
                indexCount++;

                // reset
                audioBeaconFound = false;
            }
        }
    }

    public String[] getAudioBeaconAppNames() {
        if (audioBeaconCount > 0) {
            String[] appNames = new String[audioBeaconCount];
            int i = 0;
            for (AppEntry appEntry : appEntries) {
                if (appEntry.getAudioSdk()) {
                    appNames[i] = appEntry.getActivityName();
                    i++;
                }
            }
            return appNames;
        }
        else
            return null;
    }


    // loop though services/receivers lists and look for substrings of interest,
    // hardcoded for now, user added later?
    // TODO if find one instance return true, need to account for multiple
    private boolean checkForAudioBeaconService(AppEntry appEntry) {
        if (AUDIO_SDK_NAMES != null && AUDIO_SDK_NAMES.length > 0) {
            for (String name : appEntry.getServiceNames()) {
                for (String SDKname : AUDIO_SDK_NAMES) {
                    if (name.contains(SDKname)) {
                        appEntry.setServiceWithSDK(name);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean checkForAudioBeaconReceivers(AppEntry appEntry) {
        if (AUDIO_SDK_NAMES != null && AUDIO_SDK_NAMES.length > 0) {
            for (String name : appEntry.getReceiverNames()) {
                for (String SDKname : AUDIO_SDK_NAMES) {
                    if (name.contains(SDKname)) {
                        appEntry.setReceiverWithSDK(name);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean checkPackageNameForAudioBeacons(String packageName) {
        if (AUDIO_SDK_NAMES != null && AUDIO_SDK_NAMES.length > 0) {
            for (String SDKname : AUDIO_SDK_NAMES) {
                if (packageName.contains(SDKname)) {
                    debugLogger("SDK found in: " + packageName);
                    return true;
                }
            }
        }
        return false;
    }
    /*
    //combine into one string matcher...
    public String[] getOverrideScanAppNames() {
        //
        String[] appNames = new String[appEntries.size()];
        int i = 0;
        for (AppEntry appEntry : appEntries) {
            appNames[i] = appEntry.getActivityName();
            i++;
        }
        return appNames;
    }
    */

    public AppEntry getOverrideScanAppEntry(int appEntryIndex) {
        return appEntries.get(appEntryIndex);
    }

    public ArrayList<AppEntry> getAppEntries() {
        return appEntries;
    }

    public int getNumberAppEntries() {
        return appEntries.size();
    }

    /********************************************************************/
    /*
     *
     */
    private boolean isUserApp(ApplicationInfo applicationInfo) {
        int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
        return (applicationInfo.flags & mask) == 0;
    }

    public void runChecker() {
        // get list of apps
        // also:
        // PackageManager.getSystemAvailableFeatures() - list all features
        // PackageManager.hasSystemFeature(String name) - search for specific ie: "FEATURE_MICROPHONE"

        PackageInfo packageInfo;
        List<ApplicationInfo> packages;
        packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo applicationInfo : packages) {
            try {
                packageInfo = packageManager.getPackageInfo(applicationInfo.packageName,
                        PackageManager.GET_PERMISSIONS |
                                PackageManager.GET_SERVICES |
                                PackageManager.GET_RECEIVERS);

                // check permissions and services
                if (packageInfo.requestedPermissions != null && isUserApp(applicationInfo)) {
                    AppEntry appEntry = new AppEntry(packageInfo.packageName,
                            (String) packageInfo.applicationInfo.loadLabel(packageManager));
                    // check for specific permissions
                    for (String permsString: packageInfo.requestedPermissions) {
                        if (permsString.contains(BOOT_PERMISSION)) {
                            appEntry.setBootCheck();
                        }
                        if (permsString.contains(RECORD_PERMISSION)) {
                            appEntry.setRecordable();
                        }
                        if (permsString.contains(ACCESS_PERMISSION)) {
                            appEntry.setAccessibility();
                        }
                    }

                    // check for services and receivers
                    appEntry.setServices(packageInfo.services != null);
                    if (packageInfo.services != null) {
                        appEntry.setServiceInfo(packageInfo.services);
                        if (checkForAudioBeaconService(appEntry)) {
                            appEntry.setAudioSdk(true);
                        }
                    }

                    appEntry.setReceivers(packageInfo.receivers != null);
                    if (packageInfo.receivers != null) {
                        appEntry.setActivityInfo(packageInfo.receivers);
                        if (checkForAudioBeaconReceivers(appEntry)) {
                            appEntry.setAudioSdk(true);
                        }
                    }
                    // check packagename for audioBeaconSDK as well
                    if (checkPackageNameForAudioBeacons(packageInfo.packageName)) {
                        appEntry.setAudioSdk(true);
                    }
                    // check for application icon for display in appEntryDialog
                    appEntry.setAppIcon(packageManager.getApplicationIcon(applicationInfo.packageName));
                    //add to list
                    appEntries.add(appEntry);
                }
            }
            catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void debugLogger(String message) {
        // for the times that fragments arent attached etc, print to adb
        if (DEBUG) {
            Log.e(TAG, message);
        }
        else if (false) {
            Log.d(TAG, message);
        }
        else {
            Log.i(TAG, message);
        }
    }
}
