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

import com.anxpp.blog.fragment.About;
import com.anxpp.blog.fragment.Blog;
import com.anxpp.blog.fragment.Home;
import com.anxpp.blog.fragment.Setting;
import com.anxpp.blog.fragment.Web;
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
    private Uri currentUri = Home.HOME_URI;
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
        items.add(new SatelliteMenuItem(4, R.drawable.ic_action_setting));
        items.add(new SatelliteMenuItem(3, R.drawable.ic_action_about));
        items.add(new SatelliteMenuItem(2, R.drawable.ic_action_blog));
        items.add(new SatelliteMenuItem(1, R.drawable.ic_action_home));
        final Uri[] uris= {
                Home.HOME_URI,
                Blog.BLOG_URI,
                About.ABOUT_URI,
                Setting.SETTINGS_URI};
        menu.addItems(items);
        menu.setOnItemClickedListener(new SatelliteMenu.SateliteClickedListener() {
            public void eventOccured(int id) {
                updateContent(uris[id-1]);
            }
        });

        viewActionsContentView = (ActionsContentView) findViewById(R.id.actionsContentView);
        //滑动方式设置
        viewActionsContentView.setSwipingType(ActionsContentView.SWIPING_ALL);

        viewActionsContentView.setSpacingWidth(viewActionsContentView.getSpacingWidth() + (int) (100 * getResources().getDisplayMetrics().density));

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
//                    startActivity(new Intent(getBaseContext(), MainActivity.class));
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
        startActivity(new Intent(MainActivity.this, InitialActivity.class));
//        swapMenu();
    }
    private int oldWidth = 0;
    //false表示当前为侧边栏菜单  true表示当前为扇形菜单
    private void swapMenu(boolean menuMode){
//        Toast.makeText(this,"切换到"+(menuMode?"主页":"其他页"),Toast.LENGTH_SHORT).show();
        int width = viewActionsContentView.getActionsSpacingWidth();
        if(menuMode){
            setWidth(oldWidth);
            menu.setVisibility(View.GONE);
            return;
        }
        oldWidth = width;
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
        tr.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

        if (!currentUri.equals(uri)) {
            //从其他页切换到主页
            if (uri.equals(Home.HOME_URI))
                swapMenu(true);
            final Fragment currentFragment = fm.findFragmentByTag(currentContentFragmentTag);
            if (currentFragment != null)
                tr.hide(currentFragment);
        }
        //如果为主页
        if (Home.HOME_URI.equals(uri)) {
            tag = Home.TAG;
            final Fragment foundFragment = fm.findFragmentByTag(tag);
            if (foundFragment != null) {
                fragment = foundFragment;
            } else {
                fragment = new Home();
            }
        }else if (About.ABOUT_URI.equals(uri)) {
            tag = About.TAG;
            final Fragment foundFragment = fm.findFragmentByTag(tag);
            if (foundFragment != null) {
                fragment = foundFragment;
            } else {
                fragment = new About();
            }
        }else if (Blog.BLOG_URI.equals(uri)) {
            tag = Blog.TAG;
            final Fragment foundFragment = fm.findFragmentByTag(tag);
            if (foundFragment != null) {
                fragment = foundFragment;
            } else {
                fragment = new Blog();
            }
        }else if (Setting.SETTINGS_URI.equals(uri)) {
            tag = Setting.TAG;
            final Setting foundFragment = (Setting) fm.findFragmentByTag(tag);
            if (foundFragment != null) {
                foundFragment.setOnSettingsChangedListener(mSettingsChangedListener);
                fragment = foundFragment;
            } else {
                final Setting settingsFragment = new Setting();
                settingsFragment.setOnSettingsChangedListener(mSettingsChangedListener);
                fragment = settingsFragment;
            }
        }else if (uri != null) {
            //如果为网页...
            tag = Web.TAG;
            final Web webViewFragment;
            final Fragment foundFragment = fm.findFragmentByTag(tag);
            if (foundFragment != null) {
                fragment = foundFragment;
                webViewFragment = (Web) fragment;
            } else {
                webViewFragment = new Web();
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

        //从主页切换到其他页
        if(!currentUri.equals(uri)&&currentUri.equals(Home.HOME_URI))
            swapMenu(false);

        currentUri = uri;
        currentContentFragmentTag = tag;
    }


    private class SettingsChangedListener implements Setting.OnSettingsChangedListener {
        private final float mDensity = getResources().getDisplayMetrics().density;
        private final int mAdditionaSpacingWidth = (int) (100 * mDensity);

        @Override
        public void onSettingChanged(int prefId, int value) {
            switch (prefId) {
                case Setting.PREF_SPACING_TYPE:
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
                case Setting.PREF_SPACING_WIDTH:
                    final int width;
                    if (viewActionsContentView.getSpacingType() == ActionsContentView.SPACING_ACTIONS_WIDTH)
                        width = (int) (value * mDensity) + mAdditionaSpacingWidth;
                    else
                        width = (int) (value * mDensity);
                    viewActionsContentView.setSpacingWidth(width);
                    return;
                case Setting.PREF_SPACING_ACTIONS_WIDTH:
                    viewActionsContentView.setActionsSpacingWidth((int) (value * mDensity));
                    return;
                case Setting.PREF_SHOW_SHADOW:
                    viewActionsContentView.setShadowVisible(value == 1);
                    return;
                //阴影方式
                case Setting.PREF_FADE_TYPE:
                    viewActionsContentView.setFadeType(value);
                    return;
                case Setting.PREF_FADE_MAX_VALUE:
                    viewActionsContentView.setFadeValue(value);
                    return;
                //滑动方式
                case Setting.PREF_SWIPING_TYPE:
                    viewActionsContentView.setSwipingType(value);
                    return;
                case Setting.PREF_SWIPING_EDGE_WIDTH:
                    viewActionsContentView.setSwipingEdgeWidth(value);
                    return;
                case Setting.PREF_FLING_DURATION:
                    viewActionsContentView.setFlingDuration(value);
                    return;
                default:
                    break;
            }
        }
    }

}
