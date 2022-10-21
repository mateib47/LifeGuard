package com.example.lifeguard.Api;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class Score {
    private String superName;
    private int id;
    private String sentiment;
    private int length;
}
