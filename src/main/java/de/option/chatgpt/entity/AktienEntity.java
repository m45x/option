package de.option.chatgpt.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "aktien")
public class AktienEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;
    private String exchange;
    private String currency;
    private LocalDateTime kaufdatum;
    private BigDecimal kaufpreis;
    private LocalDateTime verkaufdatum;
    private BigDecimal verkkaufpreis;
    private BigDecimal anzahlAktien;
  
    public AktienEntity() {}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public BigDecimal getKaufpreis() {
		return kaufpreis;
	}

	public void setKaufpreis(BigDecimal kaufpreis) {
		this.kaufpreis = kaufpreis;
	}

	public BigDecimal getVerkkaufpreis() {
		return verkkaufpreis;
	}

	public void setVerkkaufpreis(BigDecimal verkkaufpreis) {
		this.verkkaufpreis = verkkaufpreis;
	}

	public LocalDateTime getKaufdatum() {
		return kaufdatum;
	}

	public void setKaufdatum(LocalDateTime kaufdatum) {
		this.kaufdatum = kaufdatum;
	}

	public LocalDateTime getVerkaufdatum() {
		return verkaufdatum;
	}

	public void setVerkaufdatum(LocalDateTime verkaufdatum) {
		this.verkaufdatum = verkaufdatum;
	}

	public BigDecimal getAnzahlAktien() {
		return anzahlAktien;
	}

	public void setAnzahlAktien(BigDecimal anzahlAktien) {
		this.anzahlAktien = anzahlAktien;
	}

	
}