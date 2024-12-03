package com.example.mediaexample

import android.content.ComponentName
import android.content.ContentResolver
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ListView
import android.widget.MediaController
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Collections


class MainActivity : AppCompatActivity(),MediaController.MediaPlayerControl  {

    private  var songList :ArrayList<Song> = ArrayList<Song>();
    private  var songView : ListView? = null;
    private  var musicService :  MusicService? = null;
    private  var playIntent : Intent? = null;
    private var musicBound : Boolean = false;
    private  var controller : MusicController? = null ;
    private var paused : Boolean = false;
    private var playbackPaused : Boolean = false;
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.song_list)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Kiểm tra quyền
        val isPermissionGranted = Environment.isExternalStorageManager()
        if (!isPermissionGranted) {
            // Quyền chưa được cấp, yêu cầu quyền
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.setData(Uri.parse("package:$packageName"))
            startActivity(intent)
        } else {
            // Quyền đã được cấp
            Log.d("Permission", "Permission already granted!")
        }

        // Retrieve list menu
        songView  = findViewById<ListView>(R.id.song_list)
        //Instantiate song list
        songList = ArrayList<Song>();
        // get songs from device
        getSongList();

        // Sort alphabetically by title
        Collections.sort(songList, Comparator{ lhs, rhs ->
            lhs.title.compareTo(rhs.title)
        })
        // Create and set adapter
        var songAdapter :SongAdapter = SongAdapter(this,songList);
        songView?.adapter = songAdapter;
        setController();

    }

    override fun onStart() {
        super.onStart();
        if(playIntent == null){
            playIntent = Intent(this, MusicService::class.java);
            bindService(playIntent!!,musicConnection , BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Quyền đã được cấp, thực hiện hành động
                Log.d("Permission", "Permission granted!")
            } else {
                // Người dùng từ chối quyền
                Log.d("Permission", "Permission denied!")
            }
        }
    }
    public fun getSongList() {
        val musicResolver: ContentResolver = contentResolver
         songList = mutableListOf<Song>() as ArrayList<Song>

        // Xác định URI cho bộ sưu tập âm nhạc ngoài
        val musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        // Chỉ định các cột bạn cần lấy, giúp tiết kiệm bộ nhớ
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST
        )

        // Thực hiện truy vấn và kiểm tra kết quả
        val musicCursor: Cursor? = musicResolver.query(
            musicUri,          // URI của bộ sưu tập âm nhạc ngoài
            projection,        // Projection chứa các cột cần lấy
            null,              // Điều kiện lọc (nếu có)
            null,              // Các tham số lọc (nếu có)
            MediaStore.Audio.Media.TITLE // Sắp xếp theo tên bài hát
        )

        // Kiểm tra và duyệt qua kết quả nếu có
        musicCursor?.use { cursor ->
            val titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            if (cursor.moveToFirst()) {
                do {
                    val thisId: Long = cursor.getLong(idColumn)
                    val thisTitle: String = cursor.getString(titleColumn)
                    val thisArtist: String = cursor.getString(artistColumn)

                    // Xử lý kết quả và in log
                    Log.d("Music", "ID: $thisId, Title: $thisTitle, Artist: $thisArtist")
                    songList.add(Song(thisId, thisTitle, thisArtist))  // Thêm vào danh sách songList
                } while (cursor.moveToNext())
            } else {
                Log.d("Music", "No music found.")
            }
        } // Cursor sẽ tự động đóng ở đây
        if (songList.isEmpty()) {
            Log.e("SongList", "No music found in the device")
        } else {
            // Làm gì đó với danh sách bài hát, ví dụ cập nhật UI
            Log.d("SongList", "Found ${songList.size} songs")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            // Kiểm tra nếu quyền đã được cấp
            if (Environment.isExternalStorageManager()) {
                // Nếu quyền đã được cấp, thực hiện truy cập bộ nhớ ngoài
                getSongList();
            } else {
                // Quyền bị từ chối
                Toast.makeText(this, "Permission denied, cannot access music files.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Connect with the service
    public val musicConnection =  object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder;
            musicService = binder.service
            musicService?.songs = songList
            musicBound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            musicBound = false;
        }
    }

    public fun  songPicked(v : View){
        musicService?.setSong(v.tag.toString().toInt());
        musicService?.playSong();
        if(playbackPaused){
            setController();
            playbackPaused = false;
        }
        controller?.show(0);
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        var menuInflater : MenuInflater = menuInflater;
        menuInflater.inflate(R.menu.menu_main,menu);
        return true
    }

    override fun onOptionsItemSelected(item : MenuItem) : Boolean{
        when(item.itemId){
            R.id.action_shuffle -> {
                musicService?.setShuffle();
            }
            R.id.action_end -> {
                stopService(playIntent)
                musicService = null;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    override fun onDestroy() {
        stopService(playIntent);
        musicService = null;
        super.onDestroy()
    }

    private fun setController() {
        controller = MusicController(this);
        controller?.setPrevNextListeners(
            View.OnClickListener { playNext() } ,
            View.OnClickListener { playPrev() }
        )

        controller?.setMediaPlayer(this);
        controller?.setAnchorView(findViewById(R.id.song_list))
        controller?.isEnabled = true;
    }

    private fun playNext() {
        musicService?.playNext();
        if(playbackPaused){
            setController();
            playbackPaused = false;
        }
        controller?.show(0);
    }
    public fun playPrev() {
        musicService?.playPrev()
        if(playbackPaused){
            setController();
            playbackPaused = false;
        }
        controller?.show(0);
    }
    override fun onPause() {
        super.onPause();
        paused = true;
    }
    override fun onResume() {
        super.onResume()
        if(paused){
            setController();
            paused = false
        }
    }

    override fun onStop(){
        controller?.hide();
        super.onStop();
    }

    override fun start() {
        musicService?.go()
    }

    override fun pause() {
       playbackPaused =true;
        musicService?.pausePlayer();
    }

    override fun getDuration(): Int {
        if(musicService != null && musicBound && musicService?.isPlaying == true){
            return musicService?.dur!!;
        }
        return 0;
    }

    override fun getCurrentPosition(): Int {
        return 0;
    }

    override fun seekTo(pos: Int) {
        musicService?.seek(pos);
    }

    override fun isPlaying(): Boolean {
        if(musicService != null  && musicBound){
            return musicService?.isPlaying() == true;
        }else {
            return false;
        }
    }

    override fun getBufferPercentage(): Int {
        return 0;
    }

    override fun canPause(): Boolean {
        return true;
    }

    override fun canSeekBackward(): Boolean {
        return true;
    }

    override fun canSeekForward(): Boolean {
        return true;
    }

    override fun getAudioSessionId(): Int {
        return 0;
    }


}