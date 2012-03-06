package mobisocial.bento.anyshare.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import mobisocial.bento.anyshare.io.DataManager;
import mobisocial.bento.anyshare.io.Postdata;
import mobisocial.bento.anyshare.service.ProxyService;
import mobisocial.bento.anyshare.util.CorralClient;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.Musubi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnCancelListener;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActionBar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import mobisocial.bento.anyshare.R;

public class ViewActivity extends FragmentActivity {
	private static final String TAG = "ViewActivity";
	private static final int REQUEST_BROWSER = 1;
	private static final int REQUEST_FILEVIEW = 2;
	private static final int REQUEST_MARKET = 3;
	private static final String APP_THINKFREE = "com.tf.thinkdroid.amlite";
	private Musubi mMusubi;
	private DataManager mManager = DataManager.getInstance();
	private Postdata postdata;
	private CorralClient mCorralClient;
	private Context mContext;
	private boolean fail = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		postdata = new Postdata();
		mContext = this;
        mCorralClient = CorralClient.getInstance(this);
        fail = false;

		final ActionBar actionBar = getSupportActionBar();
		// set defaults for logo & home up
		actionBar.setDisplayHomeAsUpEnabled(false);
		actionBar.setDisplayUseLogoEnabled(false);
		
		mMusubi = mManager.getMusubi();
		if(mMusubi == null || !mManager.getContextObj().getType().equals(DataManager.TYPE_APP_STATE)){
			finish();
			return;
		}
		
		Gson gs = new Gson();
		postdata = gs.fromJson(mManager.getContextObj().getJson().toString(), Postdata.class);
		
		setContentView(R.layout.activity_view);
		TextView title = (TextView) findViewById(R.id.view_title);
		TextView text = (TextView) findViewById(R.id.view_text);
		ImageView thumb = (ImageView) findViewById(R.id.view_thumb);
		
		thumb.setVisibility(View.GONE);
		title.setText(postdata.title);
		text.setText(postdata.text);

