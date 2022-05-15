package br.com.exchange.grpc.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.mockito.internal.matchers.Any;

import br.com.exchange.grpc.service.dto.Offer;
import br.com.exchange.proto.ExecutionReport;
import br.com.exchange.proto.Identification;
import br.com.exchange.proto.NewOrderSingle;
import br.com.exchange.proto.OrdType;
import br.com.exchange.proto.Side;
import io.grpc.stub.ServerCallStreamObserver;

class EntryPointServiceImplTest {

	private EntryPointServiceImpl entryPointServiceImpl;

	@BeforeEach
	void createEntryPointServiceImpl() {
		MarketDataServiceImpl marketDataServiceImpl = Mockito.mock(MarketDataServiceImpl.class);
		entryPointServiceImpl = Mockito.spy(new EntryPointServiceImpl(marketDataServiceImpl));
	}

	private Object getPrivateFieldValue(String fieldName, Object instance)
			throws NoSuchFieldException, IllegalAccessException {
		Field field = EntryPointServiceImpl.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(instance);
	}

	@Test
	void testOpenSession() throws Exception {
		ServerCallStreamObserver observer = Mockito.mock(ServerCallStreamObserver.class);
		entryPointServiceImpl.openSession(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build(),
				observer);

		Map<String, ServerCallStreamObserver<ExecutionReport>> map = (Map<String, ServerCallStreamObserver<ExecutionReport>>) getPrivateFieldValue(
				"sessions", entryPointServiceImpl);
		assertTrue(map.containsKey("1-1"));
	}

	@Test
	void testSubmitOrder() throws Exception {
		ServerCallStreamObserver observer = Mockito.mock(ServerCallStreamObserver.class);
		entryPointServiceImpl.submitOrder(NewOrderSingle.newBuilder()
				.setIdentification(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build())
				.setClOrdID(1L).setOrderQty(1L).setOrdType(OrdType.LIMIT).setPrice(1D).setSide(Side.BUY).setSymbol("X")
				.build(), observer);

		assertTrue(getBook().isEmpty());
	}

	private Map<String, Map<Side, List<Offer>>> getBook() throws NoSuchFieldException, IllegalAccessException {
		return (Map<String, Map<Side, List<Offer>>>) getPrivateFieldValue("book", entryPointServiceImpl);
	}

	@Test
	void testOpenSessionAndSubmitOrder() throws Exception {
		ServerCallStreamObserver observer = Mockito.mock(ServerCallStreamObserver.class);
		entryPointServiceImpl.openSession(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build(),
				observer);

		entryPointServiceImpl.submitOrder(NewOrderSingle.newBuilder()
				.setIdentification(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build())
				.setClOrdID(1L).setOrderQty(1L).setOrdType(OrdType.LIMIT).setPrice(1D).setSide(Side.BUY).setSymbol("X")
				.build(), observer);
		assertFalse(getBook().isEmpty());
	}

	@ParameterizedTest
	@CsvSource({ "1, 1", "1, 2", "2, 1", })
	void testOpenSessionAndSubmitOrderTwoBuys(double priceOne, double priceTwo) throws Exception {
		ServerCallStreamObserver observer = Mockito.mock(ServerCallStreamObserver.class);
		entryPointServiceImpl.openSession(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build(),
				observer);

		entryPointServiceImpl.submitOrder(NewOrderSingle.newBuilder()
				.setIdentification(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build())
				.setClOrdID(1L).setOrderQty(1L).setOrdType(OrdType.LIMIT).setPrice(priceOne).setSide(Side.BUY)
				.setSymbol("X").build(), observer);
		entryPointServiceImpl.submitOrder(NewOrderSingle.newBuilder()
				.setIdentification(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build())
				.setClOrdID(2L).setOrderQty(1L).setOrdType(OrdType.LIMIT).setPrice(priceTwo).setSide(Side.BUY)
				.setSymbol("X").build(), observer);

		assertFalse(getBook().isEmpty());
	}

	@ParameterizedTest
	@CsvSource({ "1, 1", "1, 2", "2, 1" })
	void testOpenSessionAndSubmitOrderTwoSell(double priceOne, double priceTwo) throws Exception {
		ServerCallStreamObserver observer = Mockito.mock(ServerCallStreamObserver.class);
		entryPointServiceImpl.openSession(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build(),
				observer);

		entryPointServiceImpl.submitOrder(NewOrderSingle.newBuilder()
				.setIdentification(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build())
				.setClOrdID(1L).setOrderQty(1L).setOrdType(OrdType.LIMIT).setPrice(priceOne).setSide(Side.SELL).setSymbol("X")
				.build(), observer);
		entryPointServiceImpl.submitOrder(NewOrderSingle.newBuilder()
				.setIdentification(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build())
				.setClOrdID(2L).setOrderQty(1L).setOrdType(OrdType.LIMIT).setPrice(priceTwo).setSide(Side.SELL).setSymbol("X")
				.build(), observer);
		assertFalse(getBook().isEmpty());
	}

