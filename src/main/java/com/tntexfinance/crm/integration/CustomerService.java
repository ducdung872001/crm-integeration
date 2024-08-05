package com.tntexfinance.crm.integration;

import com.google.gson.Gson;
import com.tntexfinance.crm.integration.reborn.RBCustomer;
import org.json.JSONObject;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class CustomerService {
//    private static final String API_URL = "https://cloud.reborn.vn/adminapi/customer/update/partner";
    private static final String API_URL = "http://localhost:9100/adminapi/customer/update/partner";

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
                .fieldName("TrangThaiOnboard")
                .attributeValue("Đã onboard thành công")
                .build();

        CustomerExtraInfo ngayOnboard = CustomerExtraInfo.builder()
                .fieldName("NgayOnboard")
                .attributeValue("2/2/2024")
                .build();

        CustomerExtraInfo trangThaiKhoanVayCashLoan = CustomerExtraInfo.builder()
                .fieldName("TrangThaiKhoanVayCashLoan")
                .attributeValue("Chờ thẩm định (APPRAISAL)")
                .build();

        CustomerExtraInfo trangThaiKhoanVayCreditLine = CustomerExtraInfo.builder()
                .fieldName("TrangThaiKhoanVayCreditLine")
                .attributeValue("Chờ ký hợp đồng (FORSIGN)")
                .build();

        CustomerExtraInfo maDangKyVay = CustomerExtraInfo.builder()
                .fieldName("MaDangKyVay")
                .attributeValue("123456")
                .build();

        CustomerExtraInfo sanPham = CustomerExtraInfo.builder()
                .fieldName("SanPham")
                .attributeValue("Cash Loan")
                .build();

        CustomerExtraInfo ngayPheDuyet = CustomerExtraInfo.builder()
                .fieldName("NgayPheDuyet")
                .attributeValue("20/02/2024")
                .build();

        CustomerExtraInfo soTienPheDuyet = CustomerExtraInfo.builder()
                .fieldName("SoTienPheDuyet")
                .attributeValue("5000000")
                .build();

        // Tạo dữ liệu mẫu cho Customer
        Customer customer = Customer.builder()
                .sourceName("Dagoras")
                .code("0562b8fd-f43e-4634-a686-799b68")
                .phone("0123456789")
                .name("Phạm Tấn Trường")
                .clientId(CLIENT_ID)
                .actionWhenDuplicated("override")
                .customerExtraInfos(Arrays.asList(trangThaiOnboard, ngayOnboard, trangThaiKhoanVayCashLoan,
                        trangThaiKhoanVayCreditLine, maDangKyVay, sanPham, ngayPheDuyet, soTienPheDuyet))
                .build();

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
