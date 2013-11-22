package com.irccloud.android.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.irccloud.android.CollapsedEventsList;
import com.irccloud.android.EventsDataSource;

import junit.framework.TestCase;

public class CollapsedEventsTests extends TestCase {
    private long eid = 1;

    private void addMode(CollapsedEventsList list, String mode, String nick, String from) {
        JsonArray add = new JsonArray();
        JsonObject op = new JsonObject();
        op.addProperty("param", nick);
        op.addProperty("mode", mode);
        add.add(op);

        EventsDataSource.Event e = EventsDataSource.getInstance().new Event();
        e.eid = eid++;
        e.type = "user_channel_mode";
        e.from = from;
        e.from_mode = "q";
        e.nick = nick;
        e.target_mode = mode;
        e.server = "irc.example.net";
        e.chan = null;
        e.ops = new JsonObject();
        e.ops.add("add", add);
        e.ops.add("remove", new JsonArray());

        list.addEvent(e);
    }

    private void removeMode(CollapsedEventsList list, String mode, String nick, String from) {
        JsonArray remove = new JsonArray();
        JsonObject op = new JsonObject();
        op.addProperty("param", nick);
        op.addProperty("mode", mode);
        remove.add(op);

        EventsDataSource.Event e = EventsDataSource.getInstance().new Event();
        e.eid = eid++;
        e.type = "user_channel_mode";
        e.from = from;
        e.nick = nick;
        e.server = "irc.example.net";
        e.chan = null;
        e.ops = new JsonObject();
        e.ops.add("add", new JsonArray());
        e.ops.add("remove", remove);

        list.addEvent(e);
    }

    public void testOp() {
		CollapsedEventsList list = new CollapsedEventsList();
        addMode(list, "o", "sam", "ChanServ");

		assertEquals("<b>\u0004BA1719\u0002•\u000F sam</b> was opped (\u0004BA1719+o\u000F) by \u0004E7AA00\u0002•\u000F ChanServ", list.getCollapsedMessage(false));
	}
	
	public void testDeop() {
		CollapsedEventsList list = new CollapsedEventsList();
        removeMode(list, "o", "sam", "ChanServ");
		assertEquals("<b>sam</b> was de-opped (\u0004BA1719-o\u000F) by ChanServ", list.getCollapsedMessage(false));
	}
	
	public void testVoice() {
		CollapsedEventsList list = new CollapsedEventsList();
        addMode(list, "v", "sam", "ChanServ");
		assertEquals("<b>\u000425B100\u0002•\u000F sam</b> was voiced (\u000425B100+v\u000F) by \u0004E7AA00\u0002•\u000F ChanServ", list.getCollapsedMessage(false));
	}
	
	public void testDevoice() {
		CollapsedEventsList list = new CollapsedEventsList();
        removeMode(list, "v", "sam", "ChanServ");
		assertEquals("<b>sam</b> was de-voiced (\u000425B100-v\u000F) by ChanServ", list.getCollapsedMessage(false));
	}

    public void testOpByServer() {
        CollapsedEventsList list = new CollapsedEventsList();
        addMode(list, "o", "sam", null);

        assertEquals("<b>\u0004BA1719\u0002•\u000F sam</b> was opped (\u0004BA1719+o\u000F) by the server <b>irc.example.net</b>", list.getCollapsedMessage(false));
    }

