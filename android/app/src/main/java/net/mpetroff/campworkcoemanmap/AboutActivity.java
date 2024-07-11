package net.mpetroff.campworkcoemanmap;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        TextView version = (TextView) findViewById(R.id.about_version);
        try {
            version.append(" " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            version.append(" ???");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void openAppCopyright(View view) {
        startActivity(new Intent(this, AppCopyrightActivity.class));
    }

    public void openMapDataCopyright(View view) {
        startActivity(new Intent(this, MapDataCopyrightActivity.class));
    }

    public void openPrivacyPolicy(View view) {
        startActivity(new Intent(this, PrivacyPolicyActivity.class));
    }
}
