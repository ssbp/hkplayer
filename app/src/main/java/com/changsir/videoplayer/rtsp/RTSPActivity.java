package com.changsir.videoplayer.rtsp;

import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.changsir.videoplayer.R;

public class RTSPActivity extends AppCompatActivity {

    private SurfaceView mSurfaceView;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rtsp);

        mSurfaceView = findViewById(R.id.surfaceView);
        button = findViewById(R.id.button);
        button.setOnClickListener(view -> {

        });

        initVideo();
    }

    private void initVideo() {
        String host = "rtsp://admin:dyhb12345@220.180.188.245:8084/Streaming/Channels/102";

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
