package com.menatwork;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.menatwork.hunts.DefaultHunt;
import com.menatwork.hunts.Hunt;
import com.menatwork.hunts.HuntingCriteriaEngine;
import com.menatwork.hunts.HuntingCriteriaListener;
import com.menatwork.hunts.SimpleSkillHunt;
import com.menatwork.hunts.TalentRadarDao;
import com.menatwork.model.User;
import com.menatwork.notification.TrNotification;

public class HuntsActivity extends ListActivity implements HuntingCriteriaListener {

	private static final String KEY_TITLE = "header";
	private static final String KEY_QUANTITY = "quantity";
	private static final String KEY_DESCRIPTION = "description";
	private static final String KEY_HUNT = "hunt";
	private static final String KEY_ICON = "icon";

	private static final String[] DATA_KEYS = new String[] { //
	/*    */KEY_TITLE, //
			KEY_QUANTITY, //
			KEY_DESCRIPTION, //
			KEY_ICON };

	private static final int[] DATA_VIEW_IDS = new int[] { //
	/*    */R.id.hunt_header, //
			R.id.hunt_quantity, //
			R.id.hunt_description, //
			R.id.hunt_row_icon_hunt };

	// ************************************************ //
	// ====== Instance members ======
	// ************************************************ //

	private final List<Map<String, ?>> huntMaps = new ArrayList<Map<String, ?>>();
	private Handler mainLooperHandler;

