package br.com.homebroker.repos;

import org.springframework.data.repository.PagingAndSortingRepository;

import br.com.homebroker.model.ExecutionReportOrder;


public interface ExecutionReportOrderRepos extends PagingAndSortingRepository<ExecutionReportOrder, Integer> {

}
