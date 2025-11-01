package de.option.entity;

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
public class AktieEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;
    private String exchange;
    private String currency;
    private Integer kaufOrderId;
    private LocalDateTime kaufdatum;
    private BigDecimal kauflimit;
    private BigDecimal kaufpreis;
    private BigDecimal stoplosspreis;
    private Integer verkaufOrderId;
    private LocalDateTime verkaufdatum;
    private BigDecimal verkkaufpreis;
    private BigDecimal anzahlAktien;
    private BigDecimal gewinnOderVerlust;
    private BigDecimal gebuehren;
  
    public AktieEntity() {}

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

	public BigDecimal getGewinnOderVerlust() {
		return gewinnOderVerlust;
	}

	public void setGewinnOderVerlust(BigDecimal gewinnOderVerlust) {
		this.gewinnOderVerlust = gewinnOderVerlust;
	}

	public Integer getVerkaufOrderId() {
		return verkaufOrderId;
	}

	public void setVerkaufOrderId(Integer verkaufOrderId) {
		this.verkaufOrderId = verkaufOrderId;
	}

	public BigDecimal getStoplosspreis() {
		return stoplosspreis;
	}

	public void setStoplosspreis(BigDecimal stoplosspreis) {
		this.stoplosspreis = stoplosspreis;
	}

	public Integer getKaufOrderId() {
		return kaufOrderId;
	}

	public void setKaufOrderId(Integer kaufOrderId) {
		this.kaufOrderId = kaufOrderId;
	}

	public BigDecimal getKauflimit() {
		return kauflimit;
	}

	public void setKauflimit(BigDecimal kauflimit) {
		this.kauflimit = kauflimit;
	}

	public BigDecimal getGebuehren() {
		return gebuehren;
	}

	public void setGebuehren(BigDecimal gebuehren) {
		this.gebuehren = gebuehren;
	}
	
}