package com.irccloud.android;

import org.xml.sax.XMLReader;

import android.graphics.Color;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;

public class ColorFormatter {
	private static final String[] COLOR_MAP = {
		"FFFFFF", //white
		"000000", //black
		"000080", //navy
		"008000", //green
		"FF0000", //red
		"800000", //maroon
		"800080", //purple
		"FFA500", //orange
		"FFFF00", //yellow
		"00FF00", //lime
		"008080", //teal
		"00FFFF", //cyan
		"0000FF", //blue
		"FF00FF", //magenta
		"808080", //grey
		"C0C0C0", //silver
	};

	public static Spanned html_to_spanned(String msg) {
		return Html.fromHtml(msg, null, new Html.TagHandler() {
			@Override
			public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
				int len = output.length();
				if(tag.startsWith("_bg")) {
					String rgb = "#" + tag.substring(3);
			        if(opening) {
			            output.setSpan(new BackgroundColorSpan(Color.parseColor(rgb)), len, len, Spannable.SPAN_MARK_MARK);
			        } else {
			            Object obj = getLast(output, BackgroundColorSpan.class);
			            int where = output.getSpanStart(obj);

			            output.removeSpan(obj);

			            if (where != len) {
			                output.setSpan(new BackgroundColorSpan(Color.parseColor(rgb)), where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			            }
			        }
				}
			}
			
			@SuppressWarnings({ "unchecked", "rawtypes" })
			private Object getLast(Editable text, Class kind) {
		        Object[] objs = text.getSpans(0, text.length(), kind);

		        if (objs.length == 0) {
		            return null;
		        } else {
		            for(int i = objs.length;i>0;i--) {
		                if(text.getSpanFlags(objs[i-1]) == Spannable.SPAN_MARK_MARK) {
		                    return objs[i-1];
		                }
		            }
		            return null;
		        }
		    }
		});
	}
	
	public static String irc_to_html(String msg) {
		int pos=0;
		boolean bold=false, underline=false, italics=false;
		String fg="", bg="";
		
		while(pos < msg.length()) {
			if(msg.charAt(pos) == 2) { //Bold
				String html = "";
				if(bold)
					html += "</b>";
				else
					html += "<b>";
				bold = !bold;
				msg = removeCharAtIndex(msg, pos);
				msg = insertAtIndex(msg, pos, html);
			} else if(msg.charAt(pos) == 16 || msg.charAt(pos) == 29) { //Italics
				String html = "";
				if(italics)
					html += "</i>";
				else
					html += "<i>";
				italics = !italics;
				msg = removeCharAtIndex(msg, pos);
				msg = insertAtIndex(msg, pos, html);
			} else if(msg.charAt(pos) == 31) { //Underline
				String html = "";
				if(underline)
					html += "</u>";
				else
					html += "<u>";
				underline = !underline;
				msg = removeCharAtIndex(msg, pos);
				msg = insertAtIndex(msg, pos, html);
			} else if(msg.charAt(pos) == 15) { //Formatting clear
				String html = "";
				if(fg.length() > 0) {
					html += "</font>";
					fg = "";
				}
				if(bg.length() > 0) {
					html += "</_bg" + bg + ">";
					bg = "";
				}
				if(bold) {
					html += "</b>";
					bold = false;
				}
				if(underline) {
					html += "</u>";
					underline = false;
				}
				if(italics) {
					html += "</i>";
					italics = false;
				}
				msg = removeCharAtIndex(msg, pos);
				if(html.length() > 0)
					msg = insertAtIndex(msg, pos, html);
			} else if(msg.charAt(pos) == 3 || msg.charAt(pos) == 4) { //Color
				boolean rgb = (msg.charAt(pos) == 4);
				String new_fg="", new_bg="";
				String v = "";
				msg = removeCharAtIndex(msg, pos);
				if(pos < msg.length()) {
					while(pos < msg.length() && (
							(msg.charAt(pos) >= '0' && msg.charAt(pos) <= '9') ||
							rgb && ((msg.charAt(pos) >= 'a' && msg.charAt(pos) <= 'f') ||
							(msg.charAt(pos) >= 'A' && msg.charAt(pos) <= 'F')))) {
						v += msg.charAt(pos);
	    				msg = removeCharAtIndex(msg, pos);
					}
					if(v.length() > 0) {
						if(v.length() < 3 && !rgb) {
							try {
								new_fg = COLOR_MAP[Integer.parseInt(v)];
							} catch (NumberFormatException e) {
		    					new_fg = v;
							}
						} else
	    					new_fg = v;
					}
					v="";
					if(pos < msg.length() && msg.charAt(pos) == ',') {
	    				msg = removeCharAtIndex(msg, pos);
						if(new_fg.length() == 0)
							new_fg = "clear";
						new_bg = "clear";
						while(pos < msg.length() && (
								(msg.charAt(pos) >= '0' && msg.charAt(pos) <= '9') ||
								rgb && ((msg.charAt(pos) >= 'a' && msg.charAt(pos) <= 'f') ||
								(msg.charAt(pos) >= 'A' && msg.charAt(pos) <= 'F')))) {
							v += msg.charAt(pos);
		    				msg = removeCharAtIndex(msg, pos);
						}
	    				if(v.length() > 0) {
							if(v.length() < 3 && !rgb) {
								try {
									new_bg = COLOR_MAP[Integer.parseInt(v)];
								} catch (NumberFormatException e) {
			    					new_bg = v;
								}
							} else
		    					new_bg = v;
	    				}
					}
					String html = "";
					if(new_fg.length() == 0 && new_bg.length() == 0) {
						new_fg = "clear";
						new_bg = "clear";
					}
					if(new_fg.length() > 0 && !new_fg.equals(fg) && fg.length() > 0) {
						html += "</font>";
					}
					if(new_bg.length() > 0 && !new_bg.equals(bg) && bg.length() > 0) {
						html += "</_bg" + bg + ">";
					}
					if(new_bg.length() > 0) {
						if(!new_bg.equals(bg)) {
							if(new_bg.equals("clear")) {
								bg = "";
							} else {
	    						html += "<_bg" + new_bg + ">";
	    						bg = new_bg;
							}
						}
					}
					if(new_fg.length() > 0) {
						if(!new_fg.equals(fg)) {
							if(new_fg.equals("clear")) {
								fg = "";
							} else {
	    						html += "<font color=\"#" + new_fg + "\">";
	    						fg = new_fg;
							}
						}
					}
					msg = insertAtIndex(msg, pos, html);
				}
			}
			pos++;
		}
		if(fg.length() > 0) {
			msg += "</font>";
		}
		if(bg.length() > 0) {
			msg += "</_bg" + bg + ">";
		}
		if(bold)
			msg += "</b>";
		if(underline)
			msg += "</u>";
		if(italics)
			msg += "</i>";

		return msg;
	}
	
    public static String insertAtIndex(String input, int index, String text) {
    	String head = input.substring(0, index);
    	String tail = input.substring(index, input.length());
    	return head + text + tail;
    }
    
    public static String removeCharAtIndex(String input, int index) {
    	if(index >= input.length() - 1)
        	return input.substring(0, input.length() - 1);
    	else
    		return input.substring(0, index) + input.substring(index+1, input.length());
    }

}
