package de.option.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
@Entity
@Table(name = "contract")
public class ContractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Boolean aktiv;
    
    @Column(unique = true)
    private Integer tickerId;
    
    private String symbol;
    private String exchange;
    private String currency;
    private String secType;

    public ContractEntity() {}

    public ContractEntity(int tickerId, String symbol, String exchange, String currency, String secType) {
    	this.aktiv = true;
        this.tickerId = tickerId;
        this.symbol = symbol;
        this.exchange = exchange;
        this.currency = currency;
        this.secType = secType;
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Boolean getAktiv() {
		return aktiv;
	}

	public void setAktiv(Boolean aktiv) {
		this.aktiv = aktiv;
	}

	public Integer getTickerId() {
		return tickerId;
	}

	public void setTickerId(Integer tickerId) {
		this.tickerId = tickerId;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getExchange() {
		return exchange;
	}

	public void setExchange(String exchange) {
		this.exchange = exchange;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getSecType() {
		return secType;
	}

	public void setSecType(String secType) {
		this.secType = secType;
	}

}