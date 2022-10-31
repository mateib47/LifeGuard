package com.example.lifeguard;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.system.Os;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.lifeguard.Api.Request;
import com.example.lifeguard.Api.RetrofitClient;
import com.example.lifeguard.Api.Score;
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.Sentiment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((Button) findViewById(R.id.button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //analyzeSms();
                try {
                    googleApi();
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
//        final InputStream stream = getApplicationContext().getResources().openRawResource(R.raw.credential);
//        try {
//            final GoogleCredentials googleCredentials = serviceAccountCredentials
//                    .createScoped(Collections.singletonList(StorageScopes.DEVSTORAGE_FULL_CONTROL));
//            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(googleCredentials);
//
//            final com.google.api.services.storage.Storage myStorage = new com.google.api.services.storage.Storage.Builder(
//                    new NetHttpTransport(), new JacksonFactory(), requestInitializer).build();
//
//
//            credential.refreshToken();
//            final String accessToken = credential.getAccessToken();
//            prefs.edit().putString(PREF_ACCESS_TOKEN, accessToken).apply();
//            return accessToken;
//        } catch (IOException e) {
//            Log.e(TAG, "Failed to obtain access token.", e);
//        }
//

        saveToJson();
        Os.setenv("GOOGLE_APPLICATION_CREDENTIALS",
                "/data/data/com.example.lifeguard/files/credentials.json", true);
        // Instantiates a client
        try (LanguageServiceClient language = LanguageServiceClient.create()) {

            // The text to analyze
            String text = "Hello, world!";
            Document doc = Document.newBuilder().setContent(text).setType(Document.Type.PLAIN_TEXT).build();

            // Detects the sentiment of the text
            Sentiment sentiment = language.analyzeSentiment(doc).getDocumentSentiment();

            System.out.printf("Text: %s%n", text);
            System.out.printf("Sentiment: %s, %s%n", sentiment.getScore(), sentiment.getMagnitude());
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