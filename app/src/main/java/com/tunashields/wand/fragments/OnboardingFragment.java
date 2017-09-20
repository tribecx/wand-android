package com.tunashields.wand.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tunashields.wand.R;

/**
 * Created by Irvin on 8/29/17.
 * A placeholder fragment containing a simple view.
 */

public class OnboardingFragment extends Fragment {

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    private int sectionNumber;

    public OnboardingFragment() {
        // Required empty public constructor
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static OnboardingFragment newInstance(int sectionNumber) {
        OnboardingFragment fragment = new OnboardingFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            sectionNumber = getArguments().getInt(ARG_SECTION_NUMBER);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_onboarding, container, false);

        int mSectionResources[] = {
                R.drawable.img_onboarding_section_0,
                R.drawable.img_onboarding_section_1,
                R.drawable.img_onboarding_section_2,
                R.drawable.img_onboarding_section_3
        };

        int mSectionStrings[] = {
                R.string.label_onboarding_section_0,
                R.string.label_onboarding_section_1,
                R.string.label_onboarding_section_2,
                R.string.label_onboarding_section_3
        };

        ImageView imageView = rootView.findViewById(R.id.section_image);
        imageView.setImageResource(mSectionResources[sectionNumber]);

        TextView textView = rootView.findViewById(R.id.section_label);
        textView.setText(mSectionStrings[sectionNumber]);
        return rootView;
    }
}
