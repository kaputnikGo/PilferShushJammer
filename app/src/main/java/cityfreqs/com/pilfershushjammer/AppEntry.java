package cityfreqs.com.pilfershushjammer;

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
    private boolean audioBeacon;

    private int servicesNum;
    private int receiversNum;

    private ServiceInfo[] serviceInfo;
    private ActivityInfo[] receiversInfo;

    public AppEntry(String packageName, String activityName) {
        this.packageName = packageName;
        this.activityName = activityName;
        // defaults
        recordable = false;
        bootCheck = false;
        receivers = false;
        services = false;
        audioBeacon = false;
        servicesNum = 0;
        receiversNum = 0;
    }

    /********************************************************************/
    /*
     *
     */

    protected String getActivityName() {
        return activityName;
    }
    protected void setIdNum(int idNum) {
        this.idNum = idNum;
    }

    protected int getServicesNum() {
        return servicesNum;
    }
    protected int getReceiversNum() {
        return receiversNum;
    }

    protected void setRecordable(boolean recordable) {
        this.recordable = recordable;
    }
    protected boolean getRecordable() {
        return recordable;
    }

    protected void setBootCheck(boolean bootCheck) {
        this.bootCheck = bootCheck;
    }

    protected void setReceivers(boolean receivers) {
        this.receivers = receivers;
    }

    protected void setServices(boolean services) {
        this.services = services;
    }
    protected boolean getServices() {
        return services;
    }

    protected boolean checkForCaution() {
        // set and return,
        // called by BackgroundChecker.appEntryLog(),
        // later boolean is checked for in-depth scanning of services, receivers, etc.
        return (recordable && bootCheck && receivers && services);
    }

    protected void setAudioBeacon(boolean audioBeacon) {
        this.audioBeacon = audioBeacon;
    }

    protected boolean getAudioBeacon() {
        return audioBeacon;
    }


    /********************************************************************/
    /*
     *  arrays
     */

    protected void setServiceInfo(ServiceInfo[] serviceInfo) {
        // any background recording service list
        this.serviceInfo = new ServiceInfo[serviceInfo.length];
        this.serviceInfo = Arrays.copyOf(serviceInfo, serviceInfo.length);
        servicesNum = this.serviceInfo.length;
        services = true;
    }

    protected void setActivityInfo(ActivityInfo[] receiversInfo) {
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
    public String toString() {
        return idNum + " : " + activityName + "\n" + packageName + "\nRECORD: " + recordable +
                "\nBOOT: " + bootCheck + "\nSERVICES: " + services +
                "\nRECEIVERS: " + receivers + "\nNUHF SDK: " + audioBeacon +
                "\n--------------------------------------\n";
    }

    // also has:
    // .packageName
    // .processName
    // .permission
    protected String[] getServiceNames() {
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
    protected String[] getReceiverNames() {
        String[] names = new String[receiversInfo.length];
        for (int j = 0; j < receiversInfo.length; j++) {
            // get receiver name
            names[j] = receiversInfo[j].name;
        }
        return names;
    }
}
