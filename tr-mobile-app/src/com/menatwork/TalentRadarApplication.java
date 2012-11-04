package com.menatwork;

import java.util.List;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gcm.GCMRegistrar;
import com.menatwork.chat.ChatSessionManager;
import com.menatwork.hunts.DefaultHunt;
import com.menatwork.hunts.HuntingCriteriaEngine;
import com.menatwork.location.GpsLocationSource;
import com.menatwork.location.LocationSourceManager;
import com.menatwork.location.NetworkLocationSource;
import com.menatwork.model.PrivacySettings;
import com.menatwork.model.User;
import com.menatwork.notification.TrNotificationManager;
import com.menatwork.preferences.ConfigurationChanges;
import com.menatwork.preferences.SharedTalentRadarConfiguration;
import com.menatwork.preferences.TalentRadarConfiguration;
import com.menatwork.preferences.TalentRadarConfigurationListener;
import com.menatwork.radar.Radar;
import com.menatwork.skills.BruteForceSearchAlgorithm;
import com.menatwork.skills.DefaultSkillButtonFactory;
import com.menatwork.skills.InMemorySkillSuggestionBox;
import com.menatwork.skills.SkillButtonFactory;
import com.menatwork.skills.SkillSuggestionBox;
import com.menatwork.utils.AndroidUtils;

