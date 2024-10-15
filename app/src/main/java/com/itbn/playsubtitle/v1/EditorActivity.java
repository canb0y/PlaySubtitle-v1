package com.itbn.playsubtitle.v1;

import android.Manifest;
import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.*;
import android.content.pm.PackageManager;
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
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import androidx.cardview.*;
import androidx.cardview.widget.CardView;
import com.google.gson.*;
import com.sun.jna.*;
import io.github.nailik.androidresampler.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.*;
import kotlin.*;
import okhttp3.*;
import okio.*;
import org.json.*;
import org.vosk.*;

public class EditorActivity extends Activity {
	
	String[] alt1, alt2, alt3;
	private double position = 0;
	private String srt_path = "";
	private HashMap<String, Object> subtitle = new HashMap<>();
	
	private ArrayList<String> captions = new ArrayList<>();
	private ArrayList<String> timestamps = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> subtitleData = new ArrayList<>();
	private ArrayList<Double> sequence = new ArrayList<>();
	
	private LinearLayout linear1;
	private LinearLayout linear5;
	private ListView listview1;
	private LinearLayout linear4;
	private LinearLayout linear2;
	private LinearLayout linear3;
	private ImageView imageview1;
	private EditText edittext1;
	private Button button1;
	private TextView textview4;
	private CardView cardview1;
	private CardView cardview2;
	private CardView cardview3;
	private TextView textview1;
	private TextView textview2;
	private TextView textview3;
	private TextView textview5;
	
