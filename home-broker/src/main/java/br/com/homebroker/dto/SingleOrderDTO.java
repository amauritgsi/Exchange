package br.com.homebroker.dto;

import java.math.BigDecimal;

import br.com.exchange.proto.OrdStatus;
import br.com.exchange.proto.Side;
import lombok.Data;

@Data
public class SingleOrderDTO {
	private Long id;
	private BigDecimal price;
	private Integer qty;
	private long execQty;
	private String symbol;
	private Side side;
	private OrdStatus ordStatus;
	private BigDecimal lastPx;
	private BigDecimal avgPx;

}
