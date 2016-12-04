package com.kaltura.playkit.player;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.kaltura.playkit.Assert;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.ads.PKAdInfo;

/**
 * Created by anton.afanasiev on 01/11/2016.
 */

public class PlayerController implements Player {

    private static final PKLog log = PKLog.get("PlayerController");


    private PlayerEngine player;
    private Context context;

    private PlayerConfig.Media mediaConfig;
    private boolean wasReleased = false;

    private PKEvent.Listener eventListener;

    public void setEventListener(PKEvent.Listener eventListener) {
        this.eventListener = eventListener;
    }

    public interface EventListener {
        void onEvent(PlayerEvent.Type event);
    }

    public interface StateChangedListener {
        void onStateChanged(PlayerState oldState, PlayerState newState);
    }

    private EventListener eventTrigger = new EventListener() {

        @Override
        public void onEvent(PlayerEvent.Type eventType) {
            if (eventListener != null) {
                
                PlayerEvent event;
                
                // TODO: use specific event class
                switch (eventType) {
                    case DURATION_CHANGE:
                        event = new PlayerEvent.DurationChanged(getDuration());
                        break;
                    default:
                        event = new PlayerEvent.Generic(eventType);
                }
                
                eventListener.onEvent(event);
            }
        }
    };

    private StateChangedListener stateChangedTrigger = new StateChangedListener() {
        @Override
        public void onStateChanged(PlayerState oldState, PlayerState newState) {
            if (eventListener != null) {
                eventListener.onEvent(new PlayerEvent.StateChanged(newState, oldState));
            }
        }
    };

    public PlayerController(Context context, PlayerConfig.Media mediaConfig){
        this.context = context;
        this.mediaConfig = mediaConfig;
        player = new ExoPlayerWrapper(context);
        togglePlayerListeners(true);
    }

    public void prepare(@NonNull PlayerConfig.Media mediaConfig) {

        PKMediaSource source = SourceSelector.selectSource(mediaConfig.getMediaEntry());

        Uri sourceUri = Uri.parse(source.getUrl());
        PKDrmParams drmData = source.getDrmData();
        String licenseUri = null;
        if (drmData != null) {
            licenseUri = drmData.getLicenseUri();
        }
        
        player.load(sourceUri, licenseUri);
        startPlaybackFrom(mediaConfig.getStartPosition());
    }

    @Override
    public void destroy() {
        log.e("destroy");
        player.destroy();
        togglePlayerListeners(false);
        player = null;
        mediaConfig = null;
        eventListener = null;
    }

    private void startPlaybackFrom(long startPosition) {
        if(!wasReleased){
            togglePlayerListeners(false);
            player.startFrom(startPosition);
            togglePlayerListeners(true);
        }
    }

    public View getView() {
        //return view of the current player.
        return player.getView();
    }

    public long getDuration() {
//        Log.d(TAG, "getDuration " + player.getDuration());

        return player.getDuration();
    }

    public long getCurrentPosition() {
//        Log.d(TAG, "getPosition " + player.getCurrentPosition());
        return player.getCurrentPosition();
    }

    public long getBufferedPosition() {
        return player.getBufferedPosition();
    }

    public void seekTo(long position) {
        log.d("seek to " + position);
        player.seekTo(position);
    }

    public void play() {
        log.d("play");
        player.play();
    }

    public void pause() {
        log.d("pause");
        player.pause();
    }

    private void togglePlayerListeners(boolean enable) {
        if(enable){
            player.setEventListener(eventTrigger);
            player.setStateChangedListener(stateChangedTrigger);
        }else {
            player.setEventListener(null);
            player.setStateChangedListener(null);
        }
    }

    @Override
    public void prepareNext(@NonNull PlayerConfig.Media mediaConfig) {
        Assert.failState("Not implemented");
    }

    @Override
    public void skip() {
        Assert.failState("Not implemented");
    }

    @Override
    public void addEventListener(@NonNull PKEvent.Listener listener, Enum... events) {
        Assert.shouldNeverHappen();
    }

    @Override
    public void addStateChangeListener(@NonNull PKEvent.Listener listener) {
        Assert.shouldNeverHappen();
    }

    @Override
    public PKAdInfo getAdInfo() {
        Assert.shouldNeverHappen();
        return null;
    }

    @Override
    public void updatePluginConfig(@NonNull String pluginName, @NonNull String key, @Nullable Object value) {
        Assert.shouldNeverHappen();
    }

    @Override
    public void onApplicationPaused() {
        log.d("onApplicationPaused");
        player.release();
        togglePlayerListeners(false);
        wasReleased = true;
    }

    @Override
    public void onApplicationResumed() {
        log.d("onApplicationResumed");
        if(wasReleased){
            player.restore();
            prepare(mediaConfig);
            togglePlayerListeners(true);
            wasReleased = false;
        }
    }

    @Override
    public boolean isAutoPlay() {
        return mediaConfig.isAutoPlay();
    }
}
