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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.irccloud.android.CollapsedEventsList;
import com.irccloud.android.data.model.Event;
import com.irccloud.android.data.model.Server;

import junit.framework.TestCase;

public class CollapsedEventsTests extends TestCase {
    private long eid = 1;

    private void addMode(CollapsedEventsList list, String mode, String nick, String from) {
        ArrayNode add = new ObjectMapper().createArrayNode();
        ObjectNode op = new ObjectMapper().createObjectNode();
        op.put("param", nick);
        op.put("mode", mode);
        add.add(op);

        Event e = new Event();
        e.eid = eid++;
        e.type = "user_channel_mode";
        e.from = from;
        e.from_mode = "q";
        e.nick = nick;
        e.target_mode = mode;
        e.server = "irc.example.net";
        e.chan = null;
        e.ops = new ObjectMapper().createObjectNode();
        ((ObjectNode)e.ops).put("add", add);
        ((ObjectNode)e.ops).put("remove", new ObjectMapper().createArrayNode());

        list.addEvent(e);
    }

    private void removeMode(CollapsedEventsList list, String mode, String nick, String from) {
        ArrayNode remove = new ObjectMapper().createArrayNode();
        ObjectNode op = new ObjectMapper().createObjectNode();
        op.put("param", nick);
        op.put("mode", mode);
        remove.add(op);

        Event e = new Event();
        e.eid = eid++;
        e.type = "user_channel_mode";
        e.from = from;
        e.nick = nick;
        e.server = "irc.example.net";
        e.chan = null;
        e.ops = new ObjectMapper().createObjectNode();
        ((ObjectNode)e.ops).put("add", new ObjectMapper().createArrayNode());
        ((ObjectNode)e.ops).put("remove", remove);

        list.addEvent(e);
    }

    public void testOper1() {
        CollapsedEventsList list = new CollapsedEventsList();
        addMode(list, "Y", "sam", "ChanServ");

        assertEquals("\u0004E02305\u0002•\u000F sam was promoted to oper (\u0004E02305+Y\u000F) by \u0004E7AA00\u0002•\u000F ChanServ", list.getCollapsedMessage());
    }

    public void testOper2() {
        CollapsedEventsList list = new CollapsedEventsList();
        addMode(list, "y", "sam", "ChanServ");

        assertEquals("\u0004E02305\u0002•\u000F sam was promoted to oper (\u0004E02305+y\u000F) by \u0004E7AA00\u0002•\u000F ChanServ", list.getCollapsedMessage());
    }

    public void testOwner1() {
        CollapsedEventsList list = new CollapsedEventsList();
        addMode(list, "q", "sam", "ChanServ");

        assertEquals("\u0004E7AA00\u0002•\u000F sam was promoted to owner (\u0004E7AA00+q\u000F) by \u0004E7AA00\u0002•\u000F ChanServ", list.getCollapsedMessage());
    }

    public void testOwner2() {
        CollapsedEventsList list = new CollapsedEventsList();
        Server s = new Server();
        s.MODE_OPER = "";
        s.MODE_OWNER = "y";
        list.setServer(s);
        addMode(list, "y", "sam", "ChanServ");

        assertEquals("\u0004E7AA00\u0002•\u000F sam was promoted to owner (\u0004E7AA00+y\u000F) by \u0002•\u000F ChanServ", list.getCollapsedMessage());
    }

    public void testOp() {
		CollapsedEventsList list = new CollapsedEventsList();
        addMode(list, "o", "sam", "ChanServ");

		assertEquals("\u0004BA1719\u0002•\u000F sam was opped (\u0004BA1719+o\u000F) by \u0004E7AA00\u0002•\u000F ChanServ", list.getCollapsedMessage());
	}
	
	public void testDeop() {
		CollapsedEventsList list = new CollapsedEventsList();
        removeMode(list, "o", "sam", "ChanServ");
		assertEquals("sam was de-opped (\u0004BA1719-o\u000F) by ChanServ", list.getCollapsedMessage());
	}
	
