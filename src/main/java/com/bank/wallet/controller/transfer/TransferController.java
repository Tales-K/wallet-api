package com.bank.wallet.controller.transfer;

import com.bank.wallet.dto.transfer.TransferRequestDto;
import com.bank.wallet.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/transfers", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
public class TransferController implements TransferApi {

	private final TransferService transferService;

	@Override
	@PostMapping
	public ResponseEntity<String> createTransfer(@Valid @RequestBody TransferRequestDto request, @RequestHeader("Idempotency-Key") String idempotencyKey) {
		return transferService.create(request, idempotencyKey);
	}
}
