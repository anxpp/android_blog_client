package com.anxpp.blog.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import com.anxpp.blog.R;
import com.anxpp.blog.adapter.InitialAdapter;
import com.anxpp.blog.stickylistheaders.StickyListHeadersListView;

public class Home extends Fragment implements AdapterView.OnItemClickListener{

	public static final String TAG = Home.class.getSimpleName();

	private static final String HOME_SCHEME = "settings";
	private static final String HOME_AUTHORITY = "home";
	public static final Uri HOME_URI = new Uri.Builder().scheme(HOME_SCHEME).authority(HOME_AUTHORITY).build();

	private View rootView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_home, container, false);
		return rootView;
	}

	@Override
	public void onStart() {
		super.onStart();

		//适配器
		InitialAdapter mAdapter = new InitialAdapter(getActivity());

		//具体列表控件
		StickyListHeadersListView stickyList = (StickyListHeadersListView) rootView.findViewById(R.id.list);
		//事件监听
		stickyList.setOnItemClickListener(this);
		//添加页首页末布局
		stickyList.addHeaderView(getActivity().getLayoutInflater().inflate(R.layout.list_header, null));
		stickyList.addFooterView(getActivity().getLayoutInflater().inflate(R.layout.list_footer, null));
		stickyList.setEmptyView(rootView.findViewById(R.id.empty));
		stickyList.setDrawingListUnderStickyHeader(true);
//		stickyList.setAreHeadersSticky(true);	//设置头部固定
		stickyList.setAdapter(mAdapter);
		//从顶部的偏移量
		stickyList.setStickyHeaderTopOffset(0);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Toast.makeText(getActivity(), "Item " + position + " clicked!", Toast.LENGTH_SHORT).show();
	}
}
