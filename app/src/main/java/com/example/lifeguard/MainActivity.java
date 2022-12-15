package com.example.lifeguard;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.StrictMode;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
    private FusedLocationProviderClient fusedLocationClient;
    private HandlerThread mWorkerThread;
    private Handler mHandlerWorker;
    public static final String NOTIFICATION_CHANNEL_ID = "10001" ;
    private final static String default_notification_channel_id = "default" ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        resultTextView = (TextView) findViewById(R.id.resultText);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mWorkerThread = new HandlerThread("Worker Thread");
        mWorkerThread.start();
        mHandlerWorker = new Handler(mWorkerThread.getLooper());

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
        ((Button) findViewById(R.id.button3)).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                analyzeLocation();
            }
        });
    }

    private void analyzeLocation() {
        Location location = readLocation();
        LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(new LatLng(36.017041, 129.322594) ).include(new LatLng(36.016734, 129.322815));
        LatLngBounds problematicLocation  = builder.build();
        if(problematicLocation.contains(myLocation)){
            System.out.println("Located in a problematic location");
        }else{
            System.out.println("All good with location");
        }
    }

    private Location readLocation() {
        System.out.println("Reading location...");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            return null;
        }
        Task<Location> locationTask = fusedLocationClient.getLastLocation();
//

        locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    System.out.println(location);
                } else {
                    System.out.println("Location null");
                }
            }
        });
        while(true){
            if (locationTask.isComplete())
                return locationTask.getResult();
        }
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
        sendNotification(10000);

//        for (int message = 0; message < messages.size(); message++) {
//            for (int word = 0; word < suicide_messages.size(); word++) {
//               if (message == word)
//                    score += 1;
//            }
//        }

        //todo add score of sms detection to the total analysis score
    }

    //send notification after an amount of time to make sure user is ok
    public void sendNotification(int delay) {
        System.out.println("sending notification in " + delay + " minutes");
        Intent notificationIntent = new Intent( this, NotificationPublisher. class ) ;
        notificationIntent.putExtra(NotificationPublisher. NOTIFICATION_ID , 1 ) ;
        notificationIntent.putExtra(NotificationPublisher. NOTIFICATION , getNotification()) ;
        PendingIntent pendingIntent = PendingIntent. getBroadcast ( this, 0 , notificationIntent , PendingIntent. FLAG_UPDATE_CURRENT ) ;
        long futureInMillis = SystemClock. elapsedRealtime () + delay ;
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context. ALARM_SERVICE ) ;
        assert alarmManager != null;
        alarmManager.set(AlarmManager. ELAPSED_REALTIME_WAKEUP , futureInMillis , pendingIntent) ;
    }

    private Notification getNotification () {
        NotificationCompat.Builder builder = new NotificationCompat.Builder( this, default_notification_channel_id ) ;
        builder.setContentTitle( "Just wanted to make sure you are fine" ) ;
        builder.setContentText("How are you feeling?") ;
        builder.setSmallIcon(R.drawable.ic_launcher_foreground ) ;
        builder.setAutoCancel( true ) ;
        builder.setChannelId( NOTIFICATION_CHANNEL_ID ) ;
        return builder.build() ;
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