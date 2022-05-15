package br.com.homebroker.dto;

import br.com.exchange.proto.ExecutionReport;
import br.com.exchange.proto.Identification;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExecutionReportWrhapper {

	private final Identification identification;
	private final ExecutionReport executionReport;

}
