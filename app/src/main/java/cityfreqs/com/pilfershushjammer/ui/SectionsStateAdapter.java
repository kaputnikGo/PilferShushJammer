package cityfreqs.com.pilfershushjammer.ui;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import cityfreqs.com.pilfershushjammer.R;
import cityfreqs.com.pilfershushjammer.utilities.AudioSettings;


public class SectionsStateAdapter extends FragmentStateAdapter {

    @StringRes
    private static final int[] TAB_TITLES = new int[]{
            R.string.tab_text_1, R.string.tab_text_2, R.string.tab_text_3};
    private final Context context;
    private final Bundle audioBundle;

    public SectionsStateAdapter(final Context context, final FragmentActivity fa) {
        super(fa);
        this.context = context;
        audioBundle = new Bundle();
        //permissions check boolean
        audioBundle.putBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[16], false);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // createFragment is called to instantiate the fragment.
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
    public CharSequence getPageTitle(int position) {
        return context.getString(TAB_TITLES[position]);
    }

    @Override
    public int getItemCount() {
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
