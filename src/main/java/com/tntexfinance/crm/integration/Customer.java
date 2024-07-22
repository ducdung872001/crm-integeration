package com.tntexfinance.crm.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    private int id;
    private String name;
    private String phone;
    private String code;
    private String recommenderPhone;
    private String email;
    private String address;
    private String birthday;
    private Integer gender; //1 - Nữ, 2 - Nam, 0 - Chưa xác định
    private String sourceName;
    private String careerName;
    private String groupName;
    private String avatar;
    private String firstCall;
    private Integer weight;
    private Integer height;
    private Long timestamp;
    private String clientId;
    private String campaignCode;
    private String hashedCode;
    private String actionWhenDuplicated; //override, merge, ignore, manual (Tự có cơ chế resolve bằng thủ công)
    private List<CustomerExtraInfo> customerExtraInfos;
}