	public void testVoice() {
		CollapsedEventsList list = new CollapsedEventsList();
        addMode(list, "v", "sam", "ChanServ");
		assertEquals("\u000425B100\u0002•\u000F sam was voiced (\u000425B100+v\u000F) by \u0004E7AA00\u0002•\u000F ChanServ", list.getCollapsedMessage());
	}
	
	public void testDevoice() {
		CollapsedEventsList list = new CollapsedEventsList();
        removeMode(list, "v", "sam", "ChanServ");
		assertEquals("sam was de-voiced (\u000425B100-v\u000F) by ChanServ", list.getCollapsedMessage());
	}

    public void testOpByServer() {
        CollapsedEventsList list = new CollapsedEventsList();
        addMode(list, "o", "sam", null);

        assertEquals("\u0004BA1719\u0002•\u000F sam was opped (\u0004BA1719+o\u000F) by the server irc.example.net", list.getCollapsedMessage());
    }

    public void testOpDeop() {
        CollapsedEventsList list = new CollapsedEventsList();
        addMode(list, "o", "sam", "james");
        removeMode(list, "o", "sam", "ChanServ");

        assertEquals("\u0004BA1719\u0002•\u000F sam was de-opped (\u0004BA1719-o\u000F) by ChanServ", list.getCollapsedMessage());
    }

