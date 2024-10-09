package com.xihe.services.impl;

import com.xihe.entity.CheckHome;
import com.xihe.entity.Fuel;
import com.xihe.services.FuelServices;
import com.xihe.util.DateUtil;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * DHL获取燃油
 *
 * @Author gzy
 * @Date 2024/8/14 10:58
 */
@Service
public class DhlFuel implements FuelServices {
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMMM yyyy", Locale.US);
    private static final SimpleDateFormat backDate = new SimpleDateFormat("yyyy/MM/dd");

    @Override
    public Map<String, String> getCommonFuel() {
        Map<String, String> map = new HashMap<>();
//        HttpResponse<String> response = Unirest.get("https://mydhlplus.dhl.com/cn/zh/ship/surcharges.html").asString();
//        map.put("cn", response.getBody());
        HttpResponse<String> response = Unirest.get("https://mydhl.express.dhl/us/en/ship/surcharges.html").asString();
        map.put("us", response.getBody());
        response = Unirest.get("https://mydhl.express.dhl/hk/zh/ship/surcharges.html").asString();
        map.put("hk", response.getBody());
        return map;
    }

    @Override
    public Map<String, List<Fuel>> resolveFuel(Map<String, String> fuelMap) {
        Map<String, List<Fuel>> resultMap = new HashMap<>();
        //美国燃油列表
        String backStr = fuelMap.get("us");
        Document document = Jsoup.parse(backStr);
        Elements dhlTable = document.getElementsByClass("dhl-tables-component__main-body");
        Element element = dhlTable.get(dhlTable.size() - 2);
        Elements trArr = element.getElementsByTag("tr");
        List<Fuel> list = new ArrayList<>();
        for (Element temp : trArr) {
            Elements tdArr = temp.getElementsByTag("td");
            Fuel fuel = new Fuel();
            String fuelDate = tdArr.get(0).text();
            try {
                Date tempDate = simpleDateFormat.parse(fuelDate);
                fuelDate = backDate.format(tempDate);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            fuel.setFuelDate(fuelDate);
            fuel.setFuel1(tdArr.get(1).text());
            fuel.setFuel2(tdArr.get(2).text());
            list.add(fuel);
        }
        resultMap.put("us", list);

        //中国燃油列表
        backStr = fuelMap.get("hk");
        document = Jsoup.parse(backStr);
        dhlTable = document.getElementsByClass("dhl-tables-component__main-body");
        element = dhlTable.get(dhlTable.size() - 2);
        trArr = element.getElementsByTag("tr");
        list = new ArrayList<>();
        for (Element temp : trArr) {
            Elements tdArr = temp.getElementsByTag("td");
            Fuel fuel = new Fuel();
            String fuelDate = tdArr.get(0).text();
            int index = fuelDate.indexOf("月");
            if (index != -1) {
                String month = fuelDate.substring(0, index + 1);
                fuelDate = fuelDate.substring(index + 2) + "/" + DateUtil.chineseMonthToNumber(month) + "/01";
            }
            fuel.setFuelDate(fuelDate);
            fuel.setFuel1(tdArr.get(1).text());
            fuel.setFuel2(tdArr.get(1).text());
            list.add(fuel);
        }
        resultMap.put("cn", list);

        //香港燃油列表
//        backStr = fuelMap.get("hk");
//        document = Jsoup.parse(backStr);
//        dhlTable = document.getElementsByClass("dhl-tables-component__main-body");
//        element = dhlTable.get(dhlTable.size() - 2);
//        trArr = element.getElementsByTag("tr");
//        list = new ArrayList<>();
//        for (Element temp : trArr) {
//            Elements tdArr = temp.getElementsByTag("td");
//            Fuel fuel = new Fuel();
//            fuel.setFuelDate(tdArr.get(0).text());
//            fuel.setFuel1(tdArr.get(1).text());
//            fuel.setFuel2(tdArr.get(1).text());
//            list.add(fuel);
//        }
//        resultMap.put("hk", list);
        return resultMap;
    }

    @Override
    public Integer getCheckHome(CheckHome checkHome) {
        return 0;
    }
}
