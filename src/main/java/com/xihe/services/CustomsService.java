package com.xihe.services;

/**
 * 海关编码查询
 *
 * @author  yangL
 * @since 2024/10/9
 */
public interface CustomsService {

    /**
     * 根据关键字查询海关编码
     *
     * @param keywords 关键字
     * @return java.lang.String
     * @author yangL
     * @since 2024/10/9
     */
    String getCustoms(String keywords);

    /**
     * 根据关键字查询所有分页海关编码
     *
     * @param keywords 关键字
     * @return java.lang.String
     * @author yangL
     * @since 2024/10/9
     */
    String getAllCustoms(String keywords);

    /**
     * 查询海关编码详情
     *
     * @param productCode 商品编码
     * @return java.lang.String
     * @author yangL
     * @since 2024/10/9
     */
    String getCustomsDetails(String productCode);

}
