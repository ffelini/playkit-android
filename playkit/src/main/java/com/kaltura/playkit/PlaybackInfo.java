package com.kaltura.playkit;

/**
 * Data object that holds information about currently playing media.
 * Created by anton.afanasiev on 14/12/2016.
 */

public class PlaybackInfo {

    private String mediaUrl;
    private long videoBitrate;
    private long audioBitrate;
    private long videoThroughput;
    private long videoWidth;
    private long videoHeight;


    public PlaybackInfo(String mediaUrl, long videoBitrate, long audioBitrate, long videoThroughput, long videoWidth, long videoHeight) {
        this.mediaUrl = mediaUrl;
        this.videoBitrate = videoBitrate;
        this.audioBitrate = audioBitrate;
        this.videoThroughput = videoThroughput;
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
    }

    /**
     * @return - the current playing media url.
     */
    public String getMediaUrl() {
        return mediaUrl;
    }

    /**
     *
     * @return - the current playing video track bitrate.
     */
    public long getVideoBitrate() {
        return videoBitrate;
    }

    /**
     *
     * @return - the current playing audio track bitrate.
     */
    public long getAudioBitrate() {
        return audioBitrate;
    }

    /**
     *
     * @return - the current playing video throughput.
     */
    public long getVideoThroughput() {
        return videoThroughput;
    }

    /**
     *
     * @return - the current playing video width.
     */
    public long getVideoWidth() {
        return videoWidth;
    }

    /**
     *
     * @return - the current playing video height.
     */
    public long getVideoHeight() {
        return videoHeight;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("mediaUrl =").append(mediaUrl).append(System.getProperty("line.separator"));
        sb.append("videoBitrate =").append(videoBitrate).append(System.getProperty("line.separator"));
        sb.append("audioBitrate =").append(audioBitrate).append(System.getProperty("line.separator"));
        sb.append("videoThroughput =").append(videoThroughput).append(System.getProperty("line.separator"));
        sb.append("videoWidth =").append(videoWidth).append(System.getProperty("line.separator"));
        sb.append("videoHeight =").append(videoHeight).append(System.getProperty("line.separator"));
        return sb.toString();
    }
}