	@Test
	void testOpenSessionAndSubmitOrderTwoBuysAndOneSell() throws Exception {
		ServerCallStreamObserver observer = Mockito.mock(ServerCallStreamObserver.class);
		entryPointServiceImpl.openSession(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build(),
				observer);

		entryPointServiceImpl.submitOrder(NewOrderSingle.newBuilder()
				.setIdentification(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build())
				.setClOrdID(1L).setOrderQty(1L).setOrdType(OrdType.LIMIT).setPrice(1D).setSide(Side.BUY).setSymbol("X")
				.build(), observer);
		entryPointServiceImpl.submitOrder(NewOrderSingle.newBuilder()
				.setIdentification(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build())
				.setClOrdID(2L).setOrderQty(1L).setOrdType(OrdType.LIMIT).setPrice(1D).setSide(Side.BUY).setSymbol("X")
				.build(), observer);
		entryPointServiceImpl.submitOrder(NewOrderSingle.newBuilder()
				.setIdentification(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build())
				.setClOrdID(3L).setOrderQty(2L).setOrdType(OrdType.LIMIT).setPrice(1D).setSide(Side.SELL).setSymbol("X")
				.build(), observer);
		assertFalse(getBook().isEmpty());
	}

	@Test
	void testOpenSessionAndSubmitOrderOneWithTwoQtyBuysAndOneSell() throws Exception {
		ServerCallStreamObserver observer = Mockito.mock(ServerCallStreamObserver.class);
		entryPointServiceImpl.openSession(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build(),
				observer);

		entryPointServiceImpl.submitOrder(NewOrderSingle.newBuilder()
				.setIdentification(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build())
				.setClOrdID(1L).setOrderQty(2L).setOrdType(OrdType.LIMIT).setPrice(1D).setSide(Side.BUY).setSymbol("X")
				.build(), observer);
		entryPointServiceImpl.submitOrder(NewOrderSingle.newBuilder()
				.setIdentification(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build())
				.setClOrdID(3L).setOrderQty(1L).setOrdType(OrdType.LIMIT).setPrice(1D).setSide(Side.SELL).setSymbol("X")
				.build(), observer);
		assertFalse(getBook().isEmpty());
	}

	@Test
	void testOpenSessionAndSubmitOrderTwoBuysAndTwoSell() throws Exception {
		ServerCallStreamObserver observer = Mockito.mock(ServerCallStreamObserver.class);
		entryPointServiceImpl.openSession(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build(),
				observer);

		entryPointServiceImpl.submitOrder(NewOrderSingle.newBuilder()
				.setIdentification(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build())
				.setClOrdID(1L).setOrderQty(1L).setOrdType(OrdType.LIMIT).setPrice(1D).setSide(Side.BUY).setSymbol("X")
				.build(), observer);
		entryPointServiceImpl.submitOrder(NewOrderSingle.newBuilder()
				.setIdentification(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build())
				.setClOrdID(2L).setOrderQty(1L).setOrdType(OrdType.LIMIT).setPrice(1D).setSide(Side.BUY).setSymbol("X")
				.build(), observer);
		entryPointServiceImpl.submitOrder(NewOrderSingle.newBuilder()
				.setIdentification(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build())
				.setClOrdID(3L).setOrderQty(1L).setOrdType(OrdType.LIMIT).setPrice(1D).setSide(Side.SELL).setSymbol("X")
				.build(), observer);
		entryPointServiceImpl.submitOrder(NewOrderSingle.newBuilder()
				.setIdentification(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build())
				.setClOrdID(4L).setOrderQty(1L).setOrdType(OrdType.LIMIT).setPrice(1D).setSide(Side.SELL).setSymbol("X")
				.build(), observer);
		assertFalse(getBook().isEmpty());
	}

