package com.example.lifeguard.Api;


import android.content.Context;
import android.widget.Toast;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static RetrofitClient instance = null;
    private Api myApi;

    private RetrofitClient() {
        Retrofit retrofit = new Retrofit.Builder().baseUrl(Api.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        myApi = retrofit.create(Api.class);
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    public Api getMyApi() {
        return myApi;
    }

    public void addUser(Context context, String firstName,
                        String lastName,
                        String email,
                        String phoneNumber) {
        User user = new User(firstName, lastName,email, phoneNumber );
        Call<Long> call = getMyApi().addUser(user);
        call.enqueue(new Callback<Long>() {
            @Override
            public void onResponse(Call<Long> call, Response<Long> response) {
                Toast.makeText(context, "Data added to API", Toast.LENGTH_SHORT).show();
                Long responseFromAPI = response.body();
                String responseString = "Response Code : " + response.code() + "\nId : " + responseFromAPI.toString();
                Toast.makeText(context, responseString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<Long> call, Throwable t) {
                Toast.makeText(context, "Error in adding user", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
