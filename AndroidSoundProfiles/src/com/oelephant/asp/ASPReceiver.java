package com.oelephant.asp;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class ASPReceiver extends BroadcastReceiver {

	int NOTIFY_ID = 1;

	@Override
	public void onReceive(Context context, Intent intent) {
		
		if (intent.getAction() == "com.oelephant.asp.CANCEL") {
			
			Log.d("OE", "canceling alarm");
			cancelAlarm(context);
			cancelNotify(context);
			return;
			
		}
		
		if (intent.getAction() != "com.oelephant.asp.RECEIVE") {
			
			Log.d("OE", "recieve error");
			return;
		}
		
		long now = System.currentTimeMillis();
		
		ASPDB db = new ASPDB(context);
		
		ASPDB.QueueItem q = db.getCurrQueued(now);
		
		if (q != null) {
			
			Log.d("OE", "using queue item " + q.id);
			
			if (q.eId != 0) {
				
				ASPDB.Profile p = db.getProfile(q.pId);
				setProfile(context, p);
				setAlarm(context, q.endUTC);
				
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(q.endUTC);
				
				SimpleDateFormat sdf = new SimpleDateFormat("MM/dd hh:mma");
				String msg = String.format("e:%s %d %d %d %d - %s", p.name, p.volRing, p.volNotify, p.volMedia, p.volAlarm, sdf.format(cal.getTime()));
				notify(context, msg);
				
			} else if (q.sId != 0) {
				
				ASPDB.Profile p = db.getProfile(q.pId);
				
				setProfile(context, db.getProfile(q.pId));
				setAlarm(context, q.endUTC);
				
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(q.endUTC);
				
				SimpleDateFormat sdf = new SimpleDateFormat("MM/dd hh:mma");
				String msg = String.format("s:%s %d %d %d %d - %s", p.name, p.volRing, p.volNotify, p.volMedia, p.volAlarm, sdf.format(cal.getTime()));
				notify(context, msg);
				
			} else {
				
				// queue corrupt?
				
			}
			
		} else {
			
			Log.d("OE", "using default");
			
			setDefaultProfile(context, 7, 14, 7, 7, 1, 1);
			
			ASPDB.QueueItem nq = db.getNextQueued(now);
	
			if (nq != null) {
				
				setAlarm(context, nq.startUTC);
				
			} else {
				
				// no queue?
				
			}
				
			
		}
		
		db.createQueue(2);
		
	}

	public void notify(Context context, String msg) {
		NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
				context).setContentText(msg).setOngoing(true)
				.setContentTitle("ASP")
				.setSmallIcon(R.drawable.ic_launcher);

		notificationManager.notify(NOTIFY_ID, notificationBuilder.build());
	}

	public void cancelNotify(Context context) {
		NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(NOTIFY_ID);
	}
	
	public void setAlarm(Context context, long time) {
		
		Intent i = new Intent(context, ASPReceiver.class);
		i.setAction("com.oelephant.asp.RECEIVE");
		
		PendingIntent pi = PendingIntent.getBroadcast(context, 0,
				i, PendingIntent.FLAG_UPDATE_CURRENT);

		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, time, pi);
		
	}
	
	public void cancelAlarm(Context context) {
		
		Intent i = new Intent(context, ASPReceiver.class);
		
		PendingIntent pi = PendingIntent.getBroadcast(context, 0,
				i, PendingIntent.FLAG_CANCEL_CURRENT);

		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		am.cancel(pi);
		
	}
	
	public void setProfile(Context context, ASPDB.Profile profile) {
		
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		am.setStreamVolume(AudioManager.STREAM_RING, profile.volRing, 0);
		am.setStreamVolume(AudioManager.STREAM_MUSIC, profile.volMedia, 0);
		am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, profile.volNotify, 0);
		am.setStreamVolume(AudioManager.STREAM_ALARM, profile.volAlarm, 0);
		am.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, profile.vibRing);
		am.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, profile.vibNotify);
	}
	
	public void setDefaultProfile(Context context, int volRing, int volMedia, int volNotify, int volAlarm, int vibRing, int vibNotify) {
		
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		am.setStreamVolume(AudioManager.STREAM_RING, volRing, 0);
		am.setStreamVolume(AudioManager.STREAM_MUSIC, volMedia, 0);
		am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, volNotify, 0);
		am.setStreamVolume(AudioManager.STREAM_ALARM, volAlarm, 0);
		am.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, vibRing);
		am.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, vibNotify);
	}
}
