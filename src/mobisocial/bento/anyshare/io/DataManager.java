package mobisocial.bento.anyshare.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;

import mobisocial.bento.anyshare.R;
import mobisocial.bento.anyshare.ui.FeedItemListItem;
import mobisocial.bento.anyshare.util.BitmapHelper;
import mobisocial.bento.anyshare.util.DBHelper;
import mobisocial.bento.anyshare.util.UpnpController;
import mobisocial.socialkit.musubi.DbFeed;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.Musubi;
import mobisocial.socialkit.musubi.multiplayer.FeedRenderable;
import mobisocial.socialkit.obj.MemObj;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore.Images;
import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;


public class DataManager {
	private static final String TAG = "DataManager";
	public static final String NAME_APP = "mobisocial.bento.anyshare";

	public static final String TYPE_APP = "app";
	public static final String TYPE_APP_STATE = "appstate";
	public static final String TYPE_PICTURE   = "picture";
    public static final String TYPE_LINK = "send_file";
    public static final String TYPE_UNKNOWN = "UNKNOWN";
    public static final String TYPE_VIDEO = "video";
    public static final String TYPE_MUSIC = "music";

	public static final String OBJ_MIME_TYPE = "mimeType";
    public static final String OBJ_LOCAL_URI = "localUri";
    public static final String OBJ_HASHCODE = "hashCode";
    public static final String OBJ_FILESIZE = "fileSize";
	public static final String STATE = "state";
	public static final String B64JPGTHUMB = "b64jpgthumb";

    public static final long MY_ID = -666;

    private static DataManager sInstance = null;
	private static JSONObject sState = null;
	private Musubi mMusubi = null;

	private UpnpController upnp = null;
	private int upnpPort = 8224;
	private String wanip = null;
	private String wanport = null;
	private String lanip = null;
	
	private DbObj mContextObj = null;
	private ArrayList<Long> itemsHash;

	// WORKAROUND
	private long lastInsertHash = 0;

	
	// ----------------------------------------------------------
	// Instance
	// ----------------------------------------------------------
	private DataManager() {
		// nothing to do
	}

	public static DataManager getInstance() {
		if (sInstance == null) {
			sInstance = new DataManager();
		}
		return sInstance;
	}

	public void init(Musubi musubi, Context context) {
		mMusubi = musubi;
		sState = null;
        wanip = null;
        wanport = null;
        lanip = null;
        itemsHash = new ArrayList<Long>();
        if(mMusubi!=null){
        	updateItemDbFromFeed(context);
        	readItemsForlist(context);
        }
	}
	
	public void updateFromObj(DbObj obj){
		if (obj != null && obj.getJson() != null) {
			if (obj.getJson().has(STATE)){
				sState = obj.getJson().optJSONObject(STATE);
			}
		}
	}
	public void updateFromApp(DbObj app){
		if(app != null){
			updateFromObj(app.getSubfeed().getLatestObj());
		}
	}
	
	public void fin() {
//		mMusubi = null;
		sState = null;
		sInstance = null;
        wanip = null;
        wanport = null;
        lanip = null;
	}

	// ----------------------------------------------------------
	// Get / Retrieve
	// ----------------------------------------------------------
	public Musubi getMusubi() {
		return mMusubi;
	}
	synchronized public boolean isOwner(DbObj obj){
		if(obj.getSender().getLocalId() == MY_ID){
			return true;
		}
    	return false;
    }
	public DbObj getContextObj(){
		return mContextObj;
	}
	public void setContextObj(DbObj obj){
		mContextObj = obj;
	}

	// ----------------------------------------------------------
	// Musubi
	// ----------------------------------------------------------
	
	public void updateItemDbFromFeed(Context context){
		if(mMusubi == null){
			return;
		}
		String feedname = mMusubi.getFeed().getLatestObj().getFeedName();
		DBHelper dbh = new DBHelper(context);
		long time = dbh.getLatestTimestamp(feedname);

        String[] projection = null;
        String selection = "(type = ? OR type = ? OR (type = ? AND app_id = ?)) AND feed_name = ? AND timestamp > ?";
        String[] selectionArgs = new String[] { TYPE_PICTURE, TYPE_LINK, TYPE_APP_STATE, NAME_APP, feedname, String.valueOf(time) };
        String sortOrder = null;
		Cursor c = mMusubi.getAppFeed().query(projection, selection, selectionArgs, sortOrder);
		if (c != null && c.moveToFirst()) {
			for (int i = 0; i < c.getCount(); i++) {
				DbObj dbObj = mMusubi.objForCursor(c);
				if(dbObj.getType().equals(TYPE_APP_STATE)){
					Log.d(TAG, "newhash:"+dbObj.getHash());
					dbh.storeAppobjInDatabase(dbObj,context);
				}else if(dbObj.getType().equals(TYPE_PICTURE)){
					dbh.storePicobjInDatabase(dbObj,context);
				}else if(dbObj.getType().equals(TYPE_LINK)){
					dbh.storeLinkobjInDatabase(dbObj,context);
				}
				c.moveToNext();
			}
		}
		c.close();
		dbh.close();
	}
	
