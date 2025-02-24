package com.xihe.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * @Author gzy
 * @Date 2024/8/14 13:17
 */
public class JsonUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        // 反序列化：JSON字段中有Java对象中没有的字段时不报错
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 反序列化：不允许基本类型为null
        //objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);

        // 序列化：序列化BigDecimal时不使用科学计数法输出
        objectMapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);

        // 序列化：Java对象为空的字段不拼接JSON
        //objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static <T> T toObject(String string, Class<T> cls) {
        try {
            return objectMapper.readValue(string, cls);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T toObject(String str, TypeReference<T> valueTypeRef) {
        try {
            return objectMapper.readValue(str, valueTypeRef);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("rawtypes")
    public static Map toMap(String str) {
        try {
            return objectMapper.readValue(str, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> List<T> toObjectList(String string, TypeReference<List<T>> typeReference) {
        try {
            return objectMapper.readValue(string, typeReference);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode toTree(String string) {
        try {
            return objectMapper.readTree(string);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 第三方返回通用解析接口
     *
     * @param result 结果集
     * @param msg    错误类型
     * @return com.fasterxml.jackson.databind.JsonNode
     * @author gzy
     * @date 2024/8/23 10:50
     */
    public static JsonNode resolveInterface(String result, String msg) {
        JsonNode jsonNode = toTree(result);
        if (jsonNode.has("code") && 200 == jsonNode.get("code").asInt()) {
            return jsonNode.get("data");
        }

        throw new RuntimeException(msg + "获取失败！");
    }
}
