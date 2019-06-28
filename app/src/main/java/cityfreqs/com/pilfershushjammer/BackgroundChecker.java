package cityfreqs.com.pilfershushjammer;


import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import java.util.ArrayList;
import java.util.List;

class BackgroundChecker {
    private FileProcessor fileProcessor;
    private PackageManager packageManager;
    private ArrayList<AppEntry> appEntries;
    private int audioBeaconCount;

    // needs to check for :
    // " android.Manifest.permission.* "
    // " android.permission.* "
    //
    // Manifest.permission can be requested at runtime
    //

    private static final String RECORD_PERMISSION = "RECORD_AUDIO";
    private static final String BOOT_PERMISSION = "RECEIVE_BOOT_COMPLETED";
    private static String[] AUDIO_SDK_NAMES; // fixed as per raw/file

    BackgroundChecker(Context context) {
        fileProcessor = new FileProcessor(context);
        appEntries = new ArrayList<>();
        audioBeaconCount = 0;
    }

    boolean initChecker(PackageManager packageManager) {
        // need a user updatable SDK_NAMES list insert...
        this.packageManager = packageManager;

        // populate audio_sdk_names
        AUDIO_SDK_NAMES = fileProcessor.getAudioSdkArray();
        return (AUDIO_SDK_NAMES != null && AUDIO_SDK_NAMES.length > 0);
    }

    /********************************************************************/

    String displayAudioSdkNames() {
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

    int getUserRecordNumApps() {
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

    void audioAppEntryLog() {
        if (appEntries.size() > 0) {
            for (AppEntry appEntry : appEntries) {
                MainActivity.entryLogger(appEntry.entryPrint(), appEntry.checkForCaution());
            }
        }
        else {
            MainActivity.entryLogger(fileProcessor.context.getResources().getString(R.string.userapp_scan_12), false);
        }
    }

    boolean hasAudioBeaconApps() {
        return audioBeaconCount > 0;
    }

    void checkAudioBeaconApps() {
        audioBeaconCount = 0;
        int indexCount = 0;
        boolean audioBeaconFound = false;
        if (appEntries.size() > 0) {
            for (AppEntry appEntry : appEntries) {
                if (appEntry.getServices()) {
                    // have services, check for audioBeacon names
                    if (checkForAudioBeaconService(appEntry.getServiceNames())) {
                        // have a substring match, set original
                        audioBeaconFound = true;
                    }
                }
                if (appEntry.getReceivers()) {
                    // have services, check for audioBeacon names
                    if (checkForAudioBeaconReceivers(appEntry.getReceiverNames())) {
                        // have a substring match, set original
                        audioBeaconFound = true;
                    }
                }
                if (audioBeaconFound) audioBeaconCount++;
                appEntries.get(indexCount).setAudioBeacon(audioBeaconFound);
                indexCount++;
                // reset
                audioBeaconFound = false;
            }
        }
    }

    String[] getAudioBeaconAppNames() {
        if (audioBeaconCount > 0) {
            String[] appNames = new String[audioBeaconCount];
            int i = 0;
            for (AppEntry appEntry : appEntries) {
                if (appEntry.getAudioBeacon()) {
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
// if find one instance return true
    private boolean checkForAudioBeaconService(String[] serviceNames) {
        if (AUDIO_SDK_NAMES != null && AUDIO_SDK_NAMES.length > 0) {
            for (String name : serviceNames) {
                for (String SDKname : AUDIO_SDK_NAMES) {
                    if (name.contains(SDKname)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean checkForAudioBeaconReceivers(String[] receiverNames) {
        if (AUDIO_SDK_NAMES != null && AUDIO_SDK_NAMES.length > 0) {
            for (String name : receiverNames) {
                for (String SDKname : AUDIO_SDK_NAMES) {
                    if (name.contains(SDKname)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    String[] getOverrideScanAppNames() {
        //
        String[] appNames = new String[appEntries.size()];
        int i = 0;
        for (AppEntry appEntry : appEntries) {
            appNames[i] = appEntry.getActivityName();
            i++;
        }
        return appNames;
    }

    AppEntry getOverrideScanAppEntry(int appEntryIndex) {
        return appEntries.get(appEntryIndex);
    }

    /********************************************************************/
    /*
     *
     */
    private boolean isUserApp(ApplicationInfo applicationInfo) {
        int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
        return (applicationInfo.flags & mask) == 0;
    }

    void runChecker() {
        // get list of apps
        // also:
        // PackageManager.getSystemAvailableFeatures() - list all features
        // PackageManager.hasSystemFeature(String name) - search for specific ie: "FEATURE_MICROPHONE"

        PackageInfo packageInfo;
        List<ApplicationInfo> packages;
        packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        int idCounter = 0;
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
                            appEntry.setBootCheck(true);
                        }

                        if (permsString.contains(RECORD_PERMISSION)) {
                            appEntry.setRecordable(true);
                        }

                    }

                    // check for services and receivers
                    if (packageInfo.services != null) {
                        appEntry.setServiceInfo(packageInfo.services);
                    }
                    else {
                        appEntry.setServices(false);
                    }

                    if (packageInfo.receivers != null) {
                        appEntry.setActivityInfo(packageInfo.receivers);
                    }
                    else {
                        appEntry.setReceivers(false);
                    }
                    //add to list
                    appEntry.setIdNum(idCounter);
                    appEntries.add(appEntry);
                    idCounter++;
                }
            }
            catch (NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
