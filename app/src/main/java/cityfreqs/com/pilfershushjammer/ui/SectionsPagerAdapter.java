package cityfreqs.com.pilfershushjammer.ui;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import cityfreqs.com.pilfershushjammer.R;
import cityfreqs.com.pilfershushjammer.utilities.AudioSettings;


public class SectionsPagerAdapter extends FragmentPagerAdapter {

    @StringRes
    private static final int[] TAB_TITLES = new int[]{
            R.string.tab_text_1, R.string.tab_text_2, R.string.tab_text_3};
    private final Context context;

    private Bundle audioBundle;

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
        switch (position) {
            default:
            case 0:
                return HomeFragment.newInstance(audioBundle);
            case 1:
                return InspectorFragment.newInstance(audioBundle);
            case 2:
                return SettingsFragment.newInstance(audioBundle);
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