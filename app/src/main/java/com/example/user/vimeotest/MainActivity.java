package com.example.user.vimeotest;

import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.DebugTextViewHelper;
import com.google.android.exoplayer2.ui.DefaultTimeBar;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.TimeBar;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.vimeo.networking.Configuration;
import com.vimeo.networking.Vimeo;
import com.vimeo.networking.VimeoClient;
import com.vimeo.networking.callbacks.ModelCallback;
import com.vimeo.networking.model.Video;
import com.vimeo.networking.model.VideoFile;
import com.vimeo.networking.model.error.VimeoError;
import com.vimeo.networking.model.playback.Play;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.example.user.vimeotest.AdaptiveTrackSelection.DEFAULT_MAX_INITIAL_BITRATE;
import static com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import static com.google.android.exoplayer2.trackselection.MappingTrackSelector.SelectionOverride;

public class MainActivity extends AppCompatActivity {
    String url = "https://api.vimeo.com/videos/226994765";
    @BindString(R.string.token) String ACCESS_TOKEN;
    @BindView(R.id.mainVideo) PlayerView mainVideo;
    @BindView(R.id.mainNum) TextView mainNum;
    @BindView(R.id.exo_progress) DefaultTimeBar timeBar;
    @BindView(R.id.exo_controller_FullScreenBtn) ImageView fullScreenBtn;
    @BindView(R.id.exo_loading)ProgressBar exoLoading;
    @BindView(R.id.exo_pause) ImageButton exoPauseBtn;
    @BindView(R.id.exo_play) ImageButton exoPlayBtn;

    //exoPlayer
    SimpleExoPlayer player;
    DefaultTrackSelector trackSelector;
    MappedTrackInfo mappedTrackinfo;
    //AdaptiveTrackSelection.Factory videoSelectionFactory;
    AdaptiveTrackSelection.Factory videoSelectionFactory;
    AlertDialog.Builder builder;
    TrackGroupArray trackGroups;
    SelectionOverride override;

    DefaultBandwidthMeter bandwidthMeter;
    int rendererIndex = 0;