	public void insertItemDbFromFeed(DbObj dbObj, Context context){
		Log.d(TAG, "Inserting:"+dbObj.getJson().toString());
		
		if(mMusubi == null || dbObj.getSenderId()!=MY_ID){
			return;
		}
		
		// TODO REMOVE WORKAROUND
		// receive obj THREE times
		if(dbObj.getHash() == 0 || dbObj.getHash() == lastInsertHash){
			return;
		}
		lastInsertHash = dbObj.getHash();
		
		DBHelper dbh = new DBHelper(context);
		if(dbObj.getType().equals(TYPE_APP_STATE)){
			Log.d(TAG, "newhash:"+dbObj.getHash());
			dbh.storeAppobjInDatabase(dbObj,context);
		}else if(dbObj.getType().equals(TYPE_PICTURE)){
			dbh.storePicobjInDatabase(dbObj,context);
		}else if(dbObj.getType().equals(TYPE_LINK)){
			dbh.storeLinkobjInDatabase(dbObj,context);
		}
		dbh.close();
	}

	public void readItemsForlist(Context context){
		itemsHash.clear();
		DBHelper dbh = new DBHelper(context);
		String[] projection = {ItemObject.OBJHASH};
		String selection = null;
		String[] selectionArgs = null;
		String sortOrder = null;
		Cursor c = dbh.queryItemList(projection, selection, selectionArgs, sortOrder);
		if (c != null && c.moveToFirst()) {
			for (int i = 0; i < c.getCount(); i++) {
				itemsHash.add(i, c.getLong(0));
				c.moveToNext();
			}
		}
		Log.d(TAG, "hash:"+itemsHash.toString());
		c.close();
		dbh.close();
	}
	public int getFeedItemListCount(){
		return itemsHash.size();
	}
	public FeedItemListItem getFeedItemListItem(int position, Context context){
		long hash = itemsHash.get(position);
		DBHelper dbh = new DBHelper(context);
		FeedItemListItem item = dbh.getItemForHash(hash);
		dbh.close();
		return item;
	}
	public DbObj getFeedItemObj(int position){
		long hash = itemsHash.get(position);
		return mMusubi.objForHash(hash);
	}


    public DbObj getLatestAppstate(DbObj appobj){
    	DbObj obj = null;
		if(appobj!= null){
			DbFeed mFeed = appobj.getSubfeed();
			String sortOrder = "timestamp desc";
			String selection = "type = 'appstate'";
			Cursor c = mFeed.query(null, selection, null, sortOrder);
			if(c != null && c.moveToFirst()){
				obj =  mMusubi.objForCursor(c);
			}
		}else{
			Log.e(TAG, "Cannot get App obj from feed!");
		}
		return obj;
    }

	public DbObj getObjByKey(int slideNo, DbObj app){
		DbObj obj = null;
		DbFeed mFeed = app.getSubfeed();
		String selection = "key_int = '"+slideNo+"'";
		String sortOrder = "timestamp asc";
		Cursor c = mFeed.query(null, selection, null, sortOrder);
		Log.d(TAG, "RESULT:"+c.getCount());
		if(c != null && c.moveToFirst()){
			obj =  mMusubi.objForCursor(c);
		}
		return obj;
	}

