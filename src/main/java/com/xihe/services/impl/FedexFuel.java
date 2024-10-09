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
import com.xihe.util.DateUtil;
import com.xihe.util.JsonUtil;
import jakarta.annotation.Resource;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Fedex获取燃油
 *
 * @Author gzy
 * @Date 2024/8/14 10:58
 */
@Slf4j
@Service
public class FedexFuel implements FuelServices {
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Resource
    BusinessUtil businessUtil;
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM. dd, yyyy", Locale.US);
    private static final SimpleDateFormat backDate = new SimpleDateFormat("yyyy/MM/dd");

    //测试环境
//    private static final String fedexUrl = "https://apis-sandbox.fedex.com";
//    private static final String key = "l7201edf87e27941cda0e174fb130b2615";
//    private static final String password = "e430395269594aa1a0dc66b995db73ee";
    //生产环境
    private static final String fedexUrl = "https://apis.fedex.com";
    private static final String key = "l76e8d7831454b46c69863cf6264984f4a";
    private static final String password = "7ce0463ca49b4ad197f031f2a6af049e";

    @Override
    public Map<String, String> getCommonFuel() {
//        HttpResponse<String> response = Unirest.get("https://www.fedex.com/etc/services/dynamic.express_weekly.apac.dd%20MMMM,%20yyyy.zh_cn.false.false.13.jsonp")
//                .header("Cookie", "_abck=4547DD1DFAE2D470FAB16B8BF72B4AF8~-1~YAAQC9gjF6bkDU6RAQAA4Se0Tgxb5WLbQtt9Nx4iaNs0iw6XqkFIK4crhSLjBlL/Y0/cjJZ6vv598Mjbp34QPFmskMiAZQkC7H6zCh0cAAr7B6K/OSRdgX4JhdjmRTp1PqF4Oj86i/fAnQXxlkloPDIrjQb93WAQFQCDSTgCPr1L3ujSwIyj2XTc0QASXaxOM5PSyclfU8I+jjO6tjMfUe0YwJ9H8JM/B15mNX8v0Ii4FnqpxXuoEM9MvuVRN8UK69pRD141/QlybyoLNfDcVM3pw4nc2RfNS9UIXAXAgvdCtRKT3HtsiFYFksJKmmTxlV0pcEpyAb6BT52JJzm7JZX9URwKHPt1l+iHBvf2n6TrFTfsP6rRcA==~-1~-1~-1; aemserver=PROD-P-dotcom; ak_bmsc=E863B153287C205B45CB86237B670C46~000000000000000000000000000000~YAAQC9gjF6fkDU6RAQAA4Se0ThgRygFIYfaKsefTRFhYRWvumEnNOavBCP8Z1U2KrpcOojn0PuLUGaJx1tUdcHMwFIXNsWQIVCsLxKWsWx58K4/Mv4maI6RZr4iyv4yPWhqfydgLdNYrdvG+9oAW8wgrjsfMHB8LCqx4SpLoZcMoYRDlQINiHvhAQWaBR4QvEi5tqsz29nBJ6mvSv1SgouVvXPf9OslMWVMDlFpVg+wt/JDlgbKTfbk7/TttUra3CgqHYkXKv9eZ/nVbf3Y630rTTUPCu7ZGblelVm5iSGN6iRrGx7twtw8IIzWyX99LCSqHEtMnSk+/Yn4JseCi/5RCHXdI/CuovXqTPf0LdmpK+u6rjy69kXZ59Q==; bm_sv=FC765DB8FC9AE928DFB69279C6C8390E~YAAQC9gjF8PnD06RAQAAgej9Thh0GRwhXbJzHoq93UHM+p6w/d1nKvJ2kBo8gd5CUInj7pdefjGetowqRxPFc+qJ4OS8A5ED5KjGgO2nx43yIkkoxHT5jnyIdVG+oFbxa/YyA8Vuzi/JaLggGxXM96mWY2AlM5aCCQHbr4ZnNhIihSltAxccdveR1RfTOvuP7XCxuM5TmsTofjTNWNfW8dKetxJuj48U7tcuSiQTvszVValmULqv8FfFTBSkVCk=~1; bm_sz=FA59B060593ECB0DE41DD2F147E8EAE8~YAAQC9gjF6jkDU6RAQAA4Se0ThjIDTbP+ylufkChnh400dH4et7L9siSO3aIhI3U2XmouGvgBAmKfLY20cuURFYrteGmjtqNDcng51ceCNBlQdggsSRDieTPzYBjmPfPLu0QyqThFGa4dEeoj8LCyH0QL5RAzp/9ZBDFz45pWStRjYpDkfFKLDe+QwQl2HwveOSo4HOBxbi6wEFfqHHi8DOHU7E3fHTiqTvdbbVSTkJ+PlrJOFD8ooGgtSIimn/d2owJBR9G08W4FhvZoujVGNvV1ht9pPTu2Vn2jMkCxkSV31UOKPiAuLfvY1UaRjISBzhkCXmCn2etgRF9RzrFSryXoNb+rp4JqF4gyA==~4469559~3224389; fdx_cbid=30204579321723602315024420286801; isMobile=false; isTablet=false; isWireless=false; siteDC=edc; xacc=HK; Rbt=f0; fdx_bman=e673c3e2229786035e500378abfad3ec")
//                .asString();
//        return response.getBody();

        Map<String, String> map = new HashMap<>();
//        HttpResponse<String> response = Unirest.get("http://www.xinexpress.com/Fuel-FedEx.html")
//                .asString();
//        map.put("cn", response.getBody());
        WebDriver driver = new ChromeDriver();
        driver.get("https://www.fedex.com/zh-cn/shipping/surcharges.html");
        map.put("cn", driver.getPageSource());
        driver.get("https://www.fedex.com/en-us/shipping/fuel-surcharge.html");
        map.put("us", driver.getPageSource());
        driver.quit();
        return map;
    }

