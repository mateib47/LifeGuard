package com.example.lifeguard;

import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.natural_language_understanding.v1.NaturalLanguageUnderstanding;
import com.ibm.watson.natural_language_understanding.v1.model.AnalysisResults;
import com.ibm.watson.natural_language_understanding.v1.model.AnalyzeOptions;
import com.ibm.watson.natural_language_understanding.v1.model.EmotionOptions;
import com.ibm.watson.natural_language_understanding.v1.model.Features;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((Button) findViewById(R.id.button)).setOnClickListener(new View.OnClickListener() {
            @SneakyThrows
            @Override
            public void onClick(View view) {
                analyzeSms();
            }
        });

    }

    protected List readSms() {
        System.out.println("Reading sms");
        List messages = new ArrayList<String>();
        final String[] projection = new String[]{"_id", "address", "body", "date"};
        final Uri uri = Uri.parse("content://sms/sent");
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                String msgData = "";
                boolean good = false;
                for (int idx = 0; idx < cursor.getColumnCount(); idx++) {
                    msgData += " " + cursor.getColumnName(idx) + ":" + cursor.getString(idx) + "\n";
                    if (cursor.getColumnName(idx).equals("date")) {
                        long date = Long.parseLong(cursor.getString(idx));
                        //get messages from the last week
                        if (date > System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5)) {
                            good = true;
                        }
                    }
                }
                if (good) {
                    System.out.println("MsgData: " + msgData);
                    messages.add(msgData);
                }
            } while (cursor.moveToNext());
        } else {
            System.out.println("No SMS in inbox");
        }
        return messages;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void readFitnessActivity() {
        ZonedDateTime endTime = LocalDateTime.now().atZone(ZoneId.systemDefault());
        ZonedDateTime startTime = endTime.minusWeeks(1);
//        DataReadRequest readRequest = new DataReadRequest.Builder()
//                .aggregate(DataType.AGGREGATE_STEP_COUNT_DELTA)
//                .bucketByTime(1, TimeUnit.DAYS)
//                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
//                .build();

//        Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
//                .readData(readRequest)
//                .addOnSuccessListener (response -> {
//                    // The aggregate query puts datasets into buckets, so convert to a
//                    // single list of datasets
//                    for (Bucket bucket : response.getBuckets()) {
//                        for (DataSet dataSet : bucket.getDataSets()) {
//                            //dumpDataSet(dataSet);
//                        }
//                    }
//                })
//                .addOnFailureListener(e ->
//                        Log.w(TAG, "There was an error reading data from Google Fit", e));
    }

    private void analyzeSms() {
        System.out.println("Analyzing sms");
        List<String> messages = readSms(); //todo return list of messages to be put in post request
        int score = ibmApi(messages);
    }


    public int ibmApi(List<String> messages) {
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
            IamAuthenticator authenticator = new IamAuthenticator(String.valueOf(R.string.ibm_api));
            NaturalLanguageUnderstanding naturalLanguageUnderstanding = new NaturalLanguageUnderstanding("2022-04-07", authenticator);
            naturalLanguageUnderstanding.setServiceUrl(String.valueOf(R.string.ibm_url));

            List<String> targets = new ArrayList<>();
            // todo add meaningful targets
            targets.add("life");
            targets.add("me");

            EmotionOptions emotion = new EmotionOptions.Builder()
                    .targets(targets)
                    .build();

            Features features = new Features.Builder()
                    .emotion(emotion)
                    .build();

            for (String msg : messages) {
                AnalyzeOptions parameters = new AnalyzeOptions.Builder()
                        .text(msg)
                        .features(features)
                        .build();

                AnalysisResults response = naturalLanguageUnderstanding
                        .analyze(parameters)
                        .execute()
                        .getResult();
                System.out.println(response);
            }

        }
        return 0;
    }
}