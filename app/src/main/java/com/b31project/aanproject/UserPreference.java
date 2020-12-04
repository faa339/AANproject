package com.b31project.aanproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class UserPreference extends AppCompatActivity {
    private Button resetBtn;
    private Button submitBtn;
    private EditText usernameText;
    private RadioGroup radioGroup;
    private CheckBox voiceCheck;
    private CheckBox hapticCheck;
    private CheckBox largeCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_preference);
        initElements();
        btnListeners();
    }

    public void initElements(){
        resetBtn = findViewById(R.id.reset_button);
        submitBtn = findViewById(R.id.confirm_button);
        usernameText = findViewById(R.id.PersonName);
        radioGroup = findViewById(R.id.rGroup);
        voiceCheck = findViewById(R.id.voice);
        hapticCheck = findViewById(R.id.haptic);
        largeCheck = findViewById(R.id.largerText);
    }

    public void btnListeners(){
        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                usernameText.getText().clear();
                radioGroup.clearCheck();
                voiceCheck.setChecked(false);
                hapticCheck.setChecked(false);
                largeCheck.setChecked(false);
            }
        });

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject userData = new JSONObject();
                try{
                    userData.put("username", usernameText.getText().toString());
                    userData.put("routeType", GetChecked());
                    userData.put("voiceCheck", voiceCheck.isChecked());
                    userData.put("haptic", hapticCheck.isChecked());
                    userData.put("largetext", largeCheck.isChecked());
                }catch (JSONException e){
                    Toast.makeText(UserPreference.this, "Could not save preferences:" + e.toString(), Toast.LENGTH_LONG).show();
                    ResetPrefs();
                    finish();
                    System.exit(1);
                }
                String path = getFilesDir().getAbsolutePath() + File.separator + "userPrefs.json";
                String userDataString = userData.toString();
                File prefFile = new File(path);
                try{
                    FileWriter writer = new FileWriter(prefFile);
                    writer.write(userDataString);
                    writer.close();
                }catch (IOException e){
                    Toast.makeText(UserPreference.this, "Could not save preferences:" + e.toString(), Toast.LENGTH_LONG).show();
                    ResetPrefs();
                    finish();
                    System.exit(1);
                }
                finish();
            }
        });
    }

    public String GetChecked(){
        if (radioGroup.getCheckedRadioButtonId() == R.id.notAllRoute){
            return "accessible";
        }else{
            return "any";
        }
    }

    public void ResetPrefs(){
        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("FirstTime", true);
        editor.commit();
        editor.apply();
    }
}