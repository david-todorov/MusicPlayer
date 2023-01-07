package com.example.musicplayer;

import android.graphics.Bitmap;

public class MP3 {
    private String artistName;
    private String songName;
    private String albumName;
    private String path;
    private int id;
    private Bitmap cover;

    public MP3(String artistName, String songName, String albumName, Bitmap cover , int id, String path) {
        this.artistName = artistName;
        this.songName = songName;
        this.albumName = albumName;
        this.path = path;
        this.id = id;
        this.cover = cover;
    }

    public String getAlbumName() {
        return albumName;
    }

    public String getSongName() {
        return songName;
    }

    public String getArtistName() {
        return artistName;
    }


    public int getId() {
        return id;
    }

    public Bitmap getCover() {
        return cover;
    }

    public String getPath() {
        return path;
    }
}
