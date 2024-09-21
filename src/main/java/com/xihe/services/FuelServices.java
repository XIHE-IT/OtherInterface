package com.xihe.services;

import com.xihe.entity.CheckHome;
import com.xihe.entity.Fuel;

import java.util.List;
import java.util.Map;

/**
 * 获取燃油接口
 *
 * @Author gzy
 * @Date 2024/8/14 10:57
 */
public interface FuelServices {
//    /**
//     * 获取燃油，如果redis中不存在则从官网获取，如果存在则校验是否为有效期内，否则重新获取
//     *
//     * @param key 需要获取燃油的key
//     * @return java.lang.String
//     * @author gzy
//     * @date 2024/8/14 13:51
//     */
//    default List<Fuel> getFuel(StringRedisTemplate stringRedisTemplate, String key) {
//        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
//        String fuel = valueOperations.get(key + "--fuel");
//        if (null == fuel) {
//            fuel = getCommonFuel();
//            if (null != fuel) {
//                Date nowDate = new Date();
//                Long difference = DateUtil.getDifference(DateUtil.getThisWeekMonday(nowDate), DateUtil.getNextWeekMonday(nowDate));
//                valueOperations.set(key + "--fuel", fuel, difference, TimeUnit.SECONDS);
//            }
//        }
//
//        if (null == fuel) {
//            throw new RuntimeException("获取燃油失败！");
//        }
//        return resolveFuel(fuel);
//    }

    /**
     * 官网获取燃油
     *
     * @return java.lang.String
     * @author gzy
     * @date 2024/8/15 15:25
     */
    Map<String, String> getCommonFuel();

    /**
     * 解析燃油
     *
     * @param fuelMap 从getCommonFuel或者reids获取数据
     * @return java.util.List<com.xihe.entity.Fuel>
     * @author gzy
     * @date 2024/8/15 15:25
     */
    Map<String, List<Fuel>> resolveFuel(Map<String, String> fuelMap);

    /**
     * 校验私宅数据（0 未分类，1 商业， 2 住宅， 3 混合）
     * 未知（如果仅提供邮政编码，地址验证服务将返回商业/住宅分类“未知”。）
     * 混合（如果是多租户地址，且包含商业和住宅单位。）
     *
     * @param checkHome 请求的校验私宅数据
     * @return java.lang.String
     * @author gzy
     * @date 2024/8/15 15:29
     */
    Integer getCheckHome(CheckHome checkHome);
}
