package com.anxpp.blog.stickylistheaders;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

/**
 * 封装的带标题的ListView
 */
class WrapperViewList extends ListView {

	//生命周期监听
	interface LifeCycleListener {
		// Canvas类就是表示一块画布，你可以在上面画你想画的东西。
		// 当然，你还可以设置画布的属性，如画布的颜色/尺寸等
		void onDispatchDrawOccurred(Canvas canvas);
	}

	private LifeCycleListener mLifeCycleListener;
	private List<View> mFooterViews;
	private int mTopClippingLength;
	//Rect类主要用于表示坐标系中的一块矩形区域，并可以对其做一些简单操作
	//反射失败时使用
	private Rect mSelectorRect = new Rect();
	private Field mSelectorPositionField;
	private boolean mClippingToPadding = true;
	private boolean mBlockLayoutChildren = false;

	public WrapperViewList(Context context) {
		super(context);
		// 用反射来改变列表的大小/位置
		// selector so it does not come under/over the header
		try {
			//AbsListView用于实现条目的虚拟列表的基类. 这里的列表没有空间的定义
			//getDeclaredField获取一个类的所有字段
			Field selectorRectField = AbsListView.class.getDeclaredField("mSelectorRect");
			//setAccessible(true)可以访问private域
			//即修改成员访问权限
			selectorRectField.setAccessible(true);
			mSelectorRect = (Rect) selectorRectField.get(this);

			//1.4以上的
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				mSelectorPositionField = AbsListView.class.getDeclaredField("mSelectorPosition");
				mSelectorPositionField.setAccessible(true);
			}
		} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	//执行点击
	@Override
	public boolean performItemClick(View view, int position, long id) {
		//instanceof 运算符是用来在运行时指出对象是否是特定类的一个实例
		if (view instanceof WrapperView) {
			view = ((WrapperView) view).mItem;
		}
		return super.performItemClick(view, position, id);
	}

	//选择的区域
	private void positionSelectorRect() {
		if (!mSelectorRect.isEmpty()) {
			int selectorPosition = getSelectorPosition();
			if (selectorPosition >= 0) {
				int firstVisibleItem = getFixedFirstVisibleItem();
				View view = getChildAt(selectorPosition - firstVisibleItem);
				if (view instanceof WrapperView) {
					WrapperView wrapper = ((WrapperView) view);
					mSelectorRect.top = wrapper.getTop() + wrapper.mItemTop;
				}
			}
		}
	}

	//获取选择的位置
	private int getSelectorPosition() {
		if (mSelectorPositionField == null) { //不适用于所有android
			//当前版本有这个变量时
			for (int i = 0; i < getChildCount(); i++) {
				if (getChildAt(i).getBottom() == mSelectorRect.bottom) {
					return i + getFixedFirstVisibleItem();
				}
			}
		} else {
			try {
				return mSelectorPositionField.getInt(this);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return -1;
	}

	//绘制自己的孩子通过dispatchDraw(canvas)实现
	@Override
	protected void dispatchDraw(Canvas canvas) {
		positionSelectorRect();
		if (mTopClippingLength != 0) {
			canvas.save();
			Rect clipping = canvas.getClipBounds();
			clipping.top = mTopClippingLength;
			canvas.clipRect(clipping);
			super.dispatchDraw(canvas);
			canvas.restore();
		} else {
			super.dispatchDraw(canvas);
		}
		mLifeCycleListener.onDispatchDrawOccurred(canvas);
	}

	void setLifeCycleListener(LifeCycleListener lifeCycleListener) {
		mLifeCycleListener = lifeCycleListener;
	}

	//添加最后的布局
	@Override
	public void addFooterView(View v) {
		super.addFooterView(v);
		addInternalFooterView(v);
	}

	//添加最后的布局
	@Override
	public void addFooterView(View v, Object data, boolean isSelectable) {
		super.addFooterView(v, data, isSelectable);
		addInternalFooterView(v);
	}

	private void addInternalFooterView(View v) {
		if (mFooterViews == null) {
			mFooterViews = new ArrayList<>();
		}
		mFooterViews.add(v);
	}

	@Override
	public boolean removeFooterView(View v) {
		if (super.removeFooterView(v)) {
			mFooterViews.remove(v);
			return true;
		}
		return false;
	}

	boolean containsFooterView(View v) {
		// list.contains(o)，系统会对list中的每个元素e调用o.equals(e)方法
		// 加入list中有n个元素，那么会调用n次o.equals(e)
		// 只要有一次o.equals(e)返回了true，那么list.contains(o)返回true，否则返回false
		return mFooterViews != null && mFooterViews.contains(v);
	}

	void setTopClippingLength(int topClipping) {
		mTopClippingLength = topClipping;
	}

	int getFixedFirstVisibleItem() {
		//ListView.getFirstVisiblePosition()获取当前可见的第一个Item的position
		int firstVisibleItem = getFirstVisiblePosition();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			return firstVisibleItem;
		}
		// first getFirstVisiblePosition() reports items
		// outside the view sometimes on old versions of android
		for (int i = 0; i < getChildCount(); i++) {
			if (getChildAt(i).getBottom() >= 0) {
				firstVisibleItem += i;
				break;
			}
		}
		// work around to fix bug with firstVisibleItem being to high
		// because list view does not take clipToPadding=false into account
		// on old versions of android
		if (!mClippingToPadding && getPaddingTop() > 0 && firstVisibleItem > 0) {
			if (getChildAt(0).getTop() > 0) {
				firstVisibleItem -= 1;
			}
		}
		return firstVisibleItem;
	}

	@Override
	public void setClipToPadding(boolean clipToPadding) {
		mClippingToPadding = clipToPadding;
		super.setClipToPadding(clipToPadding);
	}

	public void setBlockLayoutChildren(boolean block) {
		mBlockLayoutChildren = block;
	}

	//ListView layoutChildren()对ListView进行了重新布局，主要有四个步骤：
	//1、保存当前状态，包括焦点，选中项，仍显示的Item等
	//2、移除所有Item
	//3、根据情况重新填充Item
	//4、其他处理
	@Override
	protected void layoutChildren() {
		if (!mBlockLayoutChildren) {
			super.layoutChildren();
		}
	}
}