	public void pushImageToSubfeed(Context context, Uri imageUri, int index) {
		try {
			
	        ContentResolver cr = context.getContentResolver();
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(cr.openInputStream(imageUri), null, options);
			
			// TODO use BitmapHelper Class
			int targetSize = 200;
			int xScale = (options.outWidth  + targetSize - 1) / targetSize;
			int yScale = (options.outHeight + targetSize - 1) / targetSize;
			int scale = xScale < yScale ? xScale : yScale;
			
			int size = 2000000;
			FileInputStream file = new FileInputStream(imageUri.getPath());
			try {
				size = file.available();
				file.close();
				Log.d(TAG, "Filesize:"+String.valueOf(size));
			} catch (IOException e1) {
				Log.d(TAG, "Failed to get filesize.");
			}
		
			options.inJustDecodeBounds = false;
			options.inSampleSize = scale;
			InputStream is = cr.openInputStream(imageUri);
			Bitmap sourceBitmap = BitmapFactory.decodeStream(is, null, options);
			
	        int width = sourceBitmap.getWidth();
	        int height = sourceBitmap.getHeight();
	        int cropSize = Math.min(width, height);

	        float scaleSize = ((float) targetSize) / cropSize;

	        Matrix matrix = new Matrix();
	        matrix.postScale(scaleSize, scaleSize);
	        float rotation = rotationForImage(context, imageUri);
	        if (rotation != 0f) {
	            matrix.preRotate(rotation);
	        }

	        Bitmap resizedBitmap = Bitmap.createBitmap(
	                sourceBitmap, 0, 0, width, height, matrix, true);

	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
	        byte[] data = baos.toByteArray();
	        sourceBitmap.recycle();
	        sourceBitmap = null;
	        resizedBitmap.recycle();
	        resizedBitmap = null;
	        System.gc(); // TODO: gross.

	        // TODO: Proper Content Corral API.
	        JSONObject base = new JSONObject();
	        try {
	            String type = cr.getType(imageUri);
	            if (type == null) {
	                type = "image/jpeg";
	            }
	            base.put(OBJ_LOCAL_URI, imageUri.toString());
	            base.put(OBJ_MIME_TYPE, type);
	            base.put(OBJ_FILESIZE, size);
//	            String localIp = getLocalIpAddress();
//	            if (localIp != null) {
//	                base.put(ATTR_LAN_IP, localIp);
//	            }
	        } catch (JSONException e) {
				Log.e(TAG, "Failed to post JSON", e);
	        }

			mMusubi.getObj().getSubfeed().postObj(new MemObj(TYPE_PICTURE, base, data, index));
		} catch (FileNotFoundException e) {
			Log.e(TAG, "File not Found", e);
		}
	}


