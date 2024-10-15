package com.itbn.playsubtitle.v1;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.*;
import android.content.Intent;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.cardview.*;
import androidx.cardview.widget.CardView;
import com.google.gson.*;
import com.sun.jna.*;
import io.github.nailik.androidresampler.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import kotlin.*;
import okhttp3.*;
import okio.*;
import org.json.*;
import org.vosk.*;

public class AboutActivity extends Activity {
	
	private LinearLayout linear1;
	private CardView cardview1;
	private LinearLayout linear2;
	private TextView textview1;
	private TextView textview2;
	private LinearLayout linear3;
	private TextView textview3;
	private TextView textview5;
	private TextView textview6;
	private TextView textview7;
	private TextView textview9;
	private TextView textview4;
	private TextView textview8;
	
	private Intent gotoURL = new Intent();
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.about);
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		linear1 = findViewById(R.id.linear1);
		cardview1 = findViewById(R.id.cardview1);
		linear2 = findViewById(R.id.linear2);
		textview1 = findViewById(R.id.textview1);
		textview2 = findViewById(R.id.textview2);
		linear3 = findViewById(R.id.linear3);
		textview3 = findViewById(R.id.textview3);
		textview5 = findViewById(R.id.textview5);
		textview6 = findViewById(R.id.textview6);
		textview7 = findViewById(R.id.textview7);
		textview9 = findViewById(R.id.textview9);
		textview4 = findViewById(R.id.textview4);
		textview8 = findViewById(R.id.textview8);
		
		textview5.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				gotoURL.setAction(Intent.ACTION_VIEW);
				gotoURL.setData(Uri.parse("https://".concat("alphacephei.com/vosk/")));
				startActivity(gotoURL);
			}
		});
		
		textview6.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				gotoURL.setAction(Intent.ACTION_VIEW);
				gotoURL.setData(Uri.parse("https://".concat("libretranslate.com/")));
				startActivity(gotoURL);
			}
		});
		
		textview7.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				gotoURL.setAction(Intent.ACTION_VIEW);
				gotoURL.setData(Uri.parse("https://".concat("github.com/Nailik/AndroidResampler")));
				startActivity(gotoURL);
			}
		});
		
		textview8.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				gotoURL.setAction(Intent.ACTION_VIEW);
				gotoURL.setData(Uri.parse("https://".concat("github.com/canb0y")));
				startActivity(gotoURL);
			}
		});
	}
	
	private void initializeLogic() {
		setTitle("About");
	}
	
}