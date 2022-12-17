package com.example.lifeguard.view;

public class ChatMessage {

    private String mText;
    private String mUsername;

    public ChatMessage(String text, String username) {
        mText = text;
        mUsername = username;
    }

    public String getText() {
        return mText;
    }

    public String getUsername() {
        return mUsername;
    }
}
