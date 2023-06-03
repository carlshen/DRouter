package com.didi.demo.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;

import com.didi.demo.remote.RemoteService;
import com.didi.drouter.annotation.Router;
import com.didi.drouter.api.DRouter;
import com.didi.drouter.api.Strategy;
import com.didi.drouter.demo.R;
import com.didi.drouter.memory.MemoryClient;
import com.didi.drouter.module_base.ParamObject;
import com.didi.drouter.module_base.remote.IRemoteFunction;
import com.didi.drouter.module_base.remote.RemoteFeature;
import com.didi.drouter.remote.IRemoteCallback;
import com.didi.drouter.utils.RouterExecutor;
import com.didi.drouter.utils.RouterLogger;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gaowei on 2022/1/28
 */
@RequiresApi(api = Build.VERSION_CODES.O_MR1)
@Router(path = "/activity/client_mem")
public class ClientMemoryActivity extends AppCompatActivity implements SurfaceHolder.Callback, View.OnClickListener {

    private MemoryClient client1 = null;
    private MemoryClient client2 = null;
    // 极限压力测试
    private final boolean extremeTesting = false;
    private TextView textView;
    private TextView textViewFrequency;
    private TextView surfaceFrequency;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Bitmap bitmap;
    private Rect rect;
    private boolean surfaceReady = false;
    private IRemoteFunction remoteFunction;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_memory);
        textView = findViewById(R.id.text);
        textViewFrequency = findViewById(R.id.text_frequency);
        surfaceFrequency = findViewById(R.id.surface_frequency);
        surfaceView = findViewById(R.id.surface);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
//        registerLight();
//        registerSurface();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (client1 != null) {
            client1.close();
            client1 = null;
        }
        if (client2 != null) {
            client2.close();
            client2 = null;
        }
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start_remote_data:
                bindRemote();
                RouterExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        do {
                            remoteFunction.handle(new ParamObject[]{}, new ParamObject(), 1,
                                    ClientMemoryActivity.this, new IRemoteCallback.Type2<String, Integer>() {
                                        @Override
                                        public void callback(String s, Integer i) {
                                            RouterLogger.getAppLogger().d("callback 参数 %s, %d", s, i);
                                            RouterLogger.toast("主进程收到子进程的回调");
                                        }

                                        @Override
                                        public int thread() {
                                            return super.thread();
                                        }

                                        @Override
                                        public Lifecycle lifecycle() {
                                            return ClientMemoryActivity.this.getLifecycle();
                                        }

                                        @Override
                                        protected void finalize() throws Throwable {
                                            super.finalize();
                                            RouterLogger.getAppLogger().e("client callback gc");
                                        }

                                        @Override
                                        protected void onServerDead() {
                                            super.onServerDead();
                                            RouterLogger.getAppLogger().e("onServerDead");
                                        }
                                    });

                            if (extremeTesting) {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        } while (extremeTesting);
                    }
                });
                break;
            case R.id.stop_remote_data:
                bindRemote();
                RouterExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        do {
                            remoteFunction.handle(new ParamObject[]{}, new ParamObject(), 2,
                                    ClientMemoryActivity.this, new IRemoteCallback.Type2<String, Integer>() {
                                        @Override
                                        public void callback(String s, Integer i) {
                                            RouterLogger.getAppLogger().d("callback 参数 %s, %d", s, i);
                                            RouterLogger.toast("主进程收到子进程的回调");
                                        }

                                        @Override
                                        public int thread() {
                                            return super.thread();
                                        }

                                        @Override
                                        public Lifecycle lifecycle() {
                                            return ClientMemoryActivity.this.getLifecycle();
                                        }

                                        @Override
                                        protected void finalize() throws Throwable {
                                            super.finalize();
                                            RouterLogger.getAppLogger().e("client callback gc");
                                        }

                                        @Override
                                        protected void onServerDead() {
                                            super.onServerDead();
                                            RouterLogger.getAppLogger().e("onServerDead");
                                        }
                                    });

                            if (extremeTesting) {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        } while (extremeTesting);
                    }
                });
                break;
            case R.id.start_remote_service:
//                startService(new Intent(this, RemoteService.class));
                registerLight();
                registerSurface();
                break;
            case R.id.stop_remote_service:
