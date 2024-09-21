package com.xihe.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xihe.entity.Fuel;
import com.xihe.util.DateUtil;
import com.xihe.util.JsonUtil;
import jakarta.annotation.Resource;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @Author gzy
 * @Date 2024/8/14 16:25
 */
@Service
@Slf4j
public class FuelImpl {
    @Resource
    UpsFuel upsFuel;
    @Resource
    FedexFuel fedexFuel;
    @Resource
    TntFuel tntFuel;
    @Resource
    DhlFuel dhlFuel;
    @Resource
    StringRedisTemplate stringRedisTemplate;
    //第三方和UPS日期格式
    private static final SimpleDateFormat thirdDateFormat = new SimpleDateFormat("yyyy/MM/dd");
    private static final SimpleDateFormat upsDateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat fedexDateFormat = new SimpleDateFormat("yyyy年MM月dd日");
    private static final SimpleDateFormat dhlDateFormat = new SimpleDateFormat("MM月 yyyy");

    /**
     * 解析燃油的日期格式，当在周一的时候校验是否需要更新，若获取到的燃油时间为7天前的时间则返回1个小时的缓存时间，否则返回到下周一的时间
     *
     * @param fuelKey  燃油类型
     * @param fuelDate 获取到的燃油最新时间
     * @return long
     * @author gzy
     * @date 2024/9/10 10:47
     */
    private long getCacheTime(String fuelKey, String fuelDate) {
        //大于7天表示还未到更新时间，则每个小时更新一次
        Date rateDate = null;
        long expireTime = 7;
        try {
            if ("all".equals(fuelKey)) {
                rateDate = thirdDateFormat.parse(fuelDate);
            } else if ("ups".equals(fuelKey)) {
                rateDate = upsDateFormat.parse(fuelDate);
            } else if ("fedex".equals(fuelKey)) {
                rateDate = fedexDateFormat.parse(fuelDate);
            } else if ("dhl".equals(fuelKey)) {
                rateDate = dhlDateFormat.parse(fuelDate);
                expireTime = 30;
            }
        } catch (ParseException e) {
            throw new RuntimeException("获取" + fuelKey + "燃油时，日期格式校验失败！");
        }
        assert rateDate != null;
        long tempRateTime = rateDate.getTime();
        long difference = 3600L;
        if (7 == expireTime) {
            if (System.currentTimeMillis() - tempRateTime < 86400000 * expireTime) {
                Date nowDate = new Date();
                return DateUtil.getDifference(new Date(), DateUtil.getNextWeekMonday(nowDate)) / 1000;
            }
        } else {
            if (System.currentTimeMillis() - tempRateTime < 86400000 * expireTime) {
                return DateUtil.getMonthDifference();
            }
        }
        return difference;
    }

    public JsonNode getCommonFuel(String fuelKey) {
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(fuelKey + "--fuel"))) {
            Map<String, String> fuelMap = switch (fuelKey) {
                case "ups" -> upsFuel.getCommonFuel();
                case "fedex" -> fedexFuel.getCommonFuel();
                case "tnt" -> tntFuel.getCommonFuel();
                case "dhl" -> dhlFuel.getCommonFuel();
                default -> new HashMap<>();
            };
            if (fuelMap.isEmpty()) {
                throw new RuntimeException("获取燃油失败！");
            }
            Map<String, List<Fuel>> resolveFuel = switch (fuelKey) {
                case "ups" -> upsFuel.resolveFuel(fuelMap);
                case "fedex" -> fedexFuel.resolveFuel(fuelMap);
                case "tnt" -> tntFuel.resolveFuel(fuelMap);
                case "dhl" -> dhlFuel.resolveFuel(fuelMap);
                default -> new HashMap<>();
            };

            if (resolveFuel.isEmpty()) {
                throw new RuntimeException("获取燃油失败！");
            }

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode objectNode = objectMapper.createObjectNode();
            for (String key : resolveFuel.keySet()) {
                List<Fuel> fuelList = resolveFuel.get(key);
                objectNode.set(key, JsonUtil.toTree(JsonUtil.toJson(fuelList)));
            }

            for (String key : resolveFuel.keySet()) {
                hashOperations.put(fuelKey + "--fuel", key, objectNode.get(key).toString());
            }
            String fuelDate = null;
            if ("ups".equals(fuelKey)) {
                fuelDate = resolveFuel.get("us").get(0).getFuelDate();
            } else if ("fedex".equals(fuelKey) || "dhl".equals(fuelKey)) {
                fuelDate = resolveFuel.get("cn").get(0).getFuelDate();
            }
            if (null == fuelDate) {
                throw new RuntimeException("获取" + fuelKey + "燃油失败！");
            }
            long difference = getCacheTime(fuelKey, fuelDate);
            log.info("获取{}燃油成功，添加缓存时间为：{}！", fuelKey, difference);
            stringRedisTemplate.expire(fuelKey + "--fuel", difference, TimeUnit.SECONDS);
            return objectNode;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();
        Set<String> keySet = hashOperations.keys(fuelKey + "--fuel");
        for (String key : keySet) {
            String value = hashOperations.get(fuelKey + "--fuel", key);
            objectNode.set(key, JsonUtil.toTree(value));
        }

        return objectNode;
    }

    public JsonNode getAllFuel() {
//        JsonNode ups = getCommonFuel("ups");
//        JsonNode fedex = getCommonFuel("fedex");
//        JsonNode dhl = getCommonFuel("dhl");
//        ObjectMapper objectMapper = new ObjectMapper();
//        ObjectNode objectNode = objectMapper.createObjectNode();
//        objectNode.set("ups", ups.get(0));
//        objectNode.set("fedex", fedex.get(0));
//        objectNode.set("dhl", dhl.get(0));
//        return objectNode;

        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        String fuel = valueOperations.get("all--fuel");
        if (null != fuel) {
            return JsonUtil.toTree(fuel);
        }
        HttpResponse<String> response = Unirest.get("http://www.shipsoeasy.com:8401/upsrate/selectRate")
                .asString();
        String backStr = response.getBody();
        JsonNode backJson = JsonUtil.toTree(backStr);
        JsonNode regionList = backJson.get("data").get(1).get("regionList").get(1).get("rateList");
        ObjectNode nowNode = (ObjectNode) regionList.get(0);
        ObjectNode beforeNode = (ObjectNode) regionList.get(1);
        //给UPS添加国际地面出口进口附加费
        JsonNode ups = getCommonFuel("ups");
        nowNode.put("communs5", ups.get("us").get(0).get("fuel5").asText());
        beforeNode.put("communs5", ups.get("us").get(1).get("fuel5").asText());
        //获取UPS的校验日期，第三方的日期格式为2024/09/09
        long difference = getCacheTime("all", nowNode.get("ratetime").asText());
        log.info("获取第三方燃油成功，添加缓存时间为：{}！", difference);
        valueOperations.set("all--fuel", backJson.toString(), difference, TimeUnit.SECONDS);
        return backJson;
    }
}