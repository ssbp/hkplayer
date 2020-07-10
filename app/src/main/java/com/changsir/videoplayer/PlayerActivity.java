package com.changsir.videoplayer;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentTransaction;

import com.changsir.videoplayer.hikvision.HikviPresenter;
import com.changsir.videoplayer.hikvision.HistoryListFragment;
import com.changsir.videoplayer.hikvision.LiveFragment;
import com.changsir.videoplayer.hikvision.PlayerActivityInterface;
import com.changsir.videoplayer.hikvision.VideoModel;
import com.changsir.videoplayer.hikvision.utils.OnHikviOptListener;

import java.util.Calendar;

/**
 * 播放界面
 */
public class PlayerActivity extends AppCompatActivity implements PlayerActivityInterface, SurfaceHolder.Callback {

    private static final String TAG = "PlayerActivity";
    public static final String PLAYER_DATA = "PLAYER_DATA";
    public static final String FORCE_FULL_SCREEN = "FORCE_FULL_SCREEN";
    public static final int OPT_SHOW = 1;

    private SurfaceView mSurfaceView;
    private VideoModel videoModel;
    private ProgressBar progressBar;
    private TextView progressText, yearTextView;
    private ConstraintLayout playerOptLayout;
    private ConstraintLayout titleOptLayout;
    private ToggleButton playbackBtn;


    private AutoHideOptHandler hideOptHandler;
    private boolean isOptShow = true;

    //全屏切换
    private int mVideoHeight = 0;
    private boolean fullScreen = false;
    private boolean forceFullScreen = true;
    private boolean hasTitleBar = true;
    private RelativeLayout videoLayout;

    private ImageView fullScreenBtn;
    private ImageView backButton;

    private HistoryListFragment historyListFragment;
    private LiveFragment liveFragment;

    private HikviPresenter hikviPresenter;

    //缩放手势
    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetector mGestureDetecotr;

    private float optHeight = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        if(null != getSupportActionBar())
            hasTitleBar = getSupportActionBar().isShowing();

