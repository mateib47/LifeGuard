package com.example.lifeguard.Api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface Api {

    String BASE_URL = "http://localhost:8080/api/v1/";
    @POST("contact/add")
    Call<Long> addUser(@Body User body);
}
