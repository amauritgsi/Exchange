package br.com.homebroker.service;

import org.springframework.stereotype.Service;

import br.com.exchange.proto.ExecutionReport;
import br.com.homebroker.model.ExecutionReportOrder;
import br.com.homebroker.repos.ExecutionReportOrderRepos;
import br.com.homebroker.repos.OrderEntryRepos;

@Service
public class ExecutionReportOrderService {

	private final ExecutionReportOrderRepos executionReportOrderRepos;
	private final OrderEntryRepos orderEntryRepos;

	public ExecutionReportOrderService(ExecutionReportOrderRepos executionReportOrderRepos,
		OrderEntryRepos orderEntryRepos) {
		super();
		this.executionReportOrderRepos = executionReportOrderRepos;
		this.orderEntryRepos = orderEntryRepos;
	}

	public void save(ExecutionReport executionReport) {
		ExecutionReportOrder executionReportOrder = ExecutionReportOrder.newExecutionReportOrderBuilder()
			.setClOrderId(executionReport.getClOrdID()).setExecID(executionReport.getExecID())
			.setExecPrice(executionReport.getExecPrice()).setExecQty(executionReport.getExecQty())
			.setExecType(executionReport.getExecTypeValue()).setOrderID(executionReport.getOrderID())
			.setOrdStatus(executionReport.getOrdStatusValue()).setQty(executionReport.getQty())
			.setSide(executionReport.getSideValue()).setSymbol(executionReport.getSymbol()).build();
		var order = orderEntryRepos.findById(executionReport.getClOrdID());
		order.ifPresent(executionReportOrder::setSingleOrder);
		executionReportOrderRepos.save(executionReportOrder);
	}

}
