package com.admin.android10store;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private Button btn_pic;
    private ByteArrayOutputStream BitmapByteStream;
    private Button btn_delete;
    private Bitmap bitmap;
    private ContentResolver resolver;
    private Button btn_text;
    private Button btn_delete_text;
    private ContentValues contentValues;
    private Button btn_saf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resolver = getContentResolver();
        contentValues = new ContentValues();

        getBitmap();
        initView();
    }

    private void getBitmap() {
        bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.loading);
        BitmapByteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, BitmapByteStream);
    }

    private void initView() {
        btn_pic = findViewById(R.id.btn_pic);
        btn_pic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                savePic();
            }
        });

        btn_delete = findViewById(R.id.btn_delete);
        btn_delete.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = 30)
            @Override
            public void onClick(View view) {
                deletePic();
            }
        });

        btn_text = findViewById(R.id.btn_text);
        btn_delete_text = findViewById(R.id.btn_delete_text);
        btn_text.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View view) {
                saveText();
            }
        });
        btn_delete_text.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onClick(View view) {
                deleteText();
            }
        });

        btn_saf = findViewById(R.id.btn_saf);
        btn_saf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                safDemo();
            }
        });
    }

    private void safDemo() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");//显示所有类型文件
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 100);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            Uri uri;
            if (data != null) {
                uri = data.getData();
                Log.i(TAG, "onActivityResult---uri: "+uri);
                try {
                    DocumentsContract.deleteDocument(resolver, uri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void deleteText() {
        Cursor cursor = resolver.query(
                //指定要查询的 Uri
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                //指定查询的列
                null,
                //指定查询语句
                MediaStore.Downloads.DISPLAY_NAME + "=?",
                //指定查询参数
                new String[]{"FF.txt"},
                //排序规则
                null);

        // 要删除的图片对应的 Uri, 需要先查询出来
        Uri uri = null;
        // 先获取该图片在数据库中的 id , 然后通过 id 获取 Uri
        if (cursor != null && cursor.moveToFirst()) {
            // 获取第 0 行 _id 所在列的值
            long id = cursor.getLong(
                    // 获取 _id 所在列的索引
                    cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID));
            // 通过 _id 字段获取图片 Uri
            uri = ContentUris.withAppendedId(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL), id);//删除Download文件夹中的文件
            // 关闭游标
            cursor.close();
        }
        Log.i(TAG, "deleteText---uri: " + uri);
        int delete = resolver.delete(uri, null, null);
        Log.i(TAG, "deleteText: " + delete);
    }

    //document存文档类型文件 download可以存任何类型文件
    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void saveText() {
        contentValues.clear();
        // 操作 external.db 数据库
        //获取uri
        Uri uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL);
        // 设置存储路径 , files 数据表中的对应 relative_path 字段在 MediaStore 中以常量形式定义
        contentValues.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + "android10");//保存到Download文件中
