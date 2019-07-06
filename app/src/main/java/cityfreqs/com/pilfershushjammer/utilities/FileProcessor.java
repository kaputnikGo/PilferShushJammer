package cityfreqs.com.pilfershushjammer.utilities;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import cityfreqs.com.pilfershushjammer.R;

public class FileProcessor {
    protected Context context;
    private String[] audioSdkArray;

    FileProcessor(Context context) {
        this.context = context;
    }

    String[] getAudioSdkArray() {
        // should always be an internal list of size > 1
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
                audioSdkArray = audioSdkList.toArray(new String[0]); // audioSdkList.size()
                return true;
            }
        }
        catch (Exception ex) {
            // error
            return false;
        }
    }
}
