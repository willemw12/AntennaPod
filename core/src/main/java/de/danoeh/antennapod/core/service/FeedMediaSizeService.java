package de.danoeh.antennapod.core.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import de.danoeh.antennapod.core.event.FeedMediaEvent;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.NetworkUtils;
import de.greenrobot.event.EventBus;

public class FeedMediaSizeService extends IntentService {

    private final static String TAG = "FeedMediaSizeService";

    public FeedMediaSizeService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent()");
        if(false == NetworkUtils.isDownloadAllowed(this)) {
            return;
        }
        List<FeedMedia> list = DBReader.getFeedMediaUnknownSize(this);
        for (FeedMedia media : list) {
            Log.d(TAG, "Getting size currently " + media.getSize() + " for " + media.getDownload_url());
            if(false == NetworkUtils.isDownloadAllowed(this)) {
                return;
            }
            long size = Integer.MIN_VALUE;
            if (media.isDownloaded()) {
                File mediaFile = new File(media.getLocalMediaUrl());
                if(mediaFile.exists()) {
                    size = mediaFile.length();
                }
            } else if (false == media.checkedOnSizeButUnknown()) {
                // only query the network if we haven't already checked
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(media.getDownload_url());
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("Accept-Encoding", "");
                    conn.setRequestMethod("HEAD");
                    size = conn.getContentLength();
                } catch (IOException e) {
                    Log.d(TAG, media.getDownload_url());
                    e.printStackTrace();
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
            if (size <= 0) {
                // they didn't tell us the size, but we don't want to keep querying on it
                media.setCheckedOnSizeButUnknown();
            } else {
                media.setSize(size);
            }
            Log.d(TAG, "Size now: " + media.getSize());
            DBWriter.setFeedMedia(this, media);
            EventBus.getDefault().post(FeedMediaEvent.update(media));
        }
    }

}
