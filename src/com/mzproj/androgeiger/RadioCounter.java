package com.mzproj.androgeiger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.util.Log;

public class RadioCounter extends Thread {
	String name = null;
	String tag = "debug";
	Integer COUNTER;
	Context context;

	public RadioCounter(String name, Integer COUNTER, Context context) {
		this.COUNTER = 0;
		this.name = name;
		this.context = context;
		RadioCounter.monitor = new Object();
	}

	private static Object monitor;
	private SensorManager sensorManager;
	private Sensor lightSensor;
	private SensorEventListener lightListener;
	private GPSTracker gps;

	private long startTime = 0;
	private double now = 0;
	private final double LOW = (double) 26 / (double) 60;
	private final double HI = (double) 65 / (double) 60;
	private double level = 0;
	private String STATE = "NULL";
	private boolean isRegistered = false;

	// =============WRITING IN SD CARD========
	// ===============00000000000=============

	static void writeLocation(double latitide, double longitude, String level) {
		if (!"NULL".equals(level)) {
			String filename = "Tracks.csv";
			String toSend = String.valueOf(latitide) + ", " + String.valueOf(longitude) + ", " + level + "\n";

			if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				Log.d("debug", "SD-карта не доступна: " + Environment.getExternalStorageState());
				return;
			}
			File sdPath = Environment.getExternalStorageDirectory();
			File sdFile = new File(sdPath, filename);
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(sdFile, true));
				bw.write(toSend);
				bw.close();
				Log.d("debug", "Файл записан на SD: " + sdFile.getAbsolutePath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// THIS IF END.
	// ======000000======

	// register and unregister

	public void setRegister() {
		sensorManager.registerListener(lightListener, lightSensor, SensorManager.SENSOR_DELAY_FASTEST);
		startTime = System.currentTimeMillis();
		isRegistered = true;
	}

	public void setUnregister() {
		sensorManager.unregisterListener(lightListener, lightSensor);
		startTime = 0;
		COUNTER = 0;
		isRegistered = false;
	}

	@Override
	public void run() {
		sensorManager = (SensorManager) context.getSystemService(context.SENSOR_SERVICE);
		lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		lightListener = new SensorEventListener() {

			@Override
			public synchronized void onSensorChanged(SensorEvent event) {
				synchronized (getMonitor()) {
					if (event.values[0] > 0)
						COUNTER++;
				}
			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				// 4TO HE TAK XA-XA
			}
		};

		while (true) {
			if (isRegistered) {
				now = (System.currentTimeMillis() - (double) startTime) / 1000;
				if (now >= 5) {
					setLevel(COUNTER / now);

					if (getLevel() >= HI) {
						STATE = "HIGH";
					} else if (getLevel() > LOW)
						STATE = "NEDIUM";
					else
						STATE = "LOW";

					if ("HIGH".equals(STATE) || "MEDIUM".equals(STATE)) {
						gps = new GPSTracker(context);
						if (gps.canGetLocation()) {
							double lat = gps.getLatitude();
							double lon = gps.getLongitude();
							writeLocation(lat, lon, STATE);
						} else
							gps.showSettingsAlert();
					}

				}
				synchronized (getMonitor()) {
					MainActivity.play = true;
					getMonitor().notify();
					try {
						sleep(10);
					} catch (InterruptedException e) {
					}
				}
			}
		}

	}

	/**
	 * @return the monitor
	 */
	public static Object getMonitor() {
		return monitor;
	}

	/**
	 * @return the level
	 */
	public double getLevel() {
		return level;
	}

	/**
	 * @param level the level to set
	 */
	private void setLevel(double level) {
		this.level = level;
	}

}

/*
 * radiocounter принимает значения со счетчика, обрабатывает их (считает дозу,
 * если state - плохой, то печатает в файлик), делает notify в main ВСЕГДА РАЗ В
 * СЕКУНДУ.
 * main держит монитор в цикле в wait(). Если есть notify - берет level
 * и выводит в textview. Все. Еще пищит в цикле, если play == true
 */
