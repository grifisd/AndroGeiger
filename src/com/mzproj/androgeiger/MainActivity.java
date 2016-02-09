package com.mzproj.androgeiger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {

	private TextView imp, dose;
	private Button btn, btn2;
	private String tag = "debug";
	private Intent intent;
	SharedPreferences mySharedPreferences;
	
	private RadioCounter counter;
	public static boolean play = false;
	private boolean bool = true;
	private boolean b = false;
	private MediaPlayer mediaPlayer;
	private String model = "null";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mySharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE);
		final Editor editor = mySharedPreferences.edit();
		imp = (TextView) findViewById(R.id.tv1);
		dose = (TextView) findViewById(R.id.tv2);
		btn = (Button) findViewById(R.id.btnStart);
		mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.blue);
		if(mySharedPreferences.contains("model")) {
		    model = (mySharedPreferences.getString("model", ""));
		}
		counter = new RadioCounter(model, 43, this);

		btn.setOnClickListener(this);

		counter.start();
		// вывод дозы в отдельном потоке
		// в разработке - DONE.
		//while-true
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					synchronized (RadioCounter.getMonitor()) {
						try {
							Log.d(tag, "before wait()");
							RadioCounter.getMonitor().wait();
							Log.d(tag, "after wait()");
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								imp.setText(String.valueOf(counter.COUNTER));
								dose.setText(String.valueOf(counter.getLevel()));
								if (play) {
									mediaPlayer.start();
									play = false;
								}
							}
						});
					}
				}
			}
		}).start();
		/*
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
		
				synchronized (RadioCounter.getMonitor()) {
					try {
						while (true) {
							RadioCounter.getMonitor().wait();
							dose.setText(String.valueOf(counter.COUNTER));
							if (play) {
								mediaPlayer.start();
								play = false;
							}
						}
					} catch (InterruptedException e) {}
				}
				
			}
		});
		
		*/
	}

	@Override
	public void onClick(View v) {
			if (bool) {
				btn.setText("Stop");
				Log.d("debug", "Начало счета");
				counter.setRegister();
				bool = false;
			} else {
				btn.setText("Start");
				counter.setUnregister();
				bool = true;	
			}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final Editor editor = mySharedPreferences.edit();
		
		
		
		LinearLayout linLayout = new LinearLayout(this);
		linLayout.setOrientation(LinearLayout.VERTICAL);
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		LayoutParams lpView = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		
		TextView tv = new TextView(this);
		tv.setText("Марка счетчика Гейгера");        
		tv.setLayoutParams(lpView);        
		linLayout.addView(tv);
		final EditText editText = new EditText(this);
		editText.setLayoutParams(lpView);
		linLayout.addView(editText);
		Button btn = new Button(this);
		btn.setText("Сохранить");
		btn.setLayoutParams(lpView);
		btn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				editor.putString("model", editText.getText().toString());
				Toast.makeText(MainActivity.this, "Записано.", Toast.LENGTH_LONG).show();
			}
		});
		linLayout.addView(btn);
		setContentView(linLayout, params);
		return super.onOptionsItemSelected(item);
	}	
}
