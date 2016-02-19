package com.anxpp.blog;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FragmentHome extends Fragment {
	public static final String TAG = FragmentHome.class.getSimpleName();

	private static final String HOME_SCHEME = "settings";
	private static final String HOME_AUTHORITY = "home";
	public static final Uri HOME_URI = new Uri.Builder().scheme(HOME_SCHEME).authority(HOME_AUTHORITY).build();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_home, container, false);
	}
}
