package com.xihe.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Author gzy
 * @Date 2024/8/14 13:57
 */
@Data
@Schema(description = "校验私宅")
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckHome {
    @Schema(description = "类型（ups、fedex，其他暂未对接）")
    @NotNull(message = "类型不能为空！")
    @Size(max = 10, message = "类型字符个数不能大于10个")
    private String type;

    @Schema(description = "收件人名称")
    @Size(max = 40, message = "类型字符个数不能大于40个")
    private String consigneeName;

    @Schema(description = "建筑名称")
    @Size(max = 40, message = "建筑名称字符个数不能大于40个")
    private String buildingName;

    @Schema(description = "地址行（地址行（街道号、街道名称和街道类型）用于街道级别信息。其他次要信息（公寓、套房、楼层等）。仅适用于美国和波多黎各。如果用户选择 RegionalRequestIndicator，则忽略。）")
    @NotNull(message = "地址行不能为空！")
    @Size(max = 100, message = "地区字符个数不能大于100个")
    private List<String> addressLine;

    @Schema(description = "地区")
    @Size(max = 100, message = "地区字符个数不能大于100个")
    private String region;

    @Schema(description = "州或省/地区名称")
    @Size(max = 30, message = "州或省/地区名称字符个数不能大于30个")
    private String politicalDivision1;

    @Schema(description = "城市或城镇名称")
    @Size(max = 30, message = "城市或城镇名称字符个数不能大于30个")
    private String politicalDivision2;

    @Schema(description = "邮编")
    @NotNull(message = "邮编不能为空！")
    @Size(max = 10, message = "邮编字符个数不能大于10个")
    private String postcodePrimaryLow;

    @Schema(description = "邮编扩充")
    @Size(max = 10, message = "邮编扩充字符个数不能大于10个")
    private String postcodeExtendedLow;

    @Schema(description = "城市化（波多黎各政治部门 3。仅适用于波多黎各）")
    @Size(max = 30, message = "城市化字符个数不能大于30个")
    private String urbanization;

    @Schema(description = "国家二字码")
    @NotNull(message = "国家二字码不能为空！")
    @Size(max = 2, message = "邮编扩充字符个数不能大于2个")
    private String countryCode;
}
