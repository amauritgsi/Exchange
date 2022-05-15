package br.com.homebroker.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.OneToMany;

import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class SingleOrder {

	@javax.persistence.Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private BigDecimal price;
	private Integer qty;
	private String symbol;
	private int side;
	private String targetCompId;
	private String senderCompId;
	@OneToMany(mappedBy = "singleOrder")
	private Set<ExecutionReportOrder> executionReportOrders;

	public SingleOrder(BigDecimal price, Integer qty, String symbol, int side, String targetCompId,
			String senderCompId) {
		super();
		this.price = price;
		this.qty = qty;
		this.symbol = symbol;
		this.side = side;
		this.targetCompId = targetCompId;
		this.senderCompId = senderCompId;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SingleOrder other = (SingleOrder) obj;
		return Objects.equals(id, other.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

}
