package br.com.exchange.grpc.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.protobuf.Empty;

import br.com.exchange.grpc.service.dto.Offer;
import br.com.exchange.proto.Book;
import br.com.exchange.proto.Book.Builder;
import br.com.exchange.proto.MDOpenSessionRequest;
import br.com.exchange.proto.MarketDataServiceGrpc.MarketDataServiceImplBase;
import br.com.exchange.proto.Side;
import br.com.exchange.proto.SubscribeSymbolRequest;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class MarketDataServiceImpl extends MarketDataServiceImplBase {

	private final Map<String, ServerCallStreamObserver<Book>> sessions = new ConcurrentHashMap<>();
	private final Map<String, List<ServerCallStreamObserver<Book>>> sessionsBySymbol = new ConcurrentHashMap<>();
	private final ArrayBlockingQueue<Book> queue = new ArrayBlockingQueue<>(1000);
	private static final Logger logger = Logger.getLogger(MarketDataServiceImpl.class.getName());

	@Override
	public void openSession(MDOpenSessionRequest request, StreamObserver<Book> responseObserver) {
		ServerCallStreamObserver<Book> session = (ServerCallStreamObserver<Book>) responseObserver;
		sessions.put(request.getGuid(), session);
		session.setCompression("gzip");
		session.setOnCancelHandler(() -> {
			logger.info("OnCancelHandler");
			String keyToRemove = null;
			for (Map.Entry<String, ServerCallStreamObserver<Book>> entry : sessions.entrySet()) {
				String key = entry.getKey();
				ServerCallStreamObserver<Book> val = entry.getValue();
				if (session.equals(val)) {
					keyToRemove = key;
				}
			}
			if (keyToRemove != null) {
				sessions.remove(keyToRemove);
			}
			for (Map.Entry<String, List<ServerCallStreamObserver<Book>>> entry : sessionsBySymbol.entrySet()) {
				entry.getValue().remove(session);
				// Clean a null Value
				while (entry.getValue().remove(null)) {
					// Not to do
				}
			}
		});
		sessions.put(request.getGuid(), session);
	}

	@Override
	public void subscribeSymbol(SubscribeSymbolRequest request, StreamObserver<Empty> responseObserver) {
		if (!sessionsBySymbol.containsKey(request.getSymbol())) {
			sessionsBySymbol.put(request.getSymbol(), new ArrayList<>());
		}
		List<ServerCallStreamObserver<Book>> list = sessionsBySymbol.get(request.getSymbol());
		if (sessions.containsKey(request.getGuid())) {
			list.add(sessions.get(request.getGuid()));
		} else {
			logger.info("Session not Found: " + request.getGuid());
		}

	}

	private void proccessQueue() {
		try {
			Book book = queue.poll(1, TimeUnit.MINUTES);
			if (sessionsBySymbol.containsKey(book.getSymbol())) {
				sessionsBySymbol.get(book.getSymbol()).forEach(session -> {
					if (session != null) {
						session.onNext(book);
					}
				});
			}
		} catch (InterruptedException e) {
			logger.log(Level.SEVERE, "", e);
			Thread.currentThread().interrupt();
		}
	}

	public void sendBook(String symbol, Map<Side, List<Offer>> map) {
		for (Map.Entry<Side, List<Offer>> entry : map.entrySet()) {
			Side key = entry.getKey();
			List<Offer> val = entry.getValue();
			Builder builder = Book.newBuilder();
			List<br.com.exchange.proto.Offer> offers = new ArrayList<>();
			for (int i = 0; i < val.size(); i++) {
				builder.getOffersBuilderList();
				offers.add(br.com.exchange.proto.Offer.newBuilder().setPrice(val.get(i).getPrice())
						.setQty(val.get(i).getQty()).build());
			}
			builder.addAllOffers(offers);
			Book book = builder.setSide(key).setSymbol(symbol).build();
			queue.add(book);
			proccessQueue();
		}
	}
}
