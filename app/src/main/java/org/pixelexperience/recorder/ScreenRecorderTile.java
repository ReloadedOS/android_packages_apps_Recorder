package org.pixelexperience.recorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.pixelexperience.recorder.utils.Utils;

import java.lang.reflect.Method;

public class ScreenRecorderTile extends TileService {

    private final BroadcastReceiver mRecordingStateChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Utils.ACTION_RECORDING_STATE_CHANGED.equals(intent.getAction())) {
                updateTile();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onClick() {
        if (isLocked()){
            unlockAndRun(() -> clickEvent(true));
        }else{
            clickEvent(false);
        }
    }

    private void collapseStatusBar(boolean delayed) {
        new Handler().postDelayed(() -> {
            try {
                Object sbservice = getSystemService("statusbar");
                Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
                Method collapse2 = statusbarManager.getMethod("collapsePanels");
                collapse2.setAccessible(true);
                collapse2.invoke(sbservice);
            } catch (Exception ignored) {
            }
        }, delayed ? 500 : 0);
    }

    private void clickEvent(final boolean locked){
        if (Utils.isScreenRecording()) {
            collapseStatusBar(locked);
            new Handler().postDelayed(() -> {
                Utils.setStatus(Utils.PREF_RECORDING_NOTHING, this);
                startService(new Intent(ScreenRecorderService.ACTION_STOP_SCREENCAST)
                        .setClass(this, ScreenRecorderService.class));
            }, locked ? 1000 : 500);
        } else {
            collapseStatusBar(locked);
            Intent intent = new Intent(this, StartScreenRecorder.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(intent);
        }
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRecordingStateChanged,
                new IntentFilter(Utils.ACTION_RECORDING_STATE_CHANGED));
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRecordingStateChanged);
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateTile();
    }

    private void updateTile() {
        Tile qsTile = getQsTile();
        qsTile.setState(Utils.isScreenRecording()
                ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        qsTile.setLabel(getString(Utils.isScreenRecording() ?
                R.string.touch_to_stop_message : R.string.main_screen_action));
        qsTile.updateTile();
    }
}
