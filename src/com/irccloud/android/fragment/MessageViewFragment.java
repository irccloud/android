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

package com.irccloud.android.fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;

import android.content.ActivityNotFoundException;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.ListFragment;
import android.text.Html;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.animation.AlphaAnimation;
import android.widget.*;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.crashlytics.android.Crashlytics;
import com.fasterxml.jackson.databind.JsonNode;
import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.data.BuffersDataSource;
import com.irccloud.android.fragment.BuffersListFragment.OnBufferSelectedListener;
import com.irccloud.android.CollapsedEventsList;
import com.irccloud.android.ColorFormatter;
import com.irccloud.android.data.EventsDataSource;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.Ignore;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.ServersDataSource;

public class MessageViewFragment extends ListFragment implements NetworkConnection.IRCEventHandler {
	private NetworkConnection conn;
	private TextView statusView;
	private View headerViewContainer;
	private View headerView;
    private TextView backlogFailed;
    private Button loadBacklogButton;
	private TextView unreadTopLabel;
	private TextView unreadBottomLabel;
	private View unreadTopView;
	private View unreadBottomView;
	private TextView highlightsTopLabel;
	private TextView highlightsBottomLabel;
    public BuffersDataSource.Buffer buffer;
    private ServersDataSource.Server server;
	private long earliest_eid;
	private long backlog_eid = 0;
	private boolean requestingBacklog = false;
	private float avgInsertTime = 0;
	private int newMsgs = 0;
	private long newMsgTime = 0;
	private int newHighlights = 0;
	private MessageViewListener mListener;
	private TextView errorMsg = null;
	private TextView connectingMsg = null;
	private ProgressBar progressBar = null;
	private static final Timer countdownTimer = new Timer("messsage-countdown-timer");
    private TimerTask countdownTimerTask = null;
	private String error = null;
	private View connecting = null;
	private View awayView = null;
	private TextView awayTxt = null;
	private int timestamp_width = -1;
    private float textSize = 14.0f;
	private View globalMsgView = null;
	private TextView globalMsg = null;
    private ProgressBar spinner = null;
	private final Handler mHandler = new Handler();

	public static final int ROW_MESSAGE = 0;
	public static final int ROW_TIMESTAMP = 1;
	public static final int ROW_BACKLOGMARKER = 2;
	public static final int ROW_SOCKETCLOSED = 3;
	public static final int ROW_LASTSEENEID = 4;
	private static final String TYPE_TIMESTAMP = "__timestamp__";
	private static final String TYPE_BACKLOGMARKER = "__backlog__";
	private static final String TYPE_LASTSEENEID = "__lastseeneid__";
	
	private MessageAdapter adapter;

	private long currentCollapsedEid = -1;
    private long lastCollapsedEid = -1;
	private CollapsedEventsList collapsedEvents = new CollapsedEventsList();
	private int lastCollapsedDay = -1;
	private HashSet<Long> expandedSectionEids = new HashSet<Long>();
	private RefreshTask refreshTask = null;
	private HeartbeatTask heartbeatTask = null;
	private Ignore ignore = new Ignore();
	private static final Timer tapTimer = new Timer("message-tap-timer");
    private TimerTask tapTimerTask = null;
	public boolean longPressOverride = false;
	private LinkMovementMethodNoLongPress linkMovementMethodNoLongPress = new LinkMovementMethodNoLongPress();
	public boolean ready = false;
    private final Object adapterLock = new Object();

    public View suggestionsContainer = null;
    public GridView suggestions = null;

