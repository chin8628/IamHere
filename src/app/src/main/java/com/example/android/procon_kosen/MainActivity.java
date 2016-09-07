package com.example.android.procon_kosen;

import android.Manifest;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private Button editBtn;
    private AudioManager am;
    private Uri notification;
    private Ringtone r;
    private SharedPreferences sharedpreferences;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;
    private MediaPlayer mp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Request Location and Boot permission if not already granted
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECEIVE_BOOT_COMPLETED}, 1002);
        }

        //Initialize audio object
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        r = RingtoneManager.getRingtone(getApplicationContext(), notification);
        mp = MediaPlayer.create(MainActivity.this, R.raw.loudalarm);
        mp.setLooping(true);

        //Start Background Wifi Service
        Intent service = new Intent(this, WiFiScanner.class);
        startService(service);

        //Broadcast to service that the application is running
        Intent mainBroadcaster = new Intent("mainBroadcaster");
        mainBroadcaster.putExtra("mainstatus", true);
        sendBroadcast(mainBroadcaster);

        //Initialize Ui object
        TextView name = (TextView) findViewById(R.id.name);
        TextView birthday = (TextView) findViewById(R.id.birthday);
        TextView blood = (TextView) findViewById(R.id.blood);
        TextView sibling1 = (TextView) findViewById(R.id.sibling_phone1);
        TextView sibling2 = (TextView) findViewById(R.id.sibling_phone2);

        //Link object to data
        sharedpreferences = getSharedPreferences("contentProfle", Context.MODE_PRIVATE);
        name.setText(sharedpreferences.getString("name", ""));
        birthday.setText(sharedpreferences.getString("birthday", ""));
        blood.setText(sharedpreferences.getString("blood", ""));
        sibling1.setText(sharedpreferences.getString("sibling1", ""));
        sibling2.setText(sharedpreferences.getString("sibling2", ""));

        editBtn = (Button) findViewById(R.id.edit_btn);
        editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, EditProfile.class);
                startActivity(i);

            }
        });

        StringBuilder sb = new StringBuilder();
        sb.append("You have been detected.\n");
        sb.append("Personal Details\n");
        sb.append("Blood Type ").append(sharedpreferences.getString("blood", "")).append("\n");
        sb.append("Contact :");
        sb.append(sharedpreferences.getString("sibling1", "" ));
        sb.append("");
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

        /*NotificationCompat.InboxStyle inBoxStyle = new NotificationCompat.InboxStyle();
        inBoxStyle.setBigContentTitle("Personal Details:");
        inBoxStyle.addLine("Siblings Contact");
        inBoxStyle.addLine(sharedpreferences.getString("sibling1", ""));
        inBoxStyle.addLine(sharedpreferences.getString("sibling2", ""));
        inBoxStyle.addLine("Blood Type " + sharedpreferences.getString("blood", ""));
        mBuilder.setStyle(inBoxStyle);*/

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //register reliever from service
        registerReceiver(mMessageReceiver, new IntentFilter("command recived"));


    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        //Broadcast to service that the application is no longer running
        Intent mainBroadcaster = new Intent("mainBroadcaster");
        mainBroadcaster.putExtra("mainstatus", false);
        sendBroadcast(mainBroadcaster);
    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String mCommand = null;
            String mtarget = null;
            mCommand= intent.getStringExtra("comamnds");
            mtarget= intent.getStringExtra("target");
            if(mCommand != null && mtarget != null )
            {
                if(mtarget.equals("AA") || mtarget.equals(sharedpreferences.getString("blood", "")))
                {
                    switch (mCommand) {
                        case "on":
                            if (!mp.isPlaying()) {
                                am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
                                mp.start();
                                mNotificationManager.notify(512, mBuilder.build());
                            }
                            break;
                        case "ff":
                            mp.stop();
                            mNotificationManager.cancel(512);
                            break;
                        case "nt":
                            mNotificationManager.notify(512, mBuilder.build());
                            break;
                        case "nf":
                            mNotificationManager.cancel(512);
                            break;
                    }
                }
            }


        }
    };
}
