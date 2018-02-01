package com.lyy.showwifipass;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private List<WifiInfo> wifiInfoList;
    private ListView mLvWifiInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiInfoList = new ArrayList<>();

        initView();
        obtainPermission();

        String wifiInfo = getWifiInfo();
        handleInfo(wifiInfo);
        showWifiInfo();

    }

    //解析获取到的wifi信息
    private void handleInfo(String info) {
        Pattern network = Pattern.compile("network=\\{([^\\}]+)\\}", Pattern.DOTALL);
        Matcher networkMatcher = network.matcher(info);
        WifiInfo wifiInfo;
        while (networkMatcher.find()) {
            String networkBlock = networkMatcher.group();
            Pattern ssid = Pattern.compile("ssid=\"([^\"]+)\"");
            Matcher ssidMatcher = ssid.matcher(networkBlock);
            if (ssidMatcher.find()) {
                wifiInfo = new WifiInfo();
                wifiInfo.setSsid(ssidMatcher.group(1));
                Pattern psk = Pattern.compile("psk=\"([^\"]+)\"");
                Matcher pskMatcher = psk.matcher(networkBlock);
                if (pskMatcher.find()) {
                    wifiInfo.setPsk(pskMatcher.group(1));
                } else {
                    wifiInfo.setPsk(getString(R.string.empty_password));
                }
                wifiInfoList.add(wifiInfo);
                // 列表倒序
                Collections.reverse(wifiInfoList);
            }
        }
    }

    private void showWifiInfo() {
        MyAdapter myAdapter = new MyAdapter();
        mLvWifiInfo.setAdapter(myAdapter);
        myAdapter.notifyDataSetChanged();
        mLvWifiInfo.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                ClipboardManager cmb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                assert cmb != null;
                cmb.setPrimaryClip(ClipData.newPlainText(getString(R.string.item_pasword_hint), wifiInfoList.get(position)
                        .getPsk()));
                Toast.makeText(getApplicationContext(), R.string.copy_success, Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    //获取读写权限
    private void obtainPermission() {
        RxPermissions rxPermissions = new RxPermissions(MainActivity.this);
        rxPermissions
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe(granted -> {
                    if (granted) {
                        Toast.makeText(MainActivity.this, "同意权限", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "拒绝权限", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    //获取Wifi信息
    private String getWifiInfo() {
        StringBuilder wifiConf = new StringBuilder();
        Process process = null;
        DataOutputStream dataOutputStream = null;
        DataInputStream dataInputStream = null;
        try {
            // 获取 root 环境
            process = Runtime.getRuntime().exec("su");
            dataOutputStream = new DataOutputStream(process.getOutputStream());
            dataInputStream = new DataInputStream(process.getInputStream());
            // cat file 打印文件的内容
            // 获取 /data/misc/wifi/wpa_supplicant.conf 数据，
            dataOutputStream.writeBytes("cat /data/misc/wifi/wpa_supplicant.conf\n");
            dataOutputStream.writeBytes("exit\n");
            dataOutputStream.flush();
            InputStreamReader inputStreamReader = new InputStreamReader(dataInputStream, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line = null;
            // 保存数据，这里都是最基本的 IO 操作，不做过多介绍
            while ((line = bufferedReader.readLine()) != null) {
                wifiConf.append(line);
            }
            bufferedReader.close();
            inputStreamReader.close();
            process.waitFor(); // 线程等待
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (dataOutputStream != null) {
                    dataOutputStream.close();
                }
                if (dataInputStream != null) {
                    dataInputStream.close();
                }
                assert process != null;
                process.destroy(); // 线程销毁
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return wifiConf.toString();
    }

    private void initView() {
        mLvWifiInfo = (ListView) findViewById(R.id.lv_wifi_info);
    }

    class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return wifiInfoList == null ? 0 : wifiInfoList.size();
        }

        @Override
        public Object getItem(int position) {
            return wifiInfoList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @SuppressLint("SetTextI18n")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_info, null);
                holder.name = (TextView) convertView.findViewById(R.id.tv_wifi_ssid);
                holder.password = (TextView) convertView.findViewById(R.id.tv_wifi_psw);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            WifiInfo wifi = (WifiInfo) getItem(position);

            holder.name.setText(wifi.getSsid());
            holder.password.setText("密码：" + wifi.getPsk());

            return convertView;
        }

        class ViewHolder {
            TextView name;
            TextView password;
        }
    }
}
