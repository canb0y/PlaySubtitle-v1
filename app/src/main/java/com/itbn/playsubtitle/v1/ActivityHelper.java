package com.itbn.playsubtitle.v1;

import android.os.Looper;
import android.os.Handler;
import android.content.Context;
import android.widget.TextView;
import android.widget.PopupWindow;
import android.widget.Toast;
import java.util.Locale;
import java.util.ArrayList;

public class ActivityHelper {
    
    public static TextView output_message = null;
    public static PopupWindow popupWindow = null;
    public static ArrayList<String> alternatives = null;
    public static boolean configAvailability = false;
    public static boolean onSettingsCreated = false;
    public static double durationInMillis = 0;
    
    public static String formatDuration(int _duration) {
        String formattedDURATION;
        
        int sec = (_duration / 1000) % 60;
        int min = (_duration / (1000 * 60)) % 60;
        int hrs = _duration / (1000 * 60 * 60);
        
        if (hrs == 0) {
            return (formattedDURATION = String.valueOf(min).concat(":".concat(String.format(Locale.UK, "%02d", sec))));
        } else {
            return (formattedDURATION = String.valueOf(hrs).concat(":".concat(String.format(Locale.UK, "%02d", min).concat(":".concat(String.format(Locale.UK, "%02d", sec))))));
        }
    }
    
    public static void displayMessage(String _text) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                output_message.setText(_text);
            }
        });
    }
    
    public static void prompt(Context _context, String _text) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                Toast.makeText(_context, _text, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    public static void closeDialog() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            public void run() {
                if (popupWindow != null) {
                    popupWindow.dismiss();
                }
                
                popupWindow = null;
            }
        }, 2000);
    }
}
