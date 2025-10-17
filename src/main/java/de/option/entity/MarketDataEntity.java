package de.option.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "market_data")
public class MarketDataEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "tickerId", nullable = true)
	private Integer tickerId;

	@Column(name = "symbol", nullable = false)
	private String symbol;
	
	@Column(name = "tickType", nullable = false)
	private String tickType;

	@Column(name = "last", nullable = true)
	private BigDecimal last;

	@Column(name = "geldkurs", nullable = true)
	private BigDecimal geldkurs;

	@Column(name = "briefkurs", nullable = true)
	private BigDecimal briefkurs;

	@Column(name = "timestamp", nullable = false)
	private LocalDateTime timestamp;

	public MarketDataEntity() {
	}

	public MarketDataEntity(int tickerId, String symbol, String tickType, BigDecimal last, BigDecimal geldkurs, BigDecimal briefkurs,
			LocalDateTime timestamp) {
		this.tickerId = tickerId;
		this.symbol = symbol;
		this.last = last;
		this.geldkurs = geldkurs;
		this.briefkurs = briefkurs;
		this.timestamp = timestamp;
		this.tickType = tickType;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getTickerId() {
		return tickerId;
	}

	public void setTickerId(int tickerId) {
		this.tickerId = tickerId;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public BigDecimal getLast() {
		return last;
	}

	public void setLast(BigDecimal last) {
		this.last = last;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public BigDecimal getGeldkurs() {
		return geldkurs;
	}

	public void setGeldkurs(BigDecimal geldkurs) {
		this.geldkurs = geldkurs;
	}

	public BigDecimal getBriefkurs() {
		return briefkurs;
	}

	public void setBriefkurs(BigDecimal briefkurs) {
		this.briefkurs = briefkurs;
	}

	public void setTickerId(Integer tickerId) {
		this.tickerId = tickerId;
	}

	public String getTickType() {
		return tickType;
	}

	public void setTickType(String tickType) {
		this.tickType = tickType;
	}


}