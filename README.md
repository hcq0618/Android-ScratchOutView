Android---ScratchOutView
========================

高仿刮奖效果的View 可以设置刮开多少比例范围 自动显现 - Scratch Card Effect View , and can set the scraping what percentage range automatically displayed bottom view


for example

	ScratchOutView scratch_out_view = (ScratchOutView) findViewById(R.id.scratch_out_view);
		scratch_out_view.setPathPaintWidth(35);
		scratch_out_view.setAutoScratchOut(true);
		scratch_out_view.resetView();
		scratch_out_view.setAutoScratchOutPercent(50);

		// if need scratch again
		// scratch_out_view.resetView();
		// else if no longer scratch 
		// scratch_out_view.destroyView();
