package com.sjl.wifi.widget;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.sjl.wifi.R;
import com.sjl.wifi.util.WifiHelper;


/**
 * song
 */

public class WifiLinkDialog extends Dialog implements View.OnClickListener {


    private TextView text_name;

    private EditText password_edit;

    private Button cancel_button;

    private Button cofirm_button;

    private String text_nameString = null;

    private String capabilities;

    private Context mContext;

    public WifiLinkDialog(@NonNull Context context,String text_nameString, String capabilities) {
        super(context, R.style.dialog_pwd);
        this.text_nameString = text_nameString;
        this.capabilities = capabilities;

        mContext = context;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = LayoutInflater.from(mContext).inflate(R.layout.setting_wifi_link_dialog, null);
        setContentView(view);
        initView(view);
        text_name.setText(text_nameString);
        initListener();
    }


    private void initListener() {
        cancel_button.setOnClickListener(this);
        cofirm_button.setOnClickListener(this);
    }

    private void initView(View view) {
        text_name = (TextView) view.findViewById(R.id.wifi_title);
        password_edit = (EditText) view.findViewById(R.id.password_edit);
        cancel_button = (Button) view.findViewById(R.id.cancel_button);
        cofirm_button = (Button) view.findViewById(R.id.cofirm_button);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cofirm_button: {
                String pwd = password_edit.getText().toString();
                WifiHelper wifiHelper = new WifiHelper(getContext());
                wifiHelper.connectWifi(text_nameString, pwd, capabilities);
                dismiss();
                break;
            }
            case R.id.cancel_button: {
                dismiss();
                break;
            }
        }
    }
}
