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

import org.json.JSONException;

import mobisocial.bento.anyshare.io.DataManager;
import mobisocial.bento.anyshare.util.ImageCache;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;
import mobisocial.bento.anyshare.R;
import mobisocial.socialkit.musubi.DbObj;

public class FeedItemListFragment extends ListFragment {
    private static final String TAG = "FeedAlbumListFragment";
    
	private static DataManager mManager = DataManager.getInstance();
	private FeedItemListItemAdapter mListAdapter = null;
	private ListView mListView = null;

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	
        View root = inflater.inflate(R.layout.fragment_feeditemlist, container, false);
        
        // ListView
		mListView = (ListView) root.findViewById(android.R.id.list);
        mListView.setFastScrollEnabled(true);
        
        registerForContextMenu(mListView);
        return root;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
    	super.onActivityCreated(savedInstanceState);
    	
        // Create adapter
        mListAdapter = new FeedItemListItemAdapter(
        		getActivity(), 
        		android.R.layout.simple_list_item_1,
        		mListView);
        setListAdapter(mListAdapter);
        
    }

    @Override
	public void onListItemClick(ListView l, View v, int position, long id) {
    	
    	DbObj obj = mManager.getFeedItemObj(position);
    	if(obj == null){
    		return;
    	}
    	mManager.setContextObj(obj);
    	if(obj.getType().equals(DataManager.TYPE_APP_STATE)){
			Intent intent = new Intent(getActivity(), ViewActivity.class);
			startActivityForResult(intent, HomeActivity.REQUEST_VIEW);
    	}else if(obj.getType().equals(DataManager.TYPE_PICTURE)){
			Intent intent = new Intent(getActivity(), ImageViewActivity.class);
			intent.putExtra("hash", obj.getHash());
			startActivityForResult(intent, HomeActivity.REQUEST_VIEW);
    	}else if(obj.getType().equals(DataManager.TYPE_LINK)){
			Intent intent = new Intent(Intent.ACTION_VIEW);
			try {
				intent.setData(Uri.parse(obj.getJson().getString("uri")));
				startActivityForResult(intent, HomeActivity.REQUEST_VIEW);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	// super.onListItemClick(l, v, position, id);

    }
    
    public void refleshView(){
    	ImageCache.clearCache();
		mListAdapter.notifyDataSetChanged();
    }
}
