package com.git.scratchoutview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;

public class ScratchOutView extends SurfaceView implements
        SurfaceHolder.Callback {

    private ScratchViewThread thread;
    private ArrayList<Path> pathList = new ArrayList<>();
    private int overlayColor = 0xff444444;// default color is dark gray
    private Paint overlayPaint;
    private int pathPaintWidth = 30;
    private boolean isEnableScratch = true, isShow = true;
    private boolean isAntiAlias = true;
    private Path path;
    private float startX = 0;
    private float startY = 0;
    private boolean scratchStart = false;
    private boolean isAutoScratchOut = false;
    private int autoScratchOutPercent = 50;

    private IScratchListener scratchListener;

    public interface IScratchListener {
        void onFinishScratch(ScratchOutView sv);

        void onStartScratch(ScratchOutView sv);

        void onAutoScratchOut(ScratchOutView sv);
    }

    public ScratchOutView(Context context) {
        this(context, null);
    }

    public ScratchOutView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        init();
    }

    private void init() {

        setZOrderOnTop(true);
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        holder.setFormat(PixelFormat.TRANSPARENT);

        overlayPaint = new Paint();
        overlayPaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
        overlayPaint.setStyle(Paint.Style.STROKE);
        overlayPaint.setStrokeCap(Paint.Cap.ROUND);
        overlayPaint.setStrokeJoin(Paint.Join.ROUND);

    }

    private Canvas computeCanvas;
    private Bitmap canvasBitmap;

    @Override
    public void onDraw(Canvas canvas) {
        if (!isShow) {
            // 清除刮奖蒙层
            canvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);
            recycle();
            return;
        }

        if (computeCanvas == null) {
            canvasBitmap = Bitmap.createBitmap(getWidth(), getHeight(),
                    Config.ARGB_8888);
            computeCanvas = new Canvas(canvasBitmap);
        }

        canvas.drawColor(overlayColor);
        computeCanvas.drawColor(overlayColor);

        for (Path path : pathList) {
            overlayPaint.setAntiAlias(isAntiAlias);
            overlayPaint.setStrokeWidth(pathPaintWidth);

            canvas.drawPath(path, overlayPaint);
            computeCanvas.drawPath(path, overlayPaint);
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {
        if (thread == null)
            return false;

        if (!isEnableScratch) {
            return true;
        }

        float x = me.getX();
        float y = me.getY();

        switch (me.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path = new Path();
                path.moveTo(x, y);
                startX = x;
                startY = y;
                pathList.add(path);

                if (scratchListener != null)
                    scratchListener.onStartScratch(this);
                break;
            case MotionEvent.ACTION_MOVE:
                if (scratchStart) {
                    path.lineTo(x, y);
                } else {
                    if (isScratch(startX, x, startY, y)) {
                        scratchStart = true;
                        path.lineTo(x, y);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                scratchStart = false;

                if (isAutoScratchOut) {
                    int scratchPercent = computeScratchOutAreaSize();// 刮开的面积百分比

                    // 刮开面积超过一定百分比 则自动全部展现出来
                    if (scratchPercent >= autoScratchOutPercent) {
                        post(new Runnable() {

                            @Override
                            public void run() {
                                // TODO Auto-generated method stub
                                isShow = false;
                                invalidSurfaceView();
                                if (scratchListener != null)
                                    scratchListener.onAutoScratchOut(ScratchOutView.this);
                            }
                        });
                    }
                }

                if (scratchListener != null)
                    scratchListener.onFinishScratch(this);
                break;
        }
        return true;
    }

    // 计算刮开的面积 遍历判断bitmap中是0的像素点比例（为0的就是透明区域）
    private int computeScratchOutAreaSize() {
        if (canvasBitmap == null)
            return 0;
        int[] pixels = new int[canvasBitmap.getWidth()
                * canvasBitmap.getHeight()];
        int width = getWidth();
        int height = getHeight();
        canvasBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        int sum = pixels.length;
        int num = 0;
        for (int pixel : pixels) {
            if (pixel == 0) {
                num++;
            }
        }

        return num * 100 / sum;
    }

    private boolean isScratch(float oldX, float x, float oldY, float y) {
        float distance = (float) Math.sqrt(Math.pow(oldX - x, 2)
                + Math.pow(oldY - y, 2));
        return distance > pathPaintWidth * 2;
    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
        // do nothing
    }

    @Override
    public void surfaceCreated(SurfaceHolder arg0) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0) {
        destroySurfaceThread();
    }

    private void destroySurfaceThread() {
        if (thread == null)
            return;
        thread.setRunning(false);
        thread.interrupt();

        while (thread != null) {
            try {
                thread.join();
                thread = null;
            } catch (InterruptedException e) {
                // do nothing but keep retry
            }
        }
    }

    private class ScratchViewThread extends Thread {
        private final SurfaceHolder mSurfaceHolder;
        private ScratchOutView mView;
        private boolean mRun = false;

        ScratchViewThread(SurfaceHolder surfaceHolder,
                          ScratchOutView view) {
            mSurfaceHolder = surfaceHolder;
            mView = view;
        }

        void setRunning(boolean run) {
            mRun = run;
        }

        SurfaceHolder getSurfaceHolder() {
            return mSurfaceHolder;
        }

        @Override
        public void run() {
            Canvas canvas;
            long currentTimeMillis;
            while (mRun) {
                canvas = null;
                currentTimeMillis = System.currentTimeMillis();
                try {
                    canvas = mSurfaceHolder.lockCanvas(null);
                    if (canvas != null) {
                        mView.draw(canvas);
                    }

                    Thread.sleep(Math.max(
                            0,
                            33 - (System.currentTimeMillis() - currentTimeMillis)));// 每秒30帧

                } catch (Exception ex) {
                } finally {
                    if (canvas != null) {
                        mSurfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }

            }
        }
    }

    private void invalidSurfaceView() {
        Canvas canvas = null;
        SurfaceHolder surfaceHolder = getHolder();
        try {
            canvas = surfaceHolder.lockCanvas(null);
            if (canvas != null) {
                draw(canvas);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (canvas != null) {
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    private void recycle() {
        pathList.clear();

        if (canvasBitmap != null)
            canvasBitmap.recycle();

        canvasBitmap = null;
        computeCanvas = null;
    }

    public void resetView() {
        isEnableScratch = true;
        isShow = true;

        if (thread == null) {
            thread = new ScratchViewThread(getHolder(), this);
            thread.setRunning(true);
            thread.start();

            recycle();
        }

    }

    // 底部全部显现出来
    public void destroyView() {
        recycle();

        destroySurfaceThread();

        isEnableScratch = false;
        isShow = false;
        invalidSurfaceView();
    }

    public boolean isEnableScratch() {
        return isEnableScratch;
    }

    public void setEnableScratch(boolean flag) {
        isEnableScratch = flag;
    }

    public void setOverlayColor(int color) {
        overlayColor = color;
    }

    public void setPathPaintWidth(int w) {
        pathPaintWidth = w;
    }

    public void setAntiAlias(boolean flag) {
        isAntiAlias = flag;
    }

    public final boolean isShow() {
        return isShow;
    }

    public final void setShow(boolean isShow) {
        this.isShow = isShow;
    }

    public final void setAutoScratchOut(boolean isAutoScratchOut) {
        this.isAutoScratchOut = isAutoScratchOut;
    }

    public final void setAutoScratchOutPercent(int autoScratchOutPercent) {
        this.autoScratchOutPercent = autoScratchOutPercent;
    }

    public IScratchListener getScratchListener() {
        return scratchListener;
    }

    public void setScratchListener(IScratchListener listener) {
        this.scratchListener = listener;
    }
}
