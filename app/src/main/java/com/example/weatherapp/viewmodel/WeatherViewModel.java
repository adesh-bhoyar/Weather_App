package com.example.weatherapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.weatherapp.apiinterface.WeatherApi;
import com.example.weatherapp.model.WeatherResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WeatherViewModel extends ViewModel {
    private MutableLiveData<WeatherResponse> weatherData = new MutableLiveData<>();

    // Implement a method to fetch weather data from the Weather API using Retrofit
    public void fetchWeatherData(String city, double latitude, double longitude, String unit) {
        // Create a Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/data/2.5/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Create the WeatherApi service
        WeatherApi weatherApi = retrofit.create(WeatherApi.class);

        // Make the API request
        Call<WeatherResponse> call = weatherApi.getCurrentWeather(city, latitude, longitude, "ac5685db2f5776bffd509201d080b888", unit);

        // Enqueue the API call
        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful()) {
                    weatherData.setValue(response.body());
                } else {
                    weatherData.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                // Handle network errors
            }
        });
    }

    // Getter for the weather data LiveData
    public LiveData<WeatherResponse> getWeatherData() {
        return weatherData;
    }
}

