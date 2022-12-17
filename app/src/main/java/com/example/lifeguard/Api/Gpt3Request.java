package com.example.lifeguard.Api;

import java.util.Arrays;
import java.util.List;

public class Gpt3Request {
    private String model;
    private String prompt;
    private double temperature = 0.9;
    private int max_tokens = 150;
    private double top_p = 1.0;
    private double frequency_penalty = 0.0;
    private double presence_penalty = 0.6;
    private List<String> stop = Arrays.asList(" Human:", " AI:");

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
    }
}
