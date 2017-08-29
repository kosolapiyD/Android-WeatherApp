package com.medved.weatherapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<Weather> weatherList = new ArrayList<>();
    private WeatherArrayAdapter weatherArrayAdapter;
    private ListView weatherListView;

    private GPSTracker mGPSTracker;
    double latitude;
    double longitude;

    private TextView cityTextView;
    private TextView descriptionTextView;
    private TextView tempTextView;
    private TextView dateTextView;
    private Button locationBtn;

    public static final  String API_KEY = "2be7df40deb484484670de1748596543";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        GPSAuthorization();
        UIElements();
        showCurrentDate();

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText locationEditText = (EditText) findViewById(R.id.locationEditText);
                String city = locationEditText.getText().toString();

                if (city.matches("[^\\s]{0,2}")) { // at least 3 char regex
                    Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.invalid_location, Snackbar.LENGTH_LONG).show();
                } else {
                    URL locationURL = createLocationURL(city);
                    // hide the keyboard and run GetWeatherTask() to get the current weather data by city name on background thread
                    dismissKeyboard(locationEditText);
                    GetWeatherTask getLocalWeatherTask = new GetWeatherTask();
                    getLocalWeatherTask.execute(locationURL);

                    URL url = createSearchURL(city);
                    // and run GetWeatherTask() to get the 16 days forecast data by the same city on background thread
                    GetWeatherTask getWeatherTask = new GetWeatherTask();
                    getWeatherTask.execute(url);

                    locationEditText.setText(""); // remove text from search bar after fab is clicked
                }
            }
        });

        locationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onResume();
            }
        });
    }

    // onResume() is called when activity will start interacting with the user or in our case after GPS is turned ON
    // sometimes it take a few seconds to GPS after it turned ON to recognize current position even if we got a latitude and longitude !
    @Override
    protected void onResume() {
        super.onResume();

        mGPSTracker = new GPSTracker(MainActivity.this);
        if (mGPSTracker.canGetLocation()) {
            latitude = mGPSTracker.getLatitude();
            longitude = mGPSTracker.getLongitude();
        }
        // download and show current weather by location
        URL locationURL = createLocationURL("");
        GetWeatherTask getLocalWeatherTask = new GetWeatherTask();
        getLocalWeatherTask.execute(locationURL);

        // download and show 16 days weather by the same location
        URL searchURL = createSearchURL("");
        GetWeatherTask getWeatherTask = new GetWeatherTask();
        getWeatherTask.execute(searchURL);
    }

    private void GPSAuthorization() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.INTERNET
                        }, 10);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mGPSTracker = new GPSTracker(MainActivity.this);

        if (mGPSTracker.canGetLocation()) {
            latitude = mGPSTracker.getLatitude();
            longitude = mGPSTracker.getLongitude();

        } else {
            mGPSTracker.showSettingsAlert();
        }
    }

    private void UIElements(){
        cityTextView = (TextView) findViewById(R.id.cityTextView);
        descriptionTextView = (TextView) findViewById(R.id.descriptionTextView);
        tempTextView = (TextView) findViewById(R.id.tempTextView);
        dateTextView = (TextView) findViewById(R.id.dateTextView);
        locationBtn = (Button) findViewById(R.id.locationBtn);

        weatherListView = (ListView) findViewById(R.id.weatherListView);
        weatherArrayAdapter = new WeatherArrayAdapter(this, weatherList);
        weatherListView.setAdapter(weatherArrayAdapter);
    }

    private void showCurrentDate(){
        DateFormat df = new SimpleDateFormat(getString(R.string.date_format));
        String currentDate = df.format(Calendar.getInstance().getTime());
        dateTextView.setText(currentDate);
    }

    private void dismissKeyboard(View view) {
        // the return value is casting to (InputMethodManager) because this method can return many different types of objects
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private URL createLocationURL(String city) { // current weather by current location on start up, or by the searched city
        String locationBaseUrl = "http://api.openweathermap.org/data/2.5/weather?q=";
        String locationUrl = "http://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude;
        try {
            String urlString;
            // if it's empty - use locationUrl, if there is a city - use locationBaseUrl, in both cases update CURRENT weather
            if (!city.equals("")) {
                urlString = locationBaseUrl + URLEncoder.encode(city, "UTF-8") + "&units=metric&APPID=" + API_KEY;
            } else {
                urlString = locationUrl + "&units=metric&APPID=" + API_KEY;
            }
//            Log.i("createLocationURL", urlString);
            return new URL(urlString);
        } catch (Exception e) {
            Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.invalid_url, Snackbar.LENGTH_LONG).show();
            e.printStackTrace();
        }
        return null; // invalid URL
    }

    private URL createSearchURL(String city) {
        String searchBaseUrl = "http://api.openweathermap.org/data/2.5/forecast/daily?q=";
        String searchLocationUrl = "http://api.openweathermap.org/data/2.5/forecast/daily?lat=" + latitude + "&lon=" + longitude;
        try {
            String urlString;
            // if it's empty - use searchLocationUrl, if there is a city - use searchBaseUrl, in both cases update 16 days FORECAST
            if (!city.equals("")) {
                urlString = searchBaseUrl + URLEncoder.encode(city, "UTF-8") + "&units=metric&cnt=16&APPID=" + API_KEY;
            } else {
                urlString = searchLocationUrl + "&units=metric&cnt=16&APPID=" + API_KEY;
            }
//            Log.i("createSearchURL", urlString);
            return new URL(urlString);
        } catch (Exception e) {
            Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.invalid_url, Snackbar.LENGTH_LONG).show();
            e.printStackTrace();
        }
        return null; // invalid URL
    }

    private class GetWeatherTask extends AsyncTask<URL, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(URL... params) {
            HttpURLConnection connection = null;

            try {
                connection = (HttpURLConnection) params[0].openConnection();
                int response = connection.getResponseCode();

                if (response == HttpURLConnection.HTTP_OK) {
                    // if connection is OK and the data received
                    StringBuilder builder = new StringBuilder();

                    // get the stream - InputStream of HttpURLConnection class, 'packs' it into a BufferedReader,
                    // reads every single text line from the response text and add it to a StringBuilder
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {

                        String line;

                        while ((line = reader.readLine()) != null) {
                            builder.append(line);
                        }
                    } catch (IOException ioe) {
                        Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.read_error, Snackbar.LENGTH_LONG).show();
                        ioe.printStackTrace();
                    }
                    // this converts and returns JSONObject from a response String (text) that was appended to the StringBuilder,
                    // and returns it to main thread, if there is no errors
                    return new JSONObject(builder.toString());
                } else {
                    Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.connect_error, Snackbar.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.connect_error, Snackbar.LENGTH_LONG).show();
                e.printStackTrace();
            } finally {
                connection.disconnect(); // close the HttpURLConnection
            }
            return null;
        }

        // get the JSON response and update ListView
        @Override
        protected void onPostExecute(JSONObject jsonObjectWeather) {
            convertJSONForecast(jsonObjectWeather); // fill in the weatherList
            convertJSONCurrentWeather(jsonObjectWeather); // fill in the current day weather
            weatherArrayAdapter.notifyDataSetChanged();
            weatherListView.smoothScrollToPosition(0); // scroll to the top of the list
//            Log.i("onPostExecute(JSON", jsonObjectWeather.toString());
        }

        private void convertJSONCurrentWeather(JSONObject current) {

            try {
                String name = current.getString("name");
                cityTextView.setText(String.valueOf(name));

                JSONObject weather = current.getJSONArray("weather").getJSONObject(0);
                String description = weather.getString("main");
                descriptionTextView.setText(String.valueOf(description));

                JSONObject main = current.getJSONObject("main");
                Double temp = main.getDouble("temp");
                tempTextView.setText(String.valueOf(Math.round(temp)) + " \u00B0C");

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        private void convertJSONForecast(JSONObject forecast) {
            weatherList.clear();

            try {

                JSONArray list = forecast.getJSONArray("list");

                for (int i = 0; i < list.length(); ++i) {
                    JSONObject day = list.getJSONObject(i); // one day data

                    JSONObject temperatures = day.getJSONObject("temp");

                    JSONObject weather = day.getJSONArray("weather").getJSONObject(0);

                    // add new Weather object into a weatherList
                    weatherList.add(new Weather(
                            day.getLong("dt"),
                            temperatures.getDouble("min"),
                            temperatures.getDouble("max"),
                            weather.getString("description"),
                            weather.getString("icon")
                    ));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}










