package com.example.lifeguard.Api;

import java.util.List;

public class Gpt3ResponseModeration {
    private List<Categories> results;

    public String[] getResponse() {
        return new String[]{results.get(0).getSelfHarmCategory(), results.get(0).getSelfHarmCategoryScore()};
    }
}
