package com.markchung.findexchange;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class ExchangeRateProvider {
	public final static String web_url = "http://rate.bot.com.tw/xrt/flcsv/0/day";
	private String m_DollarShortNames[];
	private final static String TAG = "ExchangeRateProvider";
	public final static int buy_cash = 0;
	public final static int sell_cash = 1;
	public final static int buy_spot = 2;
	public final static int sell_spot = 3;

	class ExchangeItem {
		String tag;
		double rate[];

		ExchangeItem(String name) {
			tag = name;
			rate = new double[4];
			rate[0] = rate[1] =rate[2] = rate[3] = -1;
		}
	};

	private String web_format_LastDate;
	private GregorianCalendar m_lastDate;
	ExchangeItem items[];
	java.text.DateFormat df;

	/*
	 * private String getLastDateString() { // if(m_lastDate==null) return "";
	 * // return df.format(m_lastDate); return web_format_LastDate; }
	 */
	public String getDateTimeString() {
		if (m_lastDate == null)
			return "";
		return df.format(m_lastDate.getTime());
	}

	public GregorianCalendar getLastDate() {
		return m_lastDate;
	}

	public ExchangeRateProvider(Context context, String[] shortNames) {

		m_DollarShortNames = shortNames;
		items = new ExchangeItem[m_DollarShortNames.length];
		for (int i = 0; i < m_DollarShortNames.length; ++i) {
			items[i] = new ExchangeItem(m_DollarShortNames[i]);
		}
		web_format_LastDate = "";
		df = java.text.DateFormat.getDateTimeInstance(DateFormat.LONG,
				DateFormat.SHORT);
	}

	private GregorianCalendar parserLastDate(String value) {
		if (value.length() == 0)
			return null;

		String[] list = value.split("[/: ]+");
		if (list == null || list.length != 5) {
			Log.e(TAG, "parserLastDate wrong value: " + value);
			return null;
		}

		int t[] = new int[5];
		try {
			for (int i = 0; i < 5; ++i) {
				t[i] = Integer.parseInt(list[i]);
			}
		} catch (NumberFormatException e) {
			Log.e(TAG, "parserLastDate " + e.getMessage());
			return null;
		}
		GregorianCalendar g = new GregorianCalendar();
		g.set(t[0], t[1] - 1, t[2], t[3], t[4]);
		return g;
	}

	public void LoadFromFile(SharedPreferences sets) {
		web_format_LastDate = sets.getString("LastDate", "");
		m_lastDate = parserLastDate(web_format_LastDate);
		for (int i = 0; i < m_DollarShortNames.length; ++i) {
			items[i].rate[0] = sets.getFloat(m_DollarShortNames[i] + "0", -1);
			items[i].rate[1] = sets.getFloat(m_DollarShortNames[i] + "1", -1);
			items[i].rate[2] = sets.getFloat(m_DollarShortNames[i] + "2", -1);
			items[i].rate[3] = sets.getFloat(m_DollarShortNames[i] + "3", -1);
		}
	}

	public void SaveToFile(SharedPreferences.Editor edit) {
		edit.putString("LastDate", web_format_LastDate);
		for (int i = 0; i < m_DollarShortNames.length; ++i) {
			edit.putFloat(m_DollarShortNames[i] + "0", (float) items[i].rate[0]);
			edit.putFloat(m_DollarShortNames[i] + "1", (float) items[i].rate[1]);
			edit.putFloat(m_DollarShortNames[i] + "2", (float) items[i].rate[2]);
			edit.putFloat(m_DollarShortNames[i] + "3", (float) items[i].rate[3]);
		}
	}

	private int findIndex(String tag) {
		for (int i = 0; i < m_DollarShortNames.length; ++i) {
			if (tag.equals(m_DollarShortNames[i]))
				return i;
		}
		return -1;
	}

	private String getLastCSVTime(String strtime){
		//0123 45 67 89 AB
		//2017 03 01 14 30		
		return String.format("%s/%s/%s %s:%s",
				strtime.substring(0,4),
				strtime.substring(4,6),
				strtime.substring(6,8),
				strtime.substring(8,10),
				strtime.substring(10)
				);
		
	}
	public boolean ParserCSV(ArrayList<String[]> csvfiles,String filename){
		String time = filename.substring(0, filename.lastIndexOf('.'));
		Log.d(TAG, "time = " + time);
		web_format_LastDate = getLastCSVTime(time);
		m_lastDate = parserLastDate(web_format_LastDate);
		int index;
		int [] arrayIndex = new int[]{2,12,3,13};
		for(int i=1;i<csvfiles.size();++i){
			String [] itemArray = csvfiles.get(i);
			if (itemArray.length <20) continue;
			index = findIndex(itemArray[0]);
			if(index<0) continue;
			for(int k=0;k<4;++k){				
				items[index].rate[k] = Double.parseDouble(itemArray[arrayIndex[k]]);
				if(items[index].rate[k]<=0) items[index].rate[k] = -1;
			}
			
		}
		return true;
	}
}
