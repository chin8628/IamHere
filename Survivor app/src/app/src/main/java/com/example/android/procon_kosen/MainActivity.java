package com.example.android.procon_kosen;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private Button soundButton;
    private AudioManager am;
    private SharedPreferences sharedpreferences;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;
    private MediaPlayer mp;
    private NotificationBar nb;
    private boolean soundActive = false;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Start Background Wifi Service
        Intent service = new Intent(this, WiFiScanner.class);
        startService(service);

        handler = new Handler();
        FirstTimeVisitClass visit = new FirstTimeVisitClass(this);
        if (!visit.getVisited()) {
            startActivity(new Intent(MainActivity.this, IntroSlide.class));
        }

        setContentView(R.layout.activity_main);

        //Request Location and Boot permission if not already granted
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            new AlertDialog.Builder(this)
                    .setTitle("Request Permission")
                    .setMessage("The application will request location access in order to locate your phone.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1002);
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECEIVE_BOOT_COMPLETED}, 1003);
                            Intent k = new Intent();
                            String packageName = MainActivity.this.getPackageName();
                            PowerManager pm = (PowerManager) MainActivity.this.getSystemService(Context.POWER_SERVICE);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (!pm.isIgnoringBatteryOptimizations(packageName)){
                                    k.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                                    k.setData(Uri.parse("package:" + packageName));
                                    startActivity(k);
                                }
                            }
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert).show();

            handler.post(runnableCode);
        }

        //Initialize audio object
        nb = new NotificationBar(this);
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);


        //Initialize Ui object
        TextView name = (TextView) findViewById(R.id.name);
        TextView birthday = (TextView) findViewById(R.id.birthday);
        TextView blood = (TextView) findViewById(R.id.blood);
        TextView sibling1 = (TextView) findViewById(R.id.sibling_phone1);
        TextView sibling2 = (TextView) findViewById(R.id.sibling_phone2);

        sharedpreferences = getSharedPreferences("contentProfile", Context.MODE_PRIVATE);
        name.setText(sharedpreferences.getString("name", ""));
        birthday.setText(sharedpreferences.getString("birthday", ""));
        blood.setText(sharedpreferences.getString("blood", ""));
        sibling1.setText(sharedpreferences.getString("sibling1", ""));
        sibling2.setText(sharedpreferences.getString("sibling2", ""));

        Button editBtn = (Button) findViewById(R.id.edit_btn);
        editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, EditProfile.class);
                startActivity(i);
            }
        });

        soundButton = (Button) findViewById(R.id.soundButton);
        soundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!soundActive) {
                    mp = MediaPlayer.create(MainActivity.this, R.raw.loudalarm);
                    mp.setLooping(true);
                    soundActive = true;
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
                    mp.start();
                    mNotificationManager.notify(512, mBuilder.build());
                    //nb.show();
                    soundButton.setText(R.string.silence_btn);
                    soundButton.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.ic_volume_off_black_24dp,0,0,0);
                }
                else {
                    soundActive = false;
                    mp.stop();
                    //mNotificationManager.cancel(512);
                    nb.hide();
                    soundButton.setText(R.string.alarm_btn);
                    soundButton.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.ic_volume_up_black_24dp,0,0,0);
                    Intent j = new Intent("slience b");
                    sendBroadcast(j);
                }
            }
        });

        StringBuilder sb = new StringBuilder();
        sb.append("You have been detected.\n");
        sb.append("Personal Details\n");
        sb.append("Name: ").append(sharedpreferences.getString("name", "")).append("\n");
        sb.append("Blood Type: ").append(sharedpreferences.getString("blood", "")).append("\n");
        sb.append("Contact: ");
        sb.append(sharedpreferences.getString("sibling1", ""));
        sb.append(" ");
        sb.append(sharedpreferences.getString("sibling2", ""));

        //Build notification
        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("IamHere")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(false)
                .setAutoCancel(false)
                .setContentText(sb.toString());

        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle("IamHere");
        bigTextStyle.bigText(sb.toString());
        mBuilder.setStyle(bigTextStyle);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //register reliever from service
        registerReceiver(mMessageReceiver, new IntentFilter("command recived"));

        //Broadcast to service that the application is running
        Intent mainBroadcaster = new Intent("mainBroadcaster");
        mainBroadcaster.putExtra("mainstatus", true);
        sendBroadcast(mainBroadcaster);


        handler.postDelayed(runnableCode, 5000);

    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Intent service = new Intent(this, WiFiScanner.class);
        startService(service);
        //Broadcast to service that the application is no longer running
        Intent mainBroadcaster = new Intent("mainBroadcaster");
        mainBroadcaster.putExtra("mainstatus", false);
        sendBroadcast(mainBroadcaster);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }


    @Override
    protected void onResume()
    {
        super.onResume();
        //Broadcast to service that the application is no longer running
        handler.post(runnableCode);
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        //Broadcast to service that the application is no longer running
        handler.post(runnableCode);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        //Broadcast to service that the application is no longer running
        handler.post(runnableCode);
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        //Broadcast to service that the application is running
        handler.post(runnableCode);

    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String mCommand;
            String mtarget;
            int signalStr;
            mCommand= intent.getStringExtra("comamnds");
            mtarget= intent.getStringExtra("target");
            signalStr = 100 + intent.getIntExtra("level", -100);

            Log.v("ssr", Integer.toString(signalStr));

            if(mCommand != null && mtarget != null )
            {
                if(mtarget.equals("AA") || mtarget.equals(sharedpreferences.getString("blood", "")))
                {
                    switch (mCommand) {
                        case "on":
                            PlaybackParams params = new PlaybackParams();
                            params.setSpeed(signalStr/20);
                            if(!soundActive)
                            {
                                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                                PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
                                wl.acquire();

                                mp = MediaPlayer.create(MainActivity.this, R.raw.loudalarm);
                                mp.setLooping(true);
                                mp.setPlaybackParams(params);
                                soundActive = true;
                                am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
                                mp.start();
                                mNotificationManager.notify(512, mBuilder.build());
                                nb.show();
                                soundButton.setText(R.string.silence_btn);
                                soundButton.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.ic_volume_off_black_24dp,0,0,0);
                            }
                            mp.setPlaybackParams(params);
                                break;
                        case "ff":
                            soundActive = false;
                            mp.stop();
                            nb.hide();
                            soundButton.setText(R.string.alarm_btn);
                            soundButton.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.ic_volume_up_black_24dp,0,0,0);
                            break;
                        case "nt":
                            mNotificationManager.notify(512, mBuilder.build());
                            nb.show();
                            break;
                        case "nf":
                            mNotificationManager.cancel(512);
                            nb.hide();
                            break;
                    }
                }
            }
        }
    };

    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            Intent mainBroadcaster = new Intent("mainBroadcaster");
            mainBroadcaster.putExtra("mainstatus", true);
            sendBroadcast(mainBroadcaster);
        }
    };
}
