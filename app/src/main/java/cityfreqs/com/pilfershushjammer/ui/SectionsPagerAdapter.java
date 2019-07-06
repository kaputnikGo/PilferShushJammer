package cityfreqs.com.pilfershushjammer.ui;

import android.content.Context;
import android.os.Bundle;

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
    private HomeFragment homeFragment;
    private InspectorFragment inspectorFragment;
    private SettingsFragment settingsFragment;
    private Bundle audioBundle;


    public SectionsPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        this.context = context;
        audioBundle = new Bundle();
        homeFragment = new HomeFragment(audioBundle);
        inspectorFragment = new InspectorFragment(audioBundle);
        settingsFragment = new SettingsFragment(audioBundle);
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment.
        switch (position) {
            case 0:
                return homeFragment;
            case 1:
                return inspectorFragment;
            case 2:
                return settingsFragment;
            default:
                return null;
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
}