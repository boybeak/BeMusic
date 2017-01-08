package com.nulldreams.media.model;

import java.io.Serializable;

/**
 * Created by boybe on 2017/1/6.
 */

public class Album implements Serializable{
    private int id, minYear, maxYear, numSongs;
    private String album, albumKey, artist, albumArt;

    public Album(int id, int minYear, int maxYear, int numSongs, String album, String albumKey, String artist, String albumArt) {
        this.id = id;
        this.minYear = minYear;
        this.maxYear = maxYear;
        this.numSongs = numSongs;
        this.album = album;
        this.albumKey = albumKey;
        this.artist = artist;
        this.albumArt = albumArt;
    }

    public int getId() {
        return id;
    }

    public int getMinYear() {
        return minYear;
    }

    public int getMaxYear() {
        return maxYear;
    }

    public int getNumSongs() {
        return numSongs;
    }

    public String getAlbum() {
        return album;
    }

    public String getAlbumKey() {
        return albumKey;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbumArt() {
        return albumArt;
    }
}
