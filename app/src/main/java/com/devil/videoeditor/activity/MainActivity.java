package com.devil.videoeditor.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.devil.videoeditor.R;


public class MainActivity extends AppCompatActivity {
    ImageView iv1, iv2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv1 = findViewById(R.id.imageView);
        iv2 = findViewById(R.id.imageView2);
        iv1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchActivity1();
            }
        });
        iv2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchActivity2();
            }
        });
    }

    private void launchActivity1() {
        Intent click1 = new Intent(this, CutVideoActivity.class);
        startActivity(click1);
    }

    private void launchActivity2() {
        Intent click1 = new Intent(this, ExtractAudioActivity.class);
        startActivity(click1);
    }
}