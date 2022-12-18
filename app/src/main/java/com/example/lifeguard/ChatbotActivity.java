package com.example.lifeguard;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lifeguard.Api.Gpt3Api;
import com.example.lifeguard.Api.Gpt3Request;
import com.example.lifeguard.Api.Gpt3Response;
import com.example.lifeguard.view.ChatAdapter;
import com.example.lifeguard.view.ChatMessage;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChatbotActivity extends AppCompatActivity {
    private String defaultPrompt;
    private RecyclerView mChatRecyclerView;
    private ChatAdapter mChatAdapter;
    private EditText mMessageEditText;
    private Button mSendButton;




    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatbot_main2);

        defaultPrompt = getString(R.string.chatbot_initial_prompt);


        mChatRecyclerView = findViewById(R.id.recycler_view);
        mChatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mChatAdapter = new ChatAdapter(new ArrayList<ChatMessage>());
        mChatRecyclerView.setAdapter(mChatAdapter);
        mMessageEditText = findViewById(R.id.edit_message);
        mSendButton = findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String messageText = mMessageEditText.getText().toString();
                sendMessage(messageText);
                sendRequest(defaultPrompt+"Human: "+messageText);
                mMessageEditText.setText("");
            }
        });

        sendRequest(defaultPrompt);
    }

    private void sendMessage(String messageText) {
        messageText = messageText.trim();
        String username = "User";
        ChatMessage message = new ChatMessage(messageText, username);
        mChatAdapter.addMessage(message);
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private void sendRequest(String prompt){
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
                            sendMessage(gpt3Response.getResponse());
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

}
