package br.com.homebroker.websocket;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import br.com.homebroker.dequeuer.BookDequeuer;
import br.com.homebroker.dequeuer.ExecutionReportDequeuer;
import br.com.homebroker.service.MarketDataService;
import br.com.homebroker.util.Util;
import lombok.Getter;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

	private static final String SUBSCRIBE_BOOK = "subscribeBook";
	private static final String ACTION = "action";
	private static final String SENDER_COMP_ID = "senderCompId";
	private static final String TARGET_COMP_ID = "targetCompId";
	private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
	private final Map<String, Set<WebSocketSession>> sessionsWithSymbol = new ConcurrentHashMap<>();
	@Getter
	private final Map<String, Set<WebSocketSession>> sessionsWithTargetCompIdAndSenderCompId = new ConcurrentHashMap<>();
	private final MarketDataService marketDataService;
	private final BookDequeuer bookDequeuer;
	private final ExecutionReportDequeuer executionReportDequeuer;

	private static final Object lock = new Object();

	public WebSocketHandler(MarketDataService marketDataService, BookDequeuer bookDequeuer,
			ExecutionReportDequeuer executionReportDequeuer) {
		this.marketDataService = marketDataService;
		this.bookDequeuer = bookDequeuer;
		this.executionReportDequeuer = executionReportDequeuer;
	}

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message)
			throws InterruptedException, IOException {
		Map<String, Object> value = JsonParserFactory.getJsonParser().parseMap(message.getPayload());
		if (value.get(ACTION).equals(SUBSCRIBE_BOOK)) {
			String symbol = (String) value.get("symbol");
			sessionsWithSymbol.putIfAbsent(symbol, new HashSet<>());
			sessionsWithSymbol.get(symbol).add(session);
			subscribeBookBySymbol(symbol);
			bookDequeuer.dequeueBook(sessionsWithSymbol);
		} else if (value.get(ACTION).equals("subscribeExecutionReport")) {
			String targetCompId = (String) value.get(TARGET_COMP_ID);
			String senderCompId = (String) value.get(SENDER_COMP_ID);
			sessionsWithTargetCompIdAndSenderCompId.putIfAbsent(Util.getKey(targetCompId, senderCompId),
					new HashSet<>());
			sessionsWithTargetCompIdAndSenderCompId.get(Util.getKey(targetCompId, senderCompId)).add(session);
			executionReportDequeuer.dequeueEsecutionReport(sessionsWithTargetCompIdAndSenderCompId);
		}
	}

	private void subscribeBookBySymbol(String symbol) {
		synchronized (lock) {
			marketDataService.connect();
			marketDataService.subscribeSymbol(symbol);
		}
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		sessions.add(session);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		sessions.remove(session);
		for (Map.Entry<String, Set<WebSocketSession>> entry : sessionsWithSymbol.entrySet()) {
			removeSession(session, entry);
		}
		for (Map.Entry<String, Set<WebSocketSession>> entry : sessionsWithTargetCompIdAndSenderCompId.entrySet()) {
			removeSession(session, entry);
		}
	}

	private void removeSession(WebSocketSession session, Map.Entry<String, Set<WebSocketSession>> entry) {
		Set<WebSocketSession> val = entry.getValue();
		if (val.contains(session)) {
			val.remove(session);
		}
	}

}