        playbackBtn = findViewById(R.id.playbackBtn);
        playbackBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    //回看
                    getPlayBack();
                } else {
                    //直播
                    loadPreview();
                    getLiveFragment();
                }
            }
        });
        yearTextView = findViewById(R.id.yearTextView);
        progressBar = findViewById(R.id.hkPlayerProgressBar);
        progressText = findViewById(R.id.hkPlayerProgressText);
        playerOptLayout = findViewById(R.id.playerOptLayout);
        titleOptLayout = findViewById(R.id.titleOptLayout);

        //全屏按钮
        videoLayout = findViewById(R.id.videoLayout);
        fullScreenBtn = findViewById(R.id.fullScreenBtn);
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(view -> finish());
        fullScreenBtn.setOnClickListener(view -> switchFullScreen());

        //保持屏幕常量
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mSurfaceView = findViewById(R.id.surfaceView);
        mSurfaceView.getHolder().addCallback(this);

        hideOptHandler = new AutoHideOptHandler();
        initGestureDetector();
        optHeight = dip2px(this, 30);
        initData();
        initFragment();
    }

    private void initFragment() {
        historyListFragment = new HistoryListFragment();
        historyListFragment.setHikviPresenter(hikviPresenter);
        historyListFragment.setPlayerActivityInterface(this);

        liveFragment = new LiveFragment();
        liveFragment.setHikviPresenter(hikviPresenter);
        liveFragment.setPlayerActivityInterface(this);

        getLiveFragment();
    }

    private void initData() {
        videoModel = getIntent().getParcelableExtra(PLAYER_DATA);
        forceFullScreen = getIntent().getBooleanExtra(FORCE_FULL_SCREEN, true);
        if (null == videoModel) {
            Toast.makeText(this, "播放数据获取失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fullScreenBtn.setEnabled(!forceFullScreen);
        if (forceFullScreen) {
            switchFullScreen();
        } else {
            playerOptLayout.setVisibility(View.VISIBLE);
        }

        //逻辑处理事件
        hikviPresenter = new HikviPresenter(videoModel);
        hikviPresenter.setPlayerActivityInterface(this);

        //设置默认日期
        Calendar calendar = Calendar.getInstance();
        updateDate(calendar.get(Calendar.YEAR)+"年"+(calendar.get(Calendar.MONTH)+1)+"月");
    }

    private void loadHK() {
        progressBar.setVisibility(View.VISIBLE);
        progressText.setVisibility(View.VISIBLE);
        progressText.setText("正在加载");
        hikviPresenter.startLogin(onHikviLoginListener);
    }

    private void loadPreview() {
        hikviPresenter.stopPlayback();
        hikviPresenter.stopPreview();
        hikviPresenter.startPreview(mSurfaceView, onPreviewListener);
        showOptArea(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.e(TAG, "创建视频窗口");
        mSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        hikviPresenter.surfaceChange(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e(TAG, "停止预览");
        hikviPresenter.surfaceDestory(holder);
    }

    @Override
    protected void onDestroy() {
        hikviPresenter.destory();
        Log.e(TAG, "销毁资源");
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        hikviPresenter.stopPreview();
        hikviPresenter.stopPlayback();
        super.onPause();
    }

    @Override
    protected void onResume() {
        //播放
        loadHK();
        super.onResume();
    }

    /**
     * 获取回放列表
     */
    private void getPlayBack() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragmentLayout, historyListFragment);
        fragmentTransaction.commit();
    }

    /**
     * 获取操作界面
     */
    private void getLiveFragment() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragmentLayout, liveFragment);
        fragmentTransaction.commit();
    }

    /**
     * 切换全屏
     */
    private void switchFullScreen() {
        if (!fullScreen) {
            //切换到全屏模式
            if(null != getSupportActionBar())
                getSupportActionBar().hide();
            //添加一个全屏的标记
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            //请求横屏
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

            //设置视频播放控件的布局的高度是match_parent
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) videoLayout.getLayoutParams();
            //将默认的高度缓存下来
            mVideoHeight = layoutParams.height;
            layoutParams.height = RelativeLayout.LayoutParams.MATCH_PARENT;
            videoLayout.setLayoutParams(layoutParams);
            fullScreen = true;
            titleOptLayout.setVisibility(View.VISIBLE);
        } else {
            //切换到默认模式
            //清除全屏标记
            if (hasTitleBar && null != getSupportActionBar()) {
                getSupportActionBar().show();
            }

            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            //请求纵屏
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            //设置视频播放控件的布局的高度是200
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) videoLayout.getLayoutParams();
            layoutParams.height = mVideoHeight;  //这里的单位是px
            videoLayout.setLayoutParams(layoutParams);

            fullScreen = false;
            titleOptLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void hideProgress() {
        progressBar.setVisibility(View.GONE);
        progressText.setVisibility(View.GONE);
    }

    @Override
    public void playCallBack(String name) {
        if (null != hikviPresenter) {
            showOptArea(true);
            hikviPresenter.startPlayback(name, mSurfaceView);
        }
    }

    @Override
    public void updateDate(String date) {
        yearTextView.setText(date);
    }

    @Override
    public void updateProgress(String content) {
        progressText.setText(content);
    }

    /**
     * 登录监听
     */
    private OnHikviOptListener onHikviLoginListener = new OnHikviOptListener() {
        @Override
        public void failed(String content) {
            Toast.makeText(getApplicationContext(), content, Toast.LENGTH_SHORT).show();
            setResult(Activity.RESULT_OK);
            PlayerActivity.this.finish();
        }

        @Override
        public void finish(String content) {
            hikviPresenter.startPreview(mSurfaceView, onPreviewListener);
            showOptArea(true);
        }

        @Override
        public void process(String content) {
            progressText.setText(content);
        }
    };

    /**
     * 预览事件
     */
    private OnHikviOptListener onPreviewListener = new OnHikviOptListener() {
        @Override
        public void failed(String content) {

        }

        @Override
        public void finish(String content) {
            hideProgress();
        }

        @Override
        public void process(String content) {

        }
    };

    class AutoHideOptHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == OPT_SHOW) {
                showOptArea(false);
            }
        }
    }

    /**
     * 隐藏还是显示操作区域
     *
     * @param show
     */
    private void showOptArea(boolean show) {

        float toY = show ? 0 : optHeight;

        if (show) {
            hideOptHandler.removeMessages(OPT_SHOW);
            Message msg = hideOptHandler.obtainMessage(OPT_SHOW);
            hideOptHandler.sendMessageDelayed(msg, 5000);
        }

        //没有强制全屏时，才会显示全屏和非全屏切换
        if (!forceFullScreen) {
            playerOptLayout.animate()
                    .setDuration(200)
                    .translationY(toY)
                    .setInterpolator(new AccelerateInterpolator())
                    .start();
        }

        isOptShow = show;

        showTitleArea(show);
    }

    /**
     * 显示标题
     *
     * @param show
     */
    private void showTitleArea(boolean show) {
        float toY = show ? 0 : -optHeight;
        if (show) {
            hideOptHandler.removeMessages(OPT_SHOW);
            Message msg = hideOptHandler.obtainMessage(OPT_SHOW);
            hideOptHandler.sendMessageDelayed(msg, 5000);
        }

        titleOptLayout.animate()
                .setDuration(200)
                .translationY(toY)
                .setInterpolator(new AccelerateInterpolator())
                .start();
    }

    /**
     * 初始化手势
     */
    private void initGestureDetector() {
//        mScaleGestureDetector = new ScaleGestureDetector(getBaseContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
//            @Override
//            public boolean onScaleBegin(ScaleGestureDetector detector) {
//                return true;
//            }
//
//            @Override
//            public boolean onScale(ScaleGestureDetector detector) {
//
//                if(allwoOpt) {
//                    float s = detector.getScaleFactor();
//                    if (s > 1.0f) {
//                        optCamera("ZOOM_IN");
//                    }
//                    if (s < 1.0f) {
//                        optCamera("ZOOM_OUT");
//                    }
//                }
//
//                return true;
//            }
//
//            @Override
//            public void onScaleEnd(ScaleGestureDetector detector) {
//            }
//        });

        mGestureDetecotr = new GestureDetector(getBaseContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
//                String cmd;
//                if(allwoOpt) {
//                    if (Math.abs(distanceX) > Math.abs(distanceY)) {
//                        //选择x方向
//                        cmd = distanceX > 0 ? "RIGHT" : "LEFT";
//                    } else {
//                        //选择y方向
//                        cmd = distanceY > 0 ? "DOWN" : "UP";
//                    }
//
//                    if (null != cmd) {
//                        optCamera(cmd);
//                    }
//                }

                return super.onScroll(e1, e2, distanceX, distanceY);
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                //单击，操作区域是否显示
                showOptArea(!isOptShow);
                return super.onSingleTapUp(e);
            }

        });

        //绑定播放器事件
        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return mGestureDetecotr.onTouchEvent(motionEvent);
            }
        });

    }

    public static int dip2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

}
