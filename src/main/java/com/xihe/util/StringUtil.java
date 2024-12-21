package com.xihe.util;

import java.math.BigDecimal;

/**
 * @Author gzy
 * @Date 2024/10/28 17:07
 */
public class StringUtil {
    /**
     * Object转字符串
     *
     * @param obj 对象值
     * @return java.lang.String
     * @author gzy
     * @date 2024/10/28 17:09
     */
    public static String changeObjStr(Object obj) {
        if (null == obj) {
            return "";
        }

        return obj.toString();
    }

    /**
     * 字符串转换，若为null则返回空值
     *
     * @param str 对象值
     * @return java.lang.String
     * @author gzy
     * @date 2024/10/28 17:09
     */
    public static String changeObjStr(String str) {
        if (null == str) {
            return "";
        }

        return str;
    }

    /**
     * Object转整形
     *
     * @param obj 对象值
     * @return java.lang.String
     * @author gzy
     * @date 2024/10/28 17:09
     */
    public static Integer changeObjInt(Object obj) {
        if (null == obj) {
            return 0;
        }

        return Integer.parseInt(obj.toString());
    }

    /**
     * Object转整形
     *
     * @param obj 对象值
     * @return java.lang.String
     * @author gzy
     * @date 2024/10/28 17:09
     */
    public static BigDecimal changeObjBig(Object obj) {
        if (null == obj) {
            return BigDecimal.valueOf(0);
        }

        return BigDecimal.valueOf(Double.parseDouble(obj.toString()));
    }
}
