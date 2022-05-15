package br.com.exchange.grpc.service.dto;

import br.com.exchange.proto.ExecutionReport;
import io.grpc.stub.ServerCallStreamObserver;

public class Offer {

	private final double price;
	private final long totalQty;
	private long qty;
	private final ServerCallStreamObserver<ExecutionReport> observer;
	private final int orderId;
	private final long clOrdID;

	public Offer(double price, long totalQty, long qty, ServerCallStreamObserver<ExecutionReport> observer, int orderId,
			long clOrdID) {
		super();
		this.price = price;
		this.qty = qty;
		this.observer = observer;
		this.orderId = orderId;
		this.totalQty = totalQty;
		this.clOrdID = clOrdID;
	}

	public long getQty() {
		return qty;
	}

	public double getPrice() {
		return price;
	}

	public ServerCallStreamObserver<ExecutionReport> getObserver() {
		return observer;
	}

	public void setQty(long qty) {
		this.qty = qty;
	}

	public int getOrderId() {
		return orderId;
	}

	public long getTotalQty() {
		return totalQty;
	}

	public long getClOrdID() {
		return clOrdID;
	}
}
