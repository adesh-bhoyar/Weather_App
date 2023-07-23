package com.example.weatherapp.views;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.weatherapp.R;
import com.example.weatherapp.databinding.ActivityMainBinding;
import com.example.weatherapp.model.WeatherResponse;
import com.example.weatherapp.viewmodel.WeatherViewModel;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String UNIT_METRIC = "metric";
    private static final String UNIT_IMPERIAL = "imperial";
    private WeatherViewModel weatherViewModel;
    private String city = "", formattedTemperature;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        weatherViewModel = new ViewModelProvider(this).get(WeatherViewModel.class);

        // Check location permission and request user's location
        if (checkLocationPermission()) {
            fetchWeatherData(UNIT_METRIC);
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        binding.switchButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String unit = isChecked ? UNIT_IMPERIAL : UNIT_METRIC;
            fetchWeatherData(unit);
        });
    }

    private boolean checkLocationPermission() {
        return ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void fetchWeatherData(String unit) {
        // Get the user's last known location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Fetch the last known location
            android.location.LocationManager locationManager = (android.location.LocationManager) getSystemService(LOCATION_SERVICE);
            Location lastKnownLocation = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);

            if (lastKnownLocation != null) {
                if (isNetworkAvailable(this)) {

                    binding.progress.setVisibility(View.VISIBLE);
                    double latitude = lastKnownLocation.getLatitude();
                    double longitude = lastKnownLocation.getLongitude();

                    String cityName = getCity(latitude, longitude);
                    // Fetch weather data using ViewModel
                    weatherViewModel.fetchWeatherData(cityName, latitude, longitude, unit);

                    // Observe the weather data LiveData for updates
                    weatherViewModel.getWeatherData().observe(this, weatherResponse -> {
                        binding.progress.setVisibility(View.GONE);
                        if (weatherResponse != null) {
                            // Update the UI with weather data
                            updateUI(weatherResponse, unit);
                        } else {
                            Toast.makeText(this, "Failed to fetch weather data", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(this, "Internet connection not available", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Handle location permission not granted
            Toast.makeText(this, "Location permission required to fetch weather data", Toast.LENGTH_SHORT).show();
        }
    }

    private String getCity(double latitude, double longitude) {
        // Initialize the Geocoder
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            // Get the list of addresses for the given latitude and longitude
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses != null && addresses.size() > 0) {
                // Get the first address from the list
                Address address = addresses.get(0);

                // Get the city name from the address
                city = address.getLocality();

                return city;
            } else {
                Log.d("City Name", "No city found for the given coordinates.");
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void updateUI(WeatherResponse weatherResponse, String unit) {

        double temperatureValue = weatherResponse.getMain().getTemp();
        String formattedTemperature = String.format(Locale.getDefault(), "%.1f", temperatureValue);
        String temperatureUnit = unit.equals(UNIT_METRIC) ? "°C" : "°F";
        binding.textViewTemperature.setText(formattedTemperature + " " + temperatureUnit);

        binding.textViewWindSpeed.setText(weatherResponse.getWind().getDeg() + " m/s");
        binding.humidity.setText(weatherResponse.getMain().getHumidity() + "%");
        binding.name.setText(weatherResponse.getName());
        binding.main.setText(weatherResponse.getWeather().get(0).getMain() + "");
        long timestamp = weatherResponse.getDt();
        String formattedDateTime = convertUnixTimestampToDateTime(timestamp);
        binding.date.setText(formattedDateTime);

        // Convert rain intensity to a percentage
        if (weatherResponse.getRain() != null) {
            double maximumRainIntensityMmPerHour = 100; // Maximum value for heavy rain
            double rainIntensityMmPerHour = weatherResponse.getRain().get1h();
            double rainIntensityPercentage = (rainIntensityMmPerHour / maximumRainIntensityMmPerHour) * 100;

            // Update the TextView with the rain intensity percentage
            String formattedRainIntensity = getString(R.string.rain_intensity_format, rainIntensityPercentage);
            binding.textViewWeatherRain.setText(formattedRainIntensity);
        } else {
            binding.textViewWeatherRain.setText("0%");
        }

        // Load weather icon using Picasso with error handling
        String iconUrl = "https://openweathermap.org/img/w/" + weatherResponse.getWeather().get(0).getIcon() + ".png";
        Picasso.get()
                .load(iconUrl)
                .error(R.drawable.ic_launcher_background)
                .into(binding.imageViewWeatherIcon, new Callback() {
                    @Override
                    public void onSuccess() {
                        // Icon loaded successfully, do nothing
                    }

                    @Override
                    public void onError(Exception e) {
                        // Handle error while loading icon, e.g., show a placeholder image
                        binding.imageViewWeatherIcon.setImageResource(R.drawable.wind);
                    }
                });
    }

    // Handle the result of the location permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, fetch weather data
                fetchWeatherData(UNIT_IMPERIAL);
            } else {
                Toast.makeText(this, "Location permission required to fetch weather data", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Method to convert Unix timestamp to human-readable date and time
    private String convertUnixTimestampToDateTime(long unixTimestamp) {
        try {
            // Convert the Unix timestamp to milliseconds
            Date date = new Date(unixTimestamp * 1000L);

            // Create a SimpleDateFormat object to format the date and time
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMMM", Locale.getDefault());

            // Format the date and time
            return sdf.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return "N/A";
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        NetworkInfo mWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        boolean value = activeNetworkInfo != null && activeNetworkInfo.isConnected() || mWifi != null && mWifi.isConnected();

        if (!value) {
            Toast.makeText(context, "Internet connection not available !!!", Toast.LENGTH_SHORT).show();
        }
        return value;
    }
}
