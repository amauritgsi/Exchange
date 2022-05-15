package br.com.homebroker.model;

import java.security.Timestamp;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import br.com.homebroker.model.builder.ExecutionReportOrderBuilder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class ExecutionReportOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private Long clOrderId;
	private String symbol;
	private int side;
	private long orderID;
	private long execID;
	private long qty;
	private int ordStatus;
	private long execQty;
	private double execPrice;
	private int execType;
	private Timestamp transactTime;
	@ManyToOne()
	private SingleOrder singleOrder;

	public static ExecutionReportOrderBuilder newExecutionReportOrderBuilder() {
		return new ExecutionReportOrderBuilder();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExecutionReportOrder other = (ExecutionReportOrder) obj;
		return Objects.equals(id, other.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

}
