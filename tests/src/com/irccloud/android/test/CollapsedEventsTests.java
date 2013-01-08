package com.irccloud.android.test;

import com.irccloud.android.CollapsedEventsList;

import junit.framework.TestCase;

public class CollapsedEventsTests extends TestCase {

	public void testOp() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_MODE, "sam", "ChanServ", "sam@example.net", null, null, CollapsedEventsList.MODE_OP, null);
		assertEquals("<b>sam</b> was opped (\u0004BA1719+o\u000f) by ChanServ", list.getCollapsedMessage());
	}
	
	public void testDeop() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_MODE, "sam", "ChanServ", "sam@example.net", null, null, CollapsedEventsList.MODE_DEOP, null);
		assertEquals("<b>sam</b> was de-opped (\u0004BA1719-o\u000f) by ChanServ", list.getCollapsedMessage());
	}
	
	public void testVoice() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_MODE, "sam", "ChanServ", "sam@example.net", null, null, CollapsedEventsList.MODE_VOICE, null);
		assertEquals("<b>sam</b> was voiced (\u000425B100+v\u000f) by ChanServ", list.getCollapsedMessage());
	}
	
	public void testDevoice() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_MODE, "sam", "ChanServ", "sam@example.net", null, null, CollapsedEventsList.MODE_DEVOICE, null);
		assertEquals("<b>sam</b> was de-voiced (\u000425B100-v\u000f) by ChanServ", list.getCollapsedMessage());
	}
	
	public void testJoin() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null);
		assertEquals("→ <b>sam</b> joined (sam@example.net)", list.getCollapsedMessage());
	}
	
	public void testPart() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_PART, "sam", null, "sam@example.net", null, null);
		assertEquals("← <b>sam</b> left (sam@example.net)", list.getCollapsedMessage());
	}
	
	public void testQuit() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, "Quit: leaving");
		assertEquals("⇐ <b>sam</b> quit (sam@example.net) Quit: leaving", list.getCollapsedMessage());
	}
	
	public void testNickChange() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null);
		assertEquals("sam_ → <b>sam</b>", list.getCollapsedMessage());
	}
	
	public void testJoinQuit() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null);
		list.addEvent(CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, null);
		assertEquals("↔ <b>sam</b> popped in", list.getCollapsedMessage());
	}

	public void testJoinJoin() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null);
		list.addEvent(CollapsedEventsList.TYPE_JOIN, "james", null, "james@example.net", null, null);
		assertEquals("→ <b>james</b> and <b>sam</b> joined", list.getCollapsedMessage());
	}

	public void testJoinQuit2() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null);
		list.addEvent(CollapsedEventsList.TYPE_QUIT, "james", null, "james@example.net", null, null);
		assertEquals("→ <b>sam</b> joined ⇐ <b>james</b> quit", list.getCollapsedMessage());
	}

	public void testJoinPart() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null);
		list.addEvent(CollapsedEventsList.TYPE_PART, "sam", null, "sam@example.net", null, null);
		assertEquals("↔ <b>sam</b> popped in", list.getCollapsedMessage());
	}

	public void testJoinPart2() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null);
		list.addEvent(CollapsedEventsList.TYPE_PART, "james", null, "james@example.net", null, null);
		assertEquals("→ <b>sam</b> joined ← <b>james</b> left", list.getCollapsedMessage());
	}

	public void testQuitJoin() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, null);
		list.addEvent(CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null);
		assertEquals("↔ <b>sam</b> nipped out", list.getCollapsedMessage());
	}

	public void testPartJoin() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_PART, "sam", null, "sam@example.net", null, null);
		list.addEvent(CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null);
		assertEquals("↔ <b>sam</b> nipped out", list.getCollapsedMessage());
	}

	public void testJoinNickchange() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_JOIN, "sam_", null, "sam@example.net", null, null);
		list.addEvent(CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null);
		assertEquals("→ <b>sam</b> (was sam_) joined (sam@example.net)", list.getCollapsedMessage());
	}

	public void testQuitJoinNickchange() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_QUIT, "sam_", null, "sam@example.net", null, null);
		list.addEvent(CollapsedEventsList.TYPE_JOIN, "sam_", null, "sam@example.net", null, null);
		list.addEvent(CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null);
		assertEquals("↔ <b>sam</b> (was sam_) nipped out", list.getCollapsedMessage());
	}

	public void testQuitJoinNickchange2() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, null);
		list.addEvent(CollapsedEventsList.TYPE_JOIN, "sam_", null, "sam@example.net", null, null);
		list.addEvent(CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null);
		assertEquals("↔ <b>sam</b> nipped out", list.getCollapsedMessage());
	}

	public void testQuitJoinMode() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, null);
		list.addEvent(CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null);
		list.addEvent(CollapsedEventsList.TYPE_MODE, "sam", "ChanServ", "sam@example.net", null, null, CollapsedEventsList.MODE_OP, null);
		assertEquals("↔ <b>sam</b> (opped) nipped out", list.getCollapsedMessage());
	}

	public void testQuitJoinModeNickPart() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_QUIT, "sam_", null, "sam@example.net", null, null);
		list.addEvent(CollapsedEventsList.TYPE_JOIN, "sam_", null, "sam@example.net", null, null);
		list.addEvent(CollapsedEventsList.TYPE_MODE, "sam_", "ChanServ", "sam@example.net", null, null, CollapsedEventsList.MODE_OP, null);
		list.addEvent(CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null);
		list.addEvent(CollapsedEventsList.TYPE_PART, "sam", null, "sam@example.net", null, null);
		assertEquals("← <b>sam</b> (was sam_; opped) left (sam@example.net)", list.getCollapsedMessage());
	}

	public void testNickchangeNickchange() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null);
		list.addEvent(CollapsedEventsList.TYPE_NICKCHANGE, "james", "james_old", "james@example.net", null, null);
		assertEquals("james_old → <b>james</b>, sam_ → <b>sam</b>", list.getCollapsedMessage());
	}

	public void testJoinQuitNickchange() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_JOIN, "sam_", null, "sam@example.net", null, null);
		list.addEvent(CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, null);
		list.addEvent(CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null);
		assertEquals("→ <b>sam</b> (was sam_) joined ⇐ <b>sam</b> quit", list.getCollapsedMessage());
	}

	public void testModeMode() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_MODE, "james", "ChanServ", "james@example.net", null, null, CollapsedEventsList.MODE_OP, null);
		list.addEvent(CollapsedEventsList.TYPE_MODE, "sam", "james", "sam@example.net", null, null, CollapsedEventsList.MODE_VOICE, null);
		assertEquals("mode: <b>james</b> (opped) and <b>sam</b> (voiced)", list.getCollapsedMessage());
	}

	public void testModeNickchange() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_MODE, "james", "ChanServ", "james@example.net", null, null, CollapsedEventsList.MODE_OP, null);
		list.addEvent(CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null);
		assertEquals("mode: <b>james</b> (opped) • sam_ → <b>sam</b>", list.getCollapsedMessage());
	}
	
	public void testJoinMode() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null);
		list.addEvent(CollapsedEventsList.TYPE_MODE, "sam", "ChanServ", "sam@example.net", null, null, CollapsedEventsList.MODE_OP, null);
		assertEquals("→ <b>sam</b> (opped) joined (sam@example.net)", list.getCollapsedMessage());
	}
	
	public void testModeJoinPart() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_MODE, "james", "ChanServ", "james@example.net", null, null, CollapsedEventsList.MODE_OP, null);
		list.addEvent(CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null);
		list.addEvent(CollapsedEventsList.TYPE_PART, "sam", null, "sam@example.net", null, null);
		assertEquals("mode: <b>james</b> (opped) ↔ <b>sam</b> popped in", list.getCollapsedMessage());
	}
	
	public void testJoinNickchangeModeModeMode() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null);
		list.addEvent(CollapsedEventsList.TYPE_NICKCHANGE, "james", "james_old", "james@example.net", null, null);
		list.addEvent(CollapsedEventsList.TYPE_MODE, "james", "ChanServ", "james@example.net", null, null, CollapsedEventsList.MODE_DEOP, null);
		list.addEvent(CollapsedEventsList.TYPE_MODE, "RJ", "ChanServ", "RJ@example.net", null, null, CollapsedEventsList.MODE_VOICE, null);
		list.addEvent(CollapsedEventsList.TYPE_MODE, "james", "ChanServ", "james@example.net", null, null, CollapsedEventsList.MODE_VOICE, null);
		assertEquals("→ <b>sam</b> joined • mode: <b>RJ</b> (voiced) • james_old → <b>james</b> (voiced)", list.getCollapsedMessage());
	}

}
