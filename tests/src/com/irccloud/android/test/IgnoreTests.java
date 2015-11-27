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

import android.test.AndroidTestCase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.irccloud.android.Ignore;
import com.irccloud.android.data.collection.ServersList;
import com.irccloud.android.data.model.Server;

public class IgnoreTests extends AndroidTestCase {

	public void testNoNickWithDot() {
        ArrayNode ignores = new ObjectMapper().createArrayNode();
		ignores.add("*!users.1@host.local");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);

		Ignore ignore = new Ignore();
		ignore.setIgnores(s.ignores);

		assertEquals(true, ignore.match("sam!users.1@host.local"));
		assertEquals(true, ignore.match("SAM!userS.1@host.LOCAL"));
		assertEquals(true, ignore.match("harry!users.1@host.local"));
		assertEquals(false, ignore.match("sam!~username@host.local"));
		assertEquals(false, ignore.match("harry!~username@host.local"));
		assertEquals(false, ignore.match("sam!users.1@work.local"));
		assertEquals(false, ignore.match("harry!users.1@work.local"));
		assertEquals(false, ignore.match("sam!~username@work.local"));
		assertEquals(false, ignore.match("harry!~username@work.local"));
	}

	public void testNoNick() {
		ArrayNode ignores = new ObjectMapper().createArrayNode();
		ignores.add("*!username@host.local");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);

        Ignore ignore = new Ignore();
        ignore.setIgnores(s.ignores);

		assertEquals(false, ignore.match("sam!users.1@host.local"));
		assertEquals(false, ignore.match("SAM!userS.1@host.LOCAL"));
		assertEquals(false, ignore.match("harry!users.1@host.local"));
		assertEquals(true, ignore.match("sam!~username@host.local"));
		assertEquals(true, ignore.match("harry!~username@host.local"));
		assertEquals(false, ignore.match("sam!users.1@work.local"));
		assertEquals(false, ignore.match("harry!users.1@work.local"));
		assertEquals(false, ignore.match("sam!~username@work.local"));
		assertEquals(false, ignore.match("harry!~username@work.local"));
	}

	public void testNoNickIdentUsername() {
		ArrayNode ignores = new ObjectMapper().createArrayNode();
        ignores.add("*!~username@host.local");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);

        Ignore ignore = new Ignore();
        ignore.setIgnores(s.ignores);

		assertEquals(false, ignore.match("sam!users.1@host.local"));
		assertEquals(false, ignore.match("SAM!userS.1@host.LOCAL"));
		assertEquals(false, ignore.match("harry!users.1@host.local"));
		assertEquals(true, ignore.match("sam!~username@host.local"));
		assertEquals(true, ignore.match("harry!~username@host.local"));
		assertEquals(false, ignore.match("sam!users.1@work.local"));
		assertEquals(false, ignore.match("harry!users.1@work.local"));
		assertEquals(false, ignore.match("sam!~username@work.local"));
		assertEquals(false, ignore.match("harry!~username@work.local"));
	}

	public void testNoHost() {
		ArrayNode ignores = new ObjectMapper().createArrayNode();
		ignores.add("sam!users.1@*");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);

        Ignore ignore = new Ignore();
        ignore.setIgnores(s.ignores);

		assertEquals(true, ignore.match("sam!users.1@host.local"));
		assertEquals(true, ignore.match("SAM!userS.1@host.LOCAL"));
		assertEquals(false, ignore.match("harry!users.1@host.local"));
		assertEquals(false, ignore.match("sam!~username@host.local"));
		assertEquals(false, ignore.match("harry!~username@host.local"));
		assertEquals(true, ignore.match("sam!users.1@work.local"));
		assertEquals(false, ignore.match("harry!users.1@work.local"));
		assertEquals(false, ignore.match("sam!~username@work.local"));
		assertEquals(false, ignore.match("harry!~username@work.local"));
	}

	public void testJustHost() {
		ArrayNode ignores = new ObjectMapper().createArrayNode();
		ignores.add("host.local");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);

        Ignore ignore = new Ignore();
        ignore.setIgnores(s.ignores);

		assertEquals(false, ignore.match("sam!users.1@host.local"));
		assertEquals(false, ignore.match("SAM!userS.1@host.LOCAL"));
		assertEquals(false, ignore.match("harry!users.1@host.local"));
		assertEquals(false, ignore.match("sam!~username@host.local"));
		assertEquals(false, ignore.match("harry!~username@host.local"));
		assertEquals(false, ignore.match("sam!users.1@work.local"));
		assertEquals(false, ignore.match("harry!users.1@work.local"));
		assertEquals(false, ignore.match("sam!~username@work.local"));
		assertEquals(false, ignore.match("harry!~username@work.local"));
	}

	public void testPartialWildcard1() {
		ArrayNode ignores = new ObjectMapper().createArrayNode();
		ignores.add("*sam!*users.1@*.local");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);

        Ignore ignore = new Ignore();
        ignore.setIgnores(s.ignores);

		assertEquals(true, ignore.match("sam!users.1@host.local"));
		assertEquals(true, ignore.match("SAM!userS.1@host.LOCAL"));
		assertEquals(false, ignore.match("harry!users.1@host.local"));
		assertEquals(false, ignore.match("sam!~username@host.local"));
		assertEquals(false, ignore.match("harry!~username@host.local"));
		assertEquals(true, ignore.match("sam!users.1@work.local"));
		assertEquals(false, ignore.match("harry!users.1@work.local"));
		assertEquals(false, ignore.match("sam!~username@work.local"));
		assertEquals(false, ignore.match("harry!~username@work.local"));
	}

	public void testPartialWildcard2() {
		ArrayNode ignores = new ObjectMapper().createArrayNode();
		ignores.add("*a*!*users.1@*.local");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);

        Ignore ignore = new Ignore();
        ignore.setIgnores(s.ignores);

		assertEquals(true, ignore.match("sam!users.1@host.local"));
		assertEquals(true, ignore.match("SAM!userS.1@host.LOCAL"));
		assertEquals(true, ignore.match("harry!users.1@host.local"));
		assertEquals(false, ignore.match("sam!~username@host.local"));
		assertEquals(false, ignore.match("harry!~username@host.local"));
		assertEquals(true, ignore.match("sam!users.1@work.local"));
		assertEquals(true, ignore.match("harry!users.1@work.local"));
		assertEquals(false, ignore.match("sam!~username@work.local"));
		assertEquals(false, ignore.match("harry!~username@work.local"));
	}

	public void testPartialWildcard3() {
		ArrayNode ignores = new ObjectMapper().createArrayNode();
		ignores.add("*!*users.1@*.local");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);

		Ignore ignore = new Ignore();
		ignore.setIgnores(s.ignores);

		assertEquals(true, ignore.match("sam!users.1@host.local"));
		assertEquals(true, ignore.match("SAM!userS.1@host.LOCAL"));
		assertEquals(true, ignore.match("harry!users.1@host.local"));
		assertEquals(false, ignore.match("sam!~username@host.local"));
		assertEquals(false, ignore.match("harry!~username@host.local"));
		assertEquals(true, ignore.match("sam!users.1@work.local"));
		assertEquals(true, ignore.match("harry!users.1@work.local"));
		assertEquals(false, ignore.match("sam!~username@work.local"));
		assertEquals(false, ignore.match("harry!~username@work.local"));
	}
	
	public void testPartialWildcard4() {
		ArrayNode ignores = new ObjectMapper().createArrayNode();
		ignores.add("*!user*@*.local");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(s.ignores);

		assertEquals(true, ignore.match("sam!users.1@host.local"));
		assertEquals(true, ignore.match("SAM!userS.1@host.LOCAL"));
		assertEquals(true, ignore.match("harry!users.1@host.local"));
		assertEquals(true, ignore.match("sam!~username@host.local"));
		assertEquals(true, ignore.match("harry!~username@host.local"));
		assertEquals(true, ignore.match("sam!users.1@work.local"));
		assertEquals(true, ignore.match("harry!users.1@work.local"));
		assertEquals(true, ignore.match("sam!~username@work.local"));
		assertEquals(true, ignore.match("harry!~username@work.local"));
	}

	public void testNoNickNoUsername1() {
		ArrayNode ignores = new ObjectMapper().createArrayNode();
		ignores.add("*!*@host.local");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(s.ignores);

		assertEquals(true, ignore.match("sam!users.1@host.local"));
		assertEquals(true, ignore.match("SAM!userS.1@host.LOCAL"));
		assertEquals(true, ignore.match("harry!users.1@host.local"));
		assertEquals(true, ignore.match("sam!~username@host.local"));
		assertEquals(true, ignore.match("harry!~username@host.local"));
		assertEquals(false, ignore.match("sam!users.1@work.local"));
		assertEquals(false, ignore.match("harry!users.1@work.local"));
		assertEquals(false, ignore.match("sam!~username@work.local"));
		assertEquals(false, ignore.match("harry!~username@work.local"));
	}

	public void testNoNickNoUsername2() {
		ArrayNode ignores = new ObjectMapper().createArrayNode();
		ignores.add("*@host.local");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(s.ignores);

		assertEquals(true, ignore.match("sam!users.1@host.local"));
		assertEquals(true, ignore.match("SAM!userS.1@host.LOCAL"));
		assertEquals(true, ignore.match("harry!users.1@host.local"));
		assertEquals(true, ignore.match("sam!~username@host.local"));
		assertEquals(true, ignore.match("harry!~username@host.local"));
		assertEquals(false, ignore.match("sam!users.1@work.local"));
		assertEquals(false, ignore.match("harry!users.1@work.local"));
		assertEquals(false, ignore.match("sam!~username@work.local"));
		assertEquals(false, ignore.match("harry!~username@work.local"));
	}

	public void testJustNick() {
		ArrayNode ignores = new ObjectMapper().createArrayNode();
		ignores.add("sam");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(s.ignores);

		assertEquals(true, ignore.match("sam!users.1@host.local"));
		assertEquals(true, ignore.match("SAM!userS.1@host.LOCAL"));
		assertEquals(false, ignore.match("harry!users.1@host.local"));
		assertEquals(true, ignore.match("sam!~username@host.local"));
		assertEquals(false, ignore.match("harry!~username@host.local"));
		assertEquals(true, ignore.match("sam!users.1@work.local"));
		assertEquals(false, ignore.match("harry!users.1@work.local"));
		assertEquals(true, ignore.match("sam!~username@work.local"));
		assertEquals(false, ignore.match("harry!~username@work.local"));
	}

	public void testFull1() {
		ArrayNode ignores = new ObjectMapper().createArrayNode();
		ignores.add("sam!users.1@host.local");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(s.ignores);

		assertEquals(true, ignore.match("sam!users.1@host.local"));
		assertEquals(true, ignore.match("SAM!userS.1@host.LOCAL"));
		assertEquals(false, ignore.match("harry!users.1@host.local"));
		assertEquals(false, ignore.match("sam!~username@host.local"));
		assertEquals(false, ignore.match("harry!~username@host.local"));
		assertEquals(false, ignore.match("sam!users.1@work.local"));
		assertEquals(false, ignore.match("harry!users.1@work.local"));
		assertEquals(false, ignore.match("sam!~username@work.local"));
		assertEquals(false, ignore.match("harry!~username@work.local"));
	}

	public void testFull2() {
		ArrayNode ignores = new ObjectMapper().createArrayNode();
		ignores.add("Sam!Users.1@HOST.local");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(s.ignores);

		assertEquals(true, ignore.match("sam!users.1@host.local"));
		assertEquals(true, ignore.match("SAM!userS.1@host.LOCAL"));
		assertEquals(false, ignore.match("harry!users.1@host.local"));
		assertEquals(false, ignore.match("sam!~username@host.local"));
		assertEquals(false, ignore.match("harry!~username@host.local"));
		assertEquals(false, ignore.match("sam!users.1@work.local"));
		assertEquals(false, ignore.match("harry!users.1@work.local"));
		assertEquals(false, ignore.match("sam!~username@work.local"));
		assertEquals(false, ignore.match("harry!~username@work.local"));
	}

	public void testNoNickNoHost() {
		ArrayNode ignores = new ObjectMapper().createArrayNode();
		ignores.add("*!users.1@*");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(s.ignores);

		assertEquals(true, ignore.match("sam!users.1@host.local"));
		assertEquals(true, ignore.match("SAM!userS.1@host.LOCAL"));
		assertEquals(true, ignore.match("harry!users.1@host.local"));
		assertEquals(false, ignore.match("sam!~username@host.local"));
		assertEquals(false, ignore.match("harry!~username@host.local"));
		assertEquals(true, ignore.match("sam!users.1@work.local"));
		assertEquals(true, ignore.match("harry!users.1@work.local"));
		assertEquals(false, ignore.match("sam!~username@work.local"));
		assertEquals(false, ignore.match("harry!~username@work.local"));
	}

	public void testNoUsername1() {
		ArrayNode ignores = new ObjectMapper().createArrayNode();
		ignores.add("sam!*@host.local");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(s.ignores);

		assertEquals(true, ignore.match("sam!users.1@host.local"));
		assertEquals(true, ignore.match("SAM!userS.1@host.LOCAL"));
		assertEquals(false, ignore.match("harry!users.1@host.local"));
		assertEquals(true, ignore.match("sam!~username@host.local"));
		assertEquals(false, ignore.match("harry!~username@host.local"));
		assertEquals(false, ignore.match("sam!users.1@work.local"));
		assertEquals(false, ignore.match("harry!users.1@work.local"));
		assertEquals(false, ignore.match("sam!~username@work.local"));
		assertEquals(false, ignore.match("harry!~username@work.local"));
	}

	public void testNoUsername2() {
		ArrayNode ignores = new ObjectMapper().createArrayNode();
		ignores.add("sam!host.local");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(s.ignores);

		assertEquals(true, ignore.match("sam!users.1@host.local"));
		assertEquals(true, ignore.match("SAM!userS.1@host.LOCAL"));
		assertEquals(false, ignore.match("harry!users.1@host.local"));
		assertEquals(true, ignore.match("sam!~username@host.local"));
		assertEquals(false, ignore.match("harry!~username@host.local"));
		assertEquals(false, ignore.match("sam!users.1@work.local"));
		assertEquals(false, ignore.match("harry!users.1@work.local"));
		assertEquals(false, ignore.match("sam!~username@work.local"));
		assertEquals(false, ignore.match("harry!~username@work.local"));
	}

	public void testNoUsername3() {
		ArrayNode ignores = new ObjectMapper().createArrayNode();
		ignores.add("sam!*.local");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(s.ignores);

		assertEquals(true, ignore.match("sam!users.1@host.local"));
		assertEquals(true, ignore.match("SAM!userS.1@host.LOCAL"));
		assertEquals(false, ignore.match("harry!users.1@host.local"));
		assertEquals(true, ignore.match("sam!~username@host.local"));
		assertEquals(false, ignore.match("harry!~username@host.local"));
		assertEquals(true, ignore.match("sam!users.1@work.local"));
		assertEquals(false, ignore.match("harry!users.1@work.local"));
		assertEquals(true, ignore.match("sam!~username@work.local"));
		assertEquals(false, ignore.match("harry!~username@work.local"));
	}

	public void testNoUsername4() {
		ArrayNode ignores = new ObjectMapper().createArrayNode();
		ignores.add("sam!*o*.local");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(s.ignores);

		assertEquals(true, ignore.match("sam!users.1@host.local"));
		assertEquals(true, ignore.match("SAM!userS.1@host.LOCAL"));
		assertEquals(false, ignore.match("harry!users.1@host.local"));
		assertEquals(true, ignore.match("sam!~username@host.local"));
		assertEquals(false, ignore.match("harry!~username@host.local"));
		assertEquals(true, ignore.match("sam!users.1@work.local"));
		assertEquals(false, ignore.match("harry!users.1@work.local"));
		assertEquals(true, ignore.match("sam!~username@work.local"));
		assertEquals(false, ignore.match("harry!~username@work.local"));
	}

	public void testNoUsername5() {
		ArrayNode ignores = new ObjectMapper().createArrayNode();
		ignores.add("sam!host.*");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(s.ignores);

		assertEquals(true, ignore.match("sam!users.1@host.local"));
		assertEquals(true, ignore.match("SAM!userS.1@host.LOCAL"));
		assertEquals(false, ignore.match("harry!users.1@host.local"));
		assertEquals(true, ignore.match("sam!~username@host.local"));
		assertEquals(false, ignore.match("harry!~username@host.local"));
		assertEquals(false, ignore.match("sam!users.1@work.local"));
		assertEquals(false, ignore.match("harry!users.1@work.local"));
		assertEquals(false, ignore.match("sam!~username@work.local"));
		assertEquals(false, ignore.match("harry!~username@work.local"));
	}

	public void testNoUsernameOrHost1() {
		ArrayNode ignores = new ObjectMapper().createArrayNode();
		ignores.add("sam!*@*");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(s.ignores);

		assertEquals(true, ignore.match("sam!users.1@host.local"));
		assertEquals(true, ignore.match("SAM!userS.1@host.LOCAL"));
		assertEquals(false, ignore.match("harry!users.1@host.local"));
		assertEquals(true, ignore.match("sam!~username@host.local"));
		assertEquals(false, ignore.match("harry!~username@host.local"));
		assertEquals(true, ignore.match("sam!users.1@work.local"));
		assertEquals(false, ignore.match("harry!users.1@work.local"));
		assertEquals(true, ignore.match("sam!~username@work.local"));
		assertEquals(false, ignore.match("harry!~username@work.local"));
	}

	public void testNoUsernameOrHost2() {
		ArrayNode ignores = new ObjectMapper().createArrayNode();
		ignores.add("sam!*");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(s.ignores);

		assertEquals(true, ignore.match("sam!users.1@host.local"));
		assertEquals(true, ignore.match("SAM!userS.1@host.LOCAL"));
		assertEquals(false, ignore.match("harry!users.1@host.local"));
		assertEquals(true, ignore.match("sam!~username@host.local"));
		assertEquals(false, ignore.match("harry!~username@host.local"));
		assertEquals(true, ignore.match("sam!users.1@work.local"));
		assertEquals(false, ignore.match("harry!users.1@work.local"));
		assertEquals(true, ignore.match("sam!~username@work.local"));
		assertEquals(false, ignore.match("harry!~username@work.local"));
	}

	public void testUsernameAlone() {
		ArrayNode ignores = new ObjectMapper().createArrayNode();
		ignores.add("sam!~username");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(s.ignores);

		assertEquals(false, ignore.match("sam!users.1@host.local"));
		assertEquals(false, ignore.match("SAM!userS.1@host.LOCAL"));
		assertEquals(false, ignore.match("harry!users.1@host.local"));
		assertEquals(false, ignore.match("sam!~username@host.local"));
		assertEquals(false, ignore.match("harry!~username@host.local"));
		assertEquals(false, ignore.match("sam!users.1@work.local"));
		assertEquals(false, ignore.match("harry!users.1@work.local"));
		assertEquals(false, ignore.match("sam!~username@work.local"));
		assertEquals(false, ignore.match("harry!~username@work.local"));
	}

	public void testAllWildcards1() {
		ArrayNode ignores = new ObjectMapper().createArrayNode();
		ignores.add("*!*@*");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(s.ignores);

		assertEquals(false, ignore.match("sam!users.1@host.local"));
		assertEquals(false, ignore.match("SAM!userS.1@host.LOCAL"));
		assertEquals(false, ignore.match("harry!users.1@host.local"));
		assertEquals(false, ignore.match("sam!~username@host.local"));
		assertEquals(false, ignore.match("harry!~username@host.local"));
		assertEquals(false, ignore.match("sam!users.1@work.local"));
		assertEquals(false, ignore.match("harry!users.1@work.local"));
		assertEquals(false, ignore.match("sam!~username@work.local"));
		assertEquals(false, ignore.match("harry!~username@work.local"));
	}

	public void testAllWildcards2() {
		ArrayNode ignores = new ObjectMapper().createArrayNode();
		ignores.add("*!*");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(s.ignores);

		assertEquals(false, ignore.match("sam!users.1@host.local"));
		assertEquals(false, ignore.match("SAM!userS.1@host.LOCAL"));
		assertEquals(false, ignore.match("harry!users.1@host.local"));
		assertEquals(false, ignore.match("sam!~username@host.local"));
		assertEquals(false, ignore.match("harry!~username@host.local"));
		assertEquals(false, ignore.match("sam!users.1@work.local"));
		assertEquals(false, ignore.match("harry!users.1@work.local"));
		assertEquals(false, ignore.match("sam!~username@work.local"));
		assertEquals(false, ignore.match("harry!~username@work.local"));
	}

	public void testAllWildcards3() {
		ArrayNode ignores = new ObjectMapper().createArrayNode();
		ignores.add("*");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(s.ignores);

		assertEquals(false, ignore.match("sam!users.1@host.local"));
		assertEquals(false, ignore.match("SAM!userS.1@host.LOCAL"));
		assertEquals(false, ignore.match("harry!users.1@host.local"));
		assertEquals(false, ignore.match("sam!~username@host.local"));
		assertEquals(false, ignore.match("harry!~username@host.local"));
		assertEquals(false, ignore.match("sam!users.1@work.local"));
		assertEquals(false, ignore.match("harry!users.1@work.local"));
		assertEquals(false, ignore.match("sam!~username@work.local"));
		assertEquals(false, ignore.match("harry!~username@work.local"));
	}

    public void testBrackets() {
        ArrayNode ignores = new ObjectMapper().createArrayNode();
        ignores.add("(sam)");
        ignores.add("{sam}");
        ignores.add("[sam]");
        Server s = ServersList.getInstance().createServer(0,"","",0,"","",0,0,"","","","",null,"",ignores,0);

        Ignore ignore = new Ignore();
        ignore.setIgnores(s.ignores);

        assertEquals(false, ignore.match("sam!users.1@host.local"));
        assertEquals(false, ignore.match("SAM!userS.1@host.LOCAL"));
        assertEquals(false, ignore.match("harry!users.1@host.local"));
        assertEquals(false, ignore.match("sam!~username@host.local"));
        assertEquals(false, ignore.match("harry!~username@host.local"));
        assertEquals(false, ignore.match("sam!users.1@work.local"));
        assertEquals(false, ignore.match("harry!users.1@work.local"));
        assertEquals(false, ignore.match("sam!~username@work.local"));
        assertEquals(false, ignore.match("harry!~username@work.local"));
        assertEquals(true, ignore.match("(sam)!users.1@host.local"));
        assertEquals(true, ignore.match("{sam}!users.1@host.local"));
        assertEquals(true, ignore.match("[sam]!users.1@host.local"));
    }
}
