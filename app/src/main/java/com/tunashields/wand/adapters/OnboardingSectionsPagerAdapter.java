package com.tunashields.wand.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.tunashields.wand.fragments.OnboardingFragment;

/**
 * Created by Irvin on 8/29/17.
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class OnboardingSectionsPagerAdapter extends FragmentPagerAdapter {

    public OnboardingSectionsPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // Return a OnboardingFragment.
        return OnboardingFragment.newInstance(position);
    }

    @Override
    public int getCount() {
        // Show 4 total pages.
        return 4;
    }
}
