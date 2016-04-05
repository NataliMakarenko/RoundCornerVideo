package com.example.nataliamakarenko.roundcornervideo;

/**
 * Created by natalia.makarenko on 4/5/2016.
 */
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaCodec;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.MediaController;
import android.widget.ProgressBar;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.util.PlayerControl;
import com.google.android.exoplayer.util.Util;


/**
 * Created by JoMedia_1 on 01.03.2016.
 */
public class VideoPlayerActivity extends AppCompatActivity {

    private static final String TAG = VideoPlayerActivity.class.getName();

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 256;

    private ExoPlayer mExoPlayer;
    private MediaController mMediaController;
    private ProgressBar mProgressBar;
    private SurfaceView mSurfaceView;
    private long position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        String videoUrl = "http://r2---sn-2puapox-ig3e.c.youtube.com/videoplayback?id=604ed5ce52eda7ee&itag=22&source=youtube&sparams=expire,id,ip,ipbits,mm,mn,ms,mv,pcm2cms,pl,source&ip=195.234.74.155&ipbits=0&expire=19000000000&signature=4DB2BB681D0F07C9425BA624E82149EA910830E8.350072DB9EF8A5A9BE768C553A4B25235C5B5743&key=cms1&cms_redirect=yes&mm=31&mn=sn-2puapox-ig3e&ms=au&mt=1459847072&mv=m&pcm2cms=yes&pl=24";//getIntent().getStringExtra(EXTRA_VIDEO_URL);
        View root = findViewById(R.id.root);

        mMediaController = new KeyCompatibleMediaController(this);
        mMediaController.setAnchorView(root);

        root.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    toggleControlsVisibility();
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    view.performClick();
                }
                return true;
            }
        });

        mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
        mProgressBar = (ProgressBar) findViewById(R.id.pb_video_loading);

        mExoPlayer = ExoPlayer.Factory.newInstance(2);
        mExoPlayer.addListener(new ExoPlayer.Listener() {

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                switch (playbackState) {
                    case ExoPlayer.STATE_BUFFERING:
                        Log.d(TAG, "onPlayerStateChanged(STATE_BUFFERING)", null);
                        mProgressBar.setVisibility(View.VISIBLE);
                        break;
                    case ExoPlayer.STATE_ENDED:
                        Log.d(TAG, ".onPlayerStateChanged(STATE_ENDED)", null);
                        break;
                    case ExoPlayer.STATE_IDLE:
                        Log.d(TAG, ".onPlayerStateChanged(STATE_IDLE)", null);
                        break;
                    case ExoPlayer.STATE_PREPARING:
                        Log.d( TAG, ".onPlayerStateChanged(STATE_PREPARING)", null);
                        break;
                    case ExoPlayer.STATE_READY:
                        Log.d(TAG, ".onPlayerStateChanged(STATE_READY)", null);
                        mProgressBar.setVisibility(View.GONE);
                        break;
                }
            }

            @Override
            public void onPlayWhenReadyCommitted() {
                Log.d(TAG, ".onPlayWhenReadyCommitted():", null);
            }

            @Override
            public void onPlayerError(ExoPlaybackException e) {
                Log.e(TAG, "ERROR in onPlayerError():" + e.getMessage(), e);
                mProgressBar.setVisibility(View.GONE);
                AlertDialog dialog = new AlertDialog.Builder(VideoPlayerActivity.this).create();
                dialog.setMessage(e.getMessage());
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        dialog.dismiss();
                        finish();
                    }
                });
            }
        });
        mMediaController.setMediaPlayer(new PlayerControl(mExoPlayer));
        mMediaController.setEnabled(true);

        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        String userAgent = Util.getUserAgent(this, TAG);
        DataSource dataSource = new DefaultUriDataSource(this, bandwidthMeter, userAgent);

        Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);
        ExtractorSampleSource sampleSource = new ExtractorSampleSource(Uri.parse(videoUrl), dataSource, allocator,
                BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE);
        MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(this,
                sampleSource, MediaCodecSelector.DEFAULT, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource,
                MediaCodecSelector.DEFAULT);

        mExoPlayer.prepare(videoRenderer, audioRenderer);
        mExoPlayer.sendMessage(videoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, mSurfaceView.getHolder().getSurface());
    }

    @Override
    public void onResume() {
        super.onResume();
        mExoPlayer.seekTo(position);
        mExoPlayer.setPlayWhenReady(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mExoPlayer.getPlayWhenReady()) {
            position = mExoPlayer.getCurrentPosition();
            mExoPlayer.seekTo(0);
            mExoPlayer.stop();
            mExoPlayer.release();
        }
    }

    private void toggleControlsVisibility() {
        if (mMediaController.isShowing()) {
            mMediaController.hide();
        } else {
            showControls();
        }
    }

    private void showControls() {
        mMediaController.show(0);
    }


    private static final class KeyCompatibleMediaController extends MediaController {

        private MediaController.MediaPlayerControl playerControl;

        public KeyCompatibleMediaController(Context context) {
            super(context);
        }

        @Override
        public void setMediaPlayer(MediaController.MediaPlayerControl playerControl) {
            super.setMediaPlayer(playerControl);
            this.playerControl = playerControl;
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            int keyCode = event.getKeyCode();
            if (playerControl.canSeekForward() && keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    playerControl.seekTo(playerControl.getCurrentPosition() + 15000); // milliseconds
                    show();
                }
                return true;
            } else if (playerControl.canSeekBackward() && keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    playerControl.seekTo(playerControl.getCurrentPosition() - 5000); // milliseconds
                    show();
                }
                return true;
            }
            return super.dispatchKeyEvent(event);
        }
    }

}