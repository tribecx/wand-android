package com.tunashields.wand.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tunashields.wand.R;

/**
 * Created by Irvin on 9/20/17.
 */

public class ErrorDialogFragment extends DialogFragment {
    private static final String ARG_SHOW_TIPS = "show_tips";

    private boolean mShowTips = false;

    public static ErrorDialogFragment newInstance(boolean mShowTips) {
        ErrorDialogFragment fragment = new ErrorDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_SHOW_TIPS, mShowTips);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialog);
        if (getArguments() != null) {
            mShowTips = getArguments().getBoolean(ARG_SHOW_TIPS);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.error_dialog_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FloatingActionButton mShowTipsButton = view.findViewById(R.id.fab_tips);
        mShowTipsButton.setVisibility(mShowTips ? View.VISIBLE : View.GONE);
        mShowTipsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TipsDialogFragment mTipsDialogFragment = TipsDialogFragment.newInstance();
                mTipsDialogFragment.show(getActivity().getSupportFragmentManager(), "tips_fragment");
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            if (dialog.getWindow() != null)
                dialog.getWindow().setLayout(width, height);
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        getActivity().finish();
    }
}
