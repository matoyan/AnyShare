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
import mobisocial.bento.anyshare.util.BitmapHelper;
import mobisocial.bento.anyshare.util.ImageCache;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class FeedItemListItemAdapter extends ArrayAdapter<FeedItemListItem> {
	private static final String TAG = "AlbumListItemAdapter";
	private static final int MAX_IMG_WIDTH = 48;
	private static final int MAX_IMG_HEIGHT = 48;
	private static final int DUMMY_IMG_WIDTH = 48;
	private static final int DUMMY_IMG_HEIGHT = 48;

	private DataManager mManager = DataManager.getInstance();
	private LayoutInflater mInflater;
	private ListView mListView = null;
	private Context mContext = null;

	public FeedItemListItemAdapter(Context context, int resourceId,
			ListView listView) {
		super(context, resourceId);
		mContext = context;
		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mListView = listView;
		Log.d(TAG, "FeedAlbumListItemAdapter mListView=" + mListView);
	}

	@Override
	public int getCount() {
		int count = mManager.getFeedItemListCount();
		return count;
	}

	@Override
	public FeedItemListItem getItem(int position) {
		return mManager.getFeedItemListItem(position,mContext);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final ViewHolder holder;
		if (convertView == null) {
			// Create view from Layout File
			convertView = mInflater.inflate(R.layout.listitem_feeditemlist, null);
			holder = new ViewHolder();
			holder.imageView = (ImageView) convertView.findViewById(R.id.item_image);
			holder.title = (TextView) convertView.findViewById(R.id.item_title);
			holder.desc = (TextView) convertView.findViewById(R.id.item_desc);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		// Fetch item
		FeedItemListItem item = getItem(position);

		// Set Title
		holder.title.setText(item.title);
		holder.title.setTextColor(convertView.getResources().getColor(R.color.body_text_1));
		holder.desc.setText(item.desc);
		holder.desc.setTextColor(convertView.getResources().getColor(R.color.body_text_2));

		// Set Image
		try {
			holder.imageView.setTag("Item"+position);
			Bitmap bitmap = ImageCache.getImage(String.valueOf(position));
			if (bitmap == null) {
				holder.imageView.setImageBitmap(
						BitmapHelper.getDummyBitmap(DUMMY_IMG_WIDTH, DUMMY_IMG_HEIGHT,Color.WHITE));
				ImageGetTask task = new ImageGetTask(holder.imageView);
				task.execute(position);
			} else {
				holder.imageView.setImageBitmap(bitmap);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return convertView;
	}
	

	static class ViewHolder {
		TextView title;
		TextView desc;
		ImageView imageView;
	}

	class ImageGetTask extends AsyncTask<Integer, Void, Bitmap> {
		private ImageView image;
		private String tag;

		public ImageGetTask(ImageView imageView) {
			image = imageView;
			tag = image.getTag().toString();
		}

		@Override
		protected Bitmap doInBackground(Integer... params) {
			synchronized (mContext) {
				int position = params[0];
				try {
					Bitmap bitmap = mManager.getThumbBitmap(position, mContext);
					ImageCache.setImage(String.valueOf(params[0]), bitmap);
					return bitmap;
				} catch (Exception e) {
					return null;
				}
			}
		}

		@Override
		protected void onPostExecute(Bitmap result) {
//			Log.d(TAG, "tags: "+tag+", "+image.getTag()+";");
			if (tag.equals(image.getTag())) {
				if (result != null) {
					image.setImageBitmap(result);
				} else {
					image.setImageBitmap(BitmapHelper.getDummyBitmap(
							DUMMY_IMG_WIDTH, DUMMY_IMG_HEIGHT, Color.WHITE));
				}
				image.setVisibility(View.VISIBLE);
			}else{
				Log.d(TAG, "tag maching failed???");
			}
		}
	}
}
