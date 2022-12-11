package com.example.lifeguard;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.natural_language_understanding.v1.NaturalLanguageUnderstanding;
import com.ibm.watson.natural_language_understanding.v1.model.AnalysisResults;
import com.ibm.watson.natural_language_understanding.v1.model.AnalyzeOptions;
import com.ibm.watson.natural_language_understanding.v1.model.EmotionOptions;
import com.ibm.watson.natural_language_understanding.v1.model.Features;
import com.ibm.watson.natural_language_understanding.v1.model.TargetedEmotionResults;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;

public class MainActivity extends AppCompatActivity {
    private TextView resultTextView;
    private String TAG = "mainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        resultTextView = (TextView) findViewById(R.id.resultText);

        ((Button) findViewById(R.id.button)).setOnClickListener(new View.OnClickListener() {
            @SneakyThrows
            @Override
            public void onClick(View view) {
                analyzeSms();
            }
        });
        ((Button) findViewById(R.id.button2)).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                readFitnessActivity();
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
                String msg = "";
                boolean good = false;
                for (int idx = 0; idx < cursor.getColumnCount(); idx++) {
                    msgData += " " + cursor.getColumnName(idx) + ":" + cursor.getString(idx) + "\n";
                    if (cursor.getColumnName(idx).equals("date")) {
                        long date = Long.parseLong(cursor.getString(idx));
                        //get messages from the last week
                        if (date > System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5)) {
                            good = true;
                        }
                    } else if (cursor.getColumnName(idx).equals("body")) {
                        msg = cursor.getString(idx);
                    }
                }
                if (good) {
//                    String noQuotes = msgData.replaceAll("^\"|\"$", "");
                    System.out.println("MsgData: " + msgData);
                    System.out.println(msg);
//                    Gson gson = new Gson();
//                    Type type = new TypeToken<Map<String, String>>(){}.getType();
//                    Map<String, String> myMap = gson.fromJson(msgData, type);
                    messages.add(msg);
                }
            } while (cursor.moveToNext());
        } else {
            System.out.println("No SMS in inbox");
        }
        System.out.println("messages " + messages);
        return messages;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void readFitnessActivity() {
        System.out.println("Reading fitness activity");

        if (!GoogleSignIn.hasPermissions(
                GoogleSignIn.getLastSignedInAccount(this),
                new Scope("https://www.googleapis.com/auth/fitness.activity.read"))) {
            GoogleSignIn.requestPermissions(
                    MainActivity.this,
                    123,
                    GoogleSignIn.getLastSignedInAccount(this),
                    new Scope("https://www.googleapis.com/auth/fitness.activity.read"));
        } else {
            System.out.println("Permission already granted");
        }
        ZonedDateTime endTime = LocalDateTime.now().atZone(ZoneId.systemDefault());
        ZonedDateTime startTime = endTime.minusYears(1);
        DataReadRequest request = new DataReadRequest.Builder()
                .read(DataType.TYPE_HEART_POINTS)
                .bucketByTime(7, TimeUnit.DAYS)
                .setTimeRange(startTime.toInstant().toEpochMilli(), endTime.toInstant().toEpochMilli(), TimeUnit.MILLISECONDS)
                .build();


        List <Integer> weeks = new ArrayList<>();

        Fitness.getHistoryClient(getApplicationContext(),
                Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(getApplicationContext())))
                .readData(request)
                .addOnSuccessListener(response ->{
                    System.out.println(response.getBuckets().size() +" "+ response.getBuckets().get(0).getDataSets().size());
                    for (Bucket bucket : response.getBuckets()) {
                        for (DataSet dataSet : bucket.getDataSets()) {
                            if (dataSet.isEmpty()){
//                                System.out.println("Empty dataset");
                            }else{
                                weeks.add(dumpDataSet(dataSet));

                            }
                        }
                    }
                    System.out.println(weeks);
                    double[] stats = calculateSD(weeks);
                    // System.out.println(weeks);
                    System.out.println("Standard deviation " + stats[1]);
                    if (weeks.get(weeks.size()-1) < stats[0] - stats[1]){
                        //todo actuate
                        System.out.println("Low score");
                    }
//                {
//                    Optional<DataSet> heartPointsSet = response.getDataSets().stream().findFirst();
//                    int count = 0;
//                    if (heartPointsSet.isPresent()) {
//                        int totalHeartPoints = 0;
//                        for (DataPoint dp : heartPointsSet.get().getDataPoints()) {
//                            //if((int) dp.getValue(Field.FIELD_INTENSITY).asFloat() > 2)
//                                count++;
//                            totalHeartPoints += (int) dp.getValue(Field.FIELD_INTENSITY).asFloat();
//                            System.out.println(dp.getValue(Field.FIELD_INTENSITY));
//                        }
//                        System.out.println("Hearth points" + totalHeartPoints);
//                        System.out.println(count);
//                        Log.i(TAG, "Total heart points: $totalHeartPoints");
//                    }
                });

    }
    // todo calculate the distribution of the weeks
    private int dumpDataSet(DataSet dataSet) {
        int totalHeartPoints = 0;
        for (DataPoint dp : dataSet.getDataPoints()) {
            for (Field field : dp.getDataType().getFields()) {
                totalHeartPoints += (int) dp.getValue(field.FIELD_INTENSITY).asFloat();
            }
        }
        //System.out.println("Total points "+ totalHeartPoints);
        return totalHeartPoints;
    }

    private void analyzeSms() {
        System.out.println("Analyzing sms");
        List<String> messages = readSms();
//      List<String> suicide_messages = {};
        double score = ibmApi(messages);
        if (score == -1) {
            resultTextView.setText("Error in computing the score");
        } else {
            resultTextView.setText("Sadness average score " + score);
            if (score > 0.5) {
                //actuate
            }
        }
        sendNotification(1);

//        for (int message = 0; message < messages.size(); message++) {
//            for (int word = 0; word < suicide_messages.size(); word++) {
//               if (message == word)
//                    score += 1;
//            }
//        }

        //todo add score of sms detection to the total analysis score
    }

    //send notification after an amount of time to make sure user is ok
    public void sendNotification(int multiplier) {
        Intent notificationIntent = new Intent(this, ShowNotification.class);
        PendingIntent contentIntent = PendingIntent.getService(this, 0, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.cancel(contentIntent);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
                + AlarmManager.INTERVAL_FIFTEEN_MINUTES * multiplier, AlarmManager.INTERVAL_FIFTEEN_MINUTES * multiplier, contentIntent);
    }


    public double ibmApi(List<String> messages) {
        System.out.println(messages);
        int SDK_INT = Build.VERSION.SDK_INT;
        if (SDK_INT > 8) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
            IamAuthenticator authenticator = new IamAuthenticator(getResources().getString(R.string.ibm_api));
            NaturalLanguageUnderstanding naturalLanguageUnderstanding = new NaturalLanguageUnderstanding("2022-04-07", authenticator);
            naturalLanguageUnderstanding.setServiceUrl(getResources().getString(R.string.ibm_url));
            List<String> targets = new ArrayList<>();
            /* Ref: https://www.sciencedirect.com/science/article/pii/S2214782915000160#:~:text=%E2%80%9Csuicidal%3B%20suicide%3B%20kill,to%20sleep%20forever%E2%80%9D.
             * https://www.sciencedirect.com/topics/computer-science/suicidal-ideation
             * */
            targets.addAll(Arrays.asList("life", "suicidal", "suicide", "kill", "kill myself", "my suicide note", "my suicide letter", "end my life", "never wake up", "can't go on", "not worth living", "ready to jump", "sleep forever", "want to die", "be dead", "better off without me", "better off dead", "suicide plan", "suicide pact", "tired of living", "don't want to be here", "die alone", "go to sleep forever"));
            EmotionOptions emotion = new EmotionOptions.Builder()
                    .targets(targets)
                    .build();

            Features features = new Features.Builder()
                    .emotion(emotion)
                    .build();

            double sadnessAverage = 0;
            int nrEntries = 0;
            for (String msg : messages) {
                if (msg.length() > 8) {
                    AnalyzeOptions parameters = new AnalyzeOptions.Builder()
                            .text(msg)
                            .features(features)
                            .build();

                    AnalysisResults response = naturalLanguageUnderstanding
                            .analyze(parameters)
                            .execute()
                            .getResult();
                    System.out.println(response);
                    for (TargetedEmotionResults i : response.getEmotion().getTargets()) {
                        sadnessAverage += i.getEmotion().getSadness();
                        nrEntries++;
                    }
                    //System.out.println(response.getEmotion().getTargets().get(0).getEmotion().getSadness());
                }
            }
            return sadnessAverage / nrEntries;
        }
        return -1;
    }

    public double[] calculateSD(List numArray)
    {
        int sum = 0, standardDeviation = 0;
        int length = numArray.size();
        for(Object num : numArray) {
            sum += (int) num;
        }
        double mean = sum/length;
        for(Object num: numArray) {
            standardDeviation += Math.pow((int) num - mean, 2);
        }
        double[] stats = { mean, Math.sqrt(standardDeviation/length) };
        return stats;
    }
}