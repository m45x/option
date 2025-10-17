package de.option.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class AccountDto {
	
	private String account = null;
	private BigDecimal buyingPower = new BigDecimal(0.0);
	private BigDecimal netLiquidation = new BigDecimal(0.0);
	private BigDecimal totalCashValue = new BigDecimal(0.0);
	
	public BigDecimal getBuyingPower() {
		return buyingPower;
	}
	public void setBuyingPower(BigDecimal buyingPower) {
		this.buyingPower = buyingPower;
	}
	public BigDecimal getNetLiquidation() {
		return netLiquidation;
	}
	public void setNetLiquidation(BigDecimal netLiquidation) {
		this.netLiquidation = netLiquidation;
	}
	public BigDecimal getTotalCashValue() {
		return totalCashValue;
	}
	public void setTotalCashValue(BigDecimal totalCashValue) {
		this.totalCashValue = totalCashValue;
	}
	public String getAccount() {
		return account;
	}
	public void setAccount(String account) {
		this.account = account;
	}


}