    public void testJoin() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null);
		assertEquals("→ <b>sam</b> joined (sam@example.net)", list.getCollapsedMessage(false));
	}
	
	public void testPart() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_PART, "sam", null, "sam@example.net", null, null, null);
		assertEquals("← <b>sam</b> left (sam@example.net)", list.getCollapsedMessage(false));
	}
	
	public void testQuit() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, "Quit: leaving", null);
		assertEquals("⇐ <b>sam</b> quit (sam@example.net) Quit: leaving", list.getCollapsedMessage(false));
	}

    public void testQuit2() {
        CollapsedEventsList list = new CollapsedEventsList();
        list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, "*.net *.split", null);
        assertEquals("⇐ <b>sam</b> quit (sam@example.net) *.net *.split", list.getCollapsedMessage(false));
    }

    public void testNickChange() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null, null);
		assertEquals("sam_ → <b>sam</b>", list.getCollapsedMessage(false));
	}
	
	public void testJoinQuit() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, null, null);
		assertEquals("↔ <b>sam</b> popped in", list.getCollapsedMessage(false));
	}

	public void testJoinJoin() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "james", null, "james@example.net", null, null, null);
		assertEquals("→ <b>sam</b> and <b>james</b> joined", list.getCollapsedMessage(false));
	}

	public void testJoinQuit2() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "james", null, "james@example.net", null, null, null);
		assertEquals("→ <b>sam</b> joined ⇐ <b>james</b> quit", list.getCollapsedMessage(false));
	}

	public void testJoinPart() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_PART, "sam", null, "sam@example.net", null, null, null);
		assertEquals("↔ <b>sam</b> popped in", list.getCollapsedMessage(false));
	}

	public void testJoinPart2() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_PART, "james", null, "james@example.net", null, null, null);
		assertEquals("→ <b>sam</b> joined ← <b>james</b> left", list.getCollapsedMessage(false));
	}

	public void testQuitJoin() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null);
		assertEquals("↔ <b>sam</b> nipped out", list.getCollapsedMessage(false));
	}

	public void testPartJoin() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_PART, "sam", null, "sam@example.net", null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null);
		assertEquals("↔ <b>sam</b> nipped out", list.getCollapsedMessage(false));
	}

	public void testJoinNickchange() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam_", null, "sam@example.net", null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null, null);
		assertEquals("→ <b>sam</b> (was sam_) joined (sam@example.net)", list.getCollapsedMessage(false));
	}

	public void testQuitJoinNickchange() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam_", null, "sam@example.net", null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam_", null, "sam@example.net", null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null, null);
		assertEquals("↔ <b>sam</b> (was sam_) nipped out", list.getCollapsedMessage(false));
	}

	public void testQuitJoinNickchange2() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam_", null, "sam@example.net", null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null, null);
		assertEquals("↔ <b>sam</b> nipped out", list.getCollapsedMessage(false));
	}

	public void testQuitJoinMode() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null);
        addMode(list, "o", "sam", "ChanServ");
		assertEquals("↔ <b>\u0004BA1719\u0002•\u000F sam</b> (\u00031opped\u000F) nipped out", list.getCollapsedMessage(false));
	}

	public void testQuitJoinModeNickPart() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam_", null, "sam@example.net", null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam_", null, "sam@example.net", null, null, null);
        addMode(list, "o", "sam_", "ChanServ");
		list.addEvent(eid++, CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_PART, "sam", null, "sam@example.net", null, null, null);
		assertEquals("← <b>\u0004BA1719\u0002•\u000F sam</b> (was sam_; \u00031opped\u000F) left (sam@example.net)", list.getCollapsedMessage(false));
	}

	public void testNickchangeNickchange() {
		CollapsedEventsList list = new CollapsedEventsList();
        list.addEvent(eid++, CollapsedEventsList.TYPE_NICKCHANGE, "james", "james_old", "james@example.net", null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null, null);
		assertEquals("james_old → <b>james</b>, sam_ → <b>sam</b>", list.getCollapsedMessage(false));
	}

	public void testJoinQuitNickchange() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam_", null, "sam@example.net", null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null, null);
		assertEquals("→ <b>sam</b> (was sam_) joined ⇐ <b>sam</b> quit", list.getCollapsedMessage(false));
	}

	public void testModeMode() {
		CollapsedEventsList list = new CollapsedEventsList();
        addMode(list, "v", "sam", "ChanServ");
        addMode(list, "o", "james", "ChanServ");
		assertEquals("\u00031mode:\u000F <b>\u0004BA1719\u0002•\u000F james</b> (\u00031opped\u000F) and <b>\u000425B100\u0002•\u000F sam</b> (\u00031voiced\u000F)", list.getCollapsedMessage(false));
	}

    public void testModeMode2() {
        CollapsedEventsList list = new CollapsedEventsList();
        addMode(list, "o", "sam", "ChanServ");
        addMode(list, "v", "sam", "ChanServ");
        assertEquals("\u00031mode:\u000F <b>\u0004BA1719\u0002•\u000F sam</b> (\u00031opped, voiced\u000F)", list.getCollapsedMessage(false));
    }

    public void testModeNickchange() {
		CollapsedEventsList list = new CollapsedEventsList();
        addMode(list, "o", "james", "ChanServ");
		list.addEvent(eid++, CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null, null);
		assertEquals("\u00031mode:\u000F <b>\u0004BA1719\u0002•\u000F james</b> (\u00031opped\u000F) • sam_ → <b>sam</b>", list.getCollapsedMessage(false));
	}
	
	public void testJoinMode() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null);
        addMode(list, "o", "sam", "ChanServ");
		assertEquals("→ <b>\u0004BA1719\u0002•\u000F sam</b> (\u00031opped\u000F) joined (sam@example.net)", list.getCollapsedMessage(false));
	}

    public void testJoinModeMode() {
        CollapsedEventsList list = new CollapsedEventsList();
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null);
        addMode(list, "o", "sam", "ChanServ");
        addMode(list, "q", "sam", "ChanServ");
        assertEquals("→ <b>\u0004E7AA00\u0002•\u000F sam</b> (\u00031promoted to owner, opped\u000F) joined", list.getCollapsedMessage(false));
    }

    public void testModeJoinPart() {
		CollapsedEventsList list = new CollapsedEventsList();
        addMode(list, "o", "james", "ChanServ");
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_PART, "sam", null, "sam@example.net", null, null, null);
		assertEquals("\u00031mode:\u000F <b>\u0004BA1719\u0002•\u000F james</b> (\u00031opped\u000F) ↔ <b>sam</b> popped in", list.getCollapsedMessage(false));
	}
	
	public void testJoinNickchangeModeModeMode() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_NICKCHANGE, "james", "james_old", "james@example.net", null, null, null);
        removeMode(list, "o", "james", "ChanServ");
        addMode(list, "v", "RJ", "ChanServ");
        addMode(list, "v", "james", "ChanServ");
		assertEquals("→ <b>sam</b> joined • \u00031mode:\u000F <b>\u000425B100\u0002•\u000F RJ</b> (\u00031voiced\u000F) • james_old → <b>\u000425B100\u0002•\u000F james</b> (\u00031voiced, de-opped\u000F)", list.getCollapsedMessage(false));
	}

    public void testMultiChannelJoin() {
        CollapsedEventsList list = new CollapsedEventsList();
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, "#test1");
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, "#test2");
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, "#test3");
        assertEquals("→ <b>sam</b> joined #test1, #test2, and #test3", list.getCollapsedMessage(true));
    }

    public void testNetSplit() {
        CollapsedEventsList list = new CollapsedEventsList();
        list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, "irc.example.net irc2.example.net", null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "james", null, "james@example.net", null, "irc.example.net irc2.example.net", null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "RJ", null, "RJ@example.net", null, "irc3.example.net irc2.example.net", null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "russ", null, "russ@example.net", null, "fake.net fake.net", null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null);
        assertEquals("irc.example.net ↮ irc2.example.net and irc3.example.net ↮ irc2.example.net ⇐ <b>russ</b> quit", list.getCollapsedMessage(false));
    }

    public void testChanServJoin() {
        CollapsedEventsList list = new CollapsedEventsList();
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "ChanServ", null, "ChanServ@services.", null, null, null);
        addMode(list, "o", "ChanServ", null);
        assertEquals("→ <b>\u0004BA1719\u0002•\u000F ChanServ</b> (\u00031opped\u000F) joined (ChanServ@services.)", list.getCollapsedMessage(false));
    }
}
