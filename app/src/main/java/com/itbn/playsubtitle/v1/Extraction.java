package com.itbn.playsubtitle.v1;

import android.os.Handler;
import android.os.Looper;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.itbn.playsubtitle.v1.utilities.VideoAnalyzer;

public class Extraction {
    
    private Context context;
    private int CHANNEL_COUNT, SAMPLE_RATE;
    private String videoPath, wavePath;
    private VideoAnalyzer analyzer;
    private Handler handler;
    private ExecutorService executor;
    private SharedPreferences preference;
    private boolean sessionEnded = false;
    
    public Extraction(Context _context) {
        context = _context;
    }
    
    public void startExtraction(View view, final String _vp, final String _wp) {
        // inflating popup window interface
        LayoutInflater inflater = (LayoutInflater) view.getContext().getSystemService(
        view.getContext().LAYOUT_INFLATER_SERVICE);
        View extractionView = inflater.inflate(R.layout.extraction_layout, null);
        
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;
        boolean focusable = false;
        
        ActivityHelper.popupWindow = new PopupWindow(extractionView, width, height, focusable);
        ActivityHelper.popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
        
        // instantiating variables
        videoPath = _vp;
        wavePath = _wp;
        handler = new Handler(Looper.getMainLooper());
        executor = Executors.newSingleThreadScheduledExecutor();
        preference = PreferenceManager.getDefaultSharedPreferences(context);
        CHANNEL_COUNT = Integer.parseInt(preference.getString(context.getString(R.string.channel_key), context.getString(R.string.mono_value)));
        SAMPLE_RATE = Integer.parseInt(preference.getString(context.getString(R.string.samplerate_key), context.getString(R.string.medium_value)));
        analyzer = new VideoAnalyzer(context, videoPath, SAMPLE_RATE, CHANNEL_COUNT);
        ActivityHelper.output_message = extractionView.findViewById(R.id.output_message);
        
        // initializing main logic
        executor.execute(() -> {
            analyzer.extractAudio();
            
            handler.post(() -> {
                if (!sessionEnded) {
                    if (SAMPLE_RATE == 0 && CHANNEL_COUNT == 0) {
                        convertPCMtoWAV(analyzer.sampleRate, analyzer.channelCount);
                    } else if (SAMPLE_RATE == 0 && CHANNEL_COUNT != 0) {
                        convertPCMtoWAV(analyzer.sampleRate, CHANNEL_COUNT);
                    } else if (SAMPLE_RATE != 0 && CHANNEL_COUNT == 0) {
                        convertPCMtoWAV(SAMPLE_RATE, analyzer.channelCount);
                    } else {
                        convertPCMtoWAV(SAMPLE_RATE, CHANNEL_COUNT);
                    }
                }
                executor.shutdown();
            });
        });
    }
    
    public void terminateSession() {
        sessionEnded = true;
        
        try {
            if (analyzer.output != null) {
                analyzer.output.close();
                FileUtil.deleteFile(analyzer.tempFilePath);
            }
            
            ActivityHelper.displayMessage("Terminating process...");
            ActivityHelper.closeDialog();
            
        } catch (IOException e) {
            ActivityHelper.prompt(context, e.getMessage());
            ActivityHelper.closeDialog();
        }
    }
    
    public void convertPCMtoWAV(int _samplerate, int _channel) {
        ActivityHelper.displayMessage("Converting to WAV...");
        
        File input = new File(analyzer.tempFilePath);
        File waveFile = new File(wavePath);
        
        int reading = 0;
        int rawLength = (int)input.length();
        byte[] rawData = new byte[20000];
        
        InputStream fis = null;
        DataOutputStream output = null;
        
        try {
            fis = new FileInputStream(input);
            output = new DataOutputStream(new FileOutputStream(wavePath));
            
            // WAVE header
            // refer to http://soundfile.sapp.org/doc/WaveFormat/
            writeString(output, "RIFF"); // ChunkID
            writeInt(output, 36 + rawLength); // ChunkSize
            writeString(output, "WAVE"); // Format
            writeString(output, "fmt "); // Subchunk1ID
            writeInt(output, 16); // Subchunk1Size
            writeShort(output, (short) 1); // AudioFormat (1 = PCM)
            writeShort(output, (short) _channel); // NumChannels (1 = Mono)
            writeInt(output, _samplerate); // SampleRate
            writeInt(output, _samplerate * 2); // ByteRate
            writeShort(output, (short) 2); // BlockAlign
            writeShort(output, (short) 16); // BitsPerSample
            writeString(output, "data"); // Subchunk2ID
            writeInt(output, rawLength); // Subchunk2Size
            
            while ((reading = fis.read(rawData, 0, rawData.length)) != -1) {
                output.write(rawData, 0, reading);
            }
            
            fis.close();
            output.flush();
            output.close();
            
        } catch (IOException e) {
            ActivityHelper.prompt(context, e.getMessage());
            ActivityHelper.closeDialog();
        }
        
        FileUtil.deleteFile(analyzer.tempFilePath);
        executor.shutdown();
        ActivityHelper.closeDialog();
    }
    
    /** METHOD FOR CONVERTING FILE INTO BYTE ARRAY
    private byte[] fullyReadFileToBytes(File file) throws IOException {
        int size = (int)file.length();
        byte[] b = new byte[size];
        byte[] tmpBuff = new byte[size];
        
        InputStream input = new FileInputStream(file);
        
        try {
            int read = input.read(b, 0, size);
            
            if (read < size) {
                int remain = size - read;
                while (remain > 0) {
                    read = input.read(tmpBuff, 0, remain);
                    System.arraycopy(tmpBuff, 0, b, size - remain, read);
                    remain -= read;
                }
            }
        } catch (IOException e) {
            SketchwareUtil.showMessage(context, e.getMessage());
        }
        
        return b;
    }**/
    
    private void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }
    
    private void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
    }
    
    private void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }
}