public class TalentRadarApplication extends Application implements
		TalentRadarConfigurationListener {

	private static TalentRadarApplication applicationContext;

	private final Object deviceRegistrationLock = new Object();
	private String deviceRegistrationId;

	private User localUser;

	private SkillButtonFactory skillButtonFactory;
	private LocationSourceManager locationSourceManager;
	private TalentRadarConfiguration preferences;
	private SharedPrivacySettings sharedPrivacySettings;
	private TrNotificationManager notificationManager;
	private ChatSessionManager chatSessionManager;
	private HuntingCriteriaEngine huntingCriteriaEngine;
	private SkillSuggestionBox skillSuggestionBox;
	private Radar radar;

	public static TalentRadarApplication getContext() {
		return applicationContext;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		applicationContext = this;
		// setDefaultUncaughtExceptionHandler();

		skillButtonFactory = DefaultSkillButtonFactory.newInstance();
		final SharedPreferences defaultSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);

		preferences = new SharedTalentRadarConfiguration(
				defaultSharedPreferences, this, this);
		sharedPrivacySettings = new SharedPrivacySettings(this,
				defaultSharedPreferences);

		notificationManager = TrNotificationManager.newInstance();
		locationSourceManager = NaiveLocationSourceManager.newInstance();
		chatSessionManager = ChatSessionManager.newInstance(this);
		huntingCriteriaEngine = HuntingCriteriaEngine.withHunts(DefaultHunt
				.getInstance());
		skillSuggestionBox = initializeSkillSuggestionBox();

		radar = Radar.observingLocationUpdatesFrom(locationSourceManager);
		radar.addListeners(huntingCriteriaEngine);
	}

	protected InMemorySkillSuggestionBox initializeSkillSuggestionBox() {
		final InMemorySkillSuggestionBox skillSuggestionBox = new InMemorySkillSuggestionBox();
		skillSuggestionBox.setSkills("Java", "Javelin", "Jasper", "Juno",
				"Git", "Gitorious", "Subversion", "Subversive");
		skillSuggestionBox.setSearchAlgorithm(new BruteForceSearchAlgorithm());
		return skillSuggestionBox;
	}

	private void setDefaultUncaughtExceptionHandler() {
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(final Thread paramThread,
					final Throwable paramThrowable) {
				Log.e(paramThread.toString(),
						"Uncaught ex = " + paramThrowable.toString());
				final Handler handler = getMainLooperHandler();
				handler.post(new DisplayGenericErrorToastRunnable());
			}

		});
	}

	public ChatSessionManager getChatSessionManager() {
		return chatSessionManager;
	}

	public SkillButtonFactory getSkillButtonFactory() {
		return skillButtonFactory;
	}

	public TrNotificationManager getNotificationManager() {
		return notificationManager;
	}

	// ************************************************ //
	// ====== Local User ======
	// ************************************************ //

	public void loadLocalUser(final User user) {
		Log.d("TalentRadarApplication", "loadLocalUser() " + user);
		this.localUser = user;
		this.persistLocalUserId(user.getId());
		this.sharedPrivacySettings.loadFrom(user.getPrivacySettings());
	}

	private void persistLocalUserId(final String id) {
		if (id.equals(preferences.getLocalUserId())) {
			// do nothing!
		} else {
			preferences.beginNewEdition();
			preferences.setLocalUserId(id);
			preferences.commitChanges();
		}
	}

	public void logOut() {
		this.persistLocalUserId(User.EMPTY_USER_ID);
	}

	public User getLocalUser() {
		return localUser;
	}

	public String getLocalUserId() {
		return localUser.getId();
	}

	public String getLocalUserIdFromPreferences() {
		return getPreferences().getLocalUserId();
	}

	public boolean isUserLoggedIn(final String userid) {
		return this.getLocalUserIdFromPreferences().equals(userid);
	}

	// ************************************************ //
	// ====== Hunts ======
	// ************************************************ //

	public HuntingCriteriaEngine getHuntingCriteriaEngine() {
		return huntingCriteriaEngine;
	}

	// ************************************************ //
	// ====== Preferences ======
	// ************************************************ //

	public TalentRadarConfiguration getPreferences() {
		return preferences;
	}

	public PrivacySettings getPrivacySettings() {
		return sharedPrivacySettings;
	}

	@Override
	public synchronized void onConfigurationChanged(
			final ConfigurationChanges changes,
			final TalentRadarConfiguration preferences) {
		Log.d("TalentRadarApplication", "onConfigurationChanged");

		if (changes.hasLocationSourceManagerConfigurationChanged()) {
			locationSourceManager.deactivate();
			updateLocationSourceManagerConfiguration(preferences);
			locationSourceManager.activate();
		}
	}

	// ************************************************ //
	// ====== LocationSourceManager ======
	// ************************************************ //

	public LocationSourceManager getLocationSourceManager() {
		return locationSourceManager;
	}

	public void setLocationSourceManager(
			final LocationSourceManager locationSourceManager) {
		this.locationSourceManager = locationSourceManager;
	}

	public void startLocationSourceManager() {
		final LocationSourceManager locationSourceManager = new LocationSourceManager();
		locationSourceManager.addListener(radar);

		setLocationSourceManager(locationSourceManager);
		updateLocationSourceManagerConfiguration(getPreferences());
		locationSourceManager.activate();
	}

	public void stopLocationSourceManager() {
		getLocationSourceManager().deactivate();
		setLocationSourceManager(null);
	}

	private void updateLocationSourceManagerConfiguration(
			final TalentRadarConfiguration preferences) {

		final long actualizationFrequencyMilliseconds = preferences
				.getActualizationFrequencyMilliseconds();
		final long millisecondsBetweenUpdates = actualizationFrequencyMilliseconds / 2;

		// change actualization frequency
		locationSourceManager
				.setMillisecondsBetweenUpdates(actualizationFrequencyMilliseconds);

		// change location sources
		locationSourceManager.removeAllLocationSources();

		if (preferences.isNetworkLocationActivation())
			locationSourceManager.addLocationSource( //
					new NetworkLocationSource(this, millisecondsBetweenUpdates));

		if (preferences.isGpsLocationActivation())
			locationSourceManager.addLocationSource( //
					new GpsLocationSource(this, millisecondsBetweenUpdates));
	}

	// ************************************************ //
	// ====== GCM Stuff ======
	// ************************************************ //

	public String getDeviceRegistrationId() {
		if (deviceRegistrationId == null)
			if (!this.isDeviceRegistered())
				throw new RuntimeException(
						"Device not registered, deviceRegistrationId == null");
			else
				deviceRegistrationId = GCMRegistrar.getRegistrationId(this);
		return deviceRegistrationId;
	}

	public boolean isDeviceRegistered() {
		return GCMRegistrar.isRegistered(this);
	}

	/**
	 * Synchronous method for registering the local device for GCM usage. This
	 * method will wait for 10 seconds tops to get the registration answer,
	 * after that it will stop waiting and continue execution. If the
	 * registration answer comes, it will be registered but the Talent Radar
	 * server won't be notified. There is something TODO here.
	 */
	public void registerDevice() {
		try {
			synchronized (deviceRegistrationLock) {
				GCMRegistrar.register(this, GCMIntentService.SENDER_ID);
				deviceRegistrationLock.wait();
				if (deviceRegistrationId == null)
					Log.w("TalentRadarApp",
							"Timeout registering device, continuing excecution...");
				else
					Log.d("TalentRadarApp", "Registered device");
			}
		} catch (final InterruptedException e) {
		}
	}

	public void onDeviceRegistered(final String registrationId) {
		this.deviceRegistrationId = registrationId;
		synchronized (deviceRegistrationLock) {
			deviceRegistrationLock.notify();
		}
	}

	// ************************************************ //
	// ====== Other utils ======
	// ************************************************ //

	/**
	 * @see AndroidUtils#isRunningOnEmulator()
	 */
	public boolean isRunningOnEmulator() {
		return AndroidUtils.isRunningOnEmulator();
	}

	private final class DisplayGenericErrorToastRunnable implements Runnable {
		@Override
		public void run() {
			Toast.makeText(TalentRadarApplication.getContext(),
					R.string.generic_error, Toast.LENGTH_LONG).show();
		}
	}

	public Handler getMainLooperHandler() {
		final Handler handler = new Handler(Looper.getMainLooper());
		return handler;
	}

	// ************************************************ //
	// ====== NaiveLocationSourceManager ======
	// ************************************************ //

	private static final class NaiveLocationSourceManager extends
			LocationSourceManager {
		@Override
		public void activate() {
			// nothing to do here!
		}

		public static LocationSourceManager newInstance() {
			return new NaiveLocationSourceManager();
		}

		@Override
		public void deactivate() {
			// nothing to do here!
		}
	}

	// ************************************************ //
	// ====== Others ======
	// ************************************************ //

	public SkillSuggestionBox getSkillSuggestionBox() {
		return skillSuggestionBox;
	}

	public void loadSkills(final List<String> skills) {
		if (!skills.isEmpty())
			skillSuggestionBox.setSkills(skills);
	}

	public Radar getRadar() {
		return radar;
	}

}