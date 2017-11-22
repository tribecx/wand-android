package com.tunashields.wand.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.tunashields.wand.R;

/**
 * Created by Irvin on 9/13/17.
 */

public class AssignPasswordFragment extends Fragment {

    private EditText mPasswordView;
    private String mCustomPassword;

    private AssignPasswordListener mAssignPasswordListener;

    public AssignPasswordFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_assign_password, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPasswordView = (EditText) view.findViewById(R.id.edit_enter_device_password);
        mPasswordView.requestFocus();
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_GO) {
                    attemptAssignPassword();
                    return true;
                }
                return false;
            }
        });

        view.findViewById(R.id.button_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptAssignPassword();
            }
        });
    }

    private void attemptAssignPassword() {
        mPasswordView.setError(null);

        mCustomPassword = mPasswordView.getText().toString();

        if (mCustomPassword.length() == 5) {
            if (mAssignPasswordListener != null) {
                mAssignPasswordListener.onAssignPassword(mCustomPassword);
            }
        } else {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            mPasswordView.requestFocus();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof AssignPasswordListener) {
            mAssignPasswordListener = (AssignPasswordListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement AssignPasswordListener");
        }
    }

    public interface AssignPasswordListener {
        void onAssignPassword(String password);
    }
}
