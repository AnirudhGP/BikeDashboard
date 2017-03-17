package ashwin.bikedisplay;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.hookedonplay.decoviewlib.DecoView;
import com.hookedonplay.decoviewlib.charts.SeriesItem;
import com.hookedonplay.decoviewlib.events.DecoEvent;

import java.lang.ref.WeakReference;

public class BluetoothActivity extends AppCompatActivity {

    private static SharedPreferences sharedPreferences;
    BluetoothService bluetoothService;
    BluetoothDevice device;
    String status;

    CoordinatorLayout coordinatorLayout;

    Snackbar snackTurnOn;

    public static boolean started;
    public static boolean startReading;

    private boolean showMessagesIsChecked = true;
    private boolean autoScrollIsChecked = true;
    public static boolean showTimeIsChecked = true;

    public static int duration;
    public static int delay;
    public static TextView speed;
    public static TextView batt_soc;
    public static TextView temperature;
    public static TextView current;
    public static TextView startTextView;
    public static TextView stopTextView;
    public static TextView energy;
    public static TextView range;
    public static TextView rangeKm;
    public static TextView watts;
    public static TextView rpm;
    public static TextView speed_unit;
    public static ImageView batt_image;
    public static ImageView batt_temp;
    private TextClock textClock;

    public static DecoView arcView;
    public static SeriesItem seriesItem1;
    public static int series1Index;


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

    ImageButton power;
    private static ViewFlipper viewFlipper;
    private static ProgressDialog progress;
    myHandler handler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_homepage);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        started = false;
        progress = new ProgressDialog(this);
        progress.setTitle("Connecting");
        progress.setMessage("Connecting to your Bike");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();
        viewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);
        viewFlipper.setDisplayedChild(0);

        handler = new myHandler(BluetoothActivity.this);

        sharedPreferences = getSharedPreferences("BikeAppPref", MODE_PRIVATE);

//        assert getSupportActionBar() != null; // won't be null, lint error
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        device = getIntent().getExtras().getParcelable(Constants.EXTRA_DEVICE);

        bluetoothService = new BluetoothService(handler, device);
        setTitle(device.getName());

        startTextView = (TextView) findViewById(R.id.startTextView);
        startTextView.setVisibility(View.INVISIBLE);
        stopTextView = (TextView) findViewById(R.id.stopTextView);
        speed = (TextView) findViewById(R.id.speed);
        batt_soc = (TextView) findViewById(R.id.soc);
        temperature = (TextView) findViewById(R.id.temp);
        current = (TextView) findViewById(R.id.current);
        energy = (TextView) findViewById(R.id.energy);
        range = (TextView) findViewById(R.id.range);
        rangeKm = (TextView) findViewById(R.id.range_km);
        watts = (TextView) findViewById(R.id.power);
        rpm = (TextView) findViewById(R.id.rpm);
        speed_unit = (TextView) findViewById(R.id.speed_unit);
        batt_image = (ImageView) findViewById(R.id.batt_soc);
        batt_temp = (ImageView) findViewById(R.id.batt_temp);
        textClock = (TextClock) findViewById(R.id.textClock);

        arcView = (DecoView)findViewById(R.id.rangeArc);
        seriesItem1 = new SeriesItem.Builder(Color.argb(255, 255, 0, 0), Color.argb(255, 64, 255, 64) )
                .setRange(0, 40, 0)
                .setLineWidth(32f)
                .build();

        series1Index = arcView.addSeries(seriesItem1);

        arcView.configureAngles(320, 0);

        startTextView.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Rajdhani-Medium.ttf"));
        stopTextView.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Rajdhani-Medium.ttf"));
        speed.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Rajdhani-Medium.ttf"));
        batt_soc.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Rajdhani-Medium.ttf"));
        temperature.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Rajdhani-Medium.ttf"));
        current.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Rajdhani-Medium.ttf"));
        energy.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Rajdhani-Medium.ttf"));
        range.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Rajdhani-Medium.ttf"));
        rangeKm.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Rajdhani-Medium.ttf"));
        watts.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Rajdhani-Medium.ttf"));
        rpm.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Rajdhani-Medium.ttf"));
        speed_unit.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Rajdhani-Medium.ttf"));
        textClock.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Rajdhani-Medium.ttf"));

        switch(sharedPreferences.getString("Battery", "")) {
            case "100":
                batt_image.setImageResource(R.mipmap.batt_100);
                break;
            case "75":
                batt_image.setImageResource(R.mipmap.batt_75);
                break;
            case "50":
                batt_image.setImageResource(R.mipmap.batt_50);
                break;
            case "25":
                batt_image.setImageResource(R.mipmap.batt_25);
                break;
        }

        startTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("ONCLICK", status);
                startReading();
            }
        });

        stopTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("ONCLICK STOP", status);
                if (bluetoothService != null) {
                    sendMessage("stop");
                    bluetoothService.stopReadingValues();
                    viewFlipper.setDisplayedChild(0);
                    power.setVisibility(View.VISIBLE);
                }
            }
        });

        power = (ImageButton) findViewById(R.id.status);

        power.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewFlipper.setDisplayedChild(2);

            }
        });


