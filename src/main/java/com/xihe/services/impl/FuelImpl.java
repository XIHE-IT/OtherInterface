package com.xihe.services.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xihe.entity.Fuel;
import com.xihe.util.DateUtil;
import com.xihe.util.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    //    private static final SimpleDateFormat upsDateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat dhlDateFormat = new SimpleDateFormat("yyyy/MM");
    //最小刷新时间
    private static final long differenceTime = 3600L * 5;

    /**
     * 取出查询出燃油中最小的日期
     *
     * @param resolveFuel 已经查询出的燃油
     * @return java.util.Date
     * @author gzy
     * @date 2024/10/18 19:02
     */
    private Date getMinDate(Map<String, List<Fuel>> resolveFuel, String fuelKey) {
        //先把所有的日期转成Date
        List<Date> fuelDateList = new ArrayList<>();
        for (String key : resolveFuel.keySet()) {
            String date = resolveFuel.get(key).get(0).getFuelDate();
            try {
                Date tempDate = thirdDateFormat.parse(date);
                fuelDateList.add(tempDate);
            } catch (ParseException e) {
                throw new RuntimeException("获取" + fuelKey + "燃油时，日期格式校验失败！");
            }
        }
        //取最小的日期
        return Collections.min(fuelDateList);
    }

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
        long expireTime = 5;
        try {
            if ("all".equals(fuelKey) || "ups".equals(fuelKey) || "fedex".equals(fuelKey)) {
                rateDate = thirdDateFormat.parse(fuelDate);
            } else if ("dhl".equals(fuelKey)) {
                rateDate = dhlDateFormat.parse(fuelDate);
                expireTime = 30;
            }
        } catch (ParseException e) {
            throw new RuntimeException("获取" + fuelKey + "燃油时，日期格式校验失败！");
        }
        assert rateDate != null;
        long tempRateTime = rateDate.getTime();
        if (5 == expireTime) {  //ups、fedex
            /*
             * 这里校验当前时间和下周一的时间进行比较
             * 1、获取到的时间小于下周一的时间，则表示未更新到，则5个小时后继续更新
             * 2、若获取到的时间大于下周一的时间，则缓存时间为当前时间距离下周五0点的时间
             */
            Date nowDate = new Date();
            Date nextWeekMonday = DateUtil.getNextWeekMonday(nowDate);
            long nextMondayTime = nextWeekMonday.getTime();
            if (tempRateTime >= nextMondayTime) {
                return DateUtil.getDifference(new Date(), nextWeekMonday) / 1000 + (4 * 24 * 60 * 60);
            }
        } else {                //dhl
            long nextMonthDayTime = DateUtil.getNextMonthDay().getTime();
            if (tempRateTime >= nextMonthDayTime) {
                return DateUtil.getMonthDifference(2) - (4 * 24 * 60 * 60);
            }
        }
        return differenceTime;
    }

    /**
     * 解析燃油的日期格式，当在周一的时候校验是否需要更新，若获取到的燃油时间为7天前的时间则返回1个小时的缓存时间，否则返回到下周一的时间
     *
     * @param fuelKey  燃油类型
     * @param rateDate 当前燃油类型获取到网站中的最小日期时间
     * @return long
     * @author gzy
     * @date 2024/10/18 19:04
     */
    private long getCacheTime(String fuelKey, Date rateDate) {
        //大于7天表示还未到更新时间，则每个小时更新一次
        long expireTime = 5;
        if ("dhl".equals(fuelKey)) {
            expireTime = 30;
        }
        long tempRateTime = rateDate.getTime();
        if (5 == expireTime) {  //ups、fedex
            /*
             * 这里校验当前时间和下周一的时间进行比较
             * 1、获取到的时间小于下周一的时间，则表示未更新到，则5个小时后继续更新
             * 2、若获取到的时间大于下周一的时间，则缓存时间为当前时间距离下周五0点的时间
             */
            Date nowDate = new Date();
            Date nextWeekMonday = DateUtil.getNextWeekMonday(nowDate);
            long nextMondayTime = nextWeekMonday.getTime();
            String week = DateUtil.getDayOfWeekString(nowDate);
            /*
             * 当在周五到下周一之前提前获取到燃油时
             * 还有极少数的情况是，但在周一、周二才更新的燃油，这个时候也要重新计算缓存时间，不能5小时一直刷
             * 计算方式为若获取到的燃油最小时间加7天的话超过下周一的时间表示新燃油已经更新，否则旧燃油+7必定小于下周一
             */
            if (tempRateTime >= nextMondayTime) {
                return DateUtil.getDifference(new Date(), nextWeekMonday) / 1000 + (4 * 24 * 60 * 60);
            } else if (tempRateTime + 7 * 24 * 60 * 60 * 1000 >= nextMondayTime && ("星期一".equals(week) || "星期二".equals(week))) {
                return DateUtil.getDifference(new Date(), nextWeekMonday) / 1000 - (3 * 24 * 60 * 60);
            }
        } else {                //dhl
            long nextMonthDayTime = DateUtil.getNextMonthDay().getTime();
            if (tempRateTime >= nextMonthDayTime) {
                return DateUtil.getMonthDifference(2) - (4 * 24 * 60 * 60);
            }
        }
        return differenceTime;
    }

    /**
     * 因为存在不同的接口，且fedex是通过浏览器抓取，所以存在异常情况，当出现异常情况时，直接返回5小时的缓存时间
     *
     * @param resolveFuel 解析到的燃油
     * @param fuelKey     燃油KEY，ups、fedex、dhl
     * @return long
     * @author gzy
     * @date 2024/10/25 18:05
     */
    private long getCacheTime(Map<String, List<Fuel>> resolveFuel, String fuelKey) {
        if (resolveFuel.isEmpty() || resolveFuel.size() == 1) {
            return 0;
        }
        Date minDate = getMinDate(resolveFuel, fuelKey);
        if (null == minDate) {
            throw new RuntimeException("获取" + fuelKey + "燃油失败！");
        }
        return getCacheTime(fuelKey, minDate);
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
            long difference = getCacheTime(resolveFuel, fuelKey);
            log.info("获取{}燃油成功，添加缓存时间为：{}！", fuelKey, difference);
            if (0 != difference) {
                stringRedisTemplate.expire(fuelKey + "--fuel", difference, TimeUnit.SECONDS);
            }
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

    public JSONArray getAllFuel() {
        JsonNode ups = getCommonFuel("ups");
        JsonNode fedex = getCommonFuel("fedex");
        JsonNode dhl = getCommonFuel("dhl");
        JSONArray backArr = new JSONArray();
        JSONObject upsJson = new JSONObject();
        int subIndex = 5;
        String str = "-";
        if (!ups.isEmpty()) {
            JSONArray upsList = new JSONArray();

            if (ups.has("us")) {
                JSONObject cnJson = new JSONObject();
                cnJson.put("regionInfo", "美国");
                JSONArray rateList = new JSONArray();
                for (int i = 0; i < 2; i++) {
                    JsonNode jsonNode = ups.get("us").get(i);
                    JSONObject tempJson = new JSONObject();
                    tempJson.put("communs1", jsonNode.get("fuel3").asText());
                    tempJson.put("communs2", jsonNode.get("fuel4").asText());
                    tempJson.put("communs3", jsonNode.get("fuel1").asText());
                    tempJson.put("communs4", jsonNode.get("fuel2").asText());
                    String communs5 = jsonNode.get("fuel5").asText();
                    if (0 == communs5.length()) {
                        communs5 = str;
                    }
                    tempJson.put("communs5", communs5);
                    tempJson.put("ratetime", jsonNode.get("fuelDate").asText().substring(subIndex));
                    rateList.add(tempJson);
                }
                cnJson.put("rateList", rateList);
                upsList.add(cnJson);
            }

            if (ups.has("cn")) {
                JSONObject cnJson = new JSONObject();
                cnJson.put("regionInfo", "中国");
                JSONArray rateList = new JSONArray();
                for (int i = 0; i < 2; i++) {
                    JsonNode jsonNode = ups.get("cn").get(i);
                    JSONObject tempJson = new JSONObject();
                    tempJson.put("communs1", jsonNode.get("fuel1").asText());
                    tempJson.put("communs2", jsonNode.get("fuel1").asText());
                    tempJson.put("communs3", str);
                    tempJson.put("communs4", str);
                    tempJson.put("communs5", str);
                    tempJson.put("ratetime", jsonNode.get("fuelDate").asText().substring(subIndex));
                    rateList.add(tempJson);
                }
                cnJson.put("rateList", rateList);
                upsList.add(cnJson);

                cnJson = new JSONObject();
                cnJson.put("regionInfo", "香港");
                cnJson.put("rateList", rateList);
                upsList.add(cnJson);
            }

            upsJson.put("rateTypeINfo", "UPS");
            upsJson.put("regionList", upsList);
            backArr.add(upsJson);
        }


        JSONObject fedexJson = new JSONObject();
        if (!fedex.isEmpty()) {
            JSONArray fedexList = new JSONArray();

            if (fedex.has("us")) {
                JSONObject cnJson = new JSONObject();
                cnJson.put("regionInfo", "美国");
                JSONArray rateList = new JSONArray();
                for (int i = 0; i < 2; i++) {
                    JsonNode jsonNode = fedex.get("us").get(i);
                    JSONObject tempJson = new JSONObject();
                    tempJson.put("communs1", jsonNode.get("fuel1").asText());
                    tempJson.put("communs2", jsonNode.get("fuel2").asText());
                    tempJson.put("communs3", jsonNode.get("fuel3").asText());
                    tempJson.put("communs4", jsonNode.get("fuel4").asText());
                    tempJson.put("communs5", str);
                    tempJson.put("ratetime", jsonNode.get("fuelDate").asText().substring(subIndex));
                    rateList.add(tempJson);
                }
                cnJson.put("rateList", rateList);
                fedexList.add(cnJson);
            }

            if (fedex.has("cn")) {
                JSONObject cnJson = new JSONObject();
                cnJson.put("regionInfo", "中国");
                JSONArray rateList = new JSONArray();
                for (int i = 0; i < 2; i++) {
                    JsonNode jsonNode = fedex.get("cn").get(i);
                    JSONObject tempJson = new JSONObject();
                    tempJson.put("communs1", jsonNode.get("fuel1").asText());
                    tempJson.put("communs2", jsonNode.get("fuel1").asText());
                    tempJson.put("communs3", str);
                    tempJson.put("communs4", str);
                    tempJson.put("communs5", str);
                    tempJson.put("ratetime", jsonNode.get("fuelDate").asText().substring(subIndex));
                    rateList.add(tempJson);
                }
                cnJson.put("rateList", rateList);
                fedexList.add(cnJson);

                cnJson = new JSONObject();
                cnJson.put("regionInfo", "香港");
                cnJson.put("rateList", rateList);
                fedexList.add(cnJson);
            }

            fedexJson.put("rateTypeINfo", "FED");
            fedexJson.put("regionList", fedexList);
            backArr.add(fedexJson);
        }

        JSONObject DhlJson = new JSONObject();
        if (!dhl.isEmpty()) {
            JSONArray dhlList = new JSONArray();

            if (dhl.has("us")) {
                JSONObject cnJson = new JSONObject();
                cnJson.put("regionInfo", "美国");
                JSONArray rateList = new JSONArray();
                for (int i = 0; i < 2; i++) {
                    JsonNode jsonNode = dhl.get("us").get(i);
                    JSONObject tempJson = new JSONObject();
                    tempJson.put("communs1", jsonNode.get("fuel1").asText());
                    tempJson.put("communs2", jsonNode.get("fuel2").asText());
                    tempJson.put("communs3", str);
                    tempJson.put("communs4", str);
                    tempJson.put("communs5", str);
                    tempJson.put("ratetime", jsonNode.get("fuelDate").asText().substring(subIndex));
                    rateList.add(tempJson);
                }
                cnJson.put("rateList", rateList);
                dhlList.add(cnJson);
            }

            if (dhl.has("cn")) {
                JSONObject cnJson = new JSONObject();
                cnJson.put("regionInfo", "中国");
                JSONArray rateList = new JSONArray();
                for (int i = 0; i < 2; i++) {
                    JsonNode jsonNode = dhl.get("cn").get(i);
                    JSONObject tempJson = new JSONObject();
                    tempJson.put("communs1", jsonNode.get("fuel1").asText());
                    tempJson.put("communs2", jsonNode.get("fuel1").asText());
                    tempJson.put("communs3", str);
                    tempJson.put("communs4", str);
                    tempJson.put("communs5", str);
                    tempJson.put("ratetime", jsonNode.get("fuelDate").asText().substring(subIndex));
                    rateList.add(tempJson);
                }
                cnJson.put("rateList", rateList);
                dhlList.add(cnJson);

                cnJson = new JSONObject();
                cnJson.put("regionInfo", "香港");
                cnJson.put("rateList", rateList);
                dhlList.add(cnJson);
            }

            DhlJson.put("rateTypeINfo", "DHL");
            DhlJson.put("regionList", dhlList);
            backArr.add(DhlJson);
        }
        return backArr;


//        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
//        String fuel = valueOperations.get("all--fuel");
//        if (null != fuel) {
//            return JsonUtil.toTree(fuel);
//        }
//        HttpResponse<String> response = Unirest.get("http://www.shipsoeasy.com:8401/upsrate/selectRate")
//                .asString();
//        String backStr = response.getBody();
//        JsonNode backJson = JsonUtil.toTree(backStr);
//        JsonNode regionList = backJson.get("data").get(1).get("regionList").get(1).get("rateList");
//        ObjectNode nowNode = (ObjectNode) regionList.get(0);
//        ObjectNode beforeNode = (ObjectNode) regionList.get(1);
//        //给UPS添加国际地面出口进口附加费
//        JsonNode ups = getCommonFuel("ups");
//        nowNode.put("communs5", ups.get("us").get(0).get("fuel5").asText());
//        beforeNode.put("communs5", ups.get("us").get(1).get("fuel5").asText());
//        //获取UPS的校验日期，第三方的日期格式为2024/09/09
//        long difference = getCacheTime("all", nowNode.get("ratetime").asText());
//        log.info("获取第三方燃油成功，添加缓存时间为：{}！", difference);
//        valueOperations.set("all--fuel", backJson.toString(), difference, TimeUnit.SECONDS);
//        return backJson;
    }

    public static void main(String[] args) {
        String[] strArr = new String[]{"2024/10/14", "2024/10/21", "2024/10/21"};
        List<Date> list = new ArrayList<>();
        //先把所有的日期转成Date
        for (String str : strArr) {
            try {
                list.add(thirdDateFormat.parse(str));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        //取最小的日期
        Date minDate = Collections.min(list);
        System.out.println(minDate);

        long difference = new FuelImpl().getCacheTime("ups", minDate);
        System.out.println(difference);
    }
}
