package com.example.lifeguard;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionRequest;

public class ChatbotActivity extends AppCompatActivity {
    OpenAiService service;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatbot_main);
        service = new OpenAiService(String.valueOf(R.string.openai));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void sendRequest(){
        CompletionRequest completionRequest = CompletionRequest.builder()
                .prompt("Somebody once told me the world is gonna roll me")
                .echo(true)
                .build();
        service.createCompletion("ada", completionRequest).getChoices().forEach(System.out::println);
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
