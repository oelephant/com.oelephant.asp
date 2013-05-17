package com.oelephant.asp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ASPDB extends SQLiteOpenHelper {
	public static final String DB_NAME = "asp_db";
	public static final int DB_VERSION = 1;

	public static final long UTC_THRESHOLD = 1000;

	public ASPDB(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE profiles ("
				+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "name TEXT NOT NULL, " + "vol_ring INTEGER NOT NULL, "
				+ "vol_notify INTEGER NOT NULL, "
				+ "vol_media INTEGER NOT NULL, "
				+ "vol_alarm INTEGER NOT NULL, "
				+ "vib_ring INTEGER NOT NULL, "
				+ "vib_notify INTEGER NOT NULL);");
		db.execSQL("CREATE TABLE schedules ("
				+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "name TEXT NOT NULL, " + "p_id INTEGER NOT NULL, "
				+ "sun INTEGER NOT NULL, " + "mon INTEGER NOT NULL, "
				+ "tue INTEGER NOT NULL, " + "wed INTEGER NOT NULL, "
				+ "thu INTEGER NOT NULL, " + "fri INTEGER NOT NULL, "
				+ "sat INTEGER NOT NULL, " + "start INTEGER NOT NULL, "
				+ "length INTEGER NOT NULL);");
		db.execSQL("CREATE TABLE exceptions ("
				+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "name TEXT NOT NULL, " + "p_id INTEGER NOT NULL, "
				+ "start_utc INTEGER NOT NULL, " + "length INTEGER NOT NULL, "
				+ "end_utc INTEGER NOT NULL);");
		db.execSQL("CREATE TABLE queue ("
				+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, " + "s_id INTEGER, "
				+ "e_id INTEGER, " + "p_id INTEGER NOT NULL, "
				+ "start_utc INTEGER NOT NULL, " + "length INTEGER NOT NULL, "
				+ "end_utc INTEGER NOT NULL);");
	}
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS profiles;");
		db.execSQL("DROP TABLE IF EXISTS schedules;");
		db.execSQL("DROP TABLE IF EXISTS exceptions;");
		db.execSQL("DROP TABLE IF EXISTS queue;");
		onCreate(db);
	}
	
	public QueueItem getCurrQueued(long utc) {
		
		SQLiteDatabase db = this.getReadableDatabase();
		
		Cursor c = db.rawQuery("SELECT * FROM queue WHERE start_utc <= " + utc + " AND end_utc > " + utc + " ORDER BY start_utc ASC LIMIT 1;", null);
		
		if (c.getCount() > 0) {
			
			c.moveToFirst();
			
			QueueItem q = new QueueItem(c);
			
			c.close();
			return q;
			
		}
		
		return null;
		
	}
	
	public QueueItem getNextQueued(long utc) {
		
		SQLiteDatabase db = this.getReadableDatabase();
		
		Cursor c = db.rawQuery("SELECT * FROM queue WHERE start_utc >= " + utc + " ORDER BY start_utc ASC LIMIT 1;", null);
		
		if (c.getCount() > 0) {
			
			c.moveToFirst();
			
			QueueItem q = new QueueItem(c);
			
			c.close();
			return q;
			
		}
		
		return null;
		
	}

	public void createProfile(String name, int volRing, int volNotify, int volMedia, int volAlarm, int vibRing, int vibNotify) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		db.execSQL("INSERT INTO profiles " +
				"(_id, name, vol_ring, vol_notify, vol_media, vol_alarm, vib_ring, vib_notify)" +
				" VALUES " +
				"(null, \"" + name +"\", " + volRing + ", " + volNotify + ", " + volMedia + ", " + volAlarm + ", " + vibRing + ", " + vibNotify + ");");
	
		db.close();
	}
	
	public long createSchedule(String name, long pId, int sun, int mon, int tue, int wed, int thu, int fri, int sat, long start, long length) {
		
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor c = db.rawQuery("SELECT * FROM schedules;", null);
		
		List<Schedule> schedules = new ArrayList<Schedule>();
		
		if (c.getCount() > 0) {
			
			c.moveToFirst();
			
			do {
				
				schedules.add(new Schedule(c));
				
			} while (c.moveToNext());
		
			c.close();
			
		}
		
		ArrayList<ScheduleTest> newSchedule = new ArrayList<ScheduleTest>();
		newSchedule = createScheduleTestList(
				new int[] {sun, mon, tue, wed, thu, fri, sat}, start, length);
		
		ArrayList<ScheduleTest> existingSchedule = new ArrayList<ScheduleTest>();
		
		for (Schedule schedule : schedules) {
			
			int[] days = new int[] {
					schedule.sun,
					schedule.mon,
					schedule.tue,
					schedule.wed,
					schedule.thu,
					schedule.fri,
					schedule.sat
			};
			
			existingSchedule = createScheduleTestList(days, schedule.start, schedule.length);
			
			boolean isOverlap = checkScheduleOverlap(newSchedule, existingSchedule);
			
			if (isOverlap) {
				return schedule.id;
			}
		}
		
		db.execSQL("INSERT INTO schedules " +
				"(_id, name, p_id, sun, mon, tue, wed, thu, fri, sat, start, length)" +
				" VALUES " +
				"(null, \"" + name +"\", " + pId + ", " + sun + ", " + mon + ", " + tue + ", " + wed + ", " + thu + ", " + fri + ", " + sat + ", " + start + ", " + length + ");"); 
	
		db.close();
		return -1;
	}
	
	public long createException(String name, long pId, long startUTC, long length) {
		
		long endUTC = startUTC + length;
		
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor c = db.rawQuery("SELECT * FROM exceptions;", null);
		
		List<Exception> exceptions = new ArrayList<Exception>();
		
		if (c.getCount() > 0) {
			
			c.moveToFirst();
			
			do {
				
				exceptions.add(new Exception(c));
				
			} while (c.moveToNext());
		
			c.close();
			
		}
		
		for (Exception exception : exceptions) {
			
			if (startUTC >= exception.startUTC && startUTC < exception.endUTC) {
				
				return exception.id;
				
			}
			
			if (endUTC > exception.startUTC && endUTC <= exception.endUTC) {
				
				return exception.id;
				
			}
			
		}
		
		db.execSQL("INSERT INTO exceptions " +
				"(_id, name, p_id, start_utc, length, end_utc)" +
				" VALUES " +
				"(null, \"" + name +"\", " + pId + ", " + startUTC + ", " + length + ", " + endUTC + ");");
	
		db.close();
		return -1;
	}
	
	public Profile getProfile(long id) {
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor c = db.rawQuery("SELECT * FROM profiles WHERE _id = " + id + ";", null);
		
		if (c.getCount() > 0) {
			
			c.moveToFirst();
			
			Profile p = new Profile(c);
			
			c.close();
			db.close();
			
			return p;
		}
		
		c.close();
		db.close();
		
		return null;
	}
	
	public Schedule getSchedule(long id) {
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor c = db.rawQuery("SELECT * FROM schedules WHERE _id = " + id + ";", null);
		
		if (c.getCount() > 0) {
			
			c.moveToFirst();
			
			Schedule s = new Schedule(c);
			
			c.close();
			db.close();
			
			return s;
			
		}
		
		c.close();
		db.close();
		
		return null;
	}
	
	public Exception getException(long id) {
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor c = db.rawQuery("SELECT * FROM exception WHERE _id = " + id + ";", null);

		if (c.getCount() > 0) {
			
			c.moveToFirst();
			
			Exception e = new Exception(c);
		
			c.close();
			db.close();
			
			return e;
			
		}
		
		c.close();
		db.close();
		
		return null;
	}
	
	public void deleteProfile(long id) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		db.execSQL("DELETE FROM profiles WHERE _id = " + id + ";");
		db.execSQL("DELETE FROM schedules WHERE p_id = " + id + ";");
		db.execSQL("DELETE FROM exceptions WHERE p_id = " + id + ";");
		
		db.close();
	}
	
	public void deleteSchedule(long id) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		db.execSQL("DELETE FROM schedules WHERE p_id = " + id + ";");
		
		db.close();
	}
	
	public void deleteException(long id) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		db.execSQL("DELETE FROM exceptions WHERE p_id = " + id + ";");
		
		db.close();
	}

	public void editProfile(long id, String name, int volRing, int volNotify, int volMedia, int volAlarm, int vibRing, int vibNotify) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		db.execSQL("UPDATE profile SET " +
				"name = \"" + name +"\", " +
				"vol_ring = " + volRing + ", " +
				"vol_notify = " + volNotify + ", " +
				"vol_media = " + volMedia + ", " +
				"vol_alarm = " + volAlarm + ", " +
				"vib_ring = " + vibRing + ", " +
				"vib_notify = " + vibNotify + " " + 
				"WHERE _id = " + id + ";");
	
		db.close();
	}
	
	public void editSchedule(long id, String name, long pId, int sun, int mon, int tue, int wed, int thu, int fri, int sat, long start, long length) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		db.execSQL("UPDATE schedules SET " +
				"name = \"" + name +"\", " +
				"p_id = " + pId + ", " +
				"sun = " + sun + ", " +
				"mon = " + mon + ", " +
				"tue = " + tue + ", " +
				"wed = " + wed + ", " +
				"thu = " + thu + ", " +
				"fri = " + fri + ", " +
				"sat = " + sat + ", " +
				"start = " + start + ", " +
				"length = " + length + ", " +
				"WHERE _id = " + id + ";");
	
		db.close();
	}
	
	public void editException(long id, String name, long pId, long startUTC, long length, long endUTC) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		db.execSQL("UPDATE schedules SET " +
				"name = \"" + name +"\", " +
				"p_id = " + pId + ", " +
				"startUTC = " + startUTC + ", " +
				"length = " + length + ", " +
				"endUTC = " + endUTC + " " +
				"WHERE _id = " + id + ";");
	
		db.close();
	}
	
	public void createQueue(int depthInWeeks) {
		SQLiteDatabase db;
		Cursor c1, c2;
		
		db = this.getWritableDatabase();
		db.execSQL("DELETE FROM queue;");
		db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = 'queue'");

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		long startUTC, length, endUTC, sId, eId, pId;
		String[] days = new String[] {"sun", "mon", "tue", "wed", "thu", "fri", "sat"};
		
		for (int i = 0; i < depthInWeeks; i++) {
			
			for (String day : days) {
				
				c1 = db.rawQuery("SELECT * FROM schedules WHERE " + day + " = 1 ORDER BY start ASC;", null);
				
				if (c1.getCount() > 0) {
					
					c1.moveToFirst();
					
					do {

						cal.set(Calendar.HOUR_OF_DAY, 0);
						cal.set(Calendar.MINUTE, 0);
						cal.set(Calendar.SECOND, 0);
						cal.set(Calendar.MILLISECOND, 0);
						cal.add(Calendar.MILLISECOND, c1.getInt(c1.getColumnIndex("start")));
					
						sId = c1.getLong(c1.getColumnIndex("_id"));
						pId = c1.getLong(c1.getColumnIndex("p_id"));
						startUTC = cal.getTimeInMillis();
						length = c1.getLong(c1.getColumnIndex("length"));
						endUTC = startUTC + length;
						
						db.execSQL("INSERT INTO queue (_id, s_id, e_id, p_id, start_utc, length, end_utc) VALUES ("
								+ "null, " + sId + ", "	+ "null, " + pId + ", "	+ startUTC + ", " + length + ", " + endUTC + ");");
					
					} while (c1.moveToNext());
				
				}

				cal.add(Calendar.DAY_OF_YEAR, 1);
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);
			}
		}
		
		c1 = db.rawQuery("SELECT * FROM exceptions;", null);
		
		if (c1.getCount() > 0) {
			
			c1.moveToFirst();
			
			do {
				
				startUTC = c1.getLong(c1.getColumnIndex("start_utc"));
				endUTC = c1.getLong(c1.getColumnIndex("end_utc"));
				
				c2 = db.rawQuery("SELECT * FROM queue WHERE "
						+ "start_utc >= " + startUTC + " AND start_utc < " + endUTC + ";", null);
				
				if (c2.getCount() > 0) {
					
					c2.moveToFirst();
					
					do {
						
						long id = c2.getLong(c2.getColumnIndex("_id"));
						long newStartUTC = endUTC;
						long oldEndUTC = c2.getLong(c2.getColumnIndex("end_utc"));
						long newLength = oldEndUTC - newStartUTC;
						
						db.execSQL("UPDATE queue SET "
								+ "start_utc = " + newStartUTC + ", length = " + newLength
								+ " WHERE _id = " + id + ";");
						
					} while (c2.moveToNext());
					
				}
				
				c2 = db.rawQuery("SELECT * FROM queue WHERE "
						+ "end_utc >= " + startUTC + " AND end_utc <= " + endUTC + ";", null);
				
				if (c2.getCount() > 0) {
					
					c2.moveToFirst();
					
					do {
						
						long id = c2.getLong(c2.getColumnIndex("_id"));
						long newEndUTC = startUTC;
						long oldStartUTC = c2.getLong(c2.getColumnIndex("start_utc"));
						long newLength = newEndUTC - oldStartUTC;
						
						db.execSQL("UPDATE queue SET "
								+ "end_utc = " + newEndUTC + ", length = " + newLength
								+ " WHERE _id = " + id + ";");
						
					} while (c2.moveToNext());
				
				}
				
				eId = c1.getLong(c1.getColumnIndex("_id"));
				pId = c1.getLong(c1.getColumnIndex("p_id"));
				length = endUTC - startUTC;
				
				db.execSQL("INSERT INTO queue (_id, s_id, e_id, p_id, start_utc, length, end_utc) VALUES ("
						+ "null, null, " + eId + ", " + pId + ", "	+ startUTC + ", " + length + ", " + endUTC + ");");
				
			} while (c1.moveToNext());
			
			c2.close();
		}
		
		c1.close();
		db.close();
	}
	
	public String[] queueToString() {
		
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yy HH:mm:ss");
		
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.rawQuery("SELECT * FROM queue;", null);
		
		if (c.getCount() > 0) {

			String[] ret = new String[c.getCount()];
			int i = 0;
			
			c.moveToFirst();
			
			do {
				
				cal.setTimeInMillis(c.getLong(c.getColumnIndex("start_utc")));
				String start =  sdf.format(cal.getTime());
				cal.setTimeInMillis(c.getLong(c.getColumnIndex("end_utc")));
				String end = sdf.format(cal.getTime());
				
				ret[i] = c.getString(c.getColumnIndex("_id")) + " : " + 
						c.getString(c.getColumnIndex("s_id")) + " : " + 
						c.getString(c.getColumnIndex("e_id")) + " : " + 
						c.getString(c.getColumnIndex("p_id")) + " : " +
						start + " : " + end;
				
				i++;
			
			} while (c.moveToNext());
			
			return ret;
		}
		
		return null;
	}

	public class Profile {
		long id;
		String name;
		int volRing, volNotify, volMedia, volAlarm, vibRing, vibNotify;
		
		public Profile(Cursor c) {
			id = c.getLong(c.getColumnIndex("_id"));
			name = c.getString(c.getColumnIndex("name"));
			volRing = c.getInt(c.getColumnIndex("vol_ring"));
			volNotify = c.getInt(c.getColumnIndex("vol_notify"));
			volMedia = c.getInt(c.getColumnIndex("vol_media"));
			volAlarm = c.getInt(c.getColumnIndex("vol_alarm"));
			vibRing = c.getInt(c.getColumnIndex("vib_ring"));
			vibNotify = c.getInt(c.getColumnIndex("vib_notify"));
		}
	}

	public class Schedule {
		long id, pId, start, length;
		String name;
		int sun, mon, tue, wed, thu, fri, sat;
		
		public Schedule(Cursor c) {
			id = c.getLong(c.getColumnIndex("_id"));
			name = c.getString(c.getColumnIndex("name"));
			sun = c.getInt(c.getColumnIndex("sun"));
			mon = c.getInt(c.getColumnIndex("mon"));
			tue = c.getInt(c.getColumnIndex("tue"));
			wed = c.getInt(c.getColumnIndex("wed"));
			thu = c.getInt(c.getColumnIndex("thu"));
			fri = c.getInt(c.getColumnIndex("fri"));
			sat = c.getInt(c.getColumnIndex("sat"));
			start = c.getLong(c.getColumnIndex("start"));
			length = c.getLong(c.getColumnIndex("length"));
		}
		
	}
	
	public class ScheduleTest {
		long startUTC, endUTC;
		
		public ScheduleTest(long startUTC, long endUTC) {
			this.startUTC = startUTC;
			this.endUTC = endUTC;
		}
	}
	
	public ArrayList<ScheduleTest> createScheduleTestList(int[] days, long start, long length) {
			
		ArrayList<ScheduleTest> list = new ArrayList<ScheduleTest>();
		
		for (int day = 0; day < 7; day++) {
			
			if (days[day] == 1) {

				long startUTC = 24 * 60 * 60 * 1000 * day + start;
				long endUTC = startUTC + length;
				list.add(new ScheduleTest(startUTC, endUTC));
				
			}
			
		}
		
		return list;
			
	}
	
	public boolean checkScheduleOverlap(ArrayList<ScheduleTest> newSchedule, ArrayList<ScheduleTest> existingSchedule) {
		
		for (ScheduleTest newTest : newSchedule) {
			
			for (ScheduleTest existingTest : existingSchedule) {
				
				if (newTest.startUTC >= existingTest.startUTC && newTest.startUTC < existingTest.endUTC) {
					
					return true;
					
				}
				
				if (newTest.endUTC > existingTest.startUTC && newTest.endUTC <= existingTest.endUTC) {
					
					return true;
					
				}
				
			}
			
		}
		
		return false;
	}
	
	public class QueueItem {
		Long id, sId, eId, pId, startUTC, length, endUTC;
		
		public QueueItem(Cursor c) {
			
			id = c.getLong(c.getColumnIndex("_id"));
			sId = c.getLong(c.getColumnIndex("s_id"));
			eId = c.getLong(c.getColumnIndex("e_id"));
			pId = c.getLong(c.getColumnIndex("p_id"));
			startUTC = c.getLong(c.getColumnIndex("start_utc"));
			length = c.getLong(c.getColumnIndex("length"));
			endUTC = c.getLong(c.getColumnIndex("end_utc"));
			
		}
	}

	public class Exception {
		long id, pId, startUTC, length, endUTC;
		String name;
		
		public Exception(Cursor c) {
			id = c.getLong(c.getColumnIndex("_id"));
			name = c.getString(c.getColumnIndex("name"));
			startUTC = c.getLong(c.getColumnIndex("start_utc"));
			length = c.getLong(c.getColumnIndex("length"));
			endUTC = c.getLong(c.getColumnIndex("end_utc"));
		}
	}
}
