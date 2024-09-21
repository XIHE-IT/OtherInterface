package com.xihe.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xihe.entity.CheckHome;
import com.xihe.entity.Fuel;
import com.xihe.services.FuelServices;
import com.xihe.util.BusinessUtil;
import com.xihe.util.JsonUtil;
import jakarta.annotation.Resource;
import jakarta.xml.bind.DatatypeConverter;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UPS获取燃油
 *
 * @Author gzy
 * @Date 2024/8/14 10:58
 */
@Slf4j
@Service
public class UpsFuel implements FuelServices {
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Resource
    BusinessUtil businessUtil;

    private static final String upsUrl = "https://wwwcie.ups.com";
    //测试密钥
//    private static final String key = "JHy9SaB3IR3qLLoHvi30DNsThUuyIvlfnPbjDEAagbViA2Zs";
//    private static final String password = "RAYMY8iUXAqGmUixj6qw5EVZoy2RGPxDi3lccp79TgCBISfAS9Cmrg3X6ujNddXJ";

    //生产密钥
    private static final String key = "K62GNkpdnwAfSMpNCRw4zkvPSNWDG5337g43Ac9oNutfFSt9";
    private static final String password = "G1MIMSzF66p3AvAp2O5rdAAWtR5cPiWoQUiAnPWWmx4KvjhPDUKVHh4O8jXd8wjH";

    @Override
    public Map<String, String> getCommonFuel() {
        Map<String, String> map = new HashMap<>();
        HttpResponse<String> response = Unirest.get("https://www.ups.com/assets/resources/fuel-surcharge/cn.json").asString();
        map.put("cn", response.getBody());
        response = Unirest.get("https://www.ups.com/assets/resources/fuel-surcharge/us.json").asString();
        map.put("us", response.getBody());
        response = Unirest.get("https://www.ups.com/assets/resources/fuel-surcharge/hk.json").asString();
        map.put("hk", response.getBody());
        return map;
    }

    @Override
    public Map<String, List<Fuel>> resolveFuel(Map<String, String> fuelMap) {
        Map<String, List<Fuel>> resultMap = new HashMap<>();
        //美国燃油列表
        String backStr = fuelMap.get("us");
        JsonNode jsonNode = JsonUtil.toTree(backStr);
        JsonNode tempNode = jsonNode.path("FuelSurchargeResponse").path("SurchargeHistory_en").path("RevenueSurchargeHistory");
        List<Fuel> list = new ArrayList<>();
        for (JsonNode temp : tempNode) {
            Fuel fuel = new Fuel();
            fuel.setFuelDate(temp.get("Field1").asText());
            fuel.setFuel1(temp.get("Field2").asText());
            fuel.setFuel2(temp.get("Field3").asText());
            fuel.setFuel3(temp.get("Field4").asText());
            fuel.setFuel4(temp.get("Field5").asText());
            fuel.setFuel5(temp.get("Field6").asText());
            list.add(fuel);
        }
        resultMap.put("us", list);

        //中国燃油列表
        backStr = fuelMap.get("cn");
        jsonNode = JsonUtil.toTree(backStr);
        tempNode = jsonNode.path("FuelSurchargeResponse").path("SurchargeHistory_en").path("RevenueSurchargeHistory");
        list = new ArrayList<>();
        for (JsonNode temp : tempNode) {
            Fuel fuel = new Fuel();
            fuel.setFuelDate(temp.get("Field1").asText());
            fuel.setFuel1(temp.get("Field2").asText());
            list.add(fuel);
        }
        resultMap.put("cn", list);

        //香港燃油列表
        backStr = fuelMap.get("hk");
        jsonNode = JsonUtil.toTree(backStr);
        tempNode = jsonNode.path("FuelSurchargeResponse").path("SurchargeHistory_en").path("RevenueSurchargeHistory");
        list = new ArrayList<>();
        for (JsonNode temp : tempNode) {
            Fuel fuel = new Fuel();
            fuel.setFuelDate(temp.get("Field1").asText());
            fuel.setFuel1(temp.get("Field2").asText());
            list.add(fuel);
        }
        resultMap.put("hk", list);
        return resultMap;
    }

    /**
     * 发送请求获取token
     *
     * @param platformInterfaceItemMap 请求参数
     * @return java.lang.String
     * @author gzy
     * @date 2024/8/31 13:14
     */
    public static String requestGetUpsToken(Map<String, String> platformInterfaceItemMap) {
        String key = platformInterfaceItemMap.get("key").trim();
        String password = platformInterfaceItemMap.get("password").trim();
        HttpResponse<String> response = Unirest.post(upsUrl + "/security/v1/oauth/token")
                .header("Authorization", "Basic " + DatatypeConverter.printBase64Binary((key + ":" + password).getBytes(StandardCharsets.UTF_8)))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .field("grant_type", "client_credentials")
                .asString();
        return response.getBody();
    }

