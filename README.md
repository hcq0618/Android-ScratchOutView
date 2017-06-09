Android-ScratchOutView
========================

高仿刮奖效果的View 可以设置刮开多少比例范围 自动显现 - Scratch card effect view , and can set the scraping what percentage range automatically displayed below ui


for example

```java
	ScratchOutView scratch_out_view = (ScratchOutView) findViewById(R.id.scratch_out_view);
		scratch_out_view.setPathPaintWidth(35);
		scratch_out_view.setAutoScratchOut(true);
		scratch_out_view.setAutoScratchOutPercent(50);
		scratch_out_view.resetView();
		
		// if need scratch again
		// scratch_out_view.resetView();
		// else if no longer scratch 
		// scratch_out_view.destroyView();
```

## License

MIT License

Copyright (c) 2017 Hcq

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