	@Test
	void testOpenSessionAndSubmitOrderTwoBuysAndThreeSell() throws Exception {
		ServerCallStreamObserver observer = Mockito.mock(ServerCallStreamObserver.class);
		entryPointServiceImpl.openSession(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build(),
				observer);

		entryPointServiceImpl.submitOrder(NewOrderSingle.newBuilder()
				.setIdentification(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build())
				.setClOrdID(1L).setOrderQty(1L).setOrdType(OrdType.LIMIT).setPrice(1D).setSide(Side.BUY).setSymbol("X")
				.build(), observer);
		entryPointServiceImpl.submitOrder(NewOrderSingle.newBuilder()
				.setIdentification(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build())
				.setClOrdID(2L).setOrderQty(1L).setOrdType(OrdType.LIMIT).setPrice(1D).setSide(Side.BUY).setSymbol("X")
				.build(), observer);
		entryPointServiceImpl.submitOrder(NewOrderSingle.newBuilder()
				.setIdentification(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build())
				.setClOrdID(3L).setOrderQty(1L).setOrdType(OrdType.LIMIT).setPrice(1D).setSide(Side.SELL).setSymbol("X")
				.build(), observer);
		entryPointServiceImpl.submitOrder(NewOrderSingle.newBuilder()
				.setIdentification(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build())
				.setClOrdID(4L).setOrderQty(1L).setOrdType(OrdType.LIMIT).setPrice(1D).setSide(Side.SELL).setSymbol("X")
				.build(), observer);
		entryPointServiceImpl.submitOrder(NewOrderSingle.newBuilder()
				.setIdentification(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build())
				.setClOrdID(5L).setOrderQty(1L).setOrdType(OrdType.LIMIT).setPrice(1D).setSide(Side.SELL).setSymbol("X")
				.build(), observer);
		assertFalse(getBook().isEmpty());
	}

	@ParameterizedTest
	@CsvSource({ "2, 1, 1", "1, 2, 1", "2, 1, 0.5", "1, 2, 2" })
	void testOpenSessionAndSubmitOrderOneSellWithTwoQtyAndOneBuy(long qtdOne, long qtdtwo, double priceTwo)
			throws Exception {
		ServerCallStreamObserver observer = Mockito.mock(ServerCallStreamObserver.class);
		entryPointServiceImpl.openSession(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build(),
				observer);
		entryPointServiceImpl.submitOrder(NewOrderSingle.newBuilder()
				.setIdentification(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build())
				.setClOrdID(1L).setOrderQty(qtdOne).setOrdType(OrdType.LIMIT).setPrice(1D).setSide(Side.SELL)
				.setSymbol("X").build(), observer);
		entryPointServiceImpl.submitOrder(NewOrderSingle.newBuilder()
				.setIdentification(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build())
				.setClOrdID(2L).setOrderQty(qtdtwo).setOrdType(OrdType.LIMIT).setPrice(priceTwo).setSide(Side.BUY)
				.setSymbol("X").build(), observer);
		assertFalse(getBook().isEmpty());
	}

	@ParameterizedTest
	@CsvSource({ "2, 1, 1", "1, 2, 1", "2, 1, 0.5", "2, 1, 2" })
	void testOpenSessionAndSubmitOrderOneBuyWithTwoQtyAndOneSellWithPriceOutOfRange(long qtdOne, long qtdtwo, double priceTwo) throws Exception {
		ServerCallStreamObserver observer = Mockito.mock(ServerCallStreamObserver.class);
		entryPointServiceImpl.openSession(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build(),
				observer);
		entryPointServiceImpl.submitOrder(NewOrderSingle.newBuilder()
				.setIdentification(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build())
				.setClOrdID(1L).setOrderQty(qtdOne).setOrdType(OrdType.LIMIT).setPrice(1D).setSide(Side.BUY).setSymbol("X")
				.build(), observer);
		entryPointServiceImpl.submitOrder(NewOrderSingle.newBuilder()
				.setIdentification(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build())
				.setClOrdID(2L).setOrderQty(qtdtwo).setOrdType(OrdType.LIMIT).setPrice(priceTwo).setSide(Side.SELL).setSymbol("X")
				.build(), observer);
		assertFalse(getBook().isEmpty());
	}

	@Test
	void testOpenSessionAndSubmitOrderOneSellWithTwoQtyAndOneBuyWithOrderTypeMarket() throws Exception {
		ServerCallStreamObserver observer = Mockito.mock(ServerCallStreamObserver.class);
		entryPointServiceImpl.openSession(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build(),
				observer);
		entryPointServiceImpl.submitOrder(NewOrderSingle.newBuilder()
				.setIdentification(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build())
				.setClOrdID(1L).setOrderQty(2L).setOrdType(OrdType.LIMIT).setPrice(1D).setSide(Side.SELL).setSymbol("X")
				.build(), observer);
		entryPointServiceImpl.submitOrder(NewOrderSingle.newBuilder()
				.setIdentification(Identification.newBuilder().setSenderCompId("1").setTargetCompId("1").build())
				.setClOrdID(2L).setOrderQty(1L).setOrdType(OrdType.MARKET).setPrice(0.0D).setSide(Side.BUY)
				.setSymbol("X").build(), observer);
		assertFalse(getBook().isEmpty());
	}

}
