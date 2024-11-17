package com.xihe.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Author gzy
 * @Date 2024/9/6 17:57
 */
@Data
public class ChannelInfo {
    @Schema(description = "渠道代码")
    private String channelid;

    @Schema(description = "渠道名称")
    private String channelname;

    @Schema(description = "渠道中文名")
    private String channelCnName;

    @Schema(description = "渠道英文名")
    private String channelEnName;

    @Schema(description = "是否支持快递制单（0：不支持，1：支持）")
    private Integer ifmaking;

    @Schema(description = "起运地编码")
    private String origin;

    @Schema(description = "可用客户")
    private String clientArr;

    @Schema(description = "K5类别（1:入仓出仓渠道、2：仅入仓渠道、0：仅出仓渠道）")
    private Integer type;

    @Schema(description = "停用标志（0:启用、1：停用、2：停用并报价有效期无效）")
    private Integer ifStop;
}
