package com.example.papertrail;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
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
    private String journalName;
    private String pageNumberText;
    private DatabaseHelper databaseHelper;

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;

    private FrameLayout frameLayout;
    private List<ImageView> imageViews = new ArrayList<>();
    private TextView pageNumberTv;
    private Button prevButton, nextButton, captureButton, addPageButton;
    private ScaleGestureDetector scaleGestureDetector;
    private float initialX = 0f, initialY = 0f;
    private Bitmap bm = null;
    private BottomNavigationView bottomNavigationView;
    private ImageView selectedImageView = null;
    Intent stickerIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_page);

        journalName = getIntent().getStringExtra("journal_name");
        if (journalName == null) {
            Toast.makeText(this, "No journal selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        databaseHelper = new DatabaseHelper(this);
        try {
            databaseHelper.createDataBase();
            databaseHelper.openDataBase();
        } catch (IOException ioe) {
            throw new Error("Unable to create or open database");
        }

        initializeViews();
        setupBottomNavigation();
        loadPage();
        loadArrows();

        pageNumberTv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int after) {
                loadPage();
                loadArrows();
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    private void initializeViews() {
        prevButton = findViewById(R.id.buttonPrev);
        nextButton = findViewById(R.id.buttonNext);
        pageNumberTv = findViewById(R.id.pageNumberTv);
        frameLayout = findViewById(R.id.pageLayout);
        captureButton = findViewById(R.id.buttonSavePage);
        addPageButton = findViewById(R.id.buttonAddPage);

        prevButton.setOnClickListener(v -> decrementPage());
        nextButton.setOnClickListener(v -> incrementPage());
        addPageButton.setOnClickListener(v -> incrementPage());
        captureButton.setOnClickListener(v -> savePage());

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
    }

    private void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.getMenu().add(0, 1, 0, "Image").setIcon(R.drawable.ic_image);
        bottomNavigationView.getMenu().add(0, 2, 1, "Text").setIcon(R.drawable.ic_text);
        bottomNavigationView.getMenu().add(0, 3, 2, "Stickers").setIcon(R.drawable.ic_stickers);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case 1: showImagePickerDialog(); break;
                case 2: // Handle Text selection
                     break;
                case 3:
                    stickerIntent = new Intent(EditPage.this, StickerScreen.class);
                    startActivityForResult(stickerIntent, 1001); // Request code for stickers
                    break;
            }
            return true;
        });
    }

    private void loadPage() {
        pageNumberText = pageNumberTv.getText().toString();
        int pageNumber = Integer.parseInt(pageNumberText);
        frameLayout.removeAllViews();
        if (databaseHelper.isPageExist(pageNumber, journalName)) {
            Bitmap bitmap = databaseHelper.getPageImageFromDatabase(journalName, pageNumber);
            if (bitmap != null) {
                Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                frameLayout.setBackground(drawable);
            }
        } else {
            frameLayout.setBackgroundColor(Color.WHITE);
        }
    }

    private void loadArrows() {
        pageNumberText = pageNumberTv.getText().toString();
        int pageNumber = Integer.parseInt(pageNumberText);

        if (databaseHelper.hasPagesGreaterThan(pageNumber, journalName)) {
            nextButton.setVisibility(View.VISIBLE);
            addPageButton.setVisibility(View.INVISIBLE);
        } else {
            nextButton.setVisibility(View.INVISIBLE);
            addPageButton.setVisibility(View.VISIBLE);
        }

        if (databaseHelper.hasPagesLessThan(pageNumber,journalName)) {
            prevButton.setVisibility(View.VISIBLE);
        } else {
            prevButton.setVisibility(View.INVISIBLE);
        }
    }

    private void showImagePickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new android.app.AlertDialog.Builder(this)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) openCamera();
                    else openGallery();
                }).show();
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_REQUEST);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == 1001) { // Sticker request code
                int stickerResId = data.getIntExtra("selected_sticker", -1);
                if (stickerResId != -1) {
                    addStickerToPage(stickerResId);
                }
            } else if (requestCode == PICK_IMAGE_REQUEST) {
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
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                addImageToPage(photo);
            }
        }
    }

    private void addStickerToPage(int stickerResId) {
        ImageView stickerView = new ImageView(this);
        stickerView.setImageResource(stickerResId);
        stickerView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
        stickerView.setX(100); // Initial position
        stickerView.setY(100);

        stickerView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = event.getRawX() - v.getX();
                    initialY = event.getRawY() - v.getY();
                    selectedImageView = (ImageView) v;
                    break;
                case MotionEvent.ACTION_MOVE:
                    v.setX(event.getRawX() - initialX);
                    v.setY(event.getRawY() - initialY);
                    break;
            }
            return true;
        });

        frameLayout.addView(stickerView);
        imageViews.add(stickerView); // Keep track for saving
    }



    @SuppressLint("ClickableViewAccessibility")
    private void addImageToPage(Bitmap bitmap) {
        ImageView imageViewPage = new ImageView(this);
        imageViewPage.setImageBitmap(bitmap);
        imageViewPage.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        imageViewPage.setX(100); // Initial X position
        imageViewPage.setY(100); // Initial Y position

        imageViewPage.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = event.getRawX() - v.getX();
                    initialY = event.getRawY() - v.getY();
                    selectedImageView = (ImageView) v;
                    break;
                case MotionEvent.ACTION_MOVE:
                    v.setX(event.getRawX() - initialX);
                    v.setY(event.getRawY() - initialY);
                    break;
            }
            return true;
        });

        frameLayout.addView(imageViewPage);
        imageViews.add(imageViewPage);
    }

    private void savePage() {
        pageNumberText = pageNumberTv.getText().toString();
        int pageNumber = Integer.parseInt(pageNumberText);

        frameLayout.setDrawingCacheEnabled(false);
        frameLayout.buildDrawingCache();
        bm = Bitmap.createBitmap(frameLayout.getDrawingCache());
        frameLayout.setDrawingCacheEnabled(false);

        databaseHelper.saveImageToPageTable(journalName, pageNumber, bm);
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
    }

    private void decrementPage() {
        pageNumberText = pageNumberTv.getText().toString();
        int pageNumber = Integer.parseInt(pageNumberText);
        pageNumberTv.setText(String.valueOf(pageNumber - 1));
    }

    private void incrementPage() {
        pageNumberText = pageNumberTv.getText().toString();
        int pageNumber = Integer.parseInt(pageNumberText);
        pageNumberTv.setText(String.valueOf(pageNumber + 1));
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (selectedImageView != null) {
                float scaleFactor = detector.getScaleFactor();
                selectedImageView.setScaleX(selectedImageView.getScaleX() * scaleFactor);
                selectedImageView.setScaleY(selectedImageView.getScaleY() * scaleFactor);
            }
            return true;
        }
    }
}