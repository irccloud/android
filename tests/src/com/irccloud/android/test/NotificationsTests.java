/*
 * Copyright (c) 2015 IRCCloud, Ltd.
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

package com.irccloud.android.test;

import java.util.ArrayList;

import com.irccloud.android.Notifications;

import junit.framework.TestCase;

public class NotificationsTests extends TestCase {

	public void testAdd() {
		Notifications n = Notifications.getInstance();
		n.clear();
		n.clearDismissed();

		n.addNetwork(1, "TestNode");
		n.addNotification(1, 1, 2, "sam", "test", "sam", "conversation", "buffer_msg");
		
		ArrayList<Notifications.Notification> notifications = n.getMessageNotifications();
		assertEquals(1, notifications.size());
		
		Notifications.Notification n1 = notifications.get(0);
		assertEquals(1, n1.cid);
		assertEquals(1, n1.bid);
		assertEquals(2, n1.eid);
		assertEquals("sam", n1.nick);
		assertEquals("test", n1.message);
		assertEquals("TestNode", n1.network);
		assertEquals("sam", n1.chan);
		assertEquals("conversation", n1.buffer_type);
		assertEquals("buffer_msg", n1.message_type);

		n.clear();
		n.clearDismissed();
		assertEquals(0, n.getMessageNotifications().size());
	}
	
	public void testEidUpdate() {
		Notifications n = Notifications.getInstance();
		n.clear();
		n.clearDismissed();

		n.addNetwork(1, "TestNode");
		n.addNotification(1, 1, 2, "sam", "test", "sam", "conversation", "buffer_msg");
		assertEquals(1, n.getMessageNotifications().size());
		
		//last_seen_eid is less than the notification's eid
		n.updateLastSeenEid(1, 1);
		n.deleteOldNotifications(1, 1);
		assertEquals(1, n.getMessageNotifications().size());

		//last_seen_eid is the notification's eid
		n.updateLastSeenEid(1, 2);
		n.deleteOldNotifications(1, 2);
		assertEquals(0, n.getMessageNotifications().size());

		//Attempt to insert an already-seen eid
		n.addNotification(1, 1, 2, "sam", "test", "sam", "conversation", "buffer_msg");
		assertEquals(0, n.getMessageNotifications().size());

		n.clear();
		n.clearDismissed();
		assertEquals(0, n.getMessageNotifications().size());
	}
	
	public void testDismiss() {
		Notifications n = Notifications.getInstance();
		n.clear();
		n.clearDismissed();

		n.addNetwork(1, "TestNode");
		n.addNotification(1, 1, 2, "sam", "test", "sam", "conversation", "buffer_msg");
		assertEquals(1, n.getMessageNotifications().size());

		//Dismiss the notification
		n.dismiss(1, 2);
		assertEquals(0, n.getMessageNotifications().size());

		//Attempt to insert an already-dismissed eid
		n.addNotification(1, 1, 2, "sam", "test", "sam", "conversation", "buffer_msg");
		assertEquals(0, n.getMessageNotifications().size());

		n.clear();
		n.clearDismissed();
		assertEquals(0, n.getMessageNotifications().size());
	}
}