    /**
     * 获取token
     *
     * @param platformInterfaceItemMap 请求参数
     * @return java.lang.String
     * @author gzy
     * @date 2024/8/31 13:14
     */
    public String getUpsToken(Map<String, String> platformInterfaceItemMap) {
        String key = platformInterfaceItemMap.get("key");
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key + "--ups"))) {
            return valueOperations.get(key + "--ups");
        }
//        String serviceType = platformInterfaceItemMap.get("serviceType");//服务类型
//        if (StringUtils.isNotEmpty(serviceType)) {
        String result = requestGetUpsToken(platformInterfaceItemMap);
        log.info("Ups获取token返回结果为:{}", result);
        JsonNode tokenResultObject = JsonUtil.toTree(result);
        if (tokenResultObject.has("access_token")) {
            String token = tokenResultObject.get("access_token").asText();
            long expires_in = tokenResultObject.get("expires_in").asLong();//过期的秒数
            valueOperations.set(key + "--ups", token, expires_in, java.util.concurrent.TimeUnit.SECONDS);
            return token;
        } else if (tokenResultObject.has("response")) {
            throw new RuntimeException("获取Token接口返回异常：" + tokenResultObject.get("response").toString());
        }
//        }
        return null;
    }

    /**
     * AddressClassification.code，0 未分类，1 商业，2 住宅
     *
     * @param checkHome 请求参数
     * @return java.lang.String
     * @author gzy
     * @date 2024/8/31 14:46
     */
    @Override
    public Integer getCheckHome(CheckHome checkHome) {
        String tempKey = businessUtil.getCheckHomeKey(checkHome);
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
        int code = businessUtil.getCheckHome(checkHome.getType(), tempKey, hashOperations);
        if (-1 != code) {
            return code;
        }
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("key", key);
        paramMap.put("password", password);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        if (StringUtils.isNotEmpty(checkHome.getConsigneeName())) {
            node.put("ConsigneeName", checkHome.getConsigneeName());
        }
        if (StringUtils.isNotEmpty(checkHome.getBuildingName())) {
            node.put("BuildingName", checkHome.getBuildingName());
        }
        ArrayNode listNode = JsonNodeFactory.instance.arrayNode();
        for (String address : checkHome.getAddressLine()) {
            listNode.add(address);
        }
        node.set("AddressLine", listNode);
        if (StringUtils.isNotEmpty(checkHome.getRegion())) {
            node.put("Region", checkHome.getRegion());
        }
        if (StringUtils.isNotEmpty(checkHome.getPoliticalDivision1())) {
            node.put("PoliticalDivision1", checkHome.getPoliticalDivision1());
        }
        if (StringUtils.isNotEmpty(checkHome.getPoliticalDivision2())) {
            node.put("PoliticalDivision2", checkHome.getPoliticalDivision2());
        }
        if (StringUtils.isNotEmpty(checkHome.getPostcodePrimaryLow())) {
            node.put("PostcodePrimaryLow", checkHome.getPostcodePrimaryLow());
        }
        if (StringUtils.isNotEmpty(checkHome.getPostcodeExtendedLow())) {
            node.put("PostcodeExtendedLow", checkHome.getPostcodeExtendedLow());
        }
        if (StringUtils.isNotEmpty(checkHome.getUrbanization())) {
            node.put("Urbanization", checkHome.getUrbanization());
        }
        if (StringUtils.isNotEmpty(checkHome.getCountryCode())) {
            node.put("CountryCode", checkHome.getCountryCode());
        }

        String paramBody = "{\"XAVRequest\":{\"AddressKeyFormat\":" + JsonUtil.toJson(node) + "}}";
        //获取token
        String token = getUpsToken(paramMap);
        HttpResponse<String> response = Unirest.post("https://onlinetools.ups.com/api/addressvalidation/v2/3")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token).body(paramBody)
                .asString();
        String backStr = response.getBody();
        log.info("Ups私宅校验接口返回结果为:{}", backStr);
        JsonNode jsonNode = JsonUtil.toTree(backStr);
        if (jsonNode.has("response")) {
//            throw new RuntimeException(jsonNode.get("response").get("errors").get(0).get("message").asText());
            throw new RuntimeException("Ups校验私宅错误，输入的地址有误，请检查后重新输入！");
        }
        String backCode = jsonNode.get("XAVResponse").get("AddressClassification").get("Code").asText();
        hashOperations.put("ups-check-home", tempKey, backCode);
        return Integer.parseInt(backCode);
    }
}
