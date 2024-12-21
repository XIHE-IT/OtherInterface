package com.xihe.init;

import lombok.Getter;

/**
 * @Author gzy
 * @Date 2024/10/26 14:18
 */
@Getter
public enum RedisGlobalEnum {

    K5SITEKEY("k5-site", "K5所有站点"),
    T6SITEKEY("T6-site", "T6所有站点");

    private final String value;
    private final String note;

    RedisGlobalEnum(String value, String note) {
        this.value = value;
        this.note = note;
    }
}
