package br.com.homebroker.dequeuer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.exchange.proto.Book;
import br.com.homebroker.service.MarketDataService;

@Component
public class BookDequeuer {
	private static final Logger logger = Logger.getLogger(BookDequeuer.class.getName());
	
	private boolean proccessingQueueBook;
	private final MarketDataService marketDataService;

	public BookDequeuer(MarketDataService marketDataService) {
		this.marketDataService = marketDataService;
	}

	private Map<String, Object> bookToMap(Book book) {
		Map<String, Object> map = new LinkedHashMap<>();

		List<Map<String, Object>> offers = new ArrayList<>();

		book.getOffersList().forEach(o -> {
			Map<String, Object> offer = new LinkedHashMap<>();
			offer.put("price", o.getPrice());
			offer.put("qty", o.getQty());
			offers.add(offer);
		});

		map.put("side", book.getSide());
		map.put("symbol", book.getSymbol());
		map.put("offers", offers);

		return map;
	}

	@Async("proccessBook")
	public void dequeueBook(Map<String, Set<WebSocketSession>> sessionsWithSymbol) {
		if (proccessingQueueBook) {
			return;
		}
		proccessingQueueBook = true;
		while (proccessingQueueBook) {
			Book book;
			try {
				book = marketDataService.getBlockingQueue().poll(1, TimeUnit.DAYS);
				if (sessionsWithSymbol.containsKey(book.getSymbol())) {
					sendMessage(sessionsWithSymbol, book);
				}
			} catch (InterruptedException e1) {
				proccessingQueueBook = false;
				Thread.currentThread().interrupt();
			}
		}
	}

	private void sendMessage(Map<String, Set<WebSocketSession>> sessionsWithSymbol, Book book) {
		try {
			String message = new ObjectMapper().writeValueAsString(bookToMap(book));
			for (WebSocketSession s : sessionsWithSymbol.get(book.getSymbol())) {
				synchronized(s){
					s.sendMessage(new TextMessage(message));
				}
			}
		} catch (IOException e) {
			logger.severe(e.getMessage());
		}
	}

}