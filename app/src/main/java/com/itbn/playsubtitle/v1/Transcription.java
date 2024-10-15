package com.itbn.playsubtitle.v1;

import android.os.Looper;
import android.os.Handler;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.LinearLayout;
import android.preference.PreferenceManager;
import android.net.ConnectivityManager;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import org.vosk.*;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechStreamService;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import com.itbn.playsubtitle.v1.utilities.VideoAnalyzer;
import com.itbn.playsubtitle.v1.utilities.SubtitleGenerator;

public class Transcription implements RecognitionListener {
    
    private Recognizer recognizer;
    private SpeechStreamService sss;
    private Context context;
    private StringBuilder sb;
    private Handler handler;
    private ExecutorService executor;
    private String videoPath, modelPath;
    private VideoAnalyzer analyzer;
    private int itemCount, hypothesisLength;
    private double start, end, progressCount;
    private boolean sessionEnded = false;
    private ArrayList<String> singleWord = new ArrayList<>();
    private ArrayList<String> singleTimestamp = new ArrayList<>();
    
    public Transcription(Context _context, String _modelPath) {
        context = _context;
        modelPath = _modelPath;
    }
    
    public void startTranscription(View view, String _vp, String _sm) {
        // inflating popup window interface
        LayoutInflater inflater = (LayoutInflater) view.getContext().getSystemService(
        view.getContext().LAYOUT_INFLATER_SERVICE);
        View transcriptionView = inflater.inflate(R.layout.transcription_layout, null);
        
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;
        boolean focusable = false;
        
        ActivityHelper.popupWindow = new PopupWindow(transcriptionView, width, height, focusable);
        ActivityHelper.popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
        
        // instantiating variables
        videoPath = _vp;
        SubtitleGenerator.subtitleName = _sm;
        analyzer = new VideoAnalyzer(context, videoPath, 0, 1);
        handler = new Handler(Looper.getMainLooper());
        executor = Executors.newSingleThreadScheduledExecutor();
        ActivityHelper.output_message = transcriptionView.findViewById(R.id.output_message);
        SubtitleGenerator.translatorLanguage = PreferenceManager.getDefaultSharedPreferences(context);
        SubtitleGenerator.timestamp = new ArrayList<>();
        SubtitleGenerator.wordlist = new ArrayList<>();
        
        // initializing logic
        String destination = SubtitleGenerator.translatorLanguage.getString(context.getString(R.string.translator_key), "");
        
        if (!destination.equals("none") && !isOnline()) {
            ActivityHelper.prompt(context, "Requires internet!");
            ActivityHelper.closeDialog();
        } else {
            executor.execute(() -> {
                analyzer.extractAudio();
                initializingVosk(analyzer.sampleRate);
                
                handler.post(() -> {
                    if (!sessionEnded) {
                        try {
                            InputStream input = new FileInputStream(new File(analyzer.tempFilePath));
                            sss = new SpeechStreamService(recognizer, input, analyzer.sampleRate);
                            sss.start(this);
                            ActivityHelper.displayMessage("Transcribing voice...");
                            
                        } catch (IOException e) {
                            ActivityHelper.prompt(context, e.getMessage());
                            ActivityHelper.closeDialog();
                        }
                    }
                    executor.shutdown();
                });
            });
        }
    }
    
    private void initializingVosk(int _samplerate) {
        ActivityHelper.displayMessage("Initializing ASR...");
        Model model = new Model(modelPath);
        recognizer = new Recognizer(model, _samplerate);
        recognizer.setWords(true);
    }
    
    public void terminateSession() {
        sessionEnded = true;
        
        try {
            if (analyzer.output != null) {
                analyzer.output.close();
                FileUtil.deleteFile(analyzer.tempFilePath);
            }
            
            if (sss != null) {
                sss.stop();
            }
            
            if (SubtitleGenerator.onlineThread != null) {
                SubtitleGenerator.onlineThread.interrupt();
            }
            
            ActivityHelper.displayMessage("Terminating process...");
            ActivityHelper.closeDialog();
            
        } catch (IOException e) {
            ActivityHelper.prompt(context, e.getMessage());
            ActivityHelper.closeDialog();
        }
    }
    
    public void onPartialResult(String _hypothesis) {
        // unnecessary
    }
    
    public void onResult(String _hypothesis) {
        try {
            JSONObject object = new JSONObject(_hypothesis);
            JSONArray result = object.getJSONArray("result");
            hypothesisLength = result.length();
            int firstItem = 0;
            int lastItem = hypothesisLength - 1;
            sb = new StringBuilder(hypothesisLength);
            
            for (int i = 0; i < hypothesisLength; i++) {
                itemCount++;
                getProgressCount(result.getJSONObject(i).getDouble("end"));
                
                if (itemCount <= 15) {
                    sb.append(result.getJSONObject(i).getString("word"));
                    
                    if (itemCount != hypothesisLength) {
                        sb.append(" ");
                    }
                    
                    start = result.getJSONObject(firstItem).getDouble("start");
                    
                    if (hypothesisLength > 15) {
                        end = result.getJSONObject(14).getDouble("end");
                    }
                    
                    if (hypothesisLength <= 15) {
                        end = result.getJSONObject(lastItem).getDouble("end");
                    }
                }
                
                if (hypothesisLength > 15) {
                    if (itemCount >= 16) {
                        String word = result.getJSONObject(i).getString("word");
                        singleWord.add(word);
                        String time = formatTimestamp(result.getJSONObject(i).getDouble("start"), result.getJSONObject(i).getDouble("end"));
                        singleTimestamp.add(time);
                    }
                }
                
                if (itemCount == hypothesisLength) {
                    SubtitleGenerator.wordlist.add(sb.toString());
                    SubtitleGenerator.timestamp.add(formatTimestamp(start, end));
                    
                    if (hypothesisLength > 15) {
                        SubtitleGenerator.wordlistPartitioning(singleWord);
                        SubtitleGenerator.timelistPartitioning(singleTimestamp);
                        singleWord.clear();
                        singleTimestamp.clear();
                    }
                    
                    start = 0;
                    end = 0;
                    itemCount = 0;
                }
            }
            
        } catch (JSONException e) {
            //unnecessary
        }
    }
    
