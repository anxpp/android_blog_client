package com.anxpp.blog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SectionIndexer;
import android.widget.TextView;
import com.anxpp.blog.stickylistheaders.StickyListHeadersAdapter;

import java.util.ArrayList;

/**
 * 适配器
 */
public class TestBaseAdapter extends BaseAdapter implements
        StickyListHeadersAdapter, SectionIndexer {

    //上下文
    private final Context mContext;
    //列表要填充的内容
    private String[] mCountries;
    //标记，此处为所有不同的首字母第一次出现的位置
    private int[] mSectionIndices;
    //证书 所有首字母
    private Character[] mSectionLetters;
    /**
     * 作用类似于findViewById()。
     * 不同点是LayoutInflater是用来找res/layout/下的xml布局文件，并且实例化
     * 而findViewById()是找xml布局文件下的具体widget控件(如Button、TextView等)。
     * 具体作用：
     * 1、对于一个没有被载入或者想要动态载入的界面，都需要使用LayoutInflater.inflate()来载入；
     * 2、对于一个已经载入的界面，就可以使用Activity.findViewById()方法来获得其中的界面元素。
     * LayoutInflater 是一个抽象类，在文档中如下声明：
     * public abstract class LayoutInflater extends Object
     * 获得 LayoutInflater 实例的三种方式
     * 1. LayoutInflater inflater = getLayoutInflater();//调用Activity的getLayoutInflater()
     * 2. LayoutInflater inflater = LayoutInflater.from(context);
     * 3. LayoutInflater inflater =  (LayoutInflater)context.getSystemService
     * (Context.LAYOUT_INFLATER_SERVICE);
     */
    private LayoutInflater mInflater;

    public TestBaseAdapter(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mCountries = context.getResources().getStringArray(R.array.countries);
        mSectionIndices = getSectionIndices();
        mSectionLetters = getSectionLetters();
    }

    //每个首字母出现的位置
    private int[] getSectionIndices() {
        ArrayList<Integer> sectionIndices = new ArrayList<>();
        //获取首字母
        char lastFirstChar = mCountries[0].charAt(0);
        sectionIndices.add(0);
        for (int i = 1; i < mCountries.length; i++) {
            if (mCountries[i].charAt(0) != lastFirstChar) {
                lastFirstChar = mCountries[i].charAt(0);
                sectionIndices.add(i);
            }
        }
        int[] sections = new int[sectionIndices.size()];
        for (int i = 0; i < sectionIndices.size(); i++) {
            sections[i] = sectionIndices.get(i);
        }
        return sections;
    }

    //获取所有首字母
    private Character[] getSectionLetters() {
        Character[] letters = new Character[mSectionIndices.length];
        for (int i = 0; i < mSectionIndices.length; i++) {
            letters[i] = mCountries[mSectionIndices[i]].charAt(0);
        }
        return letters;
    }

    @Override
    public int getCount() {
        return mCountries.length;
    }

    @Override
    public Object getItem(int position) {
        return mCountries[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * 是配置
     * @param position 1
     * @param convertView 1
     * @param parent 1
     * @return 1
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //ViewHolder通常出现在适配器里，为的是listview滚动的时候快速设置值，而不必每次都重新创建很多对象，从而提升性能
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.test_list_item_layout, parent, false);
            holder.text = (TextView) convertView.findViewById(R.id.text);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        //设置值
        holder.text.setText(mCountries[position]);
        return convertView;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        HeaderViewHolder holder;

        if (convertView == null) {
            holder = new HeaderViewHolder();
            convertView = mInflater.inflate(R.layout.header, parent, false);
            holder.text = (TextView) convertView.findViewById(R.id.text1);
            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder) convertView.getTag();
        }

        // set header text as first char in name
        CharSequence headerChar = mCountries[position].subSequence(0, 1);
        holder.text.setText(headerChar);

        return convertView;
    }

    /**
     * Remember that these have to be static, postion=1 should always return
     * the same Id that is.
     */
    @Override
    public long getHeaderId(int position) {
        // return the first character of the country as ID because this is what
        // headers are based upon
        return mCountries[position].subSequence(0, 1).charAt(0);
    }

    @Override
    public int getPositionForSection(int section) {
        if (mSectionIndices.length == 0) {
            return 0;
        }
        
        if (section >= mSectionIndices.length) {
            section = mSectionIndices.length - 1;
        } else if (section < 0) {
            section = 0;
        }
        return mSectionIndices[section];
    }

    @Override
    public int getSectionForPosition(int position) {
        for (int i = 0; i < mSectionIndices.length; i++) {
            if (position < mSectionIndices[i]) {
                return i - 1;
            }
        }
        return mSectionIndices.length - 1;
    }

    @Override
    public Object[] getSections() {
        return mSectionLetters;
    }

    public void clear() {
        mCountries = new String[0];
        mSectionIndices = new int[0];
        mSectionLetters = new Character[0];
        notifyDataSetChanged();
    }

    public void restore() {
        mCountries = mContext.getResources().getStringArray(R.array.countries);
        mSectionIndices = getSectionIndices();
        mSectionLetters = getSectionLetters();
        notifyDataSetChanged();
    }

    class HeaderViewHolder {
        TextView text;
    }

    class ViewHolder {
        TextView text;
    }

}
