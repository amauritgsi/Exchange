package br.com.homebroker.service;

import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.google.protobuf.Empty;

import br.com.exchange.proto.Book;
import br.com.exchange.proto.MDOpenSessionRequest;
import br.com.exchange.proto.MarketDataServiceGrpc;
import br.com.exchange.proto.SubscribeSymbolRequest;
import br.com.homebroker.util.EmptyClientCallStreamObserver;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import net.devh.boot.grpc.client.inject.GrpcClient;

@Service
public class MarketDataService {
	private static final Logger logger = Logger.getLogger(MarketDataService.class.getName());

	private ClientResponseObserver<String, Book> observer;
	private String observerUUID;

	@GrpcClient("exchange")
	private MarketDataServiceGrpc.MarketDataServiceStub marketDataServiceStub;

	@Getter
	private final ArrayBlockingQueue<Book> blockingQueue = new ArrayBlockingQueue<>(1000);

	public void connect() {
		if (observer == null) {
			observerUUID = UUID.randomUUID().toString();
			observer = new ClientResponseObserver<String, Book>() {

				@Override
				public void onNext(Book value) {
					blockingQueue.add(value);
				}

				@Override
				public void onError(Throwable t) {
					logger.severe(t.getMessage());
					observer = null;
				}

				@Override
				public void onCompleted() {
					logger.info("All Done");
					observer = null;
				}

				@Override
				public void beforeStart(ClientCallStreamObserver<String> requestStream) {
					// nothing to do
				}
			};
			marketDataServiceStub.openSession(MDOpenSessionRequest.newBuilder().setGuid(observerUUID).build(),
					observer);
		}
	}

	public void subscribeSymbol(String symbol) {
		StreamObserver<Empty> emptyObserver = new EmptyClientCallStreamObserver();
		marketDataServiceStub.subscribeSymbol(
				SubscribeSymbolRequest.newBuilder().setGuid(observerUUID).setSymbol(symbol).build(), emptyObserver);
	}

}
