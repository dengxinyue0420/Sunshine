package com.example.android.sunshine.app;


import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by xydeng on 6/22/16.
 */
public class ForecastFragment extends Fragment {

    private final String LOG_TAG = ForecastFragment.class.getSimpleName();
    private ArrayAdapter<String> forecastAdapter;
    /**
     * A placeholder fragment containing a simple view.
     */
    public ForecastFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            try {
                FetchWeatherTask fetchWeather = new FetchWeatherTask();
                fetchWeather.execute("94043,us");
                return true;
            }catch (Exception e){
                Log.e("ForecastFragment", "Error", e);
            }

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        ArrayList<String> forecast = new ArrayList<String>();
        forecast.add("Today - Sunny - 88/71");
        forecast.add("Tomorrow - Sunny - 87/73");
        forecast.add("Weds - Rainy - 66/51");
        forecast.add("Thurs - Sunny - 83/62");
        forecast.add("Fri - Sunny - 91/82");
        forecast.add("Sat - Cloudy - 81/72");
        forecast.add("Sun - Cloudy - 73/65");

        forecastAdapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item_forecast, forecast);
        ListView forecastLv = (ListView)rootView.findViewById(R.id.listview_forecast);
        forecastLv.setAdapter(forecastAdapter);

        return rootView;
    }

    private class FetchWeatherTask extends AsyncTask<String, Void, String[]>{

        private String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        private final String base_url = "http://api.openweathermap.org/data/2.5/forecast/daily?";
        private final String query_param = "q";
        private final String unit_param = "units";
        private final String day_param = "cnt";
        private final String key_param = "APPID";
        private final String api_key = "69e1a0cc3b4af661c3b543bd28466d9b";

        private final int num_days = 7;


        protected String[] doInBackground(String...params){
            HttpURLConnection urlconn = null;
            BufferedReader br = null;

            String forecastJsonStr = "";

            Uri builtURI = Uri.parse(base_url).buildUpon()
                    .appendQueryParameter(query_param, params[0])
                    .appendQueryParameter(unit_param, "metric")
                    .appendQueryParameter(day_param, Integer.toString(num_days))
                    .appendQueryParameter(key_param, api_key)
                    .build();

            try{
                URL url = new URL(builtURI.toString());
                urlconn = (HttpURLConnection) url.openConnection();
                urlconn.setRequestMethod("GET");
                urlconn.connect();

                InputStream is = urlconn.getInputStream();
                StringBuffer sb = new StringBuffer();
                if (is == null) {
                    return null;
                }
                br = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = br.readLine())!=null) {
                    sb.append(line + "\n");
                }

                if (sb.length() == 0) {
                    return null;
                }
                forecastJsonStr = sb.toString();

            }catch (IOException e){
                Log.e(LOG_TAG, "Error", e);
                return null;
            }finally {
                if (urlconn != null) {
                    urlconn.disconnect();
                }
                if (br != null) {
                    try{
                        br.close();
                    }catch (final IOException e){
                        Log.e(LOG_TAG, "Error", e);
                    }
                }
            }

            try{
                return getWeatherDataFromJson(forecastJsonStr, num_days);
            }catch (JSONException e){
                Log.e(LOG_TAG, "Error", e);
            }
            return null;
        }

        @Override
        public void onPostExecute(String[] weatherData){
            if(weatherData != null) {
                forecastAdapter.clear();
                for(String s: weatherData){
                    forecastAdapter.add(s);
                }
            }
        }

        /* The date/time conversion code is going to be moved outside the asynctask later,
         * so for convenience we're breaking it out into its own method now.
         */
        private String getReadableDateString(long time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }
            return resultStrs;

        }
    }
}
