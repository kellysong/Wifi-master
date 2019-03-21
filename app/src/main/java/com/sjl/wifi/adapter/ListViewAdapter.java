package com.sjl.wifi.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sjl.wifi.R;

import java.util.List;

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename ListViewAdapter.java
 * @time 2019/3/19 16:16
 * @copyright(C) 2019 song
 */
public class ListViewAdapter extends BaseAdapter {
    private List<String> mList;//数据源
    private LayoutInflater mInflater;//布局装载器对象


    public ListViewAdapter(Context context, List<String> list) {
        mList = list;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.test_list_view_item, parent, false);
            viewHolder.relativeLayout = (RelativeLayout) convertView.findViewById(R.id.rl_content);
            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.iv_wifi);
            viewHolder.title = (TextView) convertView.findViewById(R.id.tv_item_wifi_name);
            viewHolder.content = (TextView) convertView.findViewById(R.id.tv_item_wifi_status);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        String bean = mList.get(position);

        viewHolder.title.setText(position + ":" + bean);
        viewHolder.relativeLayout.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    Log.i("SIMPLE_LOGGER", "有焦点position:" + position);
                    viewHolder.relativeLayout.setBackgroundColor(Color.RED);
                    viewHolder.title.setTextSize(18f);
                } else {
                    Log.i("SIMPLE_LOGGER", "无焦点position:" + position);
                    viewHolder.relativeLayout.setBackgroundColor(Color.WHITE);
                    viewHolder.title.setTextSize(14f);

                }
            }
        });
        return convertView;
    }

    static class ViewHolder {
        public RelativeLayout relativeLayout;
        public ImageView imageView;
        public TextView title;
        public TextView content;
    }
}
