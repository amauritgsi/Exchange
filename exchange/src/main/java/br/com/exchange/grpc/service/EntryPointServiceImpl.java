package br.com.exchange.grpc.service;

import static br.com.exchange.grpc.util.Util.getKey;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;

import br.com.exchange.grpc.service.dto.Offer;
import br.com.exchange.proto.EntryPointServiceGrpc.EntryPointServiceImplBase;
import br.com.exchange.proto.ExecType;
import br.com.exchange.proto.ExecutionReport;
import br.com.exchange.proto.Identification;
import br.com.exchange.proto.NewOrderSingle;
import br.com.exchange.proto.OrdStatus;
import br.com.exchange.proto.OrdType;
import br.com.exchange.proto.Side;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class EntryPointServiceImpl extends EntryPointServiceImplBase {

	private static final Logger logger = Logger.getLogger(EntryPointServiceImpl.class.getName());
	private final Map<String, ServerCallStreamObserver<ExecutionReport>> sessions = new ConcurrentHashMap<>();
	private AtomicInteger execId = new AtomicInteger();
	private AtomicInteger orderId = new AtomicInteger();
	private final Map<String, Map<Side, List<Offer>>> book = new ConcurrentHashMap<>();
	private final MarketDataServiceImpl marketDataServiceImpl;

	public EntryPointServiceImpl(MarketDataServiceImpl marketDataServiceImpl) {
		this.marketDataServiceImpl = marketDataServiceImpl;
	}

	@Override
	public void openSession(Identification request, StreamObserver<ExecutionReport> responseObserver) {
		ServerCallStreamObserver<ExecutionReport> session = (ServerCallStreamObserver<ExecutionReport>) responseObserver;
		sessions.put(getKey(request), session);
		session.setCompression("gzip");
		session.setOnCancelHandler(() -> {
			logger.info("OnCancelHandler");
			String keyToRemove = null;
			for (Map.Entry<String, ServerCallStreamObserver<ExecutionReport>> entry : sessions.entrySet()) {
				String key = entry.getKey();
				ServerCallStreamObserver<ExecutionReport> val = entry.getValue();
				if (session.equals(val)) {
					keyToRemove = key;
				}
			}
			if (keyToRemove != null) {
				sessions.remove(keyToRemove);
			}
		});
	}

	@Override
	public void submitOrder(NewOrderSingle request, StreamObserver<Empty> responseObserver) {
		responseObserver.onNext(Empty.getDefaultInstance());
		responseObserver.onCompleted();
		if (!sessions.containsKey(getKey(request.getIdentification()))) {
			return;
		}
		proccessOrder(request);
	}

	private void proccessOrder(NewOrderSingle request) {
		int id = orderId.incrementAndGet();

		makeBook(request);

		sendNewOrderAccepted(request, id);

		long filledQty = 0;

		filledQty = hitsTheBook(request, id);

		insertOrderOnBook(request, filledQty, id);

		marketDataServiceImpl.sendBook(request.getSymbol(), book.get(request.getSymbol()));

	}

	private long hitsTheBook(NewOrderSingle request, int id) {
		long filledQty = 0;
		if (book.get(request.getSymbol()).containsKey(request.getSide().equals(Side.BUY) ? Side.SELL : Side.BUY)) {
			List<Offer> offers = book.get(request.getSymbol())
					.get(request.getSide().equals(Side.BUY) ? Side.SELL : Side.BUY);
			List<Offer> offersToRemove = new ArrayList<>();
			filledQty = iterateOverOffers(request, offers, offersToRemove, id);
			offers.removeAll(offersToRemove);
		}
		return filledQty;
	}

	private void sendNewOrderAccepted(NewOrderSingle request, int id) {
		sessions.get(getKey(request.getIdentification())).onNext(makeNewOrder(request, id));
	}

	private void makeBook(NewOrderSingle request) {
		if (!book.containsKey(request.getSymbol())) {
			book.put(request.getSymbol(), new ConcurrentHashMap<>());
		}
		if (!book.get(request.getSymbol()).containsKey(request.getSide())) {
			book.get(request.getSymbol()).put(request.getSide(), new ArrayList<>());
		}
	}

	private void insertOrderOnBook(NewOrderSingle request, long filledQty, int id) {
		if (filledQty < request.getOrderQty()) {
			List<Offer> offers = book.get(request.getSymbol()).get(request.getSide());
			int position = 0;
			for (Offer offer : offers) {
				if (matchPositionOnBook(request, offer)) {
					break;
				}
				position++;
			}
			offers.add(position, new Offer(request.getPrice(), request.getOrderQty(), request.getOrderQty() - filledQty,
					sessions.get(getKey(request.getIdentification())), id, request.getClOrdID()));
		}
	}

	private boolean matchPositionOnBook(NewOrderSingle request, Offer offer) {
		return (request.getSide() == Side.BUY && request.getPrice() > offer.getPrice())
				|| (request.getSide() == Side.SELL && request.getPrice() < offer.getPrice());
	}

	private ExecutionReport makeNewOrder(NewOrderSingle request, int id) {
		return ExecutionReport.newBuilder().setClOrdID(request.getClOrdID()).setExecID(execId.incrementAndGet())
				.setExecPrice(0L).setExecQty(0L).setExecType(ExecType.ET_NEW).setOrderID(id).setOrdStatus(OrdStatus.NEW)
				.setQty(request.getOrderQty()).setSide(request.getSide()).setSymbol(request.getSymbol())
				.setTransactTime(getNow()).build();
	}

	private Timestamp getNow() {
		Instant time = Instant.now();
		return Timestamp.newBuilder().setSeconds(time.getEpochSecond()).setNanos(time.getNano()).build();
	}

	private long iterateOverOffers(NewOrderSingle request, List<Offer> offers, List<Offer> offersToRemove, int id) {
		long filledQty = 0;
		for (Offer offer : offers) {
			if (filledQty >= request.getOrderQty()) {
				break;
			}
			long currentQty = request.getOrderQty() - filledQty;
			if (matchPrice(request, offer)) {
				if (currentQty >= offer.getQty()) {
					offersToRemove.add(offer);
				}

				fillOffer(request, offer, currentQty);
				fillOrder(request, id, offer, currentQty);

				if (currentQty >= offer.getQty()) {
					filledQty += offer.getQty();
				} else {
					filledQty += currentQty;
				}
				
				partialFillOrder(request, id, offer, currentQty);
				partialFillOffer(request, offer, currentQty);
				
			}
		}
		return filledQty;
	}

	private boolean matchPrice(NewOrderSingle request, Offer offer) {
		return (request.getOrdType().equals(OrdType.MARKET))
				|| (request.getSide().equals(Side.BUY) && offer.getPrice() <= request.getPrice())
				|| (request.getSide().equals(Side.SELL) && offer.getPrice() >= request.getPrice());
	}

	private void partialFillOrder(NewOrderSingle request, int id, Offer offer, long currentQty) {
		if (currentQty > offer.getQty()) {
			sessions.get(getKey(request.getIdentification()))
					.onNext(makePartialFill(request.getSymbol(), request.getOrderQty(), offer.getQty(),
							request.getSide(), id, offer.getPrice(), request.getClOrdID()));
		}
	}

	private void partialFillOffer(NewOrderSingle request, Offer offer, long currentQty) {
		if (currentQty < offer.getQty()) {
			offer.getObserver().onNext(makePartialFill(request.getSymbol(), offer.getTotalQty(), currentQty,
					invertSide(request), offer.getOrderId(), offer.getPrice(), offer.getClOrdID()));
			offer.setQty(offer.getQty() - currentQty);
		}
	}

	private void fillOrder(NewOrderSingle request, int id, Offer offer, long currentQty) {
		if (currentQty <= offer.getQty()) {
			sessions.get(getKey(request.getIdentification())).onNext(makeFilled(request.getSymbol(),
					request.getOrderQty(), currentQty, request.getSide(), id, offer.getPrice(), request.getClOrdID()));
		}
	}

	private void fillOffer(NewOrderSingle request, Offer offer, long currentQty) {
		if (currentQty >= offer.getQty()) {
			offer.getObserver().onNext(makeFilled(request.getSymbol(), offer.getTotalQty(), offer.getQty(),
					invertSide(request), offer.getOrderId(), offer.getPrice(), offer.getClOrdID()));
		}
	}

	private Side invertSide(NewOrderSingle request) {
		return request.getSide().equals(Side.BUY) ? Side.SELL : Side.BUY;
	}

	private ExecutionReport makePartialFill(String symbol, long qty, long execQty, Side side, int id, double price,
			long clOrderId) {
		return ExecutionReport.newBuilder().setClOrdID(clOrderId).setExecID(execId.incrementAndGet())
				.setExecPrice(price).setExecQty(execQty).setExecType(ExecType.ET_PARTIAL_FILL).setOrderID(id)
				.setOrdStatus(OrdStatus.NEW).setQty(qty).setSide(side).setSymbol(symbol).setTransactTime(getNow())
				.build();
	}

	private ExecutionReport makeFilled(String symbol, long qty, long execQty, Side side, int id, double price,
			long clOrderId) {
		return ExecutionReport.newBuilder().setClOrdID(clOrderId).setExecID(execId.incrementAndGet())
				.setExecPrice(price).setExecQty(execQty).setExecType(ExecType.ET_FILL).setOrderID(id)
				.setOrdStatus(OrdStatus.FILLED).setQty(qty).setSide(side).setSymbol(symbol).setTransactTime(getNow())
				.build();
	}

}
