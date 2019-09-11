package com.changsir.example;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.changsir.videoplayer.PlayerActivity;
import com.changsir.videoplayer.hikvision.VideoModel;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        methodRequiresTwoPermission();

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
        videoModel.setAddress("free.idcfengye.com");
        videoModel.setPasswd("dyhb12345");
        videoModel.setUsername("admin");
        videoModel.setPort(10433);
        videoModel.setChannel(33);
        intent.putExtra(PlayerActivity.PLAYER_DATA, videoModel);
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