		goDetail();
		
    }

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == REQUEST_BROWSER){
			// do nothing
		}else{
			if(fail==false){
				finish();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_items_view, menu);
		return super.onCreateOptionsMenu(menu);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                return true;
            case R.id.menu_viewdetail:
            	goDetail();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

	private void goDetail(){
		Log.d(TAG, String.valueOf(postdata.uri));
		Log.d(TAG, String.valueOf(postdata.mimetype));
		Log.d(TAG, "localUri="+String.valueOf(postdata.localUri));

		if(postdata.datatype.equals(Postdata.TYPE_TEXT_WITH_LINK) && postdata.uri!=null && !postdata.uri.isEmpty()){
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(postdata.uri));
			startActivityForResult(intent, REQUEST_BROWSER);
			return;
		}
		
		if(postdata.datatype.equals(Postdata.TYPE_STREAM)){
			String fname = Uri.parse(postdata.localUri).getLastPathSegment();
//			String fname = CorralClient.hashToString(mManager.getContextObj().getHash())+"."+MimeTypeMap.getFileExtensionFromUrl(postdata.localUri);
			Log.d(TAG, "fname:"+fname);
			File externalCacheDir = new File(new File(new File(new File(new File(Environment.getExternalStorageDirectory(), "Android"), "data"), this.getPackageName()), "cache"), CorralClient.hashToString(mManager.getContextObj().getHash()));
			externalCacheDir.mkdirs();
			final File cachefile = new File(externalCacheDir,fname);
			Log.d(TAG, "Opening:"+cachefile.getAbsolutePath());
			if(!cachefile.exists() && mManager.getContextObj().getRaw() != null){
				try {
					FileOutputStream output = new FileOutputStream(cachefile);
					output.write(mManager.getContextObj().getRaw());
					output.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(cachefile.exists()){
				viewfile(cachefile);
				return;
			}else{
				// ask to download original file
				AlertDialog.Builder marketDialog = new AlertDialog.Builder(this)
				.setTitle(R.string.corral_dialog_title)
				.setMessage(R.string.corral_dialog_text)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setCancelable(false)
				.setPositiveButton(getResources().getString(R.string.corral_dialog_yes), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Go to Android Market
						new PrepareForCorral(mContext,cachefile).execute();
					}
				})
				.setNegativeButton(getResources().getString(R.string.corral_dialog_no),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								finish();
							}
						});
				marketDialog.create().show();
			}
		}
	}
	
	private class PrepareForCorral extends AsyncTask<Void, Integer, Boolean> implements OnCancelListener {
		private DataManager mManager = DataManager.getInstance();
		private Context mContext;
		private File cachefile;
		private ProgressDialog mProgressDialog = null;
		
		public PrepareForCorral(Context context, File file) {
			mContext = context;
			cachefile = file;
		}

		@Override
		protected void onPreExecute() {
			// show progress dialog
			mProgressDialog = new ProgressDialog(mContext);
			mProgressDialog.setMessage(mContext.getString(R.string.view_downloading));
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setIndeterminate(false);
			mProgressDialog.setCancelable(false);
			mProgressDialog.setOnCancelListener(this);
			mProgressDialog.setMax(100);
			mProgressDialog.show();
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			DbObj obj = mManager.getContextObj();
	        Uri fileUri;

	        try {
		        if(postdata.wanip!=null){
		        	fileUri = mCorralClient.fetchContent(obj, mManager.getMusubi(), postdata.wanip);
		        	if (fileUri == null) {
		                Log.d(TAG, "IP:"+String.valueOf(postdata.lanip));
		            	fileUri = mCorralClient.fetchContent(obj, mManager.getMusubi(), postdata.lanip);
		            	if (fileUri == null) {
		            		Log.d(TAG, "Failed to get file.");
		        			corralFailed(cachefile);
		            		return false;
		            	}
		        	}
		        }else{
		            Log.d(TAG, "IP:"+String.valueOf(postdata.lanip));
		        	fileUri = mCorralClient.fetchContent(obj, mManager.getMusubi(), postdata.lanip);
		        }
		        if(fileUri == null){
		    		Log.d(TAG, "Failed to get file.");
					corralFailed(cachefile);
					return false;
		        }
		        Log.d(TAG, "Opening file " + fileUri);
		        
				OutputStream output = new FileOutputStream(cachefile);
				InputStream input = getContentResolver().openInputStream(fileUri);
				int DEFAULT_BUFFER_SIZE = 1024 * 4;
				byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
				int n = 0;
				int size = 0;
				while (-1 != (n = input.read(buffer))) {
				  output.write(buffer, 0, n);
				  size+=n;
				  publishProgress((int) Math.floor(100 * size/postdata.filesize));
				}
				input.close();
				output.close();
				if(size>0 && size==postdata.filesize){
					viewfile(cachefile);
				}else{
					cachefile.delete();
					corralFailed(cachefile);
					return false;
				}

			} catch (IOException e) {
				e.printStackTrace();
				cachefile.delete();
				corralFailed(cachefile);
				return false;
			}
			return true;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			mProgressDialog.setProgress(values[0].intValue());
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
			
		@Override
		public void onCancel(DialogInterface dialog) {
			mProgressDialog.dismiss();
			cachefile.delete();
			this.cancel(true);
			showToast("Download Canceled.",true);
			finish();
		}

	}
	
	private void corralFailed(File cachefile){
		Log.e(TAG, "CorralFailed");
        runOnUiThread(new Runnable() {
            public void run() {
				AlertDialog.Builder corralfailedDialog = new AlertDialog.Builder(mContext)
				.setTitle(R.string.cfailed_dialog_title)
				.setMessage(R.string.cfailed_dialog_text)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setCancelable(false)
				.setPositiveButton(getResources().getString(R.string.cfailed_dialog_yes), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// TODO Download Later
						showToast("Download will be started.", true);
						finish();
					}
				})
				.setNegativeButton(getResources().getString(R.string.cfailed_dialog_no),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								finish();
							}
						});
				corralfailedDialog.create().show();
            }
        });
	}
	private void viewfile(File cachefile){
		Intent intent = new Intent(Intent.ACTION_VIEW);
		MimeTypeMap mime = MimeTypeMap.getSingleton();
		String mimetype = mime.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(postdata.localUri));
		if(mimetype==null || mimetype.isEmpty()){
			mimetype = postdata.mimetype;
		}
	
		try{
			intent.setDataAndType(Uri.fromFile(cachefile), mimetype);
			startActivityForResult(intent, REQUEST_FILEVIEW);
		}catch (ActivityNotFoundException anfe){
			fail=true;
			activityNotFound();
		}
	}
	
	private void activityNotFound(){
		Log.e(TAG, "Activity not found occurs.");
		
		// TODO show download dialog for viewer apps
		String ext = MimeTypeMap.getFileExtensionFromUrl(postdata.localUri);
		if(ext.equalsIgnoreCase("xls") || ext.equalsIgnoreCase("xlsx") || 
				ext.equalsIgnoreCase("doc") || ext.equalsIgnoreCase("docx") || 
				ext.equalsIgnoreCase("ppt") || ext.equalsIgnoreCase("pptx")){
			goMarket(APP_THINKFREE);
		}else{
			new AlertDialog.Builder(this)
			.setTitle(R.string.appnotfound_dialog_title)
			.setMessage(R.string.appnotfound_dialog_text)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setCancelable(false)
			.setPositiveButton(getResources().getString(R.string.appnotfound_dialog_yes), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					finish();
				}
			}).create().show();
		}
	}
	
	private void appNotFound(){
		new AlertDialog.Builder(this)
		.setTitle(R.string.appnotfound_dialog_title)
		.setMessage(R.string.appnotfound_dialog_text)
		.setIcon(android.R.drawable.ic_dialog_info)
		.setCancelable(false)
		.setPositiveButton(getResources().getString(R.string.appnotfound_dialog_yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				finish();
			}
		}).create().show();
	}

    protected void showToast(final String msg, final boolean longLength) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(
                        ViewActivity.this,
                        msg,
                        longLength ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void goMarket(final String app_id){
		new AlertDialog.Builder(this)
		.setTitle(R.string.appnotfound2_dialog_title)
		.setMessage(R.string.appnotfound2_dialog_text)
		.setIcon(android.R.drawable.ic_dialog_info)
		.setCancelable(false)
		.setPositiveButton(getResources().getString(R.string.appnotfound2_dialog_yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				try{
			    	Uri uri = Uri.parse("market://details?id="+app_id);
			    	Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			    	startActivityForResult(intent, REQUEST_MARKET);
				}catch(ActivityNotFoundException anfe){
					appNotFound();
				}
			}
		}).setNegativeButton(getResources().getString(R.string.appnotfound2_dialog_no), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				finish();
			}
		})
		.create().show();

    }
}
