package br.com.homebroker.dto;

import java.math.BigDecimal;

import br.com.exchange.proto.Side;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewOrderSingleRequest {

	private BigDecimal price;
	private Integer qty;
	private String symbol;
	private Side side;

}
