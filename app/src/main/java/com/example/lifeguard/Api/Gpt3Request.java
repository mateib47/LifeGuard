package com.example.lifeguard.Api;

import java.util.Arrays;
import java.util.List;

public class Gpt3Request {
    private String model;
    private String prompt;
    private double temperature;
    private int max_tokens;
    private double top_p;
    private double frequency_penalty;
    private double presence_penalty;
    private List<String> stop;

    public Gpt3Request(String model, String prompt, double temperature, int max_tokens, double top_p, double frequency_penalty, double presence_penalty, List<String> stop) {
        this.model = model;
        this.prompt = prompt;
        this.temperature = temperature;
        this.max_tokens = max_tokens;
        this.top_p = top_p;
        this.frequency_penalty = frequency_penalty;
        this.presence_penalty = presence_penalty;
        this.stop = stop;
    }

    public Gpt3Request(String model, String prompt) {
        this.model = model;
        this.prompt = prompt;
        this.temperature = 0.9;
        this.max_tokens = 150;
        this.top_p = 1.0;
        this.frequency_penalty = 0.0;
        this.presence_penalty = 0.6;
        this.stop = Arrays.asList(" Human:", " AI:");
    }
}
