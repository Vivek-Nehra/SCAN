package com.example.scan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.scan.util.Watcher;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mauth;
    private ProgressDialog pd;
    private Toast toast = null;
    private boolean doublePressToExit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mauth = FirebaseAuth.getInstance();

        pd = new ProgressDialog(this);
        pd.setCanceledOnTouchOutside(false);
        pd.setTitle("Signing In");
        pd.setMessage("Please Wait");

        toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);

        final Button loginBtn = findViewById(R.id.loginBtn);
        final EditText loginId = findViewById(R.id.logInEmail);
        final EditText loginPassword = findViewById(R.id.logInPassword);

        loginId.addTextChangedListener(new Watcher(loginId));
        loginPassword.addTextChangedListener(new Watcher(loginPassword));

        findViewById(R.id.togglePassword).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (loginPassword.getTransformationMethod() instanceof  PasswordTransformationMethod) {
                    loginPassword.setTransformationMethod(new SingleLineTransformationMethod());
                    ((ImageView)findViewById(R.id.togglePassword)).setImageResource(R.drawable.baseline_visibility_off_24);
                }
                else {
                    loginPassword.setTransformationMethod(new PasswordTransformationMethod());
                    ((ImageView)findViewById(R.id.togglePassword)).setImageResource(R.drawable.baseline_visibility_24);
                }
                loginPassword.setSelection(loginPassword.getText().length());
        }});

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = loginId.getText().toString();
                String password = loginPassword.getText().toString();

                if (email.isEmpty()){
                    loginId.requestFocus();
                    loginId.setError("Enter email id");
                    return;
                }
                if (password.isEmpty()){
                    loginPassword.requestFocus();
                    loginPassword.setError("Enter Password ");
                    return;
                }

                pd.show();

                mauth.signInWithEmailAndPassword(email, password).addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d("Log", "SignIn Success");
                            if (mauth.getCurrentUser() != null && !mauth.getCurrentUser().isEmailVerified()) {
                                Toast.makeText(getApplicationContext(), "Please verify Email", Toast.LENGTH_SHORT).show();
                                pd.dismiss();
                                return;
                            } else {
                                startActivity(new Intent(getApplicationContext(), LoggedInActivity.class));
                                Toast.makeText(getApplicationContext(), "Welcome " + mauth.getCurrentUser().getDisplayName() + " !", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.d("Log", "Login Error: ", task.getException());
                            if ( task.getException() instanceof FirebaseAuthInvalidUserException){
                                loginId.setError("User does not exist");
                                loginId.requestFocus();
                                Toast.makeText(getApplicationContext(), "User id does not exist. Please sign up first",
                                        Toast.LENGTH_SHORT).show();
                            }
                            else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException){
                                Toast.makeText(getApplicationContext(), "Invalid Id or password. Please try again",
                                        Toast.LENGTH_SHORT).show();
                            }
                            else {
                                Toast.makeText(getApplicationContext(), "Authentication failed. Please try again later",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                        pd.dismiss();
                        loginId.clearFocus();
                        loginPassword.clearFocus();
                    }
                });
            }
        });

        final Button signUp = findViewById(R.id.signUp);
        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent signUpIntent = new Intent(getApplicationContext(), SignUpActivity.class);
                startActivity(signUpIntent);
            }
        });
    }

    @Override
    public void onStart(){      // todo: Add a splash screen on startup
        super.onStart();

        if(mauth.getCurrentUser() != null && mauth.getCurrentUser().isEmailVerified()){
            Log.d("Log", "Logging User in : " + mauth.getCurrentUser().getDisplayName() + " " + mauth.getCurrentUser().getEmail());
            startActivity(new Intent(getApplicationContext(), LoggedInActivity.class));
        }
    }

    @Override
    public void onBackPressed(){        // todo: Check working after adding multiple activites to the activity stack
        if (doublePressToExit){
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        doublePressToExit = true;
        toast.setText("Press again to exit.");
        toast.show();
        new Handler().postDelayed(new Runnable(){
            @Override
            public void run(){
                doublePressToExit = false;
            }
        }, 2000);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        toast.cancel();
    }
}