package com.bank.wallet.entity;

import com.bank.wallet.entity.enums.IdempotencyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Table(name = "idempotency_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey {

	@Id
	@Column("idempotency_key")
	private String idempotencyKey;

	@Column("method")
	private String method;

	@Column("path")
	private String path;

	@Column("request_hash")
	private String requestHash;

	@Column("status")
	private IdempotencyStatus status;

	@Column("response_status")
	private Integer responseStatus;

	@Column("response_body")
	private String responseBody;

	@CreatedDate
	@Column("first_seen_at")
	private OffsetDateTime firstSeenAt;

	@LastModifiedDate
	@Column("last_seen_at")
	private OffsetDateTime lastSeenAt;

	@Column("ref_id")
	private java.util.UUID refId;

}
