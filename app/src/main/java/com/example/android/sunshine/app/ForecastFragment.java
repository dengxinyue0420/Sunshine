package com.example.android.sunshine.app;


import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by xydeng on 6/22/16.
 */
public class ForecastFragment extends Fragment {

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
                new FetchWeatherTask().execute("02139,us");
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

        ArrayAdapter<String> forecastAdapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item_forecast, forecast);
        ListView forecastLv = (ListView)rootView.findViewById(R.id.listview_forecast);
        forecastLv.setAdapter(forecastAdapter);

        return rootView;
    }

    private class FetchWeatherTask extends AsyncTask<String, Void, String>{

        private String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        private final String base_url = "http://api.openweathermap.org/data/2.5/forecast/daily?";
        private final String query_param = "q";
        private final String unit_param = "units";
        private final String day_param = "cnt";
        private final String key_param = "APPID";
        private final String api_key = "69e1a0cc3b4af661c3b543bd28466d9b";


        protected String doInBackground(String...params){
            HttpURLConnection urlconn = null;
            BufferedReader br = null;

            String forecastJsonStr = "";

            Uri builtURI = Uri.parse(base_url).buildUpon()
                    .appendQueryParameter(query_param, params[0])
                    .appendQueryParameter(unit_param, "metric")
                    .appendQueryParameter(day_param, "7")
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
                    return "";
                }
                br = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = br.readLine())!=null) {
                    sb.append(line + "\n");
                }

                if (sb.length() == 0) {
                    return "";
                }
                forecastJsonStr = sb.toString();
                Log.d(LOG_TAG, forecastJsonStr);
            }catch (IOException e){
                Log.e(LOG_TAG, "Error", e);
                return "";
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
            return forecastJsonStr;
        }
    }
}
