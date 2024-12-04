package com.example.papertrail;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class EditPage extends AppCompatActivity  {
    private EditText lastFocusedEditText = null; // Track the last focused EditText


    private LinearLayout imageToolbar;
    private String journalName;
    private String pageNumberText;
    private EditPageViewModel viewModel;
    private DatabaseHelper databaseHelper;
    private PopupWindow textEditToolbar;
    private GestureDetector gestureDetector;

    private SeekBar redSeekBar, greenSeekBar, blueSeekBar;
    private TextView redTextView, greenTextView, blueTextView;
    private View popupView;
    private PopupWindow popupWindow;


    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int CROP_IMAGE_REQUEST = 3;


    private FrameLayout frameLayout;
    private List<View> movableViews = new ArrayList<>();
    private TextView pageNumberTv;
    private Button prevButton, captureButton, nextButton, addPageButton, deletePageButton, paintButton;
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
        prevButton = findViewById(R.id.buttonPrev);
        nextButton = findViewById(R.id.buttonNext);
        pageNumberTv = findViewById(R.id.pageNumberTv);
        frameLayout = findViewById(R.id.pageLayout);
        captureButton = findViewById(R.id.buttonSavePage);
        addPageButton = findViewById(R.id.buttonAddPage);
        deletePageButton = findViewById(R.id.buttonDeletePage);
        paintButton = findViewById(R.id.buttonPaint);

        imageToolbar = findViewById(R.id.imageToolbar);
        findViewById(R.id.btnDelete).setOnClickListener(v -> deleteImage());
        findViewById(R.id.btnRotateLeft).setOnClickListener(v -> rotateImage(-45));  // Rotate left (counterclockwise)
        findViewById(R.id.btnRotateRight).setOnClickListener(v -> rotateImage(45));  // Rotate right (clockwise)

        prevButton.setOnClickListener(v -> decrementPage());
        nextButton.setOnClickListener(v -> incrementPage());
        addPageButton.setOnClickListener(v -> incrementPage());
        captureButton.setOnClickListener(v -> savePage());
        deletePageButton.setOnClickListener(v -> deletePage());
        paintButton.setOnClickListener(v -> showColorPickerPopup());


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

        // Set up GestureDetector for this sticker
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                Log.d("DoubleTap", "Double-tap detected on sticker!");
                showImageToolbar(stickerView); // Show the same toolbar as images
                return true;
            }
        });

        // Attach touch listener for dragging and gestures
        stickerView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event); // Handle gesture events
            scaleGestureDetector.onTouchEvent(event); // Handle scaling

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

        // Add the sticker to the layout and track it
        frameLayout.addView(stickerView);
        movableViews.add(stickerView);
    }



    @SuppressLint("ClickableViewAccessibility")
    private void addImageToPage(Bitmap bitmap) {
        // Create ImageView to display the image
        ImageView imageViewPage = new ImageView(this);
        imageViewPage.setImageBitmap(bitmap);
        imageViewPage.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        imageViewPage.setX(100); // Initial X position
        imageViewPage.setY(100); // Initial Y position

        // Set up GestureDetector for double tap detection
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                Log.d("DoubleTap", "Double-tap detected!");
                showImageToolbar(imageViewPage);  // Show toolbar on double tap
                return true;
            }
        });

        // Set up the touch listener for drag and scaling
        imageViewPage.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event); // Detect double-tap event
            scaleGestureDetector.onTouchEvent(event); // Comment this line to debug double-tap behavior

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    // Store initial touch position for dragging
                    initialX = event.getRawX() - v.getX();
                    initialY = event.getRawY() - v.getY();
                    selectedImageView = (ImageView) v;
                    break;

                case MotionEvent.ACTION_MOVE:
                    // Handle image movement (dragging)
                    v.setX(event.getRawX() - initialX);
                    v.setY(event.getRawY() - initialY);
                    break;
            }
            return true; // Allow the event to propagate
        });

        // Add the ImageView to the layout and track it
        frameLayout.addView(imageViewPage);
        movableViews.add(imageViewPage);

        frameLayout.setOnTouchListener((v, event) -> {
            // Hide the toolbar if the touch is outside the imageView or toolbar
            if (imageToolbar.getVisibility() == View.VISIBLE
                    && !isTouchInsideView(imageViewPage, event)
                    && !isTouchInsideView(imageToolbar, event)) {
                imageToolbar.setVisibility(View.GONE);
                Log.d("ToolbarPosition", "Toolbar hidden because touch is outside.");
            }
            return false; // Allow other touch events to propagate
        });
    }

    private boolean isTouchInsideView(View view, MotionEvent event) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);

        int left = location[0];
        int top = location[1];
        int right = left + view.getWidth();
        int bottom = top + view.getHeight();

        // Check if the touch event coordinates are inside the view's boundaries
        return event.getRawX() >= left && event.getRawX() <= right && event.getRawY() >= top && event.getRawY() <= bottom;
    }

    private void showImageToolbar(ImageView imageView) {
        selectedImageView = imageView;

        // Show the toolbar above the image
        imageToolbar.setVisibility(View.VISIBLE);
        Log.d("ToolbarPosition", "Toolbar Visibility: " + imageToolbar.getVisibility());

        // Proceed with calculating the position
        int toolbarWidth = imageToolbar.getWidth();
        int toolbarHeight = imageToolbar.getHeight();

        // If width/height is zero, measure the toolbar
        if (toolbarWidth == 0 || toolbarHeight == 0) {
            imageToolbar.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            toolbarWidth = imageToolbar.getMeasuredWidth();
            toolbarHeight = imageToolbar.getMeasuredHeight();
        }

        // Log dimensions for debugging
        Log.d("ToolbarPosition", "Toolbar Width: " + toolbarWidth + ", Height: " + toolbarHeight);

        // Calculate Y position for toolbar (from the bottom of the screen)
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        float density = getResources().getDisplayMetrics().density;
        int offsetFromBottom = (int) (150 * density);  // 150dp in pixels
        int yOffset = screenHeight - offsetFromBottom - toolbarHeight;

        // Log Y offset
        Log.d("ToolbarPosition", "Toolbar Y Offset: " + yOffset);

        // Calculate X position to center toolbar
        int xOffset = (getResources().getDisplayMetrics().widthPixels - toolbarWidth) / 2;

        // Log X offset
        Log.d("ToolbarPosition", "Toolbar X Offset: " + xOffset);

        // Set the position of the toolbar
        imageToolbar.post(() -> {
            imageToolbar.setX(xOffset);
            imageToolbar.setY(yOffset);
            imageToolbar.requestLayout();  // Force layout update
            imageToolbar.invalidate();     // Force redraw
            Log.d("ToolbarPosition", "Toolbar positioned at X: " + xOffset + ", Y: " + yOffset);
        });
    }

    private void showColorPickerPopup() {
        // Create an AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Color");

        // Inflate the custom layout for the RGB SeekBars
        View popupView = getLayoutInflater().inflate(R.layout.color_seekbar, null);
        builder.setView(popupView);

        // Initialize the SeekBars and TextViews
        SeekBar redSeekBar = popupView.findViewById(R.id.redSB);
        SeekBar greenSeekBar = popupView.findViewById(R.id.greenSB);
        SeekBar blueSeekBar = popupView.findViewById(R.id.blueSB);

        TextView redTextView = popupView.findViewById(R.id.redTV);
        TextView greenTextView = popupView.findViewById(R.id.greenTV);
        TextView blueTextView = popupView.findViewById(R.id.blueTV);


        // Create the dialog
        AlertDialog dialog = builder.create();

        // Set up SeekBar listeners, passing the initialized views as parameters
        setupSeekBarListeners(redSeekBar, greenSeekBar, blueSeekBar, redTextView, greenTextView, blueTextView, frameLayout);

        // Show the dialog
        dialog.show();
    }

    private void setupSeekBarListeners(SeekBar redSeekBar, SeekBar greenSeekBar, SeekBar blueSeekBar,
                                       TextView redTextView, TextView greenTextView, TextView blueTextView,
                                       FrameLayout frameLayout) {

        // Define listener to update the color dynamically
        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Update RGB TextViews
                redTextView.setText(String.valueOf(redSeekBar.getProgress()));
                greenTextView.setText(String.valueOf(greenSeekBar.getProgress()));
                blueTextView.setText(String.valueOf(blueSeekBar.getProgress()));

                // Calculate combined color
                int red = redSeekBar.getProgress();
                int green = greenSeekBar.getProgress();
                int blue = blueSeekBar.getProgress();
                int color = Color.rgb(red, green, blue);


                frameLayout.setBackgroundColor(color);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        // Attach listener to all SeekBars
        redSeekBar.setOnSeekBarChangeListener(listener);
        greenSeekBar.setOnSeekBarChangeListener(listener);
        blueSeekBar.setOnSeekBarChangeListener(listener);
    }


    private void deleteImage() {
        if (selectedImageView != null) {
            // Remove the image from the parent layout
            ((FrameLayout) selectedImageView.getParent()).removeView(selectedImageView);
            imageToolbar.setVisibility(View.GONE);  // Hide the toolbar
        }
    }

    private void rotateImage(float angle) {
        if (selectedImageView != null) {
            // Rotate the selected image by the given angle
            selectedImageView.setRotation(selectedImageView.getRotation() + angle);
        }
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
        // In the activity where you update the cover image
        SharedPreferences sharedPreferences = getSharedPreferences("cover_update", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("cover_updated", true);  // Set the flag to true
        editor.apply();

        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
    }

    private void decrementPage() {
        viewModel.decrementPage();
    }

    private void incrementPage() {
        viewModel.incrementPage();
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
   public void showFontPicker() {
        String[] fontNames = {
                "Alegreya Sans Medium", "Alex Brush", "Christmas", "Fjalla One Regular", "Geomanist Regular",
                "Geomanist Regular Italic", "Montserrat Alternates Italic", "Montserrat Bold", "Montserrat Medium",
                "Playfair Display Bold", "Playfair Display Regular", "Reef"
        };


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


        new AlertDialog.Builder(this)
                .setTitle("Select a Font\n")
                .setAdapter(fontAdapter, (dialog, which) -> {

                    // Apply the selected font (if you need to handle it)
                    applyFont(fontNames[which]);
                })
                .show();
    }



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

        LinearLayout toolbarLayout = new LinearLayout(this);
        toolbarLayout.setOrientation(LinearLayout.VERTICAL);
        toolbarLayout.setBackgroundColor(Color.parseColor("#AA000000"));
        toolbarLayout.setPadding(16, 16, 16, 16);

        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setWeightSum(4);  // Distribute space equally among the 4 buttons

        // Create buttons with black background and white text (No circular background)
        Button btnIncrease = createToolbarButton("A+");
        btnIncrease.setOnClickListener(v -> {
            float currentSize = editText.getTextSize();
            editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, currentSize + 2);
            editText.requestFocus();
            showKeyboard(editText);
        });
        buttonLayout.addView(btnIncrease);

        Button btnDecrease = createToolbarButton("A-");
        btnDecrease.setOnClickListener(v -> {
            float currentSize = editText.getTextSize();
            if (currentSize > 12) {
                editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, currentSize - 2);
            }
            editText.requestFocus();
            showKeyboard(editText);
        });
        buttonLayout.addView(btnDecrease);

        Button btnBold = createToolbarButton("B");
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

        // Font button
        Button btnFont = createToolbarButton("F");
        btnFont.setOnClickListener(v -> showFontPicker());
        buttonLayout.addView(btnFont);

        // Add color palette (center aligned)
        LinearLayout colorPalette = new LinearLayout(this);
        colorPalette.setOrientation(LinearLayout.HORIZONTAL);
        colorPalette.setPadding(8, 8, 8, 8);

        // Center the color palette within the toolbar
        LinearLayout.LayoutParams colorPaletteParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        colorPaletteParams.gravity = Gravity.CENTER_HORIZONTAL;  // Center the palette within the toolbar
        colorPalette.setLayoutParams(colorPaletteParams);

        String[] hexColors = {
                "#FF0000", "#FFA500", "#FFFF00", "#008000", "#0000FF",
                "#800080", "#FFC0CB", "#000000", "#FFFFFF"
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

    // Helper method to create a toolbar button with equal size and black background
    private Button createToolbarButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);  // Set text color to white
        button.setBackgroundColor(Color.parseColor("#000000")); // Set background color to black
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);  // Use weight to distribute space equally
        params.setMargins(8, 8, 8, 8);  // Add margins around buttons for spacing
        button.setLayoutParams(params);
        return button;
    }


    private void showKeyboard(EditText editText) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }
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
}