//                stopService(new Intent(this, RemoteService.class));
                if (client1 != null) {
                    client1.close();
                    client1 = null;
                }
                if (client2 != null) {
                    client2.close();
                    client2 = null;
                }
                break;
            default:
                break;
        }
    }

    private void bindRemote() {
        if (remoteFunction == null) {
            final RemoteFeature feature = new RemoteFeature();
            feature.a = 1;
            feature.b = "1";

            final Map<String, ParamObject> map = new ConcurrentHashMap<>();
            map.put("param", new ParamObject());
            final List<ParamObject> list = new LinkedList<>();
            list.add(new ParamObject());
            final Set<ParamObject> set = new HashSet<>();
            set.add(new ParamObject());

            remoteFunction = DRouter.build(IRemoteFunction.class).setRemote(new Strategy("com.didi.drouter.remote" +
                    ".demo.remote")).setAlias("remote").setFeature(feature).getService(new ParamObject[]{new ParamObject()}, map, list, set, 1);
        }
    }

    private void registerLight() {
        if (client1 != null) {
            RouterLogger.getAppLogger().d("ClientMemoryActivity registerLight no need as already registered. ");
            return;
        }
        client1 = new MemoryClient("com.didi.drouter.remote.demo.remote", "remote1", 0, 0);
        client1.registerObserver(new MemoryClient.MemCallback() {
            long t;
            int count;

            @Override
            public void onConnected(@Nullable Bundle info) {
                RouterLogger.getAppLogger().d("ClientMemoryActivity registerLight onConnected. ");
            }
            @Override
            public void onServerClosed() {
                RouterLogger.getAppLogger().d("ClientMemoryActivity registerLight onServerClosed. ");
                client1.close();
                client1 = null;
            }

            @Override
            public void onNotify(@NonNull ByteBuffer buffer) {
                if (++count % 500 == 0) {
                    long now = System.nanoTime();
                    long diff = now - t;
                    if (diff != 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textViewFrequency.setText(String.format("帧率 %s", 1000_000_000 / diff));
                            }
                        });
                    }
                    count = 0;
                }
                t = System.nanoTime();
                // 耗时1ms左右
//                StringBuffer b = new StringBuffer();
//                for (int i = 0; i < buffer.capacity(); i++) {
//                    b.append(buffer.get(i));
//                }
//                textView.setText(b);

                //RouterLogger.getAppLogger().d("SharedMemory onNotify1");
            }
        });
    }

    private void registerSurface() {
        if (client2 != null) {
            RouterLogger.getAppLogger().d("ClientMemoryActivity registerSurface no need as already registered. ");
            return;
        }
        client2 = new MemoryClient("com.didi.drouter.remote.demo.remote", "remote2", 0, 0);
        client2.registerObserver(new MemoryClient.MemCallback() {
            Bundle config;
            long t;
            int count;

            @Override
            public void onConnected(@Nullable Bundle info) {
                config = info;
                RouterLogger.getAppLogger().d("ClientMemoryActivity registerSurface onConnected. ");
            }
            @Override
            public void onServerClosed() {
                RouterLogger.getAppLogger().d("ClientMemoryActivity registerSurface onServerClosed. ");
                client2.close();
                client2 = null;
            }

            @Override
            public void onNotify(@NonNull ByteBuffer buffer) {
                if (!surfaceReady) return;
                if (++count % 30 == 0) {
                    long now = System.currentTimeMillis();
                    long diff = now - t;
                    if (diff != 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                surfaceFrequency.setText(String.format("帧率 %s", 1000 / diff));
                            }
                        });
                    }
                    count = 0;
                }
                t = System.currentTimeMillis();

                Canvas canvas = surfaceHolder.lockCanvas();
                if (config != null) {
                    if (bitmap == null) {
                        int width = config.getInt("width");
                        int height = config.getInt("height");
                        int scale = surfaceView.getWidth() / width;
                        int fullHeight = height * scale;
                        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                        rect = new Rect(0, surfaceView.getHeight() / 2 - fullHeight / 2, surfaceView.getWidth(),
                                surfaceView.getHeight() / 2 + fullHeight / 2);
                    }
                    try {
                        bitmap.copyPixelsFromBuffer(buffer);
                        // 绘制比较慢，20ms左右
                        canvas.drawBitmap(bitmap, null, rect, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
    }

}
