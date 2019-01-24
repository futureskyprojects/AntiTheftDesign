package com.blogspot.tndev1403.antitheft.View;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blogspot.tndev1403.antitheft.Modal.AntitheftService;
import com.blogspot.tndev1403.antitheft.Modal.Firebase;
import com.blogspot.tndev1403.antitheft.Presenter.HomePresenter;
import com.blogspot.tndev1403.antitheft.R;
import com.blogspot.tndev1403.antitheft.Stored.Config.Config;
import com.skyfishjy.library.RippleBackground;

import java.util.Timer;
import java.util.TimerTask;

import at.markushi.ui.CircleButton;

public class Home extends AppCompatActivity {

    // Define view
    public RippleBackground safeEffect;
    public RippleBackground warningEffect;
    public RippleBackground dangerEffect;
    public Toolbar toolbar;
    private LinearLayout topViewGroup;
    public TextView stateDescription;
    public CircleButton safeButton;
    public CircleButton dangerButton;
    public Button disConnectButton;
    public ImageView infoButton;
    // Define HomePresenter
    public HomePresenter homePresenter;
    public Intent antiTheftService;
    // Define Controll
    public MediaPlayer mp;
    public Vibrator vb;
    // Define static string
    public final static String SAFE_DESCRIPTION = "Vẫn đang kết nối đồng thời báo động chưa bị kích hoạt";
    public final static String WARNING_DESCRIPTION = "Đã mất kết nối, vui lòng kiểm tra lại kết " +
            "nối của thiết bị";
    public final static String DANGER_DESCRIPTION = "Báo động đã được kích hoạt, hãy chú ý đến " +
            "tài sản của bạn!";
    // Define static bolean
    public static boolean acitivyState = false;
    public boolean isAutoAlert = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initView();
        initEventAndControll();
        // init services
        initServices();
        // Atfer start activity, fist let disconnect state of house was shown, then check to
        // internet server and change later
        this.warning();
        TimerCheckInternet();
    }

    void initServices() {
        antiTheftService = new Intent(Home.this, AntitheftService.class);
        startService(antiTheftService);
    }

    void initView() {
        safeEffect = (RippleBackground) findViewById(R.id.safe_effect);
        warningEffect = (RippleBackground) findViewById(R.id.warning_effect);
        dangerEffect = (RippleBackground) findViewById(R.id.danger_effect);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        stateDescription = (TextView) findViewById(R.id.state_description);
        safeButton = (CircleButton) findViewById(R.id.btn_turn_alert_off);
        dangerButton = (CircleButton) findViewById(R.id.btn_turn_alert_on);
        disConnectButton = (Button) findViewById(R.id.btn_disconnect_server);
        topViewGroup = (LinearLayout) findViewById(R.id.top_view_group);
        infoButton = (ImageView) findViewById(R.id.btn_info);
        // Make padding for top view group
        topViewGroup.setPadding(0, getStatusBarHeight(), 0, 0);
        // Transparent status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }

    void initEventAndControll() {
        mp = new MediaPlayer();
        vb = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        homePresenter = new HomePresenter(Home.this);
        homePresenter.homeInitEvent();
    }

    /* Region for view interactive */
    public void danger() {
        if (!dangerEffect.isRippleAnimationRunning()) {
            setStateDescription(DANGER_DESCRIPTION);
            changeTopViewGroupGackground(Config.DANGER_TYPE);
            stopAllSates(); // Stop all animate before
            // If it was hidden, show it aganin before run animate
            if (dangerEffect.getVisibility() == View.INVISIBLE)
                dangerEffect.setVisibility(View.VISIBLE);
            dangerEffect.startRippleAnimation();
            // vibrate
            homePresenter.vibrateAndShowDialog();
        }
    }

    public void safe() {
        if (!isAutoAlert || (warningEffect.isRippleAnimationRunning() && isInternetAvailable())) {
            if (!safeEffect.isRippleAnimationRunning()) {
                setStateDescription(SAFE_DESCRIPTION);
                changeTopViewGroupGackground(Config.SAFE_TYPE);
                stopAllSates(); // Stop all animate before
                // If it was hidden, show it aganin before run animate
                if (safeEffect.getVisibility() == View.INVISIBLE)
                    safeEffect.setVisibility(View.VISIBLE);
                safeEffect.startRippleAnimation();
            }
            isAutoAlert = true;
        }
    }

    public void warning() {
        if (!warningEffect.isRippleAnimationRunning()) {
            setStateDescription(WARNING_DESCRIPTION);
            changeTopViewGroupGackground(Config.WARNING_TYPE);
            stopAllSates(); // Stop all animate before
            // If it was hidden, show it aganin before run animate
            if (warningEffect.getVisibility() == View.INVISIBLE)
                warningEffect.setVisibility(View.VISIBLE);
            warningEffect.startRippleAnimation();
        }
    }

    public void stopAllSates() {
        // Stop all animation if it's running
        if (safeEffect.isRippleAnimationRunning())
            safeEffect.stopRippleAnimation();
        if (warningEffect.isRippleAnimationRunning())
            warningEffect.stopRippleAnimation();
        if (dangerEffect.isRippleAnimationRunning())
            dangerEffect.stopRippleAnimation();
        // Hide all state if it visiable
        if (safeEffect.getVisibility() == View.VISIBLE)
            safeEffect.setVisibility(View.INVISIBLE);
        if (warningEffect.getVisibility() == View.VISIBLE)
            warningEffect.setVisibility(View.INVISIBLE);
        if (dangerEffect.getVisibility() == View.VISIBLE)
            dangerEffect.setVisibility(View.INVISIBLE);
    }

    /* Region for processing view */
    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public void setStateDescription(String stateDescriptionString) {
        stateDescription.setText(stateDescriptionString);
    }

    public void changeTopViewGroupGackground(int type) {
        Drawable drawable = null;
        switch (type) {
            case Config
                    .SAFE_TYPE:
                drawable = (Drawable) getResources().getDrawable(R.drawable.bg_safe);
                break;
            case Config.DANGER_TYPE:
                drawable = (Drawable) getResources().getDrawable(R.drawable.bg_danger);
                break;
            default:
                drawable = (Drawable) getResources().getDrawable(R.drawable.bg_warning);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            topViewGroup.setBackground(drawable);
        }
    }

    /* region for acitivy process */

    @Override
    protected void onPause() {
        super.onPause();
        acitivyState = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        acitivyState = false;
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        homePresenter.checkServiceState();
        acitivyState = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        acitivyState = true;
        homePresenter.checkServiceState();
    }

    public boolean isInternetAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null;
    }

    void TimerCheckInternet() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                // if not connect to the network, set warning
                if (!isInternetAvailable()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            warning();
                        }
                    });
                } else {
                    homePresenter.capture();
                }
            }
        }, 200, 300);
    }
}
