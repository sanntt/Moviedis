package com.example.android.moviedis;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Milhouse on 08/02/2016.
 */
public class MovieFragment extends Fragment {

    private ArrayAdapter<String> mMovieAdapter;

    public MovieFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // The ArrayAdapter will take data from a source and
        // use it to populate the ListView it's attached to.
        mMovieAdapter =
                new ArrayAdapter<String>(
                        getActivity(), // The current context (this activity)
                        R.layout.list_item_movie, // The name of the layout ID.
                        R.id.list_item_movie_textview, // The ID of the textview to populate.
                        new ArrayList<String>());

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        ListView listView = (ListView) rootView.findViewById(R.id.listview_movie);
        listView.setAdapter(mMovieAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String movie = mMovieAdapter.getItem(position);
                Intent intent = new Intent(getActivity(), MovieDetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, movie);
                startActivity(intent);
            }
        });

        return rootView;
    }

    private void updateMovies() {
        FetchMoviesTask moviesTask = new FetchMoviesTask();
        // SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        moviesTask.execute();
    }

    @Override
    public void onStart() {
        super.onStart();
        updateMovies();
    }

    public class FetchMoviesTask extends AsyncTask<Void, Void, String[]> {

        private final String LOG_TAG = FetchMoviesTask.class.getSimpleName();

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                mMovieAdapter.clear();
                for (String movieStr : result) {
                    mMovieAdapter.add(movieStr);
                }
                // New data is back from the server.  Hooray!
            }
        }

        @Override
        protected String[] doInBackground(Void... params) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String moviesJsonStr = null;

            try {
                // Construct the URL for the TMDB Api query
                final String MOVIES_BASE_URL =
                        "http://api.themoviedb.org/3/discover/movie";
                final String API_KEY_PARAM = "api_key";

                Uri builtUri = Uri.parse(MOVIES_BASE_URL).buildUpon()
                        // TODO: This should not be here
                        .appendQueryParameter(API_KEY_PARAM, "")
                        .build();

                URL url = new URL(builtUri.toString());

                // Create the request to TMDB, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                moviesJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getMoviesDataFromJson(moviesJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the forecast.
            Log.v(LOG_TAG, "This message should not be logged");
            return null;
        }
    }

    private String[] getMoviesDataFromJson(String moviesJsonStr) throws JSONException{

        // These are the names of the JSON objects that need to be extracted.
        final String TMDB_RESULTS = "results";
        final String TMDB_POSTER = "poster_path";
        final String TMDB_OVERVIEW = "overview";
        final String TMDB_RELEASE = "release_date";
        final String TMDB_TITLE = "title";
        final String TMDB_AVERAGE = "vote_average";

        JSONObject moviesJson = new JSONObject(moviesJsonStr);
        JSONArray moviesArray = moviesJson.getJSONArray(TMDB_RESULTS);

        String[] resultStrs = new String[moviesArray.length()];

        for(int i = 0; i < moviesArray.length(); i++) {
            // Get the JSON object representing the movie
            JSONObject movie = moviesArray.getJSONObject(i);

            // For now, using the format "Movie Title - Release Date"
            String title = movie.getString(TMDB_TITLE);
            String release = movie.getString(TMDB_RELEASE);

            resultStrs[i] = title + " - " + release;
        }
        return resultStrs;
    }

}
