package com.b31project.aanproject;

import android.app.Application;
import android.content.Context;
import android.speech.tts.Voice;

import org.json.JSONObject;

import java.io.File;
import java.util.Scanner;

import okhttp3.Route;
import timber.log.Timber;

//General class for storing user preference data, to be used
//in route generation and display funcs
public class User extends Application {
    public String Username;
    public Boolean Routetype;
    public Boolean VoiceInstruct;
    public Boolean HapticFeedBack;
    public Boolean LargeText;
    private static User thisInstance = null;

    private User(String path){
        File prefs = new File(path);
        JSONObject JSONprefs = null;
        //JSONObject and File scanner can both throw exceptions -- gotta catch them all!
        try{
            String userPrefs = new Scanner(prefs).useDelimiter("\\Z").next();
            JSONprefs = new JSONObject(userPrefs);
            Username = JSONprefs.getString("username");
            String stringRouteType = JSONprefs.getString("routeType");
            if(stringRouteType=="accessible")
                Routetype = true;
            else
                Routetype = false;
            VoiceInstruct = JSONprefs.getBoolean("voiceCheck");
            HapticFeedBack = JSONprefs.getBoolean("haptic");
            LargeText = JSONprefs.getBoolean("largetext");
            thisInstance = this;
        }catch(Exception e){
            Timber.e("Can't read the file! %s", e.toString());
        }
    }
    //Use this method to get the class, instead of the constructor to keep it singleton
    public static User getInstance(String path){
        if(thisInstance == null){
            thisInstance = new User(path);
            return thisInstance;
        }
        return thisInstance;
    }
}
