package com.tunashields.wand.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.tunashields.wand.R;

/**
 * Created by Irvin on 9/20/17.
 */

public class ProgressDialogFragment extends DialogFragment {

    private static final String ARG_MESSAGE = "message";

    private String mMessage = null;

    private OnCancelClickListener mOnCancelClickListener;

    public static ProgressDialogFragment newInstance(String mMessage) {
        ProgressDialogFragment fragment = new ProgressDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE, mMessage);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialog);
        if (getArguments() != null) {
            mMessage = getArguments().getString(ARG_MESSAGE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.progress_dialog, container, false);
        Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate);
        view.findViewById(R.id.image_progress).setAnimation(animation);
        ((TextView) view.findViewById(R.id.text_progress_message)).setText(mMessage);
        view.findViewById(R.id.button_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnCancelClickListener != null)
                    mOnCancelClickListener.onCancel();
            }
        });
        return view;
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

    public interface OnCancelClickListener {
        void onCancel();
    }

    public void setOnCancelClickListener(OnCancelClickListener mOnCancelClickListener) {
        this.mOnCancelClickListener = mOnCancelClickListener;
    }
}
