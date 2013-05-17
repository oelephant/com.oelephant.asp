package com.oelephant.asp;

import java.util.Calendar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		ASPDB db = new ASPDB(this);
		
		db.createProfile("Home 1", 7, 0, 0, 7, 1, 1);
		db.createProfile("Home 2", 7, 7, 0, 7, 1, 1);
		db.createProfile("Home 3", 7, 7, 15, 7, 1, 1);
		db.createProfile("Work 1", 7, 7, 15, 7, 1, 1);
		db.createProfile("Work 2", 7, 7, 15, 7, 1, 1);
		db.createProfile("Work 3", 7, 7, 15, 7, 1, 1);
		db.createProfile("Sleep 1", 0, 0, 0, 7, 1, 1);
		db.createProfile("Sleep 2", 7, 0, 15, 7, 1, 1);

		db.createSchedule("Home 1", 1, 1, 1, 1, 1, 1, 1, 1, toLong(8, 0, 0), toLong(2, 0, 0));
		db.createSchedule("Home 2", 2, 1, 1, 1, 1, 1, 1, 1, toLong(12, 0, 0), toLong(0, 30, 0));
		db.createSchedule("Home 3", 3, 1, 1, 1, 1, 1, 1, 1, toLong(21, 0, 0), toLong(0, 30, 0));
		db.createSchedule("Work 1", 4, 1, 1, 0, 0, 1, 1, 1, toLong(13, 0, 0), toLong(4, 0, 0));
		db.createSchedule("Work 2", 5, 1, 1, 0, 0, 1, 1, 1, toLong(18, 0, 0), toLong(2, 0, 0));
		db.createSchedule("Sleep 1", 7, 1, 1, 1, 1, 1, 1, 1, toLong(23, 0, 0), toLong(8, 0, 0));
		db.createSchedule("Sleep 2", 8, 1, 1, 1, 1, 1, 1, 1, toLong(7, 0, 0), toLong(0, 45, 0));
		
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 16);
		cal.set(Calendar.MINUTE, 30);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		
		db.createException("Exception 1", 6, cal.getTimeInMillis(), toLong(2, 0, 0));
		
		db.createQueue(2);
		
		String[] queue = db.queueToString();
		for (int i = 0; i < queue.length; i++) {
			
			Log.d("OE", queue[i]);
			
		}
		
		startBroadcast();

		((Button) findViewById(R.id.btn_start)).setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				startBroadcast();
			}
		});

		
		((Button) findViewById(R.id.btn_stop)).setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				stopBroadcast();
			}
		});
		
	}
	
	public void startBroadcast() {
		Intent i = new Intent(this, ASPReceiver.class);
		i.setAction("com.oelephant.asp.RECEIVE");
		this.sendBroadcast(i);
	}

	public void stopBroadcast() {
		Intent i = new Intent(this, ASPReceiver.class);
		i.setAction("com.oelephant.asp.CANCEL");
		this.sendBroadcast(i);
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public long toLong(long hour, long minute, long second) {
		return (hour * 60 * 60 * 1000) + (minute * 60 * 1000) + (second * 1000);
	}
	
	public long toUTC(int M, int d, int y, int h, int m) {
		Calendar c = Calendar.getInstance();
		c.set(y, M - 1, d, h, m, 0);
		return c.getTimeInMillis();
	}
}
