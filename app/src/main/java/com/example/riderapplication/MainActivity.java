package com.example.riderapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.riderapplication.Common.Common;
import com.example.riderapplication.Model.RiderModel;
import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class MainActivity extends AppCompatActivity {
    private static  int LOGIN_REQUEST = 700;
    private List<AuthUI.IdpConfig> provider;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener listener;

    @BindView(R.id.progress__bar)
    ProgressBar progressBar;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference riderInfoRef;

    @Override
    protected void onStart() {
        super.onStart();
        displaySplashScreen();
    }

    private void displaySplashScreen() {
        Completable.timer(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe(() ->
                        firebaseAuth.addAuthStateListener(listener)
                );
    }

    @Override
    protected void onStop() {
        if(firebaseAuth != null && listener != null){
            firebaseAuth.removeAuthStateListener(listener);
        }
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initiate();
    }

    private void initiate() {
        ButterKnife.bind(this);

        firebaseDatabase = FirebaseDatabase.getInstance();
        riderInfoRef = firebaseDatabase.getReference(Common.RIDER_INFO_REFERNCE);

        provider = Arrays.asList(
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build()
        );
        firebaseAuth = FirebaseAuth.getInstance();
        listener = myFirebaseAuth -> {
          FirebaseUser currentUser = myFirebaseAuth.getCurrentUser();
          if(currentUser != null){
              checkUserFromFirebase();
          }
          else{
              showLoginLayout();

          }
        };
    }

    private void showLoginLayout() {
        AuthMethodPickerLayout authMethodPickerLayout = new AuthMethodPickerLayout
                .Builder(R.layout.layout_sign_in)
                .setPhoneButtonId(R.id.btn_sign_in_phone)
                .setGoogleButtonId(R.id.btn_sign_in_google)
                .build();
        startActivityForResult(AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAuthMethodPickerLayout(authMethodPickerLayout)
                        .setIsSmartLockEnabled(false)
                        .setTheme(R.style.login_in)
                        .setAvailableProviders(provider)
                        .build(),LOGIN_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == LOGIN_REQUEST){
            IdpResponse idpResponse = IdpResponse.fromResultIntent(data);

            if(resultCode == RESULT_OK){
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            }
            else{
                Toast.makeText(this, "Failed To Sign"+idpResponse.getError().getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkUserFromFirebase() {
        riderInfoRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){
                            RiderModel model = snapshot.getValue(RiderModel.class);
                            goToHomePageActivity(model);
                        }
                        else{
                            showResgister();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void showResgister() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.DialogTheme);
        View itemView = LayoutInflater.from(this).inflate(R.layout.layout_register,null);
//set data
        TextInputEditText first__name = (TextInputEditText)itemView.findViewById(R.id.first_name);
        TextInputEditText last__name = (TextInputEditText)itemView.findViewById(R.id.last_name);
        TextInputEditText phoneNo= (TextInputEditText)itemView.findViewById(R.id.phone_number);
        Button btn__register =(Button)itemView.findViewById(R.id.btn_register);
        if(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber() !=null &&
                !TextUtils.isEmpty(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber()))
            phoneNo.setText(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());
        //set View
        builder.setView(itemView);
        AlertDialog dialog = builder.create();
        dialog.show();
        btn__register.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (TextUtils.isEmpty(first__name.getText().toString())) {
                    first__name.setError("First name is Required");

                } else if (TextUtils.isEmpty(last__name.getText().toString())) {
                    last__name.setError("last name is Required");

                } else if (TextUtils.isEmpty(phoneNo.getText().toString())) {
                    phoneNo.setError("Phone number Required");


                }else{
                    RiderModel riderinfoModel = new RiderModel();
                    riderinfoModel.setFirstname(first__name.getText().toString());
                    riderinfoModel.setLastname(last__name.getText().toString());
                    riderinfoModel.setPhoneNumber(phoneNo.getText().toString());


                    riderInfoRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(riderinfoModel)
                            .addOnFailureListener(new OnFailureListener() {
                                public void onFailure(@NonNull Exception e) {
                                    dialog.dismiss();
                                    Toast.makeText(MainActivity.this,"Registration Failed",Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(MainActivity.this, "Registration was successful",Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                    goToHomePageActivity(riderinfoModel);
                                }
                            });
                }


            }
        });

    }

    private void goToHomePageActivity(RiderModel model) {
        Common.currentRider = model;
        startActivity(new Intent(this,HomeActivity.class));
        finish();
    }
}