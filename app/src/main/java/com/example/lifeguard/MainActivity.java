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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.lifeguard.Api.ContactRequest;
import com.example.lifeguard.Api.Gpt3Api;
import com.example.lifeguard.Api.Gpt3RequestModeration;
import com.example.lifeguard.Api.Gpt3ResponseModeration;
import com.example.lifeguard.Api.RetrofitClient;
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

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private TextView resultTextView;
    private String TAG = "mainActivity";
    private FusedLocationProviderClient fusedLocationClient;
    private HandlerThread mWorkerThread;
    private Handler mHandlerWorker;
    public static final String NOTIFICATION_CHANNEL_ID = "10001" ;
    private final static String default_notification_channel_id = "default" ;
    private List<String> messages;
    private ProgressBar progressBarMsg;
    private ProgressBar progressBarFit;
    private ProgressBar progressBarSleep;
    private ProgressBar progressBarLoc;
    private int[] scores;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        resultTextView = (TextView) findViewById(R.id.resultText);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mWorkerThread = new HandlerThread("Worker Thread");
        mWorkerThread.start();
        mHandlerWorker = new Handler(mWorkerThread.getLooper());

        progressBarMsg = findViewById(R.id.progress_bar_1);
        progressBarFit = findViewById(R.id.progress_bar_2);
        progressBarSleep = findViewById(R.id.progress_bar_3);
        progressBarLoc = findViewById(R.id.progress_bar_4);


        scores = new int[4];

        messages = readSms();

        ((Button) findViewById(R.id.button)).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @SneakyThrows
            @Override
            public void onClick(View view) {
                analyzeSms();
                readFitnessActivity();
                readSleepActivity();
                analyzeLocation();
            }
        });
    }

    private void checkScores() {
        System.out.println("Final scores" + Arrays.toString(scores));
        double finalScore = 0.5* scores[0] + 0.2*scores[1]+0.2*scores[2]+0.1*scores[3];
        System.out.println("Final Score " + finalScore);
        if(finalScore < 20){
            sendContactRequest();
        }else if (finalScore < 55){
            sendNotification(10000);
        }
    }

    private void sendContactRequest() {
            ContactRequest contactRequest = new ContactRequest(GoogleSignIn.getLastSignedInAccount(this).getEmail()," is not feeling well.");
            Call<String> call = RetrofitClient.getInstance().getMyApi().contactPerson(contactRequest);
            call.enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    Toast.makeText(getApplicationContext(), "Contact request sent", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    System.out.println(t);
//                    Toast.makeText(getApplicationContext(), "Error in sending request", Toast.LENGTH_SHORT).show();
                }
            });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void readSleepActivity() {

        ZonedDateTime endTime = LocalDateTime.now().atZone(ZoneId.systemDefault());
        ZonedDateTime startTime = endTime.minusYears(1);
        DataReadRequest request = new DataReadRequest.Builder()
                .read(DataType.TYPE_SLEEP_SEGMENT)
                .bucketByTime(7, TimeUnit.DAYS)
                .setTimeRange(startTime.toInstant().toEpochMilli(), endTime.toInstant().toEpochMilli(), TimeUnit.MILLISECONDS)
                .build();

        List <Integer> weeks = new ArrayList<>();

        Fitness.getHistoryClient(getApplicationContext(),
                Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(getApplicationContext())))
                .readData(request)
                .addOnSuccessListener(response -> {
                    for (Bucket bucket : response.getBuckets()) {
                        for (DataSet dataSet : bucket.getDataSets()) {
                            if (dataSet.isEmpty()){
//                                System.out.println("Empty dataset");
                            }else{
                                weeks.add(dumpDataSet2(dataSet));
                            }
                        }
                    }
                });
        //not enough data, do the same as fitness
        scores[2] = 10;
    }

    private void analyzeLocation() {
        Location location = readLocation();
        LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(new LatLng(36.012631, 129.321756) ).include(new LatLng(36.012432, 129.322118));
        LatLngBounds problematicLocation  = builder.build();
        if(problematicLocation.contains(myLocation)){
            System.out.println("Located in a problematic location");
            progressBarLoc.setProgress(10);
            scores[3] = 10;
        }else{
            System.out.println("All good with location");
            progressBarLoc.setProgress(90);
            scores[3] = 90;
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

    protected List<String> readSms() {
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
                    double[] stats = calculateSD(normalize(weeks));
                    int mean = (int) stats[0], standardDev = (int)  stats[1];
                    int current = weeks.get(weeks.size()-1);
                    int score = (current - mean) / standardDev;
                    progressBarFit.setProgress(score);
                    scores[1] = score;
                });

    }

    public List<Integer> normalize(List<Integer> values){
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        for (int value : values) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }

        int range = max - min;

        for (int i = 0; i < values.size(); i++) {
            values.set(i, (values.get(i) - min) / range);
        }

        for (int i = 0; i < values.size(); i++) {
            values.set(i, values.get(i)*100);
        }

        return values;
    }

    private int dumpDataSet2(DataSet dataSet) {
        long totalSleep = 0;
        for (DataPoint dp : dataSet.getDataPoints()) {
            for (Field field : dp.getDataType().getFields()) {
                totalSleep += dp.getEndTime(TimeUnit.MILLISECONDS) - dp.getStartTime(TimeUnit.MILLISECONDS);;
            }
        }
        return (int) (totalSleep / (60 * 60 * 1000));
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void analyzeSms(){
        System.out.println("Analyzing sms");
        messages = readSms();
        String input = StringUtils.join(messages, ", ");

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                OkHttpClient client = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
                    @Override
                    public okhttp3.Response intercept(Chain chain) throws IOException {
                        Request newRequest  = chain.request().newBuilder()
                                .addHeader("Authorization", "Bearer " + getString(R.string.openai))
                                .build();
                        return chain.proceed(newRequest);
                    }
                }).build();
                Retrofit retrofit = new Retrofit.Builder()
                        .client(client)
                        .baseUrl("https://api.openai.com/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
                Gpt3Api api = retrofit.create(Gpt3Api.class);
                Gpt3RequestModeration request = new Gpt3RequestModeration(input);
                api.classifyText(request).enqueue(new Callback<Gpt3ResponseModeration>() {
                    @Override
                    public void onResponse(Call<Gpt3ResponseModeration> call, Response<Gpt3ResponseModeration> response) {
                        if (response.isSuccessful()) {
                            Gpt3ResponseModeration gpt3Response = response.body();
                            String[] textResponse = gpt3Response.getResponse();
                            System.out.println(textResponse[0]+" "+ textResponse[1]);
                            int score = (int) Math.round(Double.parseDouble(textResponse[1])*100);
                            System.out.println("score "+score);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBarMsg.setProgress(100 - score);
                                    scores[0] = 100 - score;
                                    checkScores();
                                }
                            });
                            //sendMessage(gpt3Response.getResponse(), false);
                        } else {
                            System.out.println("Chatbot api error");
                            try {
                                JSONObject jObjError = new JSONObject(response.errorBody().string());
                                Toast.makeText(getApplicationContext(), jObjError.getJSONObject("error").getString("message"), Toast.LENGTH_LONG).show();
                                System.out.println(jObjError.getJSONObject("error").getString("message"));
                            } catch (Exception e) {
                                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Gpt3ResponseModeration> call, Throwable t) {
                        // handle failure
                    }
                });
            }
        });
        thread.start();
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