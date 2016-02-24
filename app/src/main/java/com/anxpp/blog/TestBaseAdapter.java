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
 * ������
 */
public class TestBaseAdapter extends BaseAdapter implements
        StickyListHeadersAdapter, SectionIndexer {

    //������
    private final Context mContext;
    //�б�Ҫ��������
    private String[] mCountries;
    //��ǣ��˴�Ϊ���в�ͬ������ĸ��һ�γ��ֵ�λ��
    private int[] mSectionIndices;
    //֤�� ��������ĸ
    private Character[] mSectionLetters;
    /**
     * ����������findViewById()��
     * ��ͬ����LayoutInflater��������res/layout/�µ�xml�����ļ�������ʵ����
     * ��findViewById()����xml�����ļ��µľ���widget�ؼ�(��Button��TextView��)��
     * �������ã�
     * 1������һ��û�б����������Ҫ��̬����Ľ��棬����Ҫʹ��LayoutInflater.inflate()�����룻
     * 2������һ���Ѿ�����Ľ��棬�Ϳ���ʹ��Activity.findViewById()������������еĽ���Ԫ�ء�
     * LayoutInflater ��һ�������࣬���ĵ�������������
     * public abstract class LayoutInflater extends Object
     * ��� LayoutInflater ʵ�������ַ�ʽ
     * 1. LayoutInflater inflater = getLayoutInflater();//����Activity��getLayoutInflater()
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

    //ÿ������ĸ���ֵ�λ��
    private int[] getSectionIndices() {
        ArrayList<Integer> sectionIndices = new ArrayList<>();
        //��ȡ����ĸ
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

    //��ȡ��������ĸ
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
     * ������
     * @param position 1
     * @param convertView 1
     * @param parent 1
     * @return 1
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //ViewHolderͨ���������������Ϊ����listview������ʱ���������ֵ��������ÿ�ζ����´����ܶ���󣬴Ӷ���������
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.test_list_item_layout, parent, false);
            holder.text = (TextView) convertView.findViewById(R.id.text);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        //����ֵ
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