	private AlertDialog dialog;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.editor);
		initialize(_savedInstanceState);
		
		if (Build.VERSION.SDK_INT >= 23) {
			if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
			||checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
				requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
			} else {
				initializeLogic();
			}
		} else {
			initializeLogic();
		}
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == 1000) {
			initializeLogic();
		}
	}
	
	private void initialize(Bundle _savedInstanceState) {
		linear1 = findViewById(R.id.linear1);
		linear5 = findViewById(R.id.linear5);
		listview1 = findViewById(R.id.listview1);
		linear4 = findViewById(R.id.linear4);
		linear2 = findViewById(R.id.linear2);
		linear3 = findViewById(R.id.linear3);
		imageview1 = findViewById(R.id.imageview1);
		edittext1 = findViewById(R.id.edittext1);
		button1 = findViewById(R.id.button1);
		textview4 = findViewById(R.id.textview4);
		cardview1 = findViewById(R.id.cardview1);
		cardview2 = findViewById(R.id.cardview2);
		cardview3 = findViewById(R.id.cardview3);
		textview1 = findViewById(R.id.textview1);
		textview2 = findViewById(R.id.textview2);
		textview3 = findViewById(R.id.textview3);
		textview5 = findViewById(R.id.textview5);
		
		listview1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> _param1, View _param2, int _param3, long _param4) {
				final int _position = _param3;
				position = _position;
				edittext1.setText(captions.get((int)(position)));
				textview1.setText(alt1[(int)(position)]);
				textview2.setText(alt2[(int)(position)]);
				textview3.setText(alt3[(int)(position)]);
			}
		});
		
		button1.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (edittext1.getText().toString().equals("")) {
					((EditText)edittext1).setError("Please fill in the blank");
				}
				else {
					double captionCount = sequence.get((int) position).doubleValue();
					String seq = String.valueOf(Math.round(captionCount + 1.0));
					dialog = new AlertDialog.Builder(EditorActivity.this)
					.setTitle("Edit caption " + seq)
					.setIcon(R.drawable.ic_warning_black)
					.setMessage("Changes applied can not be reversed. Are you sure?")
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						@Override public void onClick(DialogInterface _dialog, int _which) {
							captions.set((int)position, edittext1.getText().toString());
							((BaseAdapter)listview1.getAdapter()).notifyDataSetChanged();
						}
					})
					.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface _dialog, int _which) {
							 
						}
					})
					.create();
					
					dialog.show();
				}
			}
		});
		
		cardview1.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				String altText = textview1.getText().toString();
				textview1.setText(captions.get((int)(position)));
				alt1[(int)position] = textview1.getText().toString();
				edittext1.setText(altText);
				captions.set((int)position, altText);
				((BaseAdapter)listview1.getAdapter()).notifyDataSetChanged();
			}
		});
		
		cardview2.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				String altText = textview2.getText().toString();
				textview2.setText(captions.get((int)(position)));
				alt2[(int)position] = textview2.getText().toString();
				edittext1.setText(altText);
				captions.set((int)position, altText);
				((BaseAdapter)listview1.getAdapter()).notifyDataSetChanged();
			}
		});
		
		cardview3.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				String altText = textview3.getText().toString();
				textview3.setText(captions.get((int)(position)));
				alt3[(int)position] = textview3.getText().toString();
				edittext1.setText(altText);
				captions.set((int)position, altText);
				((BaseAdapter)listview1.getAdapter()).notifyDataSetChanged();
			}
		});
	}
	
	private void initializeLogic() {
		
		setTitle("Captions Editor");
		srt_path = getIntent().getStringExtra("subtitle");
		try {
			File file = new File(srt_path);
			
			//read srt file to memory
			BufferedReader bread = new BufferedReader(new FileReader(file));
			StringBuffer sb = new StringBuffer();
			String text = "";
			while((text = bread.readLine()) != null) {
				if (text.equals("1")) {
					sb.append("\n");
				}
				sb.append(text);
				sb.append("\n");
			}
			bread.close();
			
			//obtain captions and timestamps
			int item = 0;
			int seqCount = 0;
			String input = sb.toString();
			String[] lines = input.split("\n");
			
			for (int i = 0; i < (int)(lines.length); i++) {
				item++;
				
				if (item == 3) {
					sequence.add(Double.valueOf(seqCount++));
					timestamps.add(lines[i]);
				}
				if (item == 4) {
					captions.add(lines[i]);
					item = 0;
				}
			}
			
		} catch (IOException e) {
			SketchwareUtil.showMessage(getApplicationContext(), e.getMessage());
		}
		_setAlternatives(0);
		edittext1.setText(captions.get((int)(0)));
		listview1.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_list_item_1, captions));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.subtitle_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int _id = item.getItemId();
		final String _title = (String) item.getTitle();
		switch(_title) {
			case "Save File": {
				StringBuilder sb = new StringBuilder();
				int count = 0;
				for (int i = 0; i < (int)(timestamps.size()); i++) {
					count++;
					
					sb.append(String.valueOf(count) + "\n");
					sb.append(timestamps.get(i) + "\n");
					sb.append(captions.get(i) + "\n");
					if (i != timestamps.size() - 1) {
						sb.append("\n");
					}
				}
				FileUtil.writeFile(srt_path, sb.toString());
				SketchwareUtil.showMessage(getApplicationContext(), "Saved successfully!");
				finish();
				break;
			}
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		MainActivity.instantiateActivity()._scanFileStorage();
	}
	
	@Override
	public void onBackPressed() {
		dialog = new AlertDialog.Builder(EditorActivity.this)
		.setTitle("Exit editor?")
		.setIcon(R.drawable.ic_warning_black)
		.setMessage("Exiting the editor without applying changes. Are you sure?")
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override public void onClick(DialogInterface _dialog, int _which) {
				finish();
			}
		})
		.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface _dialog, int _which) {
				 
			}
		})
		.create();
		
		dialog.show();
	}
	public void _setAlternatives(final double _position) {
		alt1 = ActivityHelper.alternatives.get(0).toString().split("\n");
		alt2 = ActivityHelper.alternatives.get(1).toString().split("\n");
		alt3 = ActivityHelper.alternatives.get(2).toString().split("\n");
		textview1.setText(alt1[(int)(_position)]);
		textview2.setText(alt2[(int)(_position)]);
		textview3.setText(alt3[(int)(_position)]);
	}
	
}