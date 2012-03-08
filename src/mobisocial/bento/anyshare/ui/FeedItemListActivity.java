/*
 * Copyright (C) 2012 Kazuya Yokoyama <kazuya.yokoyama@gmail.com>, Kazumine Matoba <matoyan@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mobisocial.bento.anyshare.ui;

import mobisocial.bento.anyshare.R;
import mobisocial.bento.anyshare.io.DataManager;
import mobisocial.bento.anyshare.io.Postdata;
import mobisocial.bento.anyshare.service.ProxyService;
import mobisocial.bento.anyshare.util.DBHelper;
import mobisocial.bento.anyshare.util.ImageCache;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.FeedObserver;
import mobisocial.socialkit.musubi.Musubi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

public class FeedItemListActivity extends FragmentActivity {
	public static final String EXTRA_SB = "mobisocial.bento.slides.extra.EXTRA_SB";

	private static final int REQUEST_PICK = 1;

	private static final String TAG = "FeedAlbumListActivity";

	protected static final int REQUEST_MARKET = 1;

	private DataManager mManager = DataManager.getInstance();
	private Musubi mMusubi;
	private FeedItemListFragment mFeedItemListFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mMusubi = mManager.getMusubi();
		setContentView(R.layout.activity_feeditemlist);

		FragmentManager fm = getSupportFragmentManager();
		mFeedItemListFragment = (FeedItemListFragment) fm.findFragmentById(R.id.fragment_itemlist);

		mMusubi.getFeed().registerStateObserver(mStateObserver);
	}

	@Override
	protected void onDestroy() {
		ImageCache.clearCache();
		if (mMusubi != null) {
			mMusubi.getFeed().removeStateObserver(mStateObserver);
		}
		super.onDestroy();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
//		getActivityHelper().setupHomeActivity();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent ret) {
		if(requestCode == REQUEST_PICK){
			if(resultCode == RESULT_OK){
				Log.d(TAG, ret.toString());
				Uri uri = ret.getData();
				Intent intent = new Intent(this, PostActivity.class);
				intent.setAction(Intent.ACTION_SEND);
				String mimetype = getContentResolver().getType(uri);
				String ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
				
				MimeTypeMap mime = MimeTypeMap.getSingleton();
				if(mimetype==null || mimetype.isEmpty()){
					if(!ext.isEmpty()){
						mimetype = mime.getMimeTypeFromExtension(ext);
					}
				}
				
				String fname = uri.getLastPathSegment();
				if(ext.isEmpty()){
					fname += "."+mime.getExtensionFromMimeType(mimetype);
				}
				
				intent.setType(mimetype);
				intent.putExtra(Intent.EXTRA_SUBJECT, fname);
				intent.putExtra(Intent.EXTRA_TEXT, "");
				intent.putExtra(Intent.EXTRA_STREAM, uri);

				startActivityForResult(intent, HomeActivity.REQUEST_VIEW);

			}
		}
		super.onActivityResult(requestCode, resultCode, ret);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_items_feeditemlist, menu);
		return super.onCreateOptionsMenu(menu);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                return true;
            case R.id.menu_add:
            	prepareSelection();
                return true;
            case R.id.menu_refresh:
            	askRefresh();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

	// ----------------------------------------------------------
	// Musubi
	// ----------------------------------------------------------
	private final FeedObserver mStateObserver = new FeedObserver() {
		@Override
		public void onUpdate(DbObj obj) {
			mManager.insertItemDbFromFeed(obj, FeedItemListActivity.this);
        	mManager.readItemsForlist(FeedItemListActivity.this);
        	mFeedItemListFragment.refleshView();
		}
	};
	
	private boolean isFileManagerInstalled(){
        PackageManager pm = this.getPackageManager();
        String[] packages = {"com.rhmsoft.fm", "com.metago.astro", "com.estrongs.android.pop", 
        		"com.agilesoftresource", "com.agilesoftresource", "com.smartwho.SmartFileManager"};
        for(int i=0;i<packages.length;i++){
	        try {
	            pm.getApplicationInfo(packages[i], 0);
	            return true;
	        } catch (NameNotFoundException e) {
	        	// check next
	        }
        }
        return false;
	}

	private void prepareSelection(){
		if(isFileManagerInstalled()){
			goSelection();
		}else{
			askDownloadFM();
		}
	}
	
	private void goSelection(){
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("*/*");
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		startActivityForResult(intent, REQUEST_PICK);
	}
	
	private void askDownloadFM(){
		new AlertDialog.Builder(this)
		.setTitle(R.string.dlfm_dialog_title)
		.setMessage(R.string.dlfm_dialog_text)
		.setIcon(android.R.drawable.ic_dialog_info)
		.setCancelable(true)
		.setPositiveButton(getResources().getString(R.string.dlfm_dialog_yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				goMarket("com.rhmsoft.fm");
			}
		})
		.setNegativeButton(getResources().getString(R.string.refresh_dialog_no),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						goSelection();
					}
				})
		.create().show();
	}


	private void askRefresh(){
		new AlertDialog.Builder(this)
		.setTitle(R.string.refresh_dialog_title)
		.setMessage(R.string.refresh_dialog_text)
		.setIcon(android.R.drawable.ic_dialog_info)
		.setCancelable(true)
		.setPositiveButton(getResources().getString(R.string.refresh_dialog_yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
        		new RefreshAsyncTask(FeedItemListActivity.this).execute();
			}
		})
		.setNegativeButton(getResources().getString(R.string.refresh_dialog_no),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						return;
					}
				})
		.create().show();
	}
	
	private class RefreshAsyncTask extends AsyncTask<Void, Void, Boolean> {
		private DataManager mManager = DataManager.getInstance();
		private Context mContext;
		private ProgressDialog mProgressDialog = null;
		
		public RefreshAsyncTask(Context context) {
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
			DBHelper dbh = new DBHelper(mContext);
			dbh.truncateAll();
			dbh.close();
			
			mManager.updateItemDbFromFeed(mContext);
        	mManager.readItemsForlist(mContext);
            runOnUiThread(new Runnable() {
                public void run() {
                	mFeedItemListFragment.refleshView();
                }
            });
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
	
    private void goMarket(final String app_id){
		try{
	    	Uri uri = Uri.parse("market://details?id="+app_id);
	    	Intent intent = new Intent(Intent.ACTION_VIEW, uri);
	    	startActivityForResult(intent, REQUEST_MARKET);
		}catch(ActivityNotFoundException anfe){
			Log.e(TAG, "Application Not Found.");
		}
    }
}