    @Override
    public Map<String, List<Fuel>> resolveFuel(Map<String, String> fuelMap) {
        Map<String, List<Fuel>> resultMap = new HashMap<>();
//        String backStr = fuelMap.get("cn");
//        Document document = Jsoup.parse(backStr);
//        Elements dhlTable = document.getElementsByClass("Newslist");
//        Elements trArr = dhlTable.get(0).getElementsByTag("img");
//        List<Fuel> list = new ArrayList<>();
//        for (int i = 0; i < 5; i++) {
//            Element temp = trArr.get(i);
//            String title = temp.attr("title");
//            int index = title.indexOf("起");
//            String tempDate = title.substring(0, index);
//            index = title.indexOf("附加费是");
//            String tempFuel = title.substring(index + 4);
//            Fuel fuel = new Fuel();
//            fuel.setFuelDate(tempDate);
//            fuel.setFuel1(tempFuel);
//            list.add(fuel);
//        }
//        resultMap.put("cn", list);

        //中国fedex
        String backStr = fuelMap.get("cn");
        Document document = Jsoup.parse(backStr);
        Elements fedexTable = document.getElementsByClass("fxg-fsc__table");
        if(!fedexTable.isEmpty()){
            Element table = fedexTable.get(0);
            Elements trArr = table.getElementsByTag("tr");
            List<Fuel> list = new ArrayList<>();
            for (int i = 1; i < 5; i++) {
                Element temp = trArr.get(i);
                Elements tdArr = temp.getElementsByTag("td");
                Fuel fuel = new Fuel();
                String date = tdArr.get(0).text();
                int index = date.indexOf("-");
                if (index != -1) {
                    date = date.substring(0, index - 1);
                }
                //从中取出带月字
                String month = date.substring(3, 5);
                date = date.substring(7) + "/" + DateUtil.chineseMonthToNumber(month) + "/" + date.substring(0, 2);
                fuel.setFuelDate(date);
                fuel.setFuel1(tdArr.get(2).text());
                list.add(fuel);
            }
            resultMap.put("cn", list);
        }

        //美国fedex
        backStr = fuelMap.get("us");
        document = Jsoup.parse(backStr);
        fedexTable = document.getElementsByClass("fxg-table--striped-row");
        if(!fedexTable.isEmpty()){
            Element table = fedexTable.get(0);
            Elements trArr = table.getElementsByTag("tr");
            List<Fuel> list = new ArrayList<>();
            for (int i = 2; i < trArr.size(); i++) {
                Element temp = trArr.get(i);
                Elements tdArr = temp.getElementsByTag("td");
                Fuel fuel = new Fuel();
                String date = tdArr.get(0).text();
                int index = date.indexOf("–");
                if (index != -1) {
                    date = date.substring(0, index);
                }
                try {
                    //校验点前面的是几位数，若多了一位自动切割
                    index = date.indexOf(".");
                    if (index > 3) {
                        date = date.substring(0, index - 1) + ". " + date.substring(6);
                    }
                    Date tempDate = simpleDateFormat.parse(date);
                    date = backDate.format(tempDate);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                fuel.setFuelDate(date);
                fuel.setFuel1(tdArr.get(5).text());
                fuel.setFuel2(tdArr.get(6).text());
                fuel.setFuel3(tdArr.get(1).text());
                fuel.setFuel4(tdArr.get(3).text());
                list.add(fuel);
            }
            resultMap.put("us", list);
        }

        return resultMap;
    }

    /**
     * 获取Token
     *
     * @param platformInterfaceItemMap 请求参数
     * @return java.lang.String
     * @author gzy
     * @date 2024/8/31 15:15
     */
    public String getToken(Map<String, String> platformInterfaceItemMap) {
        String key = platformInterfaceItemMap.get("key").trim();
        String password = platformInterfaceItemMap.get("password").trim();
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key + "--fedex"))) {
            return valueOperations.get(key + "--fedex");
        }

        HttpResponse<String> response = Unirest.post(fedexUrl + "/oauth/token")
