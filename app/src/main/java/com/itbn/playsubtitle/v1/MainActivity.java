package com.itbn.playsubtitle.v1;

import android.Manifest;
import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.*;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import androidx.cardview.*;
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
import com.itbn.playsubtitle.v1.settings.SettingsFragment;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.app.Fragment;
import android.database.Cursor;
import android.provider.MediaStore;
import java.util.concurrent.TimeUnit;
import java.lang.ref.WeakReference;

public class MainActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {
	
	//define global variables
	private Cursor cursor;
	private SharedPreferences languagePreference;
	private Extraction extraction;
	private Transcription transcription;
	private boolean modelAvailability = false;
	
	public static WeakReference<MainActivity> weakActivity;
	private HashMap<String, Object> videoInfo = new HashMap<>();
	private HashMap<String, Object> voskModelList = new HashMap<>();
	private String fileName = "";
	
	private ArrayList<HashMap<String, Object>> videoInfoList = new ArrayList<>();
	private ArrayList<String> modelsList = new ArrayList<>();
	
	private ListView listview1;
	
	private long exitApp;
	private Intent gotoActivity = new Intent();
	private Intent shareFile = new Intent();
	private AlertDialog dialog;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.main);
		initialize(_savedInstanceState);
		
		if (Build.VERSION.SDK_INT >= 23) {
			if (checkSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_DENIED
			||checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
			||checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
				requestPermissions(new String[] {Manifest.permission.INTERNET, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
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
		languagePreference = PreferenceManager.getDefaultSharedPreferences(this);
		weakActivity = new WeakReference<>(MainActivity.this);
		listview1 = findViewById(R.id.listview1);
		
		listview1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> _param1, View _param2, int _param3, long _param4) {
				final int _position = _param3;
				if (cursor.moveToPosition(_position)) {
					if (languagePreference.contains(_getDisplayName(videoInfoList, _position))) {
						if (FileUtil.isExistFile(languagePreference.getString(_getDisplayName(videoInfoList, _position), ""))) {
							gotoActivity.setClass(getApplicationContext(), VideoplayerActivity.class);
							gotoActivity.putExtra("play_video", videoInfoList.get((int)_position).get("filePath").toString());
							gotoActivity.putExtra("subtitle", languagePreference.getString(_getDisplayName(videoInfoList, _position), ""));
							startActivity(gotoActivity);
						}
						else {
							dialog = new AlertDialog.Builder(MainActivity.this)
							.setTitle("Warning!")
							.setIcon(R.drawable.ic_warning_black)
							.setMessage("It appears that the subtitle for this particular video is missing. Please assign an SRT file as source, or use the inbuilt transcriber to generate it automatically. Note that language translation requires internet and can be turned off in Settings.")
							.setPositiveButton("TRANSCRIBE", new DialogInterface.OnClickListener() {
								@Override public void onClick(DialogInterface _dialog, int _which) {
									_transcribeAudio(videoInfoList, _position, _param2);
								}
							})
							.setNegativeButton("ASSIGN", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface _dialog, int _which) {
									_assignSubtitle(_getDisplayName(videoInfoList, _position));
								}
							})
							.create();
							
							dialog.show();
						}
					}
					else {
						dialog = new AlertDialog.Builder(MainActivity.this)
						.setTitle("Warning!")
						.setIcon(R.drawable.ic_warning_black)
						.setMessage("It appears that the subtitle for this particular video is missing. Please assign an SRT file as source, or use the inbuilt transcriber to generate it automatically. Note that language translation requires internet and can be turned off in Settings.")
						.setPositiveButton("TRANSCRIBE", new DialogInterface.OnClickListener() {
							@Override public void onClick(DialogInterface _dialog, int _which) {
								_transcribeAudio(videoInfoList, _position, _param2);
							}
						})
						.setNegativeButton("ASSIGN", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface _dialog, int _which) {
								_assignSubtitle(_getDisplayName(videoInfoList, _position));
							}
						})
						.create();
						
						dialog.show();
					}
				}
			}
		});
		
		listview1.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> _param1, View _param2, int _param3, long _param4) {
				final int _position = _param3;
				if (cursor.moveToPosition(_position)) {
					PopupMenu popupMenu = new PopupMenu(MainActivity.this, _param2);
					popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());
					popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
						    @Override
						    public boolean onMenuItemClick(MenuItem _menuItem) {
							       final String menuItem = (String) _menuItem.getTitle();
							switch(menuItem) {
								case "Details": {
									final View infoView = getLayoutInflater().inflate(R.layout.info_dialog_layout, null);
									
									TextView name = infoView.findViewById(R.id.name_detail);
									TextView path = infoView.findViewById(R.id.path_detail);
									TextView resolution = infoView.findViewById(R.id.resolution_detail);
									TextView date = infoView.findViewById(R.id.date_detail);
									TextView subtitle = infoView.findViewById(R.id.subtitle_detail);
									name.setText(videoInfoList.get((int)_position).get("title").toString());
									path.setText(videoInfoList.get((int)_position).get("filePath").toString());
									resolution.setText(videoInfoList.get((int)_position).get("resolution").toString());
									long dateAdded = Long.parseLong(videoInfoList.get((int)_position).get("date").toString());
									Date videoDate = new Date(TimeUnit.SECONDS.toMillis(dateAdded));
									date.setText(videoDate.toString());
									subtitle.setText(languagePreference.getString(_getDisplayName(videoInfoList, _position), ""));
									dialog = new AlertDialog.Builder(MainActivity.this)
									.setIcon(R.drawable.ic_info_black)
									.setTitle("Video details")
									.setView(infoView)
									.setNegativeButton("CLOSE", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface _dialog, int _which) {
											 
										}
									})
									.create();
									
									dialog.show();
									break;
								}
								case "Assign subtitle": {
									_assignSubtitle(_getDisplayName(videoInfoList, _position));
									break;
								}
								case "Extract audio": {
									extraction = new Extraction(MainActivity.this);
									String videoPath = videoInfoList.get((int)_position).get("filePath").toString();
									String wavPath = FileUtil.getExternalStorageDir().concat("/PlaySubtitle/Audio/" + _getDisplayName(videoInfoList, _position) + ".wav");
									
									extraction.startExtraction(_param2, videoPath, wavPath);
									break;
								}
								case "Transcribe voice": {
									_transcribeAudio(videoInfoList, _position, _param2);
									break;
								}
								case "Rename": {
									EditText fileName = new EditText(MainActivity.this);
									fileName.setTextSize((int)16);
									fileName.setSingleLine(true);
									
									FrameLayout container = new FrameLayout(MainActivity.this);
									LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
									ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
									lp.leftMargin = 25;
									lp.rightMargin = 25;
									fileName.setLayoutParams(lp);
									container.addView(fileName);
									
									String path = videoInfoList.get((int)_position).get("filePath").toString();
									fileName.setText(_getDisplayName(videoInfoList, _position));
									
									dialog = new AlertDialog.Builder(MainActivity.this)
									.setIcon(R.drawable.ic_edit_black)
									.setTitle("Rename file?")
									.setView(container)
									.setPositiveButton("OK", new DialogInterface.OnClickListener() {
										@Override public void onClick(DialogInterface _dialog, int _which) {
											{
												java.io.File dYx4Y = new java.io.File(path);
												java.io.File e5Cyk = new java.io.File(path.replace(_getDisplayName(videoInfoList, _position), fileName.getText().toString()));
												dYx4Y.renameTo(e5Cyk);
											}
											SketchwareUtil.showMessage(getApplicationContext(), "Renamed successfully!");
											_scanFileStorage();
										}
									})
									.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface _dialog, int _which) {
											 
										}
									})
									.create();
									
									dialog.show();
									break;
								}
								case "Delete": {
									dialog = new AlertDialog.Builder(MainActivity.this)
									.setIcon(R.drawable.ic_delete_black)
									.setTitle("Delete file permanently?")
									.setMessage(videoInfoList.get((int)_position).get("filePath").toString())
									.setPositiveButton("OK", new DialogInterface.OnClickListener() {
										@Override public void onClick(DialogInterface _dialog, int _which) {
											languagePreference.edit().remove(_getDisplayName(videoInfoList, _position)).commit();
											FileUtil.deleteFile(videoInfoList.get((int)_position).get("filePath").toString());
											videoInfoList.remove((int)(_position));
											SketchwareUtil.showMessage(getApplicationContext(), "Deleted successfully!");
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
									break;
								}
								case "Share": {
									shareFile.setAction(Intent.ACTION_SEND);
									shareFile.setType("video/mp4");
									shareFile.putExtra(Intent.EXTRA_STREAM, videoInfoList.get((int)_position).get("filePath").toString());
									startActivity(Intent.createChooser(shareFile, "Share Video File"));
									break;
								}
							}
							return true;
						}
					});
					popupMenu.show();
				}
				return true;
			}
		});
	}
	
	private void initializeLogic() {
		
		
		
		FileUtil.makeDir(FileUtil.getPackageDataDir(getApplicationContext()).concat("/Models"));
		FileUtil.makeDir(FileUtil.getPackageDataDir(getApplicationContext()).concat("/Subtitles"));
		FileUtil.makeDir(FileUtil.getExternalStorageDir().concat("/PlaySubtitle/Audio"));
		_setupSharedPreferences();
	}
	
	public static MainActivity instantiateActivity() {
		    return weakActivity.get();
	}
	
	private boolean allUnchecked(boolean[] _array) {
		    boolean unchecked = true;
		    for (boolean z : _array) {
			        if (z) {
				            unchecked = false;
				        }
			    }
		    return unchecked;
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences _preference, String _key) {
		switch(_key) {
			case "sortlist_items": {
				_sortListOrder(_preference.getString(getString(R.string.sortlist_key), getString(R.string.date_des_value)));
				break;
			}
			case "vosk_model": {
				_updateVoskLanguage(_preference.getString(getString(R.string.vosk_preference_key), getString(R.string.tr_value)));
				break;
			}
			case "min_period": {
				_updateConf(_preference.getString(getString(R.string.min_active_key), getString(R.string.min_active_period)), 0);
				break;
			}
			case "max_period": {
				_updateConf(_preference.getString(getString(R.string.max_active_key), getString(R.string.max_active_period)), 1);
				break;
			}
		}
	}
	
	
	@Override
	public void onBackPressed() {
		if (ActivityHelper.popupWindow != null) {
			if (transcription != null) {
				transcription.terminateSession();
			}
			if (extraction != null) {
				extraction.terminateSession();
			}
		}
		else {
			if ((exitApp + 2000) > System.currentTimeMillis()) {
				finish();
			}
			else {
				SketchwareUtil.showMessage(getApplicationContext(), "Press again to exit");
				exitApp = System.currentTimeMillis();
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int _id = item.getItemId();
		final String _title = (String) item.getTitle();
		switch(_title) {
			case "Models list": {
				final String[] listItems = modelsList.toArray(new String[modelsList.size()]);
				List<String> selectedItems = new ArrayList<>();
				final boolean[] checkedItems = new boolean[listItems.length];
				
				if (listItems.length > 0) {
					dialog = new AlertDialog.Builder(MainActivity.this)
					.setMultiChoiceItems(listItems, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
						@Override
						public void onClick(DialogInterface _dialog, int _position, boolean _isChecked) {
							checkedItems[_position] = _isChecked;
							int length = checkedItems.length;
							if (_isChecked) {
								selectedItems.add(listItems[_position]);
								dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
							}
							else {
								selectedItems.remove(listItems[_position]);
								if (allUnchecked(checkedItems)) {
									dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
								}
							}
						}
					})
					.setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
						@Override public void onClick(DialogInterface _dialog, int _which) {
							for (int i = 0; i < (int)(selectedItems.size()); i++) {
								FileUtil.deleteFile(voskModelList.get(selectedItems.get(i)).toString());
							}
							SketchwareUtil.showMessage(getApplicationContext(), "Deleted successfully!");
							_updateVoskLanguage(languagePreference.getString(getString(R.string.vosk_preference_key), getString(R.string.tr_value)));
						}
					})
					.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface _dialog, int _which) {
							 
						}
					})
					.setCancelable(false)
					.setIcon(R.drawable.ic_archive_black)
					.setTitle("ASR models")
					.create();
					
					dialog.show();
					dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
				}
				else {
					SketchwareUtil.showMessage(getApplicationContext(), "Directory is empty!");
				}
				break;
			}
			case "Settings": {
				gotoActivity.setClass(getApplicationContext(), SettingsActivity.class);
				startActivity(gotoActivity);
				break;
			}
			case "About": {
				gotoActivity.setClass(getApplicationContext(), AboutActivity.class);
				startActivity(gotoActivity);
				break;
			}
			case "Exit": {
				finish();
				break;
			}
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
	}
	public void _scanFileStorage() {
		runOnUiThread(new Runnable() {
			public void run() {
				String[] mediaColumn = new String[] {
					    MediaStore.Video.Media._ID,
					    MediaStore.MediaColumns.DATA,
					    MediaStore.MediaColumns.MIME_TYPE,
					    MediaStore.MediaColumns.DISPLAY_NAME,
					    MediaStore.MediaColumns.TITLE,
					    MediaStore.MediaColumns.SIZE,
					    MediaStore.MediaColumns.DATE_ADDED,
					    MediaStore.Video.VideoColumns.DURATION,
					    MediaStore.Video.VideoColumns.RESOLUTION
				};
				
				String selection = MediaStore.MediaColumns.MIME_TYPE + "=?";
				
				String[] selectionArgs = new String[] {
					    MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp4")
				};
				cursor = getApplicationContext().getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mediaColumn, selection, selectionArgs, null);
				
				videoInfo.clear();
				videoInfoList.clear();
				if (cursor.moveToFirst()) {
					do {
						videoInfo = new HashMap<>();
						
						int videoID = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media._ID));
						Bitmap thumbID = MediaStore.Video.Thumbnails.getThumbnail(getContentResolver(), videoID, MediaStore.Video.Thumbnails.MICRO_KIND, null);
						try {
							if (thumbID != null) {
								videoInfo.put("thumbnail", videoID);
								
								String videoPath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA));
								videoInfo.put("filePath", videoPath);
								String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE));
								videoInfo.put("type", mimeType);
								
								String videoNAME = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME));
								videoInfo.put("displayName", videoNAME);
								
								String videoTITLE = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.TITLE));
								videoInfo.put("title", videoTITLE);
								
								long videoSIZE = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE));
								videoInfo.put("size", videoSIZE);
								long dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED));
								videoInfo.put("date", dateAdded);
								
								int videoDURATION = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION));
								videoInfo.put("duration", videoDURATION);
								String videoResolution = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.RESOLUTION));
								videoInfo.put("resolution", videoResolution);
								videoInfoList.add(videoInfo);
								listview1.setAdapter(new Listview1Adapter(videoInfoList));
								_sortListOrder(languagePreference.getString(getString(R.string.sortlist_key), getString(R.string.date_des_value)));
							}
						} catch (Exception e) {
							SketchwareUtil.showMessage(getApplicationContext(), e.getMessage());
						}
					}
					while (cursor.moveToNext());
				}
			}
		});
	}
	
	
	public void _updateConf(final String _data, final int _line) {
		if (ActivityHelper.configAvailability == true) {
			
			File config = new File(fileName);
			
			try {
				//read the file
				BufferedReader bread = new BufferedReader(new FileReader(config));
				StringBuffer sb = new StringBuffer();
				String text = "";
				while((text = bread.readLine()) != null) {
					sb.append(text);
					sb.append("\n");
				}
				bread.close();
				
				//apply changes to the file
				String input = sb.toString();
				String[] getLine = input.split("\\n");
				String targetLine = getLine[_line];
				String editedLine = targetLine.substring(targetLine.lastIndexOf("="));
				String conf = input.replace(editedLine, "=" + _data);
				
				//rewrite the file
				try (FileWriter writer = new FileWriter(config)) {
					    writer.write(conf);
					    writer.close();
				}
			} catch (Exception e) {
				SketchwareUtil.showMessage(getApplicationContext(), e.getMessage());
			}
		}
	}
	
	
	public void _setupSharedPreferences() {
		_scanFileStorage();
		_updateVoskLanguage(languagePreference.getString(getString(R.string.vosk_preference_key), getString(R.string.tr_value)));
		_updateConf(languagePreference.getString(getString(R.string.min_active_key), getString(R.string.min_active_period)), 0);
		_updateConf(languagePreference.getString(getString(R.string.max_active_key), getString(R.string.max_active_period)), 1);
		languagePreference.registerOnSharedPreferenceChangeListener(this);
	}
	
	
	public void _sortListOrder(final String _preference) {
		switch(_preference) {
			case "name_asc": {
				SketchwareUtil.sortListMap(videoInfoList, "title", false, true);
				((BaseAdapter)listview1.getAdapter()).notifyDataSetChanged();
				break;
			}
			case "name_des": {
				SketchwareUtil.sortListMap(videoInfoList, "title", false, true);
				Collections.reverse(videoInfoList);
				((BaseAdapter)listview1.getAdapter()).notifyDataSetChanged();
				break;
			}
			case "size_asc": {
				SketchwareUtil.sortListMap(videoInfoList, "size", true, true);
				((BaseAdapter)listview1.getAdapter()).notifyDataSetChanged();
				break;
			}
			case "size_des": {
				SketchwareUtil.sortListMap(videoInfoList, "size", true, true);
				Collections.reverse(videoInfoList);
				((BaseAdapter)listview1.getAdapter()).notifyDataSetChanged();
				break;
			}
			case "date_asc": {
				SketchwareUtil.sortListMap(videoInfoList, "date", true, true);
				((BaseAdapter)listview1.getAdapter()).notifyDataSetChanged();
				break;
			}
			case "date_des": {
				SketchwareUtil.sortListMap(videoInfoList, "date", true, true);
				Collections.reverse(videoInfoList);
				((BaseAdapter)listview1.getAdapter()).notifyDataSetChanged();
				break;
			}
		}
	}
	
	
	public void _togglePreferenceView(final boolean _configAvailability) {
		if (ActivityHelper.onSettingsCreated) {
			SettingsFragment.getInstance(). disableConfigurationSetting(_configAvailability);
		}
	}
	
	
	public void _checkConfigAvailability(final String _folderPath) {
		//retrieve file from external storage
		File modelDir = new File(_folderPath);
		File[] configList = modelDir.listFiles();
		for (int c = 0; c < (int)(configList.length); c++) {
			if (configList[c].isDirectory()) {
				_checkConfigAvailability(configList[c].toString());
			}
			else {
				if (configList[c].getName().equals("model.conf")) {
					ActivityHelper.configAvailability = true;
					fileName = configList[c].getAbsolutePath();
					_togglePreferenceView(ActivityHelper.configAvailability);
				}
			}
		}
	}
	
	
	public void _updateVoskLanguage(final String _data) {
		voskModelList = new HashMap<>();
		modelsList.clear();
		
		//check for model's availability
		File modelDir = new File(FileUtil.getPackageDataDir(getApplicationContext()).concat("/Models"));
		File[] modelList = modelDir.listFiles();
		
		if (modelList != null && modelList.length > 0) {
			for (int m = 0; m < (int)(modelList.length); m++) {
				if (!modelList[m].getName().endsWith(".zip")) {
					modelsList.add(modelList[m].getName());
					voskModelList.put(modelList[m].getName(), modelList[m].getPath());
				}
			}
		}
		
		String folderName = _data.substring(_data.lastIndexOf("vosk-")).replace(".zip", "");
		
		if (voskModelList.containsKey(folderName)) {
			modelAvailability = true;
			ActivityHelper.configAvailability = false;
			_togglePreferenceView(ActivityHelper.configAvailability);
			_checkConfigAvailability(voskModelList.get(folderName).toString());
		}
		else {
			modelAvailability = false;
			ActivityHelper.configAvailability = false;
			_togglePreferenceView(ActivityHelper.configAvailability);
		}
	}
	
	
	public void _assignSubtitle(final String _key) {
		EditText sourcePath = new EditText(MainActivity.this);
		sourcePath.setTextSize((int)16);
		sourcePath.setSingleLine(true);
		
		FrameLayout container = new FrameLayout(MainActivity.this);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
		ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lp.leftMargin = 25;
		lp.rightMargin = 25;
		sourcePath.setLayoutParams(lp);
		container.addView(sourcePath);
		
		dialog = new AlertDialog.Builder(MainActivity.this)
		.setIcon(R.drawable.ic_subtitles_black)
		.setTitle("Assign subtitle")
		.setMessage("Set path to location of the source file.\neg: /storage/emulated/0/PlayScript/Subtitles/mysubtitle.srt")
		.setView(container)
		.setPositiveButton("SET", new DialogInterface.OnClickListener() {
			@Override public void onClick(DialogInterface _dialog, int _which) {
				languagePreference.edit().putString(_key, sourcePath.getText().toString()).commit();
				SketchwareUtil.showMessage(getApplicationContext(), "Assigned successfully!");
				_scanFileStorage();
			}
		})
		.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface _dialog, int _which) {
				 
			}
		})
		.create();
		
		dialog.show();
		dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
		sourcePath.addTextChangedListener(new TextWatcher() {
			    @Override
			    public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				        if (_param1.length() > 0) {
					            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
					        } else {
					            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
					        }
				    }
			    
			    @Override
			    public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				        //unnecessary
				    }
			    
			    @Override
			    public void afterTextChanged(Editable _param1) {
				        //unnecessary
				    }
		});
	}
	
	
	public String _getDisplayName(final ArrayList<HashMap<String, Object>> _data, final double _position) {
		String name = _data.get((int)_position).get("displayName").toString();
		String extension = name.substring(name.lastIndexOf("."));
		return (name.replace(extension, ""));
	}
	
	
	public void _transcribeAudio(final ArrayList<HashMap<String, Object>> _data, final double _position, final View _view) {
		ModelsDownloader md = new ModelsDownloader(MainActivity.this);
		
		String subtitleName = _getDisplayName(_data, (int)_position);
		String model = languagePreference.getString(getString(R.string.vosk_preference_key), getString(R.string.tr_value));
		String modelKey = model.substring(model.lastIndexOf("vosk-")).replace(".zip", "");
		String videoPath = _data.get((int)_position).get("filePath").toString();
		ActivityHelper.durationInMillis = Double.parseDouble(_data.get((int)_position).get("duration").toString());
		if (modelAvailability) {
			transcription = new Transcription(MainActivity.this, voskModelList.get(modelKey).toString());
			transcription.startTranscription(_view, videoPath, subtitleName);
		}
		else {
			md.showDownloadWindow(_view);
		}
	}
	
	public class Listview1Adapter extends BaseAdapter {
		
		ArrayList<HashMap<String, Object>> _data;
		
		public Listview1Adapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}
		
		@Override
		public int getCount() {
			return _data.size();
		}
		
		@Override
		public HashMap<String, Object> getItem(int _index) {
			return _data.get(_index);
		}
		
		@Override
		public long getItemId(int _index) {
			return _index;
		}
		
		@Override
		public View getView(final int _position, View _v, ViewGroup _container) {
			LayoutInflater _inflater = getLayoutInflater();
			View _view = _v;
			if (_view == null) {
				_view = _inflater.inflate(R.layout.video_listview, null);
			}
			
			final LinearLayout linear1 = _view.findViewById(R.id.linear1);
			final ImageView imageview1 = _view.findViewById(R.id.imageview1);
			final LinearLayout linear2 = _view.findViewById(R.id.linear2);
			final TextView textview1 = _view.findViewById(R.id.textview1);
			final LinearLayout linear3 = _view.findViewById(R.id.linear3);
			final TextView textview2 = _view.findViewById(R.id.textview2);
			final LinearLayout linear4 = _view.findViewById(R.id.linear4);
			final ImageView imageview2 = _view.findViewById(R.id.imageview2);
			final TextView textview3 = _view.findViewById(R.id.textview3);
			
			if (_data != null) {
				int videoID = Integer.parseInt(_data.get((int)_position).get("thumbnail").toString());
				Bitmap bitmap = MediaStore.Video.Thumbnails.getThumbnail(getContentResolver(), videoID, MediaStore.Video.Thumbnails.MICRO_KIND, null);
				Bitmap videoThumbnail = Bitmap.createScaledBitmap(bitmap, 75, 75, true);
				
				imageview1.setImageBitmap(videoThumbnail);
				
				if (FileUtil.isExistFile(languagePreference.getString(_getDisplayName(_data, _position), ""))) {
					imageview2.setVisibility(View.VISIBLE);
				}
				else {
					languagePreference.edit().remove(_getDisplayName(_data, _position)).commit();
					imageview2.setVisibility(View.INVISIBLE);
				}
				String videoName = _data.get((int)_position).get("displayName").toString();
				String extension = videoName.substring(videoName.lastIndexOf("."));
				String name = videoName.replace(extension, "");
				textview1.setText(name);
				String videoSize = "";
				double bytes = Double.parseDouble(_data.get((int)_position).get("size").toString());
				
				double kilobytes = 1000;
				double megabytes = kilobytes * 1024;
				double gigabytes = megabytes * 1024;
				if (bytes < megabytes) {
					double sizeKilo = bytes / 1024;
					videoSize = String.format("%.2f", sizeKilo).concat(" Kb");
				}
				else {
					if ((bytes > megabytes) && (bytes < gigabytes)) {
						double sizeMega = bytes / (1024 * 1024);
						videoSize = String.format("%.2f", sizeMega).concat(" Mb");
					}
					else {
						if (bytes > gigabytes) {
							double sizeGiga = bytes / (1024 * 1024 * 1024);
							videoSize = String.format("%.2f", sizeGiga).concat(" Gb");
						}
					}
				}
				String mimeType = _data.get((int)_position).get("type").toString();
				textview2.setText(videoSize + " | " + mimeType.replace("video/", "").toUpperCase());
				String duration = ActivityHelper.formatDuration(Integer.parseInt(_data.get((int)_position).get("duration").toString()));
				textview3.setText(duration);
			}
			else {
				textview1.setText("No item found!");
			}
			
			return _view;
		}
	}
}