package com.kaltura.playkit.plugins;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonObject;
import com.kaltura.netkit.connect.executor.APIOkRequestsExecutor;
import com.kaltura.netkit.connect.executor.RequestQueue;
import com.kaltura.netkit.connect.request.RequestBuilder;
import com.kaltura.netkit.connect.response.ResponseElement;
import com.kaltura.netkit.utils.OnRequestCompletion;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.PlaybackInfo;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.Utils;
import com.kaltura.playkit.api.ovp.services.LiveStatsService;
import com.kaltura.playkit.utils.Consts;

import java.util.Date;
import java.util.TimerTask;

/**
 * Created by zivilan on 02/11/2016.
 */

public class KalturaLiveStatsPlugin extends PKPlugin {
    private static final PKLog log = PKLog.get("KalturaLiveStatsPlugin");
    private static final String TAG = "KalturaLiveStatsPlugin";

    private final String DEFAULT_BASE_URL = "https://livestats.kaltura.com/api_v3/index.php";
    private String baseUrl;
    private int partnerId;
    private String entryId;
    private boolean isBuffering = false;

    public enum KLiveStatsEvent {
        LIVE(1),
        DVR(2);

        private final int value;

        KLiveStatsEvent(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private long lastReportedBitrate = -1;
    private Player player;
    private PKMediaConfig mediaConfig;
    private JsonObject pluginConfig;
    private MessageBus messageBus;
    private RequestQueue requestsExecutor;
    private java.util.Timer timer = new java.util.Timer();
    private int eventIdx = 0;
    private int currentBitrate = -1;
    private long bufferTime = 0;
    private long bufferStartTime = 0;
    private boolean isLive = false;
    private boolean isFirstPlay = true;

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "KalturaLiveStats";
        }

        @Override
        public PKPlugin newInstance() {
            return new KalturaLiveStatsPlugin();
        }

        @Override
        public void warmUp(Context context) {

        }
    };

    @Override
    protected void onLoad(Player player, Object config, final MessageBus messageBus, Context context) {
        messageBus.listen(mEventListener, PlayerEvent.Type.STATE_CHANGED, PlayerEvent.Type.PAUSE, PlayerEvent.Type.PLAY);
        this.requestsExecutor = APIOkRequestsExecutor.getSingleton();
        this.player = player;
        this.pluginConfig = (JsonObject) config;
        this.messageBus = messageBus;
    }

    @Override
    public void onDestroy() {
        stopLiveEvents();
        eventIdx = 0;
        timer.cancel();
    }

    @Override
    protected void onUpdateMedia(PKMediaConfig mediaConfig) {
        if (Utils.isJsonObjectValueValid(pluginConfig, "baseUrl")) {
            baseUrl = pluginConfig.getAsJsonPrimitive("baseUrl").getAsString();
        } else {
            baseUrl = DEFAULT_BASE_URL;
        }
        if (Utils.isJsonObjectValueValid(pluginConfig, "partnerId")) {
            partnerId = pluginConfig.getAsJsonPrimitive("partnerId").getAsInt();
        } else {
            partnerId = 0;
            log.e("Error KalturaStats partnetId is missing");
        }
        if (Utils.isJsonObjectValueValid(pluginConfig, "entryId")) {
            entryId = pluginConfig.getAsJsonPrimitive("entryId").getAsString();
        } else {
            // in case of OVP entry id is anyway the ID needed it only for OTT
            entryId = mediaConfig.getMediaEntry().getId();
            if (entryId != null && !entryId.contains("_")) {
                log.e("Error KalturaStats entryId was given as MEDIA_ID instead of entryId");
            }
        }
        eventIdx = 0;
        this.mediaConfig = mediaConfig;
    }

    @Override
    protected void onUpdateConfig(Object config) {
        this.pluginConfig = (JsonObject) config;
    }

    @Override
    protected void onApplicationPaused() {

    }

    @Override
    protected void onApplicationResumed() {

    }

    private PKEvent.Listener mEventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {
            if (event instanceof PlayerEvent) {
                switch (((PlayerEvent) event).type) {
                    case STATE_CHANGED:
                        KalturaLiveStatsPlugin.this.onEvent((PlayerEvent.StateChanged) event);
                        break;
                    case PLAY:
                        startLiveEvents();
                        break;
                    case PAUSE:
                        stopLiveEvents();
                        break;
                    case PLAYBACK_INFO_UPDATED:
                        PlaybackInfo currentPlaybackInfo = ((PlayerEvent.PlaybackInfoUpdated) event).getPlaybackInfo();
                        lastReportedBitrate = currentPlaybackInfo.getVideoBitrate();
                    default:
                        break;
                }
            }
        }
    };

    public void onEvent(PlayerEvent.StateChanged event) {
        switch (event.newState) {
            case READY:
                startTimerInterval();
                if (isBuffering) {
                    isBuffering = false;
                    sendLiveEvent(calculateBuffer(false));
                }
                break;
            case BUFFERING:
                isBuffering = true;
                bufferStartTime = new Date().getTime();
                break;
            default:
                break;
        }
    }

    private long calculateBuffer(boolean isBuffering) {
        long currTime = new Date().getTime();
        bufferTime = (currTime - bufferStartTime) / 1000;
        if (bufferTime > 10) {
            bufferTime = 10;
        }
        if (isBuffering) {
            bufferStartTime = new Date().getTime();
        } else {
            bufferStartTime = -1;
        }
        return bufferTime;
    }

    private void startTimerInterval() {
        int timerInterval = pluginConfig.has("timerInterval") ? pluginConfig.getAsJsonPrimitive("timerInterval").getAsInt() * (int)Consts.MILLISECONDS_MULTIPLIER : Consts.DEFAULT_ANALYTICS_TIMER_INTERVAL_LOW;

        if (timer == null) {
            timer = new java.util.Timer();
        }
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendLiveEvent(bufferTime);
            }
        }, 0, timerInterval);
    }


    private void startLiveEvents() {
        if (!isLive) {
            startTimerInterval();
            isLive = true;
            if (isFirstPlay) {
                sendLiveEvent(bufferTime);
                isFirstPlay = false;
            }

        }
    }

    private void stopLiveEvents() {
        isLive = false;
    }

    private void sendLiveEvent(final long bufferTime) {
        String sessionId = (player.getSessionId() != null) ? player.getSessionId() : "";

        // Parameters for the request -
        // String baseUrl, int partnerId, int eventType, int eventIndex, int bufferTime, int bitrate,
        // String startTime,  String entryId,  boolean isLive, String referrer
        RequestBuilder requestBuilder = LiveStatsService.sendLiveStatsEvent(baseUrl, partnerId, isLive ? 1 : 2, eventIdx++, bufferTime,
                lastReportedBitrate, sessionId, mediaConfig.getStartPosition(), entryId, isLive, PlayKitManager.CLIENT_TAG, "hls");

        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                Log.d(TAG, "onComplete: " + isLive);
                messageBus.post(new KalturaLiveStatsEvent.KalturaLiveStatsReport(bufferTime));
            }
        });
        requestsExecutor.queue(requestBuilder.build());
    }
}
