package org.zywx.wbpalmstar.plugin.uexqq.util;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Utils {

    public static String copyImage(Context context, String url) {
        String resPath = url;// 获取的为assets路径
        InputStream imageInputStream = null;
        OutputStream out = null;
        String imagTemPath = url;
        try {
        	if(resPath.startsWith("/data")){
        		imageInputStream = new FileInputStream(new File(resPath));
        	}else{
        		imageInputStream = context.getResources().getAssets()
                        .open(resPath);
        	}
            String sdPath = "";// 为sd卡绝对路径
            boolean sdCardExist = Environment.getExternalStorageState().equals(
                    android.os.Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
            if (sdCardExist) {
                sdPath = Environment.getExternalStorageDirectory() + "";// 获取目录
            } else {
                Toast.makeText(context, "sd卡不存在，请查看", Toast.LENGTH_SHORT)
                        .show();
            }
            imagTemPath = sdPath + File.separator + resPath;
            File file = new File(imagTemPath);
            if(!file.getParentFile().exists()){
                file.getParentFile().mkdirs();
            }
            if(file.exists()){
                file.delete();
            }
            out = new FileOutputStream(file);
            int count = 0;
            byte[] buff = new byte[1024];
            while ((count = imageInputStream.read(buff)) != -1) {
                out.write(buff, 0, count);
            }
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(imageInputStream != null) imageInputStream.close();
                if(out != null) out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return imagTemPath;
    }
    
    public static String httpGet(String urlString) {
		BufferedInputStream bis = null;
		HttpURLConnection httpConn = null;
		try {
			// 创建url对象
			URL urlObj = new URL(urlString);
			// 创建HttpURLConnection对象，通过这个对象打开跟远程服务器之间的连接
			httpConn = (HttpURLConnection) urlObj.openConnection();

			httpConn.setDoInput(true);
			httpConn.setRequestMethod("GET");
			httpConn.setConnectTimeout(5000);
			httpConn.connect();

			// 判断跟服务器的连接状态。如果是200，则说明连接正常，服务器有响应
			if (httpConn.getResponseCode() == 200) {
				// 服务器有响应后，会将访问的url页面中的内容放进inputStream中，使用httpConn就可以获取到这个字节流
				bis = new BufferedInputStream(httpConn.getInputStream());
				return streamToString(bis,"utf-8");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				// 对流对象进行关闭，对Http连接对象进行关闭。以便释放资源。
				bis.close();
				httpConn.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
    
    public static String streamToString(InputStream is, String charsetName) {
		BufferedInputStream bis = new BufferedInputStream(is);
		StringBuilder sb = new StringBuilder();
		int c = 0;
		byte[] buffer = new byte[8 * 1024];
		try {
			while ((c = bis.read(buffer)) != -1) {
				sb.append(new String(buffer, charsetName));
			}
			return sb.toString();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				bis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
    
    

}
