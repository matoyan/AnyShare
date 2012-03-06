/*
 * Copyright (C) 2011 The Stanford MobiSocial Laboratory
 *
 * This file is part of Musubi, a mobile social network.
 *
 *  This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package mobisocial.bento.anyshare.util;

import java.text.DateFormat;
import java.util.Date;

import org.json.JSONException;

import com.google.gson.Gson;

import mobisocial.bento.anyshare.R;
import mobisocial.bento.anyshare.io.DataManager;
import mobisocial.bento.anyshare.io.ItemObject;
import mobisocial.bento.anyshare.io.Postdata;
import mobisocial.bento.anyshare.ui.FeedItemListItem;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.DbUser;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQuery;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;

/**
 * Utility methods for managing the database.
 *
 */
public class DBHelper extends SQLiteOpenHelper {
	public static final String TAG = "DBHelper";
	public static final String DB_NAME = "ANYSHARE.db";
	public static final String DB_PATH = "/data/mobisocial.bento.anyshare/databases/";
	public static final int VERSION = 1;
    private long mNextId = -1;

	public DBHelper(Context context) {
		super(
		    context, 
		    DB_NAME, 
		    new SQLiteDatabase.CursorFactory() {
		    	@Override
		    	public Cursor newCursor(
                    SQLiteDatabase db,
                    SQLiteCursorDriver masterQuery,
                    String editTable,
                    SQLiteQuery query) {
		    		return new SQLiteCursor(db, masterQuery, editTable, query);
		    	}
		    }, 
		    VERSION);
	}
    public synchronized long getNextId() {
    	if(mNextId == -1) {
    		Cursor c = getReadableDatabase().query(ItemObject.TABLE, new String[] {"MAX(" + ItemObject._ID + ")"}, null, null, null, null, null);
    		try {
    			if(c.moveToFirst()) {
    				mNextId = c.getLong(0) + 1;
    			}
    		} finally {
    			c.close();
    		}
    	}
		return mNextId++;
    }
	private int mRefs = 1;
	public synchronized void addRef() {
		++mRefs;
	}
	@Override
	public synchronized void close() {
		if(--mRefs == 0) {
			super.close();
		}
	}

	
	@Override
	public void onOpen(SQLiteDatabase db) {
        // enable locking so we can safely share 
        // this instance around
        db.setLockingEnabled(true);
        Log.w(TAG, "dbhelper onopen");
    }

	/**
	 * Called whenever a database handle is requested on an outdated database.
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	    // new UpgradeTask(db, oldVersion, newVersion).execute(); // must block UI for this to work
	    new UpgradeTask(db, oldVersion, newVersion).doInBackground(); // doInForeground
	}

	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	    // Comment out the call to super to allow a downgrade.
	    super.onDowngrade(db, oldVersion, newVersion);
	}

	/**
	 * Updates the database in a background thread. The upgrade is wrapped
	 * in a transaction.
	 */
	class UpgradeTask extends AsyncTask<Void, Void, Void> {
	    private final SQLiteDatabase mDatabase;
	    private final int mOlderVersion;
	    private final int mNewVersion;

	    public UpgradeTask(SQLiteDatabase db, int oldVersion, int newVersion) {
            mDatabase = db;
            mOlderVersion = oldVersion;
            mNewVersion = newVersion;
        }

	    @Override
	    protected void onPreExecute() {
	        // TODO: block UI with dialog
	    }

	    @Override
	    protected void onPostExecute(Void result) {
	        // TODO: dismiss dialog
	    }

	    @Override
	    protected Void doInBackground(Void... params) {
	        mDatabase.beginTransaction();
	        try {
                doUpgrade(mDatabase, mOlderVersion, mNewVersion);
                mDatabase.setTransactionSuccessful();
	        } finally {
	            mDatabase.endTransaction();
	        }
	        return null;
	    }
	}