    List<String> qualityList = new ArrayList<>(); //360p,720p,...등
    int dialogNum = 0;
    boolean videoWatchCheck=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initVimeoToken();
        vimeoGetVideo();
        checkWifiChangeQuality();
        Toast.makeText(this, "onCreate", Toast.LENGTH_SHORT).show();
        initTimeBarEvent();
    }

    private void initTimeBarEvent(){
        timeBar.addListener(new TimeBar.OnScrubListener() {//timebar event
            @Override
            public void onScrubStart(TimeBar timeBar, long position) {
                if(videoWatchCheck){
                    timeBar.setEnabled(true);
                }else{
                    timeBar.setEnabled(false);
                    Toast.makeText(MainActivity.this, "처음 시청시 스킵할수 없습니다", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onScrubMove(TimeBar timeBar, long position) {
                if (videoWatchCheck){
                    timeBar.setEnabled(true);
                }else{
                    timeBar.setEnabled(false);
                    Toast.makeText(MainActivity.this, "처음 시청시 스킵할수 없습니다", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {

            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        Toast.makeText(this, "onResume", Toast.LENGTH_SHORT).show();
        if(player != null){
            player.setPlayWhenReady(true);//동영상 시작
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(player != null){
            player.setPlayWhenReady(false);//동영상 정지
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
    }

    //플레이어 셋팅
    private void initExoPlayer() {
        //link : https://github.com/google/ExoPlayer
        bandwidthMeter = new DefaultBandwidthMeter();//네트워크 용량 측정을 위한 것
        videoSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        trackSelector = new DefaultTrackSelector(videoSelectionFactory);
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
        mainVideo.setPlayer(player);
        //exoplayer debug
        DebugTextViewHelper debugTextViewHelper = new DebugTextViewHelper(player, mainNum);
        debugTextViewHelper.start();
        //event
        player.addListener(new Player.DefaultEventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
/*                switch (playbackState){
                    case Player.STATE_BUFFERING:
                        exoLoading.setVisibility(View.VISIBLE);
                        exoPauseBtn.setVisibility(View.INVISIBLE);
                        exoPlayBtn.setVisibility(View.INVISIBLE);
                        break;
                    case Player.STATE_READY:
                        exoLoading.setVisibility(View.INVISIBLE);
                        break;

                    case Player.STATE_ENDED:
                        Log.e("STATE_ENDED@@","@@");
                        videoWatchCheck=true;
                        break;
                }*/
                if(playbackState == Player.STATE_ENDED){
                    //동영상 시청완료
                    videoWatchCheck=true;

                }else if(playbackState ==Player.STATE_BUFFERING){
                    exoLoading.setVisibility(View.VISIBLE);
                    exoPauseBtn.setVisibility(View.INVISIBLE);
                    exoPlayBtn.setVisibility(View.INVISIBLE);
                }else{
                    exoLoading.setVisibility(View.INVISIBLE);
                }

            }

        });

    }

    //스트리밍 셋팅
    private void playExoStream(@NonNull String url, int type) {
        initExoPlayer();
        DataSource.Factory DataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "test"), bandwidthMeter);
        DataSource.Factory mediaDataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "test"), bandwidthMeter);
        MediaSource mediaSource = null;
        switch (type) {
            case C.TYPE_HLS:
                mediaSource = new HlsMediaSource.Factory(DataSourceFactory).createMediaSource(Uri.parse(url), null, null);
                break;
            case C.TYPE_DASH:
                mediaSource = new DashMediaSource.Factory(new DefaultDashChunkSource.Factory(DataSourceFactory), mediaDataSourceFactory).createMediaSource(Uri.parse(url), null, null);
                break;
        }
        if (mediaSource != null) {
            player.prepare(mediaSource);
            //player.setPlayWhenReady(true);//시작시 자동재생
        }
    }


    private void initVimeoToken() {
        //vimeo 초기셋팅
        //link : https://github.com/vimeo/vimeo-networking-java
        //link : https://developer.vimeo.com/apps
        Configuration.Builder configBuilder = new Configuration.Builder(ACCESS_TOKEN);
        configBuilder.setCacheDirectory(this.getCacheDir());

        configBuilder.enableCertPinning(false);
        configBuilder.setLogLevel(Vimeo.LogLevel.VERBOSE);
        VimeoClient.initialize(configBuilder.build());
    }

    private void vimeoGetVideo() {
        VimeoClient.getInstance().fetchNetworkContent(url, new ModelCallback<Video>(Video.class) {
            @Override
            public void success(Video video) {
                Play play = video.getPlay();
                //video.
                for (VideoFile file : video.files) {
                    int type = Util.inferContentType(Uri.parse(file.getLink()));
                    switch (type) {
                        case C.TYPE_HLS:
                            playExoStream(file.getLink(), type);
                            break;
                        case C.TYPE_DASH:
                            playExoStream(file.getLink(), type);
                            break;
                    }
                }
            }

            @Override
            public void failure(VimeoError error) {
                Toast.makeText(MainActivity.this, "fail", Toast.LENGTH_SHORT).show();
                Log.e("error", error.toString());
            }

        });
    }

    private void initListDialog() {
        builder = new AlertDialog.Builder(this);
        builder.setTitle("Quality");
        builder.setSingleChoiceItems(qualityList.toArray(new String[qualityList.size()]), dialogNum, (dialog, which) -> {
            dialogNum = which;
            Log.e("DialogCount", "" + which);
            trackSelect(which);
        });
        builder.setPositiveButton("확인", (dialog, which) -> {
            overrideTrackSelection();
            player.setPlayWhenReady(true);
        });
        builder.setCancelable(false);
        builder.create().show();
    }

    private void trackSelect(int num) {
        //isDisabled =trackSelector.getRendererDisabled(rendererIndex);
        override = new SelectionOverride(videoSelectionFactory, 0, num);
    }

    private void overrideTrackSelection() {
        //trackSelector.setRendererDisabled(rendererIndex,isDisabled);
        if (override != null) {
            trackSelector.setSelectionOverride(rendererIndex, mappedTrackinfo.getTrackGroups(0), override);
        } else {
            trackSelector.clearSelectionOverrides(rendererIndex);
        }
    }

    private void checkWifiChangeQuality() {
        Boolean check = null;
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            switch (networkInfo.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                    Toast.makeText(this, "wifi다", Toast.LENGTH_SHORT).show();
                    check = true;
                    break;
                case ConnectivityManager.TYPE_MOBILE:
                    Toast.makeText(this, "모바일이다", Toast.LENGTH_SHORT).show();
                    check = false;
                    break;
            }
        }
        if (check) {
            DEFAULT_MAX_INITIAL_BITRATE = Integer.MAX_VALUE;
            dialogNum = 3;
        } else {
            DEFAULT_MAX_INITIAL_BITRATE = 800000;
            dialogNum = 0;
        }
    }


    @OnClick({R.id.exo_controller_QualityBtn, R.id.exo_controller_FullScreenBtn})
    public void onViewClicked(View v) {
        switch (v.getId()) {
            case R.id.exo_controller_QualityBtn:
                mappedTrackinfo = trackSelector.getCurrentMappedTrackInfo();
                if (mappedTrackinfo != null) {
                    for (int i = 0; i < mappedTrackinfo.length; i++) {
                        trackGroups = mappedTrackinfo.getTrackGroups(i);
                        Log.e("trackGroups", mappedTrackinfo.getTrackGroups(i).toString());
                        int rendererType = player.getRendererType(i);
                        if (trackGroups.length != 0 && rendererType == C.TRACK_TYPE_VIDEO) {
                            rendererIndex = i;
                            Log.e("rendererIndex", rendererIndex + "");
                            for (int j = 0; j < trackGroups.length; j++) {//보통 0이다
                                TrackGroup trackGroup = trackGroups.get(j);
                                Log.e("trackGroup", trackGroups.get(j).toString());
                                qualityList.clear();
                                for (int k = 0; k < trackGroup.length; k++) {
                                    Format format = trackGroup.getFormat(k);
                                    Log.e("format", format.toString());
                                    qualityList.add(trackGroup.getFormat(k).height + "p");
                                }
                            }
                        }
                    }
                } else {
                    Log.e("trackSelecotor", "null........");
                }
                initListDialog();
                player.setPlayWhenReady(false);
                break;

            case R.id.exo_controller_FullScreenBtn:
                if(player.getPlayWhenReady()){//풀레이중인지 확인
                    player.setPlayWhenReady(true);
                }
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    fullScreenBtn.setImageDrawable(ContextCompat.getDrawable(MainActivity.this,R.drawable.exo_controls_fullscreen_enter));
                } else if (orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    fullScreenBtn.setImageDrawable(ContextCompat.getDrawable(MainActivity.this,R.drawable.exo_controls_fullscreen_exit));
                }
                break;
        }

    }

    @Override
    public void onBackPressed() {
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else if (orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
            super.onBackPressed();
        }
    }

}
