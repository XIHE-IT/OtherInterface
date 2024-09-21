package com.xihe.entity;

import lombok.Data;

/**
 * @Author gzy
 * @Date 2024/9/6 17:57
 */
@Data
public class ChannelInfo {
    //渠道代码
    private String channelid;
    //渠道中文名
    private String channelname;
    //渠道英文名
    private String channelenname;
    //是否支持快递制单
    private Integer ifmaking;
    //起运地编码
    private String origin;
    //可用客户
    private String clientArr;
}
