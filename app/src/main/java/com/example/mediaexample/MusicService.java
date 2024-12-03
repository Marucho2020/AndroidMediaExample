package com.example.mediaexample;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaParser;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.os.Binder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener ,
        MediaPlayer.OnCompletionListener{

    private MediaPlayer player;
    private ArrayList<Song> songs;
    private int songPos;
    private final IBinder musicBind = new MusicBinder();
    private String songTitle = "";
    private static final int NOTIFY_ID = 1;
    private boolean shuffle = false;
    private Random rand;



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        songPos = 0;
        //khởi tạo một bộ phát đa phương tiện mới
        player = new MediaPlayer();
    }
    public void initMusicPlayer() {
        player.setWakeMode(getApplicationContext() , PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnErrorListener(this);
        player.setOnCompletionListener(this);
    }



    @Override
    public void onCompletion(MediaPlayer mp) {
        if(player.getCurrentPosition() > 0){
            mp.reset();
            playNext();
        }
    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        Intent notificationIntent = new Intent(this,MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,0,notificationIntent,PendingIntent.FLAG_UPDATE_CURRENT
        );
        Notification.Builder  nBuilder = new Notification.Builder(this);
        nBuilder.setContentIntent(pendingIntent)
                .setTicker(songTitle)
                .setSmallIcon(R.drawable.play)
                .setContentTitle("Playing")
                .setContentText(songTitle);
        Notification notif = nBuilder.build();
        startForeground(NOTIFY_ID , notif);
    }


    @Override
    public boolean onUnbind(Intent intent){
        player.stop();
        player.release();
        return false;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
    }


    public void playSong() {
        player.reset();
        Song playSong = songs.get(songPos);
        songTitle = playSong.getTitle();
        Long currentSong  = playSong.getId();
        Uri trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currentSong);
        try{
            player.setDataSource(getApplicationContext(), trackUri);
        } catch (IOException e) {
            Log.e("MUSIC SERVICE" , "Error stating data source" , e);
        }
        player.prepareAsync();
    }




    public class MusicBinder extends Binder{
        MusicService getService() {
            return MusicService.this;
        }
    }

    public void playNext() {
        if(shuffle){
            int newSongPos = songPos;
            while(newSongPos == songPos){
                newSongPos = rand.nextInt(songs.size());
            }
            songPos = newSongPos;
        }else {
            songPos++;
            if(songPos >= songs.size()) songPos = 0;
        }
        playSong();
    }

    public void playPrev() {
        songPos--;
        if(songPos < 0) songPos = songs.size() - 1;
        playSong();
    }
    public void go() {
        player.start();
    }
    public void seek(int pos){
        player.seekTo(pos);
    }
    public void pausePlayer() {
        player.pause();
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }

    public int getDur() {
        return player.getDuration();
    }

    public MediaPlayer getPlayer() {
        return player;
    }

    public void setPlayer(MediaPlayer player) {
        this.player = player;
    }
    public void setSongs(ArrayList<Song> songs) {
        this.songs = songs;
    }

    public void setSong(int songIndex){
        songPos = songIndex;
    }
    public ArrayList<Song> getSongs() {
        return songs;
    }

    public int getSongPos(){
        return player.getCurrentPosition();
    }

    public void setSongPos(int songPos) {
        this.songPos = songPos;
    }

    public String getSongTitle() {
        return songTitle;
    }

    public void setSongTitle(String songTitle) {
        this.songTitle = songTitle;
    }

    public boolean isShuffle() {
        return shuffle;
    }

    public void setShuffle() {
        shuffle = !shuffle;
    }

    public IBinder getMusicBind() {
        return musicBind;
    }

    public Random getRand() {
        return rand;
    }

    public void setRand(Random rand) {
        this.rand = rand;
    }
}
