package com.example.intentsapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.Manifest;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    private static final String CAMERA = "CAMERA";
    private static final String READ_CONTACTS = "READ_CONTACTS";
    private static final String SEND_SMS = "SEND_SMS";
    private static final String TAG = "AppExplicitIntents";
    private Button btnCamera, btnGetContact, btnSendInfo;

    private TextView tvContactName, tvSendInfo;
    private ImageView ivImage;
    private String permission = "UNDEFINED";

    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted-> {
                if(isGranted){
                    if(permission.equals(CAMERA)) {
                        launchCamera();
                    }
                    if(permission.equals(READ_CONTACTS)) {
                        getContact();
                    }
                    if(permission.equals(SEND_SMS)) {
                        sendInfoToContact();
                    }
                } else{
                    Log.d(TAG, "PERMISSION DENIED ");
                }
            });

    private ActivityResultLauncher<Intent> photoResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                        if(result.getResultCode() == RESULT_OK){
                            Bundle extras = result.getData().getExtras();
                            Bitmap imageBitmap = (Bitmap)extras.get("data");
                            ivImage.setImageBitmap(imageBitmap);
                        }else{
                            Log.e(TAG, "ERROR: SOME ERROR WITH CAMERA" );
                        }
                    }
            );
    private void launchCamera(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        photoResultLauncher.launch(takePictureIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ivImage=findViewById(R.id.imageView);
        btnCamera = findViewById(R.id.btnCapture);
        btnCamera.setOnClickListener(v->{
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                permission = "CAMERA";
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });


        btnSendInfo = findViewById(R.id.btnSendInfoToContact);
        tvSendInfo =  findViewById(R.id.tvSendInfo);
        btnSendInfo.setOnClickListener(view -> {
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_CONTACTS) ==
                    PackageManager.PERMISSION_GRANTED) {
                sendInfoToContact();
            } else {
                permission = "SEND_SMS";
                requestPermissionLauncher.launch(Manifest.permission.SEND_SMS);
            }
        });
        btnGetContact=findViewById(R.id.btnGetContact);
        tvContactName = findViewById(R.id.tvContact);
        btnGetContact.setOnClickListener(view -> {
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_CONTACTS) ==
                    PackageManager.PERMISSION_GRANTED) {
                getContact();
            } else {
                permission = "READ_CONTACTS";
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
            }
        });
    }
    private ActivityResultLauncher<Intent> getContactActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if(result.getResultCode() == RESULT_OK){

                    Intent data = result.getData();
                    if (data != null) {

                        Uri contactUri = data.getData();

                        String[] queryFields = new String[]{
                                ContactsContract.Contacts.DISPLAY_NAME
                        };
                        //Log.d(TAG, "id: "+ContactsContract.Contacts.DISPLAY_NAME);
                        Cursor c =this.getContentResolver().query(contactUri, queryFields,null,null,null);
                        try {
                            if (c.getCount() == 0) {
                                return;
                            }
                            c.moveToFirst();
                            String name = c.getString(0);
                            //String number = c.getString(0);
                            //Log.d(TAG, "number: " + number);
                            tvContactName.setText(name);
                        }
                        catch (Exception e){
                            Log.e(TAG, "ERROR: "+e.getMessage());
                        }
                         finally {
                            c.close();
                        }

                    }
                }else{
                    Log.e(TAG, "ERROR: "+result.getResultCode() );
                }
            }
    );



    private ActivityResultLauncher<Intent> SendInfoActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if(result.getResultCode() == RESULT_OK){

                    Intent data = result.getData();
                    Log.d(TAG, "data: "+result);
                    if (data != null) {

                        Uri contactUri = data.getData();
                        String [] uriParts = contactUri.getPath().split("/");
                        String contactId = uriParts[uriParts.length-1];
                        String newUri =ContactsContract.CommonDataKinds.Phone.CONTENT_URI.toString()+"/" + uriParts[uriParts.length-1];
                        contactUri = Uri.parse(newUri);
                        //Uri contactUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
                        Log.d(TAG, ": "+contactUri.toString()+", "+ContactsContract.CommonDataKinds.Phone.CONTENT_URI);

                        Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = "+ contactId,
                                null, null);
                        while(phones.moveToNext())
                        {
                            @SuppressLint("Range") String phoneNumber = phones.getString(phones.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER));
                            @SuppressLint("Range") String name = phones.getString(phones.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                            Uri uri = Uri.parse("smsto:"+phoneNumber);
                            Intent it = new Intent(Intent.ACTION_SENDTO, uri);
                            it.putExtra("sms_body", "Some text in message");
                            startActivity(it);
                            tvSendInfo.setText("Info sent to "+name);
                            Log.d(TAG, "phone: "+phoneNumber);
                        }
                        phones.close();
                    }
                }else{
                    Log.e(TAG, "ERROR: ");
                }

            }
    );

    private void sendInfoToContact(){
        Intent intentGetContact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        SendInfoActivityLauncher.launch(intentGetContact);

    }

    private void getContact (){
        Intent intentGetContact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        getContactActivityLauncher.launch(intentGetContact);
    }

}