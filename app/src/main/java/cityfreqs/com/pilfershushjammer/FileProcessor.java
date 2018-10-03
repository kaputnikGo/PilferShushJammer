package cityfreqs.com.pilfershushjammer;


import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class FileProcessor {
    protected Context context;
    private String[] audioSdkArray;
    //private String[] userSdkArray;

    //private static final String AUDIO_SDK_FILE_NAME = "audio_sdk_names.txt";

    //private static final String USER_SDK_FILE_NAME = "user_sdk_names.txt";
    //private static final String APP_DIRECTORY_NAME = "BackScan";
    //private File extDirectory;
    //private File userFile;

    protected FileProcessor(Context context) {
        this.context = context;
    }

    protected String[] getAudioSdkArray() {
        // should always be an internal list of size > 1
        //TODO fix the logic here
        if (audioSdkArray == null) {
            // maybe not created yet...
            if (loadAudioSdkList()) {
                return audioSdkArray;
            }
            else {
                // error in finding and loading internal sdk list
                return null;
            }
        }
        else if (audioSdkArray.length > 0)
            return audioSdkArray;
        else {
            // no list made, trigger it
            if (loadAudioSdkList()) {
                return audioSdkArray;
            }
            else {
                // error in finding and loading internal sdk list
                return null;
            }
        }
    }

    /*
    protected String[] getUserSdkArray() {
        // the user list may not exist
        // reset anyway
        userSdkArray = null;
        if (loadUserSdkList()) {
            return userSdkArray;
        }
        // no finding and loading user sdk list
        return null;
    }
    */

    /*
    protected boolean addUserSdkName(String newName) {
        // editText class has android:digits that should sanity the input to [a-zA-Z0-9_]
        // check for empty
        if (newName == null || newName.isEmpty()) {
            return false;
        }
        if (checkUserSdkNameExists(newName)) {
            MainActivity.entryLogger("Name already exists: " + newName, true);
            return false;
        }

        return writeUserSdkName(newName);
    }
    */

    /*
    protected boolean checkUserSdkNameExists(String checkName) {
        // check file loaded first
        if (loadUserSdkList()) {
            for (String name : userSdkArray) {
                if (name.contains(checkName))
                    return true;
            }
        }
        return false;
    }
    */

    /*
    protected boolean deleteUserSdkFile() {
        return deleteUserFile();
    }
    */

/*


 */

    private boolean loadAudioSdkList() {
        // BackScan internal list of audio beacon sdk package names
        try {
            InputStream audioSdkInput = context.getResources().openRawResource(R.raw.audio_sdk_names);
            BufferedReader audioSdkStream = new BufferedReader(new InputStreamReader(audioSdkInput));

            ArrayList<String> audioSdkList = new ArrayList<>();
            String audioSdkLine;
            while ((audioSdkLine = audioSdkStream.readLine()) != null) {
                audioSdkList.add(audioSdkLine);
            }
            // clean up
            audioSdkInput.close();
            audioSdkStream.close();
            // convert list to array
            if (audioSdkList.isEmpty()) {
                return false;
            }
            else {
                audioSdkArray = audioSdkList.toArray(new String[audioSdkList.size()]);
                return true;
            }
        }
        catch (Exception ex) {
            // error
            return false;
        }
    }

    /*
    private boolean loadUserSdkList() {
        // may consist of package names that aren't audio beacon types, ie trackers etc.
        // may also be empty, ie unused
        // grab the file.
        if (!accessWriteDirectory()) {
            return false;
        }
        File location = extDirectory;
        if (location == null) {
            // error
            MainActivity.entryLogger("USER_SDK file location null.", true);
            return false;
        }


        try {
            userFile = new File(location, USER_SDK_FILE_NAME);
            FileInputStream userSdkInput = new FileInputStream(userFile);
            BufferedReader userSdkStream = new BufferedReader(new InputStreamReader(userSdkInput));

            ArrayList<String> userSdkList = new ArrayList<>();
            String userSdkLine;
            while ((userSdkLine = userSdkStream.readLine()) != null) {
                userSdkList.add(userSdkLine);
            }
            // clean up
            userSdkInput.close();
            userSdkStream.close();
            // convert list to array
            if (userSdkList.isEmpty()) {
                return false;
            }
            else {
                userSdkArray = userSdkList.toArray(new String[userSdkList.size()]);
                return true;
            }
        }
        catch (Exception ex) {
            // error
            return false;
        }
    }
    */

    /*
    private boolean writeUserSdkName(String newName) {
        if (accessWriteDirectory()) {
            // grab the file.
            File location = extDirectory;
            if (location == null) {
                // error
                MainActivity.entryLogger("USER_SDK file location error.", true);
                return false;
            }

            try {
                userFile = new File(location, USER_SDK_FILE_NAME);
                if (!userFile.exists()) {
                    userFile.createNewFile();
                }

                FileOutputStream outStream = new FileOutputStream(userFile, true);
                OutputStreamWriter outWriter = new OutputStreamWriter(outStream);
                outWriter.append(newName);
                outWriter.append("\n");

                outWriter.close();
                outStream.flush();
                outStream.close();
                return true;
            }
            catch (IOException ex) {
                // error
                MainActivity.entryLogger("Error writing to USER_SDK file.", true);
                return false;
            }
        }
        else {
            // error with external storage
            MainActivity.entryLogger("Error with access to external storage.", true);
            return false;
        }
    }
    */

    /*
    private boolean deleteUserFile() {
        if (accessWriteDirectory()) {
            // grab the file.
            File location = extDirectory;
            if (location == null) {
                // error
                MainActivity.entryLogger("USER_SDK file location error.", true);
                return false;
            }
            new File(location, USER_SDK_FILE_NAME).delete();
            return true;
        }
        return false;
    }
    */

    /*
    private boolean accessWriteDirectory() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            extDirectory = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), APP_DIRECTORY_NAME);
            if (!extDirectory.exists()) {
                extDirectory.mkdirs();
            }
            return true;
        }
        else {
            return false;
        }
    }
    */

}
