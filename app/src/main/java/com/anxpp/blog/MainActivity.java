package com.anxpp.blog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.anxpp.blog.plus.ActionsAdapter;
import com.anxpp.blog.plus.ActionsContentView;
import com.anxpp.blog.satellite.SatelliteMenu;
import com.anxpp.blog.satellite.SatelliteMenuItem;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_URI = "state:uri";
    private static final String STATE_FRAGMENT_TAG = "state:fragment_tag";
    private String currentContentFragmentTag = null;
    private SettingsChangedListener mSettingsChangedListener;
    //进入时默认的fragment
    private Uri currentUri = FragmentHome.HOME_URI;
    /**     * 内容主布局     */
    private ActionsContentView viewActionsContentView;
    private SatelliteMenu menu;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettingsChangedListener = new SettingsChangedListener();

        setContentView(R.layout.activity_main);

        menu = (SatelliteMenu) findViewById(R.id.main_menu);
//		  Set from XML, possible to programmatically set
//        float distance = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 170, getResources().getDisplayMetrics());
//        menu.setSatelliteDistance((int) distance);
//        menu.setExpandDuration(500);
//        menu.setCloseItemsOnClick(false);
//        menu.setTotalSpacingDegree(60);
        List<SatelliteMenuItem> items = new ArrayList<>();
        items.add(new SatelliteMenuItem(4, R.drawable.sat_item));
        items.add(new SatelliteMenuItem(3, R.drawable.sat_item));
        items.add(new SatelliteMenuItem(2, R.drawable.sat_item));
        items.add(new SatelliteMenuItem(1, R.drawable.sat_item));
        menu.addItems(items);
        menu.setOnItemClickedListener(new SatelliteMenu.SateliteClickedListener() {
            public void eventOccured(int id) {
                Toast.makeText(MainActivity.this,id+"",Toast.LENGTH_SHORT).show();
            }
        });

        viewActionsContentView = (ActionsContentView) findViewById(R.id.actionsContentView);
        //滑动方式设置
        viewActionsContentView.setSwipingType(ActionsContentView.SWIPING_ALL);

        //菜单列表
        final ListView viewActionsList = (ListView) findViewById(R.id.actions);
        //菜单列表适配器
        final ActionsAdapter actionsAdapter = new ActionsAdapter(this);
        viewActionsList.setAdapter(actionsAdapter);
        //菜单项点击
        viewActionsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position,long flags) {
                //获取菜单项链接
                final Uri uri = actionsAdapter.getItem(position);
                if (currentUri.equals(uri)) {
                    startActivity(new Intent(getBaseContext(), MainActivity.class));
                    return;
                }
                //切换到其他fragment
                updateContent(uri);
                viewActionsContentView.showContent();
            }
        });
        if (savedInstanceState != null) {
            currentUri = Uri.parse(savedInstanceState.getString(STATE_URI));
            currentContentFragmentTag = savedInstanceState.getString(STATE_FRAGMENT_TAG);
        }
        updateContent(currentUri);
    }

    public void test(View view){
        if(viewActionsContentView.getActionsSpacingWidth()==0){
            setWidth(64);
            menu.setVisibility(View.GONE);
            return;
        }
        setWidth(0);
        menu.setVisibility(View.VISIBLE);
    }
    private void setWidth(int num){
        viewActionsContentView.setActionsSpacingWidth(num);
    }
    /**
     * 调用改方法打开新的窗口
     * @param uri 窗口标示
     */
    public void updateContent(Uri uri) {
        final Fragment fragment;
        final String tag;

        final FragmentManager fm = getSupportFragmentManager();
        final FragmentTransaction tr = fm.beginTransaction();

        if (!currentUri.equals(uri)) {
            final Fragment currentFragment = fm.findFragmentByTag(currentContentFragmentTag);
            if (currentFragment != null)
                tr.hide(currentFragment);
        }
        //如果为about
        if (FragmentHome.HOME_URI.equals(uri)) {
            tag = FragmentHome.TAG;
            final Fragment foundFragment = fm.findFragmentByTag(tag);
            if (foundFragment != null) {
                fragment = foundFragment;
            } else {
                fragment = new FragmentAbout();
            }
        }else if (FragmentAbout.ABOUT_URI.equals(uri)) {
            tag = FragmentAbout.TAG;
            final Fragment foundFragment = fm.findFragmentByTag(tag);
            if (foundFragment != null) {
                fragment = foundFragment;
            } else {
                fragment = new FragmentAbout();
            }
        }else if (FragmentSandbox.SETTINGS_URI.equals(uri)) {
            tag = FragmentSandbox.TAG;
            final FragmentSandbox foundFragment = (FragmentSandbox) fm.findFragmentByTag(tag);
            if (foundFragment != null) {
                foundFragment.setOnSettingsChangedListener(mSettingsChangedListener);
                fragment = foundFragment;
            } else {
                final FragmentSandbox settingsFragment = new FragmentSandbox();
                settingsFragment.setOnSettingsChangedListener(mSettingsChangedListener);
                fragment = settingsFragment;
            }
        }else if (uri != null) {
            //如果为网页...
            tag = FragmentWebView.TAG;
            final FragmentWebView webViewFragment;
            final Fragment foundFragment = fm.findFragmentByTag(tag);
            if (foundFragment != null) {
                fragment = foundFragment;
                webViewFragment = (FragmentWebView) fragment;
            } else {
                webViewFragment = new FragmentWebView();
                fragment = webViewFragment;
            }
            webViewFragment.setUrl(uri.toString());
        } else {
            return;
        }

        if (fragment.isAdded()) {
            tr.show(fragment);
        } else {
            tr.replace(R.id.content, fragment, tag);
        }
        tr.commit();

        currentUri = uri;
        currentContentFragmentTag = tag;
    }


    private class SettingsChangedListener implements FragmentSandbox.OnSettingsChangedListener {
        private final float mDensity = getResources().getDisplayMetrics().density;
        private final int mAdditionaSpacingWidth = (int) (100 * mDensity);

        @Override
        public void onSettingChanged(int prefId, int value) {
            switch (prefId) {
                case FragmentSandbox.PREF_SPACING_TYPE:
                    final int currentType = viewActionsContentView.getSpacingType();
                    if (currentType == value)
                        return;
                    final int spacingWidth = viewActionsContentView.getSpacingWidth();
                    if (value == ActionsContentView.SPACING_ACTIONS_WIDTH) {
                        viewActionsContentView.setSpacingWidth(spacingWidth + mAdditionaSpacingWidth);
                    } else if (value == ActionsContentView.SPACING_RIGHT_OFFSET) {
                        viewActionsContentView.setSpacingWidth(spacingWidth - mAdditionaSpacingWidth);
                    }
                    viewActionsContentView.setSpacingType(value);
                    return;
                case FragmentSandbox.PREF_SPACING_WIDTH:
                    final int width;
                    if (viewActionsContentView.getSpacingType() == ActionsContentView.SPACING_ACTIONS_WIDTH)
                        width = (int) (value * mDensity) + mAdditionaSpacingWidth;
                    else
                        width = (int) (value * mDensity);
                    viewActionsContentView.setSpacingWidth(width);
                    return;
                case FragmentSandbox.PREF_SPACING_ACTIONS_WIDTH:
                    viewActionsContentView.setActionsSpacingWidth((int) (value * mDensity));
                    return;
                case FragmentSandbox.PREF_SHOW_SHADOW:
                    viewActionsContentView.setShadowVisible(value == 1);
                    return;
                //阴影方式
                case FragmentSandbox.PREF_FADE_TYPE:
                    viewActionsContentView.setFadeType(value);
                    return;
                case FragmentSandbox.PREF_FADE_MAX_VALUE:
                    viewActionsContentView.setFadeValue(value);
                    return;
                //滑动方式
                case FragmentSandbox.PREF_SWIPING_TYPE:
                    viewActionsContentView.setSwipingType(value);
                    return;
                case FragmentSandbox.PREF_SWIPING_EDGE_WIDTH:
                    viewActionsContentView.setSwipingEdgeWidth(value);
                    return;
                case FragmentSandbox.PREF_FLING_DURATION:
                    viewActionsContentView.setFlingDuration(value);
                    return;
                default:
                    break;
            }
        }
    }

}
