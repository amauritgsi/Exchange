package br.com.exchange.grpc.service.dto;

import io.grpc.stub.ServerCallStreamObserver;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;

class OfferTest {

	@Test
	void testGet() {
		ServerCallStreamObserver observer = Mockito.mock(ServerCallStreamObserver.class);
		Offer instance = new Offer(1D, 1L, 1L, observer, 1, 1L);
		assertEquals(1D, instance.getPrice());
		assertEquals(1L, instance.getTotalQty());
		assertEquals(1L, instance.getQty());
		assertEquals(observer, instance.getObserver());
		assertEquals(1, instance.getOrderId());
		assertEquals(1L, instance.getClOrdID());
		instance.setQty(2L);
		assertEquals(2L, instance.getQty());

	}
}
