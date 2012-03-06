package mobisocial.bento.anyshare.ui;

import mobisocial.bento.anyshare.io.DataManager;
import mobisocial.bento.anyshare.io.Postdata;
import mobisocial.bento.anyshare.service.ProxyService;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.Musubi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActionBar;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import mobisocial.bento.anyshare.R;

public class HomeActivity extends FragmentActivity {
	//private static final String TAG = "HomeActivity";
	public static final int REQUEST_SAMPLE = 1;
	public static final int REQUEST_VIEW = 2;
	public static final int REQUEST_ITEMLIST = 3;
	private Musubi mMusubi;
	private Intent mIntent;
	private static final String TAG = "HomeActivity";
	private DataManager mManager = DataManager.getInstance();
	private Context mContext;
	private DbObj selObj;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        mContext = this;

		final ActionBar actionBar = getSupportActionBar();
		// set defaults for logo & home up
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayUseLogoEnabled(true);
		mIntent = getIntent();
				
		// Check if this activity launched from Home Screen
		if (!Musubi.isMusubiIntent(mIntent)) {
			boolean bInstalled = false;
			try {
				bInstalled = Musubi.isMusubiInstalled(getApplication());
			} catch (Exception e) {
				bInstalled = false;
			}
			if (!bInstalled) {
				goMarket();
			}
			mMusubi = null;
		} else {
			mMusubi = Musubi.getInstance(this, mIntent);
			// TODO Find the reason why getContextObj() doesn't work...
			if(mIntent.hasExtra(Musubi.EXTRA_OBJ_HASH)){
				selObj = mMusubi.objForHash((Long) mIntent.getLongExtra(Musubi.EXTRA_OBJ_HASH,0));
			}
		}
		new HomeAsyncTask(this).execute();
    }

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_SAMPLE || requestCode == REQUEST_VIEW || requestCode == REQUEST_ITEMLIST) {
			finish();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unbindService(serviceConnection);
		unregisterReceiver(receiver);
//		proxyService.stopSelf();
	}

    private void goSample() {
		// Intent
		Intent intent = new Intent(this, SampleActivity.class);
		startActivityForResult(intent, REQUEST_SAMPLE);
    }

    private void goList() {
		// Intent
		Intent intent = new Intent(this, FeedItemListActivity.class);
		startActivityForResult(intent, REQUEST_ITEMLIST);
    }

    private void goView() {
		// Intent
		Intent intent = new Intent(this, ViewActivity.class);
		startActivityForResult(intent, REQUEST_VIEW);
    }
    
    private void goPostview(Postdata postdata){
		Intent intent = new Intent(this, PostActivity.class);
		intent.setAction(Intent.ACTION_SEND);
		if(postdata.mimetype.equals("text/html") && !postdata.datatype.equals(Postdata.TYPE_STREAM)){
			intent.setType("text/plain");
		}else{
			intent.setType(postdata.mimetype);
		}
		intent.putExtra(Intent.EXTRA_SUBJECT, postdata.title);
		intent.putExtra(Intent.EXTRA_TEXT, postdata.text);
		if(postdata.datatype.equals(Postdata.TYPE_STREAM)){
			intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(postdata.uri));
		}
		startActivityForResult(intent, REQUEST_VIEW);
    }

    public void goMarket() {
		AlertDialog.Builder marketDialog = new AlertDialog.Builder(this)
				.setTitle(R.string.market_dialog_title)
				.setMessage(R.string.market_dialog_text)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setCancelable(true)
				.setPositiveButton(getResources().getString(R.string.market_dialog_yes), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Go to Android Market
						startActivity(Musubi.getMarketIntent());
						finish();
					}
				})
				.setNegativeButton(getResources().getString(R.string.market_dialog_no),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								finish();
							}
						});
		marketDialog.create().show();
	}
	
	private class HomeAsyncTask extends AsyncTask<Void, Void, Boolean> {
		private DataManager mManager = DataManager.getInstance();
		private Context mContext;
		private ProgressDialog mProgressDialog = null;
		
		public HomeAsyncTask(Context context) {
			mContext = context;
		}

		@Override
		protected void onPreExecute() {
			// show progress dialog
			mProgressDialog = new ProgressDialog(mContext);
			mProgressDialog.setMessage(mContext.getString(R.string.home_loading));
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setCancelable(false);
			mProgressDialog.show();
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			// proxy service connection & get musubi obj
			Intent intent = new Intent(mContext, ProxyService.class);
			startService(intent);
			IntentFilter filter = new IntentFilter(ProxyService.ACTION);
			registerReceiver(receiver, filter);
			bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			try {
				if (mProgressDialog != null && mProgressDialog.isShowing()) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private class ProxyReceiver extends BroadcastReceiver {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Toast toast = Toast.makeText(getApplicationContext(), "Proxy Received!", Toast.LENGTH_LONG);
			toast.show();
		}
	}
	
	private ProxyService proxyService;
	private final ProxyReceiver receiver = new ProxyReceiver();
	
	private ServiceConnection serviceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			proxyService = ((ProxyService.ProxyBinder)service).getService();
			proxyService.setMusubi(mMusubi);
			mManager.init(mMusubi, mContext);
			mManager.setContextObj(selObj);
			
			Postdata postdata = proxyService.getPostdata();
			if(postdata!=null){
				Log.d(TAG, "Go Post");
				proxyService.setPostdata(null);
				goPostview(postdata);
			}else if(mManager.getContextObj()==null || 
					!mManager.getContextObj().getType().equals(DataManager.TYPE_APP_STATE)){
				if(mMusubi!=null){
					Log.d(TAG, "Go ListView");
					goList();
				}else{
					Log.d(TAG, "Go Sample");
					goSample();
				}
			}else{
				Log.d(TAG, "Go View");
				goView();
			}
			
		}
		
		@Override
		public void onServiceDisconnected(ComponentName className) {
			proxyService = null;
		}
		
	};

}