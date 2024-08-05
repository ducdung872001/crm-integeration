package com.tntexfinance.crm.integration;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Builder
public class CustomerExtraInfo {
    private Integer id;
    private Integer attributeId;
    private Integer customerId;
    private String attributeValue;
    private String datatype;
    private String fieldName;
}