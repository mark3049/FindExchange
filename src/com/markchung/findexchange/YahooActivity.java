package com.markchung.findexchange;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

public class YahooActivity extends Activity {
	private ListView m_view;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_yahoo);
		m_view = (ListView) this.findViewById(R.id.context);
		Bundle extras = this.getIntent().getExtras();
		String [] bank = extras.getStringArray("BANK");
		double [] buy = extras.getDoubleArray("BUY");
		double [] sell = extras.getDoubleArray("SELL");
		String title = String.format(this.getString(R.string.title_yahoo),extras.getString("DOLLAR"));
		this.setTitle(title);
		m_view.setAdapter(new BankListAdapter(this,bank,buy,sell));
	}
}