//        sharedPreferences = getSharedPreferences("BikeAppPrefs", Context.MODE_PRIVATE);
//
////        ButterKnife.bind(this);
//
//        editText.setError("Enter text first");
//
//        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                if (actionId == EditorInfo.IME_ACTION_SEND) {
//                    send();
//                    return true;
//                }
//                return false;
//            }
//        });
//
//        snackTurnOn = Snackbar.make(coordinatorLayout, "Bluetooth turned off", Snackbar.LENGTH_INDEFINITE)
//                .setAction("Turn On", new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        enableBluetooth();
//                    }
//                });
//
//
//
//        chatAdapter = new ChatAdapter(this);
//        chatListView.setEmptyView(emptyListTextView);
//        chatListView.setAdapter(chatAdapter);
//
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//
//        setSupportActionBar(toolbar);
        startReading = false;
    }

    private void startReading() {
        if (bluetoothService != null) {
            started = false;
            bluetoothService.startReadingValues();
            sendMessage("wakeup\n");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.e("error", e.toString());
            }
            sendMessage("start\n");
            startTextView.setVisibility(View.INVISIBLE);
            progress = new ProgressDialog(BluetoothActivity.this);
            progress.setTitle("Loading");
            progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
            progress.show();
        }
    }

    public static void stopProgressBar() {
        progress.dismiss();
        startTextView.setVisibility(View.VISIBLE);
    }

    public static void startDashboard() {
        progress.dismiss();
        viewFlipper.setDisplayedChild(1);
        startTextView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

        bluetoothService.connect();
        Log.d(Constants.TAG, "Connecting");
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bluetoothService != null) {
            sendMessage("stop");
            bluetoothService.stop();
            Log.d(Constants.TAG, "Stopping");
        }

        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                setStatus("None");
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


    public void sendMessage(String message) {
        Log.d("SENDING", message);
        // Check that we're actually connected before trying anything
        if (bluetoothService.getState() != Constants.STATE_CONNECTED) {
            Log.d("CANT SEND", "NOT CONNECTED");
            return;
        } else {
            byte[] send = message.getBytes();
            bluetoothService.write(send);
        }
    }


    private static class myHandler extends Handler {
        private final WeakReference<BluetoothActivity> mActivity;

        public myHandler(BluetoothActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {

            final BluetoothActivity activity = mActivity.get();

            switch (msg.what) {
                case 619:
                    Log.d("619", "Came ");
                    stopProgressBar();
                    break;

                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case Constants.STATE_CONNECTED:
                            activity.setStatus("Connected");
//                            activity.reconnectButton.setVisible(false);
//                            activity.toolbalProgressBar.setVisibility(View.GONE);
                            break;
                        case Constants.STATE_CONNECTING:
                            activity.setStatus("Connecting");
//                            activity.toolbalProgressBar.setVisibility(View.VISIBLE);
                            break;
                        case Constants.STATE_NONE:
                            activity.setStatus("Not Connected");
//                            activity.toolbalProgressBar.setVisibility(View.GONE);
                            break;
                        case Constants.STATE_ERROR:
                            activity.setStatus("Error");
//                            activity.reconnectButton.setVisible(true);
//                            activity.toolbalProgressBar.setVisibility(View.GONE);
                            break;
                    }
                    break;

//                case Constants.MESSAGE_WRITE:
//                    byte[] writeBuf = (byte[]) msg.obj;
//                    // construct a string from the buffer
//                    String writeMessage = new String(writeBuf);
//                    ChatMessage messageWrite = new ChatMessage("Me", writeMessage);
//                    activity.addMessageToAdapter(messageWrite);
//                    break;
                case Constants.MESSAGE_READ:

                    String readMessage = (String) msg.obj;

                    if (readMessage != null && activity.showMessagesIsChecked) {

                        Log.d("MESSAGE RECEIVED", readMessage.trim());
//                        ChatMessage messageRead = new ChatMessage(activity.device.getName(), readMessage.trim());
//                        activity.addMessageToAdapter(messageRead);
                        String values[] = readMessage.trim().split("\n");
                        SharedPreferences.Editor editor = sharedPreferences.edit();

                        if(values.length >= 1) {
                            if (!started) {
                                startDashboard();
                                started = true;
                                duration = 4000;
                                delay = 100;
                            }

                            double currAvg = 0;
                            double socAvg = 0;
                            double tempAvg = 0;
                            double speedAvg = 0;
                            double rangeAvg = 0;
                            int count = 0;
                            int num_of_values = 5;

                            int curr = 0, soc = 0, temp = 0, sp = 0;
                            float rang = 0F;
                            boolean startedFlag = false;
                            for(int i = 0; i < values.length; i++) {
                                String currValues[] = values[i].split(",");
                                try {
                                    if (currValues.length == num_of_values) {
                                        count++;
                                        currAvg += Double.parseDouble(currValues[0]);
                                        socAvg += Double.parseDouble(currValues[1]);
                                        tempAvg += Double.parseDouble(currValues[2]);
                                        speedAvg += Double.parseDouble(currValues[3]);
                                        rangeAvg += Double.parseDouble(currValues[4]);
                                    } else if (currValues.length == 3) {
                                        if(currValues[0].equals("started")) {
                                            Log.d("STARTED", "STARTED");
                                            startedFlag = true;
                                            startReading = true;
                                            soc = (int) Double.parseDouble(currValues[1]);
                                            rang = (float) Double.parseDouble(currValues[2]);
                                        }
                                    }
                                } catch (NumberFormatException e) {
                                    Log.e("Format error", e.toString());
                                }
                            }

                            if (!startReading) {
                                break;
                            }
                            if (count != 0 && !startedFlag) {
                                 curr = (int) (currAvg / count);
                                 soc = (int) (socAvg / count);
                                 temp = (int) (tempAvg / count);
                                 sp = (int) (speedAvg / count);
                                 rang = (float) (rangeAvg / count);
                            }
                            if (startedFlag ) {
                                startedFlag = false;
                            }
                            editor.putString("Battery", soc + "");
                            Log.d("BATTERY", sharedPreferences.getString("Battery", ""));
                            editor.commit();

                            switch(soc) {
                                case 100:
                                    batt_image.setImageResource(R.mipmap.batt_100);
                                    break;
                                case 75:
                                    batt_image.setImageResource(R.mipmap.batt_75);
                                    break;
                                case 50:
                                    batt_image.setImageResource(R.mipmap.batt_50);
                                    break;
                                case 25:
                                    batt_image.setImageResource(R.mipmap.batt_25);
                                    break;
                            }

                            if(temp <= 35) {
                                batt_temp.setImageResource(R.drawable.batt_temp_cool);
                            } else if (temp > 35) {
                                batt_temp.setImageResource(R.drawable.batt_temp_hot);
                            }



                            current.setText(curr + "A");
                            batt_soc.setText(soc + "%");
                            temperature.setText(temp + "\u00b0" + "C");
                            speed.setText(sp + "");
                            range.setText((int)rang + "");

                            Log.d("RANGE", rang + "");
                            if (rang > 20) {
                                arcView.addEvent(new DecoEvent.Builder(rang)
                                        .setIndex(series1Index)
                                        .setDuration(duration)
                                        .setDelay(delay)
//                                        .setColor(Color.parseColor("#00FF00"))
                                        .build());
                            } else if (rang > 10 && rang <= 20) {
                                arcView.addEvent(new DecoEvent.Builder(rang)
                                        .setIndex(series1Index)
                                        .setDuration(duration)
//                                        .setColor(Color.parseColor("#FFA500"))
                                        .build());
                            } else {
                                arcView.addEvent(new DecoEvent.Builder(rang)
                                        .setIndex(series1Index)
                                        .setDuration(duration)
//                                        .setColor(Color.parseColor("#FF0000"))
                                        .build());
                            }
                            if (duration == 4000) {
                                duration = 100;
                                delay = 0;
                            }
                        }
                    }

                    break;

                case Constants.MESSAGE_SNACKBAR:
//                    Snackbar.make(activity.coordinatorLayout, msg.getData().getString(Constants.SNACKBAR), Snackbar.LENGTH_LONG)
//                            .setAction("Connect", new View.OnClickListener() {
//                                @Override
//                                public void onClick(View v) {
//                                    activity.reconnect();
//                                }
//                            }).show();

                    break;
            }
        }


    }
//
//    private void addMessageToAdapter(ChatMessage chatMessage) {
////        chatAdapter.add(chatMessage);
//        if (autoScrollIsChecked) scrollChatListViewToBottom();
//    }

    private void scrollChatListViewToBottom() {
//        chatListView.post(new Runnable() {
//            @Override
//            public void run() {
//                // Select the last row so it will scroll into view...
//                chatListView.smoothScrollToPosition(chatAdapter.getCount() - 1);
//            }
//        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        snackTurnOn.show();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        if (snackTurnOn.isShownOrQueued()) snackTurnOn.dismiss();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        reconnect();
                }
            }
        }
    };

    private void setStatus(String status) {
        this.status = status;
//        toolbar.setSubtitle(status);
    }

    private void enableBluetooth() {
        setStatus("Enabling Bluetooth");
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
    }

    private void reconnect() {
//        reconnectButton.setVisible(false);
        bluetoothService.stop();
        bluetoothService.connect();
    }

}
