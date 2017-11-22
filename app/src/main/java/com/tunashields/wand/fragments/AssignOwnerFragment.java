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

public class AssignOwnerFragment extends Fragment {

    private EditText mOwnerView;
    private String mCustomOwner;

    private AssignOwnerListener mAssignOwnerListener;

    public AssignOwnerFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_assign_owner, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mOwnerView = (EditText) view.findViewById(R.id.edit_enter_device_owner);
        mOwnerView.requestFocus();
        mOwnerView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_GO) {
                    attemptAssignOwner();
                    return true;
                }
                return false;
            }
        });

        view.findViewById(R.id.button_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptAssignOwner();
            }
        });
    }

    private void attemptAssignOwner() {
        mOwnerView.setError(null);

        mCustomOwner = mOwnerView.getText().toString();

        if (TextUtils.isEmpty(mCustomOwner)) {
            mOwnerView.setError(getString(R.string.error_empty_field));
            mOwnerView.requestFocus();
            return;
        }

        if (mAssignOwnerListener != null) {
            mAssignOwnerListener.onAssignOwner(mCustomOwner);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof AssignOwnerListener) {
            mAssignOwnerListener = (AssignOwnerListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement AssignOwnerListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mAssignOwnerListener = null;
    }

    public interface AssignOwnerListener {
        void onAssignOwner(String owner);
    }
}
