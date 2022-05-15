package br.com.homebroker.dequeuer;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.exchange.proto.ExecutionReport;
import br.com.homebroker.dto.ExecutionReportWrhapper;
import br.com.homebroker.service.ExecutionReportOrderService;
import br.com.homebroker.util.Util;

@Component
public class ExecutionReportDequeuer {
	private boolean proccessingQueueExecutionReport;
	private final ExecutionReportOrderService executionReportOrderService;

	private ArrayBlockingQueue<ExecutionReportWrhapper> executionReportQueue;
	private Map<String, Set<WebSocketSession>> sessionsWithTargetCompIdAndSenderCompId;

	private static final Logger logger = Logger.getLogger(ExecutionReportDequeuer.class.getName());

	public ExecutionReportDequeuer(ExecutionReportOrderService executionReportOrderService) {
		this.executionReportOrderService = executionReportOrderService;
	}

	@Async("proccessExecutionReport")
	public void dequeueEsecutionReport(ArrayBlockingQueue<ExecutionReportWrhapper> executionReportQueue) {
		this.executionReportQueue = executionReportQueue;
		dequeueEsecutionReport();
	}

	public void dequeueEsecutionReport(Map<String, Set<WebSocketSession>> sessionsWithTargetCompIdAndSenderCompId) {
		this.sessionsWithTargetCompIdAndSenderCompId = sessionsWithTargetCompIdAndSenderCompId;		
	}

	private void dequeueEsecutionReport() {
		if (proccessingQueueExecutionReport) {
			return;
		}
		proccessingQueueExecutionReport = true;

		while (proccessingQueueExecutionReport) {
			if (null == executionReportQueue) {
				proccessingQueueExecutionReport = false;
				break;
			}

			ExecutionReportWrhapper executionReportWrhapper;
			try {
				executionReportWrhapper = executionReportQueue.poll(1, TimeUnit.DAYS);
				save(executionReportWrhapper);
				sendMessage(executionReportWrhapper);
			} catch (InterruptedException e1) {
				proccessingQueueExecutionReport = false;
				Thread.currentThread().interrupt();
			}
		}
	}

	private void save(ExecutionReportWrhapper executionReportWrhapper) {
		executionReportOrderService.save(executionReportWrhapper.getExecutionReport());
	}

	private void sendMessage(ExecutionReportWrhapper executionReportWrhapper) {
		if (null != sessionsWithTargetCompIdAndSenderCompId && sessionsWithTargetCompIdAndSenderCompId
				.containsKey(Util.getKey(executionReportWrhapper.getIdentification()))) {
			try {
				String message = new ObjectMapper()
						.writeValueAsString(executionReportToMap(executionReportWrhapper.getExecutionReport()));
				for (WebSocketSession s : sessionsWithTargetCompIdAndSenderCompId
						.get(Util.getKey(executionReportWrhapper.getIdentification()))) {
					synchronized (s) {
						s.sendMessage(new TextMessage(message));
					}
				}
			} catch (IOException e) {
				logger.severe(e.getMessage());
			}
		}
	}

	private Map<String, Object> executionReportToMap(ExecutionReport executionReport) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("clOrdID", executionReport.getClOrdID());
		map.put("execID", executionReport.getExecID());
		map.put("execPrice", executionReport.getExecPrice());
		map.put("execQty", executionReport.getExecQty());
		map.put("execType", executionReport.getExecType());
		map.put("orderID", executionReport.getOrderID());
		map.put("ordStatus", executionReport.getOrdStatus());
		map.put("qty", executionReport.getQty());
		map.put("side", executionReport.getSide());
		map.put("symbol", executionReport.getSymbol());
		map.put("transactTime", Instant.ofEpochSecond(executionReport.getTransactTime().getSeconds(),
				executionReport.getTransactTime().getNanos()).toEpochMilli());

		return map;
	}

}
