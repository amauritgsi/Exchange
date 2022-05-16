package br.com.homebroker.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.google.protobuf.Empty;

import br.com.exchange.proto.EntryPointServiceGrpc;
import br.com.exchange.proto.ExecutionReport;
import br.com.exchange.proto.Identification;
import br.com.exchange.proto.NewOrderSingle;
import br.com.exchange.proto.OrdStatus;
import br.com.exchange.proto.OrdType;
import br.com.exchange.proto.Side;
import br.com.homebroker.dequeuer.ExecutionReportDequeuer;
import br.com.homebroker.dto.ExecutionReportWrhapper;
import br.com.homebroker.dto.NewOrderSingleRequest;
import br.com.homebroker.dto.SingleOrderDTO;
import br.com.homebroker.model.ExecutionReportOrder;
import br.com.homebroker.model.SingleOrder;
import br.com.homebroker.repos.OrderEntryRepos;
import br.com.homebroker.util.EmptyClientCallStreamObserver;
import br.com.homebroker.util.Util;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import net.devh.boot.grpc.client.inject.GrpcClient;

@Service
public class OrderEntryService {
	private static final Logger logger = Logger.getLogger(OrderEntryService.class.getName());

	private final OrderEntryRepos orderEntryRepos;
	@GrpcClient("exchange")
	private EntryPointServiceGrpc.EntryPointServiceStub entryPointService;
	private final Map<String, ClientResponseObserver<Identification, ExecutionReport>> sessions = new ConcurrentHashMap<>();
	@Getter
	private final ArrayBlockingQueue<ExecutionReportWrhapper> executionReportQueue = new ArrayBlockingQueue<>(1000);
	private final ExecutionReportDequeuer executionReportDequeuer;

	public OrderEntryService(OrderEntryRepos orderEntryRepos, ExecutionReportDequeuer executionReportDequeuer) {
		super();
		this.orderEntryRepos = orderEntryRepos;
		this.executionReportDequeuer = executionReportDequeuer;
	}

	public void newOrderSingle(String targetCompId, String senderCompId, NewOrderSingleRequest nos) {
		SingleOrder order = new SingleOrder(nos.getPrice(), nos.getQty(), nos.getSymbol(), nos.getSide().getNumber(),
				targetCompId, senderCompId);
		orderEntryRepos.save(order);
		sendOrder(targetCompId, senderCompId, nos, order.getId());
	}

	public Page<SingleOrderDTO> findAll(Pageable pageable) {
		Page<SingleOrder> page = orderEntryRepos.findAllWithExecutionReport(pageable);
		List<SingleOrderDTO> singleOrderDTOs = convertToDTO(page);
		return new PageImpl<>(singleOrderDTOs, pageable, page.getTotalElements());
	}

	private List<SingleOrderDTO> convertToDTO(Page<SingleOrder> page) {
		List<SingleOrderDTO> singleOrderDTOs = new ArrayList<>();
		for (SingleOrder singleOrder : page.getContent()) {
			SingleOrderDTO singleOrderDTO = new SingleOrderDTO();
			singleOrderDTO.setId(singleOrder.getId());
			singleOrderDTO.setPrice(singleOrder.getPrice());
			singleOrderDTO.setQty(singleOrder.getQty());
			singleOrderDTO.setSymbol(singleOrder.getSymbol());
			singleOrderDTO.setSide(Side.forNumber(singleOrder.getSide()));

			BigDecimal totalPrice = BigDecimal.ZERO;
			List<ExecutionReportOrder> list = new ArrayList<>(singleOrder.getExecutionReportOrders());
			list.sort((ExecutionReportOrder o1, ExecutionReportOrder o2) -> {
				if (o1.getExecID() == o2.getExecID()) {
					return 0;
				}
				return o1.getExecID() > o2.getExecID() ? 1 : -1;
			});
			for (ExecutionReportOrder executionReportOrder : list) {
				singleOrderDTO.setExecQty(singleOrderDTO.getExecQty() + executionReportOrder.getExecQty());
				singleOrderDTO.setOrdStatus(OrdStatus.forNumber(executionReportOrder.getOrdStatus()));
				singleOrderDTO.setLastPx(new BigDecimal(String.valueOf(executionReportOrder.getExecPrice())));
				totalPrice = totalPrice.add(new BigDecimal(String.valueOf(executionReportOrder.getExecPrice()))
						.multiply(new BigDecimal(executionReportOrder.getExecQty())));
			}

			if (!totalPrice.equals(BigDecimal.ZERO) && singleOrderDTO.getExecQty() > 0) {
				singleOrderDTO.setAvgPx(totalPrice.divide(new BigDecimal(singleOrderDTO.getExecQty())));
			} else {
				singleOrderDTO.setAvgPx(BigDecimal.ZERO);
			}
			singleOrderDTOs.add(singleOrderDTO);
		}
		return singleOrderDTOs;
	}

	private void sendOrder(String targetCompId, String senderCompId, NewOrderSingleRequest nos, Long id) {
		StreamObserver<Empty> emptyObserver = new EmptyClientCallStreamObserver();
		Identification identification = makeIdentification(targetCompId, senderCompId);

		entryPointService.submitOrder(NewOrderSingle.newBuilder().setIdentification(identification).setClOrdID(id)
				.setOrderQty(nos.getQty()).setOrdType(OrdType.LIMIT).setPrice(nos.getPrice().doubleValue())
				.setSide(nos.getSide()).setSymbol(nos.getSymbol()).build(), emptyObserver);

	}

	private Identification makeIdentification(String targetCompId, String senderCompId) {
		return Identification.newBuilder().setTargetCompId(targetCompId).setSenderCompId(senderCompId).build();
	}

	public void connect(String targetCompId, String senderCompId) {
		Identification identification = makeIdentification(targetCompId, senderCompId);
		ClientResponseObserver<Identification, ExecutionReport> observer = new ClientResponseObserver<Identification, ExecutionReport>() {

			@Override
			public void onNext(ExecutionReport value) {
				executionReportQueue.add(new ExecutionReportWrhapper(identification, value));
			}

			@Override
			public void onError(Throwable t) {
				logger.severe(t.getMessage());
			}

			@Override
			public void onCompleted() {
				logger.info("All Done");
			}

			@Override
			public void beforeStart(ClientCallStreamObserver<Identification> requestStream) {
				// nothing to do
			}
		};
		entryPointService.openSession(identification, observer);
		sessions.put(Util.getKey(identification), observer);
		executionReportDequeuer.dequeueEsecutionReport(executionReportQueue);

	}

}
