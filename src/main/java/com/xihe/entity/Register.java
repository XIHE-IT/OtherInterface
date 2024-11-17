package com.xihe.entity;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author gzy
 * @Date 2024/8/28 16:12
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Register {

    @Schema(description = "客户账号")
    @NotNull(message = "客户账号不能为空")
    @Size(max = 20, message = "客户账号长度不能超过20个字符")
    private String userId;

    @Schema(description = "客户名称")
    @NotNull(message = "客户名称不能为空")
    @Size(max = 20, message = "客户名称长度不能超过20个字符")
    private String clientName;

    @Schema(description = "客户等级编号")
    @NotNull(message = "客户等级编号不能为空")
    private Integer gradeId;

    @Schema(description = "公司编码")
    @NotNull(message = "公司编码不能为空")
    @Size(max = 20, message = "公司编码长度不能超过20个字符")
    private String companyCode;

    @Schema(description = "手机")
    @NotNull(message = "手机不能为空")
    @Size(max = 20, message = "手机长度不能超过20个字符")
    private String phone;

    @Schema(description = "电话")
    @NotNull(message = "电话不能为空")
    @Size(max = 20, message = "电话长度不能超过20个字符")
    private String mobile;

    @Schema(description = "客户密码")
    @NotNull(message = "客户密码不能为空")
    @Size(max = 20, message = "客户密码长度不能超过20个字符")
    private String passWord;

    @Schema(description = "结算方式（1：日结、2：周结、3：月结、4：签收结、5：半月结）")
    @NotNull(message = "结算方式不能为空")
    private Integer recType;

    @Schema(description = "扣货方式（0：不扣货、1：信用额度、2：收款周期、3：每票扣货、4：付款周期 加 信用额度、5：付款周期 或 信用额度、6：收货天数、7：收货天数 或 信用额度、8：收货周期、9：收货周期 加 信用额度、10：收货周期 或 信用额度）")
    @NotNull(message = "扣货方式不能为空")
    private Integer deductionType;

    @Schema(description = "对账日期")
    @NotNull(message = "对账日期不能为空")
    private Integer checkDate;

    @Schema(description = "token")
    @NotNull(message = "token不能为空")
    @Size(max = 50, message = "token长度不能超过50个字符")
    private String token;
}
