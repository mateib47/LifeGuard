package com.example.lifeguard.Api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface Api {

    String BASE_URL = "http://172.30.1.72:8080/";
    @POST("api/v1/user/add")
    Call<Long> addUser(@Body User body);
}