	private ImageButton addNewHuntButton;
	private ViewGroup addNewHuntViewGroup;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.hunts);
		findViewElements();
		setupButtons();
		postCreate();
	}

	private void postCreate() {
		initializeListAdapter();
		initializeListViewEvents();
		initializeHuntEvents();
	}

	private void findViewElements() {
		addNewHuntButton = (ImageButton) findViewById(R.id.hunts_button_add_necessary_skill);
		addNewHuntViewGroup = (ViewGroup) findViewById(R.id.hunts_layout_add_new_hunt);
	}

	private void setupButtons() {
		final OnClickListener listener = new OnClickListener() {
			@Override
			public void onClick(final View v) {
				startNewHuntActivity();
			}
		};
		addNewHuntButton.setOnClickListener(listener);
		addNewHuntViewGroup.setOnClickListener(listener);
	}

	private void initializeHuntEvents() {
		getHuntingCriteriaEngine().addListener(this);
		DefaultHunt.getInstance().addListener(this);
	}

	private void startNewHuntActivity() {
		startActivity(new Intent(this, NewHuntActivity.class));
	}

	@Override
	protected void onResume() {
		super.onResume();
		initializeAlreadyExistentHunts();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.hunt_menu, menu);
		super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.new_hunt:
			startNewHuntActivity();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	// ************************************************ //
	// ====== ListAdapter Stuff ======
	// ************************************************ //

	private void initializeListViewEvents() {
		final ListView listView = getListView();

		registerForContextMenu(listView);
	}

	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		final Hunt hunt = huntAt((int) info.id);

		menu.setHeaderTitle(titleForHunt(hunt));

		getMenuInflater().inflate(contextMenuXmlForHunt(hunt), menu);
	}

	private int contextMenuXmlForHunt(final Hunt hunt) {
		return isDefaultHunt(hunt) ? R.menu.default_hunt_context_menu : R.menu.hunt_context_menu;
	}

	private String titleForHunt(final Hunt hunt) {
		return isDefaultHunt(hunt) ? hunt.getTitle() : getString(R.string.hunt_title_prefix)
				+ hunt.getTitle();
	}

	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.edit_hunt:
			editHunt(huntAt((int) info.id));
			return true;
		case R.id.remove_hunt:
			removeHuntFromUi(huntAt((int) info.id));
			return true;
		case R.id.empty_hunt:
			emptyHunt(huntAt((int) info.id));
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	private void emptyHunt(final Hunt hunt) {
		hunt.emptyUsers();
		updateHuntMapFor(hunt);

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				notifyDataSetChanged();
				showHuntEmptiedSuccessfullyMessage(hunt);
			}

		});
	}

	protected void showHuntEmptiedSuccessfullyMessage(final Hunt hunt) {
		Toast.makeText(this, messageWhenHuntEmptied(hunt), Toast.LENGTH_SHORT).show();
	}

	private String messageWhenHuntEmptied(final Hunt hunt) {
		return isDefaultHunt(hunt) //
		/*    */? getString(R.string.hunts_empty_default_hunt_successful)
				: String.format(getString(R.string.hunts_empty_hunt_successful), hunt.getTitle());
	}

	private void updateHuntMapFor(final Hunt hunt) {
		final Map<String, ?> map = findHuntMapFor(hunt);
		huntMaps.set(huntMaps.indexOf(map), hunt2HuntMap(hunt));
	}

	private void removeHuntFromUi(final Hunt hunt) {
		if (isDefaultHunt(hunt))
			Toast.makeText(this, R.string.hunts_default_hunt_wont_be_removed, Toast.LENGTH_SHORT).show();
		else if (isSimpleSkillHunt(hunt)) {
			final SimpleSkillHunt simpleSkillHunt = (SimpleSkillHunt) hunt;

			getHuntingCriteriaEngine().removeHunt(simpleSkillHunt);

			TalentRadarDao.withContext(this).deleteHunt(simpleSkillHunt);

			// delete it from the ui
			removeHunt(simpleSkillHunt);
			notifyDataSetChanged();
		} else
			throw new UnsupportedOperationException("this kind of hunt is not supported for removal");
	}

	private void removeHunt(final Hunt simpleSkillHunt) {
		huntMaps.remove(findHuntMapFor(simpleSkillHunt));
	}

	private Map<String, ?> findHuntMapFor(final Hunt hunt) {
		for (final Map<String, ?> huntMap : huntMaps)
			if (hunt.equals(huntMap.get(KEY_HUNT)))
				return huntMap;

		throw new NoSuchElementException("there's no hunt map with hunt = " + hunt);
	}

	private void editHunt(final Hunt hunt) {
		if (isDefaultHunt(hunt))
			Toast.makeText(this, R.string.hunts_default_hunt_wont_be_edited, Toast.LENGTH_SHORT).show();
		else if (isSimpleSkillHunt(hunt)) {
			final Intent intent = new Intent(this, NewHuntActivity.class);
			intent.putExtra(NewHuntActivity.EXTRAS_HUNT_ID, hunt.getId());
			startActivity(intent);
		} else
			throw new UnsupportedOperationException("this kind of hunt is not supported for removal");
	}

	private void initializeListAdapter() {
		final SimpleAdapter adapter = new SimpleAdapter(this, //
				huntMaps, //
				R.layout.hunt_row_view, //
				DATA_KEYS, //
				DATA_VIEW_IDS);

		setListAdapter(adapter);
	}

	@Override
	protected void onListItemClick(final ListView l, final View v, final int position, final long id) {
		super.onListItemClick(l, v, position, id);

		final Hunt hunt = huntAt(position);

		if (hunt.getUsersQuantity() <= 0)
			if (isDefaultHunt(hunt))
				Toast.makeText(this, R.string.hunts_default_hunt_empty, Toast.LENGTH_SHORT).show();
			else
				Toast.makeText(this, R.string.hunts_empty, Toast.LENGTH_SHORT).show();
		else {
			final Intent intent = new Intent(this, HuntMiniProfilesActivity.class);
			intent.putExtra(HuntMiniProfilesActivity.EXTRAS_HUNT_ID, hunt.getId());
			startActivity(intent);
		}
	}

	@SuppressWarnings("unchecked")
	protected Hunt huntAt(final int position) {
		final Map<String, Object> huntMap = (Map<String, Object>) getListAdapter().getItem(position);
		final Hunt hunt = (Hunt) huntMap.get(KEY_HUNT);
		return hunt;
	}

	public void notifyDataSetChanged() {
		getListAdapter().notifyDataSetChanged();
	}

	@Override
	public BaseAdapter getListAdapter() {
		return (BaseAdapter) super.getListAdapter();
	}

	// ************************************************ //
	// ====== TrNotificationListener ======
	// ************************************************ //

	/**
	 * Adds every notification that has been already registered by the
	 * notification manager and shows it in the list.
	 */
	private void initializeAlreadyExistentHunts() {
		final Collection<Hunt> hunts = getHuntingCriteriaEngine().getHunts();

		removeAllHuntsFromUi();
		addHuntsAndNotify(hunts);
	}

	private void removeAllHuntsFromUi() {
		huntMaps.clear();
	}

	public void addHunt(final Hunt hunt) {
		final Map<String, Object> huntMap = hunt2HuntMap(hunt);
		huntMaps.add(huntMap);
	}

	protected void addHunts(final Collection<? extends Hunt> hunts) {
		for (final Hunt hunt : hunts)
			addHunt(hunt);
	}

	/**
	 * Adds a Hunt to the list of notifications shown in the HuntsActivity,
	 * mapping it to the correct representation.
	 * 
	 * This method ALSO notifies the ListAdapter for the list shown to be
	 * refreshed in screen.
	 * 
	 * @param hunts
	 */
	protected void addHuntsAndNotify(final Collection<? extends Hunt> hunts) {
		mainLooperHandler = new Handler(this.getMainLooper());
		mainLooperHandler.post(new Runnable() {
			@Override
			public void run() {
				addHunts(hunts);
				notifyDataSetChanged();
			}
		});
	}

	protected void addHuntsAndNotify(final Hunt... hunts) {
		addHuntsAndNotify(Arrays.asList(hunts));
	}

	/**
	 * Maps a {@link TrNotification} to a map containing every value that will
	 * be showed in the activity.
	 * 
	 * @param hunt
	 * @return
	 */
	protected Map<String, Object> hunt2HuntMap(final Hunt hunt) {
		final Map<String, Object> huntMap = new HashMap<String, Object>();
		huntMap.put(KEY_TITLE, hunt.getTitle());
		huntMap.put(KEY_QUANTITY, formatQuantity(hunt.getUsersQuantity()));
		huntMap.put(KEY_DESCRIPTION, hunt.getDescription());
		huntMap.put(KEY_ICON, hunt.getIcon());

		huntMap.put(KEY_HUNT, hunt);
		return huntMap;
	}

	protected String formatQuantity(final int quantity) {
		return quantity > 0 ? "(" + quantity + ")" : "";
	}

	private boolean isDefaultHunt(final Hunt hunt) {
		return hunt instanceof DefaultHunt;
	}

	private boolean isSimpleSkillHunt(final Hunt hunt) {
		return hunt instanceof SimpleSkillHunt;
	}

	// ****************************************************** //
	// ====== HuntinCriteriaListener implementation ======
	// ****************************************************** //

	@Override
	public void onUsersAddedToHunt(final Hunt hunts, final List<User> newUsers) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				initializeAlreadyExistentHunts();
			}
		});
	}

	// ************************************************ //
	// ====== TalentRadarCommons ======
	// ************************************************ //

	private TalentRadarApplication getTalentRadarApplication() {
		return (TalentRadarApplication) getApplication();
	}

	private HuntingCriteriaEngine getHuntingCriteriaEngine() {
		return getTalentRadarApplication().getHuntingCriteriaEngine();
	}
}
