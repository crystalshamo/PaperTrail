package com.example.papertrail;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class EditPage extends AppCompatActivity {
    String journalName;
    private DatabaseHelper databaseHelper;
    static final int PICK_IMAGE_REQUEST = 1; // For picking image from gallery
    static final int CAMERA_REQUEST = 2;    // For taking a picture using the camera


    FrameLayout frameLayout;
    private List<ImageView> imageViews = new ArrayList<>();
    private TextView pageNumberTv;
    Button backButton;
    private ScaleGestureDetector scaleGestureDetector;
    private float initialX = 0f;
    private float initialY = 0f;
    Bitmap bm = null;
    Button captureButton;

    private BottomNavigationView bottomNavigationView;
    private ImageView selectedImageView = null;  // Track the selected image

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_page);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("journal_name")) {
            journalName = intent.getStringExtra("journal_name");
        } else {
            // Handle the case where the Intent is missing or extra is not found
            Toast.makeText(this, "No journal selected", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity if no journal is selected
            return;
        }

        databaseHelper = new DatabaseHelper(this);
        try {
            databaseHelper.createDataBase();
            databaseHelper.openDataBase(); // Open database for writing
        } catch (IOException ioe) {
            throw new Error("Unable to create or open database");
        }
        backButton = findViewById(R.id.buttonPrev);
        pageNumberTv = findViewById(R.id.pageNumberTv);
        frameLayout = findViewById(R.id.pageLayout);
        captureButton = findViewById(R.id.buttonSavePage);
        captureButton.setOnClickListener(v -> {
            // Get the page number from the TextView
            String pageNumberText = pageNumberTv.getText().toString();
            int pageNumber = Integer.parseInt(pageNumberText);  // Convert it to an integer

            // Capture the FrameLayout as Bitmap using Bitmap.createBitmap()
            frameLayout.setDrawingCacheEnabled(false);  // Disable it to save memory
            frameLayout.buildDrawingCache();
            bm = Bitmap.createBitmap(frameLayout.getDrawingCache());  // Create Bitmap from the cache
            frameLayout.setDrawingCacheEnabled(false);  // Clean up

            databaseHelper.saveImageToPageTable(journalName,pageNumber,bm);
            Toast.makeText(this, "saved", Toast.LENGTH_SHORT).show();
        });

        // Initialize ScaleGestureDetector to handle pinch to zoom
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Add items to BottomNavigationView dynamically
        bottomNavigationView.getMenu().add(0, 1, 0, "Image").setIcon(R.drawable.ic_image);
        bottomNavigationView.getMenu().add(0, 2, 1, "Text").setIcon(R.drawable.ic_text);
        bottomNavigationView.getMenu().add(0, 3, 2, "Stickers").setIcon(R.drawable.ic_stickers);

        // Handle navigation item selections
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    showImagePickerDialog();
                    break;
                case 2:
                    // Handle Text selection
                    break;
                case 3:
                    // Handle Stickers selection
                    break;
            }
            return true;
        });
        //checkForFirstPage();
      loadPage();
    }



    public void loadPage() {
        String pageNumberText = pageNumberTv.getText().toString();
        int pageNumber = Integer.parseInt(pageNumberText);
        if (databaseHelper.isPageExist(pageNumber,journalName)) {
            Bitmap bitmap = databaseHelper.getPageImageFromDatabase(journalName,pageNumber);
            if (bitmap != null) {
                // Convert the Bitmap to a Drawable
                Drawable drawable = new BitmapDrawable(getResources(), bitmap);


                // Set the background of the FrameLayout to the Drawable
                frameLayout.setBackground(drawable);
            }
        }
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
        ImageView imageViewPage = new ImageView(this);
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
                        selectedImageView = (ImageView) v; // Track the selected image for scaling
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
        imageViews.add(imageViewPage);  // Store the reference to this image
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (selectedImageView != null) {
                // Get the scale factor
                float scaleFactor = detector.getScaleFactor();
                float currentScaleX = selectedImageView.getScaleX();
                float currentScaleY = selectedImageView.getScaleY();

                // Apply scaling only to the selected image
                selectedImageView.setScaleX(currentScaleX * scaleFactor);
                selectedImageView.setScaleY(currentScaleY * scaleFactor);
            }

            return true;
        }
    }
}


