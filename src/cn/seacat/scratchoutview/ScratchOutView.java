package cn.seacat.scratchoutview;

import java.util.ArrayList;

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

public class ScratchOutView extends SurfaceView implements
		SurfaceHolder.Callback {

	private WScratchViewThread mThread;
	private ArrayList<Path> mPathList = new ArrayList<Path>();
	private int mOverlayColor = 0xff444444;// default color is dark gray
	private Paint mOverlayPaint;
	private int mPathPaintWidth = 30;
	private boolean mIsScratchable = true, isShow = true;
	private boolean mIsAntiAlias = true;
	private Path path;
	private float startX = 0;
	private float startY = 0;
	private boolean mScratchStart = false;
	private boolean isAutoScratchOut = false;
	private int autoScratchOutPercent = 50;

	public ScratchOutView(Context context) {
		this(context, null);
	}

	public ScratchOutView(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);
		init(ctx);
	}

	private void init(Context context) {

		setZOrderOnTop(true);
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		holder.setFormat(PixelFormat.TRANSPARENT);

		mOverlayPaint = new Paint();
		mOverlayPaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
		mOverlayPaint.setStyle(Paint.Style.STROKE);
		mOverlayPaint.setStrokeCap(Paint.Cap.ROUND);
		mOverlayPaint.setStrokeJoin(Paint.Join.ROUND);

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

		canvas.drawColor(mOverlayColor);
		computeCanvas.drawColor(mOverlayColor);

		for (Path path : mPathList) {
			mOverlayPaint.setAntiAlias(mIsAntiAlias);
			mOverlayPaint.setStrokeWidth(mPathPaintWidth);

			canvas.drawPath(path, mOverlayPaint);
			computeCanvas.drawPath(path, mOverlayPaint);
		}

	}

	@Override
	public boolean onTouchEvent(MotionEvent me) {
		if (mThread == null)
			return false;

		synchronized (mThread.getSurfaceHolder()) {
			if (!mIsScratchable) {
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
				mPathList.add(path);

				if (touchCallBack != null)
					touchCallBack.onStartScratch(this);
				break;
			case MotionEvent.ACTION_MOVE:
				if (mScratchStart) {
					path.lineTo(x, y);
				} else {
					if (isScratch(startX, x, startY, y)) {
						mScratchStart = true;
						path.lineTo(x, y);
					}
				}
				break;
			case MotionEvent.ACTION_UP:
				mScratchStart = false;

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
								if (touchCallBack != null)
									touchCallBack
											.onAutoScratchOut(ScratchOutView.this);
							}
						});
					}
				}

				if (touchCallBack != null)
					touchCallBack.onFinishScratch(this);
				break;
			}
			return true;
		}
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
		for (int i = 0; i < sum; i++) {
			if (pixels[i] == 0) {
				num++;
			}
		}

		return num * 100 / sum;
	}

	private boolean isScratch(float oldX, float x, float oldY, float y) {
		float distance = (float) Math.sqrt(Math.pow(oldX - x, 2)
				+ Math.pow(oldY - y, 2));
		if (distance > mPathPaintWidth * 2) {
			return true;
		} else {
			return false;
		}
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
		if (mThread == null)
			return;
		mThread.setRunning(false);
		mThread.interrupt();

		while (mThread != null) {
			try {
				mThread.join();
				mThread = null;
			} catch (InterruptedException e) {
				// do nothing but keep retry
			}
		}
	}

	private class WScratchViewThread extends Thread {
		private SurfaceHolder mSurfaceHolder;
		private ScratchOutView mView;
		private boolean mRun = false;

		public WScratchViewThread(SurfaceHolder surfaceHolder,
				ScratchOutView view) {
			mSurfaceHolder = surfaceHolder;
			mView = view;
		}

		public void setRunning(boolean run) {
			mRun = run;
		}

		public SurfaceHolder getSurfaceHolder() {
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
					synchronized (mSurfaceHolder) {
						if (canvas != null) {
							mView.onDraw(canvas);
						}
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
		SurfaceHolder mSurfaceHolder = getHolder();
		try {
			canvas = mSurfaceHolder.lockCanvas(null);
			synchronized (mSurfaceHolder) {
				if (canvas != null) {
					onDraw(canvas);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (canvas != null) {
				mSurfaceHolder.unlockCanvasAndPost(canvas);
			}
		}
	}

	private void recycle() {
		if (mThread != null)
			synchronized (mThread.getSurfaceHolder()) {
				mPathList.clear();
			}

		if (canvasBitmap != null)
			canvasBitmap.recycle();
		canvasBitmap = null;
		computeCanvas = null;
	}

	public void resetView() {
		mIsScratchable = true;
		isShow = true;

		if (mThread == null) {
			mThread = new WScratchViewThread(getHolder(), this);
			mThread.setRunning(true);
			mThread.start();

			recycle();
		}

	}

	// 底部全部显现出来
	public void destroyView() {
		recycle();

		destroySurfaceThread();

		mIsScratchable = false;
		isShow = false;
		invalidSurfaceView();
	}

	public boolean isScratchable() {
		return mIsScratchable;
	}

	public void setScratchable(boolean flag) {
		mIsScratchable = flag;
	}

	public void setOverlayColor(int color) {
		mOverlayColor = color;
	}

	public void setPathPaintWidth(int w) {
		mPathPaintWidth = w;
	}

	public void setAntiAlias(boolean flag) {
		mIsAntiAlias = flag;
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

	private IScratchView touchCallBack;

	public IScratchView getTouchCallBack() {
		return touchCallBack;
	}

	public void setTouchCallBack(IScratchView impl) {
		this.touchCallBack = impl;
	}

	public static interface IScratchView {
		public void onFinishScratch(ScratchOutView sv);

		public void onStartScratch(ScratchOutView sv);

		public void onAutoScratchOut(ScratchOutView sv);
	}
}
