package com.example.lifeguard.Api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface Api {

    String BASE_URL = "";
    @POST("")
    Call<List<Score>> getSentimentData(@Body Request body);
}
