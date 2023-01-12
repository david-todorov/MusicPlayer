package com.example.musicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MP3ListAdapter extends BaseAdapter {

    private Context context;
    private MP3[] mp3s;
    LayoutInflater inflater;

    public MP3ListAdapter(Context context, MP3[] mp3s){
        this.context = context;
        this.mp3s = mp3s;
        this.inflater = LayoutInflater.from(context);
    }


    @Override
    public int getCount() {
        return this.mp3s.length;
    }

    @Override
    public MP3 getItem(int i) {

        return this.mp3s[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        if(view == null){
            view = this.inflater.inflate(R.layout.sont_list_item, null);

        }

        TextView names = (TextView) view.findViewById(R.id.songName_view_in_song_list);
        names.setText(this.mp3s[i].getSongName());

        ImageView cover = (ImageView) view.findViewById(R.id.icon_view_in_song_list);
        cover.setImageBitmap(this.mp3s[i].getCover());

        return view;
    }
}
