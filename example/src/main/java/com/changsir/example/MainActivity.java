package com.changsir.example;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.changsir.videoplayer.PlayerActivity;
import com.changsir.videoplayer.hikvision.VideoModel;
import com.changsir.videoplayer.rtsp.RTSPActivity;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    boolean needRefresh = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (null != savedInstanceState) {
            needRefresh = savedInstanceState.getBoolean("need", true);
        }

        if (needRefresh) {
            methodRequiresTwoPermission();
            needRefresh = false;
        }

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("need", needRefresh);
    }

    /**
     * 登陆
     *
     * @param view
     */
    public void login(View view) {
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, PlayerActivity.class);
        VideoModel videoModel = new VideoModel();
        videoModel.setAddress("220.180.188.245");
        videoModel.setPasswd("dyhb12345");
        videoModel.setUsername("admin");
        videoModel.setPort(8083);
        videoModel.setChannel(33);
        intent.putExtra(PlayerActivity.PLAYER_DATA, videoModel);
        intent.putExtra(PlayerActivity.FORCE_FULL_SCREEN, false);
        startActivity(intent);
    }

    public void rtsp(View view) {
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, RTSPActivity.class);
        startActivity(intent);
    }

    /**
     * 权限1
     */
    @AfterPermissionGranted(101)
    private void methodRequiresTwoPermission() {
        String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (!EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(this, "需要使用文件读写权限",
                    101, perms);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
}
