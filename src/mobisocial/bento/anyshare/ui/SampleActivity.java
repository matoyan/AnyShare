package mobisocial.bento.anyshare.ui;

import mobisocial.bento.anyshare.io.DataManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.widget.TextView;

import mobisocial.bento.anyshare.R;

public class SampleActivity extends FragmentActivity {
	//private static final String TAG = "SampleActivity";
	
	private DataManager mManager = DataManager.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

		final ActionBar actionBar = getSupportActionBar();
		// set defaults for logo & home up
		actionBar.setDisplayHomeAsUpEnabled(false);
		actionBar.setDisplayUseLogoEnabled(false);
		
		TextView msg = (TextView) findViewById(R.id.message);
		msg.setText("Please launch on Musubi to view / post files.");
		
		goMusubi();
    }

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_items_sample, menu);
		return super.onCreateOptionsMenu(menu);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                return true;
            case R.id.menu_settings:
            	// nothing to do
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


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
			                finish();
//							goMarket();
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
}