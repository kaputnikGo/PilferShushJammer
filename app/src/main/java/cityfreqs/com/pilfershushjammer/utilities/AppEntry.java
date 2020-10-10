package cityfreqs.com.pilfershushjammer.utilities;

import android.content.pm.ActivityInfo;
import android.content.pm.ServiceInfo;

import java.util.Arrays;

public class AppEntry {
    private String activityName;
    private String packageName;
    private int idNum;

    private boolean recordable;
    private boolean bootCheck;
    private boolean receivers;
    private boolean services;
    private boolean audioSdk;
    private boolean accessibility;

    // get names containing SDK name string match
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
        serviceWithSDK = "n/a";
        receiverWithSDK = "n/a";
    }

    /********************************************************************/
    /*
     *
     */

    public String getActivityName() {
        return activityName;
    }
    void setIdNum(int idNum) {
        this.idNum = idNum;
    }

    public int getServicesNum() {
        return servicesNum;
    }
    public int getReceiversNum() {
        return receiversNum;
    }

    void setRecordable(boolean recordable) {
        this.recordable = recordable;
    }
    boolean getRecordable() {
        return recordable;
    }

    void setAccessibility(boolean accessibility) {
        this.accessibility = accessibility;
    }
    boolean getAccessibility() {
        return accessibility;
    }

    void setBootCheck(boolean bootCheck) {
        this.bootCheck = bootCheck;
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
        //TODO change this to be a check for SDK name and AudioRecord only?
        //return (recordable && bootCheck && receivers && services && audioSdk && accessibility);
        return (recordable && audioSdk);
    }

    void setAudioSdk(boolean audioSdk) {
        this.audioSdk = audioSdk;
    }

    boolean getAudioSdk() {
        return audioSdk;
    }

    void setServiceWithSDK(String serviceWithSDK) {
        this.serviceWithSDK = serviceWithSDK;
    }

    String getServiceWithSDK() { return serviceWithSDK; }

    void setReceiverWithSDK(String receiverWithSDK) {
        this.receiverWithSDK = receiverWithSDK;
    }

    String getReceiverWithSDK() { return receiverWithSDK; }

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

    public String entryPrint() {
        //TODO make this stringbuilder with options
        return idNum + " : " + activityName + "\n" + packageName + "\nRECORD: " + recordable +
                "\nBOOT: " + bootCheck + "\nSERVICES: " + services +
                "\nRECEIVERS: " + receivers + "\nNUHF/ACR SDK: " + audioSdk +
                "\nACCESSIBILITY: " + accessibility +
                "\nSERVICE SDK: " + serviceWithSDK +
                "\nRECEIVER SDK: " + receiverWithSDK +
                "\n--------------------------------------\n";
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
}
