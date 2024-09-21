package com.xihe.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author gzy
 * @Date 2024/8/14 13:57
 */
@Data
@Schema(description = "燃油标准类")
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Fuel {
    @Schema(description = "地面附加费")
    private String fuel1;
    @Schema(description = "航空附加费")
    private String fuel2;
    @Schema(description = "空运出口附加费")
    private String fuel3;
    @Schema(description = "空运进口附加费")
    private String fuel4;
    @Schema(description = "地面出口进口附加费")
    private String fuel5;
    @Schema(description = "更新时间")
    private String fuelDate;
}
