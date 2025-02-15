package com.tntexfinance.crm.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.tntexfinance.crm.integration.reborn.RBCustomer;
import org.json.JSONObject;
import org.modelmapper.ModelMapper;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;


import static java.util.Arrays.asList;

public class CustomerService {
    private static final String API_URL = "https://cloud.reborn.vn/adminapi/customer/update/partner";
//    private static final String API_URL = "http://localhost:9100/adminapi/customer/update/partner";

    //Cặp khóa và giá trị này đã được tạo ra trong phần Cài đặt >> Cài đặt ứng dụng
    //Môi trường của TNEX
    //CLIENT_ID: cfeccbajec, CLIENT_KEY: eaggcjkjeurpfanaklas
    private static final String CLIENT_ID = "cfeccbajec";
    private static final String CLIENT_KEY = "eaggcjkjeurpfanaklas";

    //Môi trường của greenspa => (Để test)
//    private static final String CLIENT_ID = "ggfbijecca";
//    private static final String CLIENT_KEY = "pqgjdiudikarcdfaucgq";

    private static final Gson gson = new Gson();

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
        ModelMapper modelMapper = new ModelMapper();

        // Cấu hình mapping trong trường hợp tên 2 trường khác nhau (X: source, Y: target)
//        modelMapper.addMappings(new PropertyMap<Customer, RBCustomer>() {
//            @Override
//            protected void configure() {
//                map().setExtraInfos(source.getCustomerExtraInfos());
//            }
//        });

        RBCustomer rbCustomer = modelMapper.map(customer, RBCustomer.class);
        rbCustomer.setExtraInfo(gson.toJson(customer.getCustomerExtraInfos()));

        String hashedCode = generateHashedCode(rbCustomer);
        System.out.println("hashedCode =>" + hashedCode);
        rbCustomer.setHashedCode(hashedCode);

        Gson gson = new Gson();
        String jsonInputString = gson.toJson(rbCustomer);

        URL url = new URL(API_URL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        String curl = buildCurlCommand(con, jsonInputString);
        System.out.println(curl);

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

    private static String generateHashedCode(RBCustomer customer) {
        customer.setClientId(CLIENT_ID);
        JSONObject jsonSource = new JSONObject(gson.toJson(customer));
        String encodedUrl = CommonUtils.getEncodedStr(jsonSource);
        System.out.println(encodedUrl);

        //Thực hiện băm tham số này rồi so sánh
        return CommonUtils.hashMD5(encodedUrl, CLIENT_KEY);
    }

    /**
     * Giả lập dữ liệu khách hàng của TNEX
     */
    private static Customer initCustomer() {
        // Tạo dữ liệu mẫu cho CustomerExtraInfo
        CustomerExtraInfo trangThaiOnboard = CustomerExtraInfo.builder()
                .fieldName("TrangthaiOnboard")
                .attributeValue("Đã Onboard thành công")
                .build();

        CustomerExtraInfo ngayOnboard = CustomerExtraInfo.builder()
                .fieldName("Ngayonboard")
                .attributeValue("2024-02-02")
                .build();

        CustomerExtraInfo trangThaiKhoanVayCashLoan = CustomerExtraInfo.builder()
                .fieldName("Trangthaikhoanvaycashloan")
                .attributeValue("Chờ thẩm định phê duyệt (APPRISAL/APPROVAL)")
                .build();

        CustomerExtraInfo trangThaiKhoanVayCreditLine = CustomerExtraInfo.builder()
                .fieldName("Trangthaikhoanvaycreditline")
                .attributeValue("Đã khởi tạo đơn vay (Signed)")
                .build();

        CustomerExtraInfo maDangKyVay = CustomerExtraInfo.builder()
                .fieldName("MaDangKyVay")
                .attributeValue("123456")
                .build();

        CustomerExtraInfo sanPham = CustomerExtraInfo.builder()
                .fieldName("SanPham")
                .attributeValue(gson.toJson(asList("Cash loan", "Credit line"))) //Các tùy chọn lấy tại "Định nghĩa trường thông tin bổ sung"
                .build();

        CustomerExtraInfo ngayPheDuyet = CustomerExtraInfo.builder()
                .fieldName("ngaypheduyet")
                .attributeValue("2024-02-05")
                .build();

        CustomerExtraInfo soTienPheDuyet = CustomerExtraInfo.builder()
                .fieldName("sotienpheduyet")
                .attributeValue("5000000")
                .build();

        // Tạo dữ liệu mẫu cho Customer
//        Customer customer = Customer.builder()
//                .sourceName("Dagoras")
//                .code("0562b8fd-f43e-4634-a686-799b68")
//                .phone("0123456789")
//                .name("Phạm Tấn Trường")
//                .clientId(CLIENT_ID)
//                .actionWhenDuplicated("override")
//                .customerExtraInfos(asList(trangThaiOnboard, ngayOnboard, trangThaiKhoanVayCashLoan,
//                        trangThaiKhoanVayCreditLine, maDangKyVay, sanPham, ngayPheDuyet, soTienPheDuyet))
//                .build();

        Customer customer = new Customer();

        //Chuyển đổi chuỗi sang khách hàng
        String strCustomer = "{\"id\":0,\"name\":\"Nguyễn Phan Trọng Trung\",\"phone\":\"0389784236\",\"extraInfo\":\"[{\\\"attributeValue\\\":\\\"[\\\\\\\"Cash loan\\\\\\\"]\\\",\\\"fieldName\\\":\\\"SanPham\\\"},{\\\"attributeValue\\\":\\\"Chưa tạo tài khoản\\\",\\\"fieldName\\\":\\\"TrangthaiOnboard\\\"}]\",\"extraInfos\":[{\"attributeValue\":\"[\\\"Cash loan\\\"]\",\"fieldName\":\"SanPham\"},{\"attributeValue\":\"Chưa tạo tài khoản\",\"fieldName\":\"TrangthaiOnboard\"}],\"doMigrate\":1,\"clientId\":\"cfeccbajec\",\"sourceName\":\"dagoras\",\"actionWhenDuplicated\":\"ignore\",\"hashedCode\":\"0b5dc8ab43cc9f36fa2a867353d656dc\",\"keyValue\":{}}";
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            // Chuyển đổi JSON thành đối tượng Customer
            customer = objectMapper.readValue(strCustomer, Customer.class);

            // In thông tin ra console
            System.out.println("Customer Name: " + customer.getName());
            System.out.println("Customer Phone: " + customer.getPhone());
            System.out.println("Extra Infos: " + customer.getCustomerExtraInfos());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return customer;
    }

    private static String buildCurlCommand(HttpURLConnection httpClient, String jsonInputString) {
        StringBuilder curlCmd = new StringBuilder("curl -X ");
        curlCmd.append(httpClient.getRequestMethod()).append(" ");

        // Thêm tiêu đề
        for (String header : httpClient.getRequestProperties().keySet()) {
            for (String value : httpClient.getRequestProperties().get(header)) {
                curlCmd.append("-H \"").append(header).append(": ").append(value).append("\" ");
            }
        }

        // Thêm dữ liệu JSON
        curlCmd.append("-d '").append(jsonInputString).append("' ");

        // Thêm URL
        curlCmd.append(httpClient.getURL());

        return curlCmd.toString();
    }
}
