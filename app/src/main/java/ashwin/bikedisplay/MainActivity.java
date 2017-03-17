package ashwin.bikedisplay;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemClick;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter bluetoothAdapter;

    BluetoothDevicesAdapter bluetoothDevicesAdapter;
    ProgressDialog progress;

    public static Button retryConnectButton;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.devices_list_view)
    ListView devicesListView;
    @BindView(R.id.empty_list_item)
    TextView emptyListTextView;
    @BindView(R.id.toolbar_progress_bar)
    ProgressBar toolbarProgressCircle;
    @BindView(R.id.coordinator_layout_main)
    CoordinatorLayout coordinatorLayout;

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission. ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission. ACCESS_COARSE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Grant location access")
                        .setMessage("The app needs location access to connect to the Bike")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission. ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission. ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        searchForDevice();
                    }

                } else {
                    Toast.makeText(getApplicationContext(), "The app cannot work without location permission", Toast.LENGTH_SHORT).show();
                    finish();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return;
            }

        }
    }

    private void enableBluetooth() {
        setStatus("Enabling Bluetooth");
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
    }

    @OnItemClick(R.id.devices_list_view) void onItemClick(int position) {
        setStatus("Asking to connect");
        final BluetoothDevice device = bluetoothDevicesAdapter.getItem(position);

        Log.d(Constants.TAG, "Opening new Activity");
        bluetoothAdapter.cancelDiscovery();
        toolbarProgressCircle.setVisibility(View.INVISIBLE);

        Intent intent = new Intent(MainActivity.this, BluetoothActivity.class);
        intent.putExtra(Constants.EXTRA_DEVICE, device);
        startActivity(intent);
    }

    public static boolean flag;

    public void timerDelayRemoveDialog(long time, final Dialog d, final Button button){
        new Handler().postDelayed(new Runnable() {
            public void run() {
                if(d.isShowing()) {
                    d.dismiss();
                    button.setVisibility(View.VISIBLE);
                    if (button.getId() == R.id.retrySearchButton) {
                        if(bluetoothAdapter != null) {
                            bluetoothAdapter.cancelDiscovery();
                        }
                    }
                }
            }
        }, time);
    }

    public void searchForDevice() {
        retryConnectButton.setVisibility(View.INVISIBLE);
        progress = new ProgressDialog(this);
        progress.setTitle("Please Wait");
        progress.setMessage("Searching for your Bike");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();
        timerDelayRemoveDialog(5000, progress, retryConnectButton);

        if (bluetoothAdapter.isEnabled()) {
            // Bluetooth enabled
            startSearching();
        } else {
            enableBluetooth();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        flag = false;

        retryConnectButton = (Button) findViewById(R.id.retrySearchButton);
        retryConnectButton.setVisibility(View.INVISIBLE);

        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        setStatus("None");

        bluetoothDevicesAdapter = new BluetoothDevicesAdapter(this);

        devicesListView.setAdapter(bluetoothDevicesAdapter);
        devicesListView.setEmptyView(emptyListTextView);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {

            Log.e(Constants.TAG, "Device has no bluetooth");
            new AlertDialog.Builder(MainActivity.this)
                    .setCancelable(false)
                    .setTitle("No Bluetooth")
                    .setMessage("Your device has no bluetooth")
                    .setPositiveButton("Close app", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(Constants.TAG, "App closed");
                            finish();
                        }
                    }).show();

        } else {
            bluetoothAdapter.cancelDiscovery();
        }

        if (checkLocationPermission()) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission. ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                searchForDevice();
            }
        }
        retryConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("RETRY", "Connecting");
                searchForDevice();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(Constants.TAG, "Registering receiver");
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery();
        }
        Log.d(Constants.TAG, "Receiver unregistered");
        unregisterReceiver(mReceiver);
    }


    private void setStatus(String status) {
        Log.d("STATUS", status);
        toolbar.setSubtitle(status);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                startSearching();
            } else {
                setStatus("Error");
                Snackbar.make(coordinatorLayout, "Failed to enable bluetooth", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Try Again", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                enableBluetooth();
                            }
                        }).show();
            }
        }
    }

    private void startSearching() {
        if (bluetoothAdapter.startDiscovery()) {
            toolbarProgressCircle.setVisibility(View.VISIBLE);
            setStatus("Searching for devices");
        } else {
            setStatus("Error");
            Snackbar.make(coordinatorLayout, "Failed to start searching", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Try Again", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startSearching();
                        }
                    }).show();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                device.fetchUuidsWithSdp();

                if (bluetoothDevicesAdapter.getPosition(device) == -1) {
                    // -1 is returned when the item is not in the adapter
                    if (!flag) {
                        progress.dismiss();
                        flag = true;
                    }
                    bluetoothDevicesAdapter.add(device);
                    Log.d("FOUND", device.getName());
                    bluetoothDevicesAdapter.notifyDataSetChanged();
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d("GAGA","FINISHED");
                toolbarProgressCircle.setVisibility(View.INVISIBLE);
                setStatus("None");

            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.d("GAGA","STARTED");
            }
            else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Snackbar.make(coordinatorLayout, "Bluetooth turned off", Snackbar.LENGTH_INDEFINITE)
                                .setAction("Turn on", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        enableBluetooth();
                                    }
                                }).show();
                        break;
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();



    }
}


