package com.example.mediaexample;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class SongAdapter extends BaseAdapter {
    private ArrayList<Song> songs;
    private LayoutInflater songInf;

    public SongAdapter() {
    }

    public SongAdapter(Context c , ArrayList<Song> theSongs) {
        this.songs = theSongs;
        songInf = LayoutInflater.from(c);
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //Ánh xạ đến layout mỗi bài
        LinearLayout songLayout = (LinearLayout) songInf.inflate(R.layout.song , parent , false);
        TextView songView = (TextView) songLayout.findViewById(R.id.song_title);
        TextView artistView = (TextView) songLayout.findViewById(R.id.song_artist);

        //Lấy bài hát thực hiện
        Song currentSong = songs.get(position);
        // Lấy tiêu đề và lời bài hát
        songView.setText(currentSong.getTitle());
        artistView.setText(currentSong.getArtist());

        // Cài dặt tag cho mỗi bài là vị trí của mỗi position
        songLayout.setTag(position);
        return songLayout;

    }
}
