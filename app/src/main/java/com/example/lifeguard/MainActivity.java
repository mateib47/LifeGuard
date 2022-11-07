package com.example.lifeguard;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lifeguard.Api.Request;
import com.example.lifeguard.Api.RetrofitClient;
import com.example.lifeguard.Api.Score;
import com.google.cloud.language.v1.AnalyzeSentimentResponse;
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.Sentiment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Error in main activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((Button) findViewById(R.id.button)).setOnClickListener(new View.OnClickListener() {
            @SneakyThrows
            @Override
            public void onClick(View view) {
                //analyzeSms();
                //googleApi();
                try {
                    googleApi();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    protected void readSms() {
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void readFitnessActivity(){
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
        readSms(); //todo return list of messages to be put in post request
        Call<List<Score>> call = RetrofitClient.getInstance().getMyApi().getSentimentData(new Request(1,"en","hello"));
        call.enqueue(new Callback<List<Score>>() {
            @Override
            public void onResponse(Call<List<Score>> call, Response<List<Score>> response) {
                List<Score> scores = response.body();
                //todo add logic of computing average the score
            }

            @Override
            public void onFailure(Call<List<Score>> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "An error has occured", Toast.LENGTH_LONG).show();
            }

        });
    }

    //todo add google api credentials
    public void googleApi() throws Exception {
            String text = "hello world";

        try (LanguageServiceClient language = LanguageServiceClient.create()) {
            Document doc = Document.newBuilder().setContent(text).setType(Document.Type.PLAIN_TEXT).build();
            AnalyzeSentimentResponse response = language.analyzeSentiment(doc);
            Sentiment sentiment = response.getDocumentSentiment();
            if (sentiment == null) {
                System.out.println("No sentiment found");
            } else {
                System.out.printf("Sentiment magnitude: %.3f\n", sentiment.getMagnitude());
                System.out.printf("Sentiment score: %.3f\n", sentiment.getScore());
            }
            System.out.println(sentiment);
        }
    }

    //probably useless
    public void saveToJson(){
        JSONObject json = new JSONObject();
        JSONObject jsonPar = new JSONObject();

        try {
            json.put("client_id", "");
            json.put("project_id", "");
            json.put("auth_uri", "");
            json.put("token_uri", "");
            json.put("auth_provider_x509_cert_url", "");
            jsonPar.put("installed", json);

            String jsonString = jsonPar.toString();
            jsonString=jsonString.replaceAll("\\\\","");

            FileOutputStream fos = this.openFileOutput("credentials.json", Context.MODE_PRIVATE);
            fos.write(jsonString.getBytes());
            fos.close();


        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }
}