	// TODO use BitmapHelper Class
    static float rotationForImage(Context context, Uri uri) {
        if (uri.getScheme().equals("content")) {
            String[] projection = { Images.ImageColumns.ORIENTATION };
            Cursor c = context.getContentResolver().query(
                    uri, projection, null, null, null);
            try {
                if (c.moveToFirst()) {
                    return c.getInt(0);
                }
            } finally {
                c.close();
            }
        } else if (uri.getScheme().equals("file")) {
            try {
                ExifInterface exif = new ExifInterface(uri.getPath());
                int rotation = (int) exifOrientationToDegrees(
                        exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_NORMAL));
                return rotation;
            } catch (IOException e) {
                Log.e(TAG, "Error checking exif", e);
            }
        }
        return 0f;
    }
    static float exifOrientationToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }
    
	// ----------------------------------------------------------
	// Utility
	// ----------------------------------------------------------
    
    public Bitmap objToBitmap(DbObj obj) {
    	if(obj!=null){
    		byte[] bytes = obj.getRaw();
    		if(bytes!=null){
    			return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    		}
    	}
    	return BitmapHelper.getDummyBitmap(300, 200, Color.LTGRAY);
    }
    
	// from ContentCorral
    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
                    .hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
                        .hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        // not ready for IPv6, apparently.
                        if (!inetAddress.getHostAddress().contains(":")) {
                            return inetAddress.getHostAddress().toString();
                        }
                    }
                }
            }
        } catch (SocketException ex) {
        	ex.printStackTrace();
        }
        return null;
    }

	public String getExtension(String fname){
        String filenameArray[] = fname.split("\\.");
        String extension = filenameArray[filenameArray.length-1];
        if(extension!=null){
        	return "."+extension;
        }else{
        	return "";
        }
	}
	
	public void postUpdate(Postdata postdata, Context context){
		
		Uri localuri = null;
		byte[] rawdata = null;
		if(postdata.datatype.equals(Postdata.TYPE_STREAM) && postdata.uri!=null){
			try {
				Uri uri = Uri.parse(postdata.uri);
				InputStream input = context.getContentResolver().openInputStream(uri);
				String dst = uri.getLastPathSegment();
				// set extension if it doesn't have
				if(MimeTypeMap.getFileExtensionFromUrl(uri.toString()).isEmpty()){
					MimeTypeMap mime = MimeTypeMap.getSingleton();
					String ext = mime.getExtensionFromMimeType(context.getContentResolver().getType(uri));
					if(ext!=null && !ext.isEmpty()){
						dst += "."+ext;
					}
				}
				File externalCacheDir = new File(new File(new File(new File(Environment.getExternalStorageDirectory(), "Android"), "data"), context.getPackageName()), "cache");
				externalCacheDir.mkdirs();
				File tmpfile = new File(externalCacheDir, dst);
				if(tmpfile.exists()){
					tmpfile = new File(externalCacheDir, new Date().getTime() + dst);
				}
				OutputStream output = new FileOutputStream(tmpfile);
				
				int DEFAULT_BUFFER_SIZE = 1024 * 4;
				byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
				int n = 0;
				int size = 0;
				while (-1 != (n = input.read(buffer))) {
				  output.write(buffer, 0, n);
				  size+=n;
				}
				input.close();
				output.close();

				localuri = Uri.fromFile(tmpfile);
				postdata.localUri = localuri.toString();
				if(size<200000){
					input = context.getContentResolver().openInputStream(localuri);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					while (-1 != (n = input.read(buffer))) {
					  baos.write(buffer, 0, n);
					}
					input.close();
					baos.close();
			        rawdata = baos.toByteArray();
				}
				postdata.filesize = size;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				Log.e(TAG, "File not found");
				rawdata = null;
			} catch (IOException e) {
				Log.e(TAG, "IOException occurs");
				rawdata = null;
			}
		}
		
		JSONObject b;
		try {
			String objtype = postdata.objtype;
			postdata.thumb = null; // erace binary thumb data
			postdata.attach = null; // erace attach data
			postdata.objtype = null;
			postdata.lanip = getLocalIpAddress();
			postdata.wanip = wanip;

			// select icon
			String msg;
			String ftype = "";
			int iconid = R.drawable.icon_default;
			if(postdata.datatype.equals(Postdata.TYPE_TEXT_WITH_LINK)){
				iconid = R.drawable.icon_html;
			}else if(postdata.datatype.equals(Postdata.TYPE_TEXT)){
				iconid = R.drawable.icon_text;
			}else{
				String ext = MimeTypeMap.getFileExtensionFromUrl(postdata.localUri);
				if(ext!=null){
					if(ext.equalsIgnoreCase("ppt") || ext.equalsIgnoreCase("pptx")){
						ftype  = "Powerpoint";
						iconid = R.drawable.icon_powerpoint;
					}else if(ext.equalsIgnoreCase("xls") || ext.equalsIgnoreCase("xlsx") || ext.equalsIgnoreCase("csv")){
						ftype  = "Excel";
						iconid = R.drawable.icon_excel;
					}else if(ext.equalsIgnoreCase("doc") || ext.equalsIgnoreCase("docx")){
						ftype  = "Word";
						iconid = R.drawable.icon_word;
					}else if(ext.equalsIgnoreCase("pdf")){
						ftype  = "PDF";
						iconid = R.drawable.icon_pdf;
					}else if(ext.equalsIgnoreCase("mov")){
						ftype  = "video";
						iconid = R.drawable.icon_mov;
					}else if(ext.equalsIgnoreCase("xml")){
						ftype  = "xml";
						iconid = R.drawable.icon_xml;
					}else if(ext.equalsIgnoreCase("html")){
						ftype  = "html";
						iconid = R.drawable.icon_html;
					}else if(ext.equalsIgnoreCase("png")){
						ftype  = "image";
						iconid = R.drawable.icon_png;
						if(objtype.equals(TYPE_UNKNOWN)){
							objtype = TYPE_PICTURE;
						}
					}else if(ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("jpeg")){
						ftype  = "image";
						iconid = R.drawable.icon_jpg;
						if(objtype.equals(TYPE_UNKNOWN)){
							objtype = TYPE_PICTURE;
						}
					}else if(ext.equalsIgnoreCase("zip") || ext.equalsIgnoreCase("rar")){
						ftype  = "compressed";
						iconid = R.drawable.icon_compress;
					}else if(postdata.mimetype.startsWith("image/")){
						ftype  = "image";
						iconid = R.drawable.icon_image;
						if(objtype.equals(TYPE_UNKNOWN)){
							objtype = TYPE_PICTURE;
						}
					}else if(postdata.mimetype.startsWith("video/")){
						ftype  = "video";
						iconid = R.drawable.icon_movie;
						if(objtype.equals(TYPE_UNKNOWN)){
							objtype = TYPE_VIDEO;
						}
					}else if(postdata.mimetype.startsWith("audio/")){
						ftype  = "audio";
						iconid = R.drawable.icon_music;
						if(objtype.equals(TYPE_UNKNOWN)){
							objtype = TYPE_MUSIC;
						}
					}else if(postdata.mimetype.startsWith("text/")){
						ftype  = "text";
						iconid = R.drawable.icon_text;
					}
				}
			}
			
			// message in feed
			if(postdata.comment == null || postdata.comment.isEmpty()){
				if(postdata.datatype.equals(Postdata.TYPE_TEXT_WITH_LINK)){
					msg = "Sharing link \""+postdata.title+"\".";
					iconid = R.drawable.icon_html;
					if(objtype.equals(TYPE_UNKNOWN)){
						objtype = TYPE_LINK;
					}
				}else if(postdata.datatype.equals(Postdata.TYPE_TEXT)){
					if(postdata.title==null || postdata.title.isEmpty()){
						msg = "Sharing notes. (tap to view detail)";
					}else{
						msg = "Sharing notes \""+postdata.title+"\". (tap to view detail)";
					}
				}else{
					if(postdata.title==null || postdata.title.isEmpty()){
						msg = "Sharing "+ftype+" file.";
					}else{
						msg = "Sharing "+ftype+" file: \""+postdata.title+"\".";
					}
				}
			}else{
				msg = postdata.comment+"<br />\""+postdata.title+"\".";
			}
			FeedRenderable renderable = FeedRenderable.fromText(msg);
			
			// for picture obj sumbnail
			if(objtype.equals(TYPE_PICTURE) && rawdata == null && localuri!=null){
				rawdata = prepareThumb(context, localuri);
			}
			
			// avoid to send unknown obj
			if(objtype.equals(TYPE_UNKNOWN)){
				objtype = TYPE_APP_STATE;
			}

			// convert to JSON
			Gson gs = new Gson();
			b = new JSONObject(gs.toJson(postdata));
			
			// add thumbnail
			Bitmap icon = BitmapFactory.decodeResource(context.getResources(), iconid);
			String data = Base64.encodeToString(BitmapHelper.bitmapToBytes(icon), Base64.DEFAULT);
			b.put(B64JPGTHUMB, data);
			renderable.withJson(b);
			mMusubi.getFeed().postObj(new MemObj(objtype, b, rawdata, null));
		} catch (JSONException e) {
			Log.e(TAG, "Failed to post JSON", e);
		}
	}

	public byte[] prepareThumb(Context context, Uri imageUri) {
		try {
	        ContentResolver cr = context.getContentResolver();
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(cr.openInputStream(imageUri), null, options);
			
			// TODO use BitmapHelper Class
			int targetSize = 200;
			int xScale = (options.outWidth  + targetSize - 1) / targetSize;
			int yScale = (options.outHeight + targetSize - 1) / targetSize;
			int scale = xScale < yScale ? xScale : yScale;
			
			int size = 2000000;
			FileInputStream file = new FileInputStream(imageUri.getPath());
			try {
				size = file.available();
				file.close();
				Log.d(TAG, "Filesize:"+String.valueOf(size));
			} catch (IOException e1) {
				Log.d(TAG, "Failed to get filesize.");
			}
		
			options.inJustDecodeBounds = false;
			options.inSampleSize = scale;
			InputStream is = cr.openInputStream(imageUri);
			Bitmap sourceBitmap = BitmapFactory.decodeStream(is, null, options);
			
	        int width = sourceBitmap.getWidth();
	        int height = sourceBitmap.getHeight();
	        int cropSize = Math.min(width, height);

	        float scaleSize = ((float) targetSize) / cropSize;

	        Matrix matrix = new Matrix();
	        matrix.postScale(scaleSize, scaleSize);
	        float rotation = rotationForImage(context, imageUri);
	        if (rotation != 0f) {
	            matrix.preRotate(rotation);
	        }

	        Bitmap resizedBitmap = Bitmap.createBitmap(
	                sourceBitmap, 0, 0, width, height, matrix, true);

	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
	        byte[] data = baos.toByteArray();
	        sourceBitmap.recycle();
	        sourceBitmap = null;
	        resizedBitmap.recycle();
	        resizedBitmap = null;
	        System.gc(); // TODO: gross.
	        
	        return data;

		} catch (FileNotFoundException e) {
			Log.e(TAG, "File not Found", e);
		}
		return null;
	}

	// ----------------------------------------------------------
	// UPnP
	// ----------------------------------------------------------
	public void openPort(Context context){
		if(context.getSharedPreferences("main",0).getBoolean("UPnP", false)){
			Log.e(TAG, "Trying to open port.");
	        wanip = null;
	        wanport = null;
			upnp=null;
			upnp = new MyUpnpController(context,upnpPort);
			upnp.startService();
		}
	}
	public void closePort(Context context){
        wanip = null;
        wanport = null;
		if(upnp!=null){
			upnp.closePort();
		}
	}
	private final class MyUpnpController extends UpnpController{
		public MyUpnpController(Context context, int port) {
			super(context, port);
		}
		
		@Override
		public void onGetExternalIP(String ipaddress){
            Log.e(TAG, "Suceed to set portmapping! ExternalIP:"+ipaddress+", LocalIP: "+getLocalIpAddress()+" Port:"+getPort());
            wanip = ipaddress;
            wanport = String.valueOf(getPort());
		}
		
		@Override
	    public void onPortClosed(){
			Log.e(TAG, "Suceed to remove portmapping!");
			upnp.stopService();
		}
	}
    public String getWanIpAddress() {
    	return wanip;
    }
    public String getWanPort() {
    	return wanport;
    }
    public String getLanIpAddress() {
    	return lanip;
    }
    public void setLanIpAddress(String ip) {
    	lanip = ip;
    }
    public void setWanIpAddress(String ip) {
    	wanip = ip;
    }
    public void setWanPort(String port) {
    	wanport = port;
    }

	// ----------------------------------------------------------
	// Listview in feed
	// ----------------------------------------------------------

	synchronized public Bitmap getTitleBitmap(DbObj app) {
		DbObj obj = getObjByKey(0,app);
    	if(obj!=null){
    		byte[] bytes = obj.getRaw();
    		if(bytes!=null){
    			Bitmap org =  BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    			Bitmap resized = BitmapHelper.getResizedBitmap(org, 160, 120, 0);
    			if(resized!=null){
    				return resized;
    			}
    		}
    	}
		return BitmapHelper.getDummyBitmap(160, 120, Color.LTGRAY);
	}

	synchronized public Bitmap getThumbBitmap(int position, Context context) {
		long hash = itemsHash.get(position);
		DBHelper dbh = new DBHelper(context);
		try{
			byte[] bytes = dbh.getThumbForHash(hash);
			if(bytes!=null){
				Bitmap org =  BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
				Bitmap resized = BitmapHelper.getResizedBitmap(org, 48, 48, 0);
				if(resized!=null){
					return resized;
				}
			}
			return BitmapHelper.getDummyBitmap(48, 48, Color.WHITE);
		}finally{
			dbh.close();
		}
	}

    public static int getIconid(String uri, String mimetype){
    	int iconid = R.drawable.icon_default;
		String ext = MimeTypeMap.getFileExtensionFromUrl(uri);
		if(ext!=null){
			if(ext.equalsIgnoreCase("ppt") || ext.equalsIgnoreCase("pptx")){
				iconid = R.drawable.icon_powerpoint;
			}else if(ext.equalsIgnoreCase("xls") || ext.equalsIgnoreCase("xlsx") || ext.equalsIgnoreCase("csv")){
				iconid = R.drawable.icon_excel;
			}else if(ext.equalsIgnoreCase("doc") || ext.equalsIgnoreCase("docx")){
				iconid = R.drawable.icon_word;
			}else if(ext.equalsIgnoreCase("pdf")){
				iconid = R.drawable.icon_pdf;
			}else if(ext.equalsIgnoreCase("mov")){
				iconid = R.drawable.icon_mov;
			}else if(ext.equalsIgnoreCase("xml")){
				iconid = R.drawable.icon_xml;
			}else if(ext.equalsIgnoreCase("html")){
				iconid = R.drawable.icon_html;
			}else if(ext.equalsIgnoreCase("png")){
				iconid = R.drawable.icon_png;
			}else if(ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("jpeg")){
				iconid = R.drawable.icon_jpg;
			}else if(ext.equalsIgnoreCase("zip") || ext.equalsIgnoreCase("rar")){
				iconid = R.drawable.icon_compress;
			}else if(mimetype.startsWith("image/")){
				iconid = R.drawable.icon_image;
			}else if(mimetype.startsWith("video/")){
				iconid = R.drawable.icon_movie;
			}else if(mimetype.startsWith("audio/")){
				iconid = R.drawable.icon_music;
			}else if(mimetype.startsWith("text/")){
				iconid = R.drawable.icon_text;
			}
		}
		return iconid;
    }
}
