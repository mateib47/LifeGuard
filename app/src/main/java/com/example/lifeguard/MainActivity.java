package com.example.lifeguard;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((Button) findViewById(R.id.button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                readSms();
            }
        });

    }

    protected void readSms() {
        int REQUEST_CODE_ASK_PERMISSIONS = 123;
        ActivityCompat.requestPermissions(this, new String[]{"android.permission.READ_SMS"}, REQUEST_CODE_ASK_PERMISSIONS);
        //Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);

        final String[] projection = new String[]{"_id", "address", "body", "date"};
        final Uri uri = Uri.parse("content://sms/sent");
        Cursor cursor = getContentResolver().query(uri, projection,null, null, null);
        if (cursor.moveToFirst()) {
            do {
                String msgData = "";
                boolean good = false;
                for (int idx = 0; idx < cursor.getColumnCount(); idx++) {
                    msgData += " " + cursor.getColumnName(idx) + ":" + cursor.getString(idx) + "\n";

                    if(cursor.getColumnName(idx).equals("date") ){
                        long date = Long.parseLong( cursor.getString(idx));
                        //get messages from the last week
                        if(date > System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5)){
                            good = true;
                        }
                    }
                }
                if(good)
                    System.out.println("MsgData: " + msgData);
            } while (cursor.moveToNext());
        } else {
            System.out.println("No SMS in inbox");
        }
    }
}