package com.xihe.util;

import com.xihe.entity.CheckHome;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @Author gzy
 * @Date 2024/9/2 11:06
 */
@Component
public class BusinessUtil {
    /**
     * 根据校验私宅数据获取key
     *
     * @param checkHome 校验私宅数据
     * @return int
     * @author gzy
     * @date 2024/9/2 11:08
     */
    public String getCheckHomeKey(CheckHome checkHome) {
        StringBuilder sBuilder = new StringBuilder();
        if (null != checkHome.getCountryCode()) {
            sBuilder.append(checkHome.getCountryCode()).append("---");
        }
        if (null != checkHome.getPoliticalDivision1()) {
            sBuilder.append(checkHome.getPoliticalDivision1()).append("---");
        }
        if (null != checkHome.getPoliticalDivision2()) {
            sBuilder.append(checkHome.getPoliticalDivision2()).append("---");
        }
        if (null != checkHome.getPostcodePrimaryLow()) {
            sBuilder.append(checkHome.getPostcodePrimaryLow()).append("---");
        }
        if (null != checkHome.getCountryCode()) {
            sBuilder.append(checkHome.getCountryCode()).append("---");
        }
        if (null != checkHome.getAddressLine() && !checkHome.getAddressLine().isEmpty()) {
            sBuilder.append(checkHome.getAddressLine().get(0));
        }

        return sBuilder.toString();
    }

    /**
     * 获取校验私宅的返回数据
     *
     * @param type           校验私宅数据
     * @param hashOperations 操作redis
     * @return int
     * @author gzy
     * @date 2024/9/2 11:08
     */
    public int getCheckHome(String type, String key, HashOperations<String, String, String> hashOperations) {
        String backStr = hashOperations.get(type + "-check-home", key);
        if (StringUtils.hasText(backStr)) {
            return Integer.parseInt(backStr);
        }
        return -1;
    }
}
