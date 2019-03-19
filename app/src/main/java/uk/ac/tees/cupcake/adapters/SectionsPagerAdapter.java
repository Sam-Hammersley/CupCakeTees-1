package uk.ac.tees.cupcake.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

import uk.ac.tees.cupcake.home.HomeFragment;
import uk.ac.tees.cupcake.home.NewsFeedFragment;
import uk.ac.tees.cupcake.home.ProfileFragment;

/**
 * SectionPager Adapter
 * @author Hugo Tomas <s6006225@live.tees.ac.uk>
 */

public class SectionsPagerAdapter extends FragmentPagerAdapter {

    private static final String TAG = "SectionPagerAdapter";

    private final List<Fragment> fragments;

    public SectionsPagerAdapter(FragmentManager fm, List<Fragment> fragments) {
        super(fm);
        this.fragments = fragments;
    }

    /**
     * @param position of the selected item in navigation bar
     * @return selected fragment
     */
    @Override
    public Fragment getItem(int position) {
        Log.d(TAG, "getItem: getting fragment at position: " + position);

        return fragments.get(position);
    }

    /**
     * @return amount of fragments
     */
    @Override
    public int getCount() {
        return fragments.size();
    }

}