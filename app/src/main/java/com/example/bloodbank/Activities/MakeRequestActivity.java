package com.example.bloodbank.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.PermissionChecker;
import androidx.preference.PreferenceManager;

import com.androidnetworking.*;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.interfaces.StringRequestListener;
import com.androidnetworking.interfaces.UploadProgressListener;
import com.bumptech.glide.Glide;
import com.example.bloodbank.R;
import com.example.bloodbank.Utils.Endpoints;

import org.json.JSONObject;

import java.io.File;
import java.net.URISyntaxException;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

public class MakeRequestActivity extends AppCompatActivity {

    EditText messageText;
    TextView chooseImageText;
    ImageView postImage;
    Button submit_button;
    Uri imageuri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_request);
        AndroidNetworking.initialize(getApplicationContext());
        messageText=findViewById(R.id.message);
        chooseImageText=findViewById(R.id.choose_text);
        postImage=findViewById(R.id.post_image);
        submit_button=findViewById(R.id.submitbutton);
        submit_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isValid()){
                    //code to upload this post
                    System.out.println("========"+messageText.getText().toString());

                    uploadRequest(messageText.getText().toString());
                }
            }
        });
        chooseImageText.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                permission();
            }
        });
    }


    private void pickImage(){
        Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent,101);
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void permission(){
        if (PermissionChecker.checkSelfPermission(getApplicationContext(),READ_EXTERNAL_STORAGE)!=PermissionChecker.PERMISSION_GRANTED){
            requestPermissions(new String[]{READ_EXTERNAL_STORAGE},401);
        }
        else{
            pickImage();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==401){
            if (grantResults[0]==PermissionChecker.PERMISSION_GRANTED){
                pickImage();
            }else{
                showMessage("Permission Denied");
            }
        }
    }

    private void uploadRequest(String msg){
        String path="";
        try{
            path=getPath(imageuri);

        }catch (URISyntaxException e){
            showMessage("wrong uri");
        }
        String number= PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("number","12345");
        AndroidNetworking.upload(Endpoints.upload_request)
                .addMultipartFile("file",new File(path))
                .addQueryParameter("message",msg)
                .addQueryParameter("number",number)
                .setPriority(Priority.HIGH)
                .build()
                .setUploadProgressListener(new UploadProgressListener() {
                    @Override
                    public void onProgress(long bytesUploaded, long totalBytes) {
                        // do anything with progress
                        long process=(bytesUploaded/totalBytes)*100;
                        chooseImageText.setText(String.valueOf((process)));
                        chooseImageText.setOnClickListener(null);
                    }
                })
                .getAsString(new StringRequestListener() {
                    @Override
                    public void onResponse(String response) {
                        showMessage(response);
                        chooseImageText.setOnClickListener(new View.OnClickListener() {
                            @RequiresApi(api = Build.VERSION_CODES.M)
                            @Override
                            public void onClick(View v) {
                                permission();
                            }
                        });
                    }

                    @Override
                    public void onError(ANError anError) {
                        showMessage(anError.getMessage());
                    }
                });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode ==101 && resultCode==RESULT_OK){
            if(data!=null){
                imageuri=data.getData();
                Glide.with(getApplicationContext()).load(imageuri).into(postImage);
                System.out.println("========"+imageuri);
            }
        }
    }

    private boolean isValid(){
        if (messageText.getText().toString().isEmpty()){
            showMessage("Message shouldn't be Empty");
            return false;

        }else if(imageuri==null){
            showMessage("Image shouldn't be empty");
            return false;
        }
        return true;
    }

    private  void showMessage(String msg){
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("NewApi")
    private String getPath(Uri uri) throws URISyntaxException {
        final boolean needToCheckUri = Build.VERSION.SDK_INT >= 19;
        String selection = null;
        String[] selectionArgs = null;
        // Uri is different in versions after KITKAT (Android 4.4), we need to
        // deal with different Uris.
        if (needToCheckUri && DocumentsContract.isDocumentUri(getApplicationContext(), uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                return Environment.getExternalStorageDirectory() + "/" + split[1];
            } else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                uri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
            } else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("image".equals(type)) {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                selection = "_id=?";
                selectionArgs = new String[]{
                        split[1]
                };
            }
        }
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {
                    MediaStore.Images.Media.DATA
            };
            Cursor cursor = null;
            try {
                cursor = getContentResolver()
                        .query(uri, projection, selection, selectionArgs, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }


    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }


    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }


    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

}
