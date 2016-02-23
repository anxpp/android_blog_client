package com.anxpp.blog;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import com.anxpp.blog.stickylistheaders.StickyListHeadersListView;

public class FragmentHome extends Fragment implements AdapterView.OnItemClickListener{
	public static final String TAG = FragmentHome.class.getSimpleName();

	private static final String HOME_SCHEME = "settings";
	private static final String HOME_AUTHORITY = "home";
	public static final Uri HOME_URI = new Uri.Builder().scheme(HOME_SCHEME).authority(HOME_AUTHORITY).build();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_home, container, false);
	}

	@Override
	public void onStart() {
		super.onStart();

		//适配器
		TestBaseAdapter mAdapter = new TestBaseAdapter(getActivity());

		//具体列表控件
		StickyListHeadersListView stickyList = (StickyListHeadersListView) getView().findViewById(R.id.list);
		//事件监听
		stickyList.setOnItemClickListener(this);
		//添加页首页末布局
		stickyList.addHeaderView(getActivity().getLayoutInflater().inflate(R.layout.list_header, null));
		stickyList.addFooterView(getActivity().getLayoutInflater().inflate(R.layout.list_footer, null));
		stickyList.setEmptyView(getView().findViewById(R.id.empty));
		stickyList.setDrawingListUnderStickyHeader(true);
		stickyList.setAreHeadersSticky(true);
		stickyList.setAdapter(mAdapter);
		//从顶部的偏移量
		stickyList.setStickyHeaderTopOffset(-20);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Toast.makeText(getActivity(), "Item " + position + " clicked!", Toast.LENGTH_SHORT).show();
	}
}
