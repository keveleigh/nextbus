package com.doug.nextbus.activities;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import roboguice.inject.InjectView;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SlidingDrawer;
import android.widget.SlidingDrawer.OnDrawerCloseListener;
import android.widget.SlidingDrawer.OnDrawerOpenListener;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.doug.nextbus.R;
import com.doug.nextbus.RoboSherlock.RoboSherlockActivity;
import com.doug.nextbus.backend.APIController;
import com.doug.nextbus.backend.BundleKeys;
import com.doug.nextbus.backend.Data;
import com.doug.nextbus.backend.Favorite;
import com.doug.nextbus.backend.FavoritesGSON;
import com.doug.nextbus.backend.MenuFunctions;
import com.doug.nextbus.backend.RouteDataGSON.Route.Direction;
import com.doug.nextbus.backend.RouteDataGSON.Route.Stop;
import com.doug.nextbus.backend.RouteDirectionStop;
import com.doug.nextbus.custom.ArrivalsAdapter;
import com.google.gson.Gson;

/** This activity displays the predictions for a the current stop */
public class StopViewActivity extends RoboSherlockActivity implements
		OnSharedPreferenceChangeListener {

	@InjectView(R.id.firstArrival) private TextView firstArrival;
	@InjectView(R.id.secondArrival) private TextView secondArrival;
	@InjectView(R.id.thirdArrival) private TextView thirdArrival;
	@InjectView(R.id.fourthArrival) private TextView fourthArrival;
	@InjectView(R.id.stopTextView) private TextView stopTextView;
	@InjectView(R.id.drawerTextView) private TextView drawerHandleTextView;

	@InjectView(R.id.refreshButton) private ImageView refreshButton;

	@InjectView(R.id.routeviewprogressbar) private ProgressBar routeViewProgressBar;

	@InjectView(R.id.colorbar) private View colorBar;
	@InjectView(R.id.colorSeperator) private View colorSeperator;
	@InjectView(R.id.arrivalsDrawer) private SlidingDrawer arrivalsDrawer;
	@InjectView(R.id.arrivalList) private ListView arrivalList;
	@InjectView(R.id.favoriteButton) private ImageButton favoriteButton;

	@InjectView(R.id.footer_redcell) private View mFooterRedCell;
	@InjectView(R.id.footer_bluecell) private View mFooterBlueCell;
	@InjectView(R.id.footer_yellowcell) private View mFooterYellowCell;
	@InjectView(R.id.footer_greencell) private View mFooterGreenCell;

	private String mRouteTag;
	private String mDirectionTitle;
	private String mDirectionTag;
	private String mStopTitle;
	private String mStopTag;

	private static FavoritesGSON sFavorites;
	private RouteDirectionStop[] mRdsArray;

	public static Context mCtx;

	public static Intent createIntent(Context ctx, String routeTag,
			Direction direction, Stop stop) {
		Intent intent = new Intent(ctx, StopViewActivity.class);
		intent.putExtra(BundleKeys.ROUTE_TAG_KEY, routeTag);
		intent.putExtra(BundleKeys.DIRECTION_TITLE_KEY, direction.title);
		intent.putExtra(BundleKeys.DIRECTION_TAG_KEY, direction.tag);
		intent.putExtra(BundleKeys.STOP_TITLE_KEY, stop.title);
		intent.putExtra(BundleKeys.STOP_TAG_KEY, stop.tag);
		// Closes all instances of the same activity
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return intent;
	}

	public static Intent createIntent(Context ctx, Favorite favorite) {
		Intent intent = new Intent(ctx, StopViewActivity.class);
		intent.putExtra(BundleKeys.ROUTE_TAG_KEY, favorite.routeTag);
		intent.putExtra(BundleKeys.DIRECTION_TITLE_KEY, favorite.directionTitle);
		intent.putExtra(BundleKeys.DIRECTION_TAG_KEY, favorite.directionTag);
		intent.putExtra(BundleKeys.STOP_TITLE_KEY, favorite.stopTitle);
		intent.putExtra(BundleKeys.STOP_TAG_KEY, favorite.stopTag);
		// Closes all instances of the same activity
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return intent;
	}

	@Override
	public void onCreate(Bundle savedInstance) {

		super.onCreate(savedInstance);

		this.mCtx = getApplicationContext();

		setContentView(R.layout.stop_view);

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);

		// Extras
		Bundle extras = getIntent().getExtras();
		mRouteTag = extras.getString(BundleKeys.ROUTE_TAG_KEY);
		mDirectionTitle = extras.getString(BundleKeys.DIRECTION_TITLE_KEY);
		mDirectionTag = extras.getString(BundleKeys.DIRECTION_TAG_KEY);
		mStopTitle = extras.getString(BundleKeys.STOP_TITLE_KEY);
		mStopTag = extras.getString(BundleKeys.STOP_TAG_KEY);

		String[] extraStrings = new String[] { mRouteTag, mDirectionTitle,
				mDirectionTag, mStopTitle, mStopTag };
		for (String str : extraStrings) {
			if (str == null)
				finish();
		}

		stopTextView.setText(mStopTitle);
		setViewColor();

		updateArrivals();

		// Setting text views to default values
		firstArrival.setText("");
		secondArrival.setText("");
		thirdArrival.setText("");
		fourthArrival.setText("");

		Favorite favorite = new Favorite(mRouteTag, mDirectionTag,
				mDirectionTitle, mStopTag, mStopTitle);

		int starImageResource = R.drawable.ic_favorite_toadd;
		if (isFavorite(favorite)) {
			starImageResource = R.drawable.ic_favorite_toremove;
		}
		favoriteButton.setImageResource(starImageResource);

		routeViewProgressBar.setVisibility(View.INVISIBLE);

		refresh();

		setEventListeners(favorite);

		// Setting colors, are needed because of view recycling.
		mFooterBlueCell.setBackgroundColor(getResources()
				.getColor(R.color.blue));
		mFooterRedCell.setBackgroundColor(getResources().getColor(R.color.red));
		mFooterYellowCell.setBackgroundColor(getResources().getColor(
				R.color.yellow));
		mFooterGreenCell.setBackgroundColor(getResources().getColor(
				R.color.green));
	}

	@Override
	protected void onRestart() {
		try {
			refresh();
			super.onRestart();
		} catch (Exception e) {
			finish();
		}
	}

	private void setEventListeners(Favorite favorite) {

		refreshButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				refresh();
			}
		});

		favoriteButton.setOnClickListener(new CustomOnClickListener(favorite));

		arrivalsDrawer.setOnDrawerOpenListener(new OnDrawerOpenListener() {
			public void onDrawerOpened() {
				drawerHandleTextView.setText(mStopTitle);
			}
		});

		arrivalsDrawer.setOnDrawerCloseListener(new OnDrawerCloseListener() {
			public void onDrawerClosed() {
				drawerHandleTextView.setText("OTHER ROUTES");
			}
		});

		arrivalList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (mRdsArray.length == 0) {
					return; //
				}
				Intent intent = StopViewActivity
						.createIntent(getApplicationContext(),
								mRdsArray[position].route.tag,
								mRdsArray[position].direction,
								mRdsArray[position].stop);
				startActivity(intent);
			}
		});

	}

	public void updateArrivals() {
		// TODO: check if the preferences is being used
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		boolean onlyActiveRoutes = prefs.getBoolean(
				Data.SHOW_ACTIVE_ROUTES_PREF, false);

		mRdsArray = getAllRdsWithStopTitle(onlyActiveRoutes, mStopTitle,
				mRouteTag, mDirectionTag);

		if (mRdsArray.length == 0) {
			arrivalsDrawer.setVisibility(View.INVISIBLE);
			return;
		} else {
			arrivalsDrawer.setVisibility(View.VISIBLE);
		}

		arrivalList.setAdapter(new ArrivalsAdapter(getApplicationContext(),
				mRdsArray));

		arrivalList.setOnItemClickListener(null);

		/* If a cell is clicked, open the stop for that route */
		arrivalList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				RouteDirectionStop rds = mRdsArray[position];
				Intent intent = StopViewActivity.createIntent(
						getApplicationContext(), rds.route.tag, rds.direction,
						rds.stop);
				startActivity(intent);

			}
		});
	}

	/** Gets the latest prediction data */
	private void refresh() {
		if (mRouteTag == null || mDirectionTag == null || mStopTag == null) {
			return;
		}

		routeViewProgressBar.setVisibility(View.VISIBLE);
		refreshButton.setVisibility(View.INVISIBLE);
		new LoadPredictionAsyncTask(this).execute(mRouteTag, mDirectionTag,
				mStopTag);
	}

	/** Set color of text with respect to current routeTag */
	private void setViewColor() {
		int color = Data.getColorFromRouteTag(mRouteTag);
		stopTextView.setTextColor(getResources().getColor(color));
		colorBar.setBackgroundColor(getResources().getColor(color));
		colorSeperator.setBackgroundColor(getResources().getColor(color));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		int[] disabledItems = {};
		return MenuFunctions.onCreateOptionsMenu(this, menu, R.menu.stock_menu,
				disabledItems);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return MenuFunctions.onOptionsItemSelected(this, item);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

		if (key.equals(Data.SHOW_ACTIVE_ROUTES_PREF)) {
			this.updateArrivals();
		}

	}

	/** Custom OnClickListener so the favorite class could be passed to it. */
	private class CustomOnClickListener implements OnClickListener {

		private Favorite favorite;

		public CustomOnClickListener(Favorite favorite) {
			this.favorite = favorite;
		}

		@Override
		public void onClick(View v) {
			boolean ret = toggleFavorite(favorite);

			if (ret) {
				((ImageButton) v)
						.setImageResource(R.drawable.ic_favorite_toremove);
				Toast.makeText(getApplicationContext(), "Added to Favorites",
						Toast.LENGTH_SHORT).show();
			} else {
				((ImageButton) v)
						.setImageResource(R.drawable.ic_favorite_toadd);
				Toast.makeText(getApplicationContext(),
						"Removed from Favorites", Toast.LENGTH_SHORT).show();
			}
		}

	}

	public boolean toggleFavorite(Favorite favorite) {
		if (sFavorites == null)
			loadFavoritesData();
		boolean ret = sFavorites.toggleFavorite(favorite);
		saveFavoriteData();
		return ret;

	}

	private void loadFavoritesData() {
		try {
			FileInputStream fis = getApplicationContext().openFileInput(
					"favorites.txt");
			Reader reader = new InputStreamReader(fis);
			sFavorites = new Gson().fromJson(reader, FavoritesGSON.class);
		} catch (Exception e) {
			System.out.println(e);
			sFavorites = new FavoritesGSON();
		}
	}

	private void saveFavoriteData() {
		try {
			sFavorites.sort();
			String toSave = new Gson().toJson(sFavorites);
			FileOutputStream fos = getApplicationContext().openFileOutput(
					"favorites.txt", Context.MODE_PRIVATE);
			fos.write(toSave.getBytes());
			fos.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean isFavorite(Favorite favorite) {
		if (sFavorites == null)
			loadFavoritesData();
		return sFavorites.contains(favorite);
	}

	/**
	 * Finds all route/direction/stops with that share the same stop title.
	 * Excludes Rds with given routeTag and directionTag
	 */
	public static RouteDirectionStop[] getAllRdsWithStopTitle(
			boolean onlyActiveRoutes, String stopTitle, String routeTag,
			String directionTag) {

		ArrayList<RouteDirectionStop> rdsList = new ArrayList<RouteDirectionStop>();

		// Get the default list of routes and overwrite if active routes is true
		String[] currentRoutes = Data.DEFAULT_ALL_ROUTES;
		if (onlyActiveRoutes)
			currentRoutes = APIController.getActiveRoutesList();

		HashMap<String, HashSet<RouteDirectionStop>> sSharedStops = Data
				.getSharedStops();

		/*
		 * Iterate through route and direction if the rds matches the given
		 * route/direction or is not in the activeRoutes then continue
		 */
		Iterator<RouteDirectionStop> iter = sSharedStops.get(stopTitle)
				.iterator();
		while (iter.hasNext()) {
			RouteDirectionStop rds = iter.next();
			if ((rds.route.tag.equals(routeTag) && rds.direction.tag
					.equals(directionTag))
					|| !Data.isInArray(currentRoutes, rds.route.tag))
				continue;
			rdsList.add(rds);
		}

		// Sorting to put the blues, reds, etc together
		Collections.sort(rdsList);
		RouteDirectionStop[] rdsArray = {};
		return rdsList.toArray(rdsArray);
	}

	/** Load prediction data asynchronously. */
	private class LoadPredictionAsyncTask extends
			AsyncTask<String, Void, ArrayList<String>> {
		Context ctx;

		public LoadPredictionAsyncTask(Context ctx) {
			this.ctx = ctx;
		}

		@Override
		protected ArrayList<String> doInBackground(String... values) {
			// Gets the data
			ArrayList<String> predictions = APIController.getPrediction(
					values[0], values[1], values[2]);
			return predictions;
		}

		/* Update the UI */
		@Override
		public void onPostExecute(ArrayList<String> predictions) {
			routeViewProgressBar.setVisibility(View.INVISIBLE);
			refreshButton.setVisibility(View.VISIBLE);

			try {
				if (predictions.size() == 1 && predictions.get(0).equals("-1")) {
					Toast.makeText(ctx, "Error, Server Down?",
							Toast.LENGTH_LONG).show();
					predictions.clear();
				}

				if (predictions.size() == 0) {
					firstArrival.setText("--");
					secondArrival.setText("");
					thirdArrival.setText("");
					fourthArrival.setText("");

				} else {
					firstArrival.setText(predictions.get(0));
					if (predictions.size() > 1)
						secondArrival.setText(predictions.get(1));
					else
						secondArrival.setText("");
					if (predictions.size() > 2)
						thirdArrival.setText(predictions.get(2));
					else
						thirdArrival.setText("");
					if (predictions.size() > 3)
						fourthArrival.setText(predictions.get(3));
					else
						fourthArrival.setText("");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
