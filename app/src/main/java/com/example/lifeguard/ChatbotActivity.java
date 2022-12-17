package com.example.lifeguard;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lifeguard.Api.Gpt3Api;
import com.example.lifeguard.Api.Gpt3Request;
import com.example.lifeguard.Api.Gpt3Response;
import com.google.android.material.textfield.TextInputEditText;
import com.theokanning.openai.OpenAiService;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChatbotActivity extends AppCompatActivity {
    OpenAiService service;
    public static Handler handler = new Handler();
    String defaultPrompt = "The assistant is respectful, empathetic, non-judgmental and very friendly. The assistant's task is to listen to the person and give empathetic responses and sometimes ask questions.";
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatbot_main);
        service = new OpenAiService(String.valueOf(R.string.openai));
        Button submit_btn = (Button) findViewById(R.id.submit_btn);
        TextInputEditText textInputEditText = (TextInputEditText) findViewById(R.id.input_message);
        initAi(defaultPrompt);

        textInputEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    submit_btn.performClick();
                    return true;
                }
                return false;
            }
        });
        submit_btn.setOnClickListener(new Button.OnClickListener() {

            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                String msg = textInputEditText.getText().toString().trim();
                sendMessage(msg);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void initAi(String prompt){
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
                Gpt3Request request = new Gpt3Request("text-davinci-003", prompt);
                api.generateText(request).enqueue(new Callback<Gpt3Response>() {
                    @Override
                    public void onResponse(Call<Gpt3Response> call, Response<Gpt3Response> response) {
                        if (response.isSuccessful()) {
                            Gpt3Response gpt3Response = response.body();
                            System.out.println(gpt3Response.getResponse());
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
                    public void onFailure(Call<Gpt3Response> call, Throwable t) {
                        // handle failure
                    }
                });
            }
        });
        thread.start();
    }

    public void sendMessage(String msg){
        System.out.println(msg);
    }
}
