package com.blogspot.tndev1403.antitheft.Presenter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.VibrationEffect;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.blogspot.tndev1403.antitheft.BuildConfig;
import com.blogspot.tndev1403.antitheft.Modal.AntitheftService;
import com.blogspot.tndev1403.antitheft.Modal.Firebase;
import com.blogspot.tndev1403.antitheft.R;
import com.blogspot.tndev1403.antitheft.Stored.Config.Config;
import com.blogspot.tndev1403.antitheft.View.Home;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.net.InetAddress;

import cn.pedant.SweetAlert.SweetAlertDialog;
import es.dmoral.toasty.Toasty;

public class HomePresenter {
    public final static String TAG = "TN_HomePresenter";
    Home home;
    Resources res;
    UpdateState updateState = null;
    DestroyAll destroyAll = null;
    SweetAlertDialog stateDialog;
    MediaPlayer playAlertSound;
    public SweetAlertDialog turnOffVibrate;

    public HomePresenter(Home home) {
        this.home = home;
        this.res = home.getResources();
        playAlertSound = new MediaPlayer();
        capture();
    }

    public void homeInitEvent() {
        initSafeButton();
        initDangerButton();
        initDisconnectButton();
        initInfoButton();
    }

    //region /* Init event per view */

    void initSafeButton() {
        home.safeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertWhenPressButtonNotEnough();
            }
        });
        home.safeButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!home.isInternetAvailable()) {
                    home.isAutoAlert = false;
                    showNetWorkErrorDialog();
                    return false;
                }
                home.isAutoAlert = false;
                updateState = new UpdateState();
                updateState.execute(Config.SAFE_TYPE);
                return true;
            }
        });
    }

    void initDangerButton() {
        home.dangerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertWhenPressButtonNotEnough();
            }
        });
        home.dangerButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!home.isInternetAvailable()) {
                    showNetWorkErrorDialog();
                    return false;
                }
                updateState = new UpdateState();
                updateState.execute(Config.DANGER_TYPE);
                return true;
            }
        });
    }

    void initDisconnectButton() {
        home.disConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SweetAlertDialog disConnectConfirm = new SweetAlertDialog(home, SweetAlertDialog
                        .WARNING_TYPE)
                        .setTitleText("Ngưng hoạt động?")
                        .setContentText("Mọi dịch vụ và kết nối đến máy chủ sẽ chấm dứt!")
                        .setConfirmButton("Xác nhận", new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sweetAlertDialog) {
                                sweetAlertDialog.cancel();
                                destroyAll = new DestroyAll();
                                destroyAll.execute(true);
                            }
                        })
                        .setCancelButton("Quay lại", new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sweetAlertDialog) {
                                sweetAlertDialog.cancel();
                            }
                        });
                disConnectConfirm.setCancelable(false);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    disConnectConfirm.create();
                }
                disConnectConfirm.show();
            }
        });
    }

    void initInfoButton() {
        home.infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SweetAlertDialog description = new SweetAlertDialog(home, SweetAlertDialog.NORMAL_TYPE)
                        .setTitleText(home.getResources().getString(R.string.app_name) + " v" + BuildConfig.VERSION_NAME)
                        .setContentText("Ngô Minh Quân");
                description.setConfirmButton("Đóng", new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.cancel();
                    }
                });
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    description.create();
                }
                description.show();
            }
        });
    }
    //endregion

    //region Destroiy
    void DestroyAsyncTask() {
        if (updateState != null) {
            if (!updateState.isCancelled())
                updateState.cancel(true);
            updateState = null;
        }
    }
    //endregion

    //region Capture data change from firebase
    public void doUpdate(Object value) {
        // else, otherwise
        int i = -1;
        while (i < 10) {
            i++;
            try {
                int type = Integer.parseInt("" + value);
                if (type == Config.SAFE_TYPE) {
                    home.safe();
                }
                else if (type == Config.DANGER_TYPE)
                    home.danger();
                return;
            } catch (Exception e) {
                Log.e(TAG, "doUpdate: " + e);
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    public void capture() {
        Firebase.databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Object value = new Object();
                try {
                    value = dataSnapshot.getValue();
                    doUpdate(value);
                } catch (Exception e) {
                    Log.e(TAG, "onDataChange: " + e + " || " + value);
                    home.warning();
                    home.setStateDescription("Lỗi khi nhận và phân giải giá trị từ máy chủ.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (databaseError.getCode() == DatabaseError.DISCONNECTED) {
                    home.warning();
                    home.setStateDescription("Đã ngắt kết nối đến máy chủ.");
                }
            }
        });
    }
    //endregion

    //region Class for anycTask and controll processing
    private class UpdateState extends AsyncTask<Integer, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(Integer... types) {
            try {
                Firebase.databaseReference.setValue("" + types[0]);
                final int temp = types[0];
                home.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stateDialog.cancel();
                        if (temp == Config.SAFE_TYPE)
                            home.safe();
                        else if (temp == Config.DANGER_TYPE)
                            home.danger();
                    }
                });
                return true;
            } catch (Exception e) {
                home.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stateDialog.cancel();
                        home.warning();
                    }
                });
                Log.d(TAG, "doInBackground: " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            stateDialog = new SweetAlertDialog(home, SweetAlertDialog
                    .PROGRESS_TYPE);
            stateDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
            stateDialog.setTitleText("Đang thao tác");
            stateDialog.setCancelButton("Hủy", new SweetAlertDialog.OnSweetClickListener() {
                @Override
                public void onClick(SweetAlertDialog sweetAlertDialog) {
                    sweetAlertDialog.cancel();
                    if (updateState != null && !updateState.isCancelled()) {
                        updateState.cancel(true);
                        DestroyAsyncTask();
                    }
                }
            });
            stateDialog.setCancelable(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                stateDialog.create();
            }
            stateDialog.show();
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            playMediaPlayer(aBoolean);
            if (aBoolean)
                showSuccessDialog();
            else
                showFailDialog();
            DestroyAsyncTask();
        }
    }

    private class DestroyAll extends AsyncTask<Boolean, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(Boolean... want) {
            if (want[0]) {
                try {
                    Firebase.database.goOffline();
                    home.stopService(home.antiTheftService);
                } catch (Exception e) {
                    Log.e(TAG, "doInBackground: " + e.getMessage());
                    return false;
                }
            }
            return true;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            SweetAlertDialog destroyAllDialog = new SweetAlertDialog(home, SweetAlertDialog
                    .PROGRESS_TYPE);
            destroyAllDialog.getProgressHelper().setBarColor(Color.parseColor("#ff9f43"));
            destroyAllDialog.setTitleText("Đang ngưng kết nối");
            destroyAllDialog.setCancelButton("Hủy", new SweetAlertDialog.OnSweetClickListener() {
                @Override
                public void onClick(SweetAlertDialog sweetAlertDialog) {
                    if (destroyAll != null && !destroyAll.isCancelled()) {
                        destroyAll.cancel(true);
                    }
                    sweetAlertDialog.cancel();
                }
            });
            destroyAllDialog.setCancelable(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                destroyAllDialog.create();
            }
            try {
                destroyAllDialog.show();
            } catch (Exception e) {
                //
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            playMediaPlayer(aBoolean);
            if (aBoolean) {
                showSuccessDialog();
                home.finish();
            } else
                showFailDialog();
            DestroyAsyncTask();
        }
    }

    void cancelMediaPlayer() {
        try {
            if (home.mp != null && home.mp.isPlaying()) {
                home.mp.stop();
                home.mp.release();
            }
        } catch (Exception e) {
            Log.e(TAG, "cancelMediaPlayer: " + e.getMessage());
        }
    }

    void playMediaPlayer(boolean isSuccess) {
        if (isSuccess)
            home.mp = MediaPlayer.create(home, R.raw.success);
        else
            home.mp = MediaPlayer.create(home, R.raw.error);
        home.mp.start();
    }

    public void vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            home.vb.vibrate(VibrationEffect.createOneShot(Config.VIBRATE_TIME, VibrationEffect
                    .DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            home.vb.vibrate(Config.VIBRATE_TIME);
        }
    }

    void playAlertSound() {
        playAlertSound = MediaPlayer.create(home, R.raw.alert);
        playAlertSound.setLooping(true);
        playAlertSound.start();
    }

    public void cancelAlertSound() {
        try {
            if (playAlertSound != null && playAlertSound.isPlaying()) {
                if (playAlertSound.isLooping())
                    playAlertSound.setLooping(false);
                playAlertSound.stop();
                playAlertSound.release();
            }
        } catch (Exception e) {
            Log.e(TAG, "cancelMediaPlayer: " + e.getMessage());
        }
    }

    public void cancelAlert() {
        cancelAlertSound();
        if (home.vb.hasVibrator())
            home.vb.cancel();
        if (turnOffVibrate != null && turnOffVibrate.isShowing())
            turnOffVibrate.cancel();
    }

    public void vibrateAndShowDialog() {
        playAlertSound();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            home.vb.vibrate(VibrationEffect.createWaveform(Config.VIBRATE_WAVE[0], (int) Config
                    .VIBRATE_WAVE[1][0]));
        } else {
            long sum = 0;
            for (long i : Config.VIBRATE_WAVE[0])
                sum += i;
            sum *= Config.VIBRATE_WAVE[0][1];
            home.vb.vibrate(sum);
        }
        turnOffVibrate = new SweetAlertDialog(home, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("CHÚ Ý!")
                .setContentText("Báo động của bạn đang được kích hoạt, vui lòng kiểm tra an ninh!")
                .setConfirmButton("Đã biết", new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.cancel();
                        cancelAlert();
                    }
                });
        turnOffVibrate.setCancelable(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            turnOffVibrate.create();
        }
        try {
            turnOffVibrate.show();
        } catch (Exception e) {
            //
        }
    }
    //endregion

    //region Create custom view
    void showSuccessDialog() {
        SweetAlertDialog success = new SweetAlertDialog(home, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("Thao tác thành công!")
                .setConfirmButton("Đóng", new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.cancel();
                        cancelMediaPlayer();
                    }
                });
        success.setCancelable(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            success.create();
        }
        try {
            success.show();
        } catch (Exception e) {
            //
        }
    }

    void showFailDialog() {
        SweetAlertDialog fail = new SweetAlertDialog(home, SweetAlertDialog.ERROR_TYPE)
                .setTitleText("Vui lòng thử lại!")
                .setConfirmButton("Đóng", new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.cancel();
                        cancelMediaPlayer();
                    }
                });
        fail.setCancelable(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fail.create();
        }
        try {
            fail.show();
        } catch (Exception e) {
            //
        }
    }

    void showNetWorkErrorDialog() {
        SweetAlertDialog noInternet = new SweetAlertDialog(home,
                SweetAlertDialog.CUSTOM_IMAGE_TYPE)
                .setTitleText("Mất kết nối!")
                .setContentText("Vui lòng kiểm tra lại kết nối trước khi thao tác!")
                .setCustomImage(R.drawable.ic_no_wifi)
                .setConfirmButton("Đóng", null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            noInternet.create();
        }
        noInternet.show();
    }

    void AlertWhenPressButtonNotEnough() {
        Toasty.info(home, "Hãy nhấn giữ để xác nhận!", Toast.LENGTH_SHORT, true).show();
    }
    //endregion

    public void checkServiceState() {
        if (AntitheftService.isDanger)
        {
            home.danger();
        }
    }

}
