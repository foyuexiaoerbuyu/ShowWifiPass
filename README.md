# ShowWifiPass
一个简单的wifi密码查看器
# 前言
最近在学习安卓root方面的知识，结合着网上的一些案例做了一个利用root权限的获取手机wifi信息的App，软件截图如下：
![APP截图](http://upload-images.jianshu.io/upload_images/5666077-975e792a2d2aa4a4.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

# 制作思路
1. 获取root权限
2. 找到手机里存放wifi密码的地方
3. 将文件读取出来并保存
4. 显示数据

其实重点就在第一步和第二步，安卓系统中存放wifi信息的文件在``` /data/misc/wifi/wpa_supplicant.conf ```目录下，所以只需要读取到该文件里的数据就可以了。对于root权限的介绍，可以参考[慕课网的视频](https://www.imooc.com/learn/126)，在这里采用的方式是用命令行的方式，获取 /data/misc/wifi/wpa_supplicant.conf 文件的数据；
> 这里要先声明一点，Android 上你获取到root权限，只是代表你可以使用 su 了，你依然无法直接读取 /data/misc/wifi/ 这个路径的，你想要做的一切“非法”操作，都必须通过 su 来完成，也就是通过 shell 命令。
关于 shell 的方法，有个工具类，挺好用。
[ShellUtils.java](https://github.com/YueYongDev/ShowWifiPass/blob/master/app/src/main/java/com/lyy/showwifipass/ShellUtils.java)

# 开始制作
### 1.获取root权限
每个手机都不一样，喜欢刷机的朋友会比较清楚，得靠自己，不多说。
### 2.用命令行的方式，获取存放wifi信息的文件
直接上代码
```
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
```
这样/data/misc/wifi/wpa_supplicant.conf 文件的内容就被保存下来了，接下来只需要解析获取到的内容就可以了。
### 3.解析文本内容
```
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
```
### 4.将结果用ListView显示
这里就不多说了

> 总结一下，其实就是通过shell获取到 /data/misc/wifi/wpa_supplicant.conf 文件的内容，拿到数据之后，在进行相应的操作就可以了。

这里是我的源码地址[ShowWifiPass](https://github.com/YueYongDev/ShowWifiPass)
