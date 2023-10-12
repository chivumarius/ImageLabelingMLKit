package com.chivumarius.imagelabelingmlkit;



import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.List;

import android.util.Log;
import android.database.Cursor;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;


public class MainActivity extends AppCompatActivity {

    private static final int RESULT_LOAD_IMAGE = 123;
    public static final int IMAGE_CAPTURE_CODE = 654;
    private static final int PERMISSION_CODE = 321;


    // ▼ "DECLARATION" OF "VIEWS" → FROM "UI" ▼
    ImageView frame, innerImage;

    private Uri image_uri;
    TextView resultTv;


    // ▼ ("STEP 2-2" - "IMAGE LABELING") "DECLARATION" OF "IMAGE LABELER"  ▼
    ImageLabeler labeler;



    // ▬ "ON CREATE()" METHOD ▬
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        frame = findViewById(R.id.imageView);

        // ▼ "INITIALIZATION" OF "INNER IMAGE" VIEW → FROM "UI" ▼
        innerImage = findViewById(R.id.imageView2);
        resultTv = findViewById(R.id.textView);

        frame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, RESULT_LOAD_IMAGE);
            }
        });


        frame.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_DENIED){
                        String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        requestPermissions(permission, PERMISSION_CODE);
                    }
                    else {
                        openCamera();
                    }
                }
                else {
                    openCamera();
                }
                return false;
            }
        });



        // ▼ (STEP 2-1 - "IMAGE LABELING")
        //      → "CONFIGURE" AND "RUN"
        //      → THE "IMAGE LABELER"
        //      → TO USE "DEFAULT OPTIONS" ▼
        labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);


        // Or, to set the minimum confidence required:
// ImageLabelerOptions options =
//     new ImageLabelerOptions.Builder()
//         .setConfidenceThreshold(0.7f)
//         .build();
// ImageLabeler labeler = ImageLabeling.getClient(options);
    }






    // ▬ "OPEN CAMERA()" METHOD ▬
    //TODO TO "OPEN" THE "CAMERA" SO THAT "USER" CAN "CAPTURE IMAGES" ▼
    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE);
    }





    // ▬ "ON REQUEST PERMISSIONS RESULT()" METHOD ▬
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            //TODO show live camera footage
            openCamera();
        } else {
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
        }
    }

    



    // ▬ "ON ACTIVITY RESULT()" METHOD ▬
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null){
            image_uri = data.getData();
            //innerImage.setImageURI(image_uri);
            Bitmap bitmap = uriToBitmap(image_uri);
            doInference(bitmap);
        }

        if (requestCode == IMAGE_CAPTURE_CODE && resultCode == RESULT_OK){
            //innerImage.setImageURI(image_uri);
            Bitmap bitmap = uriToBitmap(image_uri);
            doInference(bitmap);

        }

    }

    


    // ▬ "DO INFERENCE()" METHOD ▬
    public void doInference(Bitmap input){
        Bitmap rotated = rotateBitmap(input);
        innerImage.setImageBitmap(rotated);


        // ▼ (STEP 1 - "IMAGE LABELING")
        //      → "PREPARE" THE "INPUT IMAGE"
        //      → USING A "BITMAP" ▼
        InputImage image = InputImage.fromBitmap(rotated, 0);



        // ▼ (STEP 4-2 - "IMAGE LABELING")
        //      → RESET THE TEXT FOR "RESULT TV"
        //      → TO "EMPTY" ▼
       resultTv.setText("");


        // ▼ (STEP 3 - "IMAGE LABELING")
        //      → "PASSING" THE "INPUT IMAGE"
        //      → TO  THE  "PROCESS()" METHOD ▼
        labeler.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
                    @Override
                    public void onSuccess(List<ImageLabel> labels) {

                        // ▼ (STEP 4 - "IMAGE LABELING")
                        //      → GET "INFORMATION"
                        //      → ABOUT "LABELED" OBJECTS ▼

                        for (ImageLabel label : labels) {
                            String text = label.getText();
                            float confidence = label.getConfidence();
                            int index = label.getIndex();


                            // ▼ (STEP 4-1 - "IMAGE LABELING")
                            //      → SHOWING THE "NAME" AND THE "CONFIDENCE SCORE"
                            //      → FOR "EACH PREDICTION" OBJECT" ▼
                            resultTv.append(text + " " + confidence + "\n");
                        }

                    }
                })

                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        // ...
                    }
                });
    }






    // ▬ "ROTATE BITMAP()" METHOD
    //      → TO "ROTATE IMAGES" IF "IMAGE" IS "CAPTURED" ON "SAMSUNG DEVICES" ▬
    public Bitmap rotateBitmap(Bitmap input){
        String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
        Cursor cur = getContentResolver().query(image_uri, orientationColumn, null, null, null);
        int orientation = -1;
        if (cur != null && cur.moveToFirst()) {
            orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]));
        }
        Log.d("tryOrientation",orientation+"");
        Matrix rotationMatrix = new Matrix();
        rotationMatrix.setRotate(orientation);
        Bitmap cropped = Bitmap.createBitmap(input,0,0, input.getWidth(), input.getHeight(), rotationMatrix, true);
        return cropped;
    }





    // ▬ "URI TO BITMAP()" METHOD
    //      → IT "TAKES" A "URI" OF THE "IMAGE" AND RETURNS" A BITMAP" ▬
    private Bitmap uriToBitmap(Uri selectedFileUri) {
        try {
            ParcelFileDescriptor parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(selectedFileUri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);

            parcelFileDescriptor.close();
            return image;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  null;
    }




    // ▬ "ON DESTROY()" METHOD
    //      → FOR "CLOSING IMAGE LABELING"
    //      → WHEN WE "CLOSING" THE "APPLICATION" ▬
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // ▼ "CLOSE IMAGE LABELING" ▼
        labeler.close();
    }
}
