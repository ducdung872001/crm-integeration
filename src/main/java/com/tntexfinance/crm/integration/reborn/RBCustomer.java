package com.tntexfinance.crm.integration.reborn;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.tntexfinance.crm.integration.CommonUtils;
import com.tntexfinance.crm.integration.CustomerExtraInfo;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Cấu trúc các trường dữ liệu chuẩn bên ứng dụng Reborn CRM
 */

@Data
@Accessors(chain = true)
public class RBCustomer {
    private Integer id;

    //Tên khách hàng(*)
    private String name;

    //Số điện thoại(*)
    private String phone;

    //Mã khách hàng
    private String code;

    //Điện thoại người giới thiệu
    private String recommenderPhone;

    //Email
    private String email;

    //Tài khoản Facebook
    private String profileLink;

    //Tài khoản Facebook thứ 2
    private String secondProfileLink;

    //Trạng thái hoạt động tài khoản Facebook
    private Boolean profileStatusValue;

    private Integer profileStatus;

    //Trạng thái hoạt động tài khoản Facebook thứ 2
    private Boolean secondProfileStatusValue;
    private Integer secondProfileStatus;

    //Địa chỉ
    private String address;

    //Ngày sinh
    private String birthday;

    //Giới tính
    private String genderName; // nũ 1, nam 2
    private Integer gender; // nũ 1, nam 2

    //Nguồn khách hàng
    private String source;
    private Integer sourceId;

    //Chi nhánh(*)
    private String branchName;
    private Integer branchId;

    //Ngành nghề
    private String careerName;
    private Integer careerId;

    //Nhóm khách hàng
    private String cgpName;
    private Integer cgpId;

    //Người phụ trách
    private String employeeName;
    private Integer employeeId;

    //Tình trạng cuội gọi đầu tiên
    private String firstCall;

    private Boolean isValid;

    //Các trường lỗi
    private String errorCode;

    //Chi tiết lỗi
    private String messageError;

    //Tình trạng đã xác định được nguồn hay chưa
    private Integer uploadId;
    private Integer contactId;

    //Số điện thoại liên hệ
    private String contactPhone;

    //Email người liên hệ
    private String contactEmail;
    private String contactEmails;

    //Tên người liên hệ
    private String contactName;

    //Mã số thuế
    private String taxCode;

    private String extraInfo;
    private List<CustomerExtraInfo> extraInfos;

    // Id cuar customer
    private Integer duplicateId;
    private Integer bsnId;
    private Integer custType;
    private Integer creatorId;
    private String status;

    private String phoneMasked;
    private String phoneHashed;
    private String phoneEncrypted;

    private String emailMasked;
    private String emailHashed;
    private String emailEncrypted;

    private RBCustomer duplicateData;

    public String getEmail() {
        if (email == null) return null;
        return email.toLowerCase();
    }

    //Thông tin bổ sung để nhận dữ liệu từ bên CDP (Để đưa vào pipeline bán hàng)
    private String campaignCode;
    private String clientId; //Xem đẩy từ nguồn nào => xác định được bsnId
    private String sourceName; //Nguồn được phân tệp
    private String actionWhenDuplicated; //override, merge, ignore, manual (Tự có cơ chế resolve bằng thủ công)
    private String hashedCode; //Giá trị băm các tham số

    public Integer getGender() {
        if (gender != null) return gender;
        if (genderName == null) return null;
        if (genderName.equalsIgnoreCase("Nữ")) return 1;
        if (genderName.equalsIgnoreCase("Nam")) return 2;
        return null;
    }

    public Integer getGenderId() {
        return CommonUtils.NVL(gender);
    }

    public Integer getProfileStatus() {
        return profileStatusValue != null && profileStatusValue == Boolean.TRUE ? 1 : 0;
    }

    public Integer getSecondProfileStatus() {
        return secondProfileStatusValue != null && secondProfileStatusValue == Boolean.TRUE ? 1 : 0;
    }

    public boolean isNullData() {
        return StringUtils.isEmpty(name) &&
                StringUtils.isEmpty(phone) &&
                StringUtils.isEmpty(email) &&
                StringUtils.isEmpty(branchName);
    }

    public String getExtraInfo() {
        if (extraInfo != null) return extraInfo;
        if (extraInfos == null || extraInfos.isEmpty()) return null;
        try {
            // Create Gson instance
            Gson gson = new Gson();

            extraInfo = gson.toJson(extraInfos);
        } catch (Exception e) {
            return null;
        }
        return extraInfo;
    }

    public List<CustomerExtraInfo> getExtraInfos() {
        if (extraInfos != null) {
            return extraInfos;
        }

        if (extraInfo != null) {
            try {
                // Create Gson instance
                Gson gson = new Gson();

                // Define the type of the List
                Type listType = new TypeToken<List<CustomerExtraInfo>>() {
                }.getType();

                // Convert JSON string to List
                extraInfos = gson.fromJson(extraInfo, listType);
            } catch (Exception e) {
                return null;
            }
        }

        return extraInfos;
    }
}
