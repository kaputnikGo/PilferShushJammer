package cityfreqs.com.pilfershushjammer.ui;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import cityfreqs.com.pilfershushjammer.R;
import cityfreqs.com.pilfershushjammer.utilities.AudioSettings;


public class SectionsPagerAdapter extends FragmentPagerAdapter {

    private static final String TAG = "PSJAM_FRAGPAGER";
    @StringRes
    private static final int[] TAB_TITLES = new int[]{
            R.string.tab_text_1, R.string.tab_text_2, R.string.tab_text_3};
    private final Context context;

    private final Bundle audioBundle;
    private FragmentManager fragmentManager;

    public SectionsPagerAdapter(Context context, FragmentManager fm) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.context = context;
        audioBundle = new Bundle();
        //permissions check boolean
        audioBundle.putBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[16], false);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment.
        // findFragmentById returns the fragment if found or null otherwise.
        HomeFragment homeFragment = null;
        InspectorFragment inspectorFragment = null;
        SettingsFragment settingsFragment = null;
        switch (position) {
            default:
            case 0:
                try {
                    homeFragment = (HomeFragment) fragmentManager.findFragmentById(R.id.home_layout);
                } catch (NullPointerException e) {
                    // dont stack print, first time run will trip this always.
                    Log.d(TAG, "homeFragment is null ex");
                }
                if (homeFragment == null)
                    return HomeFragment.newInstance(audioBundle);
                else {
                    return homeFragment;
                }
            case 1:
                try {
                inspectorFragment = (InspectorFragment)fragmentManager.findFragmentById(R.id.inspector_layout);
                } catch (NullPointerException e) {
                    // dont stack print, first time run will trip this always.
                    Log.d(TAG, "inspectorFragment is null ex");
                }
                if (inspectorFragment == null) {
                    return InspectorFragment.newInstance(audioBundle);
                }
                else {
                    return inspectorFragment;
                }
            case 2:
                try {
                settingsFragment = (SettingsFragment)fragmentManager.findFragmentById(R.id.settings_layout);
                } catch (NullPointerException e) {
                    // dont stack print, first time run will trip this always.
                    Log.d(TAG, "settingsFragment is null ex");
                }
                if (settingsFragment == null) {
                    return SettingsFragment.newInstance(audioBundle);
                }
                else {
                    return settingsFragment;
                }
        }

    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return context.getResources().getString(TAB_TITLES[position]);
    }

    @Override
    public int getCount() {
        // Show 3 total fragments.
        return 3;
    }

    public void setDebugBoolean(boolean setBoolean) {
        audioBundle.putBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15], setBoolean);
    }

    public void permissionCheckPassed(boolean permissionCheck) {
        audioBundle.putBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[16], permissionCheck);
    }
}