package de.option.dto;

import java.math.BigDecimal;

import com.ib.client.Contract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class Dto {
	private BigDecimal preis = new BigDecimal(0.0);
	private BigDecimal geldKurs = new BigDecimal(0.0);
	private BigDecimal briefKurs = new BigDecimal(0.0);
	private Boolean dataExist = false;
	private Contract contract;

	public Dto(Contract contract) {
		this.contract = contract;
	}

	public BigDecimal getPreis() {
		return preis;
	}

	public void setPreis(BigDecimal preis) {
		this.preis = preis;
	}

	public BigDecimal getGeldKurs() {
		return geldKurs;
	}

	public void setGeldKurs(BigDecimal geldKurs) {
		this.geldKurs = geldKurs;
	}

	public BigDecimal getBriefKurs() {
		return briefKurs;
	}

	public void setBriefKurs(BigDecimal briefKurs) {
		this.briefKurs = briefKurs;
	}

	public Boolean getDataExist() {
		return dataExist;
	}

	public void setDataExist(Boolean dataExist) {
		this.dataExist = dataExist;
	}

	public Contract getContract() {
		return contract;
	}

	public void setContract(Contract contract) {
		this.contract = contract;
	}
}