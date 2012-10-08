package com.markchung.findexchange;

import java.util.Vector;

import android.os.Bundle;

public class YahooFind {

	private final static String web_url = "http://tw.money.yahoo.com/currency_foreign2bank?currency=";
	
	class Element{
		String name;
		double buy;
		double sell;
	};
	Vector<Element> elements;
	public YahooFind(){
		elements = new Vector<Element>();
	}
	public void putBundle(Bundle b){
		int size = elements.size();
		String [] banklist = new String[size];
		double [] buy = new double[size];
		double [] sell = new double[size];
		Element e;
		for(int i=0;i<size;++i){
			e = elements.get(i);
			banklist[i]=e.name;
			buy[i]=e.buy;
			sell[i]=e.sell;
		}
		b.putStringArray("BANK", banklist);
		b.putDoubleArray("BUY", buy);
		b.putDoubleArray("SELL",sell);		
	}
	public static String getURL(String tag){
		return web_url+tag;
	}
	private String parserTable(String html){
		String Begin = "<table class=\"ratelist\">";
		int start = html.indexOf(Begin);
		if(start<0) return null;
		int end = html.indexOf("</table>",start+Begin.length());
		if(end<0) return null;
		return html.substring(start,end);
	}
	
	private String parserName(String lable){
		int start = lable.indexOf('>');
		if(start<0) return null;
		int end = lable.indexOf('<',start+1);
		if(end<0) return null;
		return lable.substring(start+1,end);
	}
	private class Tagindex{
		int begin;
		int end;
	};
	private boolean findTag(Tagindex index,String value,String tag,int begin){
		index.begin = value.indexOf("<"+tag,begin);
		if(index.begin<0) return false;
		index.end = value.indexOf("</"+tag+">",index.begin+tag.length()+1);
		if(index.end<0) return false;
		return true;
	}
	private void parserItem(String items){
		int start=0;
		int count=0;
		Tagindex index = new Tagindex();
		String []array = new String[4];
		do{
			if(!findTag(index,items,"td",start)) break;
			index.begin = items.indexOf('>',index.begin)+1;
			array[count] = items.substring(index.begin,index.end);
			start = index.end;
			count++;
		}while(count<4);
		if(count!=3) return;
		array[3] = parserName(array[0]);
		if(array[3]==null) return;
		Element e = new Element();
		e.name = array[3];
		try{
			e.sell = Double.parseDouble(array[1]);
			e.buy = Double.parseDouble(array[2]);
			elements.add(e);
		}catch(NumberFormatException err){
			
		}
	}
	public void parser(String html){
		html = parserTable(html);
		if(html == null) return;
		int start=0;
		int end;
		String item;
		do{
			start = html.indexOf("<tr>",start);
			if(start<0) break;
			end = html.indexOf("</tr>",start+4);
			if(end<0)break;
			item = html.substring(start+4,end);
			start = end+4;
			parserItem(item);
			//System.out.println(item);
		}while(true);
	}
}
