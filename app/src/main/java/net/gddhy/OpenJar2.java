package net.gddhy;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;


public class OpenJar2 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/java-archive");
        startActivityForResult(intent,10086);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == 10086 && resultCode == RESULT_OK){
            Uri uri = data.getData();
            if(uri != null) {
                File f = OpenJarFileActivity.saveFileFromSAF(this, uri);
                OpenJarFileActivity.openJar(this,f);
            } else{
                Toast.makeText(this,"文件获取失败",Toast.LENGTH_LONG).show();
            }
        }
        finish();
        super.onActivityResult(requestCode, resultCode, data);
    }
}