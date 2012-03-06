package mobisocial.bento.anyshare.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mobisocial.bento.anyshare.io.DataManager;
import mobisocial.bento.anyshare.io.Postdata;
import mobisocial.bento.anyshare.service.ProxyService;
import mobisocial.socialkit.musubi.Musubi;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActionBar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.text.SpannableString;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import mobisocial.bento.anyshare.R;

public class PostActivity extends FragmentActivity {
	private static final String TAG = "PostActivity";
	private Musubi mMusubi;
	private Intent mIntent;
	private DataManager mManager = DataManager.getInstance();
	private Context mContext;
	private Postdata postdata;
	private ActionBar actionBar;
	private ProxyService proxyService;
	private final ProxyReceiver receiver = new ProxyReceiver();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);
		mContext = this;
		mIntent  = getIntent();
		Log.d(TAG, mIntent.toString());
		postdata = new Postdata();

		actionBar = getSupportActionBar();
		// set defaults for logo & home up
		actionBar.setDisplayHomeAsUpEnabled(false);
		actionBar.setDisplayUseLogoEnabled(false);
		
		// setup content
		if (mIntent.getAction().equals(Intent.ACTION_SEND) && false) {
	         Uri uri = mIntent.getData();
	         Log.e(TAG, "Action:"+mIntent.getAction());
	         Log.e(TAG, "Type:"+mIntent.getType());
	         Log.e(TAG, String.valueOf(uri));

	         if ("file".equals(uri.getScheme())) {
	             File file = new File(uri.getPath());
	             // TODO file post 
                 Log.d(TAG, "File:"+String.valueOf(uri.getPath()));
	         } else if 
	             ("content".equals(uri.getScheme())){
	             try {
	                 StringBuffer buf = new StringBuffer();
	                 ContentResolver cr = getContentResolver();
	             
	                 BufferedReader reader = new BufferedReader(
	                     new InputStreamReader(cr.openInputStream(uri)));
	                 
	                 String line = null;
	                 while((line = reader.readLine()) != null) {
	                     buf.append(line).append("\n");
	                 }
	                 reader.close();
	                 Log.d(TAG, "Content:"+String.valueOf(buf));
	             } catch (Exception e) {
	             }
	         }
	     }
		
		// proxy service connection & get musubi obj
		Intent intent = new Intent(this, ProxyService.class);
		startService(intent);
		IntentFilter filter = new IntentFilter(ProxyService.ACTION);
		registerReceiver(receiver, filter);
		bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

    }
    
    private OnClickListener radio_listener = new OnClickListener() {
        public void onClick(View v) {
            // Perform action on clicks
            RadioButton rb = (RadioButton) v;
            if(rb.getId() == R.id.radio_musubi){
            	postdata.objtype = DataManager.TYPE_UNKNOWN;
            }else{
            	postdata.objtype = DataManager.TYPE_APP_STATE;
            }
        }
    };
    
	@Override
	public void onDestroy() {
		this.unbindService(serviceConnection);
		super.onDestroy();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_items_post, menu);
		return super.onCreateOptionsMenu(menu);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                return true;
            case R.id.menu_postfeed:
        		TextView title = (EditText) findViewById(R.id.post_title);
        		TextView text = (EditText) findViewById(R.id.post_message);
            	postdata.title = title.getText().toString().trim();
            	postdata.text  = text.getText().toString().trim();
            	mManager.postUpdate(postdata,this);
            	showToast("Post finished.", true);
            	finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public void onPause(){
		unregisterReceiver(receiver);
    	super.onPause();
    }
    @Override
    public void onResume(){
		IntentFilter filter = new IntentFilter(ProxyService.ACTION);
		registerReceiver(receiver, filter);
    	super.onResume();
    }


	// ---------------------------------------------------------------------------------
	// Service
	// ---------------------------------------------------------------------------------

	private class ProxyReceiver extends BroadcastReceiver {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Toast toast = Toast.makeText(getApplicationContext(), "Proxy Received!", Toast.LENGTH_LONG);
			toast.show();
		}
	}
	

	private ServiceConnection serviceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			proxyService = ((ProxyService.ProxyBinder)service).getService();
			
    		postdata.title    = mIntent.getStringExtra(Intent.EXTRA_SUBJECT);
    		postdata.mimetype = mIntent.getType();
    		if(postdata.mimetype == null){
    			postdata.mimetype = "*/*";
    		}
			// -------------------------------------------------------------------------
			// for text data
			// -------------------------------------------------------------------------
    		if(mIntent.hasExtra(Intent.EXTRA_STREAM)){
	    		postdata.datatype = Postdata.TYPE_STREAM;
	    		Log.d(TAG,String.valueOf(getIntent().getExtras().get(Intent.EXTRA_STREAM)));
	    		Uri streamuri = (Uri) getIntent().getExtras().get(Intent.EXTRA_STREAM);
	    		if(streamuri!=null){
	    			postdata.uri = streamuri.toString();
	    		}
    		}else if(mIntent.getType().startsWith("text")){
    			if(mIntent.getType().equals("text/html")){
    				SpannableString text = (SpannableString) mIntent.getExtras().get(Intent.EXTRA_TEXT);
    				postdata.text = text.toString();
    			}else{
    				postdata.text  = mIntent.getStringExtra(Intent.EXTRA_TEXT);
    			}
	    		postdata.uri = extractUrl(postdata.text);
	    		if(postdata.uri!=null){
		    		postdata.datatype = Postdata.TYPE_TEXT_WITH_LINK;
	    		}else{
		    		postdata.datatype = Postdata.TYPE_TEXT;
	    		}
    		}else{
    			Log.e(TAG, "File is not attached.");
    			finish();
	        }

	        // ------------------------------------------------------------------
			// Resume musubi obj
			// ------------------------------------------------------------------
			if(mMusubi==null){
				mMusubi = proxyService.getMusubi();
			}
			if(mMusubi==null){
				proxyService.setPostdata(postdata);
				goMusubi();
			}else{
				mManager.init(mMusubi, mContext);
				String gname = getGroupName(mMusubi.getFeed().getLatestObj().getFeedName());
				if(gname!=null){
					actionBar.setTitle("Post to "+gname);
				}
			}
			
    		// radio button config
    		RadioGroup radio = (RadioGroup) findViewById(R.id.radio_grp);
    		RadioButton radio_app = (RadioButton) findViewById(R.id.radio_app);
    		RadioButton radio_musubi = (RadioButton) findViewById(R.id.radio_musubi);
    		if(postdata.datatype.equals(Postdata.TYPE_TEXT_WITH_LINK)){
    			radio_app.setText("Link with text");
    			radio_musubi.setText("Link");
    		}else if(postdata.mimetype.startsWith("image/")){
    			radio_app.setText("Image file");
    			radio_musubi.setText("Picture");
    		}else{
    			radio.setVisibility(View.GONE);
    		}
        	postdata.objtype = DataManager.TYPE_APP_STATE;
        	radio_app.setOnClickListener(radio_listener);
        	radio_musubi.setOnClickListener(radio_listener);

    		TextView title = (EditText) findViewById(R.id.post_title);
    		TextView data = (TextView) findViewById(R.id.post_data);
    		TextView text = (EditText) findViewById(R.id.post_message);

    		if(postdata.title!=null && !postdata.title.isEmpty()){
    			title.setText(postdata.title);
    		}else if(postdata.uri!=null){
    			String ext = MimeTypeMap.getFileExtensionFromUrl(postdata.uri);
    			if(ext!=null && !ext.isEmpty()){
    				title.setText(Uri.parse(postdata.uri).getLastPathSegment());
    			}
    		}
    		if(postdata.uri!=null){
    			data.setText(postdata.uri);
    		}
    		if(postdata.text!=null && !postdata.text.equals(postdata.uri)){
    			text.setText(postdata.text);
    		}
    		if(postdata.uri==null || postdata.uri.isEmpty()){
    			data.setVisibility(View.GONE);
    		}
    		// show keyborad
