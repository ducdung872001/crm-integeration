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

    //1. Trường thông tin tĩnh
    private String sourceName; //Nguồn/kênh
    private String code; //Customer id (Mã khách hàng)
    private String phone; //Số ĐT khách hàng
    private String name; //Tên khách hàng

    //2. Trường thông tin động
    private List<CustomerExtraInfo> customerExtraInfos;

    //3. Trường thông tin để xác thực và chỉ dẫn hành động
    private String clientId;
    private String hashedCode;
    private String actionWhenDuplicated; //override, merge, ignore, manual (Tự có cơ chế resolve bằng thủ công)
}

