package com.menatwork;

import android.app.Activity;
import android.app.Application;

import com.menatwork.model.User;
import com.menatwork.preferences.TalentRadarConfiguration;
import com.menatwork.service.Defect;
import com.menatwork.service.GetUser;
import com.menatwork.service.GetUserJobPositions;
import com.menatwork.service.GetUserSkills;
import com.menatwork.service.ResponseException;
import com.menatwork.service.response.GetUserJobPositionsResponse;
import com.menatwork.service.response.GetUserResponse;
import com.menatwork.service.response.GetUserSkillsResponse;

/**
 * Common Activity superclass for all our activities.
 * 
 * @author miguel
 * 
 */
public class TalentRadarActivity extends Activity {

	/**
	 * {@link TalentRadarApplication#isRunningOnEmulator()}
	 */
	public boolean isRunningOnEmulator() {
		return getTalentRadarApplication().isRunningOnEmulator();
	}

	/**
	 * Retrieves the {@link Application} object casted to a
	 * {@link TalentRadarApplication}
	 * 
	 * @return {@link TalentRadarApplication} for the app
	 */
	public TalentRadarApplication getTalentRadarApplication() {
		return (TalentRadarApplication) getApplication();
	}

	public TalentRadarConfiguration getPreferences() {
		return getTalentRadarApplication().getPreferences();
	}

	/* ****************************************** */
	/* ********* Business' commons ************** */
	/* ****************************************** */

	User getLocalUser() {
		return getTalentRadarApplication().getLocalUser();
	}

	User getUserById(final String userid) {
		return this.getUserById(userid, getLocalUser().getId());
	}

	User getUserById(final String userid, final String userRequestId) {
		try {
			final GetUserJobPositions getUserJobPositions = GetUserJobPositions.newInstance(this, userid);
			final GetUserJobPositionsResponse getUserJobPositionsResponse = getUserJobPositions.execute();
			
			if(!getUserJobPositionsResponse.isValid())
				throw new Defect("getUserJobPositions failed, blame Gonza");
			
			final GetUser getUser = GetUser.newInstance(this, userid,
					userRequestId);
			final GetUserResponse response = getUser.execute();
			final User user = response.getUser();

			try {
				final GetUserSkills getUserSkills = GetUserSkills.newInstance(
						this, userid);
				final GetUserSkillsResponse userSkillsResponse = getUserSkills
						.execute();
				user.setSkills(userSkillsResponse.getSkills());
			} catch (final ResponseException e) {
				// TODO Auto-generated catch block
				// won't set skills, but will return user, is it a lot of
				// damage?
				e.printStackTrace();
			}

			return user;
		} catch (final Exception e) {
			// TODO get this right please
			e.printStackTrace();
			return null;
		}
	}
}
