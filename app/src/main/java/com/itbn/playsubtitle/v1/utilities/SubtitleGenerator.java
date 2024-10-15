package com.itbn.playsubtitle.v1.utilities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.itbn.playsubtitle.v1.utilities.RequestNetwork;
import com.itbn.playsubtitle.v1.utilities.RequestNetworkController;
import com.itbn.playsubtitle.v1.MainActivity;
import com.itbn.playsubtitle.v1.EditorActivity;
import com.itbn.playsubtitle.v1.ActivityHelper;
import com.itbn.playsubtitle.v1.SketchApplication;
import com.itbn.playsubtitle.v1.FileUtil;
import com.itbn.playsubtitle.v1.R;

public class SubtitleGenerator {
    
    private static boolean isTranslated = false;
    public static String subtitleName;
    public static Thread onlineThread = null;
    public static SharedPreferences translatorLanguage = null;
    public static ArrayList<String> timestamp = null;
    public static ArrayList<String> wordlist = null;
    
    public static void generateSubtitle() {
        onlineThread = new Thread(() -> {
            
            if (getStringIdentifier() == 0) {
                exportSubtitle(rawText());
            } else {
                String srcLanguage = SketchApplication.getContext().getString(getStringIdentifier());
                String dstLanguage = translatorLanguage.getString(SketchApplication.getContext().getString(R.string.translator_key), SketchApplication.getContext().getString(R.string.small_en_us));
                
                if (dstLanguage.equals("none") || dstLanguage.equals(srcLanguage)) {
                    exportSubtitle(rawText());
                } else {
                    ActivityHelper.displayMessage("Translating captions to " + dstLanguage.toUpperCase());
                    translateText(rawText(), srcLanguage, dstLanguage);
                }
            }
        });
        onlineThread.start();
    }
    
    public static void wordlistPartitioning(ArrayList<String> _input) {
        final int SIZE = 15;
        
        StringBuilder sb = new StringBuilder();
        
        for (String s : _input) {
            sb.append(s);
            sb.append("\n");
        }
        String[] words = sb.toString().split("\n");
        String[] captions = new String[(int)Math.ceil((double)words.length / SIZE)];
        
        for (int i = 0, j = 0; i < words.length; i += SIZE, j++) {
            captions[j] = String.join(" ", Arrays.copyOfRange(words, i, Integer.min(words.length, i + SIZE)));
        }
        
        for (String s : captions) {
            wordlist.add(s);
        }
    }
    
    public static void timelistPartitioning(ArrayList<String> _input) {
        final int SIZE = 15;
        int extraMillis = 001;
        
        StringBuilder sb = new StringBuilder();
        
        for (String s : _input) {
            sb.append(s);
            sb.append("\n");
        }
        
        String[] time = sb.toString().split("\n");
        String[] chunks = new String[(int)Math.ceil((double)time.length / SIZE)];
        
        for (int i = 0, j = 0; i < time.length; i += SIZE, j++) {
            chunks[j] = String.join("\n", Arrays.copyOfRange(time, i, Integer.min(time.length, i + SIZE)));
        }
        
        for (String s : chunks) {
            String endTime = s.substring(s.lastIndexOf(" ")).replace(" ", "");
            String unwanted = s.substring(s.indexOf(" -->"));
            String digits = s.replace(unwanted, "");
            String millis = digits.substring(digits.lastIndexOf(",")).replace(",", "");
            int modifiedTime = Integer.parseInt(millis) + extraMillis;
            String startTime = digits.replace(millis, String.valueOf(modifiedTime));
            
            timestamp.add(startTime + " --> " + endTime);
        }
    }
    
