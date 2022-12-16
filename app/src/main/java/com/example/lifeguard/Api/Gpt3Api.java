package com.example.lifeguard.Api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface Gpt3Api {
    @POST("/v1/documents")
    Call<Gpt3Response> generateText(@Body Gpt3Request request);
}
