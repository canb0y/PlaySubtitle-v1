package com.itbn.playsubtitle.v1;

import android.os.Handler;
import android.os.Looper;
import android.net.ConnectivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.preference.PreferenceManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import androidx.cardview.widget.CardView;
import com.itbn.playsubtitle.v1.utilities.Decompress;

public class ModelsDownloader {
        
    private boolean isDownloadingFile = false;
        
    private HttpURLConnection connection;
    private Thread downloading_thread;
    private SharedPreferences voskLanguage;
    private String vosk_data;
    private Context context;
    private CardView cardview;
    private ProgressBar download_progress;
    private ProgressBar loading_progress;
    private TextView bytes_txt;
    private TextView output_messages;
    private Button downloadBtn;
    private Button cancelBtn;
    
    public ModelsDownloader(Context context) {
        this.context = context;
    }
    
    public void showDownloadWindow(final View view) {
        //inflate download_window_layout interface
        LayoutInflater inflater = (LayoutInflater) view.getContext().getSystemService(
        view.getContext().LAYOUT_INFLATER_SERVICE);
        View downloadView = inflater.inflate(R.layout.download_window_layout, null);
        
        //set layout dimensions
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;
        boolean focusable = false;
        
        final PopupWindow popupWindow = new PopupWindow(downloadView, width, height, focusable);
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
        
        //initialize download_window_layout elements
        output_messages = downloadView.findViewById(R.id.output_messages);
        bytes_txt = downloadView.findViewById(R.id.bytes_txt);
        download_progress = downloadView.findViewById(R.id.download_progress);
        cardview = downloadView.findViewById(R.id.cardview);
        loading_progress = downloadView.findViewById(R.id.loading_progress);
        downloadBtn = downloadView.findViewById(R.id.downloadBtn);
        cancelBtn = downloadView.findViewById(R.id.cancelBtn);
        voskLanguage = PreferenceManager.getDefaultSharedPreferences(context);
        vosk_data = voskLanguage.getString(context.getString(R.string.vosk_preference_key), context.getString(R.string.tr_value));
        
        cardview.setVisibility(View.GONE);
        loading_progress.setVisibility(View.GONE);
        
        downloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isDownloadingFile == false) {
                    downloading_thread = new Thread(() -> initiatingDownloadProcess());
                    downloading_thread.start();
                    
                    cardview.setVisibility(View.GONE);
                    
                    if (isOnline()) {
                        loading_progress.setVisibility(View.VISIBLE);
                        changeText(output_messages, "Preparing to download...");
                    }
                    
                } else {
                    isDownloadingFile = false;
                    changeText(downloadBtn, context.getString(R.string.toggleBtnStart));
                    connection.disconnect();
                    FileUtil.deleteFile(zipFileName().replace(".zip", ""));
                    downloading_thread.interrupt();
                }
            }
        });
        
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isDownloadingFile == true) {
                    connection.disconnect();
                    FileUtil.deleteFile(zipFileName().replace(".zip", ""));
                    downloading_thread.interrupt();
                    popupWindow.dismiss();
                } else {
                    popupWindow.dismiss();
                }
            }
        });
    }
    
    private void initiatingDownloadProcess() {
        Handler handler = new Handler(Looper.getMainLooper());
		ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		
		executor.execute(() -> {
            try {
                URL url = new URL(vosk_data);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
				
				byte[] data = new byte[1024];
				double bytes_downloaded = 0;
				double bytes_total = connection.getContentLength();
                
                if (bytes_total > 0) {
                    isDownloadingFile = true;
					
					new Handler(Looper.getMainLooper()).post(() -> {
                        loading_progress.setVisibility(View.GONE);
                        cardview.setVisibility(View.VISIBLE);
                        changeText(downloadBtn, context.getString(R.string.toggleBtnStop));
                    });
                    
                    InputStream input = connection.getInputStream();
					FileOutputStream output = new FileOutputStream(zipFileName());
                    
                    int count;
                    while((count = input.read(data)) != -1) {
                        bytes_downloaded += count;
                        
						double fileSize = (bytes_total / (1024 * 1024));
						double downloadedSize = (bytes_downloaded / (1024 * 1024));
						String formatted_fileSize = String.format("%.2f", fileSize).concat(" Mb");
						String formatted_downloadedSize = String.format("%.2f", downloadedSize).concat(" Mb");
                        
						changeText(output_messages, "Downloading ASR model in progress...");
						changeText(bytes_txt, formatted_downloadedSize + " / " + formatted_fileSize);
                        
						publishProgress((int)((bytes_downloaded * 100) / bytes_total));
						output.write(data, 0, count);
                    }
                    output.flush();
					output.close();
					input.close();
                }
                handler.post(() -> {
                    String extracted_path = FileUtil.getPackageDataDir(context).concat("/Models/");
					Decompress decompressFile = new Decompress(zipFileName(), extracted_path);
					decompressFile.unzip();
					FileUtil.deleteFile(zipFileName());
                    
					isDownloadingFile = false;
					changeText(output_messages, "Downloaded successfully!");
					changeText(downloadBtn, context.getString(R.string.toggleBtnStart));
                    
                    //update available models in storage directory
                    MainActivity.instantiateActivity()._updateVoskLanguage(vosk_data);
                    
					downloading_thread.interrupt();
					executor.shutdown();
                });
            } catch (Exception e) {
                isDownloadingFile = false;
                loading_progress.setVisibility(View.GONE);
                changeText(output_messages, e.getMessage());
                changeText(downloadBtn, context.getString(R.string.toggleBtnStart));
            }
        });
    }
    
    private String zipFileName() {
        String fileName = vosk_data.substring(vosk_data.lastIndexOf("vosk-"));
        File myFile = new File(FileUtil.getPackageDataDir(context).concat("/Models/") + fileName + ".zip");
        String string_myFile = myFile.toString();
        
        return string_myFile;
    }
    
    private void publishProgress(Integer... progress) {
        download_progress.setProgress(progress[0]);
    }
    
    private void changeText(final TextView _tv, final String _text) {
        new Handler(Looper.getMainLooper()).post(() -> _tv.setText(_text));
    }
    
    private boolean isOnline() {
        boolean networkAvailability = false;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm.getActiveNetworkInfo() != null) {
            networkAvailability = true;
        }
        
        return networkAvailability;
    }
}
