
package br.com.exchange.grpc.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import br.com.exchange.proto.Identification;

class UtilTest {

	@Test
	void testGetKey_Identification() {
		assertEquals("1-1", Util.getKey(Identification.newBuilder().setTargetCompId("1").setSenderCompId("1").build()));
	}

	@Test
	void testGetKey_String_String() {
		assertEquals("1-1", Util.getKey("1", "1"));
	}

}
