package com.changsir.videoplayer.hikvision;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.changsir.videoplayer.R;
import com.dlong.rep.dlroundmenuview.DLRoundMenuView;
import com.dlong.rep.dlroundmenuview.Interface.OnMenuClickListener;
import com.hikvision.netsdk.NET_DVR_PTZCFG;

/**
 * 直播控制
 */
public class LiveFragment extends Fragment {

    private HikviPresenter hikviPresenter;
    private PlayerActivityInterface playerActivityInterface;
    private DLRoundMenuView menuView;

    public LiveFragment() {
    }

    public void setHikviPresenter(HikviPresenter hikviPresenter) {
        this.hikviPresenter = hikviPresenter;
    }

    public void setPlayerActivityInterface(PlayerActivityInterface playerActivityInterface) {
        this.playerActivityInterface = playerActivityInterface;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_live, container, false);
        menuView = v.findViewById(R.id.menuView);
        menuView.setOnMenuClickListener(onMenuClickListener);

        NET_DVR_PTZCFG n = hikviPresenter.fetchPTZControl();
        return v;
    }

    /**
     * TILT_UP 21 云台上仰
     * TILT_DOWN 22 云台下俯
     * PAN_LEFT 23 云台左转
     * PAN_RIGHT 24 云台右转
     */
    private static final int[] PTZ_CONTROL = {21, 24, 22, 23};

    /**
     * 菜单点击
     */
    private OnMenuClickListener onMenuClickListener = new OnMenuClickListener() {
        @Override
        public void OnMenuClick(int position) {
            //顺时针 0，1，2，3
            if(null != hikviPresenter && hikviPresenter.isAllowPTZ()) {
                hikviPresenter.ptzControl(PTZ_CONTROL[position]);
            } else {
                Toast.makeText(getContext(), "设备不支持云台操作", Toast.LENGTH_SHORT).show();
            }
        }
    };

}
