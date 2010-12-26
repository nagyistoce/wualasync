package com.laksrecordings.wualasync;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.util.ArrayList;

public class DataHelper {

   private static final String DATABASE_NAME = "WualaSync.db";
   private static final int DATABASE_VERSION = 1;
   private static final String TABLE_NAME = "syncedFiles";
   private static final String TABLE_NAME_ALL = "wualaFiles";

   private Context context;
   private SQLiteDatabase db;

   private SQLiteStatement insertStmt;
   private static final String INSERT = "insert into " + TABLE_NAME + " (name) values (?)";
   private SQLiteStatement insertWualaStmt;
   private static final String INSERT_WUALA = "insert into " + TABLE_NAME_ALL + " (name) values (?)";
   private SQLiteStatement deleteStmt;
   private static final String DELETE = "delete from " + TABLE_NAME + " WHERE name = ?";

   public DataHelper(Context context) {
      this.context = context;
      OpenHelper openHelper = new OpenHelper(this.context);
      this.db = openHelper.getWritableDatabase();
      this.insertStmt = this.db.compileStatement(INSERT);
      this.insertWualaStmt = this.db.compileStatement(INSERT_WUALA);
      this.deleteStmt = this.db.compileStatement(DELETE);
   }
   
   protected void finalize() throws Throwable {
	   close();
   }
   
   public void close() {
	   try {
		   insertStmt.close();
		   insertWualaStmt.close();
		   deleteStmt.close();
		   db.close();
	   } catch (Exception e) {}
   }

   public long insert(String name) {
      this.insertStmt.bindString(1, name);
      return this.insertStmt.executeInsert();
   }

   public long insertWuala(String name) {
	   this.insertWualaStmt.bindString(1, name);
	   return this.insertWualaStmt.executeInsert();
   }
   
   public long delete(String name) {
	   this.deleteStmt.bindString(1, name);
	   return this.deleteStmt.executeInsert();
   }

   public void deleteAll() {
      this.db.delete(TABLE_NAME, null, null);
   }

   public void deleteWualaAll() {
	   this.db.delete(TABLE_NAME_ALL, null, null);
   }
   
   public ArrayList<String> selectAll() {
      ArrayList<String> list = new ArrayList<String>();
      Cursor cursor = this.db.query(TABLE_NAME, new String[] { "name" }, 
        null, null, null, null, null);
      if (cursor.moveToFirst()) {
         do {
            list.add(cursor.getString(0));
         } while (cursor.moveToNext());
      }
      if (cursor != null && !cursor.isClosed()) {
         cursor.close();
      }
      return list;
   }
   
   public boolean existsInWuala(String filename) {
	   boolean result = false;
	   Cursor cursor = this.db.query(TABLE_NAME_ALL, new String[] { "name" }, 
	    	        "name = ?", new String[] { filename }, null, null, null);
	   result = cursor.getCount() > 0;
	   if (cursor != null && !cursor.isClosed()) {
		   cursor.close();
	   }
	   return result;
   }

   private static class OpenHelper extends SQLiteOpenHelper {

      OpenHelper(Context context) {
         super(context, DATABASE_NAME, null, DATABASE_VERSION);
      }

      @Override
      public void onCreate(SQLiteDatabase db) {
         db.execSQL("CREATE TABLE " + TABLE_NAME + "(name TEXT not null primary key)");
         db.execSQL("CREATE TABLE " + TABLE_NAME_ALL + "(name TEXT not null primary key)");
      }

      @Override
      public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
         Log.w(getClass().getSimpleName(), "Upgrading database, this will drop tables and recreate.");
         db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
         db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_ALL);
         onCreate(db);
      }
   }
}
