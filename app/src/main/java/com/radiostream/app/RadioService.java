package com.radiostream.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

public class RadioService extends Service {

    private static final String CHANNEL_ID  = "radiostream_channel";
    private static final int    NOTIF_ID    = 1;
    private static final String ACTION_PLAY  = "com.radiostream.ACTION_PLAY";
    private static final String ACTION_PAUSE = "com.radiostream.ACTION_PAUSE";
    private static final String ACTION_STOP  = "com.radiostream.ACTION_STOP";

    private final IBinder binder = new LocalBinder();
    private MediaSessionCompat mediaSession;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private boolean isPlaying = true;

    public class LocalBinder extends Binder {
        RadioService getService() { return RadioService.this; }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        createNotificationChannel();
        setupMediaSession();
        requestAudioFocus();
        startForeground(NOTIF_ID, buildNotification());
    }

    private void setupMediaSession() {
        mediaSession = new MediaSessionCompat(this, "RadioStream");
        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override public void onPlay()  { isPlaying = true;  updatePlaybackState(); updateNotification(); }
            @Override public void onPause() { isPlaying = false; updatePlaybackState(); updateNotification(); }
            @Override public void onStop()  { stopSelf(); }
        });
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE,  "RadioStream")
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Live Radio")
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
                BitmapFactory.decodeResource(getResources(), R.drawable.ic_logo))
            .build());
        mediaSession.setActive(true);
        updatePlaybackState();
    }

    private void updatePlaybackState() {
        int state = isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
            .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
            .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP)
            .build());
    }

    private void requestAudioFocus() {
        AudioAttributes attrs = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build();
        audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener(focusChange -> {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                    isPlaying = false; updatePlaybackState(); updateNotification();
                } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                    isPlaying = true;  updatePlaybackState(); updateNotification();
                }
            }).build();
        audioManager.requestAudioFocus(audioFocusRequest);
    }

    private Notification buildNotification() {
        Intent openApp = new Intent(this, MainActivity.class);
        PendingIntent pendingOpen = PendingIntent.getActivity(this, 0, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String actionLabel = isPlaying ? "Pausa" : "Riproduci";
        int actionIcon = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
        Intent actionIntent = new Intent(this, RadioService.class)
            .setAction(isPlaying ? ACTION_PAUSE : ACTION_PLAY);
        PendingIntent actionPending = PendingIntent.getService(this, 0, actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stopIntent = new Intent(this, RadioService.class).setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RadioStream")
            .setContentText(isPlaying ? "In riproduzione..." : "In pausa")
            .setSmallIcon(R.drawable.ic_logo)
            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_logo))
            .setContentIntent(pendingOpen)
            .addAction(actionIcon, actionLabel, actionPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)
            .setStyle(new MediaStyle()
                .setMediaSession(mediaSession.getSessionToken())
                .setShowActionsInCompactView(0))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .build();
    }

    private void updateNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIF_ID, buildNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_PLAY:  isPlaying = true;  updatePlaybackState(); updateNotification(); break;
                case ACTION_PAUSE: isPlaying = false; updatePlaybackState(); updateNotification(); break;
                case ACTION_STOP:  stopForeground(true); stopSelf(); break;
            }
        }
        return START_STICKY;
    }

    private void createNotificationChannel() {
        NotificationChannel ch = new NotificationChannel(
            CHANNEL_ID, "RadioStream", NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("Controlli riproduzione radio");
        ch.setShowBadge(false);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(ch);
    }

    @Override
    public IBinder onBind(Intent intent) { return binder; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaSession != null) { mediaSession.setActive(false); mediaSession.release(); }
        if (audioFocusRequest != null) audioManager.abandonAudioFocusRequest(audioFocusRequest);
    }
}
