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

import com.irccloud.android.data.collection.BuffersList;
import com.irccloud.android.data.collection.NotificationsList;
import com.irccloud.android.data.model.Notification;

import junit.framework.TestCase;

import java.util.List;

public class NotificationsTests extends TestCase {

	public void testAdd() {
		BuffersList b = BuffersList.getInstance();
		b.clear();
		b.createBuffer(1, 1, 1, 1, "sam", "conversation", 0, 0, 0, 0);

		NotificationsList n = NotificationsList.getInstance();
		n.clear();

		n.addNotification(1, 1, 2, "sam", "test", "sam", "conversation", "buffer_msg", "TestNetwork", null);
		
		List<Notification> notifications = n.getMessageNotifications();
		assertEquals(1, notifications.size());
		
		Notification n1 = notifications.get(0);
		assertEquals(1, n1.getCid());
		assertEquals(1, n1.getBid());
		assertEquals(2, n1.getEid());
		assertEquals("sam", n1.getNick());
		assertEquals("test", n1.getMessage());
		assertEquals("TestNetwork", n1.getNetwork());
		assertEquals("sam", n1.getChan());
		assertEquals("conversation", n1.getBuffer_type());
		assertEquals("buffer_msg", n1.getMessage_type());

		n.clear();
		b.clear();
		assertEquals(0, n.getMessageNotifications().size());
	}
	
	public void testEidUpdate() {
		BuffersList b = BuffersList.getInstance();
		b.clear();
		b.createBuffer(1, 1, 1, 1, "sam", "conversation", 0, 0, 0, 0);

		NotificationsList n = NotificationsList.getInstance();
		n.clear();

		n.addNotification(1, 1, 2, "sam", "test", "sam", "conversation", "buffer_msg", "TestNetwork", null);
		assertEquals(1, n.getMessageNotifications().size());
		
		//last_seen_eid is less than the notification's eid
		n.updateLastSeenEid(1, 1);
		n.deleteOldNotifications();
		assertEquals(1, n.getMessageNotifications().size());

		//last_seen_eid is the notification's eid
		b.getBuffer(1).setLast_seen_eid(2);
		n.updateLastSeenEid(1, 2);
		n.deleteOldNotifications();
		assertEquals(0, n.getMessageNotifications().size());

		//Attempt to insert an already-seen eid
		n.addNotification(1, 1, 2, "sam", "test", "sam", "conversation", "buffer_msg", "TestNetwork", null);
		assertEquals(0, n.getMessageNotifications().size());

		n.clear();
		b.clear();
		assertEquals(0, n.getMessageNotifications().size());
	}
	
	public void testDismiss() {
		BuffersList b = BuffersList.getInstance();
		b.clear();
		b.createBuffer(1, 1, 1, 1, "sam", "conversation", 0, 0, 0, 0);

		NotificationsList n = NotificationsList.getInstance();
		n.clear();

		n.addNotification(1, 1, 2, "sam", "test", "sam", "conversation", "buffer_msg", "TestNetwork", null);
		assertEquals(1, n.getMessageNotifications().size());

		//Dismiss the notification
		n.dismiss(1, 2);
		assertEquals(0, n.getMessageNotifications().size());

		n.clear();
		b.clear();
		assertEquals(0, n.getMessageNotifications().size());
	}
}
