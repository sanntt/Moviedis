package com.example.android.moviedis;

/**
 * Created by Milhouse on 08/02/2016.
 */
public class Movie {

    String id;
    String title;
    String posterUri;
    String releaseDate;
    String plotSynopsis;
    String voteAverage;

    public Movie(String id, String title, String posterUri, String releaseDate, String plotSynopsis, String voteAverage) {
        this.id = id;
        this.title = title;
        this.posterUri = posterUri;
        this.releaseDate = releaseDate;
        this.plotSynopsis = plotSynopsis;
        this.voteAverage = voteAverage;
    }

}
