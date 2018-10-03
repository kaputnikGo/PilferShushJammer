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
    //private boolean caution;
    private boolean audioBeacon;

    private int servicesNum;
    private int receiversNum;

    //private int permissionsNum;
    //private String[] requestedPermissions;

    private ServiceInfo[] serviceInfo;
    private ActivityInfo[] receiversInfo;
    //private String[] beaconServices;
    //private String[] beaconReceivers;

    public AppEntry(String packageName, String activityName) {
        this.packageName = packageName;
        this.activityName = activityName;
        // defaults
        recordable = false;
        bootCheck = false;
        receivers = false;
        services = false;
        //caution = false;
        audioBeacon = false;
        servicesNum = 0;
        receiversNum = 0;
        //permissionsNum = 0;
    }

    /********************************************************************/
    /*
     *
     */

    /*
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    public String getPackageName() {
        return packageName;
    }
    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }
    */
    protected String getActivityName() {
        return activityName;
    }
    protected void setIdNum(int idNum) {
        this.idNum = idNum;
    }
    /*
    public int getIdNum() {
        return idNum;
    }
    */

    protected int getServicesNum() {
        return servicesNum;
    }
    protected int getReceiversNum() {
        return receiversNum;
    }
    /*
    public int getPermissionsNum() {
        return permissionsNum;
    }
    */
    /*
     *
     */
    protected void setRecordable(boolean recordable) {
        this.recordable = recordable;
    }
    protected boolean getRecordable() {
        return recordable;
    }

    protected void setBootCheck(boolean bootCheck) {
        this.bootCheck = bootCheck;
    }
    /*
    public boolean getBootCheck() {
        return bootCheck;
    }
    */

    protected void setReceivers(boolean receivers) {
        this.receivers = receivers;
    }
    /*
    public boolean getReceivers() {
        return receivers;
    }
    */

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

    /*
    public boolean getCaution() {
        return caution;
    }
    */

    protected void setAudioBeacon(boolean audioBeacon) {
        this.audioBeacon = audioBeacon;
    }

    protected boolean getAudioBeacon() {
        return audioBeacon;
    }

    /*
    public boolean checkBeaconServiceNames() {
        return (beaconServices != null && beaconServices.length > 0);
    }

    public int getBeaconServiceNamesNum() {
        if (beaconServices != null)
            return beaconServices.length;
        else
            return 0;
    }

    public String[] getBeaconServiceNames() {
        //TODO better the return
        if (beaconServices != null)
            return beaconServices;
        else
            return null;
    }

    public boolean checkBeaconReceiverNames() {
        return (beaconReceivers != null && beaconReceivers.length > 0);
    }

    public int getBeaconReceiverNamesNum() {
        if (beaconReceivers != null)
            return beaconReceivers.length;
        else
            return 0;
    }

    public String[] getBeaconReceiverNames() {
        return beaconReceivers;
    }
    */

    /********************************************************************/
    /*
     *  arrays
     */
    /*
    public void setRequestedPermissions(String[] requestedPermissions) {
        // manifest declared permissions list
        this.requestedPermissions = new String[requestedPermissions.length];
        this.requestedPermissions = Arrays.copyOf(requestedPermissions, requestedPermissions.length);
        permissionsNum = this.requestedPermissions.length;
    }
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
        //String[] beaconServices = new String[serviceInfo.length];
        int i = 0;

        String[] names = new String[serviceInfo.length];
        for (int j = 0; j < serviceInfo.length; j++) {
            // get service name
            names[j] = serviceInfo[j].name;
            if (BackgroundChecker.isSdkName(names[j])) {
                //beaconServices[i] = names[j];
                i++;
            }
        }
        return names;
    }

    // also has (is ActivityInfo...) :
    // .packageName;
    // .applicationInfo;
    // .processName
    // .targetActivity
    protected String[] getReceiverNames() {
        //String[] beaconReceivers = new String[receiversInfo.length];
        int i = 0;

        String[] names = new String[receiversInfo.length];
        for (int j = 0; j < receiversInfo.length; j++) {
            // get receiver name
            names[j] = receiversInfo[j].name;
            if (BackgroundChecker.isSdkName(names[j])) {
                //beaconReceivers[i] = names[j];
                i++;
            }
        }
        return names;
    }
}
