package com.xihe.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.xihe.entity.CheckHome;
import com.xihe.entity.Fuel;
import com.xihe.services.FuelServices;
import com.xihe.util.JsonUtil;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TNT获取燃油
 *
 * @Author gzy
 * @Date 2024/8/14 10:58
 */
@Service
public class TntFuel implements FuelServices {
    @Override
    public Map<String, String> getCommonFuel() {
        Map<String, String> map = new HashMap<>();
        HttpResponse<String> response = Unirest.get("https://www.tnt.com/express/getDynamicData.apac.json").asString();
        map.put("cn", response.getBody());
        return map;
    }

    @Override
    public Map<String, List<Fuel>> resolveFuel(Map<String, String> fuelMap) {
        Map<String, List<Fuel>> resultMap = new HashMap<>();
        String backStr = fuelMap.get("cn");
        JsonNode jsonNode = JsonUtil.toTree(backStr);
        JsonNode tempNode = jsonNode.path("list");
        List<Fuel> list = new ArrayList<>();
        for (JsonNode temp : tempNode) {
            Fuel fuel = new Fuel();
            fuel.setFuelDate(temp.get("week").asText());
            fuel.setFuel1(temp.get("surcharge").asText());
            list.add(fuel);
        }
        resultMap.put("cn", list);
        return resultMap;
    }

    @Override
    public Integer getCheckHome(CheckHome checkHome) {
        return 0;
    }
}