	private void doUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        db.setVersion(VERSION);
    }

    private void dropAll(SQLiteDatabase db){
        db.execSQL("DROP TABLE IF EXISTS " + ItemObject.TABLE);
    }
    public void truncateAll(){
    	getWritableDatabase().execSQL("DROP TABLE IF EXISTS " + ItemObject.TABLE);
    	onCreate(getWritableDatabase());
    }

    private void createTable(SQLiteDatabase db, String tableName, String[] uniqueCols, String... cols){
        assert cols.length % 2 == 0;
        String s = "CREATE TABLE " + tableName + " (";
        for(int i = 0; i < cols.length; i += 2){
            s += cols[i] + " " + cols[i + 1];
            if(i < (cols.length - 2)){
                s += ", ";
            }
            else{
                s += " ";
            }
        }
        if(uniqueCols != null && uniqueCols.length > 0){
            s+= ", UNIQUE (" + join(uniqueCols, ",") + ")";
        }
        s += ")";
        Log.i(TAG, s);
        db.execSQL(s);
    }

    private void createIndex(SQLiteDatabase db, String type, String name, String tableName, String col){
        String s = "CREATE " + type + " " + name + " on " + tableName + " (" + col + ")";
        Log.i(TAG, s);
        db.execSQL(s);
    }

	@Override
	public void onCreate(SQLiteDatabase db) {
		    db.beginTransaction();

            createTable(db, ItemObject.TABLE, null,
                        ItemObject._ID, "INTEGER PRIMARY KEY",
                        ItemObject.FEEDNAME, "TEXT",
                        ItemObject.TITLE, "TEXT",
                        ItemObject.DESC, "TEXT",
                        ItemObject.TIMESTAMP, "INTEGER",
                        ItemObject.RAW, "BLOB",
                        ItemObject.OBJHASH, "INTEGER",
                        ItemObject.PARENT_ID, "INTEGER"
                        );
            createIndex(db, "INDEX", "objects_by_hash", ItemObject.TABLE, ItemObject.OBJHASH);
            createIndex(db, "INDEX", "objects_timestamp", ItemObject.TABLE, ItemObject.TIMESTAMP);
            db.execSQL("CREATE INDEX objects_by_parent_id ON " + ItemObject.TABLE + "(" + ItemObject.PARENT_ID + ", " + ItemObject.TIMESTAMP + ")");

            db.setVersion(VERSION);
            db.setTransactionSuccessful();
            db.endTransaction();
            this.onOpen(db);
        //}
	}
	
	public long storeAppobjInDatabase(DbObj obj, Context context){
		long localId = -1; // if replace entry, set ID
		Gson gs = new Gson();

		Postdata postdata = gs.fromJson(obj.getJson().toString(), Postdata.class);
		long hash = obj.getHash();
		byte[] raw = null;
		if(postdata.datatype.equals(Postdata.TYPE_STREAM)){
			int iconid = DataManager.getIconid(postdata.localUri, postdata.mimetype);
			Bitmap icon = BitmapFactory.decodeResource(context.getResources(), iconid);
			raw = BitmapHelper.bitmapToBytes(icon);
		}else if(postdata.datatype.equals(Postdata.TYPE_TEXT_WITH_LINK)){
			Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_html);
			raw = BitmapHelper.bitmapToBytes(icon);
		}else{
			raw = obj.getRaw();
		}
		
		String feedname = obj.getFeedName();
		long parentid = 0;
		try {
			if(obj.getJson().has("target_relation") && obj.getJson().getString("target_relation").equals("parent")
					&& obj.getJson().has("target_hash")){
				parentid = objIdForHash(obj.getJson().getLong("target_hash"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		DbUser usr = obj.getSender();
		String sender = "";
		if(usr!=null){
			sender = usr.getName();
		}
		
		return addObjectByPostdata(localId, postdata, hash, raw, parentid, feedname, sender);
	}

    private long addObjectByPostdata(long localId, Postdata postdata, long hash, byte[] raw, long parentid, String feedname, String sender) {
        long objId = (localId == -1) ? getNextId() : localId;
        String sizestr = "";
        if(postdata.datatype.equals(Postdata.TYPE_STREAM)){
	        if(postdata.filesize<972){
	        	sizestr = "("+postdata.filesize+"Byte)";
	        }else if(postdata.filesize<996147){
	        	sizestr = "("+Math.floor(postdata.filesize/1024*10)/10+"KB)";
	        }else if(postdata.filesize>0){
	        	sizestr = "("+Math.floor(postdata.filesize/1024/1024*10)/10+"MB)";
	        }
        }
        String desc = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(Long.parseLong(postdata.timestamp)))
        				+ " " + sizestr;
        if(!sender.isEmpty()){
        	desc += " by "+sender;
        }
        String title = postdata.title;
        
        ContentValues cv = new ContentValues();
        cv.put(ItemObject._ID, objId);
        cv.put(ItemObject.FEEDNAME, feedname);
        cv.put(ItemObject.TITLE, title);
        cv.put(ItemObject.DESC, desc);
        cv.put(ItemObject.TIMESTAMP, postdata.timestamp);
        cv.put(ItemObject.OBJHASH, hash);
        cv.put(ItemObject.PARENT_ID, parentid);
 
        if (raw != null) {
        	cv.put(ItemObject.RAW, raw);
        }
        Log.e(TAG, cv.toString());

        long newObjId = getWritableDatabase().insertOrThrow(ItemObject.TABLE, null, cv);

        return objId;
    }

	
	public long storeLinkobjInDatabase(DbObj obj, Context context){
		long localId = -1; // if replace entry, set ID
		long hash = obj.getHash();
		Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_html);
		byte[] raw = BitmapHelper.bitmapToBytes(icon);
		
		String feedname = obj.getFeedName();
		long parentid = 0;
		try {
			if(obj.getJson().has("target_relation") && obj.getJson().getString("target_relation").equals("parent")
					&& obj.getJson().has("target_hash")){
				parentid = objIdForHash(obj.getJson().getLong("target_hash"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		DbUser usr = obj.getSender();
		String sender = "";
		if(usr!=null){
			sender = usr.getName();
		}
		
		long timestamp = 0;
		String title = "";
		try {
			timestamp = Long.parseLong(obj.getJson().getString("timestamp"));
			title = obj.getJson().getString("title");
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// add object
        long objId = (localId == -1) ? getNextId() : localId;
        String desc = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(timestamp));
        if(!sender.isEmpty()){
    		desc += " by "+sender;
        }
    
		ContentValues cv = new ContentValues();
		cv.put(ItemObject._ID, objId);
		cv.put(ItemObject.FEEDNAME, feedname);
		cv.put(ItemObject.TITLE, title);
		cv.put(ItemObject.DESC, desc);
		cv.put(ItemObject.TIMESTAMP, timestamp);
		cv.put(ItemObject.OBJHASH, hash);
		cv.put(ItemObject.PARENT_ID, parentid);

		if (raw != null) {
			cv.put(ItemObject.RAW, raw);
		}

		long newObjId = getWritableDatabase().insertOrThrow(ItemObject.TABLE, null, cv);

		return objId;
	}

	
	public long storePicobjInDatabase(DbObj obj, Context context){
		long localId = -1; // if replace entry, set ID
		long hash = obj.getHash();
		byte[] raw = obj.getRaw();
		
		String feedname = obj.getFeedName();
		long parentid = 0;
		try {
			if(obj.getJson().has("target_relation") && obj.getJson().getString("target_relation").equals("parent")
					&& obj.getJson().has("target_hash")){
				parentid = objIdForHash(obj.getJson().getLong("target_hash"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		DbUser usr = obj.getSender();
		String sender = "";
		if(usr!=null){
			sender = usr.getName();
		}
		
		long timestamp = 0;
		String title = "Picture";
		try {
			timestamp = Long.parseLong(obj.getJson().getString("timestamp"));
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// add object
        long objId = (localId == -1) ? getNextId() : localId;
        String desc = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(timestamp));
        if(!sender.isEmpty()){
    		desc += " by "+sender;
        }
    
		ContentValues cv = new ContentValues();
		cv.put(ItemObject._ID, objId);
		cv.put(ItemObject.FEEDNAME, feedname);
		cv.put(ItemObject.TITLE, title);
		cv.put(ItemObject.DESC, desc);
		cv.put(ItemObject.TIMESTAMP, timestamp);
		cv.put(ItemObject.OBJHASH, hash);
		cv.put(ItemObject.PARENT_ID, parentid);

		if (raw != null) {
			cv.put(ItemObject.RAW, raw);
		}

		long newObjId = getWritableDatabase().insertOrThrow(ItemObject.TABLE, null, cv);

		return objId;
	}
	
	public long objIdForHash(long hash) {
        Cursor c = getReadableDatabase().query(
                ItemObject.TABLE,
                new String[]{ ItemObject._ID },
                ItemObject.OBJHASH + "= ?",
                new String[] { String.valueOf(hash) },
                null,
                null,
                null);
        try {
	        if (c.moveToFirst()) {
	            return c.getLong(0);
	        }
	        return -1;
        } finally {
        	c.close();
        }
    }

    public Cursor queryItemList(String[] projection, String selection,
            String[] selectionArgs, String sortOrder){

        final String OBJECTS = ItemObject.TABLE;

        String tables = OBJECTS;
        if (sortOrder == null) {
            sortOrder = ItemObject.TIMESTAMP + " DESC";
        }
        Cursor c = getReadableDatabase().query(tables, projection, selection, selectionArgs,
                null, null, sortOrder, null);
        return c;
    }

    public long getLatestTimestamp(String feedName){

    	long tstamp = 0;
    	String[] projection = {ItemObject.TIMESTAMP};
    	String selection = "feedname = ?";
    	String[] selectionArgs = {feedName};
        String tables = ItemObject.TABLE;
        String sortOrder = ItemObject.TIMESTAMP + " DESC";
        String limits = "0,1";
        
        Cursor c = getReadableDatabase().query(tables, projection, selection, selectionArgs,
                null, null, sortOrder, limits);
		if (c != null && c.moveToFirst()) {
			tstamp = c.getLong(0);
		}
		return tstamp;
    }
 
    public FeedItemListItem getItemForHash(long hash){

    	FeedItemListItem item = new FeedItemListItem();
    	
    	String[] projection = {ItemObject.TITLE, ItemObject.DESC};
    	String selection = ItemObject.OBJHASH+" = ?";
    	String[] selectionArgs = {String.valueOf(hash)};
        String tables = ItemObject.TABLE;
        String sortOrder = null;
        String limits = "0,1";
        
        Cursor c = getReadableDatabase().query(tables, projection, selection, selectionArgs,
                null, null, sortOrder, limits);
		if (c != null && c.moveToFirst()) {
			item.title = c.getString(0);
			item.desc  = c.getString(1);
			c.close();
			return item;
		}
		c.close();
		return null;
    }

    public byte[] getThumbForHash(long hash){

    	byte[] thumb = null;
    	
    	String[] projection = {ItemObject.RAW};
    	String selection = ItemObject.OBJHASH+" = ?";
    	String[] selectionArgs = {String.valueOf(hash)};
        String tables = ItemObject.TABLE;
        String sortOrder = null;
        String limits = "0,1";
        
        Cursor c = getReadableDatabase().query(tables, projection, selection, selectionArgs,
                null, null, sortOrder, limits);
		if (c != null && c.moveToFirst()) {
			thumb = c.getBlob(0);
			Log.e(TAG,"thumb found!"+thumb.length);
		}
		c.close();
		return thumb;
    }

	public String getDatabasePath() {
		return DB_PATH+DB_NAME;
	}
	public void vacuum() {
		getWritableDatabase().execSQL("VACUUM");
	}

	public void deleteObj(long id) {
		getWritableDatabase().delete(ItemObject.TABLE, ItemObject._ID + " = ?", new String[] {String.valueOf(id)});
    }

	// from Util.java
    public static String join(String[] s, String delimiter) {
        if (s.length == 0) return "";
        StringBuffer buffer = new StringBuffer(s[0]);
        for(int i = 1; i < s.length; i++){
            buffer.append(delimiter);
            buffer.append(s[i]);
        }
        return buffer.toString();
    }

 }
