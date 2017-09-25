package com.tunashields.wand.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
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

public class AssignNameFragment extends Fragment {

    private EditText mNameView;
    private String mCustomName = null;

    private AssignNameListener mAssignNameListener;

    public AssignNameFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_assign_name, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mNameView = view.findViewById(R.id.edit_enter_device_name);
        mNameView.requestFocus();
        mNameView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_GO) {
                    attemptAssignName();
                    return true;
                }
                return false;
            }
        });

        view.findViewById(R.id.button_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptAssignName();
            }
        });
    }

    private void attemptAssignName() {
        mNameView.setError(null);
        mCustomName = mNameView.getText().toString();

        if (TextUtils.isEmpty(mCustomName)) {
            mNameView.setError(getString(R.string.error_empty_field));
            mNameView.requestFocus();
            return;
        }

        if (mAssignNameListener != null) {
            mAssignNameListener.onAssignName(mCustomName);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof AssignNameListener) {
            mAssignNameListener = (AssignNameListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement AssignNameListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mAssignNameListener = null;
    }

    public interface AssignNameListener {
        void onAssignName(String name);
    }
}
