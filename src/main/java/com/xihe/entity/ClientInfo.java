package com.xihe.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author gzy
 * @Date 2024/9/6 16:41
 */
@Data
public class ClientInfo {
    //客户编码
    private String clientid;
    //客户简称
    private String clientname;
    //客户全称
    private String totalname;
    //客户类型
    private Integer clienttype;
    //客户等级
    private Integer levelid;
    //电话
    private String tel;
    //手机
    private String mobile;
    //注册号
    private String regno;
    //注册地址
    private String clientaddress;
    //其他联系方式
    private String othertel;
    //取件地址
    private String pickaddr;
    //结算方式
    private Integer rectypeid;
    //扣货方式
    private Integer holdway;
    //信用额度
    private BigDecimal credit;
    //默认报价等级
    private String degreeid;
    //业务员
    private String salemanid;
    //客服人员
    private String serviceid;
    //财务人员
    private String financeid;
    //机密资料
    private String confidential;
    //状态
    private Integer status;
    //备注
    private String note;
    //客户附件
    private String filePathName;
    //登录名
    private String userid;
    //密码
    private String password;
    //token
    private String token;
}
