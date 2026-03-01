package lk.evolvex.rivinaka.verdant.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import lk.evolvex.rivinaka.verdant.R;

public class SignUp extends AppCompatActivity {

    private TextView moveSignIn;
    private Button signUpBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        moveSignIn = findViewById(R.id.tvSignIn);
        signUpBtn= findViewById(R.id.btnSignUp);

        moveSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(SignUp.this, SignIn.class);
            startActivity(intent);
            finish();
        });

        signUpBtn.setOnClickListener(v->{
            Intent intent = new Intent(SignUp.this, MainHome.class);
            startActivity(intent);
            finish();
        });
    }
}