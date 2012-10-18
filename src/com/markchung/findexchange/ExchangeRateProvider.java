package com.markchung.findexchange;

import java.text.DateFormat;
import java.util.GregorianCalendar;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class ExchangeRateProvider {
	public final static String web_url = "http://rate.bot.com.tw/Pages/Static/UIP003.zh-TW.htm";
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

	private void parserItem(String line) {
		int start = line.indexOf('(');
		int end = line.indexOf(')');
		String tmp;
		int index;
		tmp = line.substring(start + 1, end);
		index = findIndex(tmp);
		if (index < 0)
			return;
		int count = 0;
		do {
			start = line.indexOf("decimal\">", end + 1);
			if (start < 0)
				break;
			end = line.indexOf("</td>", start + 1);
			if (end < 0)
				break;
			tmp = line.substring(start + 9, end);
			if (!tmp.equals("-")) {
				try {
					items[index].rate[count] = Double.parseDouble(tmp);
					// items[index].rate[count] = Math.random();
				} catch (NumberFormatException e) {

				}
			} else {
				items[index].rate[count] = -1;
			}
			++count;
			if (count >= 4)
				break;
		} while (true);
	}

	/*
	 * public void Dump() { for (int i = 0; i < items.length; ++i) {
	 * System.out.print(items[i].tag); System.out.print(" "); for (int j = 0; j
	 * < 2; ++j) { if (items[i].rate[j] > 0) {
	 * System.out.print(items[i].rate[j]); } else { System.out.print("-"); }
	 * System.out.print("\t"); } System.out.println(); } if (LastDate != null) {
	 * System.out.println(LastDate); } }
	 */
	private final static String time_tag = "±¾µP®É¶¡¡G&nbsp;";

	public boolean Parser(String htmlfile) {
		int start, end;
		start = end = 0;
		String tmp;
		start = htmlfile.indexOf(time_tag);
		if (start > 0) {
			end = htmlfile.indexOf("</td>", start + time_tag.length());
			if (end > 0) {
				tmp = htmlfile.substring(start + time_tag.length(), end);
				web_format_LastDate = tmp;
				m_lastDate = parserLastDate(web_format_LastDate);
			}
		}
		do {
			start = htmlfile.indexOf("<tr", end + 1);
			if (start < 0)
				break;
			end = htmlfile.indexOf("</tr>", start + 1);
			if (end < 0)
				break;
			tmp = htmlfile.substring(start, end);
			if (tmp.indexOf("class=\"decimal\"") < 0)
				continue;
			parserItem(tmp);
		} while (true);
		return m_lastDate != null;
	}
}
