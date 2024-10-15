package com.itbn.playsubtitle.v1;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import androidx.cardview.*;
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
import com.itbn.playsubtitle.v1.settings.SettingsFragment;

public class SettingsActivity extends Activity {
	
	private FrameLayout framelayout;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.settings);
		initialize(_savedInstanceState);
if (findViewById(R.id.framelayout) != null) {
if (_savedInstanceState != null) {
return;
}
getFragmentManager().beginTransaction().add(R.id.framelayout, new SettingsFragment()).commit();
}
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		framelayout = findViewById(R.id.framelayout);
	}
	
	private void initializeLogic() {
		setTitle("Settings");
	}
	
}