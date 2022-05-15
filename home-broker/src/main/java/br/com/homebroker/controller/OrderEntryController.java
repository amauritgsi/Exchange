package br.com.homebroker.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.homebroker.dto.NewOrderSingleRequest;
import br.com.homebroker.dto.SingleOrderDTO;
import br.com.homebroker.service.OrderEntryService;

@RestController
@RequestMapping("order/v1")
public class OrderEntryController {

	private final OrderEntryService orderEntryService;

	public OrderEntryController(OrderEntryService orderEntryService) {
		super();
		this.orderEntryService = orderEntryService;
	}

	@PutMapping(path = "nos")
	public ResponseEntity<String> newOrderSingle(String targetCompId, String senderCompId, NewOrderSingleRequest nos) {
		orderEntryService.newOrderSingle(targetCompId, senderCompId, nos);
		return ResponseEntity.ok("");
	}

	@PostMapping(path = "connect")
	public ResponseEntity<String> connect(String targetCompId, String senderCompId) {
		orderEntryService.connect(targetCompId, senderCompId);
		return ResponseEntity.ok("");
	}

	@GetMapping()
	public ResponseEntity<Page<SingleOrderDTO>> findAll(Pageable pageable) {
		return ResponseEntity.ok(orderEntryService.findAll(pageable));
	}
}
