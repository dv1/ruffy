package org.monkey.d.ruffy.ruffy;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class Start extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_start);

        checkExternalStoragePermission();

        getSupportFragmentManager().beginTransaction().add(R.id.container,new MainFragment()).commit();

        SharedPreferences prefs = getSharedPreferences("pumpdata", MODE_PRIVATE);
        boolean paired = prefs.getBoolean("paired", false);
        if (!paired) {
            getSupportFragmentManager().beginTransaction().replace(R.id.container,new SetupFragment()).addToBackStack("Start").commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_start, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private static final int EXTERNAL_STORAGE_REQUEST = 100;

    private void checkExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case EXTERNAL_STORAGE_REQUEST: {
                if ((grantResults.length == 0) || (grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, "External storage permission is required in order to produce binary data dumps", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
