package com.example.ocr;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
//import org.apache.poi.ss.usermodel.*;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;XSSFWorkbook


public class MainActivity extends AppCompatActivity {

    private MaterialButton inputimages, recognoizetext, excelbtn;
    private ShapeableImageView imageView;
    private TextView txtRecognized, path;


    private static final String Tag = "MAIN_TAG";

    private ProgressDialog progressDialog;

    private static final int Camera_request_code = 100;
    private static final int Storage_request_code = 101;

    private String[] cameraperssion;
    private String[] storageperssion;

    List<String[]> rows = new ArrayList<>();
    private Bitmap bitmap;
    private Uri imageurl = null;

    List<String[]> dataList = new ArrayList<String[]>();
    private static final String CHANNEL_ID = "csv_channel";
    private static final String CHANNEL_NAME = "CSV Channel";
    private static final String CHANNEL_DESCRIPTION = "Channel for CSV notifications";
    private static final int NOTIFICATION_ID = 1;
    private TessBaseAPI tessBaseAPI;
    private com.google.mlkit.vision.text.TextRecognizer TextRecognizer;
    String csv = "";
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    //    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputimages = findViewById(R.id.inputimages);
//        recognoizetext = findViewById(R.id.readimages);
        imageView = findViewById(R.id.images);
        txtRecognized = findViewById(R.id.txtRecognized);
        path = findViewById(R.id.path);

        tessBaseAPI = new TessBaseAPI();
//        String tessDataPath = getFilesDir() + "/tesseract/";
//        String language = "eng"; // Replace with the language code you are using
//        tessBaseAPI.init(tessDataPath, language);

        rows.add(new String[]{"Name", "Age"});
        rows.add(new String[]{"John", "25"});
        rows.add(new String[]{"Alice", "30"});

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "my_channel_id";
            CharSequence channelName = "My Channel";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // Create the intent to open the app when the notification is clicked
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);


        csv = (Environment.getExternalStorageDirectory().getAbsolutePath() + "/Expensive Application.csv");

        //        Workbook workbook = new XSSFWorkbook();

        cameraperssion = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storageperssion = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};


        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Waits");
        progressDialog.setCanceledOnTouchOutside(false);

        dataList.add(new String[]{"User Name", "Expensive Application"});


        TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        inputimages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                path.setText(" ");
//                showimagesDialog();
                if (checkcamerapermission()) {
                    pickimages();
                } else {
                    requestCamerapermission();
                }
            }
        });

