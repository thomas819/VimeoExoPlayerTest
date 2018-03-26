package com.example.user.vimeotest;

import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.media.MediaPlayer.TrackInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Selection;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.PlayerView;
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
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.jzvd.JZVideoPlayer;

public class MainActivity extends AppCompatActivity {
    String ACCESS_TOKEN = "cffbb78296f2476a83a410475a8673c7";
    String url = "https://api.vimeo.com/videos/226994817";
    @BindView(R.id.mainVideo) PlayerView mainVideo;

    DefaultTrackSelector trackSelector;
    //exoPlayer
    SimpleExoPlayer player;
    MappingTrackSelector.MappedTrackInfo mappedTrackinfo;
    TrackSelection.Factory videoSelectionFactory;
    AlertDialog.Builder builder;
    TrackGroupArray trackGroups;

    List<String> qualityList = new ArrayList<>();
    List<TrackGroup> trackGroupList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initVimeo();

        VimeoClient.getInstance().fetchNetworkContent(url, new ModelCallback<Video>(Video.class) {
            @Override
            public void success(Video video) {
                Play play = video.getPlay();
                //video.
                for (VideoFile file : video.files) {
                    if (file.getQuality() == VideoFile.VideoQuality.HLS) {
                        // mainVideo.setUp(file.getLink(), JZVideoPlayer.SCREEN_WINDOW_NORMAL,"야호");
                        //initExoPlayer(file.getLink());
                        playExoStream(file.getLink(), VideoFile.VideoQuality.HLS);

                    }
                }
            }

            @Override
            public void failure(VimeoError error) {
                Toast.makeText(MainActivity.this, "fail", Toast.LENGTH_SHORT).show();
                Log.e("error", error.toString());
            }

        });
        //vimeo();
    }

    private void initExoPlayer() {
        DefaultBandwidthMeter bandwidthMeter2 = new DefaultBandwidthMeter();
        videoSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter2);
        trackSelector = new DefaultTrackSelector(videoSelectionFactory);

        // trackSelector.setSelectionOverride(C.TRACK_TYPE_VIDEO);
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
        mainVideo.setPlayer(player);

        //DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        //MediaSource mediaSource = new HlsMediaSource(Uri.parse(url),mediaDataSourceFactory,null,null);
    }

    private void playExoStream(String url, VideoFile.VideoQuality type) {
        initExoPlayer();

        MediaSource mediaSource = null;
        DataSource.Factory DataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "test"), new DefaultBandwidthMeter());

        if (type == VideoFile.VideoQuality.HLS) {
            mediaSource = new HlsMediaSource.Factory(DataSourceFactory).createMediaSource(Uri.parse(url), null, null);
        }

        if (mediaSource != null) {
            player.prepare(mediaSource);
            //player.setPlayWhenReady(true);//시작시 자동재생
        }
    }

    private void initVimeo() {
        Configuration.Builder configBuilder = new Configuration.Builder(ACCESS_TOKEN);
        configBuilder.setCacheDirectory(this.getCacheDir());

        configBuilder.enableCertPinning(false);
        configBuilder.setLogLevel(Vimeo.LogLevel.VERBOSE);
        VimeoClient.initialize(configBuilder.build());
    }


    @OnClick(R.id.mainQualityBtn)
    public void onViewClicked(View view) {
        mappedTrackinfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackinfo != null) {

            for(int i=0;i<mappedTrackinfo.length;i++){
                trackGroups = mappedTrackinfo.getTrackGroups(i);

                int rendererType = player.getRendererType(i);

                if(trackGroups.length != 0 && rendererType ==C.TRACK_TYPE_VIDEO){

                    for(int j=0;j<trackGroups.length;j++){
                        TrackGroup trackGroup = trackGroups.get(i);
                        trackGroupList.add(trackGroup);
                        qualityList.clear();
                        for(int k=0;k<trackGroup.length;k++){
                            Format format = trackGroup.getFormat(k);
                            qualityList.add(videoFormatToString(format));
                        }
                    }



                }
            }
        } else {
            Log.e("trackSelecotor", "null........");
        }
        initListDialog();
    }

    private void initListDialog(){
        builder = new AlertDialog.Builder(this);
        builder.setTitle("Quality");
        builder.setItems(qualityList.toArray(new String[qualityList.size()]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, "count="+which, Toast.LENGTH_SHORT).show();
                //MappingTrackSelector.SelectionOverride override = new MappingTrackSelector.SelectionOverride(videoSelectionFactory,mappedTrackinfo.getTrackGroups(which).get(which),null);
                //trackSelector.setSelectionOverride(which,trackGroupList.get(which),);


                //TODO :: 화질변경을 마저 하자 selectionOverrides 고고
                Boolean isDisabled =trackSelector.getRendererDisabled(which);
                MappingTrackSelector.SelectionOverride override =trackSelector.getSelectionOverride(which,trackGroups);//TODO::여긴 왜 null이 뜰까 ...


                trackSelector.setRendererDisabled(which,isDisabled);
                if(override != null){
                    trackSelector.setSelectionOverride(which,trackGroups,override);
                }else{
                    trackSelector.clearSelectionOverrides(which);
                }

            }
        });
        builder.create().show();
    }


    private String videoFormatToString(Format format){
        String result;
        if (format.height < 0) {
            result = buildBitrateString(format);
        } else {
            result = format.height + "p";
        }
        return result;
    }

    private String buildBitrateString(Format format){
        return format.bitrate == Format.NO_VALUE ? "" : String.format(Locale.US, "%.2fMbit", format.bitrate / 1000000f);
    }


}
