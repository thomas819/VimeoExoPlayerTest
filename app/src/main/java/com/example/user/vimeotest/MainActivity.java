package com.example.user.vimeotest;

import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Selection;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
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
import com.google.android.exoplayer2.trackselection.FixedTrackSelection;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.DebugTextViewHelper;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.vimeo.networking.Configuration;
import com.vimeo.networking.Vimeo;
import com.vimeo.networking.VimeoClient;
import com.vimeo.networking.callbacks.ModelCallback;
import com.vimeo.networking.model.Video;
import com.vimeo.networking.model.VideoFile;
import com.vimeo.networking.model.error.VimeoError;
import com.vimeo.networking.model.playback.Play;

import java.security.AlgorithmConstraints;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import static com.google.android.exoplayer2.trackselection.MappingTrackSelector.SelectionOverride;

public class MainActivity extends AppCompatActivity {
    String ACCESS_TOKEN = "cffbb78296f2476a83a410475a8673c7";
    String url = "https://api.vimeo.com/videos/226994817";
    @BindView(R.id.mainVideo) PlayerView mainVideo;
    @BindView(R.id.mainNum) TextView mainNum;

    DefaultTrackSelector trackSelector;
    //exoPlayer
    SimpleExoPlayer player;
    MappedTrackInfo mappedTrackinfo;
    AdaptiveTrackSelection.Factory videoSelectionFactory;
    AlertDialog.Builder builder;
    TrackGroupArray trackGroups;
    SelectionOverride override;

    Boolean isDisabled;

    int rendererIndex = 0;
    List<String> qualityList = new ArrayList<>(); //360p,720p,...등

    List<Format> formatList = new ArrayList<>();

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
                        // HLS 이면
                        //file.getLink() 링크를 가져오쟈
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
    //플레이어 셋팅
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
    //스트리밍 셋팅
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

            for (int i = 0; i < mappedTrackinfo.length; i++) {
                trackGroups = mappedTrackinfo.getTrackGroups(i);
                Log.e("trackGroups",mappedTrackinfo.getTrackGroups(i).toString());
                int rendererType = player.getRendererType(i);
                if (trackGroups.length != 0 && rendererType == C.TRACK_TYPE_VIDEO) {
                    rendererIndex = i;
                    Log.e("rendererIndex",rendererIndex+"");
                    for (int j = 0; j < trackGroups.length; j++) {//보통 0이다
                        TrackGroup trackGroup = trackGroups.get(j);
                        Log.e("trackGroup",trackGroups.get(j).toString());
                        qualityList.clear();
                        for (int k = 0; k < trackGroup.length; k++) {
                            Format format = trackGroup.getFormat(k);
                            Log.e("format",format.toString());
                            qualityList.add(trackGroup.getFormat(k).height+"p");
                            formatList.add(format);
                        }
                    }
                }
            }


        } else {
            Log.e("trackSelecotor", "null........");
        }

        DebugTextViewHelper debugTextViewHelper = new DebugTextViewHelper(player,mainNum);
        debugTextViewHelper.start();
        initListDialog();
    }

    private void initListDialog() {
        builder = new AlertDialog.Builder(this);
        builder.setTitle("Quality");
        builder.setSingleChoiceItems(qualityList.toArray(new String[qualityList.size()]), 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.e("DialogCount",""+which);
                    switch (which){
                        case 0:
                            trackSelect(0);
                            break;
                        case 1:
                            trackSelect(1);
                            break;
                        case 2:
                            trackSelect(2);
                            break;
                        case 3:
                            trackSelect(3);
                            break;
                    }

/*                int[] tracks = new int[trackGroups.length];
                for(int i=0;i<trackGroups.length;i++){
                    tracks[i]=i;
                }*/
                //Log.e("override-tracks",tracks.toString());
                //trackSelector.setSelectionOverride(0,trackGroups,new MappingTrackSelector.SelectionOverride(new FixedTrackSelection.Factory(),0,3));
                //TODO :: 화질변경을 마저 하자 selectionOverrides 고고
                /*Boolean isDisabled =trackSelector.getRendererDisabled(which);
                MappingTrackSelector.SelectionOverride override =trackSelector.getSelectionOverride(which,trackGroups);

                trackSelector.setRendererDisabled(which,isDisabled);
                if(override != null){
                    trackSelector.setSelectionOverride(which,trackGroups,override);
                }else{
                    trackSelector.clearSelectionOverrides(which);
                }*/

            }
        });
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                overrideTrackSelection();
            }
        });
        builder.setCancelable(false);
        builder.create().show();
    }

    private void trackSelect(int num){
        //isDisabled =trackSelector.getRendererDisabled(rendererIndex);
        override = new SelectionOverride(videoSelectionFactory, 0, num);
    }
    private void overrideTrackSelection(){
        //trackSelector.setRendererDisabled(rendererIndex,isDisabled);
        if (override != null) {
            //trackSelector.clearSelectionOverrides(rendererIndex);
            trackSelector.setSelectionOverride(rendererIndex, mappedTrackinfo.getTrackGroups(0), override);
            Log.e("override","살아있다");
        }else{
            trackSelector.clearSelectionOverrides(rendererIndex);
            Log.e("override","null 이다");
        }
    }

}
