package com.xihe.services;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 城邮服务
 *
 * @Author gzy
 * @Date 2024/9/4 13:54
 */
public interface CityPostService {
    /**
     * 获取所有的省/州正则表达式
     *
     * @return com.xihe.internalauthority.entity.BasicRoom
     * @author gzy
     * @date 2024-09-03 17:29:35
     */
    String getAllProvince();

    /**
     * 根据省/州获取所有的城市
     *
     * @param name 国家
     * @return java.util.List<com.xihe.internalauthority.entity.BasicRoom>
     * @author gzy
     * @date 2024-09-03 17:29:35
     */
    String provinceGetTownname(String name);

    /**
     * 根据省/州判断地址中是否存在,若存在则切割地址组
     *
     * @param address 全部的地址
     * @return com.fasterxml.jackson.databind.JsonNode
     * @author gzy
     * @date 2024/9/4 9:56
     */
    ObjectNode judgeProvinceExists(String address);

    /**
     * 按上面省/州查找完毕后,继续查找城市
     *
     * @param jsonNode 继续传导下去的数据
     * @author gzy
     * @date 2024-09-03 17:29:35
     */
    void judgeTownnameExists(ObjectNode jsonNode);

    /**
     * 按上面省/州和城市校验完毕后,继续查找邮编
     *
     * @param jsonNode 继续传导下去的数据
     * @author gzy
     * @date 2024-09-03 17:29:35
     */
    void judgePostCodeExists(ObjectNode jsonNode);

    /**
     * 根据地址截取省/州,城市,邮编
     *
     * @param address 完整地址信息
     * @return com.fasterxml.jackson.databind.JsonNode
     * @author gzy
     * @date 2024/9/4 13:23
     */
    ObjectNode interceptAddress(String address);
}