    public void testJoin() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null, null, null);
		assertEquals("→ sam joined (sam@example.net)", list.getCollapsedMessage());
	}
	
	public void testPart() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_PART, "sam", null, "sam@example.net", null, null, null, null, null);
		assertEquals("← sam left (sam@example.net)", list.getCollapsedMessage());
	}
	
	public void testQuit() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, "Quit: leaving", null, null);
		assertEquals("⇐ sam quit (sam@example.net) Quit: leaving", list.getCollapsedMessage());
	}

    public void testQuit2() {
        CollapsedEventsList list = new CollapsedEventsList();
        list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, "*.net *.split", null, null);
        assertEquals("⇐ sam quit (sam@example.net) *.net *.split", list.getCollapsedMessage());
    }

    public void testNickChange() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null, null, null, null);
		assertEquals("sam_ → sam", list.getCollapsedMessage());
	}

    public void testNickChangeQuit() {
        CollapsedEventsList list = new CollapsedEventsList();
        list.addEvent(eid++, CollapsedEventsList.TYPE_NICKCHANGE, "sam_", "sam", "sam@example.net", null, null, null, null, null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam_", null, "sam@example.net", null, "Bye!", null, null);
        assertEquals("⇐ sam_ (was sam) quit (sam@example.net) Bye!", list.getCollapsedMessage());
    }

    public void testJoinQuit() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, null, null, null, null);
		assertEquals("↔ sam popped in", list.getCollapsedMessage());
	}

    public void testJoinQuitJoin() {
        CollapsedEventsList list = new CollapsedEventsList();
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null, null, null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, null, null, null, null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null, null, null);
        assertEquals("→ sam joined (sam@example.net)", list.getCollapsedMessage());
    }

    public void testJoinJoin() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "james", null, "james@example.net", null, null, null, null, null);
		assertEquals("→ sam and james joined", list.getCollapsedMessage());
	}

    public void testJoinJoinJoin() {
        CollapsedEventsList list = new CollapsedEventsList();
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null, null, null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "james", null, "james@example.net", null, null, null, null, null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "RJ", null, "RJ@example.net", null, null, null, null, null);
        assertEquals("→ sam, james, and RJ joined", list.getCollapsedMessage());
    }

    public void testJoinQuit2() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "james", null, "james@example.net", null, null, null, null, null);
		assertEquals("→ sam joined ⇐ james quit", list.getCollapsedMessage());
	}

	public void testJoinPart() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_PART, "sam", null, "sam@example.net", null, null, null, null);
		assertEquals("↔ sam popped in", list.getCollapsedMessage());
	}

	public void testJoinPart2() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_PART, "james", null, "james@example.net", null, null, null, null);
		assertEquals("→ sam joined ← james left", list.getCollapsedMessage());
	}

	public void testQuitJoin() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null, null);
		assertEquals("↔ sam nipped out", list.getCollapsedMessage());
	}

	public void testPartJoin() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_PART, "sam", null, "sam@example.net", null, null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null, null);
		assertEquals("↔ sam nipped out", list.getCollapsedMessage());
	}

	public void testJoinNickchange() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam_", null, "sam@example.net", null, null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null, null, null);
		assertEquals("→ sam (was sam_) joined (sam@example.net)", list.getCollapsedMessage());
	}

	public void testQuitJoinNickchange() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam_", null, "sam@example.net", null, null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam_", null, "sam@example.net", null, null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null, null, null);
		assertEquals("↔ sam (was sam_) nipped out", list.getCollapsedMessage());
	}

	public void testQuitJoinNickchange2() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam_", null, "sam@example.net", null, null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null, null, null);
		assertEquals("↔ sam nipped out", list.getCollapsedMessage());
	}

	public void testQuitJoinMode() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null, null);
        addMode(list, "o", "sam", "ChanServ");
		assertEquals("↔ \u0004BA1719\u0002•\u000F sam (opped) nipped out", list.getCollapsedMessage());
	}

	public void testQuitJoinModeNickPart() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam_", null, "sam@example.net", null, null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam_", null, "sam@example.net", null, null, null, null);
        addMode(list, "o", "sam_", "ChanServ");
		list.addEvent(eid++, CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_PART, "sam", null, "sam@example.net", null, null, null, null);
		assertEquals("← \u0004BA1719\u0002•\u000F sam (was sam_; opped) left", list.getCollapsedMessage());
	}

	public void testNickchangeNickchange() {
		CollapsedEventsList list = new CollapsedEventsList();
        list.addEvent(eid++, CollapsedEventsList.TYPE_NICKCHANGE, "james", "james_old", "james@example.net", null, null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null, null, null);
		assertEquals("james_old → james, sam_ → sam", list.getCollapsedMessage());
	}

	public void testJoinQuitNickchange() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam_", null, "sam@example.net", null, null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null, null, null);
		assertEquals("↔ sam (was sam_) nipped out", list.getCollapsedMessage());
	}

    public void testJoinQuitNickchange2() {
        CollapsedEventsList list = new CollapsedEventsList();
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam_", null, "sam@example.net", null, null, null, null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, null, null, null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null, null, null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam_", null, "sam@example.net", null, null, null, null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, null, null, null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null, null, null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam_", null, "sam@example.net", null, null, null, null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, null, null, null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null, null, null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam_", null, "sam@example.net", null, null, null, null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, null, null, null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null, null, null);
        assertEquals("↔ sam (was sam_) nipped out", list.getCollapsedMessage());
    }

    public void testModeMode() {
		CollapsedEventsList list = new CollapsedEventsList();
        addMode(list, "v", "sam", "ChanServ");
        addMode(list, "o", "james", "ChanServ");
		assertEquals("mode: \u0004BA1719\u0002•\u000F james (opped) and \u000425B100\u0002•\u000F sam (voiced)", list.getCollapsedMessage());
	}

    public void testModeMode2() {
        CollapsedEventsList list = new CollapsedEventsList();
        addMode(list, "o", "sam", "ChanServ");
        addMode(list, "v", "sam", "ChanServ");
        assertEquals("mode: \u0004BA1719\u0002•\u000F sam (opped, voiced)", list.getCollapsedMessage());
    }

    public void testModeNickchange() {
		CollapsedEventsList list = new CollapsedEventsList();
        addMode(list, "o", "james", "ChanServ");
		list.addEvent(eid++, CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_", "sam@example.net", null, null, null, null);
		assertEquals("mode: \u0004BA1719\u0002•\u000F james (opped) • sam_ → sam", list.getCollapsedMessage());
	}
	
	public void testJoinMode() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null, null);
        addMode(list, "o", "sam", "ChanServ");
		assertEquals("→ \u0004BA1719\u0002•\u000F sam (opped) joined", list.getCollapsedMessage());
	}

    public void testJoinModeMode() {
        CollapsedEventsList list = new CollapsedEventsList();
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null, null);
        addMode(list, "o", "sam", "ChanServ");
        addMode(list, "q", "sam", "ChanServ");
        assertEquals("→ \u0004E7AA00\u0002•\u000F sam (promoted to owner, opped) joined", list.getCollapsedMessage());
    }

    public void testModeJoinPart() {
		CollapsedEventsList list = new CollapsedEventsList();
        addMode(list, "o", "james", "ChanServ");
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_PART, "sam", null, "sam@example.net", null, null, null, null);
		assertEquals("mode: \u0004BA1719\u0002•\u000F james (opped) ↔ sam popped in", list.getCollapsedMessage());
	}
	
	public void testJoinNickchangeModeModeMode() {
		CollapsedEventsList list = new CollapsedEventsList();
		list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null, null);
		list.addEvent(eid++, CollapsedEventsList.TYPE_NICKCHANGE, "james", "james_old", "james@example.net", null, null, null, null);
        removeMode(list, "o", "james", "ChanServ");
        addMode(list, "v", "RJ", "ChanServ");
        addMode(list, "v", "james", "ChanServ");
		assertEquals("→ sam joined • mode: \u000425B100\u0002•\u000F RJ (voiced) • james_old → \u000425B100\u0002•\u000F james (voiced, de-opped)", list.getCollapsedMessage());
	}

    public void testMultiChannelJoin() {
        CollapsedEventsList list = new CollapsedEventsList();
        list.showChan = true;
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, "#test1", null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, "#test2", null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, "#test3", null);
        assertEquals("→ sam joined #test1, #test2, and #test3", list.getCollapsedMessage());
    }

    public void testMultiChannelNickChangeQuitJoin() {
        CollapsedEventsList list = new CollapsedEventsList();
        list.showChan = true;
        list.addEvent(eid++, CollapsedEventsList.TYPE_NICKCHANGE, "sam", "sam_old", "sam@example.net", null, null, null, null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, null, null, null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, "#test1", null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, "#test2", null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, null, null, null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, "#test1", null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, "#test2", null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, null, null, null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, "#test1", null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, "#test2", null);
        assertEquals("↔ sam (was sam_old) nipped out #test1 and #test2", list.getCollapsedMessage());
    }

    public void testMultiChannelPopIn1() {
        CollapsedEventsList list = new CollapsedEventsList();
        list.showChan = true;
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, "#test1", null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, "#test2", null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, "#test3", null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_PART, "sam", null, "sam@example.net", null, null, "#test1", null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_PART, "sam", null, "sam@example.net", null, null, "#test2", null);
        assertEquals("→ sam joined #test3 ↔ sam popped in #test1 and #test2", list.getCollapsedMessage());
    }

    public void testMultiChannelPopIn2() {
        CollapsedEventsList list = new CollapsedEventsList();
        list.showChan = true;
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, "#test1", null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, "#test2", null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, "#test3", null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, null, null, null);
        assertEquals("↔ sam popped in #test1, #test2, and #test3", list.getCollapsedMessage());
    }

    public void testMultiChannelQuit() {
        CollapsedEventsList list = new CollapsedEventsList();
        list.showChan = true;
        list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, null, null, null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, "#test1", null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, null, null, null);
        assertEquals("⇐ sam quit (sam@example.net) ", list.getCollapsedMessage());
    }

    public void testNetSplit() {
        CollapsedEventsList list = new CollapsedEventsList();
        list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "sam", null, "sam@example.net", null, "irc.example.net irc2.example.net", null, null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "james", null, "james@example.net", null, "irc.example.net irc2.example.net", null, null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "RJ", null, "RJ@example.net", null, "irc3.example.net irc2.example.net", null, null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_QUIT, "russ", null, "russ@example.net", null, "fake.net fake.net", null, null);
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "sam", null, "sam@example.net", null, null, null, null);
        assertEquals("irc.example.net ↮ irc2.example.net and irc3.example.net ↮ irc2.example.net ⇐ james, RJ, and russ quit ↔ sam nipped out", list.getCollapsedMessage());
    }

    public void testChanServJoin() {
        CollapsedEventsList list = new CollapsedEventsList();
        list.addEvent(eid++, CollapsedEventsList.TYPE_JOIN, "ChanServ", null, "ChanServ@services.", null, null, null, null);
        addMode(list, "o", "ChanServ", null);
        assertEquals("→ \u0004BA1719\u0002•\u000F ChanServ (opped) joined", list.getCollapsedMessage());
    }
}
