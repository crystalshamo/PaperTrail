package com.example.papertrail;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class HomeScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_screen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Button goToEditPageBtn = findViewById(R.id.goToEditPageBtn);
        goToEditPageBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick (View v){
                // Create an Intent to start SecondActivity
                Intent intent = new Intent(HomeScreen.this, EditPage.class);

                // Start the new activity
                startActivity(intent);
            }
        });
    }

}