    public void onFinalResult(String _hypothesis) {
        try {
            JSONObject object = new JSONObject(_hypothesis);
            JSONArray result = object.getJSONArray("result");
            hypothesisLength = result.length();
            int firstItem = 0;
            int lastItem = hypothesisLength - 1;
            sb = new StringBuilder(hypothesisLength);
            
            for (int i = 0; i < hypothesisLength; i++) {
                itemCount++;
                getProgressCount(result.getJSONObject(i).getDouble("end"));
                
                if (itemCount <= 15) {
                    sb.append(result.getJSONObject(i).getString("word"));
                    
                    if (itemCount != hypothesisLength) {
                        sb.append(" ");
                    }
                    
                    start = result.getJSONObject(firstItem).getDouble("start");
                    
                    if (hypothesisLength > 15) {
                        end = result.getJSONObject(14).getDouble("end");
                    }
                    
                    if (hypothesisLength <= 15) {
                        end = result.getJSONObject(lastItem).getDouble("end");
                    }
                }
                
                if (hypothesisLength > 15) {
                    if (itemCount >= 16) {
                        String word = result.getJSONObject(i).getString("word");
                        singleWord.add(word);
                        String time = formatTimestamp(result.getJSONObject(i).getDouble("start"), result.getJSONObject(i).getDouble("end"));
                        singleTimestamp.add(time);
                    }
                }
                
                if (itemCount == hypothesisLength) {
                    SubtitleGenerator.wordlist.add(sb.toString());
                    SubtitleGenerator.timestamp.add(formatTimestamp(start, end));
                    
                    if (hypothesisLength > 15) {
                        SubtitleGenerator.wordlistPartitioning(singleWord);
                        SubtitleGenerator.timelistPartitioning(singleTimestamp);
                        singleWord.clear();
                        singleTimestamp.clear();
                    }
                    
                    start = 0;
                    end = 0;
                    itemCount = 0;
                }
            }
            
            if (!sessionEnded) {
                SubtitleGenerator.generateSubtitle();
                FileUtil.deleteFile(analyzer.tempFilePath);
            }
            
        } catch (JSONException e) {
            if (!sessionEnded) {
                SubtitleGenerator.generateSubtitle();
                FileUtil.deleteFile(analyzer.tempFilePath);
            } else {
                ActivityHelper.prompt(context, e.getMessage());
                ActivityHelper.closeDialog();
            }
        }
    }
    
    public void onError(Exception exception) {
        ActivityHelper.prompt(context, exception.getMessage());
        ActivityHelper.closeDialog();
    }
    
    public void onTimeout() {
        // unnecessary
    }
    
    private void getProgressCount(double _count) {
        double count =+ Math.round(_count * 1000);
        progressCount = Math.round((count / ActivityHelper.durationInMillis) * 100);
    }
    
    private String formatTimestamp(double _start, double _end) {
        double start = Math.round(_start * 1000);
        double end = Math.round(_end * 1000);
        
        String hours1 = String.valueOf((long)start / (60 * 60 * 1000));
        String minutes1 = String.valueOf((long)(start / (60 * 1000)) % 60);
        String seconds1 = String.valueOf((long)(start / 1000) % 60);
        String milliseconds1 = String.valueOf((long)start % 1000);
        String startTime = String.format("%02d", Integer.parseInt(hours1)) + ":" + String.format("%02d", Integer.parseInt(minutes1))
         + ":" + String.format("%02d", Integer.parseInt(seconds1)) + "," + String.format("%03d", Integer.parseInt(milliseconds1));
        
        String hours2 = String.valueOf((long)end / (60 * 60 * 1000));
        String minutes2 = String.valueOf((long)(end / (60 * 1000)) % 60);
        String seconds2 = String.valueOf((long)(end / 1000) % 60);
        String milliseconds2 = String.valueOf((long)end % 1000);
        String endTime = String.format("%02d", Integer.parseInt(hours2)) + ":" + String.format("%02d", Integer.parseInt(minutes2))
         + ":" + String.format("%02d", Integer.parseInt(seconds2)) + "," + String.format("%03d", Integer.parseInt(milliseconds2));
        
        String timestamp = startTime + " --> " + endTime;
        
        return timestamp;
    }
    
    private boolean isOnline() {
        boolean networkAvailability = false;
        ConnectivityManager cm = (ConnectivityManager) SketchApplication.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm.getActiveNetworkInfo() != null) {
            networkAvailability = true;
        } else {
            networkAvailability = false;
        }
        
        return networkAvailability;
    }
}