//        保存到Document文件中，暂时没找到方法使用MediaStore删除Document文件夹中文档（可以使用SAF框架，删除文档）
//        contentValues.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + File.separator + "android10");
//         设置文件名称
        contentValues.put(MediaStore.Downloads.DISPLAY_NAME, "FF.txt");
        // 设置文件标题, 一般是删除后缀, 可以不设置
        contentValues.put(MediaStore.Downloads.TITLE, "FF");

        // uri 表示操作哪个数据库 , contentValues 表示要插入的数据内容
        Uri insertUri = resolver.insert(uri, contentValues);
        Log.i(TAG, "saveText---insertUri: "+insertUri);
        try {
            OutputStream outputStream = resolver.openOutputStream(insertUri);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            bufferedOutputStream.write("测试安卓10存储文件".getBytes());
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Uri getBitmapUri() {
        contentValues.clear();
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "android10.png");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Android10");
        Uri picUri = resolver.insert(uri, contentValues);//生成uri

        return picUri;
    }

    // MediaStore.Video.Media.EXTERNAL_CONTENT_URI   删除视频的Uri
    // MediaStore.Images.Media.EXTERNAL_CONTENT_URI   删除图片的Uri
    // MediaStore.Audio.Media.EXTERNAL_CONTENT_URI   删除音频的Uri
    @RequiresApi(api = 30)
    private void deletePic() {
        Cursor cursor = resolver.query(
                //指定要查询的 Uri
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                //指定查询的列
                null,
                //指定查询语句
                MediaStore.Images.Media.DISPLAY_NAME + "=?",
                //指定查询参数
                new String[]{"android10.png"},
                //排序规则
                null);

        // 要删除的图片对应的 Uri, 需要先查询出来
        Uri uri = null;
        // 先获取该图片在数据库中的 id , 然后通过 id 获取 Uri
        if (cursor != null && cursor.moveToFirst()) {
            // 获取第 0 行 _id 所在列的值
            long id = cursor.getLong(
                    // 获取 _id 所在列的索引
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
            // 通过 _id 字段获取图片 Uri
            uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
            // 关闭游标
            cursor.close();
        }
        int delete = resolver.delete(uri, null, null);
        Log.i(TAG, "deletePic: " + delete);
    }

    private void savePic() {
        Uri picUri = getBitmapUri();
        try {
            OutputStream outputStream = resolver.openOutputStream(picUri);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


//  这是一种存储图片方式
//        try {
//            OutputStream outputStream = resolver.openOutputStream(picUri);
//            outputStream.write(BitmapByteStream.toByteArray());
//            outputStream.flush();
//            outputStream.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

     private void saveVideo() {
        String path = getExternalFilesDir("").getAbsolutePath() + File.separator + "otg.mp4";
        Uri uri = null;
        try {
            uri = insertVideo(this, path);

            //这是一种存储方式
//            byte[] bytesByFile = getBytesByFile(path);
//            try {
//                OutputStream outputStream = resolver.openOutputStream(uri);
//                outputStream.write(bytesByFile);
//                outputStream.flush();
//                outputStream.close();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }

            //因为只需要向URI指向的目录里写文件，因此只要设置为w的写入模式
            AssetFileDescriptor descriptor=getContentResolver().openAssetFileDescriptor(uri,"w");
            if(descriptor!=null) {
                FileInputStream inputStream=new FileInputStream(path);
                FileOutputStream outputStream=descriptor.createOutputStream();
                int byteRead;
                byte[] buffer = new byte[1024];
                while ((byteRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, byteRead);
                }
                inputStream.close();
                outputStream.close();
            }
        } catch (Exception e) {
            Log.e("dddd", Arrays.toString(e.getStackTrace()), e);
        }
    }

     /**
     * 插入一条视频的记录到媒体库
     *
     * @param context  上下文
     * @param filePath 需要插入的视频文件地址
     * @return 返回一个等待使用的Uri，不使用，则记录不会插入成功
     */
    private static Uri insertVideo(Context context, String filePath) {
        File file = new File(filePath);
        long paramLong = System.currentTimeMillis();
        ContentValues localContentValues = new ContentValues();
        localContentValues.put(MediaStore.Audio.Media.TITLE, file.getName());
        localContentValues.put(MediaStore.Audio.Media.DISPLAY_NAME, "test.mp4");
        localContentValues.put(MediaStore.Audio.Media.MIME_TYPE, "video/mp4");
        localContentValues.put(MediaStore.Audio.Media.DATE_ADDED, paramLong);
        localContentValues.put(MediaStore.Audio.Media.DATA, file.getAbsolutePath());
        localContentValues.put(MediaStore.Audio.Media.SIZE, file.length());
        localContentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Test");

        return context
                .getContentResolver()
                .insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, localContentValues);
    }
    //文件转byte[]
    public static byte[] getBytesByFile(String pathStr) {
        File file = new File(pathStr);
        try {
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
            byte[] b = new byte[1024];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            fis.close();
            byte[] data = bos.toByteArray();
            bos.close();
            return data;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
