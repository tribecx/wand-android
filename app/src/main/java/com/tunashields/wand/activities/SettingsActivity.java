package com.tunashields.wand.activities;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.tunashields.wand.R;

import java.io.InputStream;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
        setContentView(R.layout.activity_settings);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(getString(R.string.label_settings));
        setSupportActionBar(mToolbar);

        ActionBar mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    public void startInformationActivity(View view) {
        Intent intent = new Intent(SettingsActivity.this, InformationActivity.class);
        switch (view.getId()) {
            case R.id.text_about_us:

                try {
                    Resources res = getResources();
                    InputStream inputStream = res.openRawResource(R.raw.about_us);
                    byte[] b = new byte[inputStream.available()];
                    inputStream.read(b);
                    String content = new String(b);

                    intent.putExtra(InformationActivity.TITLE_KEY, getString(R.string.label_about_us));
                    intent.putExtra(InformationActivity.CONTENT_KEY, content);
                    startActivity(intent);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            case R.id.text_service_conditions:

                try {
                    Resources res = getResources();
                    InputStream inputStream = res.openRawResource(R.raw.service_conditions);
                    byte[] b = new byte[inputStream.available()];
                    inputStream.read(b);
                    String content = new String(b);

                    intent.putExtra(InformationActivity.TITLE_KEY, getString(R.string.label_service_conditions));
                    intent.putExtra(InformationActivity.CONTENT_KEY, content);
                    startActivity(intent);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
        }
    }
}
