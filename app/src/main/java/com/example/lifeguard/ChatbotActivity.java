package com.example.lifeguard;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;

import java.util.Arrays;
import java.util.List;

public class ChatbotActivity extends AppCompatActivity {
    OpenAiService service;
    public static Handler handler = new Handler();
    String defaultPrompt = "The following is a conversation with an AI assistant. The assistant is respectful, empathetic, non-judgmental and very friendly.\\n\\nHuman: Hello, who are you?\\nAI: I am an AI created by OpenAI. How can I help you today?\\nHuman: ";
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
                try  {
                    CompletionRequest completionRequest = CompletionRequest.builder()
                            .model("text-davinci-002")
                            .prompt(prompt)
                            .temperature(0.9)
                            .maxTokens(150)
                            .topP(1.0)
                            .frequencyPenalty(0.0)
                            .presencePenalty(0.6)
                            .stop(Arrays.asList(" Human:", " AI:"))
                            .build();
                    List<CompletionChoice> list =  service.createCompletion(completionRequest).getChoices();
                    for(CompletionChoice c : list){
                        System.out.println(c.getText());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    public void sendMessage(String msg){
        System.out.println(msg);
    }
    /*
    *
    * {
  model: "text-davinci-002",
  prompt: "The following is a conversation with an AI assistant. The assistant is helpful, creative, clever, and very friendly.\n\nHuman: Hello, who are you?\nAI: I am an AI created by OpenAI. How can I help you today?\nHuman: ",
  temperature: 0.9,
  max_tokens: 150,
  top_p: 1,
  frequency_penalty: 0,
  presence_penalty: 0.6,
  stop: [" Human:", " AI:"],
}
    * */
}
