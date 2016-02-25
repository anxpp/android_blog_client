package com.anxpp.blog.fragment;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.anxpp.blog.R;

public class Blog extends Fragment {
	public static final String TAG = Blog.class.getSimpleName();

	private static final String BLOG_SCHEME = "settings";
	private static final String BLOG_AUTHORITY = "blog";
	public static final Uri BLOG_URI = new Uri.Builder().scheme(BLOG_SCHEME).authority(BLOG_AUTHORITY).build();

	//下拉刷新
	private SwipeRefreshLayout swipeRefreshLayout;
	private WebView webView;
	private View rootView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_blog, container, false);
		return rootView;
	}

	@Override
	public void onStart() {
		super.onStart();
		initView();
	}
	//显示下拉刷新
	private void showRefreshLayout(){
		swipeRefreshLayout.setProgressViewOffset(false, 0, 50);
		swipeRefreshLayout.setRefreshing(true);
	}
	//初始化
	private void initView(){
		//设置下拉刷新
		swipeRefreshLayout = (SwipeRefreshLayout)rootView.findViewById(R.id.refresh_layout);
		swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				new Handler().post(new Runnable() {
					@Override
					public void run() {
						webView.reload();
					}
				});
			}
		});

		final ProgressBar viewContentProgress = (ProgressBar)rootView.findViewById(R.id.progress);
		webView = (WebView)rootView.findViewById(R.id.webView);
		//		webView.setVisibility(View.GONE);
		//获取浏览器设置
		WebSettings webSettings = webView.getSettings();
		//有限使用缓存
		webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
//		webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
		//支持js
		webSettings.setJavaScriptEnabled(true);
		//打开页面时， 自适应屏幕
		webSettings.setUseWideViewPort(false); //设置此属性，可任意比例缩放
		webSettings.setLoadWithOverviewMode(true); // 缩放至屏幕的大小
		showRefreshLayout();
		webView.loadUrl("http://anxpp.com");
		//设置应用内打开
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				showRefreshLayout();
				view.loadUrl(url);
				//返回值是true的时候控制去WebView打开，为false调用系统浏览器或第三方浏览器
				return true;
			}
		});
		webView.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					//表示按返回键时的操作
					if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
						showRefreshLayout();
						webView.goBack(); //后退
						return true; //已处理
					}
				}
				return false;
			}
		});
		webView.setWebChromeClient(new WebChromeClient() {
			public void onProgressChanged(WebView view, int progress) {
				viewContentProgress.setProgress(progress);
				viewContentProgress.setVisibility(progress == 100 ? View.GONE : View.VISIBLE);
				Activity activity = getActivity();
				//此处若不加这个判断，会因为Fragment在网页加载完成前退出而导致程序错误
				if (activity == null) return;
//				activity.setTitle("Loading..." + progress + "%");
//				activity.setProgress(progress * 100);
				if (progress == 100) {
//					activity.setTitle(R.string.app_name);
					webView.setVisibility(View.VISIBLE);
					swipeRefreshLayout.setRefreshing(false);
				}
			}
		});
	}
}
