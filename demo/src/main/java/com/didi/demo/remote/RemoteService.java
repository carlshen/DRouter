package com.didi.demo.remote;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.didi.drouter.demo.R;
import com.didi.drouter.memory.MemoryClient;
import com.didi.drouter.memory.MemoryServer;
import com.didi.drouter.utils.RouterExecutor;
import com.didi.drouter.utils.RouterLogger;

import java.nio.ByteBuffer;

/**
 * Created by gaowei on 2022/2/4
 */
@RequiresApi(api = Build.VERSION_CODES.O_MR1)
public class RemoteService extends Service {
    private MemoryServer server1 = null;
    private MemoryServer server2 = null;
    private boolean stop = false;
    private HandlerThread workThread;
    private static Handler workHandler;

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        workThread = new HandlerThread("WorkThread");
        workThread.start();
        workHandler = new Handler(workThread.getLooper());
        launchMemory();
//        for (int i = 0; i < 1; i++) {
//            MemoryClient client = new MemoryClient(
//                    "com.didi.drouter.remote.demo.host", "host2", 0);
//            final int finalI = i;
//            client.registerObserver(new MemoryClient.MemCallback() {
//                @Override
//                public void onNotify(@NonNull ByteBuffer buffer) {
//
////                    try {
////                        Thread.sleep(13000);
////                    } catch (InterruptedException e) {
////                        e.printStackTrace();
////                    }
//                    RouterLogger.getAppLogger().d("SharedMemory onNotify %s", finalI);
//                }
//
//                @Override
//                public void onServerClosed() {
//                    RouterLogger.getAppLogger().e("SharedMemory server closed");
//                }
//            });
//        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        stop = true;
    }
    private void launchMemory() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (server2 == null) {
                RouterExecutor.worker(new Runnable() {
                    @Override
                    public void run() {
                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                        AssetFileDescriptor fd = getResources().openRawResourceFd(R.raw.big_buck_bunny);
                        retriever.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
                        MediaMetadataRetriever.BitmapParams option = new MediaMetadataRetriever.BitmapParams();
                        option.setPreferredConfig(Bitmap.Config.RGB_565);
                        Bundle config = new Bundle();
                        Bitmap bitmap = retriever.getFrameAtIndex(0, option);
                        config.putInt("width", bitmap.getWidth());
                        config.putInt("height", bitmap.getHeight());

                        RouterLogger.getAppLogger().d("launchMemory start MemoryServer2. ");
                        server2 = MemoryServer.Companion.create("remote2", 5 * 1024 * 1024, 128, config);  // 5MB
                        int index = 0;
                        // 一帧大概10ms
                        while (!stop) {
                            long time = System.currentTimeMillis();
                            try {
                                bitmap = retriever.getFrameAtIndex(index++, option);
                            } catch (Exception e) {
                                index = 0;
                                continue;
                            }
                            ByteBuffer byteBuffer = server2.acquireBuffer();
                            if (byteBuffer != null) {
                                bitmap.copyPixelsToBuffer(byteBuffer);
                                server2.notifyClient();
                            }
                            long time2 = System.currentTimeMillis() - time;
//                            RouterLogger.getAppLogger().d("launchMemory MemoryServer2 retriever time = " + time2);
                            try {
                                Thread.sleep((time2 >= 30) ? 0 : (30 - time2));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        server2.close();
                        server2 = null;
                    }
                });
            } else {
                RouterLogger.getAppLogger().d("launchMemory MemoryServer2 already running. ");
            }
            if (server1 == null) {
                RouterExecutor.worker(new Runnable() {
                    @Override
                    public void run() {
                        RouterLogger.getAppLogger().d("launchMemory start MemoryServer1. ");
                        server1 = MemoryServer.Companion.create("remote1", 10, 128, null);
                        while (!stop) {
                            // 耗时0.x毫秒
                            ByteBuffer byteBuffer = server1.acquireBuffer();
                            if (byteBuffer != null) {
                                increase(byteBuffer);
                                server1.notifyClient();
                            }
                            try {
                                Thread.sleep(2);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        server1.close();
                        server1 = null;
                    }
                });
            } else {
                RouterLogger.getAppLogger().d("launchMemory MemoryServer1 already running. ");
            }
        } else {
            RouterLogger.getAppLogger().d("launchMemory can't start process data for lower version: " + Build.VERSION.SDK_INT);
        }
    }

    private Boolean increase(ByteBuffer num) {
        int n = num.capacity();
        int carry = 1; //进位标志，每轮都加1
        for (int i = n - 1; i >= 0; i--) {
            if (carry == 0) break;
            int next = (num.get(i) + carry);
            if (i == 0 && next > 9) {
                return false;
            }
            if (next > 9) {
                num.put(i, (byte) 0);
                carry = 1; //进位
            } else {
                num.put(i, (byte) next);
                carry = 0;
            }
        }
        return true;
    }

}
