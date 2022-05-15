package br.com.exchange.grpc.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.protobuf.Empty;

import br.com.exchange.grpc.service.dto.Offer;
import br.com.exchange.proto.Book;
import br.com.exchange.proto.MDOpenSessionRequest;
import br.com.exchange.proto.Side;
import br.com.exchange.proto.SubscribeSymbolRequest;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.ServerCallStreamObserver;

class MarketDataServiceImplTest {

	private static final String X = "X";
	private MarketDataServiceImpl marketDataServiceImpl;

	private Object getPrivateFieldValue(String fieldName, Object instance)
			throws NoSuchFieldException, IllegalAccessException {
		Field field = MarketDataServiceImpl.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(instance);
	}

	private Map<String, List<ServerCallStreamObserver<Book>>> getSessionsBySymbol()
			throws NoSuchFieldException, IllegalAccessException {
		return (Map<String, List<ServerCallStreamObserver<Book>>>) getPrivateFieldValue("sessionsBySymbol",
				marketDataServiceImpl);
	}

	private Map<String, ServerCallStreamObserver<Book>> getSessions()
			throws NoSuchFieldException, IllegalAccessException {
		return (Map<String, ServerCallStreamObserver<Book>>) getPrivateFieldValue("sessions", marketDataServiceImpl);
	}

	@BeforeEach
	void createMarketDataServiceImpl() {
		marketDataServiceImpl = new MarketDataServiceImpl();
	}

	@Test
	void testOpenSession() throws Exception {
		ServerCallStreamObserver observer = Mockito.mock(ServerCallStreamObserver.class);
		marketDataServiceImpl.openSession(MDOpenSessionRequest.newBuilder().setGuid(X).build(), observer);
		assertFalse(getSessions().isEmpty());
	}

	@Test
	void testSubscribeSymbol() throws Exception {
		ClientResponseObserver<String, Empty> observer = Mockito.mock(ClientResponseObserver.class);
		marketDataServiceImpl.subscribeSymbol(SubscribeSymbolRequest.newBuilder().setGuid(X).setSymbol(X).build(),
				observer);
		marketDataServiceImpl.subscribeSymbol(SubscribeSymbolRequest.newBuilder().setGuid(X).setSymbol(X).build(),
				observer);

		assertTrue(getSessionsBySymbol().get(X).isEmpty());
	}

	@Test
	void testSendBookEmpty() throws Exception {
		
		Map<Side, List<Offer>> map = new HashMap<>();
		marketDataServiceImpl.sendBook(X, map);
		assertFalse(getSessionsBySymbol().containsKey(X));
	}

	@Test
	void testSendBook() throws Exception {

		ServerCallStreamObserver observer = Mockito.mock(ServerCallStreamObserver.class);
		Map<Side, List<Offer>> map = new HashMap<>();
		map.put(Side.BUY, new ArrayList<>());
		map.put(Side.SELL, new ArrayList<>());

		map.get(Side.BUY).add(new Offer(1D, 1L, 1L, observer, 1, 1));
		map.get(Side.SELL).add(new Offer(1D, 1L, 1L, observer, 2, 2));
		marketDataServiceImpl.sendBook(X, map);

		Map<String, List<ServerCallStreamObserver<Book>>> sessions = getSessionsBySymbol();
		sessions.put(X, new ArrayList<ServerCallStreamObserver<Book>>());
		sessions.get(X).add(null);

		marketDataServiceImpl.sendBook(X, map);
		assertFalse(getSessionsBySymbol().isEmpty());
	}

	@Test
	void testOpenSessionAndSubscribe() throws Exception {
		ServerCallStreamObserver observer = Mockito.mock(ServerCallStreamObserver.class);
		marketDataServiceImpl.openSession(MDOpenSessionRequest.newBuilder().setGuid(X).build(), observer);
		ClientResponseObserver<String, Empty> observer2 = Mockito.mock(ClientResponseObserver.class);
		marketDataServiceImpl.subscribeSymbol(SubscribeSymbolRequest.newBuilder().setGuid(X).setSymbol(X).build(),
				observer2);
		assertFalse(getSessionsBySymbol().get(X).isEmpty());
	}

	@Test
	void testOpenSessionAndSubscribeAndSendEmptyBook() throws Exception {
		
		ServerCallStreamObserver observer = Mockito.mock(ServerCallStreamObserver.class);
		marketDataServiceImpl.openSession(MDOpenSessionRequest.newBuilder().setGuid(X).build(), observer);
		ClientResponseObserver<String, Empty> observer2 = Mockito.mock(ClientResponseObserver.class);
		marketDataServiceImpl.subscribeSymbol(SubscribeSymbolRequest.newBuilder().setGuid(X).setSymbol(X).build(),
				observer2);
		Map<Side, List<Offer>> map = new HashMap<>();
		marketDataServiceImpl.sendBook(X, map);
		assertFalse(getSessionsBySymbol().get(X).isEmpty());
	}

	@Test
	void testOpenSessionAndSubscribeAndSendEmptyOffersBook() throws Exception {
		
		ServerCallStreamObserver observer = Mockito.mock(ServerCallStreamObserver.class);
		marketDataServiceImpl.openSession(MDOpenSessionRequest.newBuilder().setGuid(X).build(), observer);
		ClientResponseObserver<String, Empty> observer2 = Mockito.mock(ClientResponseObserver.class);
		marketDataServiceImpl.subscribeSymbol(SubscribeSymbolRequest.newBuilder().setGuid(X).setSymbol(X).build(),
				observer2);
		Map<Side, List<Offer>> map = new HashMap<>();
		map.put(Side.BUY, new ArrayList<>());
		map.put(Side.SELL, new ArrayList<>());
		marketDataServiceImpl.sendBook(X, map);
		assertFalse(getSessionsBySymbol().get(X).isEmpty());
	}

	@Test
	void testOpenSessionAndSubscribeAndSendBook() throws Exception {
		
		ServerCallStreamObserver observer = Mockito.mock(ServerCallStreamObserver.class);
		marketDataServiceImpl.openSession(MDOpenSessionRequest.newBuilder().setGuid(X).build(), observer);
		ClientResponseObserver<String, Empty> observer2 = Mockito.mock(ClientResponseObserver.class);
		marketDataServiceImpl.subscribeSymbol(SubscribeSymbolRequest.newBuilder().setGuid(X).setSymbol(X).build(),
				observer2);
		Map<Side, List<Offer>> map = new HashMap<>();
		map.put(Side.BUY, new ArrayList<>());
		map.put(Side.SELL, new ArrayList<>());

		map.get(Side.BUY).add(new Offer(1D, 1L, 1L, observer, 1, 1));
		map.get(Side.SELL).add(new Offer(1D, 1L, 1L, observer, 2, 2));
		marketDataServiceImpl.sendBook(X, map);
		assertFalse(getSessionsBySymbol().get(X).isEmpty());

	}

}