//                .header("Authorization", "Basic " + DatatypeConverter.printBase64Binary((key + ":" + password).getBytes(StandardCharsets.UTF_8)))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .field("grant_type", "client_credentials")
                .field("client_id", key)
                .field("client_secret", password)
                .asString();
        String resultString = response.getBody();
        log.info("Fedex获取token返回结果为:{}", resultString);
        JsonNode resultObject = JsonUtil.toTree(resultString);
        StringBuilder msg = new StringBuilder();
        if (resultObject.has("errors")) {
            JsonNode errorsArray = resultObject.get("errors");
            for (int i = 0; i < errorsArray.size(); i++) {
                JsonNode errorObject = errorsArray.get(i);
                msg.append(errorObject.get("message").asText());
            }
            throw new RuntimeException("接口响应异常：" + msg);
        }

        String token = resultObject.get("access_token").asText();
        long expires_in = resultObject.get("expires_in").asLong();
        valueOperations.set(key + "--fedex", token, expires_in, java.util.concurrent.TimeUnit.SECONDS);
        return token;
    }

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
//        if (StringUtils.isNotEmpty(checkHome.getConsigneeName())) {
//            node.put("ConsigneeName", checkHome.getConsigneeName());
//        }
//        if (StringUtils.isNotEmpty(checkHome.getBuildingName())) {
//            node.put("BuildingName", checkHome.getBuildingName());
//        }
        ArrayNode listNode = JsonNodeFactory.instance.arrayNode();
        for (String address : checkHome.getAddressLine()) {
            listNode.add(address);
        }
        node.set("streetLines", listNode);
//        if (StringUtils.isNotEmpty(checkHome.getRegion())) {
//            node.put("Region", checkHome.getRegion());
//        }
        if (StringUtils.isNotEmpty(checkHome.getPoliticalDivision1())) {
            node.put("stateOrProvinceCode", checkHome.getPoliticalDivision1());
        }
        if (StringUtils.isNotEmpty(checkHome.getPoliticalDivision2())) {
            node.put("city", checkHome.getPoliticalDivision2());
        }
        if (StringUtils.isNotEmpty(checkHome.getPostcodePrimaryLow())) {
            node.put("postalCode", checkHome.getPostcodePrimaryLow());
        }
//        if (StringUtils.isNotEmpty(checkHome.getPostcodeExtendedLow())) {
//            node.put("PostcodeExtendedLow", checkHome.getPostcodeExtendedLow());
//        }
//        if (StringUtils.isNotEmpty(checkHome.getUrbanization())) {
//            node.put("Urbanization", checkHome.getUrbanization());
//        }
        if (StringUtils.isNotEmpty(checkHome.getCountryCode())) {
            node.put("countryCode", checkHome.getCountryCode());
        }

        String paramBody = "{\"inEffectAsOfTimestamp\":\"2019-09-06\",\"validateAddressControlParameters\":{\"includeResolutionTokens\":true},\"addressesToValidate\":[{\"address\":" + JsonUtil.toJson(node) + ",\"clientReferenceId\":\"None\"}]}";
        //获取token
        String token = getToken(paramMap);
        HttpResponse<String> response = Unirest.post(fedexUrl + "/address/v1/addresses/resolve")
                .header("Content-Type", "application/json")
                .header("X-locale", "en_US")
                .header("Authorization", "Bearer " + token).body(paramBody)
                .asString();
        String backStr = response.getBody();
        log.info("Fedex私宅校验接口返回结果为:{}", backStr);
        if (!backStr.startsWith("{")) {
//            throw new RuntimeException("fedex校验私宅接口获取失败!");
            throw new RuntimeException("Fedex校验私宅错误，输入的地址有误，请检查后重新输入！");
        }
        JsonNode backJson = JsonUtil.toTree(backStr);
        if (backJson.has("errors")) {
            throw new RuntimeException(backJson.get("errors").get(0).get("message").asText());
        }
        JsonNode resolvedAddresses = backJson.get("output").get("resolvedAddresses");
        code = 0;
        if (resolvedAddresses.isArray()) {
            JsonNode tempNode = resolvedAddresses.get(0);
            String classification = tempNode.get("classification").asText();
            if ("BUSINESS".equals(classification)) {
                code = 1;
            } else if ("RESIDENTIAL".equals(classification)) {
                code = 2;
            } else if ("MIXED".equals(classification)) {
                code = 3;
            }
        }
        hashOperations.put("fedex-check-home", tempKey, String.valueOf(code));
        return code;
    }
}
