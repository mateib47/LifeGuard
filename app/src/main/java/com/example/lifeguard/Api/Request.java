package com.example.lifeguard.Api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class Request {
    private int id;
    private String language;
    private String text;

}