    private static void exportSubtitle(String _wordlist) {
        ActivityHelper.displayMessage("Generating subtitle...");
        
        String path = FileUtil.getPackageDataDir(SketchApplication.getContext()).concat("/Subtitles/" + subtitleName + ".srt");
        StringBuilder subtitle = new StringBuilder();
        String[] translation = _wordlist.split("\n");
        ArrayList<String> captions = new ArrayList(Arrays.asList(translation));
        
        int sequenceCount = 1;
        int len = timestamp.size();
        len = Math.max(len, captions.size());
        
        for (int i = 0; i < len; i++) {
            if (i < timestamp.size()) {
                subtitle.append(sequenceCount++);
                subtitle.append("\n");
                subtitle.append(timestamp.get(i));
                subtitle.append("\n");
            }
            
            if (i < captions.size()) {
                subtitle.append(captions.get(i));
                
                if (i != (captions.size() - 1)) {
                    subtitle.append("\n");
                    subtitle.append("\n");
                }
            }
        }
        
        FileUtil.writeFile(path, subtitle.toString());
        translatorLanguage.edit().putString(subtitleName, path).commit();
        MainActivity.instantiateActivity()._scanFileStorage();
        onlineThread.interrupt();
        ActivityHelper.closeDialog();
        
        if (isTranslated) {
            isTranslated = false;
            Intent gotoActivity = new Intent();
            gotoActivity.setClass(SketchApplication.getContext(), EditorActivity.class);
            gotoActivity.putExtra("subtitle", path);
            MainActivity.instantiateActivity().startActivity(gotoActivity);
        }
    }
    
    private static void translateText(String _text, String _src, String _dst) {
        isTranslated = true;
        ActivityHelper.alternatives = new ArrayList<>();
        RequestNetwork.RequestListener connection_request_listener = null;
        connection_request_listener = new RequestNetwork.RequestListener() {
            @Override
            public void onResponse(String _param1, String _param2, HashMap<String, Object> _param3) {
                final String _tag = _param1;
                final String _response = _param2;
                final HashMap<String, Object> _responseHeaders = _param3;
                HashMap<String, Object> apiResponse = new Gson().fromJson(_response, new TypeToken<HashMap<String, Object>>(){}.getType());
                String result = apiResponse.get("translatedText").toString();
                
                try {
                    JSONObject object = new JSONObject(_response);
                    JSONArray array = object.getJSONArray("alternatives");
                    
                    for (int i = 0; i < array.length(); i++) {
                        ActivityHelper.alternatives.add(array.getString(i));
                    }
                    
                } catch (JSONException e) {
                    ActivityHelper.prompt(SketchApplication.getContext(), e.getMessage());
                }
                
                exportSubtitle(result);
            }
            
            @Override
            public void onErrorResponse(String _param1, String _param2) {
                final String _tag = _param1;
                final String _message = _param2;
                
                if (_message.equals("Read timed out")) {
                    ActivityHelper.prompt(SketchApplication.getContext(), _message);
                    ActivityHelper.displayMessage("Terminating process...");
                    onlineThread.interrupt();
                    ActivityHelper.closeDialog();
                }
            }
        };
        
        RequestNetwork connection = new RequestNetwork(MainActivity.instantiateActivity());
        String api = "https://translate.fedilab.app/translate";
        HashMap<String, Object> translator = new HashMap<>();
        translator.put("q", _text);
        translator.put("source", _src);
        translator.put("target", _dst);
        translator.put("alternatives", 3);
        translator.put("api_key", "");
        translator.put("format", "text");
        connection.setParams(translator, RequestNetworkController.REQUEST_PARAM);
        connection.startRequestNetwork(RequestNetworkController.POST, api, "", connection_request_listener);
    }
    
    private static int getStringIdentifier() {
        String value = translatorLanguage.getString(SketchApplication.getContext().getString(R.string.vosk_preference_key), SketchApplication.getContext().getString(R.string.en_us_value));
	    String small = value.substring(value.lastIndexOf("small-"));
	    String number = "";
        
        if (value.contains("-0.")) {
            number = value.substring(value.lastIndexOf("-0."));
        } else {
            if (value.contains("-v3")) {
                number = value.substring(value.lastIndexOf("-v3"));
            }
        }
        
        String noNumber = small.replace(number, "");
        String stringID = noNumber.replaceAll("-", "_");
        
        return SketchApplication.getContext().getResources().getIdentifier(stringID, "string", SketchApplication.getContext().getPackageName());
    }
    
    private static String rawText() {
        StringBuilder sb = new StringBuilder();
        
        for (String word : wordlist) {
            sb.append(word);
            sb.append("\n");
        }
        
        return sb.toString();
    }
}
