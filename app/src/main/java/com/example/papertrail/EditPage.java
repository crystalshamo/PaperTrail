package com.example.papertrail;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
public class EditPage extends AppCompatActivity implements ColorWheelView.OnColorSelectedListener {

    private String journalName;
    private DatabaseHelper databaseHelper;
    private EditPageViewModel viewModel;
    private ImageView selectedImageView = null;

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int CROP_IMAGE_REQUEST = 3;

    private FrameLayout frameLayout;
    private TextView pageNumberTv;
    private Button prevButton, captureButton, nextButton, addPageButton, deletePageButton, paintButton;
    private ScaleGestureDetector scaleGestureDetector;
    private float initialX = 0f, initialY = 0f;
    private BottomNavigationView bottomNavigationView;
    private ColorWheelView colorWheelView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_page);

        // Initialize ViewModel and DatabaseHelper
        viewModel = new ViewModelProvider(this).get(EditPageViewModel.class);
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

        // Observe page number changes
        viewModel.getCurrentPage().observe(this, pageNumber -> {
            pageNumberTv.setText(String.valueOf(pageNumber));
            loadPage(pageNumber);
            loadArrows(pageNumber);
        });
    }

    private void initializeViews() {
        // Initialize all UI elements
        prevButton = findViewById(R.id.buttonPrev);
        nextButton = findViewById(R.id.buttonNext);
        frameLayout = findViewById(R.id.pageLayout);
        captureButton = findViewById(R.id.buttonSavePage);
        addPageButton = findViewById(R.id.buttonAddPage);
        pageNumberTv = findViewById(R.id.pageNumberTv);
        deletePageButton = findViewById(R.id.buttonDeletePage);
        paintButton = findViewById(R.id.buttonPaint);
        colorWheelView = findViewById(R.id.colorWheelView);

        prevButton.setOnClickListener(v -> decrementPage());
        nextButton.setOnClickListener(v -> incrementPage());
        addPageButton.setOnClickListener(v -> incrementPage());
        captureButton.setOnClickListener(v -> savePage());
        deletePageButton.setOnClickListener(v -> deletePage());

        paintButton.setOnClickListener(v -> colorWheelView.setVisibility(View.VISIBLE));
        colorWheelView.setOnColorSelectedListener(this);

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
    }

    private void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.getMenu().add(0, 1, 0, "Image").setIcon(R.drawable.ic_image);
        bottomNavigationView.getMenu().add(0, 2, 1, "Text").setIcon(R.drawable.ic_text);
        bottomNavigationView.getMenu().add(0, 3, 2, "Stickers").setIcon(R.drawable.ic_stickers);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    showImagePickerDialog();
                    break;
                case 3:
                    Intent stickerIntent = new Intent(EditPage.this, StickerScreen.class);
                    startActivity(stickerIntent);
                    break;
            }
            return true;
        });
    }

    private void loadPage(int pageNumber) {
        Log.d("EditPage", "Loading page: " + pageNumber);
        frameLayout.removeAllViews();

        if (databaseHelper.isPageExist(pageNumber, journalName)) {
            Bitmap bitmap = databaseHelper.getPageImageFromDatabase(journalName, pageNumber);
            if (bitmap != null) {
                frameLayout.setBackground(new BitmapDrawable(getResources(), bitmap));
            }
        } else {
            frameLayout.setBackgroundColor(Color.WHITE);
        }
    }

    private void loadArrows(int pageNumber) {
        if (databaseHelper.hasPagesGreaterThan(pageNumber, journalName)) {
            nextButton.setVisibility(View.VISIBLE);
            addPageButton.setVisibility(View.INVISIBLE);
        } else {
            nextButton.setVisibility(View.INVISIBLE);
            addPageButton.setVisibility(View.VISIBLE);
        }

        prevButton.setVisibility(databaseHelper.hasPagesLessThan(pageNumber, journalName) ? View.VISIBLE : View.INVISIBLE);
    }

    private void showImagePickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new AlertDialog.Builder(this)
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
            if (requestCode == PICK_IMAGE_REQUEST) {
                Uri imageUri = data.getData();
                if (imageUri != null) startCrop(imageUri);
            } else if (requestCode == CAMERA_REQUEST) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                Uri tempUri = getImageUri(getApplicationContext(), photo);
                startCrop(tempUri);
            } else if (requestCode == CROP_IMAGE_REQUEST) {
                handleCropResult(data);
            }
        }
    }

    private void startCrop(Uri sourceUri) {
        Intent cropIntent = new Intent("com.android.camera.action.CROP");
        cropIntent.setDataAndType(sourceUri, "image/*");
        cropIntent.putExtra("crop", "true");
        cropIntent.putExtra("outputX", 500); // Set output width
        cropIntent.putExtra("outputY", 500); // Set output height
        cropIntent.putExtra("aspectX", 1); // Aspect ratio
        cropIntent.putExtra("aspectY", 1); // Aspect ratio
        cropIntent.putExtra("scale", true); // Scale image
        cropIntent.putExtra("return-data", true); // Return data as a Bitmap
        startActivityForResult(cropIntent, CROP_IMAGE_REQUEST);
    }

    private void handleCropResult(Intent result) {
        Bundle extras = result.getExtras();
        if (extras != null) {
            Bitmap croppedBitmap = extras.getParcelable("data");
            if (croppedBitmap != null) {
                addImageToPage(croppedBitmap);
            }
        }
    }

    public static Uri getImageUri(Context context, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
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
            handleImageTouch(v, event);
            return true;
        });

        frameLayout.addView(imageViewPage);
    }

    private void handleImageTouch(View v, MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            initialX = event.getRawX() - v.getX();
            initialY = event.getRawY() - v.getY();
            selectedImageView = (ImageView) v;
        } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            v.setX(event.getRawX() - initialX);
            v.setY(event.getRawY() - initialY);
        }
    }

    @Override
    public void onColorSelected(int color) {
        frameLayout.setBackgroundColor(color);
        colorWheelView.setVisibility(View.GONE);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            if (selectedImageView != null) {
                float scaleFactor = detector.getScaleFactor();
                selectedImageView.setScaleX(selectedImageView.getScaleX() * scaleFactor);
                selectedImageView.setScaleY(selectedImageView.getScaleY() * scaleFactor);
            }
            return true;
        }
    }

    private void decrementPage() {
        viewModel.decrementPage();
    }

    private void incrementPage() {
        viewModel.incrementPage();
    }

    private void savePage() {
        Integer pageNumber = viewModel.getCurrentPage().getValue();
        if (pageNumber == null) {
            Toast.makeText(this, "Invalid page number", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap bm = Bitmap.createBitmap(frameLayout.getWidth(), frameLayout.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        frameLayout.draw(canvas);

        databaseHelper.saveImageToPageTable(journalName, pageNumber, bm);
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
    }

    private void deletePage() {
        Integer pageNumber = viewModel.getCurrentPage().getValue();
        if (pageNumber == 1 && !databaseHelper.hasPagesGreaterThan(pageNumber, journalName)) {
            databaseHelper.deletePage(pageNumber, journalName);
            databaseHelper.deleteJournal(journalName);
            startActivity(new Intent(EditPage.this, HomeScreen.class));
        } else {
            databaseHelper.deletePage(pageNumber, journalName);
            loadPage(pageNumber);
            if (pageNumber > 1) decrementPage();
        }
    }
}