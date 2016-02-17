package com.anxpp.blog;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FragmentAbout extends Fragment {
	public static final String TAG = FragmentAbout.class.getSimpleName();

	private static final String ABOUT_SCHEME = "settings";
	private static final String ABOUT_AUTHORITY = "about";
	public static final Uri ABOUT_URI = new Uri.Builder().scheme(ABOUT_SCHEME).authority(ABOUT_AUTHORITY).build();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_about, container, false);
	}
}
