package com.xihe.services.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xihe.services.CityPostService;
import com.xihe.util.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 美国国家信息表(用作城市校验地址转换)(BasicUsCountryInfo)表服务实现类
 *
 * @author gzy
 * @since 2024-09-03 17:29:36
 */
@DS("k5")
@Slf4j
@Service("basicUsCountryInfoService")
public class CityPostServiceImpl implements CityPostService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 查询所有省/州
     *
     * @return java.util.List<java.lang.String>
     * @author gzy
     * @date 2024/9/4 14:05
     */
    private List<String> getAllProvinceList() {
        List<String> backList = new ArrayList<>();
        List<Map<String, Object>> list = jdbcTemplate.queryForList("select distinct province from basic_us_country_info");
        for (Map<String, Object> tempMap : list) {
            backList.add(tempMap.get("province").toString().trim());
        }

        return backList;
    }

    /**
     * 查询所有省/州
     *
     * @return java.util.List<java.lang.String>
     * @author gzy
     * @date 2024/9/4 14:05
     */
    private List<String> provinceGetTownname(String country, String province) {
        List<String> backList = new ArrayList<>();

        String sql = "select distinct townname from basic_us_country_info where country='" + country + "' and province='" + province + "'";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        for (Map<String, Object> tempMap : list) {
            backList.add(tempMap.get("townname").toString().trim());
        }

        return backList;
    }

    @Override
    public String getAllProvince() {
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey("all--province"))) {
            return valueOperations.get("all--province");
        }
        List<String> list = getAllProvinceList();
        StringBuilder sBuilder = new StringBuilder();
        for (String str : list) {
            sBuilder.append(str).append("|");
        }
        if (!sBuilder.isEmpty()) {
            sBuilder.delete(sBuilder.length() - 1, sBuilder.length());
        }
        valueOperations.set("all--province", sBuilder.toString());
        return sBuilder.toString();
    }

    @Override
    public ObjectNode judgeProvinceExists(String address) {
        String provinceStr = getAllProvince();
        log.info("获取到的所有省/州信息为:{}", provinceStr);
        Pattern pattern = Pattern.compile(provinceStr);
        Matcher matcher = pattern.matcher(address);
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("address", address);
        boolean judge = false;
        while (matcher.find()) {
            String str = matcher.group();
            int start = matcher.start();
            int end = matcher.end();
            String beforeStr = "";
            String endStr = "";
            if (-1 != start) {
                beforeStr = address.substring(start - 1, start);
            }
            if (end != address.length()) {
                endStr = address.substring(end, end + 1);
            }
            log.info("地址:{},截取到的省/州信息为:{},获取的前置匹配为:{},后置匹配为:{}", address, str, beforeStr, endStr);
            String province = "";
            if ((" ".equals(beforeStr) || beforeStr.isEmpty()) && (" ".equals(endStr) || endStr.isEmpty())) {
                province = str;
                //地址栏切割两段
                ArrayNode listNode = JsonNodeFactory.instance.arrayNode();
                listNode.add(address.substring(0, start).trim());
                listNode.add(address.substring(end).trim());
                jsonNode.set("addressArr", listNode);
                judge = true;
            }
            jsonNode.put("province", province);
            if (judge) {
                return jsonNode;
            }
        }

        if (!jsonNode.has("province")) {
            jsonNode.put("province", "");
        }

        return jsonNode;
    }

    @Override
    public String provinceGetTownname(String name) {
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
        if (Boolean.TRUE.equals(hashOperations.hasKey("all--province--townname", name))) {
            return hashOperations.get("all--province--townname", name);
        }
        List<String> list = provinceGetTownname("US", name);
        // 使用自定义Comparator进行排序
        list.sort((s1, s2) -> {
            // 首先比较字符串长度
            int lenDiff = Integer.compare(s2.length(), s1.length());
            if (lenDiff != 0) {
                return lenDiff;
            }
            // 如果长度相同，则按字典顺序比较
            return s2.compareTo(s1);
        });
        StringBuilder sBuilder = new StringBuilder();
        for (String str : list) {
            sBuilder.append(str).append("|");
        }
        if (!sBuilder.isEmpty()) {
            sBuilder.delete(sBuilder.length() - 1, sBuilder.length());
        }
        hashOperations.put("all--province--townname", name, sBuilder.toString());
        return sBuilder.toString();
    }

    @Override
    public void judgeTownnameExists(ObjectNode jsonNode) {
        String province = jsonNode.get("province").asText();
        if (province.isEmpty()) {
            jsonNode.put("townname", "");
            return;
        }
        //获取到province省对应的所有城市
        String townnameStr = provinceGetTownname(province);
        log.info("根据省/州:{},获取到的所有城市信息为:{}", province, townnameStr);
        Pattern pattern = Pattern.compile(townnameStr);
        List<String> addressList = new ArrayList<>();
        if (jsonNode.has("addressArr")) {   //前面省/州分段后继续处理城市
            ArrayNode arrayNode = (ArrayNode) jsonNode.get("addressArr");
            boolean judge = false;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode tempAddress = arrayNode.get(i);
                String address = tempAddress.asText();
                Matcher matcher = pattern.matcher(address);
                while (matcher.find()) {
                    String str = matcher.group();
                    int start = matcher.start();
                    int end = matcher.end();
                    String beforeStr = "";
                    String endStr = "";
                    if (start > 0) {
                        beforeStr = address.substring(start - 1, start);
                    }
                    if (end != address.length()) {
                        endStr = address.substring(end, end + 1);
                    }
                    log.info("地址:{},截取到的城市信息为:{},获取的前置匹配为:{},后置匹配为:{}", address, str, beforeStr, endStr);
                    String townname;
                    if ((" ".equals(beforeStr) || beforeStr.isEmpty()) && (" ".equals(endStr) || endStr.isEmpty())) {
                        townname = str;
                        //再次地址切割两段
                        String address1 = address.substring(0, start).trim();
                        String address2 = address.substring(end).trim();
                        if (!address1.isEmpty()) {
                            addressList.add(address1);
                        }
                        if (!address2.isEmpty()) {
                            addressList.add(address2);
                        }
                        judge = true;
                        jsonNode.put("townname", townname);
                    }
                }
                if (!judge) {
                    addressList.add(address);
                } else {
                    //表明已经找到,所以添加后续的地址给到addressList并跳出循环
                    for (int j = i + 1; j < arrayNode.size(); j++) {
                        addressList.add(arrayNode.get(j).asText());
                    }
                    break;
                }
            }
        }
        //替换掉地址组
        if (!addressList.isEmpty()) {
            ArrayNode listNode = JsonNodeFactory.instance.arrayNode();
            for (String address : addressList) {
                listNode.add(address);
            }
            jsonNode.set("addressArr", listNode);
        }

        if (!jsonNode.has("townname")) {
            jsonNode.put("townname", "");
        }
        //说明前面省/州没有找到直接不找城市了按原路返回
    }

    @Override
    public void judgePostCodeExists(ObjectNode jsonNode) {
        //校验连贯数字
        String POSTCODE_REGEX = "([\\d]){5,}";
        Pattern pattern = Pattern.compile(POSTCODE_REGEX);
        List<String> addressList = new ArrayList<>();
        if (jsonNode.has("addressArr")) {   //前面分段完毕后校验邮编
            ArrayNode arrayNode = (ArrayNode) jsonNode.get("addressArr");
            for (JsonNode tempAddress : arrayNode) {
                String address = tempAddress.asText();
                Matcher matcher = pattern.matcher(address);
                boolean judge = false;
                while (matcher.find()) {
                    String str = matcher.group();
                    int start = matcher.start();
                    int end = matcher.end();
                    String beforeStr = "";
                    String endStr = "";
                    if (start > 0) {
                        beforeStr = address.substring(start - 1, start);
                    }
                    if (end != address.length()) {
                        endStr = address.substring(end, end + 1);
                    }
                    log.info("地址组:{},截取到的邮编信息为:{},获取的前置匹配为:{},后置匹配为:{}", address, str, beforeStr, endStr);
                    String postcode = "";
                    if ((" ".equals(beforeStr) || beforeStr.isEmpty()) && (" ".equals(endStr) || endStr.isEmpty())) {
                        postcode = str;
                        //再次地址切割两段
                        String address1 = address.substring(0, start).trim();
                        String address2 = address.substring(end).trim();
                        if (!address1.isEmpty()) {
                            addressList.add(address1);
                        }
                        if (!address2.isEmpty()) {
                            addressList.add(address2);
                        }
                    }
                    judge = true;
                    jsonNode.put("postcode", postcode);
                }
                if (!judge) {
                    addressList.add(address);
                }
            }
        } else {
            String address = jsonNode.get("address").asText();
            Matcher matcher = pattern.matcher(address);
            boolean judge = false;
            while (matcher.find()) {
                String str = matcher.group();
                int start = matcher.start();
                int end = matcher.end();
                String beforeStr = "";
                String endStr = "";
                if (start > 0) {
                    beforeStr = address.substring(start - 1, start);
                }
                if (end != address.length()) {
                    endStr = address.substring(end, end + 1);
                }
                log.info("地址:{},截取到的邮编信息为:{},获取的前置匹配为:{},后置匹配为:{}", address, str, beforeStr, endStr);
                String postcode = "";
                if ((" ".equals(beforeStr) || beforeStr.isEmpty()) && (" ".equals(endStr) || endStr.isEmpty())) {
                    postcode = str;
                    //再次地址切割两段
                    String address1 = address.substring(0, start).trim();
                    String address2 = address.substring(end).trim();
                    if (!address1.isEmpty()) {
                        addressList.add(address1);
                    }
                    if (!address2.isEmpty()) {
                        addressList.add(address2);
                    }
                }
                judge = true;
                jsonNode.put("postcode", postcode);
            }
            if (!judge) {
                addressList.add(address);
            }
        }
        if (!jsonNode.has("postcode")) {
            jsonNode.put("postcode", "");
        }
        //替换掉地址组
        if (!addressList.isEmpty()) {
            ArrayNode listNode = JsonNodeFactory.instance.arrayNode();
            for (String address : addressList) {
                listNode.add(address);
            }
            jsonNode.set("addressArr", listNode);
        }
        //说明前面省/州没有找到直接不找城市了按原路返回
    }

    @Override
    public ObjectNode interceptAddress(String address) {
        if (address.isEmpty()) {
            throw new RuntimeException("地址不能为空!");
        }
        //逗号全部变空格
        if (address.contains(",")) {
            address = address.replace(",", " ");
        }
        if (address.contains("，")) {
            address = address.replace("，", " ");
        }
        if (address.contains("\n")) {
            address = address.replace("\n", " ");
        }
        address = address.toUpperCase();
        ObjectNode objectNode = judgeProvinceExists(address);
        judgeTownnameExists(objectNode);
        judgePostCodeExists(objectNode);
        return objectNode;
    }

    @Override
    public List<String> getStatCodeList(String str) {
        List<String> statCodeList = new ArrayList<>();

        String sql = "select distinct basic_stat_code from basic_stat_country_info where basic_stat_code like '%" + str.toUpperCase() + "%'";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        for (Map<String, Object> tempMap : list) {
            statCodeList.add(tempMap.get("basic_stat_code").toString().trim());
        }

        return statCodeList;
    }

    @Override
    public List<String> getCountryCombinationInfoList(String statCode) {
        List<String> countryCombinationInfoList = new ArrayList<>();

        String sql = "select distinct basic_country_address,townname,province,postcode from basic_stat_country_info where basic_stat_code = '" + statCode + "'";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        for (Map<String, Object> tempMap : list) {
            String tempStr = JsonUtil.toJson(Map.ofEntries(
                    Map.entry("basic_country_address", tempMap.get("basic_country_address").toString().trim()),
                    Map.entry("townname", tempMap.get("townname").toString().trim()),
                    Map.entry("province", tempMap.get("province").toString().trim()),
                    Map.entry("postcode", tempMap.get("postcode").toString().trim())
            ));
            countryCombinationInfoList.add(tempStr);
        }

        return countryCombinationInfoList;
    }
}

