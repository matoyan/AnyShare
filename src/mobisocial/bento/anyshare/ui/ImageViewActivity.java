package mobisocial.bento.anyshare.ui;

import mobisocial.bento.anyshare.io.DataManager;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.Musubi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.ImageView;
import mobisocial.bento.anyshare.R;

public class ImageViewActivity extends FragmentActivity {
	private static final String TAG = "ImageViewActivity";
	private Musubi mMusubi;
	private DataManager mManager = DataManager.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_imageview);

		final ActionBar actionBar = getSupportActionBar();
		actionBar.hide();
				
		mMusubi = mManager.getMusubi();
		if(mMusubi == null){
			finish();
			return;
		}
		
		Intent intent = getIntent();
		long hash = intent.getLongExtra("hash",0);
		if(hash == 0){
			finish();
			return;
		}
		DbObj obj = mMusubi.objForHash(hash);
		byte[] bytes = obj.getRaw();
		Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
		
		ImageView img = (ImageView) findViewById(R.id.view_image);
		img.setImageBitmap(bm);

    }

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}

}
