package com.anxpp.blog.stickylistheaders;

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.ListAdapter;

/**
 * A {@link ListAdapter} which wraps a {@link StickyListHeadersAdapter} and
 * automatically handles wrapping the result of
 * {@link StickyListHeadersAdapter#getView(int, View, ViewGroup)}
 * and
 * {@link StickyListHeadersAdapter#getHeaderView(int, View, ViewGroup)}
 * appropriately.
 *
 * @author Jake Wharton (jakewharton@gmail.com)
 */
class AdapterWrapper extends BaseAdapter implements StickyListHeadersAdapter {

	interface OnHeaderClickListener {
		void onHeaderClick(View header, int itemPosition, long headerId);
	}

	StickyListHeadersAdapter stickyListHeadersAdapter;
	private final List<View> mHeaderCache = new LinkedList<>();
	private final Context mContext;
	private Drawable mDivider;
	private int mDividerHeight;
	private OnHeaderClickListener mOnHeaderClickListener;

	AdapterWrapper(Context context,
			StickyListHeadersAdapter delegate) {
		this.mContext = context;
		this.stickyListHeadersAdapter = delegate;
		DataSetObserver mDataSetObserver = new DataSetObserver() {

			@Override
			public void onInvalidated() {
				mHeaderCache.clear();
				AdapterWrapper.super.notifyDataSetInvalidated();
			}

			@Override
			public void onChanged() {
				AdapterWrapper.super.notifyDataSetChanged();
			}
		};
		delegate.registerDataSetObserver(mDataSetObserver);
	}

	void setDivider(Drawable divider, int dividerHeight) {
		this.mDivider = divider;
		this.mDividerHeight = dividerHeight;
		notifyDataSetChanged();
	}

	@Override
	public boolean areAllItemsEnabled() {
		return stickyListHeadersAdapter.areAllItemsEnabled();
	}

	@Override
	public boolean isEnabled(int position) {
		return stickyListHeadersAdapter.isEnabled(position);
	}

	@Override
	public int getCount() {
		return stickyListHeadersAdapter.getCount();
	}

	@Override
	public Object getItem(int position) {
		return stickyListHeadersAdapter.getItem(position);
	}

	@Override
	public long getItemId(int position) {
		return stickyListHeadersAdapter.getItemId(position);
	}

	@Override
	public boolean hasStableIds() {
		return stickyListHeadersAdapter.hasStableIds();
	}

	@Override
	public int getItemViewType(int position) {
		return stickyListHeadersAdapter.getItemViewType(position);
	}

	@Override
	public int getViewTypeCount() {
		return stickyListHeadersAdapter.getViewTypeCount();
	}

	@Override
	public boolean isEmpty() {
		return stickyListHeadersAdapter.isEmpty();
	}

	/**
	 * Will recycle header from {@link WrapperView} if it exists
	 */
	private void recycleHeaderIfExists(WrapperView wv) {
		View header = wv.mHeader;
		if (header != null) {
			// reset the headers visibility when adding it to the cache
			header.setVisibility(View.VISIBLE);
			mHeaderCache.add(header);
		}
	}

	/**
	 * Get a header view. This optionally pulls a header from the supplied
	 * {@link WrapperView} and will also recycle the divider if it exists.
	 */
	private View configureHeader(WrapperView wv, final int position) {
		View header = wv.mHeader == null ? popHeader() : wv.mHeader;
		header = stickyListHeadersAdapter.getHeaderView(position, header, wv);
		if (header == null) {
			throw new NullPointerException("Header view must not be null.");
		}
		//if the header isn't clickable, the listselector will be drawn on top of the header
		header.setClickable(true);
		header.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(mOnHeaderClickListener != null){
					long headerId = stickyListHeadersAdapter.getHeaderId(position);
					mOnHeaderClickListener.onHeaderClick(v, position, headerId);
				}
			}
		});
		return header;
	}

	private View popHeader() {
		if(mHeaderCache.size() > 0) {
			return mHeaderCache.remove(0);
		}
		return null;
	}

	/** Returns {@code true} if the previous position has the same header ID. */
	private boolean previousPositionHasSameHeader(int position) {
		return position != 0
				&& stickyListHeadersAdapter.getHeaderId(position) == stickyListHeadersAdapter
						.getHeaderId(position - 1);
	}

	@Override
	public WrapperView getView(int position, View convertView, ViewGroup parent) {
		WrapperView wv = (convertView == null) ? new WrapperView(mContext) : (WrapperView) convertView;
		View item = stickyListHeadersAdapter.getView(position, wv.mItem, parent);
		View header = null;
		if (previousPositionHasSameHeader(position)) {
			recycleHeaderIfExists(wv);
		} else {
			header = configureHeader(wv, position);
		}
		if((item instanceof Checkable) && !(wv instanceof CheckableWrapperView)) {
			// Need to create Checkable subclass of WrapperView for ListView to work correctly
			wv = new CheckableWrapperView(mContext);
		} else if(!(item instanceof Checkable) && (wv instanceof CheckableWrapperView)) {
			wv = new WrapperView(mContext);
		}
		wv.update(item, header, mDivider, mDividerHeight);
		return wv;
	}

	public void setOnHeaderClickListener(OnHeaderClickListener onHeaderClickListener){
		this.mOnHeaderClickListener = onHeaderClickListener;
	}

	@Override
	public boolean equals(Object o) {
		return stickyListHeadersAdapter.equals(o);
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		return ((BaseAdapter) stickyListHeadersAdapter).getDropDownView(position, convertView, parent);
	}

	@Override
	public int hashCode() {
		return stickyListHeadersAdapter.hashCode();
	}

	@Override
	public void notifyDataSetChanged() {
		((BaseAdapter) stickyListHeadersAdapter).notifyDataSetChanged();
	}

	@Override
	public void notifyDataSetInvalidated() {
		((BaseAdapter) stickyListHeadersAdapter).notifyDataSetInvalidated();
	}

	@Override
	public String toString() {
		return stickyListHeadersAdapter.toString();
	}

	@Override
	public View getHeaderView(int position, View convertView, ViewGroup parent) {
		return stickyListHeadersAdapter.getHeaderView(position, convertView, parent);
	}

	@Override
	public long getHeaderId(int position) {
		return stickyListHeadersAdapter.getHeaderId(position);
	}

}
