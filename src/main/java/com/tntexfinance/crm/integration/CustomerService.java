package com.tntexfinance.crm.integration;

import com.google.gson.Gson;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.util.stream.Collectors.joining;

public class CustomerService {
    private static final String API_URL = "https://cloud.reborn.vn/adminapi/customer/update/partner";

    //Cặp khóa và giá trị này đã được tạo ra trong phần Cài đặt >> Cài đặt ứng dụng
    //CLIENT_ID: cfeccbajec, CLIENT_KEY: eaggcjkjeurpfanaklas
    private static final String CLIENT_ID = "cfeccbajec";
    private static final String CLIENT_KEY = "eaggcjkjeurpfanaklas";

    public static void main(String[] args) {
        Customer customer = initCustomer();

        try {
            //Thực hiện đồng bộ sang TNTech CRM
            syncCustomer(customer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void syncCustomer(Customer customer) throws Exception {
        String hashedCode = generateHashedCode(customer);
        System.out.println("hashedCode =>" + hashedCode);

        customer.setHashedCode(hashedCode);

        Gson gson = new Gson();
        String jsonInputString = gson.toJson(customer);

        URL url = new URL(API_URL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = con.getResponseCode();
        System.out.println("POST Response Code :: " + responseCode);

        if (responseCode == HttpURLConnection.HTTP_OK) {
            System.out.println("Customer updated successfully.");
        } else {
            System.out.println("Failed to update customer.");
        }
    }

    private static String generateHashedCode(Customer customer) {
        SortedMap<String, String> map = new TreeMap<>();
        map.put("name", CommonUtils.NVL(customer.getName()));
        map.put("phone", CommonUtils.NVL(customer.getPhone()));
        map.put("email", CommonUtils.NVL(customer.getEmail()));
        map.put("gender", CommonUtils.NVL(customer.getGender()).toString());
        map.put("birthday", customer.getBirthday() == null ? "" : customer.getBirthday());
        map.put("code", CommonUtils.NVL(customer.getCode()));
        map.put("address", CommonUtils.NVL(customer.getAddress()));
        map.put("campaignCode", CommonUtils.NVL(customer.getCampaignCode()));
        map.put("clientId", CLIENT_ID); //Được cấp từ ứng dụng
        map.put("sourceName", CommonUtils.NVL(customer.getSourceName()));
        map.put("actionWhenDuplicated", CommonUtils.NVL(customer.getActionWhenDuplicated()));

        String encodedURL = map.keySet().stream()
                .map(key -> key + "=" + CommonUtils.encodeValue(map.get(key)))
                .collect(joining("&", "", ""));

        System.out.println(encodedURL);

        //Thực hiện băm tham số này rồi so sánh
        return CommonUtils.hashMD5(encodedURL, CLIENT_KEY);
    }

    /**
     * Giả lập dữ liệu khách hàng của TNEX
     */
    private static Customer initCustomer() {
        // Tạo dữ liệu mẫu cho CustomerExtraInfo
        CustomerExtraInfo extraInfo1 = CustomerExtraInfo.builder()
//                .attributeId(1)
                .attributeValue("20/10/2023")
                .build();

        CustomerExtraInfo extraInfo2 = CustomerExtraInfo.builder()
//                .attributeId(2)
                .attributeValue("Thẻ tín dụng")
//                .customerId(1001)
                .build();

        // Tạo dữ liệu mẫu cho Customer
        Customer customer = Customer.builder()
                .name("John Doe")
                .phone("0123456789")
                .code("CUST1001")
                .recommenderPhone("0987654321")
                .email("john.doe@example.com")
                .address("123 Main St, Anytown, USA")
                .birthday("1980-01-01")
                .gender(1)
                .sourceName("Website")
                .careerName("Engineer")
                .groupName("VIP")
                .avatar("http://example.com/avatars/johndoe.png")
                .firstCall("2024-01-01 10:00:00")
                .weight(75)
                .height(180)
                .timestamp(System.currentTimeMillis())
                .clientId(CLIENT_ID)
                .campaignCode("CMP1001")
                .actionWhenDuplicated("override")
                .customerExtraInfos(Arrays.asList(extraInfo1, extraInfo2))
                .build();

        return customer;
    }
}
