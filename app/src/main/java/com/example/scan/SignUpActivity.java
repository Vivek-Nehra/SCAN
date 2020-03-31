package com.example.scan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.scan.util.Watcher;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import java.util.regex.*;

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {
    private FirebaseAuth mauth;
    private EditText signUpEmail, signUpPassword;
    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mauth = FirebaseAuth.getInstance();
        pd = new ProgressDialog(this);
        pd.setCanceledOnTouchOutside(false);

        signUpEmail = findViewById(R.id.signUpEmail);
        signUpPassword = findViewById(R.id.signUpPassword);
        signUpEmail.addTextChangedListener(new Watcher(signUpEmail));
        signUpPassword.addTextChangedListener(new Watcher(signUpPassword));

        findViewById(R.id.login).setOnClickListener(this);
        findViewById(R.id.signUpBtn).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.login) {
            Intent loginIntent = new Intent(this, MainActivity.class);
            startActivity(loginIntent);
        }
        if (v.getId() == R.id.signUpBtn) {
            signUpUser(signUpEmail.getText().toString(), (signUpPassword.getText().toString()));
        }
    }

    private void toastMessage(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }

    private boolean checkValidId(String email) {
        if (!Pattern.matches("^\\w+@nitkkr.ac.in$", email)) {
            Log.d("Log", "Invalid Email address: " + email);
            signUpEmail.requestFocus();
            signUpEmail.setError("Email must have nitkkr domain id");
            return false;
        }
        return true;
    }

    private boolean checkValidPassword(String password) {
        if (!Pattern.matches("^.{6,}", password)) {
            Log.d("Log", "Invalid Password: " + password);
            signUpPassword.requestFocus();
            signUpPassword.setError("Password must have at least 6 characters");
            return false;
        } else if (!Pattern.matches("^[a-zA-Z0-9_@]{6,}", password)) {
            Log.d("Log", "Invalid Password: " + password);
            signUpPassword.requestFocus();
            signUpPassword.setError("Password can contain only alpha-numeric characters");
            return false;
        }

        return true;
    }

    public void signUpUser(String email, String password) {
        if (!checkValidId(email) || !checkValidPassword(password)) {
            return;
        }

        pd.setTitle("Signing Up");
        pd.setMessage("Please Wait");
        pd.show();  //todo : check startActivityForResult for automatic Sign In

        mauth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            final FirebaseUser user = mauth.getCurrentUser();
                            Log.d("Log", " signed in... ");
                            if (user != null && !user.isEmailVerified()) {
                                signUpEmail.setEnabled(false);
                                Log.d("Log", "Mail not verified " + user.getEmail());
                                user.sendEmailVerification()    //todo : Check dynamic links for direct redirection back to app
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Log.d("Log", "Email sent to: " + user.getEmail());
                                                    toastMessage("Verification mail sent to id. Please verify. ");
                                                    pd.dismiss();
                                                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                                } else {
                                                    Exception e = task.getException();
                                                    Log.d("Log", "Email not send", e);
                                                }
                                            }
                                        });
                            }
                        } else {
                            Exception e = task.getException();
                            Log.d("Log", "Email not send", e);
                            if (e instanceof FirebaseAuthInvalidCredentialsException) {
                                signUpEmail.requestFocus();
                                signUpEmail.setError("Invalid email address.");
                            } else if (e instanceof FirebaseAuthUserCollisionException) {
                                signUpEmail.requestFocus();
                                signUpEmail.setError("User with email id already exists");
                            } else {
                                toastMessage("Authentication failed. Try Again Later.");
                            }
                        }
                        pd.dismiss();
                    }
                });
    }
}