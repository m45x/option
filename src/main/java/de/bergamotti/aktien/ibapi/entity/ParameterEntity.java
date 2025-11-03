package de.bergamotti.aktien.ibapi.entity;

import java.math.BigDecimal;

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
@Table(name = "parameter")
public class ParameterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String parameterName;
    private String stringWert;
    private Boolean booleanWert;
    private BigDecimal bigdecimalWert;
  
    public ParameterEntity() {}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getParameterName() {
		return parameterName;
	}

	public void setParameterName(String parameterName) {
		this.parameterName = parameterName;
	}

	public String getStringWert() {
		return stringWert;
	}

	public void setStringWert(String stringWert) {
		this.stringWert = stringWert;
	}

	public Boolean getBooleanWert() {
		return booleanWert;
	}

	public void setBooleanWert(Boolean booleanWert) {
		this.booleanWert = booleanWert;
	}

	public BigDecimal getBigdecimalWert() {
		return bigdecimalWert;
	}

	public void setBigdecimalWert(BigDecimal bigdecimalWert) {
		this.bigdecimalWert = bigdecimalWert;
	}

}