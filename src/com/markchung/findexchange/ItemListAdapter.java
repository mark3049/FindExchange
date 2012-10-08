package com.markchung.findexchange;

import java.text.NumberFormat;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
class BankListAdapter extends BaseAdapter {
	private String m_name[];
	private double m_buy[];
	private double m_sell[];
	private LayoutInflater minflater;
	NumberFormat nr;
	private int findMin(double []value){
		double min = value[0];
		int index = 0;
		for(int i=1;i<value.length;++i){
			if(value[i]<min){
				min = value[i];
				index = i;
			}
		}
		return index;
	}
	private int findMax(double []value){
		double max = value[0];
		int index = 0;
		for(int i=1;i<value.length;++i){
			if(value[i]>max){
				max = value[i];
				index = i;
			}
		}
		return index;
	}
	int max_index;
	int min_index;
	int red,normal;
	BankListAdapter(Context context,String []name,double []buy,double []sell){
		minflater = LayoutInflater.from(context);
		red = context.getResources().getColor(R.color.red);
		normal = context.getResources().getColor(R.color.normal);
		m_name = name;
		m_buy = buy;
		m_sell = sell;
		max_index = findMax(buy);
		min_index = findMin(sell);
		nr = NumberFormat.getNumberInstance();
		nr.setMinimumFractionDigits(5);
	}
	@Override
	public int getCount() {
		return m_name.length;
	}

	@Override
	public Object getItem(int arg0) {
		return arg0;
	}

	@Override
	public long getItemId(int arg0) {
		return arg0;
	}
	static class ViewHolder {
		TextView m_nameView;
		TextView m_buyView;
		TextView m_sellView;
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = this.minflater.inflate(R.layout.item_yahoo, null);
			holder = new ViewHolder();
			holder.m_nameView = (TextView) convertView.findViewById(R.id.bank_name);
			holder.m_buyView = (TextView) convertView.findViewById(R.id.rate_buy);
			holder.m_sellView = (TextView) convertView.findViewById(R.id.rate_sell);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		holder.m_nameView.setText(m_name[position]);
		holder.m_buyView.setText(nr.format(m_buy[position]));
		holder.m_sellView.setText(nr.format(m_sell[position]));
		if(position==max_index){
			holder.m_buyView.setTextColor(red);
		}else{
			holder.m_buyView.setTextColor(normal);
		}
		if(position==min_index){
			holder.m_sellView.setTextColor(red);
		}else{
			holder.m_sellView.setTextColor(normal);
		}
		if((position&0x01)==0){
			convertView.setBackgroundResource(R.color.odd);
		}else{
			convertView.setBackgroundResource(R.color.even);
		}
		return convertView;
	}
	
};
public class ItemListAdapter extends BaseAdapter {
	static class ViewHolder {
		ImageView icon;
		TextView name;
		TextView rate;
		TextView rate1;
		//TextView rate2;
		//TextView rate3;
	}

	private LayoutInflater minflater;
	private Resources res;
	private ExchangeRateProvider m_provider;
	private final static int TOTAL_ITEMS = 19;

	private final static int icon_res[] = { R.drawable.usd, R.drawable.hkd,
			R.drawable.gbp, R.drawable.aud, R.drawable.cad, R.drawable.sgd,
			R.drawable.chf, R.drawable.jpy, R.drawable.zar, R.drawable.sek,
			R.drawable.nzd, R.drawable.thb, R.drawable.php, R.drawable.idr,
			R.drawable.eur, R.drawable.krw, R.drawable.vnd, R.drawable.myr,
			R.drawable.cny };
	private String[] m_dollorName = null;// = new String[TOTAL_ITEMS];
	private static Bitmap[] icon = new Bitmap[TOTAL_ITEMS];

	NumberFormat nr;

	public ItemListAdapter(Context context, ExchangeRateProvider provider,String []dollars) {
		res = context.getResources();
		m_dollorName = dollars;
		m_provider = provider;
		
		nr = NumberFormat.getNumberInstance();
		nr.setMinimumFractionDigits(6);
		minflater = LayoutInflater.from(context);
	}
	public void update(ExchangeRateProvider bank){
		m_provider = bank;		
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return TOTAL_ITEMS;
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	String getText(int index,int pos){
		double value =m_provider.items[index].rate[pos];  
		if(value<0){
			return "-";
		}else{
			return nr.format(value);
		}
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = this.minflater.inflate(R.layout.item, null);
			holder = new ViewHolder();
			holder.icon = (ImageView) convertView.findViewById(R.id.item_icon);
			holder.name = (TextView) convertView.findViewById(R.id.name);
			holder.rate = (TextView) convertView.findViewById(R.id.rate);
			holder.rate1 = (TextView) convertView.findViewById(R.id.rate1);
			//holder.rate2 = (TextView) convertView.findViewById(R.id.rate2);
			//holder.rate3 = (TextView) convertView.findViewById(R.id.rate3);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		if (icon[position] == null) {
			icon[position] = BitmapFactory.decodeResource(res,
					icon_res[position]);
		}
		holder.icon.setImageBitmap(icon[position]);
		holder.name.setText(m_dollorName[position]);
		holder.rate.setText(getText(position,ExchangeRateProvider.buy_cash));
		holder.rate1.setText(getText(position,ExchangeRateProvider.sell_cash));
		//holder.rate2.setText(getText(position,TaiwanBank.sell_spot));
		//holder.rate3.setText(getText(position,TaiwanBank.sell_spot));
		if((position&0x01)==0){
			convertView.setBackgroundResource(R.color.odd);
		}else{
			convertView.setBackgroundResource(R.color.even);
		}
		return convertView;
	}

}
