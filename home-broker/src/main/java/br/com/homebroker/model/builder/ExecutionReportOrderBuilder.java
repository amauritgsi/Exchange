package br.com.homebroker.model.builder;

import br.com.homebroker.model.ExecutionReportOrder;

public class ExecutionReportOrderBuilder {

	private final ExecutionReportOrder executionReportOrder;

	public ExecutionReportOrderBuilder() {
		executionReportOrder = new ExecutionReportOrder();
	}

	public ExecutionReportOrderBuilder setClOrderId(long clOrderId) {
		executionReportOrder.setClOrderId(clOrderId);
		return this;
	}

	public ExecutionReportOrderBuilder setSymbol(String symbol) {
		executionReportOrder.setSymbol(symbol);
		return this;
	}

	public ExecutionReportOrderBuilder setOrderID(long orderID) {
		executionReportOrder.setOrderID(orderID);
		return this;
	}

	public ExecutionReportOrderBuilder setExecID(long execID) {
		executionReportOrder.setExecID(execID);
		return this;
	}

	public ExecutionReportOrderBuilder setOrdStatus(int ordStatus) {
		executionReportOrder.setOrdStatus(ordStatus);
		return this;
	}

	public ExecutionReportOrderBuilder setSide(int side) {
		executionReportOrder.setSide(side);
		return this;
	}

	public ExecutionReportOrderBuilder setExecQty(long execQty) {
		executionReportOrder.setExecQty(execQty);
		return this;
	}

	public ExecutionReportOrderBuilder setExecPrice(double execPrice) {
		executionReportOrder.setExecPrice(execPrice);
		return this;
	}

	public ExecutionReportOrderBuilder setExecType(int execType) {
		executionReportOrder.setExecType(execType);
		return this;
	}

	public ExecutionReportOrderBuilder setQty(long qty) {
		executionReportOrder.setQty(qty);
		return this;
	}

	public ExecutionReportOrder build() {
		return executionReportOrder;
	}

}
