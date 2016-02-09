package com.example.android.moviedis;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MovieDetailActivity extends AppCompatActivity {

    public static View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.detail_container, new MovieDetailFragment())
                    .commit();
        }
    }

    public class MovieDetailFragment extends Fragment {

        private final String LOG_TAG = MovieDetailFragment.class.getSimpleName();
        private String mMovieStr;

        public MovieDetailFragment() {

        }

        @Override
        public void onStart() {
            super.onStart();

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            rootView = inflater.inflate(R.layout.fragment_movie_detail, container, false);

            // The detail Activity called via intent.  Inspect the intent for forecast data.
            Intent intent = getActivity().getIntent();
            if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
                String movieId = intent.getStringExtra(Intent.EXTRA_TEXT);
                findMovie(movieId);

            }

            return rootView;
        }

        private void findMovie(String id) {
            FindMovieTask movieTask = new FindMovieTask();
            movieTask.execute(id);
        }


    }

    public class FindMovieTask extends AsyncTask<String, Void, Movie> {

        private final String LOG_TAG = FindMovieTask.class.getSimpleName();

        @Override
        protected void onPostExecute(Movie movie) {
            if (movie != null) {
                ((TextView) rootView.findViewById(R.id.movie_detail_title)).setText(movie.title);
                ((TextView) rootView.findViewById(R.id.movie_detail_vote_average)).setText(movie.voteAverage);
                ((TextView) rootView.findViewById(R.id.movie_detail_release)).setText(movie.releaseDate);
                ImageView iconView = (ImageView) rootView.findViewById(R.id.movie_detail_poster);
                Picasso.with(getApplicationContext()).load("http://image.tmdb.org/t/p/w185/" + movie.posterUri).into(iconView);
                ((TextView) rootView.findViewById(R.id.movie_detail_plot)).setText(movie.plotSynopsis);
            }
        }

        @Override
        protected Movie doInBackground(String... params) {

            if (params.length == 0) {
                return null;
            }

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String movieJsonStr = null;

            try {
                // Construct the URL for the TMDB Api query
                final String MOVIES_BASE_URL = "http://api.themoviedb.org/3/movie/";
                final String API_KEY_PARAM = "api_key";

                Uri builtUri = Uri.parse(MOVIES_BASE_URL).buildUpon()
                        .appendPath(params[0])
                        .appendQueryParameter(API_KEY_PARAM, BuildConfig.THE_MOVIE_DATABASE_API_KEY)
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
                movieJsonStr = buffer.toString();
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
                return getMovieDataFromJson(movieJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the forecast.
            return null;
        }

        private Movie getMovieDataFromJson(String movieJsonStr) throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String TMDB_ID = "id";
            final String TMDB_POSTER = "poster_path";
            final String TMDB_OVERVIEW = "overview";
            final String TMDB_RELEASE = "release_date";
            final String TMDB_TITLE = "title";
            final String TMDB_AVERAGE = "vote_average";

            JSONObject jsonMovie = new JSONObject(movieJsonStr);

            // For now, using the format "Movie Title - Release Date"
            String id = jsonMovie.getString(TMDB_ID);
            String title = jsonMovie.getString(TMDB_TITLE);
            String release = getYearFromDate(jsonMovie.getString(TMDB_RELEASE));
            String posterUri = jsonMovie.getString(TMDB_POSTER);
            String plot = jsonMovie.getString(TMDB_OVERVIEW);
            String voteAverage = jsonMovie.getString(TMDB_AVERAGE) + "/10";

            Movie movie = new Movie(id, title, posterUri, release, plot, voteAverage);

            return movie;
        }

        private String getYearFromDate(String dateStr) {
            DateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
            DateFormat outputFormat = new SimpleDateFormat("yyyy");
            try {
                Date date = inputFormat.parse(dateStr);
                return outputFormat.format(date);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error parsing date", e);
            }
            return dateStr;
        }
    }
}
