package com.itbn.playsubtitle.v1.utilities;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.widget.TextView;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import com.itbn.playsubtitle.v1.ActivityHelper;
import com.itbn.playsubtitle.v1.SketchApplication;

public class SubtitleView extends TextView implements Runnable {
    
    private static final int UPDATE_INTERVAL = 300;
    private MediaPlayer player;
    private TreeMap<Long, Line> track;
    
    public SubtitleView(Context context) {
        super(context);
    }
    
    public SubtitleView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public SubtitleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    @Override
    public void run() {
        if (player !=null && track!= null) {
            int seconds = player.getCurrentPosition() / 1000;
            setText(getTimedText(player.getCurrentPosition()));
        }
        postDelayed(this, UPDATE_INTERVAL);
    }
    
    private String getTimedText(long currentPosition) {
        String result = "";
        
        for (Map.Entry<Long, Line> entry: track.entrySet()) {
            if (currentPosition < entry.getKey()) break;
            if (currentPosition < entry.getValue().to) result = entry.getValue().text;
        }
        
        return result;
    }
    
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        postDelayed(this, 300);
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(this);
    }
    
    public void setPlayer(MediaPlayer player) {
        this.player = player;
    }
    
    public void setSubSource(String subtitlePath, String mime) {
        if (mime.equals(MediaPlayer.MEDIA_MIMETYPE_TEXT_SUBRIP)) {
            track = getSubtitleFile(subtitlePath);
        } else {
            throw new UnsupportedOperationException("Parser only built for SRT subs");
        }
    }
    
    public static TreeMap<Long, Line> parse(InputStream is) throws IOException {
        LineNumberReader r = new LineNumberReader(new InputStreamReader(is, "UTF-8"));
        TreeMap<Long, Line> track = new TreeMap<>();
        
        while ((r.readLine()) != null) {
            String timeString = r.readLine();
            String lineString = "";
            String s;
            
            while (!((s = r.readLine()) == null || s.trim().equals(""))) {
                lineString += s + "\n";
            }
            
            long startTime = parse(timeString.split("-->")[0]);
            long endTime = parse(timeString.split("-->")[1]);
            track.put(startTime, new Line(startTime, endTime, lineString));
        }
        
        return track;
    }
    
    private static long parse(String in) {
        long hours = Long.parseLong(in.split(":")[0].trim());
        long minutes = Long.parseLong(in.split(":")[1].trim());
        long seconds = Long.parseLong(in.split(":")[2].split(",")[0].trim());
        long millies = Long.parseLong(in.split(":")[2].split(",")[1].trim());
        
        return hours * 60 * 60 * 1000 + minutes * 60 * 1000 + seconds * 1000 + millies;
    }
    
    private TreeMap<Long, Line> getSubtitleFile(String subtitlePath) {
        InputStream inputStream = null;
        
        try {
            inputStream = new FileInputStream(new File(subtitlePath));
            return parse(inputStream);
            
        } catch (Exception e) {
            ActivityHelper.prompt(SketchApplication.getContext(), e.getMessage());
            
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                    
                } catch (IOException e) {
                    ActivityHelper.prompt(SketchApplication.getContext(), e.getMessage());
                }
            }
        }
        
        return null;
    }
    
    public static class Line {
        long from;
        long to;
        String text;
        
        public Line(long from, long to, String text) {
            this.from = from;
            this.to = to;
            this.text = text;
        }
    }
}