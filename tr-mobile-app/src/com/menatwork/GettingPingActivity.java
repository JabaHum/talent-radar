package com.menatwork;

import java.io.IOException;

import org.json.JSONException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.menatwork.service.GetPing;
import com.menatwork.service.response.GetPingResponse;

public class GettingPingActivity extends Activity {

	public static final String EXTRAS_PING_ID = "pingId";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		doYourThing();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		this.setIntent(intent);
		doYourThing();
	}

	private void doYourThing() {
		Bundle extras = this.getIntent().getExtras();
		if (extras == null)
			throw new RuntimeException(
					"This action MUST be called with extras (so as to pass along the pingId)");
		String pingId = extras.getString(EXTRAS_PING_ID);
		if (pingId == null)
			throw new RuntimeException(
					"This action MUST be called with extras GettingPingActivity.EXTRAS_PING_ID");
		String localId = ((TalentRadarApplication) getApplication())
				.getLocalUser().getId();
		new GetPingTask().execute(pingId, localId);
	}

	private class GetPingTask extends AsyncTask<String, Void, GetPingResponse> {

		private ProgressDialog progressDialog;

		@Override
		protected void onPostExecute(GetPingResponse result) {
			progressDialog.dismiss();
			if (result != null && result.isSuccessful()) {
				Intent intent = new Intent(GettingPingActivity.this,
						PingAlertActivity.class);
				intent.putExtra(PingAlertActivity.EXTRA_USER_ID,
						result.getUserId());
				intent.putExtra(PingAlertActivity.EXTRA_MESSAGE,
						result.getMessage());
				intent.putExtra(PingAlertActivity.EXTRA_USER_FULLNAME,
						result.getUserFullName());
				startActivity(intent);
			} else
				Log.e("GetPingTask", "Result was either null or unsuccessful");
		}

		@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(GettingPingActivity.this, "",
					getString(R.string.generic_wait));
		}

		@Override
		protected GetPingResponse doInBackground(String... arg0) {
			try {
				GetPing getPing = GetPing.newInstance(GettingPingActivity.this,
						arg0[0], arg0[1]);
				return getPing.execute();
			} catch (JSONException e) {
				Log.e("GetPingTask", "Error retrieving ping to display alert",
						e);
			} catch (IOException e) {
				Log.e("GetPingTask", "Error retrieving ping to display alert",
						e);
			}
			return null;
		}
	}
}
