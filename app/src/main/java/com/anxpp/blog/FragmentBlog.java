package com.anxpp.blog;

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

public class FragmentBlog extends Fragment {
	public static final String TAG = FragmentBlog.class.getSimpleName();

	private static final String BLOG_SCHEME = "settings";
	private static final String BLOG_AUTHORITY = "blog";
	public static final Uri BLOG_URI = new Uri.Builder().scheme(BLOG_SCHEME).authority(BLOG_AUTHORITY).build();

	//下拉刷新
	private SwipeRefreshLayout refreshLayout;
	private WebView webView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_blog, container, false);
	}

	@Override
	public void onStart() {
		super.onStart();
		initView();
	}

	private void initView(){

		//设置下拉刷新
		refreshLayout = (SwipeRefreshLayout)getView().findViewById(R.id.refresh_layout);
		refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
//						refreshLayout.setRefreshing(false);
						webView.reload();
					}
				}, 0);
			}
		});

		final ProgressBar viewContentProgress = (ProgressBar) getView().findViewById(R.id.progress);
		webView = (WebView) getView().findViewById(R.id.webView);
		//		webView.setVisibility(View.GONE);
		webView.setWebChromeClient(new WebChromeClient(){
			public void onProgressChanged(WebView view, int progress){
				viewContentProgress.setProgress(progress);
				viewContentProgress.setVisibility(progress == 100 ? View.GONE : View.VISIBLE);
				Activity activity = getActivity();
				/**
				 * 此处若不加这个判断，会因为Fragment在网页加载完成前退出而导致程序错误
				 * I am not sure why are you getting this error,
				 * i think it should be something like NullPointerException.
				 * Try next:
				 *     Evert time you calling getActivity() on Fragment instance you should be sure,
				 *     that fragment is actually have this Activity.
				 *     Because when your webview is loading you are calling this function:
				 * */
				if(activity == null) return;
				activity.setTitle("Loading..." + progress + "%");
				activity.setProgress(progress * 100);
				if(progress == 100){
					activity.setTitle(R.string.app_name);
					webView.setVisibility(View.VISIBLE);
					refreshLayout.setRefreshing(false);
				}
			}
		});
		webView.setWebViewClient(new WebViewClient() {
			public boolean shouldOverrideUrlLoading(WebView view, String url){
				view.loadUrl(url);
				return true;
			}
		});
		//获取浏览器设置
		WebSettings webSettings = webView.getSettings();
		//有限使用缓存
		//webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
		webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
		//支持js
		webSettings.setJavaScriptEnabled(true);
		//打开页面时， 自适应屏幕
		webSettings.setUseWideViewPort(false); //设置此属性，可任意比例缩放
		webSettings.setLoadWithOverviewMode(true); // 缩放至屏幕的大小
		webView.loadUrl("http://anxpp.com");
		//设置应用内打开
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				//返回值是true的时候控制去WebView打开，为false调用系统浏览器或第三方浏览器
				view.loadUrl(url);
				return true; //false为调用浏览器打开
			}
		});
		webView.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) { //表示按返回键 时的操作
						webView.goBack(); //后退
						return true; //已处理
					}
				}
				return false;
			}
		});
	}
}
