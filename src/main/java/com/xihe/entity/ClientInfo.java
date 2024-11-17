package com.xihe.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author gzy
 * @Date 2024/9/6 16:41
 */
@Data
public class ClientInfo {
    @Schema(description = "客户编码")
    @NotNull(message = "客户编码不能为空")
    @Size(max = 20, message = "客户编码长度不能超过20个字符")
    private String clientid;

    @Schema(description = "客户简称")
    @NotNull(message = "客户简称不能为空")
    @Size(max = 20, message = "客户简称长度不能超过20个字符")
    private String clientname;

    @Schema(description = "客户全称")
    @NotNull(message = "客户全称不能为空")
    @Size(max = 20, message = "客户全称长度不能超过20个字符")
    private String totalname;

    @Schema(description = "客户类型")
    private Integer clienttype;

    @Schema(description = "客户等级")
    private Integer levelid;

    @Schema(description = "电话")
    @Size(max = 20, message = "电话长度不能超过20个字符")
    private String tel;

    @Schema(description = "手机")
    @Size(max = 20, message = "手机长度不能超过20个字符")
    private String mobile;

    @Schema(description = "注册号")
    @Size(max = 200, message = "注册号长度不能超过200个字符")
    private String regno;

    @Schema(description = "注册地址")
    @Size(max = 200, message = "注册地址长度不能超过200个字符")
    private String clientaddress;

    @Schema(description = "其他联系方式")
    @Size(max = 100, message = "其他联系方式长度不能超过100个字符")
    private String othertel;

    @Schema(description = "取件地址")
    @Size(max = 100, message = "取件地址长度不能超过100个字符")
    private String pickaddr;

    @Schema(description = "结算方式（1：日结、2：周结、3：月结、4：签收结、5：半月结）")
    @NotNull(message = "结算方式不能为空")
    private Integer rectype;

    @Schema(description = "扣货方式（0：不扣货、1：信用额度、2：收款周期、3：每票扣货、4：付款周期 加 信用额度、5：付款周期 或 信用额度、6：收货天数、7：收货天数 或 信用额度、8：收货周期、9：收货周期 加 信用额度、10：收货周期 或 信用额度）")
    @NotNull(message = "扣货方式不能为空")
    private Integer holdway;

    @Schema(description = "对账日期")
    @NotNull(message = "对账日期不能为空")
    private Integer checkDate;

    @Schema(description = "信用额度")
    private BigDecimal credit;

    @Schema(description = "默认报价等级")
    private String degreeid;

    @Schema(description = "业务员")
    private String salemanid;

    @Schema(description = "客服人员")
    private String serviceid;

    @Schema(description = "财务人员")
    private String financeid;

    @Schema(description = "机密资料")
    private String confidential;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "备注")
    private String note;

    @Schema(description = "客户附件")
    private String filePathName;

    @Schema(description = "登录名")
    private String userid;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "token")
    private String token;
}
