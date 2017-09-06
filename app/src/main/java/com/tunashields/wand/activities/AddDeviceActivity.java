package com.tunashields.wand.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.tunashields.wand.R;

public class AddDeviceActivity extends AppCompatActivity {

    ImageView mProgressView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);
        mProgressView = (ImageView) findViewById(R.id.image_looking_devices);
        showProgress();
    }

    private void showProgress() {
        mProgressView.setVisibility(View.VISIBLE);
        Animation animation = AnimationUtils.loadAnimation(AddDeviceActivity.this, R.anim.rotate);
        mProgressView.setAnimation(animation);
    }

    private void hideProgress() {
        mProgressView.setVisibility(View.GONE);
    }
}
