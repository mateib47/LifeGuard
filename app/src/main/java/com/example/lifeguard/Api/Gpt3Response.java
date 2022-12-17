package com.example.lifeguard.Api;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Gpt3Response {
    private List<Choices> choices;
    //TODO
    public String getResponse() {
        return choices.get(0).getText();
    }
}
