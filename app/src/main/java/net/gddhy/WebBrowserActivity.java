package net.gddhy;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.base.BaseActivity;
import ru.playsoftware.j2meloader.config.Config;

import static net.gddhy.OpenJarFileActivity.openJar;

public class WebBrowserActivity extends BaseActivity {

    private static final String SAI_JAVA_STORE  = "http://82.156.23.136/java";

    WebView webView;
    ProgressBar progressBar;
    String url;
    ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_browser);
        Intent intent = getIntent();
        url = intent.getStringExtra("URL");
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("下载中");
        progressDialog.setCancelable(false);
        if(url == null || url.isEmpty()){
            finish();
        } else {
            initView();
            webView.loadUrl(url);
        }
    }

    private void initView() {
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);

        WebSettings webSettings = webView.getSettings();
        //如果访问的页面中要与Javascript交互，则webView必须设置支持Javascript
        webSettings.setJavaScriptEnabled(true);
        //网页在app内打开
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view,@NonNull String url) {
                String type = url.substring(url.lastIndexOf(".")+1).toLowerCase();
                if(type.equals("jar")||type.equals("apk")){
                    progressDialog.show();
                    downFile(url);
                } else {
                    view.loadUrl(url);
                }
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);//加载完网页进度条消失
                    Objects.requireNonNull(getSupportActionBar()).setTitle(view.getTitle());
                } else {
                    progressBar.setVisibility(View.VISIBLE);//开始加载网页时显示进度条
                    progressBar.setProgress(newProgress);//设置进度值
                }
                super.onProgressChanged(view, newProgress);
            }
        });
    }

    /**
     * 实现按下源生返回键，返回到上一个网页的方法，直接复制即可，
     * 此方法为监听返回按键时的处理
     **/
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == keyCode && webView.canGoBack()) {
            if (webView.getUrl().equals(url)) {
                finish();
            } else {
                webView.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
        }
        return super.onOptionsItemSelected(item);
    }

    public static void openBrowser(Context context, String url){
        Intent intent = new Intent(context, WebBrowserActivity.class);
        intent.putExtra("URL", url);
        context.startActivity(intent);
    }

    public static void openBrowser(Context context){
        openBrowser(context,SAI_JAVA_STORE);
    }

    public static void installApk(Activity context, File apk){
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = MyFileProvider.getUriForFile(context, context.getPackageName()+".FileProvider", apk);
        } else {
            uri = Uri.fromFile(apk);
        }
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION| Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            if(!context.getPackageManager().canRequestPackageInstalls()){
                Uri packageUri = Uri.parse("package:" + context.getPackageName());
                Intent intent2 = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,packageUri);
                context.startActivity(intent2);
                Toast.makeText(context,"缺少必要权限",Toast.LENGTH_LONG).show();
                return;
            }
        }
        context.startActivity(intent);
    }

    private void downFile(final String file_Url){
        final File tmpFile = new File(new File( new File(Config.getEmulatorDir()),"Download"),file_Url.substring(file_Url.lastIndexOf("/")+1));
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(tmpFile.exists()){
                        tmpFile.delete();
                    }
                    downLoadFromUrl(file_Url,tmpFile);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            Toast.makeText(WebBrowserActivity.this,"下载完成",Toast.LENGTH_LONG).show();
                            String type = file_Url.substring(file_Url.lastIndexOf(".")+1).toLowerCase();
                            if(type.equals("jar")) {
                                openJar(WebBrowserActivity.this, tmpFile);
                            } else if(type.equals("apk")){
                                //TODO
                                installApk(WebBrowserActivity.this,tmpFile);
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            Toast.makeText(WebBrowserActivity.this,"下载失败",Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }


    /**
     * https://blog.csdn.net/xb12369/article/details/40543649
     * 有修改
     * 从网络Url中下载文件
     * @param urlStr
     * @param saveFile
     * @throws IOException
     */
    public static void  downLoadFromUrl(String urlStr, File saveFile) throws IOException{
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        //设置超时间为3秒
        conn.setConnectTimeout(3*1000);
        //防止屏蔽程序抓取而返回403错误
        conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");

        //得到输入流
        InputStream inputStream = conn.getInputStream();
        //获取自己数组
        byte[] getData = readInputStream(inputStream);

        //文件保存位置
        if(!saveFile.getParentFile().exists()){
            saveFile.getParentFile().mkdirs();
        }
        FileOutputStream fos = new FileOutputStream(saveFile);
        fos.write(getData);
        if(fos!=null){
            fos.close();
        }
        if(inputStream!=null){
            inputStream.close();
        }

    }



    /**
     * 从输入流中获取字节数组
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static  byte[] readInputStream(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int len = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while((len = inputStream.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        bos.close();
        return bos.toByteArray();
    }
}