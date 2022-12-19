package com.example.lifeguard.Api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface Api {

    String BASE_URL = "https://lifeguard-api.herokuapp.com/";
    @POST("api/v1/user/add")
    Call<Long> addUser(@Body User body);
    @POST("api/v1/contact/add")
    Call<String> contactPerson(@Body ContactRequest body);
}
