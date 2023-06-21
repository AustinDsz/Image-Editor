package com.example.image_editor;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int PICK_IMAGE_REQUEST = 1;
    private TextView textView;
    private ImageButton btnGrayscale;
    private ImageButton btnReset;
    private ImageButton btnImport;
    private ImageButton btnRotate;
    private ImageButton btnSave;

    private Bitmap originalBitmap;
    private Bitmap editedBitmap;

    private ImageView imageView;

    private ImageButton btnShare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        btnGrayscale = findViewById(R.id.btnGrayscale);
        btnReset = findViewById(R.id.btnReset);
        btnImport = findViewById(R.id.btnImport);
        btnRotate = findViewById(R.id.btnRotate);
        btnSave = findViewById(R.id.btnSave);
        imageView = findViewById(R.id.imageView);
        btnShare = findViewById(R.id.btnShare);

        btnGrayscale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyGrayscaleFilter();
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetImage();
            }
        });

        btnImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        btnRotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotateImage();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveImage();
            }
        });

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareImage();
            }
        });
    }

    private void applyGrayscaleFilter() {
        if (originalBitmap != null) {
            editedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0); // Convert to grayscale
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);

            Paint paint = new Paint();
            paint.setColorFilter(filter);

            Canvas canvas = new Canvas(editedBitmap);
            canvas.drawBitmap(editedBitmap, 0, 0, paint);

            ImageView imageView = findViewById(R.id.imageView);
            imageView.setImageBitmap(editedBitmap);
        }
        else{
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
        }
    }


    private void resetImage() {
        if (originalBitmap != null) {
            editedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
            ImageView imageView = findViewById(R.id.imageView);
            imageView.setImageBitmap(editedBitmap);
            imageView.setColorFilter(null);
        }
        else{
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void rotateImage() {
        if (editedBitmap != null) {
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap rotatedBitmap = Bitmap.createBitmap(editedBitmap, 0, 0, editedBitmap.getWidth(), editedBitmap.getHeight(), matrix, true);
            ImageView imageView = findViewById(R.id.imageView);
            imageView.setImageBitmap(rotatedBitmap);
            editedBitmap = rotatedBitmap;
        }
        else{
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
        }
    }



    private void saveImage() {
        Bitmap finalBitmap = editedBitmap; // Use the editedBitmap or grayscaleBitmap based on your requirements

        if (finalBitmap != null) {
            String fileName = "edited_image.jpg";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, fileName);
            values.put(MediaStore.Images.Media.DESCRIPTION, "Edited Image");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            ContentResolver contentResolver = getContentResolver();
            Uri imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            try {
                OutputStream outputStream = contentResolver.openOutputStream(imageUri);

                if (finalBitmap.getConfig() == null) {
                    finalBitmap = finalBitmap.copy(Bitmap.Config.ARGB_8888, false);
                }

                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

                if (outputStream != null) {
                    outputStream.close();
                }

                Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(this, "No image to save", Toast.LENGTH_SHORT).show();
        }
    }

    public void shareImage() {
        if (editedBitmap != null) {
            try {
                // Save the edited image to a temporary file
                File file = new File(getCacheDir(), "edited_image.jpg");
                FileOutputStream outputStream = new FileOutputStream(file);
                editedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                outputStream.flush();
                outputStream.close();

                // Create a content URI for the temporary file
                Uri imageUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);

                // Create an intent to share the image
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/jpeg");
                shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                // Launch the share intent
                startActivity(Intent.createChooser(shareIntent, "Share Image"));
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to share image", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No image to share", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveImage(); // Retry saving the image after permission is granted
            } else {
                Toast.makeText(this, "Permission denied, unable to save image", Toast.LENGTH_SHORT).show();
            }
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                editedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                imageView.setImageBitmap(editedBitmap);
                textView.setVisibility(View.GONE);
                imageView.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
