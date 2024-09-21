package com.xihe.services.impl;

import com.xihe.entity.CheckHome;
import com.xihe.entity.Fuel;
import com.xihe.services.FuelServices;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DHL获取燃油
 *
 * @Author gzy
 * @Date 2024/8/14 10:58
 */
@Service
public class DhlFuel implements FuelServices {

    @Override
    public Map<String, String> getCommonFuel() {
        Map<String, String> map = new HashMap<>();
        HttpResponse<String> response = Unirest.get("https://mydhlplus.dhl.com/cn/zh/ship/surcharges.html").asString();
        map.put("cn", response.getBody());
        return map;
    }

    @Override
    public Map<String, List<Fuel>> resolveFuel(Map<String, String> fuelMap) {
        Map<String, List<Fuel>> resultMap = new HashMap<>();
        String backStr = fuelMap.get("cn");
        Document document = Jsoup.parse(backStr);
        Elements dhlTable = document.getElementsByClass("dhl-tables-component__main-body");
        Element element = dhlTable.get(dhlTable.size() - 2);
        Elements trArr = element.getElementsByTag("tr");
        List<Fuel> list = new ArrayList<>();
        for (Element temp : trArr) {
            Elements tdArr = temp.getElementsByTag("td");
            Fuel fuel = new Fuel();
            fuel.setFuelDate(tdArr.get(0).text());
            fuel.setFuel1(tdArr.get(1).text());
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
