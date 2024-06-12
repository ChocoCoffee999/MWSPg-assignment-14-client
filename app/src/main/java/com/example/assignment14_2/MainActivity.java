package com.example.assignment14_2;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int READ_MEDIA_IMAGES_PERMISSION_CODE = 1001;  // 상수 정의
    private static final int READ_EXTERNAL_STORAGE_PERMISSION_CODE = 1002;
    // 상수 정의

    //private static final String UPLOAD_URL = "http://127.0.0.1:8000/api_root/Post/";
    private static final String UPLOAD_URL = "http://10.0.2.2:8000/api_root/Post/";
    private static final String UPLOAD_URL_PYTHON_ANYWHERE = "http://chococoffee999.pythonanywhere.com/api_root/Post/";
    Uri imageUri = null;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    String twoHyphens = "--";
    String boundary = "*****" + Long.toString(System.currentTimeMillis()) + "*****";
    String lineEnd = "\r\n";

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult( //...코드 계속
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    String filePath = getRealPathFromURI(imageUri);
                    executorService.execute(() -> {
                        String uploadResult;
                        try {
                            uploadResult = uploadImage(filePath);
                        } catch (IOException e) {
                            uploadResult = "Upload failed: " + e.getMessage();
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        String finalUploadResult = uploadResult;
                        handler.post(() -> Toast.makeText(MainActivity.this, finalUploadResult, Toast.LENGTH_LONG).show());
                    });
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button uploadButton = findViewById(R.id.uploadButton);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                                READ_MEDIA_IMAGES_PERMISSION_CODE);
                    } else {
                        openImagePicker();
                    }
                } else {
                    if (ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                READ_EXTERNAL_STORAGE_PERMISSION_CODE);
                    } else {
                        openImagePicker();
                    }
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_MEDIA_IMAGES_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, projection, null, null, null);
        if (cursor == null) {
            return contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            String path = cursor.getString(columnIndex);
            cursor.close();
            return path;
        }
    }

    private String uploadImage(String imageUrl) throws IOException, JSONException {
        OutputStreamWriter outputStreamWriter = null;
        DataOutputStream outputStream = null;

        try {
            try {
                URL url = new URL(UPLOAD_URL_PYTHON_ANYWHERE);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");

                //Authorization, Token
                //connection.setRequestProperty("Authorization", "JWT c0c06532928e0d2374d8e462d3a62cc750edf157");
                connection.setRequestProperty("Authorization", "JWT d0ed741045ce342106954398c943fc9d28457246"); // pythonanywhere token

                //Content-Type
                //connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                //JSONObject jsonObject = new JSONObject();
                //jsonObject.put("author", "1"); token을 이용해 작성자를 확인
                //jsonObject.put("title", "안드로이드-REST API 테스트");
                //jsonObject.put("text", "안드로이드로 작성된 REST API 테스트 입력 입니다.");
                //jsonObject.put("created_date", "2024-06-03T18:34:00+09:00");
                //jsonObject.put("published_date", "2024-06-03T18:34:00+09:00");
                //jsonObject.put("image", imageUrl);

                //outputStreamWriter = new OutputStreamWriter(connection.getOutputStream());
                //outputStreamWriter.write(jsonObject.toString());
                //outputStreamWriter.flush();

//                if (connection.getResponseCode() == 200 || connection.getResponseCode() == 201) {
//                    Log.e("uploadImage", "Success");
//                }
//                connection.disconnect();

                outputStream = new DataOutputStream(connection.getOutputStream());

                File imageFile = new File(imageUrl);

                if (!imageFile.isFile()) {
                    System.out.println("Image file not found: " + imageUrl);
                }
                Log.i("Test", "A");
                FileInputStream fileInputStream = new FileInputStream(imageFile);
                byte[] fileBytes = new byte[(int) imageFile.length()];
                fileInputStream.read(fileBytes);
                fileInputStream.close();
                Log.i("Test", "B");

                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"image\";filename=\"" + imageFile.getName() + "\"\r\n");
                outputStream.writeBytes(lineEnd);
                outputStream.write(fileBytes);
                outputStream.writeBytes(lineEnd);

                Log.i("Test", "C");

                addFormField(outputStream, "title", "안드로이드-REST API 테스트");
                addFormField(outputStream, "text", "안드로이드로 작성된 REST API 테스트 입력 입니다.");
                addFormField(outputStream, "created_date", "2024-06-03T18:34:00+09:00");
                addFormField(outputStream, "published_date", "2024-06-03T18:34:00+09:00");

                Log.i("Test", "D");

                outputStream.writeBytes("--" + boundary + "--\r\n");
                outputStream.flush();
                outputStream.close();

                Log.i("Test", "E");

                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                System.out.println("Response: " + response.toString());

            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            Log.e("uploadImage", "Exception in uploadImage: " + e.getMessage());
        }
        return "success";
    }

    private void addFormField(DataOutputStream outputStream, String fieldName, String value) throws IOException {
        outputStream.writeBytes(twoHyphens + boundary + lineEnd);
        outputStream.writeBytes("Content-Disposition: form-data; name=\"" + fieldName + "\"" + lineEnd);
        outputStream.writeBytes(lineEnd);
        outputStream.write(value.getBytes(StandardCharsets.UTF_8));
        outputStream.writeBytes(lineEnd);
    }
}