/*
 * Copyright (c) 2013 IRCCloud, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.irccloud.android;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.XMLReader;

import android.graphics.Color;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.util.Linkify;
import android.text.util.Linkify.MatchFilter;
import android.text.util.Linkify.TransformFilter;
import android.util.Patterns;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.irccloud.android.data.ServersDataSource;

public class ColorFormatter {
    //From: https://github.com/android/platform_frameworks_base/blob/master/core/java/android/util/Patterns.java
    public static final String TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL =
        "(?:"
        + "(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])"
        + "|(?:biz|b[abdefghijmnorstvwyz])"
        + "|(?:cat|com|coop|c[acdfghiklmnoruvxyz])"
        + "|d[ejkmoz]"
        + "|(?:edu|e[cegrstu])"
        + "|f[ijkmor]"
        + "|(?:gov|g[abdefghilmnpqrstuwy])"
        + "|h[kmnrtu]"
        + "|(?:info|int|i[delmnoqrst])"
        + "|(?:jobs|j[emop])"
        + "|k[eghimnprwyz]"
        + "|l[abcikrstuvy]"
        + "|(?:mil|mobi|museum|m[acdeghklmnopqrstuvwxyz])"
        + "|(?:name|net|n[acefgilopruz])"
        + "|(?:org|om)"
        + "|(?:pro|p[aefghklmnrstwy])"
        + "|qa"
        + "|r[eosuw]"
        + "|s[abcdeghijklmnortuvyz]"
        + "|(?:tel|travel|t[cdfghjklmnoprtvwz])"
        + "|u[agksyz]"
        + "|v[aceginu]"
        + "|w[fs]"
        + "|(?:\u03b4\u03bf\u03ba\u03b9\u03bc\u03ae|\u0438\u0441\u043f\u044b\u0442\u0430\u043d\u0438\u0435|\u0440\u0444|\u0441\u0440\u0431|\u05d8\u05e2\u05e1\u05d8|\u0622\u0632\u0645\u0627\u06cc\u0634\u06cc|\u0625\u062e\u062a\u0628\u0627\u0631|\u0627\u0644\u0627\u0631\u062f\u0646|\u0627\u0644\u062c\u0632\u0627\u0626\u0631|\u0627\u0644\u0633\u0639\u0648\u062f\u064a\u0629|\u0627\u0644\u0645\u063a\u0631\u0628|\u0627\u0645\u0627\u0631\u0627\u062a|\u0628\u06be\u0627\u0631\u062a|\u062a\u0648\u0646\u0633|\u0633\u0648\u0631\u064a\u0629|\u0641\u0644\u0633\u0637\u064a\u0646|\u0642\u0637\u0631|\u0645\u0635\u0631|\u092a\u0930\u0940\u0915\u094d\u0937\u093e|\u092d\u093e\u0930\u0924|\u09ad\u09be\u09b0\u09a4|\u0a2d\u0a3e\u0a30\u0a24|\u0aad\u0abe\u0ab0\u0aa4|\u0b87\u0ba8\u0bcd\u0ba4\u0bbf\u0baf\u0bbe|\u0b87\u0bb2\u0b99\u0bcd\u0b95\u0bc8|\u0b9a\u0bbf\u0b99\u0bcd\u0b95\u0baa\u0bcd\u0baa\u0bc2\u0bb0\u0bcd|\u0baa\u0bb0\u0bbf\u0b9f\u0bcd\u0b9a\u0bc8|\u0c2d\u0c3e\u0c30\u0c24\u0c4d|\u0dbd\u0d82\u0d9a\u0dcf|\u0e44\u0e17\u0e22|\u30c6\u30b9\u30c8|\u4e2d\u56fd|\u4e2d\u570b|\u53f0\u6e7e|\u53f0\u7063|\u65b0\u52a0\u5761|\u6d4b\u8bd5|\u6e2c\u8a66|\u9999\u6e2f|\ud14c\uc2a4\ud2b8|\ud55c\uad6d|xn\\-\\-0zwm56d|xn\\-\\-11b5bs3a9aj6g|xn\\-\\-3e0b707e|xn\\-\\-45brj9c|xn\\-\\-80akhbyknj4f|xn\\-\\-90a3ac|xn\\-\\-9t4b11yi5a|xn\\-\\-clchc0ea0b2g2a9gcd|xn\\-\\-deba0ad|xn\\-\\-fiqs8s|xn\\-\\-fiqz9s|xn\\-\\-fpcrj9c3d|xn\\-\\-fzc2c9e2c|xn\\-\\-g6w251d|xn\\-\\-gecrj9c|xn\\-\\-h2brj9c|xn\\-\\-hgbk6aj7f53bba|xn\\-\\-hlcj6aya9esc7a|xn\\-\\-j6w193g|xn\\-\\-jxalpdlp|xn\\-\\-kgbechtv|xn\\-\\-kprw13d|xn\\-\\-kpry57d|xn\\-\\-lgbbat1ad8j|xn\\-\\-mgbaam7a8h|xn\\-\\-mgbayh7gpa|xn\\-\\-mgbbh1a71e|xn\\-\\-mgbc0a9azcg|xn\\-\\-mgberp4a5d4ar|xn\\-\\-o3cw4h|xn\\-\\-ogbpf8fl|xn\\-\\-p1ai|xn\\-\\-pgbs0dh|xn\\-\\-s9brj9c|xn\\-\\-wgbh1c|xn\\-\\-wgbl6a|xn\\-\\-xkc2al3hye2a|xn\\-\\-xkc2dl3a5ee0h|xn\\-\\-yfro4i67o|xn\\-\\-ygbi2ammx|xn\\-\\-zckzah|xxx)"
        + "|y[et]"
        + "|z[amw]))";

    public static final String GOOD_IRI_CHAR =
            "a-zA-Z0-9\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF";

    public static final Pattern WEB_URL = Pattern.compile(
            "((?:(http|https|Http|Https|rtsp|Rtsp):\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
                    + "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
                    + "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)?"
                    + "((?:(?:[" + GOOD_IRI_CHAR + "][" + GOOD_IRI_CHAR + "\\-]{0,64}\\.)+"   // named host
                    + TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL
                    + "|(?:(?:25[0-5]|2[0-4]" // or ip address
                    + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(?:25[0-5]|2[0-4][0-9]"
                    + "|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1]"
                    + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                    + "|[1-9][0-9]|[0-9])))"
                    + "(?:\\:\\d{1,5})?)" // plus option port number
                    + "(\\/(?:(?:[" + GOOD_IRI_CHAR + "\\;\\/\\?\\:\\@\\&\\=\\#\\~\\$"  // plus option query params
                    + "\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])|(?:\\%[a-fA-F0-9]{2}))*)?"
                    + "(?:\\b|$)");

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
		return html_to_spanned(msg, false, null);
	}
	
	public static Spanned html_to_spanned(String msg, boolean linkify, final ServersDataSource.Server server) {
		if(msg == null)
			msg = "";
		Spannable output = (Spannable)Html.fromHtml(msg, null, new Html.TagHandler() {
			@Override
			public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
				int len = output.length();
				if(tag.startsWith("_bg")) {
					String rgb = "#";
					if(tag.length() == 9) {
						rgb += tag.substring(3);
					} else {
						rgb += "ffffff";
					}
			        if(opening) {
			        	try {
			        		output.setSpan(new BackgroundColorSpan(Color.parseColor(rgb)), len, len, Spannable.SPAN_MARK_MARK);
			        	} catch (IllegalArgumentException e) {
			        		output.setSpan(new BackgroundColorSpan(Color.parseColor("#ffffff")), len, len, Spannable.SPAN_MARK_MARK);
			        	}
			        } else {
			            Object obj = getLast(output, BackgroundColorSpan.class);
			            int where = output.getSpanStart(obj);

			            output.removeSpan(obj);

			            if (where != len) {
			            	try {
			            		output.setSpan(new BackgroundColorSpan(Color.parseColor(rgb)), where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				        	} catch (IllegalArgumentException e) {
			            		output.setSpan(new BackgroundColorSpan(Color.parseColor("#ffffff")), where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				        	}
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

        String chanTypes = "#";
        if(server != null && server.CHANTYPES != null && server.CHANTYPES.length() > 0)
            chanTypes = server.CHANTYPES;

        final String pattern = "\\B([" + chanTypes + "][^<>!?\"()\\[\\],\\s\ufe55]+)";

		if(linkify) {
            Linkify.addLinks(output, WEB_URL, null, new MatchFilter() {
		        public final boolean acceptMatch(CharSequence s, int start, int end) {
		        	if(start >= 6 && s.subSequence(start - 6, end).toString().toLowerCase().startsWith("irc://"))
		        		return false;
		        	if(start >= 7 && s.subSequence(start - 7, end).toString().toLowerCase().startsWith("ircs://"))
		        		return false;
		        	if(s.subSequence(start, end).toString().toLowerCase().startsWith("https://"))
		        		return false;
		        	if(s.subSequence(start, end).toString().toLowerCase().startsWith("http://"))
		        		return false;
                    if(start >= 1 && s.subSequence(start - 1, end).toString().matches(pattern))
                        return false;
		        	return Linkify.sUrlMatchFilter.acceptMatch(s, start, end);
		        }
		    }, new TransformFilter() {
                        @Override
                        public String transformUrl(Matcher match, String url) {
                            if(PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("imageviewer", true)) {
                                String lower = url.toLowerCase();
                                if(lower.matches("(^.*\\/.*\\.png$)|(^.*\\/.*\\.jpe?g$)|(^.*\\/.*\\.gif$)|" +
                                        "(^(www\\.)?flickr\\.com/photos/.*$)|" +
                                        "(^(www\\.)?instagram\\.com/p/.*$)|(^(www\\.)?instagr\\.am/p/.*$)|" +
                                        "(^(www\\.)?imgur\\.com/(?!a/).*$)|" +
                                        "(^d\\.pr/i/.*)|(^droplr\\.com/i/.*)|"+
                                        "(^cl\\.ly/.*)"
                                        ) && !lower.matches("(^cl\\.ly/robots\\.txt$)|(^cl\\.ly/image/?$)")) {
                                    return IRCCloudApplication.getInstance().getApplicationContext().getResources().getString(R.string.IMAGE_SCHEME) + "://" + url;
                                }
                            }
                            return "http://" + url;
                        }
                    });
			//based on http://daringfireball.net/2010/07/improved_regex_for_matching_urls
			Linkify.addLinks(output, Pattern.compile("https?://(" +
                    "(?:|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)" +
                    "(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))" +
                    "+(?:(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))", Pattern.CASE_INSENSITIVE), null, null, new TransformFilter() {
                @Override
                public String transformUrl(Matcher match, String url) {
                    if(PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("imageviewer", true)) {
                        String lower = url.toLowerCase();
                        if(lower.matches("(^.*\\/.*\\.png$)|(^.*\\/.*\\.jpe?g$)|(^.*\\/.*\\.gif$)|" +
                                "(^https?://(www\\.)?flickr\\.com/photos/.*$)|" +
                                "(^https?://(www\\.)?instagram\\.com/p/.*$)|(^https?://(www\\.)?instagr\\.am/p/.*$)|" +
                                "(^https?://(www\\.)?imgur\\.com/(?!a/).*$)|" +
                                "(^https?://d\\.pr/i/.*)|(^https?://droplr\\.com/i/.*)|"+
                                "(^https?://cl\\.ly/.*)"
                        ) && !lower.matches("(^https?://cl\\.ly/robots\\.txt$)|(^https?://cl\\.ly/image/?$)")) {
                            if(lower.startsWith("http://"))
                                return IRCCloudApplication.getInstance().getApplicationContext().getResources().getString(R.string.IMAGE_SCHEME) + "://" + url.substring(7);
                            else if(lower.startsWith("https://"))
                                return IRCCloudApplication.getInstance().getApplicationContext().getResources().getString(R.string.IMAGE_SCHEME_SECURE) + "://" + url.substring(8);
                        }
                    }
                    if(url.toLowerCase().startsWith("https:"))
                        url = "https" + url.substring(5);
                    else
                        url = "http" + url.substring(4);
                    return url;
                }
            });
			Linkify.addLinks(output, Patterns.EMAIL_ADDRESS, "mailto:");
			Linkify.addLinks(output, Pattern.compile("ircs?://[^<>\"()\\[\\],\\s]+"), null, null, new TransformFilter() {
		        public final String transformUrl(final Matcher match, String url) {
		            return url.replace("#", "%23");
		        }
		    });
    		
			
		}
        if(server != null) {
            Linkify.addLinks(output, Pattern.compile(pattern), null, new MatchFilter() {
                        public final boolean acceptMatch(CharSequence s, int start, int end) {
                            try {
                                Integer.parseInt(s.subSequence(start+1, end).toString());
                                return false;
                            } catch (NumberFormatException e) {
                                return true;
                            }
                        }
                    }, new TransformFilter() {
                        public final String transformUrl(final Matcher match, String url) {
                            String channel = match.group(1);
                            try {
                                channel = URLEncoder.encode(channel, "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                            }
                            return "irc://" + server.cid + "/" + channel;
                        }
                    });
        }

		return output;
	}
	
	public static String irc_to_html(String msg) {
		int pos=0;
		boolean bold=false, underline=false, italics=false;
		String fg="", bg="";
		StringBuilder builder = new StringBuilder(msg);
		
		while(pos < builder.length()) {
			if(builder.charAt(pos) == 2) { //Bold
				String html = "";
				if(bold)
					html += "</b>";
				else
					html += "<b>";
				bold = !bold;
				builder.deleteCharAt(pos);
				builder.insert(pos, html);
			} else if(builder.charAt(pos) == 22 || builder.charAt(pos) == 29) { //Italics
				String html = "";
				if(italics)
					html += "</i>";
				else
					html += "<i>";
				italics = !italics;
				builder.deleteCharAt(pos);
				builder.insert(pos, html);
			} else if(builder.charAt(pos) == 31) { //Underline
				String html = "";
				if(underline)
					html += "</u>";
				else
					html += "<u>";
				underline = !underline;
				builder.deleteCharAt(pos);
				builder.insert(pos, html);
			} else if(builder.charAt(pos) == 15) { //Formatting clear
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
				builder.deleteCharAt(pos);
				if(html.length() > 0)
					builder.insert(pos, html);
			} else if(builder.charAt(pos) == 3 || builder.charAt(pos) == 4) { //Color
				boolean rgb = (builder.charAt(pos) == 4);
				int count = 0;
				String new_fg="", new_bg="";
				builder.deleteCharAt(pos);
				if(pos < builder.length()) {
					while(pos+count < builder.length() && (
							(builder.charAt(pos+count) >= '0' && builder.charAt(pos+count) <= '9') ||
							rgb && ((builder.charAt(pos+count) >= 'a' && builder.charAt(pos+count) <= 'f') ||
							(builder.charAt(pos+count) >= 'A' && builder.charAt(pos+count) <= 'F')))) {
						if((++count == 2 && !rgb) || count == 6)
							break;
					}
					if(count > 0) {
						if(count < 3 && !rgb) {
							try {
								int col = Integer.parseInt(builder.substring(pos, pos + count));
								if(col > 15) {
									count--;
									col /= 10;
								}
								new_fg = COLOR_MAP[col];
							} catch (NumberFormatException e) {
		    					new_fg = builder.substring(pos, pos + count);
							}
						} else
	    					new_fg = builder.substring(pos, pos + count);
						builder.delete(pos, pos + count);
					}
					if(pos < builder.length() && builder.charAt(pos) == ',') {
						builder.deleteCharAt(pos);
						if(new_fg.length() == 0)
							new_fg = "clear";
						new_bg = "clear";
						count = 0;
						while(pos+count < builder.length() && (
								(builder.charAt(pos+count) >= '0' && builder.charAt(pos+count) <= '9') ||
								rgb && ((builder.charAt(pos+count) >= 'a' && builder.charAt(pos+count) <= 'f') ||
								(builder.charAt(pos+count) >= 'A' && builder.charAt(pos+count) <= 'F')))) {
                            if((++count == 2 && !rgb) || count == 6)
								break;
						}
						if(count > 0) {
							if(count < 3 && !rgb) {
								try {
									int col = Integer.parseInt(builder.substring(pos, pos + count));
									if(col > 15) {
										count--;
										col /= 10;
									}
									new_bg = COLOR_MAP[col];
								} catch (NumberFormatException e) {
			    					new_bg = builder.substring(pos, pos + count);
								}
							} else
		    					new_bg = builder.substring(pos, pos + count);
							builder.delete(pos, pos + count);
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
								bg = "";
								if(new_bg.length() == 6) {
									bg = new_bg;
								} else if(new_bg.length() == 3) {
									bg += new_bg.charAt(0);
									bg += new_bg.charAt(0);
									bg += new_bg.charAt(1);
									bg += new_bg.charAt(1);
									bg += new_bg.charAt(2);
									bg += new_bg.charAt(2);
								} else {
									bg = "#ffffff";
								}
	    						html += "<_bg" + bg + ">";
							}
						}
					}
					if(new_fg.length() > 0) {
						if(!new_fg.equals(fg)) {
							if(new_fg.equals("clear")) {
								fg = "";
							} else {
								fg = "";
								if(new_fg.length() == 6) {
									fg = new_fg;
								} else if(new_fg.length() == 3) {
									fg += new_fg.charAt(0);
									fg += new_fg.charAt(0);
									fg += new_fg.charAt(1);
									fg += new_fg.charAt(1);
									fg += new_fg.charAt(2);
									fg += new_fg.charAt(2);
								} else {
									fg = "#000000";
								}
	    						html += "<font color=\"#" + fg + "\">";
							}
						}
					}
					builder.insert(pos, html);
				}
			}
			pos++;
		}
		if(fg.length() > 0) {
			builder.append("</font>");
		}
		if(bg.length() > 0) {
			builder.append("</_bg" + bg + ">");
		}
		if(bold)
			builder.append("</b>");
		if(underline)
			builder.append("</u>");
		if(italics)
			builder.append("</i>");

		return builder.toString();
	}
}
