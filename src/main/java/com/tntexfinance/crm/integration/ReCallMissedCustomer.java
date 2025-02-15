package com.tntexfinance.crm.integration;

import com.google.gson.Gson;
import com.tntexfinance.crm.integration.reborn.RBCustomer;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tntexfinance.crm.integration.CommonUtils.getEncodedStr;


public class ReCallMissedCustomer {
  //    private static final String API_URL = "https://cloud.reborn.vn/adminapi/customer/update/partner";
  private static final String API_URL = "http://localhost:9100/adminapi/customer/update/partner";
  private static final Logger logger = LoggerFactory.getLogger(ReCallMissedCustomer.class);
  private static String FILE_PATH = "/home/hoangdd/Downloads/Field dữ liệu trên CRM TNEX (tháng 1 thiếu).xlsx";
  private static final int PAGE_SIZE = 1;
  private static final String PASSWORD = "171284";
  private final AtomicInteger currentPage = new AtomicInteger(0);
  private boolean processingComplete = false;
  private static final String CLIENT_ID = "cfeccbajec";
  private static final String CLIENT_KEY = "eaggcjkjeurpfanaklas";
  private static final Gson gson = new Gson();

  public static void main(String[] args) {
    try {
      ReCallMissedCustomer processor = new ReCallMissedCustomer();
      processor.processCustomerExcelBatch();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public void processCustomerExcelBatch() {
    if (processingComplete) {
      logger.info("Processing already completed. Skipping this iteration.");
      return;
    }

    try (FileInputStream file = new FileInputStream(FILE_PATH);
         XSSFWorkbook workbook = (XSSFWorkbook) WorkbookFactory.create(file, PASSWORD)) {

      XSSFSheet sheet = workbook.getSheet("DATA MAP");
      Map<String, Integer> fieldNameMap = getFieldNameMap(sheet);
      if (sheet == null) {
        logger.error("Sheet 'DATA MAP' not found");
        processingComplete = true;
        return;
      }

      if (currentPage.get() == 0) {
        logger.info("Starting new processing cycle. Total rows: {}", sheet.getLastRowNum());
      }
      boolean hasMoreData = processBatch(sheet, fieldNameMap);

      if (!hasMoreData) {
        logger.info("Processing completed successfully");
        processingComplete = true;
        currentPage.set(0);
      }

    } catch (IOException e) {
      logger.error("Error processing Excel file: {}", e.getMessage(), e);
      processingComplete = false;
    }
  }

  private boolean processBatch(XSSFSheet sheet, Map<String, Integer> fieldNameMap) {
    int startRow = currentPage.get() * PAGE_SIZE + 2; // +2 to skip header rows
    int endRow = Math.min(startRow + PAGE_SIZE, sheet.getLastRowNum() + 1);

    if (startRow >= sheet.getLastRowNum()) {
      return false; // No more data to process
    }

    try {
      List<String> customerAttributes = Arrays.asList("ThongTinDanhChoTelesale",
        "SanPham",
        "TrangthaiOnboard",
        "MaDangKyVayCashloan",
        "Ngayonboard",
        "ngaypheduyetcashloan",
        "sotienpheduyetcashloan",
        "ThuTu",
        "Trangthaikhoanvaycashloan",
        "Trangthaikhoanvaycreditline",
        "MaDangKyVayCreditline",
        "SoTienPheDuyetCreditline",
        "ThongTinKhoanVayCashLoan",
        "ThongTinKhoanVayCreditline",
        "ngaypheduyetcreditline",
        "LyDoTuChoi",
        "LyDo",
        "ThongTinKhoanVayTBoss",
        "TrangThaiKhoanVayTBoss",
        "SoTienPheDuyetTBoss",
        "MaDangKyVayTBoss",
        "NgayPheDuyetTBoss",
        "TrangThaiLienKetShop",
        "marketingSendLeadTime",
        "marketingSendLeadSource");

      List<RBCustomer> batchCustomers = new ArrayList<>();

      for (int rowIndex = startRow; rowIndex < endRow; rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (row != null) {
          RBCustomer customer = processRow(row, fieldNameMap, customerAttributes);
          if (customer != null) {
            batchCustomers.add(customer);
          }
        }
      }

      processBatchCustomers(batchCustomers);

      logger.info("Processed batch {} ({} records). Rows {}-{}",
        currentPage.get(), batchCustomers.size(), startRow, endRow - 1);

      currentPage.incrementAndGet();
      return true;

    } catch (Exception e) {
      logger.error("Error processing batch starting at row {}: {}", startRow, e.getMessage(), e);
      return false;
    }
  }

  private Map<String, Integer> getFieldNameMap(XSSFSheet sheet) {
    Map<String, Integer> fieldNameMap = new HashMap<>();
    Row firstRow = sheet.getRow(1);
    if (firstRow != null) {
      firstRow.forEach(cell -> {
        if (cell != null && cell.getStringCellValue() != null) {
          fieldNameMap.put(cell.getStringCellValue(), cell.getColumnIndex());
        }
      });
    }
    return fieldNameMap;
  }

  private RBCustomer processRow(Row row, Map<String, Integer> fieldNameMap, List<String> customerAttributes) {
    try {
      RBCustomer customer = new RBCustomer();
      List<Field> fields = Arrays.asList(RBCustomer.class.getDeclaredFields());
      fields.forEach(field -> {
        if (fieldNameMap.containsKey(field.getName())) {
          Cell cell = row.getCell(fieldNameMap.get(field.getName()));
          if (cell != null) {
            Object value = null;
            if (field.getName().equalsIgnoreCase("phone")) {
              value = new BigDecimal(cell.getNumericCellValue()).toPlainString();
            } else {
              value = convertToFieldType(field, cell);
            }
            try {
              field.setAccessible(true);
              field.set(customer, value); // Set converted value to object
            } catch (IllegalAccessException e) {
              e.printStackTrace();
            }
          }
        }
      });
      List<CustomerExtraInfo> customerExtraInfos = new ArrayList<>();
      for (String attribute : customerAttributes) {
        if (fieldNameMap.containsKey(attribute)) {
          Cell cell = row.getCell(fieldNameMap.get(attribute));
          if (cell == null) {
            continue;
          }
          CustomerExtraInfo extraInfo = CustomerExtraInfo.builder().build();
          extraInfo.setFieldName(attribute);
          extraInfo.setAttributeValue(getCellValueAsString(cell));
          customerExtraInfos.add(extraInfo);
        }
      }
      String extraInfo = gson.toJson(customerExtraInfos);
      customer.setExtraInfos(customerExtraInfos);
      customer.setExtraInfo(extraInfo);
      return customer;
    } catch (Exception e) {
      logger.error("Error processing row {}: {}", row.getRowNum(), e.getMessage());
      return null;
    }
  }

  private static String getCellValueAsString(Cell cell) {
    switch (cell.getCellType()) {
      case STRING:
        return cell.getStringCellValue();
      case NUMERIC:
        return new BigDecimal(cell.getNumericCellValue()).toPlainString(); // Avoid scientific notation
      case BOOLEAN:
        return String.valueOf(cell.getBooleanCellValue());
      case FORMULA:
        return cell.getCellFormula();
      case BLANK:
        return "";
      default:
        return null;
    }
  }

  @SneakyThrows
  private void processBatchCustomers(List<RBCustomer> customers) {
    syncCustomer(customers.get(0));
    logger.info("Processed batch of {} customers", customers.size());
  }

  public void resetProcessing() {
    processingComplete = false;
    currentPage.set(0);
    logger.info("Processing reset. Will start from beginning on next schedule.");
  }

  private static Object convertToFieldType(Field field, Cell cell) {
    String fieldType = field.getType().getSimpleName(); // Get field type name

    switch (cell.getCellType()) {
      case STRING:
        return fieldType.equals("Integer") ? Integer.parseInt(cell.getStringCellValue()) :
          fieldType.equals("Double") ? Double.parseDouble(cell.getStringCellValue()) :
            fieldType.equals("Boolean") ? Boolean.parseBoolean(cell.getStringCellValue()) :
              cell.getStringCellValue();

      case NUMERIC:
        if (fieldType.equals("Integer")) return (int) cell.getNumericCellValue();
        if (fieldType.equals("Double")) return cell.getNumericCellValue();
        if (fieldType.equals("String")) return String.valueOf(cell.getNumericCellValue());
        return cell.getNumericCellValue();

      case BOOLEAN:
        return cell.getBooleanCellValue();

      case FORMULA:
        return cell.getCellFormula(); // Formulas return as String

      case BLANK:
        return fieldType.equals("String") ? "" : null;

      default:
        return null;
    }
  }

  private static String generateHashedCode(RBCustomer customer) {
    customer.setClientId(CLIENT_ID);
    JSONObject jsonSource = new JSONObject(gson.toJson(customer));
    String encodedUrl = getEncodedStr(jsonSource);
    System.out.println(encodedUrl);

    //Thực hiện băm tham số này rồi so sánh
    return CommonUtils.hashMD5(encodedUrl, CLIENT_KEY);
  }

  public void syncCustomer(RBCustomer customer) throws Exception {
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
