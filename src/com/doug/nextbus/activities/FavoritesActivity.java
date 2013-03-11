package com.doug.nextbus.activities;

import roboguice.inject.InjectView;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.doug.nextbus.R;
import com.doug.nextbus.RoboSherlock.RoboSherlockActivity;
import com.doug.nextbus.backend.Data;
import com.doug.nextbus.backend.Favorite;
import com.doug.nextbus.custom.FavoritesAdapter;

public class FavoritesActivity extends RoboSherlockActivity {

	@InjectView(R.id.stopListView) private ListView stopListView;
	@InjectView(R.id.directionTextView) private TextView directionTextView;
	@InjectView(R.id.colorbar) private View colorBar;

	FavoritesAdapter mFavoritesAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stop_list);

		mFavoritesAdapter = new FavoritesAdapter(getApplicationContext());

		/*
		 * Making the colorbar smaller height, cleaner look. Maybe make new
		 * layout file?
		 */
		colorBar.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT, 3));

		// Just getting rid of these
		((LinearLayout) directionTextView.getParent())
				.removeView(directionTextView);
		((LinearLayout) colorBar.getParent()).removeView(colorBar);

		stopListView.setAdapter(mFavoritesAdapter);
		setEventListeners();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// In case any preferences were changed.
		mFavoritesAdapter.notifyDataSetChanged();
	}

	private void setEventListeners() {
		stopListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Favorite favorite = Data.getFavorite(position);
				startActivity(StopViewActivity.createIntent(
						getApplicationContext(), favorite));
			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportMenuInflater().inflate(R.menu.stock_menu, menu);
		menu.findItem(R.id.favoritesitem).setVisible(false);
		menu.findItem(R.id.mapsitem).setVisible(false);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

}