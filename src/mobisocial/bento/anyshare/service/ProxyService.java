package mobisocial.bento.anyshare.service;


import mobisocial.bento.anyshare.io.DataManager;
import mobisocial.bento.anyshare.io.Postdata;
import mobisocial.socialkit.musubi.Musubi;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

public class ProxyService extends Service {
	
	public class ProxyBinder extends Binder {
		
		public ProxyService getService() {
			return ProxyService.this;
		}
		
	}
	
	public static final String ACTION = "Proxy Service";
	private Musubi mMusubi;
	private Postdata mPostdata;
	public DataManager mManager = DataManager.getInstance();
	
	@Override
	public void onCreate() {
		super.onCreate();
//		Toast toast = Toast.makeText(getApplicationContext(), "onCreate()", Toast.LENGTH_SHORT);
//		toast.show();
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
//		Toast toast = Toast.makeText(getApplicationContext(), "onStart()", Toast.LENGTH_SHORT);
//		toast.show();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
//		Toast toast = Toast.makeText(getApplicationContext(), "onDestroy()", Toast.LENGTH_SHORT);
//		toast.show();
		if (mMusubi != null) {
			mMusubi = null;
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
//		Toast toast = Toast.makeText(getApplicationContext(), "onBind()", Toast.LENGTH_SHORT);
//		toast.show();
		return new ProxyBinder();
	}
	
	@Override
	public void onRebind(Intent intent) {
//		Toast toast = Toast.makeText(getApplicationContext(), "onRebind()", Toast.LENGTH_SHORT);
//		toast.show();
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
//		Toast toast = Toast.makeText(getApplicationContext(), "onUnbind()", Toast.LENGTH_SHORT);
//		toast.show();
		return true; // call onRebind() next time
	}
	
	public void setMusubi(Musubi musubi) {
		if(musubi!=null && (mMusubi == null || mMusubi.getFeed() == null || mMusubi.getFeed().getLatestObj() == null || 
				!musubi.getFeed().getLatestObj().getFeedName().equals(mMusubi.getFeed().getLatestObj().getFeedName()))){
			mMusubi = musubi;
			Toast toast = Toast.makeText(getApplicationContext(), "This group has been set as a default.\nYou can post files/links via AnyShare.", Toast.LENGTH_SHORT);
			toast.show();
		}
	}
	public Musubi getMusubi() {
		return mMusubi;
	}

	public Postdata getPostdata() {
		return mPostdata;
	}
	public void setPostdata(Postdata pd){
		mPostdata = pd;
	}
}