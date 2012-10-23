package com.irccloud.android.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.irccloud.android.Ignore;

import junit.framework.TestCase;

public class IgnoreTests extends TestCase {

	public void testNoNickWithDot() {
		JsonArray ignores = new JsonArray();
		ignores.add(new JsonPrimitive("*!users.1@host.local"));
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(ignores);

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
		JsonArray ignores = new JsonArray();
		ignores.add(new JsonPrimitive("*!username@host.local"));
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(ignores);

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
		JsonArray ignores = new JsonArray();
		ignores.add(new JsonPrimitive("*!~username@host.local"));
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(ignores);

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
		JsonArray ignores = new JsonArray();
		ignores.add(new JsonPrimitive("sam!users.1@*"));
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(ignores);

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
		JsonArray ignores = new JsonArray();
		ignores.add(new JsonPrimitive("host.local"));
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(ignores);

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
		JsonArray ignores = new JsonArray();
		ignores.add(new JsonPrimitive("*sam!*users.1@*.local"));
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(ignores);

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
		JsonArray ignores = new JsonArray();
		ignores.add(new JsonPrimitive("*a*!*users.1@*.local"));
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(ignores);

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
		JsonArray ignores = new JsonArray();
		ignores.add(new JsonPrimitive("*!*users.1@*.local"));
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(ignores);

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
		JsonArray ignores = new JsonArray();
		ignores.add(new JsonPrimitive("*!user*@*.local"));
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(ignores);

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
		JsonArray ignores = new JsonArray();
		ignores.add(new JsonPrimitive("*!*@host.local"));
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(ignores);

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
		JsonArray ignores = new JsonArray();
		ignores.add(new JsonPrimitive("*@host.local"));
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(ignores);

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
		JsonArray ignores = new JsonArray();
		ignores.add(new JsonPrimitive("sam"));
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(ignores);

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
		JsonArray ignores = new JsonArray();
		ignores.add(new JsonPrimitive("sam!users.1@host.local"));
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(ignores);

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
		JsonArray ignores = new JsonArray();
		ignores.add(new JsonPrimitive("Sam!Users.1@HOST.local"));
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(ignores);

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
		JsonArray ignores = new JsonArray();
		ignores.add(new JsonPrimitive("*!users.1@*"));
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(ignores);

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
		JsonArray ignores = new JsonArray();
		ignores.add(new JsonPrimitive("sam!*@host.local"));
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(ignores);

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
		JsonArray ignores = new JsonArray();
		ignores.add(new JsonPrimitive("sam!host.local"));
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(ignores);

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
		JsonArray ignores = new JsonArray();
		ignores.add(new JsonPrimitive("sam!*.local"));
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(ignores);

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
		JsonArray ignores = new JsonArray();
		ignores.add(new JsonPrimitive("sam!*o*.local"));
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(ignores);

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
		JsonArray ignores = new JsonArray();
		ignores.add(new JsonPrimitive("sam!host.*"));
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(ignores);

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
		JsonArray ignores = new JsonArray();
		ignores.add(new JsonPrimitive("sam!*@*"));
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(ignores);

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
		JsonArray ignores = new JsonArray();
		ignores.add(new JsonPrimitive("sam!*"));
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(ignores);

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
		JsonArray ignores = new JsonArray();
		ignores.add(new JsonPrimitive("sam!~username"));
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(ignores);

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
		JsonArray ignores = new JsonArray();
		ignores.add(new JsonPrimitive("*!*@*"));
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(ignores);

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
		JsonArray ignores = new JsonArray();
		ignores.add(new JsonPrimitive("*!*"));
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(ignores);

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
		JsonArray ignores = new JsonArray();
		ignores.add(new JsonPrimitive("*"));
		
		Ignore ignore = new Ignore();
		ignore.setIgnores(ignores);

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

}
