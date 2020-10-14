package cityfreqs.com.pilfershushjammer.utilities;

import android.content.pm.ActivityInfo;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;

import java.util.Arrays;

public class AppEntry {
    private final String activityName;
    private final String packageName;
    private Drawable appIcon;

    private boolean recordable;
    private boolean bootCheck;
    private boolean receivers;
    private boolean services;
    private boolean audioSdk;
    private boolean accessibility;

    // get first name containing SDK name string match
    private String serviceWithSDK;
    private String receiverWithSDK;

    private int servicesNum;
    private int receiversNum;

    private ServiceInfo[] serviceInfo;
    private ActivityInfo[] receiversInfo;

    AppEntry(String packageName, String activityName) {
        this.packageName = packageName;
        this.activityName = activityName;
        // defaults
        recordable = false;
        bootCheck = false;
        receivers = false;
        services = false;
        audioSdk = false;
        accessibility = false;
        servicesNum = 0;
        receiversNum = 0;
        serviceWithSDK = "not found";
        receiverWithSDK = "not found";
    }

    /********************************************************************/
    /*
     *
     */

    public String getActivityName() {
        return activityName;
    }


    /*
    void setIdNum(int idNum) {
        this.idNum = idNum;
    }
    */

    public String getPackageName() {
        return packageName;
    }

    public int getServicesNum() {
        return servicesNum;
    }
    public int getReceiversNum() {
        return receiversNum;
    }

    void setRecordable() {
        this.recordable = true;
    }
    boolean getRecordable() {
        return recordable;
    }

    void setAccessibility() {
        this.accessibility = true;
    }
    /*
    boolean getAccessibility() {
        return accessibility;
    }
    */

    void setBootCheck() {
        this.bootCheck = true;
    }

    void setReceivers(boolean receivers) {
        this.receivers = receivers;
    }
    boolean getReceivers() { return receivers; }

    void setServices(boolean services) {
        this.services = services;
    }
    boolean getServices() {
        return services;
    }

    public boolean checkForCaution() {
        // set and return,
        // called by BackgroundChecker.appEntryLog(),
        // later boolean is checked for in-depth scanning of services, receivers, etc.
        // was:
        //return (recordable && bootCheck && receivers && services && audioSdk && accessibility);
        // experiment with:
        return (recordable && audioSdk);
    }

    void setAudioSdk(boolean audioSdk) {
        this.audioSdk = audioSdk;
    }

    public boolean getAudioSdk() {
        return audioSdk;
    }

    void setServiceWithSDK(String serviceWithSDK) {
        this.serviceWithSDK = serviceWithSDK;
    }

    public String getServiceWithSDK() { return serviceWithSDK; }

    void setReceiverWithSDK(String receiverWithSDK) {
        this.receiverWithSDK = receiverWithSDK;
    }

    public String getReceiverWithSDK() { return receiverWithSDK; }

    void setAppIcon(Drawable appIcon) {
        this.appIcon = appIcon;
    }

    public Drawable getAppIcon() {
        return appIcon;
    }

    /********************************************************************/
    /*
     *  arrays
     */

    void setServiceInfo(ServiceInfo[] serviceInfo) {
        // any background recording service list
        this.serviceInfo = new ServiceInfo[serviceInfo.length];
        this.serviceInfo = Arrays.copyOf(serviceInfo, serviceInfo.length);
        servicesNum = this.serviceInfo.length;
        services = true;
    }

    void setActivityInfo(ActivityInfo[] receiversInfo) {
        // receivers list derived from activityInfo
        this.receiversInfo = new ActivityInfo[receiversInfo.length];
        this.receiversInfo = Arrays.copyOf(receiversInfo, receiversInfo.length);
        receiversNum = this.receiversInfo.length;
        receivers = true;
    }

    /********************************************************************/
    /*
     * methods
     */

    public String basicEntryPrint() {
        // make this just activity name and package name for InspectorFragment recyclerview
        return "\n" + activityName + "\n(" + packageName + ")\n";
    }

    public String getEntryFeatures() {
        return "RECORD: " + recordable +
                "\nBOOT: " + bootCheck +
                "\nSERVICES: " + services +
                "\nRECEIVERS: " + receivers +
                "\nNUHF/ACR SDK: " + audioSdk +
                "\nACCESSIBILITY: " + accessibility;
    }

    // also has:
    // .packageName
    // .processName
    // .permission
    public String[] getServiceNames() {
        String[] names = new String[serviceInfo.length];
        for (int j = 0; j < serviceInfo.length; j++) {
            // get service name
            names[j] = serviceInfo[j].name;
        }
        return names;
    }
    public String printServiceNames() {
        StringBuilder appServices = new StringBuilder();
        for (ServiceInfo serviceInfo : serviceInfo) {
            // get receiver name
            appServices.append(serviceInfo.name);
            appServices.append("\n");
        }
        return appServices.toString();
    }

    // also has (is ActivityInfo...) :
    // .packageName;
    // .applicationInfo;
    // .processName
    // .targetActivity
    public String[] getReceiverNames() {
        String[] names = new String[receiversInfo.length];
        for (int j = 0; j < receiversInfo.length; j++) {
            // get receiver name
            names[j] = receiversInfo[j].name;
        }
        return names;
    }

    public String printReceiverNames() {
        StringBuilder appReceivers = new StringBuilder();
        for (ActivityInfo activityInfo : receiversInfo) {
            // get receiver name
            appReceivers.append(activityInfo.name);
            appReceivers.append("\n");
        }
        return appReceivers.toString();
    }
}
