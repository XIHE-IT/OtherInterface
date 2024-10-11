package com.xihe.services.impl;

import com.xihe.services.CustomsService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * 海关编码查询 服务实现类
 *
 * @author gzy
 * @since 2024-09-03 17:29:36
 */
@Service
public class CustomsServiceImpl implements CustomsService {

    @Value("${other.customs.url}")
    private String customsUrl;

    @Override
    public String getCustoms(String keywords) {

        try{
            Document doc = Jsoup.connect(customsUrl + "/search?Keywords=" + keywords + "&displayenname=true").get();
            String result = doc.getElementsByClass("result").html();
            return result;
        }catch (Exception e){
            throw new RuntimeException("海关编码查询异常：{}", e);
        }

    }

    @Override
    public String getAllCustoms(String keywords) {

        try{
            // 获取第一页的内容作为起点
            Document doc = getPageContent(keywords,1);

            // 从起点页面获取总页数
            int maxPageSize = getTotalPages(doc);

            // 爬取所有页面的内容
            for (int i = 1; i <= maxPageSize; i++) {
                Document page = getPageContent(keywords,i);
                System.out.println(page.getElementsByClass("result").html());
            }
            return "";
        }catch (Exception e){
            throw new RuntimeException("海关编码详情查询异常：{}", e);
        }

    }

    @Override
    public String getCustomsDetails(String productCode) {

        try{
            Document doc = Jsoup.connect(customsUrl + "/Code/" + productCode + ".html").get();
            return doc.getElementById("code-info").html();
        }catch (Exception e){
            throw new RuntimeException("海关编码查询异常：{}", e);
        }

    }

    /**
     * 获取最大页码
     *
     * @param keywords 关键字
     * @param i        页码
     * @author yangL
     * @since 2024/10/10
     */
    private Document getPageContent(String keywords, int i) throws IOException {

        return Jsoup.connect(customsUrl + "/search/" +i+ "?Keywords=" + keywords + "&displayenname=true").get();

    }

    /**
     * 获取最大页码
     *
     * @param doc 文档
     * @author yangL
     * @since 2024/10/10
     */
    private int getTotalPages(Document doc) {

        return Integer.parseInt(doc.select("div.pagination a").last().text());

    }

}
