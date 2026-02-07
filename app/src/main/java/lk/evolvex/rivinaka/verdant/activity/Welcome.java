package lk.evolvex.rivinaka.verdant.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.VideoView; // Import added

import androidx.appcompat.app.AppCompatActivity;

import lk.evolvex.rivinaka.verdant.R;

public class Welcome extends AppCompatActivity {

    // 1. Declare both variables here
    private Button getStartedBtn;
    private VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // 2. Link Views
        // Note: Make sure this ID matches your XML (I used btnGetStarted in the design)
        getStartedBtn = findViewById(R.id.getStartBtn);
        videoView = findViewById(R.id.videoView);

        // 3. Button Click Listener
        getStartedBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Welcome.this, SignIn.class);
            startActivity(intent);
            finish();
        });

        // 4. Video Logic
        // Ensure "plant_orbit.mp4" exists in the "res/raw" folder
        String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.plant_orbit;
        Uri uri = Uri.parse(videoPath);
        videoView.setVideoURI(uri);

        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(true);

            // Calculate scaling to fill screen (Center Crop effect)
            float videoRatio = mp.getVideoWidth() / (float) mp.getVideoHeight();
            float screenRatio = videoView.getWidth() / (float) videoView.getHeight();
            float scaleX = videoRatio / screenRatio;

            if (scaleX >= 1f) {
                videoView.setScaleX(scaleX);
            } else {
                videoView.setScaleY(1f / scaleX);
            }
        });

        videoView.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if videoView is initialized to prevent crashes
        if (videoView != null) {
            videoView.start();
        }
    }
}
