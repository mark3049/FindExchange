package com.markchung.findexchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnItemClickListener {
	private final static String TAG = "findexchange";
	private ListView m_listView;
	private TextView m_lastdateView;
	private ExchangeRateProvider m_dataProvider;
	private ProgressDialog myDialog = null;
	private CharSequence title = null;
	private CharSequence message = null;
	private String[] m_DollarShortNames;
	private String[] m_DollarDispayNames;
	private TimerTask m_task;
	private Timer m_timer;
	ItemListAdapter adapter;
	
	private static final String myAdID = "a150724cb805164";
	private static final String myTestDevice = "BA76119486D364D047D0C789B4F61E46";
	private static final String myTestDevice2 = "CF95DC53F383F9A836FD749F3EF439CD";
	private AdView adview;

	static class MyHandler extends Handler {
		WeakReference<MainActivity> mActivity;

		MyHandler(MainActivity activity) {
			mActivity = new WeakReference<MainActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				mActivity.get().Update(false);
				break;
			}
			super.handleMessage(msg);
		}
	};

	final Handler handler = new MyHandler(this);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		CreateAdRequest(this, (LinearLayout) findViewById(R.id.adview));
		m_listView = (ListView) this.findViewById(R.id.context);
		m_lastdateView = (TextView) findViewById(R.id.last_update);
		m_DollarShortNames = this.getResources().getStringArray(
				R.array.dollar_short);
		m_DollarDispayNames = this.getResources()
				.getStringArray(R.array.dollar);

		m_dataProvider = new ExchangeRateProvider(this, m_DollarShortNames);
		if (savedInstanceState == null) {
			SharedPreferences sets = getSharedPreferences(TAG, 0);
			isAutoUpdate = sets.getBoolean("AUTO_UPDATE", false);
			m_dataProvider.LoadFromFile(sets);
		}
		m_lastdateView.setText(m_dataProvider.getDateTimeString());
		adapter = new ItemListAdapter(this, m_dataProvider, m_DollarDispayNames);
		m_listView.setAdapter(adapter);
		m_listView.setOnItemClickListener(this);

		//
	}

	@Override
	protected void onPause() {
		if (m_timer != null) {
			m_timer.cancel();
			m_timer = null;
		}
		super.onPause();
	}

	private static final int SEC = 1000;
	private static final int MIN = 60 * SEC;
	private static final int UpdateDestion = 10 * MIN;

	private void ReScheduleTime(long minDelay) {
		GregorianCalendar d = m_dataProvider.getLastDate();
		if (m_timer != null) {
			m_timer.cancel();
		}
		m_timer = new Timer(true);
		long delay = 0;
		if (d != null) {
			Calendar now = GregorianCalendar.getInstance();
			long diff = (now.getTimeInMillis() - d.getTimeInMillis());
			Log.d(TAG, String.format("Diff is %d SEC:%f MIN", diff,
					((double) diff) / SEC, ((double) diff) / MIN));
			if (diff < UpdateDestion) {
				delay = UpdateDestion - diff;
			}
		}
		Log.d(TAG,
				String.format("Delay is %d %f", delay, ((double) delay) / SEC));
		if (delay < minDelay)
			delay = minDelay;
		m_task = new TimerTask() {
			@Override
			public void run() {
				Message msg = new Message();
				msg.what = 1;
				handler.sendMessage(msg);
			}
		};
		if (isAutoUpdate) {
			m_timer.schedule(m_task, delay, UpdateDestion);
		} else {
			m_timer.schedule(m_task, delay);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		ReScheduleTime(3 * SEC);
	}

	private boolean isAutoUpdate = true;

	@Override
	protected void onStop() {
		SharedPreferences settings = getSharedPreferences(TAG, 0);
		SharedPreferences.Editor edit = settings.edit();
		edit.putBoolean("AUTO_UPDATE", isAutoUpdate);
		m_dataProvider.SaveToFile(edit);
		edit.commit();
		super.onStop();
	}
	@Override
	protected void onDestroy() {
		if (adview != null)
			adview.destroy();
		super.onDestroy();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		if (!isAutoUpdate) {
			menu.findItem(R.id.auto_update).setTitle(R.string.auto_update);
		} else {
			menu.findItem(R.id.auto_update).setTitle(
					R.string.disable_autoupdate);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.update) {
			Update();
		} else if (id == R.id.auto_update) {
			this.isAutoUpdate = !this.isAutoUpdate;
			if (isAutoUpdate) {
				this.ReScheduleTime(1 * SEC);

			} else {
				if (m_timer != null) {
					m_timer.cancel();
					m_timer = null;
				}
			}
		}
		return true;
	}

	private String Download(String myurl) throws IOException {
		URL url = new URL(myurl);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		try {
			con.setRequestMethod("GET");
			con.setReadTimeout(10 * 1000);
			con.setConnectTimeout(15 * 1000);
			con.setDoInput(true);
			// con.setDoOutput(true);
			con.connect();
			int response = con.getResponseCode();
			Log.d(MainActivity.TAG, "The response is: " + response);
			BufferedReader in = new BufferedReader(new InputStreamReader(
					con.getInputStream(), "UTF-8"));
			String decodeString;
			StringBuffer urldata = new StringBuffer();
			while ((decodeString = in.readLine()) != null) {
				// System.out.println(decodeString);
				urldata.append(decodeString);
			}
			// System.out.println(htmlfile);
			in.close();
			return urldata.toString();
		} finally {
			con.disconnect();
		}
	}

	private class DownloadWebpageText extends AsyncTask<String, Void, ExchangeRateProvider> {

		@Override
		protected ExchangeRateProvider doInBackground(String... urls) {
			String file = "";
			try {
				file = Download(urls[0]);
				ExchangeRateProvider provider = new ExchangeRateProvider(
						MainActivity.this, m_DollarShortNames);
				provider.Parser(file);
				return provider;
			} catch (IOException e) {
				return null;

			}
		}

		@Override
		protected void onPostExecute(ExchangeRateProvider provider) {
			if (myDialog != null) {
				myDialog.dismiss();
				myDialog = null;
			}
			if (provider==null) {
				Toast.makeText(MainActivity.this,
						getString(R.string.UpdateFinish), Toast.LENGTH_SHORT)
						.show();
			} else {
				String t1 = provider.getDateTimeString();
				String t2 = m_dataProvider.getDateTimeString();
				if (!t1.equals(t2)) {
					Log.d(TAG, String.format("Time Change: %s => %s", t2, t1));
					adapter.update(provider);
					if (isAutoUpdate) {
						ReScheduleTime(UpdateDestion);
					}
					m_lastdateView.setText(t1);
					m_dataProvider = provider;
					Toast.makeText(MainActivity.this,
							getString(R.string.UpdateFinish),
							Toast.LENGTH_SHORT).show();
				} else {
					Log.d(TAG, "Date time is same");
				}
			}
			isUpdateRunning = false;
		}
	}

	private class DownloadYahooWebpageText extends
			AsyncTask<Integer, Void, YahooFind> {
		private int index;

		@Override
		protected YahooFind doInBackground(Integer... position) {
			String file = "";
			try {
				index = position[0];
				file = Download(YahooFind.getURL(m_DollarShortNames[index]));
				YahooFind find = new YahooFind();
				find.parser(file);
				return find;
			} catch (IOException e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(YahooFind find) {
			if (myDialog != null) {
				myDialog.dismiss();
				myDialog = null;
			}
			if (find == null) {
				Toast.makeText(MainActivity.this,
						getString(R.string.UpdateFinish), Toast.LENGTH_SHORT)
						.show();
			} else {
				if (find.elements.size() == 0) {
					String msg = String.format(getString(R.string.msg_no_find),
							m_DollarDispayNames[index]);
					Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT)
							.show();
					return;
				} else {
					Intent intent = new Intent();
					intent.setClass(MainActivity.this, YahooActivity.class);
					Bundle b = new Bundle();
					find.putBundle(b);
					b.putString("DOLLAR", m_DollarDispayNames[index]);
					intent.putExtras(b);
					MainActivity.this.startActivity(intent);
				}
			}

		}
	}

	private boolean isUpdateRunning = false;

	void Update() {
		Update(true);
	}

	void Update(boolean showDialog) {
		if (isUpdateRunning)
			return;

		isUpdateRunning = true;
		if (title == null)
			title = this.getString(R.string.dialog_title_wait);
		if (message == null)
			message = getString(R.string.dialog_body_update);
		if (showDialog) {
			myDialog = ProgressDialog.show(this, title, message, true, true);
		}
		new DownloadWebpageText().execute(ExchangeRateProvider.web_url);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position,
			long id) {

		if (myDialog != null)
			return;
		if (title == null)
			title = this.getString(R.string.dialog_title_wait);
		
		String message = String.format(getString(R.string.dialog_body_search),this.m_DollarDispayNames[position]);
		myDialog = ProgressDialog.show(this, title, message, true, true);
		new DownloadYahooWebpageText().execute(position);
	}
	void CreateAdRequest(Activity activity, LinearLayout view) {
		adview = new AdView(activity, AdSize.BANNER, myAdID);
		view.addView(adview);
		AdRequest adRequest = new AdRequest();
		if (BuildConfig.DEBUG) {
			adRequest.addTestDevice(AdRequest.TEST_EMULATOR);
			adRequest.addTestDevice(myTestDevice);
			adRequest.addTestDevice(myTestDevice2);
		}
		adview.loadAd(adRequest);
	}
}
