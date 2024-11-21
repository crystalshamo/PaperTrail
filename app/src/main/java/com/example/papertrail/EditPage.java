package com.example.papertrail;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class EditPage extends AppCompatActivity {

    //DB STUFF
    private DatabaseHelper dbHelper;

    static final int PICK_IMAGE_REQUEST = 1; // For picking image from gallery
    static final int CAMERA_REQUEST = 2;    // For taking a picture using the camera

    FrameLayout frameLayout;
    ImageView imageViewPage;
    private TextView pageNumberTv;
    Button backButton;

    private ScaleGestureDetector scaleGestureDetector;
    private float currentScale = 1f;
    private float initialX = 0f;
    private float initialY = 0f;
    Bitmap bm = null;

    private BottomNavigationView bottomNavigationView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_page);

        backButton = findViewById(R.id.buttonPrev);
        dbHelper = new DatabaseHelper(this);
        pageNumberTv = findViewById(R.id.pageNumberTv);
        frameLayout = findViewById(R.id.frameLayoutPage);
        Button captureButton = findViewById(R.id.buttonSavePage);
        captureButton.setOnClickListener(v -> {
            // Get the page number from the TextView
            String pageNumberText = pageNumberTv.getText().toString();
            int pageNumber = Integer.parseInt(pageNumberText);  // Convert it to an integer

            // Capture the FrameLayout as Bitmap using Bitmap.createBitmap()
            frameLayout.setDrawingCacheEnabled(false);  // Disable it to save memory
            frameLayout.buildDrawingCache();
            bm = Bitmap.createBitmap(frameLayout.getDrawingCache());  // Create Bitmap from the cache
            frameLayout.setDrawingCacheEnabled(false);  // Clean up

            // Save the image to the database using the page number as ID
            dbHelper.saveImageToDatabase(pageNumber, bm);
        });
        backButton.setOnClickListener(v-> {
            Intent intent = new Intent(EditPage.this, HomeScreen.class);

            // Start the new activity
            startActivity(intent);
        });

        // Initialize ScaleGestureDetector to handle pinch to zoom
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Add items to BottomNavigationView dynamically
        bottomNavigationView.getMenu().add(0, 1, 0, "Image").setIcon(R.drawable.ic_image);
        bottomNavigationView.getMenu().add(0, 2, 1, "Text").setIcon(R.drawable.ic_text);
        bottomNavigationView.getMenu().add(0, 3, 2, "Stickers").setIcon(R.drawable.ic_stickers);
       // bottomNavigationView.setItemRippleColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ripple_color)));
        // Handle navigation item selections
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {

                switch (item.getItemId()) {
                    case 1:
                        showImagePickerDialog();
                        break;
                    case 2:

                        break;
                    case 3:

                        break;
                }
                return true;
            }
        });
    }

    // Dialog for choosing between Camera or Gallery
    private void showImagePickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                openCamera(); // Open Camera to take a photo
            } else {
                openGallery(); // Open Gallery to pick an image
            }
        });
        builder.show();
    }

    // Open Camera to take a photo
    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_REQUEST);
    }

    // Open Gallery to pick an image
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                // If the result is from the gallery
                Uri imageUri = data.getData();
                try {
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    addImageToPage(bitmap);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == CAMERA_REQUEST) {
                // If the result is from the camera
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                addImageToPage(photo);
            }
        }
    }

    private void addImageToPage(Bitmap bitmap) {
        // Create a new ImageView and add the image to the page
        imageViewPage = new ImageView(this);
        imageViewPage.setImageBitmap(bitmap);
        imageViewPage.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        // Set the initial position for the image (optional)
        imageViewPage.setX(100); // Initial X position
        imageViewPage.setY(100); // Initial Y position

        // Enable dragging and resizing
        imageViewPage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleGestureDetector.onTouchEvent(event); // Handle pinch to zoom

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = event.getRawX() - v.getX();
                        initialY = event.getRawY() - v.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        v.setX(event.getRawX() - initialX);
                        v.setY(event.getRawY() - initialY);
                        break;
                }
                return true;
            }
        });

        // Add the image to the FrameLayout (page)
        frameLayout.addView(imageViewPage);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            // Get the scale factor
            float scaleFactor = detector.getScaleFactor();

            // Optional: Use getCurrentSpanX/Y or getPreviousSpanX/Y for more control
            float currentSpanX = detector.getCurrentSpanX();
            float currentSpanY = detector.getCurrentSpanY();
            float previousSpanX = detector.getPreviousSpanX();
            float previousSpanY = detector.getPreviousSpanY();

            // Optionally, apply different scaling for X and Y directions
            float scaleFactorX = currentSpanX / previousSpanX;
            float scaleFactorY = currentSpanY / previousSpanY;

            // Update the scale factor for the image
            currentScale *= scaleFactor;
            currentScale = Math.max(0.1f, Math.min(currentScale, 5.0f));  // Limit the scale

            // Apply scaling to the image
            imageViewPage.setScaleX(currentScale);
            imageViewPage.setScaleY(currentScale);

            // Optionally, log or use time delta for smooth transitions (if you plan to animate)
            long timeDelta = detector.getTimeDelta();  // This gives you the time difference between scale events
            // You can use timeDelta to adjust the speed or animation behavior

            return true;
        }
    }

}

