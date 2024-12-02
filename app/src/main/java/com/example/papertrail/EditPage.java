package com.example.papertrail;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class EditPage extends AppCompatActivity {
    private Button btnIncreaseTextSize, btnDecreaseTextSize, btnToggleBold;
    private EditText lastFocusedEditText = null; // Track the last focused EditText
    // Track the last focused EditText
    private String journalName;
    private String pageNumberText;
    private DatabaseHelper databaseHelper;
    private PopupWindow textEditToolbar;

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;

    private FrameLayout frameLayout;
    private List<View> movableViews = new ArrayList<>();
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
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int after) {
                loadPage();
                loadArrows();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
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
                case 1:
                    showImagePickerDialog();
                    break;
                case 2:
                    addMovableEditText();
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

        if (databaseHelper.hasPagesLessThan(pageNumber, journalName)) {
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
                FrameLayout.LayoutParams.WRAP_CONTENT
        ));
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

        // Add the sticker to the FrameLayout
        frameLayout.addView(stickerView);

        // Track the ImageView in the generalized list
        movableViews.add(stickerView);
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
        movableViews.add(imageViewPage);
    }

    private void savePage() {
        pageNumberText = pageNumberTv.getText().toString();
        int pageNumber = Integer.parseInt(pageNumberText);

        frameLayout.setDrawingCacheEnabled(false);
        frameLayout.buildDrawingCache();
        bm = Bitmap.createBitmap(frameLayout.getDrawingCache());
        frameLayout.setDrawingCacheEnabled(false);

        databaseHelper.saveImageToPageTable(journalName, pageNumber, bm);
        // In the activity where you update the cover image
        SharedPreferences sharedPreferences = getSharedPreferences("cover_update", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("cover_updated", true);  // Set the flag to true
        editor.apply();

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


    private void addMovableEditText() {
        // Clear focus from the last focused EditText
        if (lastFocusedEditText != null) {
            lastFocusedEditText.clearFocus();
        }

        EditText editText = new EditText(this);
        editText.setHint("Type here...");
        editText.setBackgroundColor(Color.TRANSPARENT);
        editText.setTextSize(16);
        editText.setTextColor(Color.BLACK);
        editText.setFocusableInTouchMode(true);
        editText.setClickable(true);
        editText.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        ));
        editText.setX(100);
        editText.setY(100);

        // Add touch listener for dragging
        editText.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = event.getRawX() - v.getX();
                    initialY = event.getRawY() - v.getY();
                    // Request focus when touched
                    editText.requestFocus();
                    break;
                case MotionEvent.ACTION_MOVE:
                    v.setX(event.getRawX() - initialX);
                    v.setY(event.getRawY() - initialY);
                    break;
            }
            return false; // Allow event to propagate
        });

        editText.setOnClickListener(v -> {
            lastFocusedEditText = editText; // Track the focused EditText
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);

            // Show the toolbar again when clicked
            showTextEditToolbar(editText);
        });

        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showTextEditToolbar(editText); // Show toolbar for text adjustments
            } else {
                dismissTextEditToolbar(); // Dismiss toolbar when focus is lost
            }
        });


        // Add the EditText to the FrameLayout
        frameLayout.addView(editText);
        movableViews.add(editText);

        lastFocusedEditText = editText; // Update last focused EditText
    }

    private void dismissTextEditToolbar() {
        if (textEditToolbar != null) {
            textEditToolbar.dismiss();
            textEditToolbar = null; // Set to null to prevent reuse of a dismissed toolbar
        }
    }

    private void showFontPicker() {
        String[] fontNames = {
                "Alegreya Sans Medium", "Alex Brush", "Christmas", "Fjalla One Regular", "Geomanist Regular",
                "Geomanist Regular Italic", "Montserrat Alternates Italic", "Montserrat Bold", "Montserrat Medium",
                "Playfair Display Bold", "Playfair Display Regular", "Reef"
        };

        // Create an ArrayAdapter
        ArrayAdapter<String> fontAdapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_item, fontNames) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);

                switch (position) {
                    case 0:
                        textView.setTypeface(ResourcesCompat.getFont(EditPage.this, R.font.alegreyaaansmedium));
                        break;
                    case 1:
                        textView.setTypeface(ResourcesCompat.getFont(EditPage.this, R.font.alexbrush));
                        break;
                    case 2:
                        textView.setTypeface(ResourcesCompat.getFont(EditPage.this, R.font.chfont));
                        break;
                    case 3:
                        textView.setTypeface(ResourcesCompat.getFont(EditPage.this, R.font.fjallaoneregular));
                        break;
                    case 4:
                        textView.setTypeface(ResourcesCompat.getFont(EditPage.this, R.font.geomanistregular));
                        break;
                    case 5:
                        textView.setTypeface(ResourcesCompat.getFont(EditPage.this, R.font.geomanistregularitalic));
                        break;
                    case 6:
                        textView.setTypeface(ResourcesCompat.getFont(EditPage.this, R.font.montserratalternatesitalic));
                        break;
                    case 7:
                        textView.setTypeface(ResourcesCompat.getFont(EditPage.this, R.font.montserratbold));
                        break;
                    case 8:
                        textView.setTypeface(ResourcesCompat.getFont(EditPage.this, R.font.montserratmedium));
                        break;
                    case 9:
                        textView.setTypeface(ResourcesCompat.getFont(EditPage.this, R.font.playfairdisplaybold));
                        break;
                    case 10:
                        textView.setTypeface(ResourcesCompat.getFont(EditPage.this, R.font.playfairdisplayregular));
                        break;
                    case 11:
                        textView.setTypeface(ResourcesCompat.getFont(EditPage.this, R.font.reef));
                        break;
                    default:
                        break;
                }
                return textView;
            }
        };

        // Show the font picker dialog
        new AlertDialog.Builder(this)
                .setTitle("Select a Font\n")
                .setAdapter(fontAdapter, (dialog, which) -> {
                    // Apply the selected font (if you need to handle it)
                    applyFont(fontNames[which]);
                })
                .show();
    }


    //text changing to font
    private void applyFont(String fontName) {
        if (lastFocusedEditText != null) {
            int fontResId = 0;

            switch (fontName) {
                case "Alegreya Sans Medium":
                    fontResId = R.font.alegreyaaansmedium;
                    break;
                case "Alex Brush":
                    fontResId = R.font.alexbrush;
                    break;
                case "Christmas":
                    fontResId = R.font.chfont;
                    break;
                case "Fjalla One Regular":
                    fontResId = R.font.fjallaoneregular;
                    break;
                case "Geomanist Regular":
                    fontResId = R.font.geomanistregular;
                    break;
                case "Geomanist Regular Italic":
                    fontResId = R.font.geomanistregularitalic;
                    break;
                case "Montserrat Alternates Italic":
                    fontResId = R.font.montserratalternatesitalic;
                    break;
                case "Montserrat Bold":
                    fontResId = R.font.montserratbold;
                    break;
                case "Montserrat Medium":
                    fontResId = R.font.montserratmedium;
                    break;
                case "Playfair Display Bold":
                    fontResId = R.font.playfairdisplaybold;
                    break;
                case "Playfair Display Regular":
                    fontResId = R.font.playfairdisplayregular;
                    break;
                case "Reef":
                    fontResId = R.font.reef;
                    break;
                default:
                    Toast.makeText(this, "Font not found!", Toast.LENGTH_SHORT).show();
                    return;
            }

            // Load and apply the font
            Typeface typeface = ResourcesCompat.getFont(this, fontResId);
            if (typeface != null) {
                lastFocusedEditText.setTypeface(typeface);
            } else {
                Toast.makeText(this, "Failed to load font!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showTextEditToolbar(EditText editText) {
        if (textEditToolbar != null) {
            textEditToolbar.dismiss();
        }

        // Create the main toolbar layout
        LinearLayout toolbarLayout = new LinearLayout(this);
        toolbarLayout.setOrientation(LinearLayout.VERTICAL);
        toolbarLayout.setBackgroundColor(Color.LTGRAY);
        toolbarLayout.setPadding(16, 16, 16, 16);

        // Add a horizontal layout for buttons
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);

        // Add buttons for size and bold adjustments
        Button btnIncrease = new Button(this);
        btnIncrease.setText("A+");
        btnIncrease.setOnClickListener(v -> {
            float currentSize = editText.getTextSize();
            editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, currentSize + 2);
            editText.requestFocus();
            showKeyboard(editText);
        });
        buttonLayout.addView(btnIncrease);

        Button btnDecrease = new Button(this);
        btnDecrease.setText("A-");
        btnDecrease.setOnClickListener(v -> {
            float currentSize = editText.getTextSize();
            if (currentSize > 12) {
                editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, currentSize - 2);
            }
            editText.requestFocus();
            showKeyboard(editText);
        });
        buttonLayout.addView(btnDecrease);

        Button btnBold = new Button(this);
        btnBold.setText("B");
        btnBold.setOnClickListener(v -> {
            if (editText.getTypeface() != null) {
                if (editText.getTypeface().isBold()) {
                    editText.setTypeface(Typeface.create(editText.getTypeface(), Typeface.NORMAL));
                } else {
                    editText.setTypeface(Typeface.create(editText.getTypeface(), Typeface.BOLD));
                }
            } else {
                editText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            }
            editText.requestFocus();
            showKeyboard(editText);
        });
        buttonLayout.addView(btnBold);

        //font button
        Button btnFont = new Button(this);
        btnFont.setText("F");
        btnFont.setOnClickListener(v -> showFontPicker());
        buttonLayout.addView(btnFont);

        // Add color palette (horizontal layout for colors)
        LinearLayout colorPalette = new LinearLayout(this);
        colorPalette.setOrientation(LinearLayout.HORIZONTAL);
        colorPalette.setPadding(8, 8, 8, 8);


        String[] hexColors = {
                "#FF0000",
                "#FFA500",
                "#FFFF00",
                "#008000",
                "#0000FF",
                "#800080",
                "#FFC0CB",
                "#000000",
                "#FFFFFF"
        };
        for (String hex : hexColors) {
            View colorButton = new View(this);
            colorButton.setBackgroundColor(Color.parseColor(hex));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(50, 50); // Tiny squares
            params.setMargins(8, 8, 8, 8);
            colorButton.setLayoutParams(params);
            colorButton.setOnClickListener(v -> {
                editText.setTextColor(Color.parseColor(hex));
                editText.requestFocus();
                showKeyboard(editText);
            });
            colorPalette.addView(colorButton);
        }

        // Add the color palette above the buttons
        toolbarLayout.addView(colorPalette);
        toolbarLayout.addView(buttonLayout);

        // Create and configure the PopupWindow
        textEditToolbar = new PopupWindow(toolbarLayout, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        textEditToolbar.setOutsideTouchable(true);
        textEditToolbar.setFocusable(false);
        textEditToolbar.setTouchable(true);
        textEditToolbar.setBackgroundDrawable(null);

        // Dismiss toolbar on outside touch
        textEditToolbar.setOnDismissListener(() -> {
            if (lastFocusedEditText != null) {
                lastFocusedEditText.clearFocus();
            }
        });

        // Calculate the position for the toolbar
        int[] location = new int[2];
        editText.getLocationOnScreen(location);

        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int toolbarHeight = 150; // Approximate height of the toolbar in pixels
        int yOffset;

        // Determine whether to show the toolbar above or below the EditText
        if (location[1] + editText.getHeight() + toolbarHeight > screenHeight) {
            // Not enough space below, show above
            yOffset = location[1] - toolbarHeight;
        } else {
            // Show below
            yOffset = location[1] + editText.getHeight();
        }

        // Show the toolbar near the EditText
        textEditToolbar.showAtLocation(editText, Gravity.NO_GRAVITY, location[0], yOffset);

        // Ensure the EditText retains focus
        editText.requestFocus();
        showKeyboard(editText);

        // Dismiss the toolbar when the EditText loses focus
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                dismissTextEditToolbar();
            }
        });
    }

    private void showKeyboard(EditText editText) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }
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