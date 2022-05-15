package br.com.homebroker.repos;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import br.com.homebroker.model.SingleOrder;


public interface OrderEntryRepos extends PagingAndSortingRepository<SingleOrder, Long> {

	@Query(value = "SELECT S FROM SingleOrder AS S JOIN FETCH S.executionReportOrders ER", countQuery = "SELECT COUNT(S) FROM SingleOrder AS S")
	Page<SingleOrder> findAllWithExecutionReport(Pageable pageable);

}
