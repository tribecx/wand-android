package com.tunashields.wand.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tunashields.wand.R;
import com.tunashields.wand.activities.MainActivity;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Irvin on 9/20/17.
 */

public class DoneDialogFragment extends DialogFragment {

    private static final String ARG_MESSAGE = "message";

    private String mMessage = null;

    public static DoneDialogFragment newInstance(String mMessage) {
        DoneDialogFragment fragment = new DoneDialogFragment();
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
        return inflater.inflate(R.layout.done_dialog_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((TextView) view.findViewById(R.id.text_done_dialog_message)).setText(mMessage);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                for (int i = 0; i < fragmentManager.getBackStackEntryCount(); i++) {
                    fragmentManager.popBackStack();
                }
                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getActivity().startActivity(intent);
                getActivity().finishAffinity();
                dismiss();
            }
        };
        Timer timer = new Timer();
        timer.schedule(task, 1500);
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
}
