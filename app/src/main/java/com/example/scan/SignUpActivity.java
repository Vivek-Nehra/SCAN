package com.example.scan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import java.util.regex.*;

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener{
    private FirebaseAuth mauth;
    private EditText signUpEmail, signUpPassword;
    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mauth = FirebaseAuth.getInstance();

        pd = new ProgressDialog(this);

        signUpEmail = findViewById(R.id.signUpEmail);
        signUpPassword = findViewById(R.id.signUpPassword);
        findViewById(R.id.login).setOnClickListener(this);
        findViewById(R.id.signUpBtn).setOnClickListener(this);
        findViewById(R.id.verifyEmail).setOnClickListener(this);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if (mauth.getCurrentUser().isAnonymous()){
            Log.d("Log" , "Deleting Activity -- Deleting Anonymous Account");
            deleteUser(mauth.getCurrentUser());
        }
    }

    @Override
    public void onClick(View v){
        if (v.getId() == R.id.login){
            Intent loginIntent = new Intent(this, MainActivity.class);
            startActivity(loginIntent);
        }
        if (v.getId() == R.id.signUpBtn){
            signUpUser(signUpEmail.getText().toString(), (signUpPassword.getText().toString()));
        }
        if (v.getId() == R.id.verifyEmail){
            verifyEmail(signUpEmail.getText().toString());
        }
    }

    private void toastMessage(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private void showSnackbar(String message){
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }

    public void verifyEmail(String email){
        if (!checkValidId(email)){
            return ;
        }
        pd.setTitle("Processing");
        pd.setMessage("Please Wait");
        pd.show();
        mauth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            final FirebaseUser user = mauth.getCurrentUser();
                            user.reload();
                            Log.d("Log", "Anonymously signed in... " + user.isAnonymous() );
                            if (user.isEmailVerified() == false) {
                                signUpEmail.setEnabled(false);
                                Log.d("Log", "Mail not verified " + user.getEmail());
                                user.updateEmail(signUpEmail.getText().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        Log.d("Log", "Mail not verified " + user.getEmail());
                                        if (task.isSuccessful()) {
                                            user.sendEmailVerification()    //todo : Check dynamic links for direct redirection back to app
                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            if (task.isSuccessful()) {
                                                                Log.d("Log", "Email sent to: " + user.getEmail());
                                                                showSnackbar("Verification mail sent to id. Please verify. ");
                                                                AsyncT waitForEmailVerification = new AsyncT();
                                                                waitForEmailVerification.execute();
                                                            }
                                                            else{
                                                                Exception e = task.getException();
                                                                Log.d( "Log", "Email not send", e);

                                                                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                                                                    signUpEmail.requestFocus();
                                                                    signUpEmail.setError("Invalid email address.");
                                                                }
                                                                else if (e instanceof FirebaseAuthUserCollisionException){
                                                                    signUpEmail.requestFocus();
                                                                    signUpEmail.setError("User with email id already exists");
                                                                } else{
                                                                    toastMessage("Authentication failed. Try Again Later.");
                                                                }
                                                                deleteUser(user);
                                                            }
                                                        }
                                                    });
                                            } else{
                                            Log.d("Log", "Cannot update email id", task.getException());
                                            toastMessage("Authentication failed. Try Again Later.");
                                            deleteUser(user);
                                        }
                                    }
                            });
                        } else {
                                Log.d("Log", "Mail already verified");
                                toastMessage("Email already verified");
                            }
                        }  else {
                            // If sign in fails, display a message to the user.
                            Log.d("Log", "signInAnonymously:failure", task.getException());
                            toastMessage("Authentication failed. Try Again Later.");
                        }
                        pd.dismiss();
                    }
                });
    }

    private void deleteUser(FirebaseUser user){
        user.delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d("Log", "Anonymous Account deleted");
                        }
                        else{
                            Log.d("Log", "Account not deleted", task.getException());
                        }
                    }
                });
        signUpEmail.setEnabled(true);
    }

    private boolean checkValidId(String email){
        if (!Pattern.matches("^\\w+@gmail.com$", email)){
            Log.d("Log", "Invalid Email address: " + email);
            signUpEmail.requestFocus();
            signUpEmail.setError("Email must have nitkkr domain id");
            return false;
        }
        return true;
    }

    private boolean checkValidPassword(String password){
        if (!Pattern.matches("^.{6,}", password)){
            Log.d("Log", "Invalid Password: " + password);
            signUpPassword.requestFocus();
            signUpPassword.setError("Password must have at least 6 characters");
            return false;
        }
        else if (!Pattern.matches("^[a-zA-Z0-9_@]{6,}", password)){
            Log.d("Log", "Invalid Password: " + password);
            signUpPassword.requestFocus();
            signUpPassword.setError("Password can contain only alpha-numeric characters");
            return false;
        }

        return true;
    }

    public void signUpUser(String email, String password){
        if (!mauth.getCurrentUser().isEmailVerified()){
            signUpEmail.requestFocus();
            signUpEmail.setError("Email not verified");
            return ;
        }

        if (!checkValidId(email) || !checkValidPassword(password)){
            return ;
        }

        pd.setTitle("Signing Up");
        pd.setMessage("Please Wait");
        pd.show();

        mauth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("Log", "createUserWithEmail:success");
                            FirebaseUser user = mauth.getCurrentUser();
                            Toast.makeText(getApplicationContext(), "Successfully Signed Up", Toast.LENGTH_LONG).show();
                            startActivity(new Intent(getApplicationContext(), MainActivity.class));     //todo : check startActivityForResult for automatic Sign In
                        } else {
                            // If sign in fails, display a message to the user.
                            try{
                                throw task.getException();
                            }
                            catch (FirebaseAuthUserCollisionException e){
                                signUpEmail.requestFocus();
                                signUpEmail.setError("User with this email id already exists");
                            }
                            catch (Exception e) {
                                Log.w("Log", "createUserWithEmail:failure", task.getException());
                            }
                        }

                        pd.dismiss();
                    }
                });
    }

    private class AsyncT extends AsyncTask<Void,Void,Void> {
        TextView verifyEmail = findViewById(R.id.verifyEmail);
        @Override
        protected void onPreExecute(){
            verifyEmail.setText("Waitinnn");    // todo : Try to add a spinning animation
            verifyEmail.setClickable(false);
        }

        @Override
        protected Void doInBackground(Void... params) {
            while (!mauth.getCurrentUser().isEmailVerified()){
                mauth.getCurrentUser().reload();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void params){
            showSnackbar("Email id has been verified");
            verifyEmail.setText("Verified");
        }
    }
}
