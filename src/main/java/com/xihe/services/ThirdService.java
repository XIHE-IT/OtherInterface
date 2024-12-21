package com.xihe.services;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xihe.controller.K5RegisterController;
import com.xihe.entity.ChannelInfo;
import com.xihe.entity.ClientInfo;
import com.xihe.entity.Company;
import com.xihe.util.IOUtil;
import com.xihe.util.StringUtil;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * @Author gzy
 * @Date 2024/10/22 13:23
 */
public interface ThirdService {
    /**
     * 公共校验token方法
     *
     * @param requestToken 请求的token参数
     * @author gzy
     * @date 2024/10/22 13:27
     */
    default void judgeToken(String requestToken) {
        if (!requestToken.equals(K5RegisterController.commonToken)) {
            throw new RuntimeException("请检查传递的参数！");
        }
    }

    /**
     * 获取到数据库的数据后转为JSONArray的数据
     *
     * @param list  从数据库查询到的数据
     * @param id    客户编码
     * @param name  数据库文件名称
     * @param value 数据库文件路径
     * @return com.alibaba.fastjson2.JSONArray
     * @author gzy
     * @date 2024/11/23 13:53
     */
    default JSONArray fileListChangeArr(List<Map<String, Object>> list, String id, String name, String value) {
        JSONArray backArr = new JSONArray();
        for (Map<String, Object> map : list) {
            JSONObject tempObj = null;
            String clientId = StringUtil.changeObjStr(map.get(id));
            boolean judge = false;
            //校验是否存在该客户若存在则使用上一个客户的
            if (!backArr.isEmpty()) {
                JSONObject lastObj = backArr.getJSONObject(backArr.size() - 1);
                if (lastObj.getString(id).equals(clientId)) {
                    tempObj = lastObj;
                    judge = true;
                }
            }
            if (null == tempObj) {
                tempObj = new JSONObject();
            }
            tempObj.put("clientid", clientId);
            JSONArray pathArr = tempObj.getJSONArray("pathArr");
            if (null == pathArr) {
                pathArr = new JSONArray();
            }
            JSONObject pathObj = new JSONObject();
            pathObj.put("name", StringUtil.changeObjStr(map.get(name)));
            pathObj.put("path", StringUtil.changeObjStr(map.get(value)));
            pathArr.add(pathObj);
            tempObj.put("pathArr", pathArr);
            if (!judge) {
                backArr.add(tempObj);
            }
        }
        return backArr;
    }

    /**
     * jsonArr转换为文件base的数据
     *
     * @param fileArr    文件数组
     * @param beforePath 需要的前缀路径
     * @return com.alibaba.fastjson2.JSONArray
     * @author gzy
     * @date 2024/11/23 13:56
     */
    default JSONArray arrChangeFile(JSONArray fileArr, String beforePath) {
        for (int i = 0; i < fileArr.size(); i++) {
            JSONObject tempObj = fileArr.getJSONObject(i);
            JSONArray pathArr = tempObj.getJSONArray("pathArr");
            for (int j = 0; j < pathArr.size(); j++) {
                JSONObject pathObj = pathArr.getJSONObject(j);
                String path = pathObj.getString("path");
                String filePath = beforePath + path;
                try {
                    pathObj.put("path", IOUtil.convertToBase64(filePath));
                } catch (IOException e) {
                    System.out.println("客户：" + tempObj.getString("clientid") + "，文件：" + path + "转换失败！");
                }
            }
        }
        return fileArr;
    }

    /**
     * 获取公司信息
     *
     * @return com.xihe.entity.Company
     * @author gzy
     * @date 2024/10/22 13:27
     */
    Company getCompany(String requestToken);

    /**
     * 获取所有的客户等级
     *
     * @param requestToken 请求token
     * @param companyCode  公司编码
     * @return com.alibaba.fastjson2.JSONArray
     * @author gzy
     * @date 2024/10/28 10:39
     */
    JSONArray getClientGrade(String requestToken, String companyCode);

    /**
     * 获取所有站点
     *
     * @param requestToken 请求token
     * @author gzy
     * @date 2024/10/28 10:41
     */
    void getAllSite(String requestToken);

    /**
     * 根据站点编码获取站点信息
     *
     * @param requestToken 请求token
     * @param siteCode     站点编码
     * @return java.lang.String
     * @author gzy
     * @date 2024/10/28 10:43
     */
    String getSite(String requestToken, String siteCode);

    /**
     * 获取默认站点
     *
     * @param requestToken 请求token
     * @return java.lang.String
     * @author gzy
     * @date 2024/10/28 10:46
     */
    String getSite(String requestToken);

    /**
     * 注册客户
     *
     * @param clientInfo   需要注册的客户信息
     * @param requestToken 请求token
     * @return java.lang.Integer
     * @author gzy
     * @date 2024/10/28 10:46
     */
    Integer addRegister(ClientInfo clientInfo, String requestToken);

