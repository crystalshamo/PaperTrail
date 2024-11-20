package com.example.papertrail;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class EditPage extends AppCompatActivity {
    private static final int GALLERY_REQUEST_CODE = 101;
    private FrameLayout frameLayout;
    private ImageView imageView;
    private Bitmap selectedImage;
    private ScaleGestureDetector scaleGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_page);

        // Initialize the layout components
        frameLayout = findViewById(R.id.frameLayout);

        // Initialize ScaleGestureDetector for resizing
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        // Bottom navigation setup (as before)
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
               // case R.id.nav_image:
                //    openImagePicker();  // Open image picker or camera
                  //  return true;
               // case R.id.nav_stickers:
                    // Op en sticker selector activity
                 //   return true;
                default:
                    return false;
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, GALLERY_REQUEST_CODE);  // Open gallery
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == GALLERY_REQUEST_CODE) {
                try {
                    selectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());

                    // Create an ImageView to display the selected image
                    imageView = new ImageView(this);
                    imageView.setImageBitmap(selectedImage);

                    // Set the image view properties
                    imageView.setLayoutParams(new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT));

                    imageView.setBackgroundColor(Color.TRANSPARENT);

                    // Add the image view to the FrameLayout
                    frameLayout.removeAllViews();  // Remove any existing views
                    frameLayout.addView(imageView);

                    // Enable dragging and resizing
                    setImageViewTouchListener(imageView);

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // Set up touch listener for dragging and resizing the image
    private void setImageViewTouchListener(final ImageView imageView) {
        imageView.setOnTouchListener((view, motionEvent) -> {
            // Handle scaling (resizing)
            scaleGestureDetector.onTouchEvent(motionEvent);

            switch (motionEvent.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    // For drag: get the initial touch position
                    break;
                case MotionEvent.ACTION_MOVE:
                    // For drag: update position based on movement
                    float deltaX = motionEvent.getRawX() - imageView.getWidth() / 2;
                    float deltaY = motionEvent.getRawY() - imageView.getHeight() / 2;

                    imageView.setX(deltaX);
                    imageView.setY(deltaY);
                    break;
                case MotionEvent.ACTION_UP:
                    // You can add logic to finalize the position when the user lifts their finger
                    break;
            }

            return true;  // Return true to indicate that we’ve handled the touch event
        });
    }

    // Scale Gesture Listener for resizing the image
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            // Get the scale factor and apply it to the image
            float scaleFactor = detector.getScaleFactor();
            if (scaleFactor > 0) {
                imageView.setScaleX(imageView.getScaleX() * scaleFactor);
                imageView.setScaleY(imageView.getScaleY() * scaleFactor);
            }
            return true;
        }
    }
}


