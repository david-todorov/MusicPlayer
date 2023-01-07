package com.example.musicplayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private MP3[] mp3s;
    private int ID;
    private MP3ListAdapter adapter;
    private int progress;

    private TextView artistNameView;
    private TextView songNameView;
    private TextView albumNameView;
    private TextView currentTime;
    private TextView finishTime;
    private ListView songsList;
    private SeekBar progressBar;

    private static final int MY_PERMISSION_REQUEST = 123;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FD9F01")));

        this.mediaPlayer = new MediaPlayer();
        this.songsList = findViewById(R.id.song_list_view);
        this.artistNameView = findViewById(R.id.artist_name);
        this.songNameView = findViewById(R.id.song_name);
        this.albumNameView = findViewById(R.id.album_name);
        this.currentTime = findViewById(R.id.current_time);
        this.finishTime = findViewById(R.id.finish_time);
        this.progressBar = findViewById(R.id.seekbar);
        this.progressBar.getProgressDrawable().setColorFilter(getResources().getColor(R.color.app_orange), PorterDuff.Mode.MULTIPLY);

        //Asking for permissions
        askingForPermissions();
        //Loading the songs
        loadSongs();
        displaySongs();

        //Changing the progress bar accordingly
        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                if(mediaPlayer != null && fromTouch){
                    mediaPlayer.seekTo(progress);
                    MainActivity.this.progress = progress;
                }

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.seekTo(seekBar.getProgress());
                    progress= seekBar.getProgress();
                }
            }
        });

        //Playing specific song from listView aka songList
        this.songsList.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                int id = adapter.getItem(i).getId();
                setID(id);
                progressBar.setEnabled(true);
                playMP3(getMP3byID(ID), 0);
                displayMP3Details();
            }
        });

        //Runnable which updates the progress bar
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null){
                    progressBar.setProgress(mediaPlayer.getCurrentPosition());
                    currentTime.setText(convertToMMSS(mediaPlayer.getCurrentPosition()));
                    progress = mediaPlayer.getCurrentPosition();
                }
                new Handler().postDelayed(this,1000);
            }
        });

    }

    synchronized private void loadSongs() {

        //Creating the projection
        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + " !=0";
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, null);
        this.mp3s = new MP3[cursor.getCount()];
        int index = 0;
        //Iterating over the results
        while (cursor.moveToNext()){

            String songName = cursor.getString(0);
            String albumName = cursor.getString(1);
            String artistName = cursor.getString(2);
            int id = Integer.parseInt(cursor.getString(3));
            Bitmap cover = ((BitmapDrawable) getResources().getDrawable(R.drawable.song_icon_orange)).getBitmap();

            String path = cursor.getString(4);

            //Adding new MP3 to the collectionn
            this.mp3s[index] = new MP3(artistName, songName, albumName, cover, id, path);
            index++;
        }
        cursor.close();
    }


   synchronized private void askingForPermissions() {
        if (ContextCompat.checkSelfPermission(
                MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {

        }
         else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                    MY_PERMISSION_REQUEST);

        }
    }

    synchronized private void displaySongs() {
        //Populating the ListView
       this.adapter = new MP3ListAdapter(this, this.mp3s);
       this.songsList.setAdapter(this.adapter);
    }


    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Saving the ID, progress of the currently played song and boolean if its being played at the given time
        editor.putInt("currentId", this.ID);
        editor.putInt("currentProgress", this.progress);
        editor.putBoolean("wasPlaying", this.mediaPlayer.isPlaying());
        editor.apply();

    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);

        //Retrieving the data
        this.ID =prefs.getInt("currentId", -1);

        if(this.ID != -1){
            this.progress = prefs.getInt("currentProgress", 0);
            boolean wasPlaying = prefs.getBoolean("wasPlaying", false);

            //Starting the song again
            playMP3(getMP3byID(this.ID), this.progress);

            //if it was being played, not paused we just set the the progress bar
            if (wasPlaying){
                this.progressBar.setEnabled(true);
            }
            //Otherwise we pause it again
            else {
                this.mediaPlayer.pause();
                this.progressBar.setEnabled(false);
            }

            this.displayMP3Details();
        }

    }

    //Converting milliseconds to minutes and seconds with format 00:00
    private String convertToMMSS(int duration){

       return String.format("%02d:%02d",
               TimeUnit.MILLISECONDS.toMinutes(duration) % TimeUnit.HOURS.toMinutes(1),
               TimeUnit.MILLISECONDS.toSeconds(duration) % TimeUnit.MINUTES.toSeconds(1)
                );

    }

    // Pausing and resuming the currently played song
    public void playPause(View view) {
        try {
            if(this.mediaPlayer.isPlaying() == false){
                this.mediaPlayer.seekTo(this.progress);
                this.mediaPlayer.start();
                this.progressBar.setEnabled(true);
            }
            else {
                this.mediaPlayer.pause();
                this.progressBar.setEnabled(false);
            }
        }
        catch (Exception e){
            Toast noSongToast =  Toast.makeText(MainActivity.this, "No song is selected in order to be paused or played",Toast.LENGTH_SHORT);
            noSongToast.show();
        }

    }


    private void setID(int newID){
        this.ID = newID;
    }

    //Selecting random song from our song collection
    synchronized public void selectRandom(View view) {
        setID(getRandomSongId());
        this.playMP3(getMP3byID(this.ID), 0);
        this.displayMP3Details();
    }

    // Restart the current song
    synchronized public void restart(View view) {
        try {
            this.playMP3(this.getMP3byID(this.ID), 0);
        }
        catch (Exception e){
            Toast noSongToast =  Toast.makeText(MainActivity.this, "No song is selected in order to be restarted",Toast.LENGTH_SHORT);
            noSongToast.show();
        }
    }

    //Getting the specific MP3 with from id
    synchronized private MP3 getMP3byID(int id){

        for(int i = 0; i<this.mp3s.length; i++){
            if (this.mp3s[i].getId() == id){
                return this.mp3s[i];
            }
        }
        return null;
    }

    //Getting random id of our song collection
    synchronized private int getRandomSongId(){

        try {
            Random random = new Random();
            int randomIndex = random.nextInt(this.mp3s.length);
            return this.mp3s[randomIndex].getId();
        }
        catch (Exception e){
            e.printStackTrace();
            return  -1;
        }
    }
    //Playing the song, second parameter gives us the ability to chose from where to start the song
    synchronized private void playMP3(MP3 mp3, int from){

        try {
            this.mediaPlayer.stop();
            this.mediaPlayer.reset();
            this.mediaPlayer.setDataSource(mp3.getPath());
            this.mediaPlayer.prepare();
            this.mediaPlayer.seekTo(from);
            this.mediaPlayer.start();
            this.mediaPlayer.setLooping(true);

            this.finishTime.setText(convertToMMSS(mediaPlayer.getDuration()));

            this.progressBar.setEnabled(true);
            this.progressBar.setProgress(from);
            progressBar.setMax(mediaPlayer.getDuration());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //We display the details about the song, Artist, Song name, Album...
    synchronized private void displayMP3Details(){
        this.artistNameView.setText( this.getMP3byID(this.ID).getArtistName());
        this.songNameView.setText(this.getMP3byID(this.ID).getSongName());
        this.albumNameView.setText(this.getMP3byID(this.ID).getAlbumName());
    }



}