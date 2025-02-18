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
import java.io.FileOutputStream;
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

public class ReCallMissedCustomerByMarkedExcel {
  private static final String API_URL = "http://localhost:9100/adminapi/customer/update/partner";
  private static final Logger logger = LoggerFactory.getLogger(ReCallMissedCustomer.class);
  //  private static String FILE_PATH = "/code/tnex/Field dữ liệu trên CRM TNEX (tháng 1 thiếu).xlsx";
//  private static String FILE_PATH = "src/main/resources/test.xlsx";
  private static String FILE_PATH = "excel-files/test.xlsx";
  private static final int PAGE_SIZE = 1;
  private static final String PASSWORD = "171284";
  private static final String READ_STATUS_COLUMN = "IsRead";
  private final AtomicInteger currentPage = new AtomicInteger(0);
  private boolean processingComplete = false;
  private static final String CLIENT_ID = "cfeccbajec";
  private static final String CLIENT_KEY = "eaggcjkjeurpfanaklas";
  private static final Gson gson = new Gson();
  private FileInputStream fileInputStream;
  private XSSFWorkbook workbook;

  public static void main(String[] args) {
    try {
      ReCallMissedCustomerByMarkedExcel processor = new ReCallMissedCustomerByMarkedExcel();
      while (!processor.processingComplete) {
        processor.processCustomerExcelBatch();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void processCustomerExcelBatch() {
    if (processingComplete) {
      logger.info("Processing already completed. Skipping this iteration.");
      return;
    }

    try {
      fileInputStream = new FileInputStream(FILE_PATH);
      workbook = (XSSFWorkbook) WorkbookFactory.create(fileInputStream, PASSWORD);

      XSSFSheet sheet = workbook.getSheet("DATA MAP");
      Map<String, Integer> fieldNameMap = getFieldNameMap(sheet);

      if (!fieldNameMap.containsKey(READ_STATUS_COLUMN)) {
        addReadStatusColumn(sheet, fieldNameMap);
      }

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

      saveAndCloseWorkbook();

    } catch (IOException e) {
      logger.error("Error processing Excel file: {}", e.getMessage(), e);
      processingComplete = false;
    }
  }

  private void addReadStatusColumn(XSSFSheet sheet, Map<String, Integer> fieldNameMap) {
    Row headerRow = sheet.getRow(1);
    int newColumnIndex = headerRow.getLastCellNum();
    Cell newHeaderCell = headerRow.createCell(newColumnIndex);
    newHeaderCell.setCellValue(READ_STATUS_COLUMN);
    fieldNameMap.put(READ_STATUS_COLUMN, newColumnIndex);

    // Initialize all rows with 0 (unread)
    for (int i = 2; i <= sheet.getLastRowNum(); i++) {
      Row row = sheet.getRow(i);
      if (row != null) {
        Cell cell = row.createCell(newColumnIndex);
        cell.setCellValue(0);
      }
    }
    saveWorkbook();
  }

  private boolean processBatch(XSSFSheet sheet, Map<String, Integer> fieldNameMap) {
    // Find the next unread row
    int nextUnreadRow = findNextUnreadRow(sheet, fieldNameMap);
    if (nextUnreadRow == -1) {
      return false; // No more unread rows
    }

    try {
      List<String> customerAttributes = Arrays.asList(
        "ThongTinDanhChoTelesale",
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
        "marketingSendLeadSource"
      );

      Row row = sheet.getRow(nextUnreadRow);
      if (row != null) {
        RBCustomer customer = processRow(row, fieldNameMap, customerAttributes);
        if (customer != null) {
          processBatchCustomers(Collections.singletonList(customer));
          markRowAsRead(row, fieldNameMap);
          saveWorkbook();
          logger.info("Processed and marked row {} as read", nextUnreadRow);
        }
      }

      currentPage.incrementAndGet();
      return true;

    } catch (Exception e) {
      logger.error("Error processing row {}: {}", nextUnreadRow, e.getMessage(), e);
      return false;
    }
  }

  private int findNextUnreadRow(XSSFSheet sheet, Map<String, Integer> fieldNameMap) {
    int readStatusColumnIndex = fieldNameMap.get(READ_STATUS_COLUMN);
    for (int rowIndex = 2; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
      Row row = sheet.getRow(rowIndex);
      if (row != null) {
        Cell readStatusCell = row.getCell(readStatusColumnIndex);
        if (readStatusCell == null || readStatusCell.getNumericCellValue() == 0) {
          return rowIndex;
        }
      }
    }
    return -1; // No unread rows found
  }

  private void markRowAsRead(Row row, Map<String, Integer> fieldNameMap) {
    Cell readStatusCell = row.getCell(fieldNameMap.get(READ_STATUS_COLUMN));
    if (readStatusCell == null) {
      readStatusCell = row.createCell(fieldNameMap.get(READ_STATUS_COLUMN));
    }
    readStatusCell.setCellValue(1);
  }

  private void saveWorkbook() {
    try (FileOutputStream fileOut = new FileOutputStream(FILE_PATH)) {
      workbook.write(fileOut);
    } catch (IOException e) {
      logger.error("Error saving workbook: {}", e.getMessage(), e);
    }
  }

  private void saveAndCloseWorkbook() {
    try {
      saveWorkbook();
      if (workbook != null) {
        workbook.close();
      }
      if (fileInputStream != null) {
        fileInputStream.close();
      }
    } catch (IOException e) {
      logger.error("Error closing workbook: {}", e.getMessage(), e);
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
              field.set(customer, value);
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
        return new BigDecimal(cell.getNumericCellValue()).toPlainString();
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
    String fieldType = field.getType().getSimpleName();

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
        return cell.getCellFormula();

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