package com.github.scratchoutview.example;

import android.app.Activity;
import android.os.Bundle;
import cn.seacat.scratchoutview.R;
import cn.seacat.scratchoutview.ScratchOutView;

public class MainActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);

		ScratchOutView scratch_out_view = (ScratchOutView) findViewById(R.id.scratch_out_view);
		scratch_out_view.setPathPaintWidth(35);
		scratch_out_view.setAutoScratchOut(true);
		scratch_out_view.resetView();

		// if need scratch again
		// scratch_out_view.resetView();
		// else if no longer scratch 
		// scratch_out_view.destroyView();
	}
}
