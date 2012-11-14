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