//        recognoizetext.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                path.setText(" ");
//                recognoizetextforImages();
//
//            }
//        });

    }

    private void showNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "my_channel_id")
                .setSmallIcon(R.drawable.ic_baseline_circle_notifications_24)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private Bitmap getBitmapFromImageView(ImageView imageView) {
        imageView.setDrawingCacheEnabled(true);
        imageView.buildDrawingCache(true);
        Bitmap bitmap = Bitmap.createBitmap(imageView.getDrawingCache());
        imageView.setDrawingCacheEnabled(false);
        return bitmap;
    }

    private void processRecognizedText(Text text) {
        if (text != null) {

        } else {
            // Text recognition failed, no text found in the image.
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        dismissProgressDialog();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void recognoizetextforImages() {
        progressDialog.setMessage("prepare text");
        progressDialog.show();

        try {

            InputImage inputImage = InputImage.fromFilePath(this, imageurl);
            Task<Text> textTask = TextRecognizer.process(inputImage).addOnSuccessListener(new OnSuccessListener<Text>() {
                @Override
                public void onSuccess(Text text) {
                    dismissProgressDialog();
                    processRecognizedText(text);
                    Log.d(TAG, "onSuccess: " + text);
                    List<Text.TextBlock> test = text.getTextBlocks();
                    String reconginze = text.getText();
                    System.out.print("reconginze: " + reconginze);


                    String findresult = "";
                    String userresult = "";
                    String[] a = reconginze.split(("\n"));
                    for (int i = 0; i < a.length; i++) {

                        if (a[i].contains("MYR-Malaysian Ringgit"))
                            findresult = a[i + 1];


                        //Z01856

                        if (a[i].contains("Payment Currency"))
                            userresult = a[i + 1];


                        System.out.println(a[i]);
                    }

                    txtRecognized.setText(reconginze);


                    if (findresult.isEmpty() && userresult.isEmpty()) {
                        txtRecognized.setText("User Name and Expense Application Not found");
                    } else if (findresult.isEmpty()) {
                        txtRecognized.setText("Expense Application: Not found");
                    } else if (userresult.isEmpty()) {
                        txtRecognized.setText("User Name: Not found");
                    } else {
                        dataList.add(new String[]{userresult, findresult});
                        txtRecognized.setText("User Name: " + userresult + " \nExpense Application: " + findresult);
                    }


                    System.out.println("Reconginze: " + reconginze);


                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Failed in text", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            progressDialog.dismiss();
            e.printStackTrace();
        }


    }

    @Override
    protected void onStop() {

        if (dataList.size() > 1) {

            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                CSVWriter writer = null;
                String fileDir = StorageUtil.getInternalStoragePath(getApplicationContext());
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);


                String fileName = "data.csv";
                File file = new File(getExternalFilesDir(null), fileName);

                try {
                    FileWriter fileWriter = new FileWriter(file);
                    CSVWriter csvWriter = new CSVWriter(fileWriter);

                    csvWriter.writeAll(dataList);
                    csvWriter.close();

                    path.setText("path: " + getExternalFilesDir(null) + "/" + fileName);

                    showNotification("CSV File Saved", "CSV file saved successfully");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //


                //                    callRead();
            } else {
                Toast.makeText(MainActivity.this, "Write external storage permission not granted", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }
        super.onStop();
        // Perform cleanup and save data here
    }

    private void showimagesDialog() {
        PopupMenu popupMenu = new PopupMenu(this, inputimages);
        popupMenu.getMenu().add(Menu.NONE, 1, 1, "CAMERA");
        popupMenu.getMenu().add(Menu.NONE, 2, 2, "GALLERY");
        popupMenu.show();

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                int id = item.getItemId();
                if (id == 1) {
                    if (checkcamerapermission()) {
                        pickimages();
                    } else {
                        requestCamerapermission();
                    }
                } else if (id == 2) {
                    if (checkstoragepermission()) {
                        gallery();
                    } else {
                        requestStoragepermission();
                    }
                }
                return true;
            }
        });
    }

    private void gallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryactivity.launch(intent);
    }

    private ActivityResultLauncher<Intent> galleryactivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        imageurl = data.getData();
                        imageView.setImageURI(imageurl);
                        txtRecognized.setText(" ");
                        path.setText(" ");
                        if (imageurl == null) {
                            path.setText(" ");
                            Toast.makeText(MainActivity.this, "Pick Images first...", Toast.LENGTH_SHORT).show();

                        } else {
                            recognoizetextforImages();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    public void pickimages() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "sample TITLE");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "sample DESCRIPTION");

        imageurl = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageurl);
        cameraactivitylaunch.launch(intent);

    }


    private ActivityResultLauncher<Intent> cameraactivitylaunch = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {

                        imageView.setImageURI(imageurl);
                        txtRecognized.setText(" ");
                        path.setText(" ");
                        if (imageurl == null) {
                            path.setText(" ");
                            Toast.makeText(MainActivity.this, "Pick Images first...", Toast.LENGTH_SHORT).show();
                        } else {
                            recognoizetextforImages();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );


    private Boolean checkstoragepermission() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragepermission() {
        ActivityCompat.requestPermissions(this, storageperssion, Storage_request_code);
    }

    private Boolean checkcamerapermission() {
        boolean cameraresult = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_DENIED);
        boolean Storageresult = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return cameraresult && Storageresult;
    }

    private void requestCamerapermission() {
        ActivityCompat.requestPermissions(this, cameraperssion, Camera_request_code);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        switch (requestCode) {

            case Camera_request_code: {
                if (grantResults.length > 0) {
                    boolean cameaaccept = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean Storageaccept = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (cameaaccept && Storageaccept) {
                        pickimages();
                    } else {
                        Toast.makeText(this, "Camera and Storage are required", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Camera", Toast.LENGTH_SHORT).show();
                }
            }
            break;
            case Storage_request_code: {
                if (grantResults.length > 0) {
                    boolean Storageaccept = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (Storageaccept) {
                        gallery();
                    } else {
                        Toast.makeText(this, "Camera and Storage are required", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Camera", Toast.LENGTH_SHORT).show();
                }
            }
            break;

        }
    }
}