//    		getWindow().setSoftInputMode(
//					WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    		
		}
		
		@Override
		public void onServiceDisconnected(ComponentName className) {
			proxyService = null;
		}
		
	};

	private void goMusubi() {
		AlertDialog.Builder musubiDialog = new AlertDialog.Builder(this)
				.setTitle(R.string.musubi_dialog_title)
				.setMessage(R.string.musubi_dialog_text)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setCancelable(false)
				.setPositiveButton(getResources().getString(R.string.musubi_dialog_yes), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						try {
							// Launching Musubi
			                Intent intent = new Intent(Intent.ACTION_MAIN);
			                intent.setClassName("edu.stanford.mobisocial.dungbeetle", "edu.stanford.mobisocial.dungbeetle.BootstrapActivity"); 
			                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
			                startActivity(intent);
			                finish();
						} catch (Exception e) {
//							goMarket();
							finish();
						}
					}
				})
				.setNegativeButton(getResources().getString(R.string.musubi_dialog_no),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								finish();
							}
						});
		musubiDialog.create().show();
	}
	
	private String extractUrl(String text) {
		final Pattern pattern = 
				Pattern.compile("(http://|https://){1}[\\w\\.\\-/:\\#\\?\\=\\&\\;\\%\\~\\+]+", 
						Pattern.CASE_INSENSITIVE);

	    Matcher matcher = pattern.matcher(text);
	    if(matcher.find()){
	      return matcher.group();
	    }
	    return null;
	}
	
	private String getGroupName(String feedname){
		Uri uri = Uri.parse("content://" + Musubi.AUTHORITY + "/groups/");
		String[] projection = { "name" };
        String selection = "feed_name = ?";
        String[] selectionArgs = new String[] {
            feedname
        };

		Cursor c = mContext.getContentResolver().query(uri, projection, selection, selectionArgs, null);
		try {
            if (c == null || !c.moveToFirst()) {
                return null;
            }
            return c.getString(c.getColumnIndexOrThrow("name"));
        } finally {
            if (c != null) {
                c.close();
            }
        }
	}
    protected void showToast(final String msg, final boolean longLength) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(
                        PostActivity.this,
                        msg,
                        longLength ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT
                ).show();
            }
        });
    }


}