	private class LinkMovementMethodNoLongPress extends LinkMovementMethod {
		@Override
	    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
			if(!longPressOverride && event.getAction() == MotionEvent.ACTION_UP) {
                try {
                    return super.onTouchEvent(widget, buffer, event);
                } catch (ActivityNotFoundException e) {
                    // No app installed to handle this URL
                }
			}
            return false;
		}
	}
	
	private class MessageAdapter extends BaseAdapter {
		ArrayList<EventsDataSource.Event> data;
		private ListFragment ctx;
		private long max_eid = 0;
		private long min_eid = 0;
		private int lastDay = -1;
		private int lastSeenEidMarkerPosition = -1;
		private int currentGroupPosition = -1;
		private TreeSet<Integer> unseenHighlightPositions;
		
		private class ViewHolder {
			int type;
			TextView timestamp;
			TextView message;
            ImageView expandable;
            ImageView failed;
		}
	
		public MessageAdapter(ListFragment context) {
			ctx = context;
			data = new ArrayList<EventsDataSource.Event>();
			unseenHighlightPositions = new TreeSet<Integer>(Collections.reverseOrder());
		}
		
		public void clear() {
			max_eid = 0;
			min_eid = 0;
			lastDay = -1;
			lastSeenEidMarkerPosition = -1;
			currentGroupPosition = -1;
			data.clear();
			unseenHighlightPositions.clear();
		}
		
		public void clearPending() {
			for(int i = 0; i < data.size(); i++) {
				if(data.get(i).reqid != -1 && data.get(i).color == R.color.timestamp) {
					data.remove(i);
					i--;
				}
			}
		}

		public void removeItem(long eid) {
			for(int i = 0; i < data.size(); i++) {
				if(data.get(i).eid == eid) {
					data.remove(i);
					i--;
				}
			}
		}

		public int getBacklogMarkerPosition() {
            try {
                for(int i = 0; data != null && i < data.size(); i++) {
                    EventsDataSource.Event e = data.get(i);
                    if(e != null && e.row_type == ROW_BACKLOGMARKER) {
                        return i;
                    }
                }
            } catch (Exception e) {
            }
			return -1;
		}

		public int insertLastSeenEIDMarker() {
			EventsDataSource.Event e = new EventsDataSource.Event();
			e.type = TYPE_LASTSEENEID;
			e.row_type = ROW_LASTSEENEID;
			e.bg_color = R.drawable.socketclosed_bg;
			for(int i = 0; i < data.size(); i++) {
				if(data.get(i).row_type == ROW_LASTSEENEID) {
					data.remove(i);
				}
			}

            if(buffer == null)
                return -1;

            if(min_eid > 0 && buffer.last_seen_eid > 0 && min_eid >= buffer.last_seen_eid) {
				lastSeenEidMarkerPosition = 0;
			} else {
				for(int i = data.size() - 1; i >= 0; i--) {
					if(data.get(i).eid <= buffer.last_seen_eid) {
						lastSeenEidMarkerPosition = i;
						break;
					}
				}
				if(lastSeenEidMarkerPosition > 0 && lastSeenEidMarkerPosition != data.size() - 1 && !data.get(lastSeenEidMarkerPosition).self && !data.get(lastSeenEidMarkerPosition).pending) {
					if(data.get(lastSeenEidMarkerPosition - 1).row_type == ROW_TIMESTAMP)
						lastSeenEidMarkerPosition--;
					if(lastSeenEidMarkerPosition > 0)
						data.add(lastSeenEidMarkerPosition + 1, e);
				} else {
					lastSeenEidMarkerPosition = -1;
				}
			}
            if(lastSeenEidMarkerPosition > 0 && lastSeenEidMarkerPosition <= currentGroupPosition)
                currentGroupPosition++;
			return lastSeenEidMarkerPosition;
		}
		
		public void clearLastSeenEIDMarker() {
			for(int i = 0; i < data.size(); i++) {
				if(data.get(i).row_type == ROW_LASTSEENEID) {
					data.remove(i);
				}
			}
			lastSeenEidMarkerPosition = -1;
		}
		
		public int getLastSeenEIDPosition() {
			return lastSeenEidMarkerPosition;
		}
		
		public int getUnreadHighlightsAbovePosition(int pos) {
			int count = 0;

			Iterator<Integer> i = unseenHighlightPositions.iterator();
			while(i.hasNext()) {
				Integer p = i.next();
				if(p < pos)
					break;
				count++;
			}
			
			return unseenHighlightPositions.size() - count;
		}

        public synchronized void addItem(long eid, EventsDataSource.Event e) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(eid / 1000);
			int insert_pos = -1;
			SimpleDateFormat formatter = null;
            if(e.timestamp == null || e.timestamp.length() == 0) {
                formatter = new SimpleDateFormat("h:mm a");
                if(conn.getUserInfo() != null && conn.getUserInfo().prefs != null) {
                    try {
                        JSONObject prefs = conn.getUserInfo().prefs;
                        if(prefs.has("time-24hr") && prefs.getBoolean("time-24hr")) {
                            if(prefs.has("time-seconds") && prefs.getBoolean("time-seconds"))
                                formatter = new SimpleDateFormat("H:mm:ss");
                            else
                                formatter = new SimpleDateFormat("H:mm");
                        } else if(prefs.has("time-seconds") && prefs.getBoolean("time-seconds")) {
                            formatter = new SimpleDateFormat("h:mm:ss a");
                        }
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                    }
                }
                e.timestamp = formatter.format(calendar.getTime());
            }
			e.group_eid = currentCollapsedEid;
			if(e.group_msg != null && e.html == null)
				e.html = e.group_msg;

			/*if(e.html != null) {
				e.html = ColorFormatter.irc_to_html(e.html);
                e.formatted = ColorFormatter.html_to_spanned(e.html, e.linkify, server);
			}*/

            if(e.day < 1) {
                e.day = calendar.get(Calendar.DAY_OF_YEAR);
            }

			if(currentGroupPosition > 0 && eid == currentCollapsedEid && e.eid != eid) { //Shortcut for replacing the current group
				calendar.setTimeInMillis(e.eid / 1000);
				lastDay = e.day;
				data.remove(currentGroupPosition);
				data.add(currentGroupPosition, e);
				insert_pos = currentGroupPosition;
			} else if(eid > max_eid || data.size() == 0 || eid > data.get(data.size()-1).eid) { //Message at the bottom
				if(data.size() > 0) {
					lastDay = data.get(data.size()-1).day;
				} else {
					lastDay = 0;
				}
				max_eid = eid;
				data.add(e);
				insert_pos = data.size() - 1;
			} else if(min_eid > eid) { //Message goes on top
				if(data.size() > 1) {
					lastDay = data.get(1).day;
					if(calendar.get(Calendar.DAY_OF_YEAR) != lastDay) { //Insert above the dateline
						data.add(0, e);
						insert_pos = 0;
					} else { //Insert below the dateline
						data.add(1, e);
						insert_pos = 1;
					}
				} else {
					data.add(0, e);
					insert_pos = 0;
				}
			} else {
				int i = 0;
				for(EventsDataSource.Event e1 : data) {
                    if(e1.row_type != ROW_TIMESTAMP && e1.eid > eid && e.eid == eid && e1.group_eid != eid) { //Insert the message
						if(i > 0 && data.get(i-1).row_type != ROW_TIMESTAMP) {
							lastDay = data.get(i-1).day;
							data.add(i, e);
							insert_pos = i;
							break;
						} else { //There was a date line above our insertion point
							lastDay = e1.day;
							if(calendar.get(Calendar.DAY_OF_YEAR) != lastDay) { //Insert above the dateline
								if(i > 1) {
									lastDay = data.get(i-2).day;
								} else {
									//We're above the first dateline, so we'll need to put a new one on top!
									lastDay = 0;
								}
								data.add(i-1, e);
								insert_pos = i-1;
							} else { //Insert below the dateline
								data.add(i, e);
								insert_pos = i;
							}
							break;
						}
					} else if(e1.row_type != ROW_TIMESTAMP && (e1.eid == eid || e1.group_eid == eid)) { //Replace the message
						lastDay = calendar.get(Calendar.DAY_OF_YEAR);
						data.remove(i);
						data.add(i, e);
						insert_pos = i;
						break;
					}
					i++;
				}
			}

			if(insert_pos == -1) {
				Log.e("IRCCloud", "Couldn't insert EID: " + eid + " MSG: " + e.html);
				return;
			}
			
			if(eid > buffer.last_seen_eid && e.highlight)
				unseenHighlightPositions.add(insert_pos);
			
			if(eid < min_eid || min_eid == 0)
				min_eid = eid;
			
			if(eid == currentCollapsedEid && e.eid == eid) {
				currentGroupPosition = insert_pos;
			} else if(currentCollapsedEid == -1) {
				currentGroupPosition = -1;
			}
			
			if(calendar.get(Calendar.DAY_OF_YEAR) != lastDay) {
                if(formatter == null)
    				formatter = new SimpleDateFormat("EEEE, MMMM dd, yyyy");
                else
                    formatter.applyPattern("EEEE, MMMM dd, yyyy");
				EventsDataSource.Event d = new EventsDataSource.Event();
				d.type = TYPE_TIMESTAMP;
				d.row_type = ROW_TIMESTAMP;
				d.eid = eid;
				d.timestamp = formatter.format(calendar.getTime());
				d.bg_color = R.drawable.row_timestamp_bg;
                d.day = lastDay = calendar.get(Calendar.DAY_OF_YEAR);
				data.add(insert_pos, d);
				if(currentGroupPosition > -1)
					currentGroupPosition++;
			}
		}
		
		@Override
		public int getCount() {
			return data.size();
		}

		@Override
		public Object getItem(int position) {
			return data.get(position);
		}

		@Override
		public long getItemId(int position) {
			return data.get(position).eid;
		}
		
        public void format() {
            for(int i = 0; i < data.size(); i++) {
                EventsDataSource.Event e = data.get(i);
                synchronized(e) {
                    if(e.html != null) {
                        try {
                            e.html = ColorFormatter.emojify(ColorFormatter.irc_to_html(e.html));
                            e.formatted = ColorFormatter.html_to_spanned(e.html, e.linkify, server);
                            if(e.msg != null && e.msg.length() > 0)
                                e.contentDescription = ColorFormatter.html_to_spanned(ColorFormatter.irc_to_html(e.msg), e.linkify, server).toString();
                        } catch (Exception ex) {
                        }
                    }
                }
            }
        }

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			EventsDataSource.Event e = data.get(position);
            synchronized (e) {
                View row = convertView;
                ViewHolder holder;

                if(row != null && ((ViewHolder)row.getTag()).type != e.row_type)
                    row = null;

                if (row == null) {
                    LayoutInflater inflater = ctx.getLayoutInflater(null);
                    if(e.row_type == ROW_BACKLOGMARKER)
                        row = inflater.inflate(R.layout.row_backlogmarker, null);
                    else if(e.row_type == ROW_TIMESTAMP)
                        row = inflater.inflate(R.layout.row_timestamp, null);
                    else if(e.row_type == ROW_SOCKETCLOSED)
                        row = inflater.inflate(R.layout.row_socketclosed, null);
                    else if(e.row_type == ROW_LASTSEENEID)
                        row = inflater.inflate(R.layout.row_lastseeneid, null);
                    else
                        row = inflater.inflate(R.layout.row_message, null);

                    holder = new ViewHolder();
                    holder.timestamp = (TextView) row.findViewById(R.id.timestamp);
                    holder.message = (TextView) row.findViewById(R.id.message);
                    holder.expandable = (ImageView) row.findViewById(R.id.expandable);
                    holder.failed = (ImageView) row.findViewById(R.id.failed);
                    holder.type = e.row_type;

                    row.setTag(holder);
                } else {
                    holder = (ViewHolder) row.getTag();
                }

                row.setOnClickListener(new OnItemClickListener(position));

                if(e.html != null && e.formatted == null) {
                    e.html = ColorFormatter.emojify(ColorFormatter.irc_to_html(e.html));
                    e.formatted = ColorFormatter.html_to_spanned(e.html, e.linkify, server);
                    if(e.msg != null && e.msg.length() > 0)
                        e.contentDescription = ColorFormatter.html_to_spanned(ColorFormatter.irc_to_html(e.msg), e.linkify, server).toString();
                }

                if(e.row_type == ROW_MESSAGE) {
                    if(e.bg_color == R.color.message_bg)
                        row.setBackgroundDrawable(null);
                    else
                        row.setBackgroundResource(e.bg_color);
                    if(e.contentDescription != null && e.from != null && e.from.length() > 0 && e.msg != null && e.msg.length() > 0) {
                        row.setContentDescription("Message from " + e.from + " at " + e.timestamp + ": " + e.contentDescription);
                    }
                }

                if(holder.timestamp != null) {
                    if(e.row_type == ROW_TIMESTAMP) {
                        holder.timestamp.setTextSize(textSize);
                    } else {
                        holder.timestamp.setTextSize(textSize - 2);

                        if (timestamp_width == -1) {
                            String s = "888:888";
                            if (conn != null && conn.getUserInfo() != null && conn.getUserInfo().prefs != null) {
                                try {
                                    JSONObject prefs = conn.getUserInfo().prefs;
                                    if (prefs.has("time-seconds") && prefs.getBoolean("time-seconds"))
                                        s += ":88";
                                    if (!prefs.has("time-24hr") || !prefs.getBoolean("time-24hr"))
                                        s += " 88";
                                } catch (Exception e1) {

                                }
                            }
                            timestamp_width = (int) holder.timestamp.getPaint().measureText(s);
                        }
                        holder.timestamp.setMinWidth(timestamp_width);
                    }
                    if(e.highlight && !e.self)
                        holder.timestamp.setTextColor(getSafeResources().getColor(R.color.highlight_timestamp));
                    else if(e.row_type != ROW_TIMESTAMP)
                        holder.timestamp.setTextColor(getSafeResources().getColor(R.color.timestamp));
                    holder.timestamp.setText(e.timestamp);
                }
                if(e.row_type == ROW_SOCKETCLOSED) {
                    if(e.msg.length() > 0) {
                        holder.timestamp.setVisibility(View.VISIBLE);
                        holder.message.setVisibility(View.VISIBLE);
                    } else {
                        holder.timestamp.setVisibility(View.GONE);
                        holder.message.setVisibility(View.GONE);
                    }
                }

                if(holder.message != null && e.html != null) {
                    holder.message.setMovementMethod(linkMovementMethodNoLongPress);
                    holder.message.setOnClickListener(new OnItemClickListener(position));
                    if(e.msg != null && e.msg.startsWith("<pre>"))
                        holder.message.setTypeface(Typeface.MONOSPACE);
                    else
                        holder.message.setTypeface(Typeface.DEFAULT);
                    try {
                        holder.message.setTextColor(getSafeResources().getColorStateList(e.color));
                    } catch (Exception e1) {

                    }
                    if(e.color == R.color.timestamp || e.pending)
                        holder.message.setLinkTextColor(getSafeResources().getColor(R.color.lightLinkColor));
                    else
                        holder.message.setLinkTextColor(getSafeResources().getColor(R.color.linkColor));
                    holder.message.setText(e.formatted);
                    if(e.from != null && e.from.length() > 0 && e.msg != null && e.msg.length() > 0) {
                        holder.message.setContentDescription(e.from + ": " + e.contentDescription);
                    }
                    holder.message.setTextSize(textSize);
                }

                if(holder.expandable != null) {
                    if(e.group_eid > 0 && (e.group_eid != e.eid || expandedSectionEids.contains(e.group_eid))) {
                        if(expandedSectionEids.contains(e.group_eid)) {
                            if(e.group_eid == e.eid + 1) {
                                holder.expandable.setImageResource(R.drawable.bullet_toggle_minus);
                                row.setBackgroundResource(R.drawable.status_bg);
                            } else {
                                holder.expandable.setImageResource(R.drawable.tiny_plus);
                                row.setBackgroundResource(R.drawable.expanded_row_bg);
                            }
                        } else {
                            holder.expandable.setImageResource(R.drawable.bullet_toggle_plus);
                        }
                        holder.expandable.setVisibility(View.VISIBLE);
                    } else {
                        holder.expandable.setVisibility(View.GONE);
                    }
                }

                if(holder.failed != null)
                    holder.failed.setVisibility(e.failed?View.VISIBLE:View.GONE);

                return row;
            }
        }
	}
	
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	final View v = inflater.inflate(R.layout.messageview, container, false);
        connecting = v.findViewById(R.id.connecting);
		errorMsg = (TextView)v.findViewById(R.id.errorMsg);
		connectingMsg = (TextView)v.findViewById(R.id.connectingMsg);
		progressBar = (ProgressBar)v.findViewById(R.id.connectingProgress);
    	statusView = (TextView)v.findViewById(R.id.statusView);
    	statusView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(buffer != null && conn != null && server != null && server.status != null && server.status.equalsIgnoreCase("disconnected")) {
					conn.reconnect(buffer.cid);
				}
			}
    		
    	});
    	
    	awayView = v.findViewById(R.id.away);
    	awayView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				conn.back(buffer.cid);
			}
    		
    	});
    	awayTxt = (TextView)v.findViewById(R.id.awayTxt);
    	unreadBottomView = v.findViewById(R.id.unreadBottom);
    	unreadBottomView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				getListView().setSelection(adapter.getCount() - 1);
			}
    		
    	});
    	unreadBottomLabel = (TextView)v.findViewById(R.id.unread);
    	highlightsBottomLabel = (TextView)v.findViewById(R.id.highlightsBottom);

    	unreadTopView = v.findViewById(R.id.unreadTop);
		unreadTopView.setVisibility(View.GONE);
    	unreadTopView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(adapter.getLastSeenEIDPosition() > 0) {
                    if(heartbeatTask != null)
                        heartbeatTask.cancel(true);
                    heartbeatTask = new HeartbeatTask();
                    heartbeatTask.execute((Void)null);
                    unreadTopView.setVisibility(View.GONE);
				}
				getListView().setSelection(adapter.getLastSeenEIDPosition());
			}
    		
    	});
    	unreadTopLabel = (TextView)v.findViewById(R.id.unreadTopText);
    	highlightsTopLabel = (TextView)v.findViewById(R.id.highlightsTop);
    	Button b = (Button)v.findViewById(R.id.markAsRead);
    	b.setOnClickListener(new OnClickListener() {
    		@Override
    		public void onClick(View v) {
				unreadTopView.setVisibility(View.GONE);
                if(heartbeatTask != null)
                    heartbeatTask.cancel(true);
                heartbeatTask = new HeartbeatTask();
                heartbeatTask.execute((Void)null);
    		}
    	});
    	globalMsgView = v.findViewById(R.id.globalMessageView);
    	globalMsg = (TextView)v.findViewById(R.id.globalMessageTxt);
    	b = (Button)v.findViewById(R.id.dismissGlobalMessage);
    	b.setOnClickListener(new OnClickListener() {
    		@Override
    		public void onClick(View v) {
    			if(conn != null)
    				conn.globalMsg = null;
    			update_global_msg();
    		}
    	});
    	((ListView)v.findViewById(android.R.id.list)).setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {
				if(pos > 1 && pos <= adapter.data.size()) {
					longPressOverride = mListener.onMessageLongClicked(adapter.data.get(pos - 1));
					return longPressOverride;
				} else {
					return false;
				}
			}
    	});
        spinner = (ProgressBar)v.findViewById(R.id.spinner);
        suggestionsContainer = v.findViewById(R.id.suggestionsContainer);
        suggestions = (GridView)v.findViewById(R.id.suggestions);
        headerViewContainer = getLayoutInflater(null).inflate(R.layout.messageview_header, null);
        headerView = headerViewContainer.findViewById(R.id.progress);
        backlogFailed = (TextView)headerViewContainer.findViewById(R.id.backlogFailed);
        loadBacklogButton = (Button)headerViewContainer.findViewById(R.id.loadBacklogButton);
        loadBacklogButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(conn != null && buffer != null) {
                    backlogFailed.setVisibility(View.GONE);
                    loadBacklogButton.setVisibility(View.GONE);
                    headerView.setVisibility(View.VISIBLE);
                    conn.request_backlog(buffer.cid, buffer.bid, earliest_eid);
                }
            }
        });
        return v;
    }

    public void showSpinner(boolean show) {
        if(show) {
            AlphaAnimation anim = new AlphaAnimation(0, 1);
            anim.setDuration(150);
            anim.setFillAfter(true);
            spinner.setAnimation(anim);
            spinner.setVisibility(View.VISIBLE);
        } else {
            AlphaAnimation anim = new AlphaAnimation(1, 0);
            anim.setDuration(150);
            anim.setFillAfter(true);
            anim.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    spinner.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            spinner.setAnimation(anim);
        }
    }

    private OnScrollListener mOnScrollListener = new OnScrollListener() {
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			if(!ready || buffer == null)
				return;

            if(connecting.getVisibility() == View.VISIBLE)
                return;

			if(headerView != null && buffer.min_eid > 0 && conn.ready) {
				if(firstVisibleItem == 0 && !requestingBacklog && headerView.getVisibility() == View.VISIBLE && conn.getState() == NetworkConnection.STATE_CONNECTED) {
					requestingBacklog = true;
					conn.request_backlog(buffer.cid, buffer.bid, earliest_eid);
				}
			}
			
			if(unreadBottomView != null && adapter != null && adapter.data.size() > 0) {
				if(firstVisibleItem + visibleItemCount == totalItemCount) {
					unreadBottomView.setVisibility(View.GONE);
					if(unreadTopView.getVisibility() == View.GONE) {
	    				if(heartbeatTask != null)
	    					heartbeatTask.cancel(true);
	    				heartbeatTask = new HeartbeatTask();
	    				heartbeatTask.execute((Void)null);
					}
					newMsgs = 0;
					newMsgTime = 0;
					newHighlights = 0;
				}
			}
			if(firstVisibleItem + visibleItemCount < totalItemCount) {
                View v = view.getChildAt(0);
				buffer.scrolledUp = true;
                buffer.scrollPosition = firstVisibleItem;
                buffer.scrollPositionOffset = (v == null)?0:v.getTop();
            } else {
                buffer.scrolledUp = false;
                buffer.scrollPosition = -1;
            }
			if(adapter != null && adapter.data.size() > 0 && unreadTopView != null && unreadTopView.getVisibility() == View.VISIBLE) {
				mUpdateTopUnreadRunnable.run();
				int markerPos = -1;
				if(adapter != null)
					markerPos = adapter.getLastSeenEIDPosition();
	    		if(markerPos > 1 && getListView().getFirstVisiblePosition() <= markerPos) {
	    			unreadTopView.setVisibility(View.GONE);
    				if(heartbeatTask != null)
    					heartbeatTask.cancel(true);
    				heartbeatTask = new HeartbeatTask();
    				heartbeatTask.execute((Void)null);
	    		}
			}
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
		}
		
	};
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        conn = NetworkConnection.getInstance();
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (MessageViewListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement MessageViewListener");
        }
    }
    
    @Override
    public void setArguments(Bundle args) {
        ready = false;
        if(heartbeatTask != null)
            heartbeatTask.cancel(true);
        heartbeatTask = null;
    	if(tapTimerTask != null)
    		tapTimerTask.cancel();
    	tapTimerTask = null;
        buffer = BuffersDataSource.getInstance().getBuffer(args.getInt("bid", -1));
        if(buffer != null) {
            server = ServersDataSource.getInstance().getServer(buffer.cid);
            Crashlytics.log(Log.DEBUG, "IRCCloud", "MessageViewFragment: switched to bid: " + buffer.bid);
        } else {
            Crashlytics.log(Log.WARN, "IRCCloud", "MessageViewFragment: couldn't find buffer to switch to");
        }
		requestingBacklog = false;
		avgInsertTime = 0;
		newMsgs = 0;
		newMsgTime = 0;
		newHighlights = 0;
		earliest_eid = 0;
		backlog_eid = 0;
        currentCollapsedEid = -1;
        lastCollapsedDay = -1;
    	if(server != null) {
    		ignore.setIgnores(server.ignores);
    		if(server.away != null && server.away.length() > 0) {
    			awayTxt.setText(ColorFormatter.html_to_spanned(ColorFormatter.irc_to_html(TextUtils.htmlEncode("Away (" + server.away + ")"))).toString());
    			awayView.setVisibility(View.VISIBLE);
    		} else {
    			awayView.setVisibility(View.GONE);
    		}
			update_status(server.status, server.fail_info);
    	}
		if(unreadTopView != null)
			unreadTopView.setVisibility(View.GONE);
        backlogFailed.setVisibility(View.GONE);
        loadBacklogButton.setVisibility(View.GONE);
        try {
            if (getListView().getHeaderViewsCount() == 0) {
                getListView().addHeaderView(headerViewContainer);
            }
        } catch (IllegalStateException e) {
        }
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)headerView.getLayoutParams();
        lp.topMargin = 0;
        headerView.setLayoutParams(lp);
        lp = (ViewGroup.MarginLayoutParams)backlogFailed.getLayoutParams();
        lp.topMargin = 0;
        backlogFailed.setLayoutParams(lp);
        if(buffer != null && EventsDataSource.getInstance().getEventsForBuffer(buffer.bid) != null) {
            requestingBacklog = true;
            if(refreshTask != null)
                refreshTask.cancel(true);
            refreshTask = new RefreshTask();
            if(args.getBoolean("fade")) {
                Crashlytics.log(Log.DEBUG, "IRCCloud", "MessageViewFragment: Loading message contents in the background");
                refreshTask.execute((Void)null);
            } else {
                Crashlytics.log(Log.DEBUG, "IRCCloud", "MessageViewFragment: Loading message contents");
                refreshTask.onPreExecute();
                refreshTask.onPostExecute(refreshTask.doInBackground());
            }
        } else {
            if(buffer == null || buffer.min_eid == 0 || earliest_eid == buffer.min_eid || conn.getState() != NetworkConnection.STATE_CONNECTED || !conn.ready) {
                headerView.setVisibility(View.GONE);
            } else {
                headerView.setVisibility(View.VISIBLE);
            }
            if(adapter != null) {
                adapter.clear();
                adapter.notifyDataSetInvalidated();
            }
            mListener.onMessageViewReady();
            ready = true;
        }
    }

    private void runOnUiThread(Runnable r) {
        if(getActivity() != null)
            getActivity().runOnUiThread(r);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Crashlytics.log(Log.DEBUG, "IRCCloud", "Received low memory warning in the foreground, cleaning backlog in other buffers");
        for (BuffersDataSource.Buffer b : BuffersDataSource.getInstance().getBuffers()) {
            if (b != buffer)
                EventsDataSource.getInstance().pruneEvents(b.bid);
        }
    }

    private synchronized void insertEvent(final MessageAdapter adapter, EventsDataSource.Event event, boolean backlog, boolean nextIsGrouped) {
        synchronized(adapterLock) {
            try {
                boolean colors = false;
                if(!event.self && conn != null && conn.getUserInfo() != null && conn.getUserInfo().prefs != null && conn.getUserInfo().prefs.has("nick-colors") && conn.getUserInfo().prefs.getBoolean("nick-colors"))
                    colors = true;

                long start = System.currentTimeMillis();
                if(event.eid <= buffer.min_eid) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            headerView.setVisibility(View.GONE);
                            backlogFailed.setVisibility(View.GONE);
                            loadBacklogButton.setVisibility(View.GONE);
                        }
                    });
                }
                if(earliest_eid == 0 || event.eid < earliest_eid)
                    earliest_eid = event.eid;

                String type = event.type;
                long eid = event.eid;

                if(type.startsWith("you_"))
                    type = type.substring(4);

                if(type.equals("joined_channel") || type.equals("parted_channel") || type.equals("nickchange") || type.equals("quit") || type.equals("user_channel_mode") || type.equals("socket_closed") || type.equals("connecting_cancelled") || type.equals("connecting_failed")) {
                    boolean shouldExpand = false;
                    collapsedEvents.showChan = !buffer.type.equals("channel");
                    if(conn != null && conn.getUserInfo() != null && conn.getUserInfo().prefs != null) {
                        JSONObject hiddenMap = null;
                        if(buffer.type.equals("channel")) {
                            if(conn.getUserInfo().prefs.has("channel-hideJoinPart"))
                                hiddenMap = conn.getUserInfo().prefs.getJSONObject("channel-hideJoinPart");
                        } else {
                            if(conn.getUserInfo().prefs.has("buffer-hideJoinPart"))
                                hiddenMap = conn.getUserInfo().prefs.getJSONObject("buffer-hideJoinPart");
                        }

                        if(hiddenMap != null && hiddenMap.has(String.valueOf(buffer.bid)) && hiddenMap.getBoolean(String.valueOf(buffer.bid))) {
                            adapter.removeItem(event.eid);
                            if(!backlog)
                                adapter.notifyDataSetChanged();
                            return;
                        }

                        JSONObject expandMap = null;
                        if(buffer.type.equals("channel")) {
                            if(conn.getUserInfo().prefs.has("channel-expandJoinPart"))
                                expandMap = conn.getUserInfo().prefs.getJSONObject("channel-expandJoinPart");
                        } else if(buffer.type.equals("console")) {
                            if(conn.getUserInfo().prefs.has("buffer-expandDisco"))
                                expandMap = conn.getUserInfo().prefs.getJSONObject("buffer-expandDisco");
                        } else {
                            if(conn.getUserInfo().prefs.has("buffer-expandJoinPart"))
                                expandMap = conn.getUserInfo().prefs.getJSONObject("buffer-expandJoinPart");
                        }

                        if(expandMap != null && expandMap.has(String.valueOf(buffer.bid)) && expandMap.getBoolean(String.valueOf(buffer.bid))) {
                            shouldExpand = true;
                        }
                    }

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(eid / 1000);

                    if(shouldExpand)
                        expandedSectionEids.clear();

                    if(event.type.equals("socket_closed") || event.type.equals("connecting_failed") || event.type.equals("connecting_cancelled")) {
                        EventsDataSource.Event last = EventsDataSource.getInstance().getEvent(lastCollapsedEid, buffer.bid);
                        if(last != null && !last.type.equals("socket_closed") && !last.type.equals("connecting_failed") && !last.type.equals("connecting_cancelled"))
                            currentCollapsedEid = -1;
                    } else {
                        EventsDataSource.Event last = EventsDataSource.getInstance().getEvent(lastCollapsedEid, buffer.bid);
                        if(last != null && (last.type.equals("socket_closed") || last.type.equals("connecting_failed") || last.type.equals("connecting_cancelled")))
                            currentCollapsedEid = -1;
                    }

                    if(currentCollapsedEid == -1 || calendar.get(Calendar.DAY_OF_YEAR) != lastCollapsedDay || shouldExpand) {
                        collapsedEvents.clear();
                        currentCollapsedEid = eid;
                        lastCollapsedDay = calendar.get(Calendar.DAY_OF_YEAR);
                    }

                    if(!collapsedEvents.showChan)
                        event.chan = buffer.name;

                    if(!collapsedEvents.addEvent(event))
                        collapsedEvents.clear();

                    if((currentCollapsedEid == event.eid || shouldExpand) && type.equals("user_channel_mode")) {
                        event.color = R.color.row_message_label;
                        event.bg_color = R.color.status_bg;
                    } else {
                        event.color = R.color.timestamp;
                        event.bg_color = R.color.message_bg;
                    }

                    String msg;
                    if(expandedSectionEids.contains(currentCollapsedEid)) {
                        CollapsedEventsList c = new CollapsedEventsList();
                        c.showChan = collapsedEvents.showChan;
                        c.setServer(server);
                        c.addEvent(event);
                        msg = c.getCollapsedMessage();
                        if(!nextIsGrouped) {
                            String group_msg = collapsedEvents.getCollapsedMessage();
                            if(group_msg == null && type.equals("nickchange")) {
                                group_msg = event.old_nick + " → <b>" + event.nick + "</b>";
                            }
                            if(group_msg == null && type.equals("user_channel_mode")) {
                                if(event.from != null && event.from.length() > 0)
                                    msg = collapsedEvents.formatNick(event.nick, event.target_mode, false) + " was set to <b>" + event.diff + "</b> by <b>" + collapsedEvents.formatNick(event.from, event.from_mode, false) + "</b>";
                                else
                                    msg = collapsedEvents.formatNick(event.nick, event.target_mode, false) + " was set to <b>" + event.diff + "</b> by the server <b>" + event.server + "</b>";
                                currentCollapsedEid = eid;
                            }
                            EventsDataSource.Event heading = new EventsDataSource.Event();
                            heading.type = "__expanded_group_heading__";
                            heading.cid = event.cid;
                            heading.bid = event.bid;
                            heading.eid = currentCollapsedEid - 1;
                            heading.group_msg = group_msg;
                            heading.color = R.color.timestamp;
                            heading.bg_color = R.color.message_bg;
                            heading.linkify = false;
                            adapter.addItem(currentCollapsedEid - 1, heading);
                            if(event.type.equals("socket_closed") || event.type.equals("connecting_failed") || event.type.equals("connecting_cancelled")) {
                                EventsDataSource.Event last = EventsDataSource.getInstance().getEvent(lastCollapsedEid, buffer.bid);
                                if(last != null)
                                    last.row_type = ROW_MESSAGE;
                                event.row_type = ROW_SOCKETCLOSED;
                            }
                        }
                        event.timestamp = null;
                    } else {
                        msg = (nextIsGrouped && currentCollapsedEid != event.eid)?"":collapsedEvents.getCollapsedMessage();
                    }

                    if(msg == null && type.equals("nickchange")) {
                        msg = event.old_nick + " → <b>" + event.nick + "</b>";
                    }
                    if(msg == null && type.equals("user_channel_mode")) {
                        if(event.from != null && event.from.length() > 0)
                            msg = collapsedEvents.formatNick(event.nick, event.target_mode, false) + " was set to <b>" + event.diff + "</b> by <b>" + collapsedEvents.formatNick(event.from, event.from_mode, false) + "</b>";
                        else
                            msg = collapsedEvents.formatNick(event.nick, event.target_mode, false) + " was set to <b>" + event.diff + "</b> by the server <b>" + event.server + "</b>";
                        currentCollapsedEid = eid;
                    }
                    if(!expandedSectionEids.contains(currentCollapsedEid)) {
                        if(eid != currentCollapsedEid) {
                            event.color = R.color.timestamp;
                            event.bg_color = R.color.message_bg;
                        }
                        eid = currentCollapsedEid;
                    }
                    event.group_msg = msg;
                    event.html = null;
                    event.formatted = null;
                    event.linkify = false;
                    lastCollapsedEid = event.eid;
                    if(buffer.type.equals("console") && !event.type.equals("socket_closed") && !event.type.equals("connecting_failed") && !event.type.equals("connecting_cancelled")) {
                        currentCollapsedEid = -1;
                        lastCollapsedEid = -1;
                        collapsedEvents.clear();
                    }
                } else {
                    currentCollapsedEid = -1;
                    lastCollapsedEid = -1;
                    collapsedEvents.clear();
                    if(event.html == null) {
                        if(event.from != null && event.from.length() > 0)
                            event.html = "<b>" + collapsedEvents.formatNick(event.from, event.from_mode, colors) + "</b> " + event.msg;
                        else if(event.type.equals("buffer_msg") && event.server != null && event.server.length() > 0)
                            event.html = "<b>" + event.server + "</b> " + event.msg;
                        else
                            event.html = event.msg;
                    }
                }

                String from = event.from;
                if(from == null || from.length() == 0)
                    from = event.nick;

                if(from != null && event.hostmask != null && (type.equals("buffer_msg") || type.equals("buffer_me_msg") || type.equals("notice") || type.equals("channel_invite") || type.equals("callerid") || type.equals("wallops")) && buffer.type != null && !buffer.type.equals("conversation")) {
                    String usermask = from + "!" + event.hostmask;
                    if(ignore.match(usermask)) {
                        if(unreadTopView != null && unreadTopView.getVisibility() == View.GONE && unreadBottomView != null && unreadBottomView.getVisibility() == View.GONE) {
                            if(heartbeatTask != null)
                                heartbeatTask.cancel(true);
                            heartbeatTask = new HeartbeatTask();
                            heartbeatTask.execute((Void)null);
                        }
                        return;
                    }
                }

                if(type.equals("channel_mode")) {
                    if(event.nick != null && event.nick.length() > 0)
                        event.html = event.msg + " by <b>" + collapsedEvents.formatNick(event.nick, event.from_mode, false) + "</b>";
                    else if(event.server != null && event.server.length() > 0)
                        event.html = event.msg + " by the server <b>" + event.server + "</b>";
                } else if(type.equals("buffer_me_msg")) {
                    event.html = "— <i><b>" + collapsedEvents.formatNick(event.nick, event.from_mode, colors) + "</b> " + event.msg + "</i>";
                } else if(type.equals("notice")) {
                    if(event.from != null && event.from.length() > 0)
                        event.html = "<b>" + collapsedEvents.formatNick(event.from, event.from_mode, false) + "</b> ";
                    else
                        event.html = "";
                    if(buffer.type.equals("console") && event.to_chan && event.chan != null && event.chan.length() > 0) {
                        event.html += event.chan + "&#xfe55; " + event.msg;
                    } else {
                        event.html += event.msg;
                    }
                } else if(type.equals("kicked_channel")) {
                    event.html = "← ";
                    if(event.type.startsWith("you_"))
                        event.html += "You";
                    else
                        event.html += "<b>" + collapsedEvents.formatNick(event.old_nick, null, false) + "</b>";
                    if(event.type.startsWith("you_"))
                        event.html += " were";
                    else
                        event.html += " was";
                    if(event.hostmask != null && event.hostmask.length() > 0)
                        event.html += " kicked by <b>" + collapsedEvents.formatNick(event.nick, event.from_mode, false) + "</b> (" + event.hostmask + ")";
                    else
                        event.html += " kicked by the server <b>" + event.nick + "</b>";
                    if(event.msg != null && event.msg.length() > 0)
                        event.html += ": " + event.msg;
                } else if(type.equals("callerid")) {
                    event.html = "<b>" + collapsedEvents.formatNick(event.from, event.from_mode, false) + "</b> ("+ event.hostmask + ") " + event.msg + " Tap to accept.";
                } else if(type.equals("channel_mode_list_change")) {
                    if(event.from.length() == 0) {
                        if(event.nick != null && event.nick.length() > 0)
                            event.html = event.msg + " by <b>" + collapsedEvents.formatNick(event.nick, event.from_mode, false) + "</b>";
                        else if(event.server != null && event.server.length() > 0)
                            event.html = event.msg + " by the server <b>" + event.server + "</b>";
                    }
                }

                adapter.addItem(eid, event);
                if(!backlog)
                    adapter.notifyDataSetChanged();

                long time = (System.currentTimeMillis() - start);
                if(avgInsertTime == 0)
                    avgInsertTime = time;
                avgInsertTime += time;
                avgInsertTime /= 2.0;
                //Log.i("IRCCloud", "Average insert time: " + avgInsertTime);
                if(!backlog && buffer.scrolledUp && !event.self && event.isImportant(type)) {
                    if(newMsgTime == 0)
                        newMsgTime = System.currentTimeMillis();
                    newMsgs++;
                    if(event.highlight)
                        newHighlights++;
                    update_unread();
                }
                if(!backlog && !buffer.scrolledUp) {
                    getListView().setSelection(adapter.getCount() - 1);
                    tapTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        getListView().setSelection(adapter.getCount() - 1);
                                    } catch (Exception e) {
                                        //List view isn't ready yet
                                    }
                                }
                            });
                        }
                    }, 200);
                }

                if(!backlog && event.highlight && !getActivity().getSharedPreferences("prefs", 0).getBoolean("mentionTip", false)) {
                    Toast.makeText(getActivity(), "Double-tap a message to quickly reply to the sender", Toast.LENGTH_LONG).show();
                    SharedPreferences.Editor editor = getActivity().getSharedPreferences("prefs", 0).edit();
                    editor.putBoolean("mentionTip", true);
                    editor.commit();
                }
                if(!backlog) {
                    int markerPos = adapter.getLastSeenEIDPosition();
                    if(markerPos > 0 && getListView().getFirstVisiblePosition() > markerPos) {
                        unreadTopLabel.setText((getListView().getFirstVisiblePosition() - markerPos) + " unread messages");
                    }
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    private class OnItemClickListener implements OnClickListener {
        private int pos;
        OnItemClickListener(int position){
            pos = position;
        }
        
        @Override
        public void onClick(View arg0) {
        	longPressOverride = false;
        	
	    	if(pos < 0 || pos >= adapter.data.size())
	    		return;

	    	if(adapter != null) {
	    		if(tapTimerTask != null) {
	    			tapTimerTask.cancel();
	    			tapTimerTask = null;
	    			mListener.onMessageDoubleClicked(adapter.data.get(pos));
	    		} else {
                    tapTimerTask = new TimerTask() {
                        int position = pos;

                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (adapter != null && adapter.data != null && position < adapter.data.size()) {
                                        EventsDataSource.Event e = adapter.data.get(position);
                                        if (e != null) {
                                            if (e.type.equals("channel_invite")) {
                                                conn.join(buffer.cid, e.old_nick, null);
                                            } else if (e.type.equals("callerid")) {
                                                conn.say(buffer.cid, null, "/accept " + e.from);
                                                BuffersDataSource.Buffer b = BuffersDataSource.getInstance().getBufferByName(buffer.cid, e.from);
                                                if (b != null) {
                                                    mListener.onBufferSelected(b.bid);
                                                } else {
                                                    conn.say(buffer.cid, null, "/query " + e.from);
                                                }
                                            } else {
                                                long group = e.group_eid;
                                                if (expandedSectionEids.contains(group))
                                                    expandedSectionEids.remove(group);
                                                else if (e.eid != group)
                                                    expandedSectionEids.add(group);
                                                if (e.eid != e.group_eid) {
                                                    if (refreshTask != null)
                                                        refreshTask.cancel(true);
                                                    refreshTask = new RefreshTask();
                                                    refreshTask.execute((Void) null);
                                                }
                                            }
                                        }
                                    }
                                }
                            });
                            tapTimerTask = null;
                        }
                    };
	    			tapTimer.schedule(tapTimerTask, 300);
	    		}
	    	}
    	}
    }
    
    @SuppressWarnings("unchecked")
	public void onResume() {
    	super.onResume();
    	conn.addHandler(this);
    	if(NetworkConnection.getInstance().getState() != NetworkConnection.STATE_CONNECTED || !NetworkConnection.getInstance().ready) {
			TranslateAnimation anim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -1, Animation.RELATIVE_TO_SELF, 0);
			anim.setDuration(200);
			anim.setFillAfter(true);
			connecting.startAnimation(anim);
			connecting.setVisibility(View.VISIBLE);
        }
        getListView().requestFocus();
        getListView().setOnScrollListener(mOnScrollListener);
    	updateReconnecting();
    	update_global_msg();
    }
    
    private class HeartbeatTask extends AsyncTaskEx<Void, Void, Void> {

    	@Override
    	protected void onPreExecute() {
            //if(buffer != null)
            //    Log.d("IRCCloud", "Heartbeat task created. Ready: " + ready + " BID: " + buffer.bid);
    	}
    	
		@Override
		protected Void doInBackground(Void... params) {
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
			}

			if(isCancelled())
				return null;

            if(connecting.getVisibility() == View.VISIBLE)
                return null;

            try {
                TreeMap<Long, EventsDataSource.Event> events = EventsDataSource.getInstance().getEventsForBuffer(buffer.bid);
                if(events != null && events.size() > 0) {
                    Long eid = events.get(events.lastKey()).eid;

                    if(eid >= buffer.last_seen_eid && conn != null && conn.getState() == NetworkConnection.STATE_CONNECTED) {
                        if(getActivity() != null && getActivity().getIntent() != null)
                            getActivity().getIntent().putExtra("last_seen_eid", eid);
                        NetworkConnection.getInstance().heartbeat(buffer.cid, buffer.bid, eid);
                        BuffersDataSource.getInstance().updateLastSeenEid(buffer.bid, eid);
                        buffer.unread = 0;
                        buffer.highlights = 0;
                    }
                }
            } catch (Exception e) {
            }
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if(!isCancelled())
				heartbeatTask = null;
		}
    }

    private class FormatTask extends AsyncTaskEx<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... params) {
            adapter.format();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }
    }

    private class RefreshTask extends AsyncTaskEx<Void, Void, Void> {
        private MessageAdapter adapter;

        TreeMap<Long,EventsDataSource.Event> events;
        BuffersDataSource.Buffer buffer;
        int oldPosition = -1;
        int topOffset = -1;

        @Override
        protected void onPreExecute() {
            //Debug.startMethodTracing("refresh");
            try {
                oldPosition = getListView().getFirstVisiblePosition();
                View v = getListView().getChildAt(0);
                topOffset = (v == null) ? 0 : v.getTop();
                buffer = MessageViewFragment.this.buffer;
            } catch (IllegalStateException e) {
                //The list view isn't on screen anymore
                cancel(true);
                refreshTask = null;
            }
        }

        @SuppressWarnings("unchecked")
		@Override
		protected Void doInBackground(Void... params) {
            TreeMap<Long,EventsDataSource.Event> evs = null;
			long time = System.currentTimeMillis();
            if(buffer != null)
    			evs = EventsDataSource.getInstance().getEventsForBuffer(buffer.bid);
			Log.i("IRCCloud", "Loaded data in " + (System.currentTimeMillis() - time) + "ms");
			if(!isCancelled() && evs != null && evs.size() > 0) {
    			events = (TreeMap<Long, EventsDataSource.Event>)evs.clone();
                if(isCancelled())
                    return null;

                if(events != null && events.size() > 0) {
                    try {
                        if (MessageViewFragment.this.adapter != null && MessageViewFragment.this.adapter.data.size() > 0 && earliest_eid > events.firstKey()) {
                            if (oldPosition > 0 && oldPosition == MessageViewFragment.this.adapter.data.size())
                                oldPosition--;
                            EventsDataSource.Event e = MessageViewFragment.this.adapter.data.get(oldPosition);
                            if (e != null)
                                backlog_eid = e.group_eid - 1;
                            else
                                backlog_eid = -1;
                            if (backlog_eid < 0) {
                                backlog_eid = MessageViewFragment.this.adapter.getItemId(oldPosition) - 1;
                            }
                            EventsDataSource.Event backlogMarker = new EventsDataSource.Event();
                            backlogMarker.eid = backlog_eid;
                            backlogMarker.type = TYPE_BACKLOGMARKER;
                            backlogMarker.row_type = ROW_BACKLOGMARKER;
                            backlogMarker.html = "__backlog__";
                            backlogMarker.bg_color = R.color.message_bg;
                            events.put(backlog_eid, backlogMarker);
                        }
                        adapter = new MessageAdapter(MessageViewFragment.this);
                        refresh(adapter, events);
                    } catch (IndexOutOfBoundsException e) {
                        e.printStackTrace();
                        return null;
                    } catch (IllegalStateException e) {
                        //The list view doesn't exist yet
                        e.printStackTrace();
                        Log.e("IRCCloud", "Tried to refresh the message list, but it didn't exist.");
                    }
                }
            } else if(buffer != null && buffer.min_eid > 0 && conn.ready && conn.getState() == NetworkConnection.STATE_CONNECTED && !isCancelled()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        headerView.setVisibility(View.VISIBLE);
                        backlogFailed.setVisibility(View.GONE);
                        loadBacklogButton.setVisibility(View.GONE);
                        adapter = new MessageAdapter(MessageViewFragment.this);
                        setListAdapter(adapter);
                        MessageViewFragment.this.adapter = adapter;
                        requestingBacklog = true;
                        conn.request_backlog(buffer.cid, buffer.bid, 0);
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        headerView.setVisibility(View.GONE);
                        backlogFailed.setVisibility(View.GONE);
                        loadBacklogButton.setVisibility(View.GONE);
                        adapter = new MessageAdapter(MessageViewFragment.this);
                        setListAdapter(adapter);
                        MessageViewFragment.this.adapter = adapter;
                    }});
            }
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
            if(!isCancelled() && adapter != null) {
                try {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) headerView.getLayoutParams();
                    if(adapter.getLastSeenEIDPosition() == 0)
                        lp.topMargin = (int)getSafeResources().getDimension(R.dimen.top_bar_height);
                    else
                        lp.topMargin = 0;
                    headerView.setLayoutParams(lp);
                    lp = (ViewGroup.MarginLayoutParams)backlogFailed.getLayoutParams();
                    if(adapter.getLastSeenEIDPosition() == 0)
                        lp.topMargin = (int)getSafeResources().getDimension(R.dimen.top_bar_height);
                    else
                        lp.topMargin = 0;
                    backlogFailed.setLayoutParams(lp);
                    setListAdapter(adapter);
                    MessageViewFragment.this.adapter = adapter;
                    if(events != null && events.size() > 0) {
                        int markerPos = adapter.getBacklogMarkerPosition();
                        if(markerPos != -1 && requestingBacklog)
                            getListView().setSelectionFromTop(oldPosition + markerPos + 1, headerViewContainer.getHeight());
                        else if(!buffer.scrolledUp)
                            getListView().setSelection(adapter.getCount() - 1);
                        else {
                            getListView().setSelectionFromTop(buffer.scrollPosition, buffer.scrollPositionOffset);

                            if(adapter.getLastSeenEIDPosition() > buffer.scrollPosition) {
                                newMsgs = 0;
                                newHighlights = 0;

                                for(int i = adapter.data.size() - 1; i >= 0; i--) {
                                    EventsDataSource.Event e = adapter.data.get(i);
                                    if(e.eid <= buffer.last_seen_eid)
                                        break;

                                    if(e.isImportant(buffer.type)) {
                                        if(e.highlight)
                                            newHighlights++;
                                        else
                                            newMsgs++;
                                    }
                                }
                            }

                            update_unread();
                        }
                    }
                    new FormatTask().execute((Void)null);
                } catch (IllegalStateException e) {
                    //The list view isn't on screen anymore
                    e.printStackTrace();
                }
                refreshTask = null;
                requestingBacklog = false;
                //Debug.stopMethodTracing();
            }
        }
	}

	private synchronized void refresh(MessageAdapter adapter, TreeMap<Long,EventsDataSource.Event> events) {
        synchronized (adapterLock) {
            if(getActivity() != null)
                textSize = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt("textSize", 14);
            timestamp_width = -1;
            if(conn.getReconnectTimestamp() == 0)
                conn.cancel_idle_timer(); //This may take a while...
            collapsedEvents.clear();
            currentCollapsedEid = -1;
            lastCollapsedDay = -1;

            if(events == null || (events.size() == 0 && buffer.min_eid > 0)) {
                if(buffer != null && conn != null && conn.getState() == NetworkConnection.STATE_CONNECTED) {
                    requestingBacklog = true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            conn.request_backlog(buffer.cid, buffer.bid, 0);
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            headerView.setVisibility(View.GONE);
                            backlogFailed.setVisibility(View.GONE);
                            loadBacklogButton.setVisibility(View.GONE);
                        }
                    });
                }
            } else if(events.size() > 0) {
                if(server != null) {
                    ignore.setIgnores(server.ignores);
                } else {
                    ignore.setIgnores(null);
                }
                collapsedEvents.setServer(server);
                earliest_eid = events.firstKey();
                if(events.firstKey() > buffer.min_eid && buffer.min_eid > 0 && conn != null && conn.getState() == NetworkConnection.STATE_CONNECTED) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            headerView.setVisibility(View.VISIBLE);
                            backlogFailed.setVisibility(View.GONE);
                            loadBacklogButton.setVisibility(View.GONE);
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            headerView.setVisibility(View.GONE);
                            backlogFailed.setVisibility(View.GONE);
                            loadBacklogButton.setVisibility(View.GONE);
                        }
                    });
                }
                if(events.size() > 0) {
                    avgInsertTime = 0;
                    //Debug.startMethodTracing("refresh");
                    long start = System.currentTimeMillis();
                    Iterator<EventsDataSource.Event> i = events.values().iterator();
                    EventsDataSource.Event next = i.next();
                    Calendar calendar = Calendar.getInstance();
                    while(next != null) {
                        EventsDataSource.Event e = next;
                        next = i.hasNext()?i.next():null;
                        String type = (next == null)?"":next.type;

                        if(next != null && currentCollapsedEid != -1 && !expandedSectionEids.contains(currentCollapsedEid) && (type.equalsIgnoreCase("joined_channel") || type.equalsIgnoreCase("parted_channel") || type.equalsIgnoreCase("nickchange") || type.equalsIgnoreCase("quit") || type.equalsIgnoreCase("user_channel_mode"))) {
                            calendar.setTimeInMillis(next.eid / 1000);
                            insertEvent(adapter, e, true, calendar.get(Calendar.DAY_OF_YEAR) == lastCollapsedDay);
                        } else {
                            insertEvent(adapter, e, true, false);
                        }
                    }
                    adapter.insertLastSeenEIDMarker();
                    Log.i("IRCCloud", "Backlog rendering took: " + (System.currentTimeMillis() - start) + "ms");
                    //Debug.stopMethodTracing();
                    avgInsertTime = 0;
                    //adapter.notifyDataSetChanged();
                }
            }
            mHandler.removeCallbacks(mUpdateTopUnreadRunnable);
            mHandler.postDelayed(mUpdateTopUnreadRunnable, 100);
            if(conn.getReconnectTimestamp() == 0 && conn.getState() == NetworkConnection.STATE_CONNECTED)
                conn.schedule_idle_timer();
        }
	}

	private Runnable mUpdateTopUnreadRunnable = new Runnable() {
		@Override
		public void run() {
			if(adapter != null) {
				try {
					int markerPos = adapter.getLastSeenEIDPosition();
		    		if(markerPos >= 0 && getListView().getFirstVisiblePosition() > (markerPos + 1)) {
		    			if(shouldTrackUnread()) {
		    				int highlights = adapter.getUnreadHighlightsAbovePosition(getListView().getFirstVisiblePosition());
		    				int count = (getListView().getFirstVisiblePosition() - markerPos - 1) - highlights;
		    				String txt = "";
		    				if(highlights > 0) {
			    				if(highlights == 1)
			    					txt = "mention";
			    				else if(highlights > 0)
			    					txt = "mentions";
			    				highlightsTopLabel.setText(String.valueOf(highlights));
		    					highlightsTopLabel.setVisibility(View.VISIBLE);
		    					
		    					if(count > 0)
		    						txt += " and ";
		    				} else {
		    					highlightsTopLabel.setVisibility(View.GONE);
		    				}
		    				if(markerPos == 0) {
		    			        long seconds = (long)Math.ceil((earliest_eid - buffer.last_seen_eid) / 1000000.0);
                                if(seconds < 0) {
                                    if(count < 0) {
                                        unreadTopView.setVisibility(View.GONE);
                                        return;
                                    } else {
                                        if(count == 1)
                                            txt += count + " unread message";
                                        else if(count > 0)
                                            txt += count + " unread messages";
                                    }
                                } else {
                                    int minutes = (int)Math.ceil(seconds / 60.0);
                                    int hours = (int)Math.ceil(seconds / 60.0 / 60.0);
                                    int days = (int)Math.ceil(seconds / 60.0 / 60.0 / 24.0);
                                    if(hours >= 24) {
                                        if(days == 1)
                                            txt += days + " day of unread messages";
                                        else
                                            txt += days + " days of unread messages";
                                    } else if(hours > 0) {
                                        if(hours == 1)
                                            txt += hours + " hour of unread messages";
                                        else
                                            txt += hours + " hours of unread messages";
                                    } else if(minutes > 0) {
                                        if(minutes == 1)
                                            txt += minutes + " minute of unread messages";
                                        else
                                            txt += minutes + " minutes of unread messages";
                                    } else {
                                        if(seconds == 1)
                                            txt += seconds + " second of unread messages";
                                        else
                                            txt += seconds + " seconds of unread messages";
                                    }
                                }
		    				} else {
			    				if(count == 1)
			    					txt += count + " unread message";
			    				else if(count > 0)
			    					txt += count + " unread messages";
		    				}
			    			unreadTopLabel.setText(txt);
			    			unreadTopView.setVisibility(View.VISIBLE);
		    			} else {
			    			unreadTopView.setVisibility(View.GONE);
		    			}
		    		} else {
                        if(markerPos > 0) {
                            unreadTopView.setVisibility(View.GONE);
                            if(adapter.data.size() > 0) {
                                if(heartbeatTask != null)
                                    heartbeatTask.cancel(true);
                                heartbeatTask = new HeartbeatTask();
                                heartbeatTask.execute((Void)null);
                            }
                        }
		    		}
		    		if(server != null)
		    			update_status(server.status, server.fail_info);
					if(mListener != null && !ready)
						mListener.onMessageViewReady();
					ready = true;
				} catch (IllegalStateException e) {
					//The list view wasn't on screen yet
                    e.printStackTrace();
				}
			}
		}
	};
	
	private boolean shouldTrackUnread() {
		if(conn != null && conn.getUserInfo() != null && conn.getUserInfo().prefs != null && conn.getUserInfo().prefs.has("channel-disableTrackUnread")) {
			try {
				JSONObject disabledMap = conn.getUserInfo().prefs.getJSONObject("channel-disableTrackUnread");
				if(disabledMap.has(String.valueOf(buffer.bid)) && disabledMap.getBoolean(String.valueOf(buffer.bid))) {
					return false;
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return true;
	}
	
	private class UnreadRefreshRunnable implements Runnable {
		@Override
		public void run() {
			update_unread();
		}
	}
	
	UnreadRefreshRunnable unreadRefreshRunnable = null;

	private void update_unread() {
		if(unreadRefreshRunnable != null) {
			mHandler.removeCallbacks(unreadRefreshRunnable);
			unreadRefreshRunnable = null;
		}

		if(newMsgs > 0) {
			/*int minutes = (int)((System.currentTimeMillis() - newMsgTime)/60000);
			
			if(minutes < 1)
				unreadBottomLabel.setText("Less than a minute of chatter (");
			else if(minutes == 1)
				unreadBottomLabel.setText("1 minute of chatter (");
			else
				unreadBottomLabel.setText(minutes + " minutes of chatter (");
			if(newMsgs == 1)
				unreadBottomLabel.setText(unreadBottomLabel.getText() + "1 message)");
			else
				unreadBottomLabel.setText(unreadBottomLabel.getText() + (newMsgs + " messages)"));*/
			
			String txt = "";
			int msgCnt = newMsgs - newHighlights;
			if(newHighlights > 0) {
				if(newHighlights == 1)
					txt = "mention";
				else
					txt = "mentions";
				if(msgCnt > 0)
					txt += " and ";
				highlightsBottomLabel.setText(String.valueOf(newHighlights));
				highlightsBottomLabel.setVisibility(View.VISIBLE);
			} else {
				highlightsBottomLabel.setVisibility(View.GONE);
			}
			if(msgCnt == 1)
				txt += msgCnt + " unread message";
			else if(msgCnt > 0)
				txt += msgCnt + " unread messages";
			unreadBottomLabel.setText(txt);
			unreadBottomView.setVisibility(View.VISIBLE);
			unreadRefreshRunnable = new UnreadRefreshRunnable();
			mHandler.postDelayed(unreadRefreshRunnable, 10000);
		}
	}
	
	private class StatusRefreshRunnable implements Runnable {
		String status;
		JsonNode fail_info;
		
		public StatusRefreshRunnable(String status, JsonNode fail_info) {
			this.status = status;
			this.fail_info = fail_info;
		}

		@Override
		public void run() {
			update_status(status, fail_info);
		}
	}
	
	StatusRefreshRunnable statusRefreshRunnable = null;

    public static String ordinal(int i) {
        String[] sufixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
        switch (i % 100) {
            case 11:
            case 12:
            case 13:
                return i + "th";
            default:
                return i + sufixes[i % 10];

        }
    }

    private String reason_txt(String reason) {
        String r = reason;
        if(reason.equalsIgnoreCase("pool_lost")) {
            r = "Connection pool failed";
        } else if(reason.equalsIgnoreCase("no_pool")) {
            r = "No available connection pools";
        } else if(reason.equalsIgnoreCase("enetdown")) {
            r = "Network down";
        } else if(reason.equalsIgnoreCase("etimedout") || reason.equalsIgnoreCase("timeout")) {
            r = "Timed out";
        } else if(reason.equalsIgnoreCase("ehostunreach")) {
            r = "Host unreachable";
        } else if(reason.equalsIgnoreCase("econnrefused")) {
            r = "Connection refused";
        } else if(reason.equalsIgnoreCase("nxdomain") || reason.equalsIgnoreCase("einval")) {
            r = "Invalid hostname";
        } else if(reason.equalsIgnoreCase("server_ping_timeout")) {
            r = "PING timeout";
        } else if(reason.equalsIgnoreCase("ssl_certificate_error")) {
            r = "SSL certificate error";
        } else if(reason.equalsIgnoreCase("ssl_error")) {
            r = "SSL error";
        } else if(reason.equalsIgnoreCase("crash")) {
            r = "Connection crashed";
        } else if(reason.equalsIgnoreCase("networks")) {
            r = "You've exceeded the connection limit for free accounts.";
        } else if(reason.equalsIgnoreCase("passworded_servers")) {
            r = "You can't connect to passworded servers with free accounts.";
        } else if(reason.equalsIgnoreCase("unverified")) {
            r = "You can’t connect to external servers until you confirm your email address.";
        }
        return r;
    }

	private void update_status(String status, JsonNode fail_info) {
		if(statusRefreshRunnable != null) {
			mHandler.removeCallbacks(statusRefreshRunnable);
			statusRefreshRunnable = null;
		}
		
    	if(status.equals("connected_ready")) {
    		if(server != null && server.lag >= 2*1000*1000) {
	    		statusView.setVisibility(View.VISIBLE);
	    		statusView.setText("Slow ping response from " + server.hostname + " (" + (server.lag / 1000 / 1000) + "s)");
    		} else {
	    		statusView.setVisibility(View.GONE);
	    		statusView.setText("");
    		}
    	} else if(status.equals("quitting")) {
    		statusView.setVisibility(View.VISIBLE);
    		statusView.setText("Disconnecting");
    		statusView.setTextColor(getSafeResources().getColor(R.color.dark_blue));
    		statusView.setBackgroundResource(R.drawable.background_blue);
    	} else if(status.equals("disconnected")) {
            statusView.setVisibility(View.VISIBLE);
            if(fail_info.has("type") && fail_info.get("type").asText().length() > 0) {
                String text = "Disconnected: ";
                if(fail_info.get("type").asText().equals("connecting_restricted")) {
                    text = reason_txt(fail_info.get("reason").asText());
                    if(text.equals(fail_info.get("reason").asText()))
                        text = "You can’t connect to this server with a free account.";
                } else if(fail_info.get("type").asText().equals("connection_blocked")) {
                    text = "Disconnected - Connections to this server have been blocked";
                } else {
                    if(fail_info.has("type") && fail_info.get("type").asText().equals("killed"))
                        text = "Disconnected - Killed: ";
                    else if(fail_info.has("type") && fail_info.get("type").asText().equals("connecting_failed"))
                        text = "Disconnected: Failed to connect - ";
                    if(fail_info.has("reason"))
                        text += reason_txt(fail_info.get("reason").asText());
                }
                statusView.setText(text);
                statusView.setTextColor(getSafeResources().getColor(R.color.status_fail_text));
                statusView.setBackgroundResource(R.drawable.status_fail_bg);
            } else {
                statusView.setText("Disconnected. Tap to reconnect.");
                statusView.setTextColor(getSafeResources().getColor(R.color.dark_blue));
                statusView.setBackgroundResource(R.drawable.background_blue);
            }
    	} else if(status.equals("queued")) {
    		statusView.setVisibility(View.VISIBLE);
    		statusView.setText("Connection queued");
    		statusView.setTextColor(getSafeResources().getColor(R.color.dark_blue));
    		statusView.setBackgroundResource(R.drawable.background_blue);
    	} else if(status.equals("connecting")) {
    		statusView.setVisibility(View.VISIBLE);
    		statusView.setText("Connecting");
    		statusView.setTextColor(getSafeResources().getColor(R.color.dark_blue));
    		statusView.setBackgroundResource(R.drawable.background_blue);
    	} else if(status.equals("connected")) {
    		statusView.setVisibility(View.VISIBLE);
    		statusView.setText("Connected");
    		statusView.setTextColor(getSafeResources().getColor(R.color.dark_blue));
    		statusView.setBackgroundResource(R.drawable.background_blue);
    	} else if(status.equals("connected_joining")) {
    		statusView.setVisibility(View.VISIBLE);
    		statusView.setText("Connected: Joining Channels");
    		statusView.setTextColor(getSafeResources().getColor(R.color.dark_blue));
    		statusView.setBackgroundResource(R.drawable.background_blue);
    	} else if(status.equals("pool_unavailable")) {
    		statusView.setVisibility(View.VISIBLE);
    		statusView.setText("Connection temporarily unavailable");
    		statusView.setTextColor(getSafeResources().getColor(R.color.status_fail_text));
    		statusView.setBackgroundResource(R.drawable.status_fail_bg);
    	} else if(status.equals("waiting_to_retry")) {
    		try {
	    		statusView.setVisibility(View.VISIBLE);
	    		long seconds = (fail_info.get("timestamp").asLong() + fail_info.get("retry_timeout").asLong() - conn.clockOffset) - System.currentTimeMillis()/1000;
	    		if(seconds > 0) {
		    		String text = "Disconnected";
		    		if(fail_info.has("reason") && fail_info.get("reason").asText().length() > 0) {
                        String reason = fail_info.get("reason").asText();
                        reason = reason_txt(reason);
		    			text += ": " + reason + ". ";
                    } else
		    			text += "; ";
                    text += "Reconnecting in ";
                    int minutes = (int)(seconds / 60.0);
                    int hours = (int)(seconds / 60.0 / 60.0);
                    int days = (int)(seconds / 60.0 / 60.0 / 24.0);
                    if(days > 0) {
                        if(days == 1)
                            text += days + " day.";
                        else
                            text += days + " days.";
                    } else if(hours > 0) {
                        if(hours == 1)
                            text += hours + " hour.";
                        else
                            text += hours + " hours.";
                    } else if(minutes > 0) {
                        if(minutes == 1)
                            text += minutes + " minute.";
                        else
                            text += minutes + " minutes.";
                    } else {
                        if(seconds == 1)
                            text += seconds + " second.";
                        else
                            text += seconds + " seconds.";
                    }
                    int attempts = fail_info.get("attempts").asInt();
                    if(attempts > 1)
                        text += " (" + ordinal(attempts) + " attempt)";
		    		statusView.setText(text);
		    		statusView.setTextColor(getSafeResources().getColor(R.color.status_fail_text));
		    		statusView.setBackgroundResource(R.drawable.status_fail_bg);
		    		statusRefreshRunnable = new StatusRefreshRunnable(status, fail_info);
	    		} else {
	        		statusView.setVisibility(View.VISIBLE);
	        		statusView.setText("Ready to connect, waiting our turn…");
	        		statusView.setTextColor(getSafeResources().getColor(R.color.dark_blue));
	        		statusView.setBackgroundResource(R.drawable.background_blue);
	    		}
	    		mHandler.postDelayed(statusRefreshRunnable, 500);
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	} else if(status.equals("ip_retry")) {
    		statusView.setVisibility(View.VISIBLE);
    		statusView.setText("Trying another IP address");
    		statusView.setTextColor(getSafeResources().getColor(R.color.dark_blue));
    		statusView.setBackgroundResource(R.drawable.background_blue);
    	}
	}

	private void update_global_msg() {
		if(globalMsgView != null) {
			if(conn != null && conn.globalMsg != null) {
				globalMsg.setText(Html.fromHtml(conn.globalMsg));
				globalMsgView.setVisibility(View.VISIBLE);
			} else {
				globalMsgView.setVisibility(View.GONE);
			}
		}
	}
	
	@Override
    public void onPause() {
    	super.onPause();
		if(statusRefreshRunnable != null) {
			mHandler.removeCallbacks(statusRefreshRunnable);
			statusRefreshRunnable = null;
		}
    	if(conn != null)
    		conn.removeHandler(this);
		TranslateAnimation anim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -1, Animation.RELATIVE_TO_SELF, -1);
		anim.setDuration(10);
		anim.setFillAfter(true);
		anim.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation arg0) {
				connecting.setVisibility(View.GONE);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationStart(Animation animation) {
			}
			
		});
		connecting.startAnimation(anim);
		error = null;
		try {
			getListView().setOnScrollListener(null);
		} catch (Exception e) {
		}
   	}

    private void updateReconnecting() {
		if(conn.getState() == NetworkConnection.STATE_CONNECTED) {
			connectingMsg.setText("Loading");
		} else if(conn.getState() == NetworkConnection.STATE_CONNECTING || conn.getReconnectTimestamp() > 0) {
            progressBar.setProgress(0);
			progressBar.setIndeterminate(true);
			if(connecting.getVisibility() == View.GONE) {
				TranslateAnimation anim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -1, Animation.RELATIVE_TO_SELF, 0);
				anim.setDuration(200);
				anim.setFillAfter(true);
				connecting.startAnimation(anim);
				connecting.setVisibility(View.VISIBLE);
			}
			if(conn.getState() == NetworkConnection.STATE_DISCONNECTED && conn.getReconnectTimestamp() > 0) {
	    		String plural = "";
	    		int seconds = (int)((conn.getReconnectTimestamp() - System.currentTimeMillis()) / 1000);
	    		if(seconds != 1)
	    			plural = "s";
	    		if(seconds < 1) {
	    			connectingMsg.setText("Connecting");
					errorMsg.setVisibility(View.GONE);
	    		} else if(seconds > 10 && error != null) {
	    			connectingMsg.setText("Reconnecting in " + seconds + " second" + plural);
					errorMsg.setText(error);
					errorMsg.setVisibility(View.VISIBLE);
	    		} else {
					connectingMsg.setText("Reconnecting in " + seconds + " second" + plural);
					errorMsg.setVisibility(View.GONE);
					error = null;
	    		}
				try {
					if(countdownTimerTask != null)
						countdownTimerTask.cancel();
                    countdownTimerTask =  new TimerTask(){
                        public void run() {
                            if(conn.getState() == NetworkConnection.STATE_DISCONNECTED) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateReconnecting();
                                    }
                                });
                            }
                        }
                    };
					countdownTimer.schedule(countdownTimerTask, 1000);
				} catch (Exception e) {
				}
			} else {
				connectingMsg.setText("Connecting");
				error = null;
				errorMsg.setVisibility(View.GONE);
			}
    	} else {
			connectingMsg.setText("Offline");
			progressBar.setIndeterminate(false);
			progressBar.setProgress(0);
    	}
    }

    public void onIRCEvent(int what, final Object obj) {
		IRCCloudJSONObject e;

        switch (what) {
            case NetworkConnection.EVENT_DEBUG:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        errorMsg.setVisibility(View.VISIBLE);
                        errorMsg.setText(obj.toString());
                    }
                });
                break;
            case NetworkConnection.EVENT_PROGRESS:
                final float progress = (Float)obj;
                if(progressBar.getProgress() < progress) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setIndeterminate(false);
                            progressBar.setProgress((int) progress);
                        }
                    });
                }
                break;
            case NetworkConnection.EVENT_BACKLOG_START:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setProgress(0);
                    }
                });
                break;
            case NetworkConnection.EVENT_BACKLOG_FAILED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        headerView.setVisibility(View.GONE);
                        backlogFailed.setVisibility(View.VISIBLE);
                        loadBacklogButton.setVisibility(View.VISIBLE);
                    }
                });
                break;
            case NetworkConnection.EVENT_BACKLOG_END:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(connecting.getVisibility() == View.VISIBLE && conn != null && conn.ready) {
                            TranslateAnimation anim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -1);
                            anim.setDuration(200);
                            anim.setFillAfter(true);
                            anim.setAnimationListener(new AnimationListener() {

                                @Override
                                public void onAnimationEnd(Animation arg0) {
                                    connecting.setVisibility(View.GONE);
                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {
                                }

                                @Override
                                public void onAnimationStart(Animation animation) {
                                }

                            });
                            connecting.startAnimation(anim);
                            error = null;
                        }
                    }
                });
            case NetworkConnection.EVENT_CONNECTIVITY:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateReconnecting();
                    }
                });
            case NetworkConnection.EVENT_USERINFO:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(refreshTask != null)
                            refreshTask.cancel(true);
                        refreshTask = new RefreshTask();
                        refreshTask.execute((Void)null);
                    }
                });
                break;
            case NetworkConnection.EVENT_FAILURE_MSG:
                IRCCloudJSONObject o = (IRCCloudJSONObject)obj;
                try {
                    error = o.getString("message");
                    if(error.equals("temp_unavailable"))
                        error = "Your account is temporarily unavailable";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateReconnecting();
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;
            case NetworkConnection.EVENT_CONNECTIONLAG:
                try {
                    IRCCloudJSONObject object = (IRCCloudJSONObject)obj;
                    if(server != null && buffer != null && object.cid() == buffer.cid) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                update_status(server.status, server.fail_info);
                            }
                        });
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;
            case NetworkConnection.EVENT_STATUSCHANGED:
                try {
                    final IRCCloudJSONObject object = (IRCCloudJSONObject)obj;
                    if(buffer != null && object.cid() == buffer.cid) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                update_status(object.getString("new_status"), object.getJsonObject("fail_info"));
                            }
                        });
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;
            case NetworkConnection.EVENT_SETIGNORES:
                e = (IRCCloudJSONObject)obj;
                if(buffer != null && e.cid() == buffer.cid) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(refreshTask != null)
                                refreshTask.cancel(true);
                            refreshTask = new RefreshTask();
                            refreshTask.execute((Void)null);
                        }
                    });
                }
                break;
            case NetworkConnection.EVENT_HEARTBEATECHO:
                if(buffer != null && adapter != null && adapter.data.size() > 0) {
                    if(buffer.last_seen_eid == adapter.data.get(adapter.data.size() - 1).eid || !shouldTrackUnread()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                unreadTopView.setVisibility(View.GONE);
                            }
                        });
                    }
                }
                break;
            case NetworkConnection.EVENT_CHANNELTOPIC:
            case NetworkConnection.EVENT_JOIN:
            case NetworkConnection.EVENT_PART:
            case NetworkConnection.EVENT_NICKCHANGE:
            case NetworkConnection.EVENT_QUIT:
            case NetworkConnection.EVENT_KICK:
            case NetworkConnection.EVENT_CHANNELMODE:
            case NetworkConnection.EVENT_SELFDETAILS:
            case NetworkConnection.EVENT_USERMODE:
            case NetworkConnection.EVENT_USERCHANNELMODE:
                e = (IRCCloudJSONObject)obj;
                if(buffer != null && e.bid() == buffer.bid) {
                    final EventsDataSource.Event event = EventsDataSource.getInstance().getEvent(e.eid(), e.bid());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(adapter != null)
                                insertEvent(adapter, event, false, false);
                        }
                    });
                }
                break;
            case NetworkConnection.EVENT_BUFFERMSG:
                final EventsDataSource.Event event = (EventsDataSource.Event)obj;
                if(buffer != null && event.bid == buffer.bid) {
                    if(event.from != null && event.from.equalsIgnoreCase(buffer.name) && event.reqid == -1) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(adapter != null)
                                    adapter.clearPending();
                            }
                        });
                    } else if(event.reqid != -1) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(adapter != null && adapter.data != null) {
                                    for (int i = 0; i < adapter.data.size(); i++) {
                                        EventsDataSource.Event e = adapter.data.get(i);
                                        if (e.reqid == event.reqid && e.pending) {
                                            if (i > 0) {
                                                EventsDataSource.Event p = adapter.data.get(i - 1);
                                                if (p.row_type == ROW_TIMESTAMP) {
                                                    adapter.data.remove(p);
                                                    i--;
                                                }
                                            }
                                            adapter.data.remove(e);
                                            i--;
                                        }
                                    }
                                }
                            }
                        });
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            insertEvent(adapter, event, false, false);
                        }
                    });
                    if(event.pending && event.self && adapter != null && getListView() != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getListView().setSelection(adapter.getCount() - 1);
                            }
                        });
                    }
                }
                BuffersDataSource.Buffer b = BuffersDataSource.getInstance().getBuffer(event.bid);
                if(b != null && !b.scrolledUp && EventsDataSource.getInstance().getSizeOfBuffer(b.bid) > 200) {
                    EventsDataSource.getInstance().pruneEvents(b.bid);
                    if(buffer != null && b.bid == buffer.bid) {
                        if(b.last_seen_eid < event.eid && unreadTopView.getVisibility() == View.GONE)
                            b.last_seen_eid = event.eid;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (refreshTask != null)
                                    refreshTask.cancel(true);
                                refreshTask = new RefreshTask();
                                refreshTask.execute((Void) null);
                            }
                        });
                    }
                }
                break;
            case NetworkConnection.EVENT_AWAY:
            case NetworkConnection.EVENT_SELFBACK:
                if(server != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(server.away != null && server.away.length() > 0) {
                                awayTxt.setText(ColorFormatter.html_to_spanned(ColorFormatter.irc_to_html(TextUtils.htmlEncode("Away (" + server.away + ")"))).toString());
                                awayView.setVisibility(View.VISIBLE);
                            } else {
                                awayView.setVisibility(View.GONE);
                            }
                        }
                    });
                }
                break;
            case NetworkConnection.EVENT_GLOBALMSG:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        update_global_msg();
                    }
                });
                break;
            default:
                break;
        }
    }

    public Resources getSafeResources() {
        return IRCCloudApplication.getInstance().getApplicationContext().getResources();
    }

    public interface MessageViewListener extends OnBufferSelectedListener {
		public void onMessageViewReady();
		public boolean onMessageLongClicked(EventsDataSource.Event event);
		public void onMessageDoubleClicked(EventsDataSource.Event event);
	}
}
