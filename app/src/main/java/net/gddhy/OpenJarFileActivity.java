package net.gddhy;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import ru.playsoftware.j2meloader.MainActivity;
import ru.playsoftware.j2meloader.config.Config;

public class OpenJarFileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        {
            //TODO
            Intent intent = getIntent();
            File f = null;
            String data = "";
            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                Uri uri = intent.getData();
                data = intent.getDataString();
                try {
                    if(!data.contains("file://")){
                        f = saveFileFromSAF(this, uri);
                        if(f!=null)
                            openJar(this,f);
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
            if (f==null){
                Toast.makeText(OpenJarFileActivity.this,"文件读取失败\n"+data,Toast.LENGTH_LONG).show();
            }
            finish();
        }
    }

    public static File saveFileFromSAF(Context context,Uri safUri){
        //DocumentFile documentFile = DocumentFile.fromSingleUri(context,safUri);
        File dir = new File(Config.getEmulatorDir(),"SAF");
        if(!dir.exists()){
            dir.mkdirs();
        }
        try {
            File save = new File(dir,"tmp.jar");//documentFile.getName());
            if(save.exists()){
                save.delete();
            }
            if(!save.exists()){
                save.createNewFile();
            }
            InputStream in  = context.getContentResolver().openInputStream(safUri);
            FileOutputStream out = new FileOutputStream(save);
            int n = 0;// 每次读取的字节长度
            byte[] bb = new byte[1024];// 存储每次读取的内容
            while ((n = in.read(bb)) != -1) {
                out.write(bb, 0, n);// 将读取的内容，写入到输出流当中
            }
            in.close();
            out.close();
            return save;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void openJar(Context context, File jarFile){
        Intent intent = new Intent();
        intent.setClass(context, MainActivity.class);
        intent.setData(Uri.fromFile(jarFile));
        context.startActivity(intent);
    }
}