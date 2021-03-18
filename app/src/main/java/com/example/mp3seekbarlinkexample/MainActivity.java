package com.example.mp3seekbarlinkexample;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ListView listViewMP3;
    private Button btnPlay, btnStop, btnPause;
    private TextView tvMP3, tvTime;
    private SeekBar sbMP3;
    private boolean flag = true;
    private ArrayList<String> mp3List = new ArrayList<String>();
    private ArrayList<String> mp3ListSdcard = new ArrayList<String>();
    private ArrayList<MusicData> sdCardList = new ArrayList<MusicData>();

    private SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");

    private String MP3PATH = Environment.getExternalStorageDirectory().getPath() + "/Music/";
    MediaPlayer mPlayer;
    private String selectedMP3;
    private int selectPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("플레이 리스트인거임");
        findViewByIdFunc();

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, MODE_PRIVATE);
        findSdcardMP3ToArrayList();

        findMP3ContentProviderToArrayList();

        for(int i =0; i<sdCardList.size();i++)
        {
            mp3ListSdcard.add(sdCardList.get(i). getTitle());
        }
        if(sdCardList.size() != 0)
        {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, mp3ListSdcard);
            listViewMP3.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            listViewMP3.setAdapter(adapter);
            listViewMP3.setItemChecked(0, true);
        }

        selectedMP3 = mp3ListSdcard.get(0);

        eventHandlerFunc();
    }



    private void findMP3ContentProviderToArrayList() {

        String[] data = {MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION
        };

        Cursor cursor = getApplicationContext().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, data,
                null, null, data[2]+ " ASC");

        if(cursor != null)
        {
            while (cursor.moveToNext())
            {
                String id = cursor.getString(cursor.getColumnIndex(data[0]));
                String artist = cursor.getString(cursor.getColumnIndex(data[1]));
                String title = cursor.getString(cursor.getColumnIndex(data[2]));
                String albumArt = cursor.getString(cursor.getColumnIndex(data[3]));
                String duration = cursor.getString(cursor.getColumnIndex(data[4]));

                MusicData musicData = new MusicData(id, artist, title, albumArt, duration);
                sdCardList.add(musicData);
            }//while end
        }//if end
    }

    private void eventHandlerFunc() {
        listViewMP3.setOnItemClickListener((AdapterView<?> adapterView, View view, int position, long l) -> {
            selectedMP3 = mp3ListSdcard.get(position);
            selectPosition = position;

        });

        btnPlay.setOnClickListener(v -> {
            mPlayer = new MediaPlayer();
            try
            {
                MusicData musicData = sdCardList.get(selectPosition);
                Uri musicURI = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, musicData.getId());
                mPlayer.setDataSource(MainActivity.this,musicURI);
                mPlayer.prepare();
                mPlayer.start();

                btnPlay.setEnabled(false);
                btnStop.setEnabled(true);
                tvMP3.setText(selectedMP3);

                //seekBar도 같이 움직여준다.
                Thread thread = new Thread(){
                    @Override
                    public void run() {
                        if(mPlayer == null) return;

                            runOnUiThread(() -> {
                                    sbMP3.setMax(mPlayer.getDuration()); // 프로그래스바 전체시간을 가져와 세팅한다.
                            });
                            while(mPlayer.isPlaying())
                            {
                                runOnUiThread(() -> {
                                        sbMP3.setProgress(mPlayer.getCurrentPosition());
                                        tvTime.setText("재생시간 : "+sdf.format(mPlayer.getCurrentPosition()));
                                });
                                SystemClock.sleep(50);
                            }//while
                        }
                };
                thread.start();
            }
            catch (IOException e)
            {
                Log.d("MainActivity", "음악파일에 접근 실패");
            }
        });

        btnStop.setOnClickListener(v ->{
            mPlayer.stop();
            mPlayer.reset();

            btnPlay.setEnabled(true);
            btnStop.setEnabled(false);
            tvMP3.setText("Empty");
        });

        btnStop.setEnabled(false);

        btnPause.setOnClickListener(v -> {
            if(flag == true)
            {
                flag = false;
                mPlayer.pause();
                btnPause.setText("Keep Play");
            }
            else
            {
                flag = true;
                mPlayer.start();
                btnPause.setText("Pause");
            }
        });

        sbMP3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean myTouchStart) {
                if(myTouchStart == true)
                {
                    mPlayer.seekTo(progress);   //중요
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    private void findSdcardMP3ToArrayList() {
        File[] listFiles = new File(MP3PATH).listFiles();

        for(File f:listFiles)
        {
            if(f.getName().substring(f.getName().length() -3).equals("mp3"))
            {
                mp3List.add(f.getName());
            }
        }
    }

    public void findViewByIdFunc() {
        listViewMP3 = findViewById(R.id.listViewMP3);
        btnPlay = findViewById(R.id.btnPlay);
        btnStop = findViewById(R.id.btnStop);
        tvMP3 = findViewById(R.id.tvMP3);
        btnPause = findViewById(R.id.btnPause);
        sbMP3 = findViewById(R.id.sbMP3);
        tvTime = findViewById(R.id.tvTime);
    }
}