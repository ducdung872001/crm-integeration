package com.tntexfinance.crm.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerExtraInfo {
    private String attributeCode;
    private String attributeValue;
    private int customerId;
}