    /**
     * 把base64格式的文件写入K5、T6的特定文件夹
     *
     * @param fileObj 需要写入的文件对象
     * @author gzy
     * @date 2024/11/25 10:12
     */
    default void saveFile(JSONObject fileObj) {
        String base64Image = fileObj.getString("img");  //需要写入的base64的文件
        String outputFile = fileObj.getString("path");  //需要放置的文件夹
        String fileName = fileObj.getString("name");    //文件名称
        //这里只做写入操作
        // 解码Base64字符串为字节数组
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
        // 将字节数组写入文件
        IOUtil.byteChangeFile(outputFile + fileName, imageBytes);
    }

    /**
     * 客户附件批量上传
     *
     * @param requestToken 请求token
     * @param attachArr    需要上传的附件数据数组
     * @return java.lang.Integer
     * @author gzy
     * @date 2024/11/23 14:41
     */
    Integer addClientAttach(String requestToken, String attachArr);

    /**
     * 修改客户附件
     *
     * @param requestToken 请求token
     * @param attachArr    需要修改的附件数据数组
     * @return java.lang.Integer
     * @author gzy
     * @date 2024/11/23 17:47
     */
    Integer updateClientAttach(String requestToken, String attachArr);

    /**
     * 删除客户附件
     *
     * @param requestToken 请求token
     * @param attachArr    需要删除的附件数据数组
     * @return java.lang.Integer
     * @author gzy
     * @date 2024/11/23 17:47
     */
    Integer deleteClientAttach(String requestToken, String attachArr);

    /**
     * 获取所有客户的上传附件
     *
     * @param requestToken 请求token
     * @return com.alibaba.fastjson2.JSONArray
     * @author gzy
     * @date 2024/11/23 13:26
     */
    JSONArray getAllClientAttach(String requestToken);

    /**
     * 更新客户信息
     *
     * @param clientInfo   需要更新的客户信息
     * @param requestToken 请求token
     * @return java.lang.Integer
     * @author gzy
     * @date 2024/10/28 10:48
     */
    Integer updateClient(ClientInfo clientInfo, String requestToken);

    /**
     * 根据客户编码删除客户
     *
     * @param clientCode   客户编码，可批量删除，以英文逗号分隔
     * @param requestToken 请求token
     * @return java.lang.Integer
     * @author gzy
     * @date 2024/10/28 10:48
     */
    Integer deleteClient(String clientCode, String requestToken);

    /**
     * 获取所有客户信息
     *
     * @param requestToken 请求token
     * @return java.util.List<com.xihe.entity.ClientInfo>
     * @author gzy
     * @date 2024/10/28 10:49
     */
    List<ClientInfo> getAllClientInfo(String requestToken);

    /**
     * 添加渠道
     *
     * @param requestToken 请求token
     * @param channelInfo  渠道数据
     * @return java.lang.Integer
     * @author gzy
     * @date 2024/10/29 11:52
     */
    Integer addChannelInfo(String requestToken, JSONObject channelInfo);

    /**
     * 获取所有渠道信息
     *
     * @param requestToken 请求token
     * @return java.util.List<com.xihe.entity.ChannelInfo>
     * @author gzy
     * @date 2024/10/28 10:50
     */
    List<ChannelInfo> getAllChannelInfo(String requestToken);

    /**
     * 获取所有的起运地
     *
     * @param requestToken 请求token
     * @return java.util.List<com.xihe.entity.ChannelInfo>
     * @author gzy
     * @date 2024/10/28 13:36
     */
    JSONArray getAllBaseOrigin(String requestToken);

    /**
     * 同步第三方订单
     *
     * @param requestToken 请求token
     * @param type         目前仅K5使用，0 订单，1 制单
     * @param startTime    同步开始时间
     * @param endTime      同步结束时间
     * @return com.alibaba.fastjson2.JSONArray
     * @author gzy
     * @date 2024/11/5 17:15
     */
    JSONArray synchronizeOrder(String requestToken, int type, String startTime, String endTime);

    /**
     * 同步第三方订单
     *
     * @param requestToken 请求token
     * @return com.alibaba.fastjson2.JSONArray
     * @author gzy
     * @date 2024/12/3 15:12
     */
    JSONArray synchronizeOrder(String requestToken);

    /**
     * 同步运单状态
     *
     * @param requestToken 请求token
     * @param startTime    开始日期
     * @param endTime      结束日期
     * @return com.alibaba.fastjson2.JSONArray
     * @author gzy
     * @date 2024/11/29 16:36
     */
    JSONArray synchronizeBill(String requestToken, String startTime, String endTime);

    /**
     * 同步客户等级接口
     *
     * @param requestToken 请求token
     * @return com.alibaba.fastjson2.JSONArray
     * @author gzy
     * @date 2024/11/15 15:59
     */
    JSONArray synchronizeClientLevel(String requestToken);
}
