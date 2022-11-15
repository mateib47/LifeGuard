package com.example.lifeguard;

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
import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;

public class MainActivity extends AppCompatActivity {
    private TextView resultTextView;

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
        List<String> messages = readSms();
//      List<String> suicide_messages = {};
        double score = ibmApi(messages);
        if(score == -1){
            resultTextView.setText("Error in computing the score");
        }else{
            resultTextView.setText("Sadness average score " + score);
        }

//        for (int message = 0; message < messages.size(); message++) {
//            for (int word = 0; word < suicide_messages.size(); word++) {
//               if (message == word)
//                    score += 1;
//            }
//        }

        //todo add score of sms detection to the total analysis score
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
}