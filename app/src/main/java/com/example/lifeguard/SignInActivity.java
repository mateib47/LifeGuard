package com.example.lifeguard;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.lifeguard.Api.RetrofitClient;
import com.example.lifeguard.Api.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignInActivity extends AppCompatActivity {
    GoogleSignInClient mGoogleSignInClient;
    private TextView mStatusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signin_main);
        mStatusTextView = findViewById(R.id.status);

        //todo add all scopes
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope("https://www.googleapis.com/auth/fitness.activity.read"),
                        new Scope("https://www.googleapis.com/auth/user.phonenumbers.read"),
                        new Scope("https://www.googleapis.com/auth/fitness.location.read"),
                        new Scope("https://www.googleapis.com/auth/fitness.nutrition.read"))
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        showPermission(android.Manifest.permission.READ_SMS);
        showPermission(android.Manifest.permission.READ_PHONE_STATE);

        ((SignInButton) findViewById(R.id.sign_in_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });
        ((Button) findViewById(R.id.sign_out_and_disconnect)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOut();
            }
        });
        ((Button) findViewById(R.id.home_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchActivities();
            }
        });


        int REQUEST_CODE_ASK_PERMISSIONS = 123;
        ActivityCompat.requestPermissions(this, new String[]{"android.permission.READ_SMS, android.permission.ACTIVITY_RECOGNITION"}, REQUEST_CODE_ASK_PERMISSIONS);
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
//                    != PackageManager.PERMISSION_GRANTED) {
//                Toast toast = Toast.makeText(getApplicationContext(), "The app needs this permission to run properly...", Toast.LENGTH_LONG);
//                toast.show();
//            }
        //not enough info on health connect for java (api released 1 month ago)
//        if (HealthConnectClient.isAvailable(getApplicationContext())) {
//            // Health Connect is available and installed.
//            val healthConnectClient = HealthConnectClient.getOrCreate(context)
//        } else {
//            // ...
//        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        someActivityResultLauncher.launch(signInIntent);
    }

    ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                System.out.println("result code" + result.getResultCode());
                if (result.getResultCode() == RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    handleSignInResult(task);
                }
            });

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            TelephonyManager tMgr = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            String mPhoneNumber = tMgr.getLine1Number();
            if (account != null)
                addUser(account.getGivenName(), account.getFamilyName(), account.getEmail(), mPhoneNumber);
            System.out.println("signed in");
            System.out.println(account.getDisplayName());
            updateUI(account);
            switchActivities();
        } catch (ApiException e) {
            System.out.println("Error" + e.getStatusCode());
            updateUI(null);
        }
    }

    private void showPermission(String permission) {
        int permissionCheck = ContextCompat.checkSelfPermission(
                this, permission);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    permission)) {
                //TODO show why is needed
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    ActivityCompat.requestPermissions(this, new String[]{permission}, 225);
                }
            }
        } else {
            Toast.makeText(this, "Permission (already) Granted!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        updateUI(account);
    }

    private void switchActivities() {
        Intent switchActivityIntent = new Intent(this, MainActivity.class);
        startActivity(switchActivityIntent);
    }

    private void updateUI(@Nullable GoogleSignInAccount account) {
        if (account != null) {
            mStatusTextView.setText("Signed in: " + account.getDisplayName());
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);
        } else {
            mStatusTextView.setText("Signed out");
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
            findViewById(R.id.home_btn).setVisibility(View.GONE);
        }
    }

    private void signOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        updateUI(null);
                    }
                });
    }

    public void addUser(String firstName,
                        String lastName,
                        String email,
                        String phoneNumber) {
        User user = new User(firstName, lastName, email, phoneNumber);
        Call<Long> call = RetrofitClient.getInstance().getMyApi().addUser(user);
        call.enqueue(new Callback<Long>() {
            @Override
            public void onResponse(Call<Long> call, Response<Long> response) {
                Toast.makeText(getApplicationContext(), "Data added to API", Toast.LENGTH_SHORT).show();
                Long responseFromAPI = response.body();
                String responseString = "Response Code : " + response.code() + "\nId : " + responseFromAPI.toString();
                Toast.makeText(getApplicationContext(), responseString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<Long> call, Throwable t) {
                System.out.println(t);
                Toast.makeText(getApplicationContext(), "Error in adding user", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
