package com.xihe.services.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.xihe.entity.ChannelInfo;
import com.xihe.entity.ClientInfo;
import com.xihe.entity.Company;
import com.xihe.services.ThirdService;
import com.xihe.util.IOUtil;
import com.xihe.util.StringUtil;
import jakarta.annotation.Resource;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.xihe.init.RedisGlobalEnum.T6SITEKEY;

/**
 * T6服务具体实现类
 *
 * @Author gzy
 * @Date 2024/10/22 13:22
 */
@DS("t6")
@Slf4j
@Service
public class T6ServiceImpl implements ThirdService {
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String COMPANY = "T6";
    private static Company pkCompany = null;
    private static String siteCode = null;
    //Cookie刷新时间
    private static long refreshTime = 0L;
    private static String refreshCookie = null;

    @Override
    public Company getCompany(String requestToken) {
        if (null != pkCompany) {
            return pkCompany;
        }
        List<Map<String, Object>> list = jdbcTemplate.queryForList("select code,name from s_company");
        if (list.isEmpty()) {
            throw new RuntimeException("未在" + COMPANY + "查询到公司数据！");
        }
        Company company = new Company();
        Map<String, Object> tempMap = list.get(0);
        company.setCompanyCode(StringUtil.changeObjStr(tempMap.get("code")));
        company.setShortName(StringUtil.changeObjStr(tempMap.get("name")));
        company.setUrl("http://47.99.161.33:8380");
        pkCompany = company;
        return company;
    }

    @Override
    public JSONArray getClientGrade(String requestToken, String companyCode) {
        judgeToken(requestToken);

        String sql = "select * from d_client_grade where companycode='" + companyCode + "'";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        if (list.isEmpty()) {
            throw new RuntimeException("未在" + COMPANY + "查询到客户等级数据！");
        }
        JSONArray backArr = new JSONArray();
        for (Map<String, Object> tempMap : list) {
            JSONObject tempJson = new JSONObject();
            tempJson.put("gradeid", StringUtil.changeObjInt(tempMap.get("gradeid")));
            tempJson.put("gradename", StringUtil.changeObjStr(tempMap.get("gradename")));
            backArr.add(tempJson);
        }

        return backArr;
    }

    @Override
    public void getAllSite(String requestToken) {
        judgeToken(requestToken);

        Company company = getCompany(requestToken);
        if (null == company) {
            throw new RuntimeException("未在" + COMPANY + "找到公司！");
        }
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(T6SITEKEY.getValue()))) {
            return;
        }

        String sql = "select code,name,defaultFlag from s_site where companycode='" + company.getCompanyCode() + "'";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        if (list.isEmpty()) {
            throw new RuntimeException("未在" + COMPANY + "查询到站点数据！");
        }

        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
        for (Map<String, Object> tempMap : list) {
            JSONObject tempJson = new JSONObject();
            String code = StringUtil.changeObjStr(tempMap.get("code"));
            tempJson.put("code", code);
            tempJson.put("name", StringUtil.changeObjStr(tempMap.get("name")));
            String defaultFlag = StringUtil.changeObjStr(tempMap.get("defaultFlag"));
            tempJson.put("defaultFlag", defaultFlag);
            if ("Y".equals(defaultFlag)) {
                siteCode = code;
            }
            hashOperations.put(T6SITEKEY.getValue(), code, tempJson.toJSONString());
        }

        if (null == siteCode && !list.isEmpty()) {
            siteCode = list.get(0).get("code").toString();
        }
    }

    @Override
    public String getSite(String requestToken, String siteCode) {
        judgeToken(requestToken);

        getAllSite(requestToken);
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
        String str = hashOperations.get(T6SITEKEY.getValue(), siteCode);
        if (null == str) {
            return null;
        }

        return JSONObject.parseObject(str).getString("code");
    }

    @Override
    public String getSite(String requestToken) {
        judgeToken(requestToken);

        if (null != siteCode) {
            return siteCode;
        }

        getAllSite(requestToken);

        return siteCode;
    }

    /**
     * 刷新T6缓存
     *
     * @author gzy
     * @date 2024/12/4 11:49
     */
    private void refreshCache() {
        HttpResponse<String> response;
        if (0L == refreshTime || System.currentTimeMillis() - refreshTime > 1000 * 60 * 30) {
            response = Unirest.post("http://47.99.161.33:8380/logon/checkLogon").header("Accept", "application/json, text/javascript, */*; q=0.01").header("Referer", "http://47.99.161.33:8380/logon").header("X-Requested-With", "XMLHttpRequest").header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36").header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8").body("usertype=sys&userid=sys&password=sys963&logontype=userAccount&pcinfo=%7B%22os%22%3A%22windows%22%7D").asString();
            if (response.getCookies().isEmpty()) {
                log.error("T6客户注册接口模拟登录获取Cookie错误，返回信息：{}", response.getBody());
                return;
            }
            String value = response.getCookies().get(0).getValue();
            log.info("T6客户注册接口模拟登录返回Cookie:{}", value);
            refreshTime = System.currentTimeMillis();
            refreshCookie = value;
        }

        response = Unirest.get("http://47.99.161.33:8380/sys/devmodel/refreshCache").header("Cookie", "JSESSIONID=" + refreshCookie).asString();
        log.info("T6客户注册调用接口最终返回：{}", response.getBody());
    }

    @Override
    public Integer addRegister(ClientInfo clientInfo, String requestToken) {
        judgeToken(requestToken);

        String sql = "select 1 from d_client where clientid='" + clientInfo.getClientid() + "'";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        if (!list.isEmpty()) {
            throw new RuntimeException("T6客户表已存在客户编码为：" + clientInfo.getClientid() + "的客户，请检查！");
        }

        sql = "select 1 from d_client where clientname='" + clientInfo.getClientname() + "'";
        list = jdbcTemplate.queryForList(sql);
        if (!list.isEmpty()) {
            throw new RuntimeException("T6客户表已存在客户简称为：" + clientInfo.getClientname() + "的客户，请检查！");
        }

        String siteCode = getSite(requestToken);
        Company company = getCompany(requestToken);

        //添加d_client表
        int gradeId = 0;
        if (null != clientInfo.getLevelid()) {
            gradeId = clientInfo.getLevelid();
        }
        int status = clientInfo.getStatus();
        String[] sqlArr = new String[2];
        sqlArr[0] = "insert into d_client(clientid,clientname,totalname,sitecode,gradeid,status,label,companycode,phone,mobile,createdate,createusername,apiflag,apitoken,rectype,deductiontype,checkdate,receiptdate) values('" + clientInfo.getClientid() + "','" + clientInfo.getClientname() + "','" + clientInfo.getTotalname() + "','" + siteCode + "'," + gradeId + "," + status + ",'直客','" + company.getCompanyCode() + "','" + clientInfo.getTel() + "','" + clientInfo.getMobile() + "',GETDATE(),'系统管理员',1,'" + clientInfo.getToken() + "'," + clientInfo.getRectype() + "," + clientInfo.getHoldway() + "," + clientInfo.getCheckDate() + ",0)";
        log.info("添加到" + COMPANY + "客户表，sql为：{}", sqlArr[0]);
        sqlArr[1] = "insert into d_client_user ( clientid, userid, password, userface, roleid, status, type, noticetype, noticeflag, contactname, position, phone, mobile, qq, email, note, createusername, createdate ) values ('" + clientInfo.getClientid() + "', '" + clientInfo.getClientid() + "','" + clientInfo.getPassword() + "','/resources/img/defaultUserFace.png', 'Group0006', 1, 1, '', 0, '" + clientInfo.getClientid() + "', '', '', '', '', '', '', 'admin', GETDATE())";
        log.info("添加到" + COMPANY + "客户子客户表，sql为：{}", sqlArr[1]);
        jdbcTemplate.batchUpdate(sqlArr);

        refreshCache();
        return 1;
    }

    @Override
    public Integer addClientAttach(String requestToken, String tempAttachArr) {
        judgeToken(requestToken);

        JSONArray attachArr = JSONArray.parse(tempAttachArr);
        List<String> sqlList = new ArrayList<>();
        for (int i = 0; i < attachArr.size(); i++) {
            JSONObject tempJson = attachArr.getJSONObject(i);
            String attachName = tempJson.getString("name");
            int index = tempJson.getString("path").lastIndexOf("/") + 1;
            String name = tempJson.getString("path").substring(index);
            String path = "/resources/upload/client/" + name;
            //先把文件放入T6客户文件夹
            tempJson.put("name", name);
            tempJson.put("path", "C:\\T6\\resources\\upload\\client\\");
            saveFile(tempJson);
            sqlList.add("insert into d_client_annex(clientid,annexname,annexfile,createdate,createusername) values('" + tempJson.getString("clientId") + "', '" + attachName + "','" + path + "',getdate(),'管理员')");
        }
        jdbcTemplate.batchUpdate(sqlList.toArray(new String[0]));

        return attachArr.size();
    }

    @Override
    public Integer updateClientAttach(String requestToken, String tempAttachArr) {
        judgeToken(requestToken);

        log.info("请求修改{}客户附件，请求参数为：{}！", COMPANY, tempAttachArr);
        JSONArray attachArr = JSONArray.parse(tempAttachArr);
        List<String> sqlList = new ArrayList<>();
        for (int i = 0; i < attachArr.size(); i++) {
            JSONObject tempJson = attachArr.getJSONObject(i);
            sqlList.add("update d_client_annex set annexfile='" + tempJson.getString("path") + "') where clientid='" + tempJson.getString("clientId") + "' and annexname='" + tempJson.getString("name") + "'");
        }
        jdbcTemplate.batchUpdate(sqlList.toArray(new String[0]));
        log.info("请求{}修改客户附件，执行SQL为：{}！", COMPANY, sqlList);

        return attachArr.size();
    }

    @Override
    public Integer deleteClientAttach(String requestToken, String tempAttachArr) {
        judgeToken(requestToken);

        log.info("请求删除{}客户附件，请求参数为：{}！", COMPANY, tempAttachArr);
        JSONArray attachArr = JSONArray.parse(tempAttachArr);
        List<String> sqlList = new ArrayList<>();
        for (int i = 0; i < attachArr.size(); i++) {
            JSONObject tempJson = attachArr.getJSONObject(i);
            //先删除图片
            String attachName = tempJson.getString("name");
            int index = tempJson.getString("path").lastIndexOf("/") + 1;
            String name = tempJson.getString("path").substring(index);
            String path = "C:\\T6\\resources\\upload\\client\\resources\\upload\\client\\" + name;
            IOUtil.deleteFile(path);

            sqlList.add("delete from d_client_annex where clientid='" + tempJson.getString("clientId") + "' and annexname='" + attachName + "'");
        }
        jdbcTemplate.batchUpdate(sqlList.toArray(new String[0]));
        log.info("请求{}删除客户附件，执行SQL为：{}！", COMPANY, sqlList);

        return attachArr.size();
    }

    @Override
    public JSONArray getAllClientAttach(String requestToken) {
        judgeToken(requestToken);

        String sql = "select clientid, annexname, annexfile from d_client_annex where clientid<>'0' and clientid is not null and annexfile<>'' order by clientid,pk";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);

        return fileListChangeArr(list, "clientid", "annexname", "annexfile");
    }

    @Override
    public Integer updateClient(ClientInfo clientInfo, String requestToken) {
        judgeToken(requestToken);
        int gradeId = 0;
        if (null != clientInfo.getLevelid()) {
            gradeId = clientInfo.getLevelid();
        }
        String marketuserid = clientInfo.getSalemanid();
        String salesserviceuserid = clientInfo.getServiceid();
        String financeuserid = clientInfo.getFinanceid();

        String sql = "update d_client set status=" + clientInfo.getStatus() + ",clientname='" + clientInfo.getClientname() + "',totalname='" + clientInfo.getTotalname() + "',gradeid='" + gradeId + "',phone='" + clientInfo.getTel() + "',mobile='" + clientInfo.getMobile() + "',rectype=" + clientInfo.getRectype() + ",deductiontype=" + clientInfo.getHoldway();
        if (null != marketuserid) {
            sql += ",marketuserid = '" + marketuserid + "'";
        }
        if (null != salesserviceuserid) {
            sql += ",salesserviceuserid = '" + salesserviceuserid + "'";
        }
        if (null != financeuserid) {
            sql += ",financeuserid = '" + financeuserid + "'";
        }
        sql += " where clientid = '" + clientInfo.getClientid() + "'";
        log.info("到" + COMPANY + "修改客户表，sql为：{}", sql);

        int num = jdbcTemplate.update(sql);

        refreshCache();
        return num;
    }

    @Override
    public Integer deleteClient(String clientCode, String requestToken) {
        judgeToken(requestToken);

        //更新client表
        String sql = "delete from d_client where clientid in(";
        StringBuilder clientBuilder = new StringBuilder();
        String[] clientCodes = clientCode.split(",");
        if (clientCodes.length == 0) {
            return 0;
        }
        for (String code : clientCodes) {
            clientBuilder.append("'").append(code).append("'");
        }
        clientBuilder.delete(clientBuilder.length() - 1, clientBuilder.length());
        sql += clientBuilder + ")";
        log.info("删除" + COMPANY + "客户表，sql为：{}", sql);

        return jdbcTemplate.update(sql);
    }

    @Override
    public List<ClientInfo> getAllClientInfo(String requestToken) {
        judgeToken(requestToken);

        //读取d_client表
        String sql = "select clientid,clientname,totalname,gradeid,phone,mobile,companyaddress,rectype,deductiontype,creditamount,marketuserid,salesserviceuserid,financeuserid,contractannex,status,note,annex,apitoken from d_client";
        log.info("获取到" + COMPANY + "客户表，sql为：{}", sql);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        List<ClientInfo> clientInfoList = new ArrayList<>();
        for (Map<String, Object> tempMap : list) {
            ClientInfo clientInfo = new ClientInfo();
            clientInfo.setClientid(StringUtil.changeObjStr(tempMap.get("clientid")));
            clientInfo.setClientname(StringUtil.changeObjStr(tempMap.get("clientname")));
            clientInfo.setTotalname(StringUtil.changeObjStr(tempMap.get("totalname")));
//            clientInfo.setClienttype(StringUtil.changeObjInt(tempMap.get("clienttype")));
            clientInfo.setLevelid(StringUtil.changeObjInt(tempMap.get("gradeid")));         //T6客户等级
            clientInfo.setDegreeid("0");
            clientInfo.setTel(StringUtil.changeObjStr(tempMap.get("phone")));
            clientInfo.setMobile(StringUtil.changeObjStr(tempMap.get("mobile")));
//            clientInfo.setRegno(StringUtil.changeObjStr(tempMap.get("regno")));
            clientInfo.setClientaddress(StringUtil.changeObjStr(tempMap.get("companyaddress")));
//            clientInfo.setOthertel(StringUtil.changeObjStr(tempMap.get("othertel")));
//            clientInfo.setPickaddr(StringUtil.changeObjStr(tempMap.get("pickaddr")));
            clientInfo.setRectype(StringUtil.changeObjInt(tempMap.get("rectype")));
            clientInfo.setHoldway(StringUtil.changeObjInt(tempMap.get("deductiontype")));
            clientInfo.setCredit(StringUtil.changeObjBig(tempMap.get("creditamount")));
            clientInfo.setSalemanid(StringUtil.changeObjStr(tempMap.get("marketuserid")));
            clientInfo.setServiceid(StringUtil.changeObjStr(tempMap.get("salesserviceuserid")));
            clientInfo.setFinanceid(StringUtil.changeObjStr(tempMap.get("financeuserid")));
            clientInfo.setConfidential(StringUtil.changeObjStr(tempMap.get("contractannex")));
            clientInfo.setStatus(StringUtil.changeObjInt(tempMap.get("status")));
            clientInfo.setNote(StringUtil.changeObjStr(tempMap.get("note")));
            clientInfo.setFilePathName(StringUtil.changeObjStr(tempMap.get("annex")));
            clientInfo.setToken(StringUtil.changeObjStr(tempMap.get("apitoken")));
            clientInfoList.add(clientInfo);
        }

        sql = "select clientid,userid,password from d_client_user";
        log.info("获取到" + COMPANY + "客户用户表，sql为：{}", sql);
        list = jdbcTemplate.queryForList(sql);
        for (Map<String, Object> tempMap : list) {
            String clientid = StringUtil.changeObjStr(tempMap.get("clientid"));
            for (ClientInfo clientInfo : clientInfoList) {
                if (clientInfo.getClientid().equals(clientid) && !StringUtils.hasText(clientInfo.getUserid())) {
                    clientInfo.setUserid(StringUtil.changeObjStr(tempMap.get("userid")));
                    clientInfo.setPassword(StringUtil.changeObjStr(tempMap.get("password")));
                    break;
                }
            }
        }

        return clientInfoList;
    }

    @Override
    public Integer addChannelInfo(String requestToken, JSONObject channelInfo) {
        String siteCode = getSite(requestToken);
        //由于细节字段太多，暂时OMS系统没办法给这么多字段，所以暂时未完
        String sql = "insert into d_channel(channelid,channeltypecode,channelname,channelnamecn,channelnameen,label,channeltype,shiptype,venderportid,areaid,rweightruleid," + "totalrweightruleid,volumeruleid,totalvalumeruleid,venderid,fuel) values('" + channelInfo.getString("channelid") + "','" + channelInfo.getString("channelcode") + "'," + "'" + channelInfo.getString("channelname") + "','" + channelInfo.getString("logisticid") + "'," + "'" + channelInfo.getString("mcalid") + "','" + channelInfo.getString("channeltypeid") + "'," + "'" + channelInfo.getString("type") + "','" + siteCode + "','" + channelInfo.getString("channelenname") + "')";
        jdbcTemplate.update(sql);
        return 0;
    }

    @Override
    public List<ChannelInfo> getAllChannelInfo(String requestToken) {
        judgeToken(requestToken);

        //读取d_channel表
        String sql = "select channelid,channelname,channelnamecn,channelnameen,channeltype,status from d_channel";
        log.info("获取到" + COMPANY + "渠道表，sql为：{}", sql);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        List<ChannelInfo> channelInfoList = new ArrayList<>();
        for (Map<String, Object> tempMap : list) {
            ChannelInfo channelInfo = new ChannelInfo();
            channelInfo.setChannelid(StringUtil.changeObjStr(tempMap.get("channelid")));
            channelInfo.setChannelname(StringUtil.changeObjStr(tempMap.get("channelname")));
            channelInfo.setChannelCnName(StringUtil.changeObjStr(tempMap.get("channelnamecn")));
            channelInfo.setChannelEnName(StringUtil.changeObjStr(tempMap.get("channelnameen")));
            channelInfo.setIfmaking(0);
            channelInfo.setOrigin("");
            channelInfo.setType(StringUtil.changeObjInt(tempMap.get("channeltype")));
            channelInfo.setIfStop(StringUtil.changeObjInt(tempMap.get("status")) == 1 ? 1 : 0);
            channelInfoList.add(channelInfo);
        }

        return channelInfoList;
    }

    @Override
    public JSONArray getAllBaseOrigin(String requestToken) {
        judgeToken(requestToken);

        return null;
    }

    @Override
    public JSONArray synchronizeOrder(String requestToken, int type, String startTime, String endTime) {
        judgeToken(requestToken);

        log.info("请求到" + COMPANY + "同步订单接口！");
        //读取b_order表
        String sql = "select systemnumber,waybillnumber,tracknumber,newtracknumber,labelnumber,customernumber1,billnumber,fbanumber,agentnumber,status,clientid,channelid,outchannelid,createdate from b_order where createdate between '" + startTime + "' and '" + endTime + "' order by pkid desc";
        log.info("查询" + COMPANY + "b_order表，sql为：{}", sql);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        JSONArray backArr = new JSONArray();
        for (Map<String, Object> tempMap : list) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("systemnumber", StringUtil.changeObjStr(tempMap.get("systemnumber")));       //系统单号      oms_order_third_system_number
            jsonObject.put("waybillnumber", StringUtil.changeObjStr(tempMap.get("waybillnumber")));     //运单号       oms_order_billid
            jsonObject.put("tracknumber", StringUtil.changeObjStr(tempMap.get("tracknumber")));         //转单号       oms_order_trans_track_number
            jsonObject.put("newtracknumber", StringUtil.changeObjStr(tempMap.get("newtracknumber")));    //换单号      oms_order_new_billid
            jsonObject.put("labelnumber", StringUtil.changeObjStr(tempMap.get("labelnumber")));       //标签单号        oms_order_lable_billid
            jsonObject.put("customernumber1", StringUtil.changeObjStr(tempMap.get("customernumber1")));    //客户参考号1 oms_order_customer_number
            jsonObject.put("billnumber", StringUtil.changeObjStr(tempMap.get("billnumber")));       //提单号           oms_order_bill_number
            jsonObject.put("fbanumber", StringUtil.changeObjStr(tempMap.get("fbanumber")));       //FBA ID           子单表
            jsonObject.put("agentnumber", StringUtil.changeObjStr(tempMap.get("agentnumber")));       //代理单号
            jsonObject.put("channelCode", StringUtil.changeObjStr(tempMap.get("channelid")));       //渠道编码          oms_order_channel_code
            jsonObject.put("clientid", StringUtil.changeObjStr(tempMap.get("clientid")));       //客户编码              oms_order_client_code
            jsonObject.put("status", StringUtil.changeObjStr(tempMap.get("status")));       //状态                     oms_order_status
            jsonObject.put("createDate", StringUtil.changeObjStr(tempMap.get("createdate")));       //制单时间          createdate
            backArr.add(jsonObject);
        }

        //读取b_order_receipt表
        sql = "select systemnumber,waybillnumber,tracknumber,newtracknumber,labelnumber,customernumber1,billnumber,fbanumber,agentnumber,status,clientid,channelid,outchannelid,createdate from b_order_receipt where createdate between '" + startTime + "' and '" + endTime + "' order by pkid desc";
        log.info("查询" + COMPANY + "b_order_receipt表，sql为：{}", sql);
        list = jdbcTemplate.queryForList(sql);
        for (Map<String, Object> tempMap : list) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("systemnumber", StringUtil.changeObjStr(tempMap.get("systemnumber")));       //系统单号
            jsonObject.put("waybillnumber", StringUtil.changeObjStr(tempMap.get("waybillnumber")));          //运单号
            jsonObject.put("tracknumber", StringUtil.changeObjStr(tempMap.get("tracknumber")));         //转单号
            jsonObject.put("newtracknumber", StringUtil.changeObjStr(tempMap.get("newtracknumber")));    //换单号
            jsonObject.put("labelnumber", StringUtil.changeObjStr(tempMap.get("labelnumber")));       //标签单号
            jsonObject.put("customernumber1", StringUtil.changeObjStr(tempMap.get("customernumber1")));    //客户参考号1
            jsonObject.put("billnumber", StringUtil.changeObjStr(tempMap.get("billnumber")));       //提单号
            jsonObject.put("fbanumber", StringUtil.changeObjStr(tempMap.get("fbanumber")));       //FBA ID
            jsonObject.put("agentnumber", StringUtil.changeObjStr(tempMap.get("agentnumber")));       //代理单号
            jsonObject.put("channelCode", StringUtil.changeObjStr(tempMap.get("channelid")));       //渠道编码
            jsonObject.put("clientid", StringUtil.changeObjStr(tempMap.get("clientid")));       //客户编码
            jsonObject.put("status", StringUtil.changeObjStr(tempMap.get("status")));       //状态
            jsonObject.put("createDate", StringUtil.changeObjStr(tempMap.get("createdate")));       //制单时间          createdate
            backArr.add(jsonObject);
        }

        //读取b_order_shipped表
        sql = "select systemnumber,waybillnumber,tracknumber,newtracknumber,labelnumber,customernumber1,billnumber,fbanumber,agentnumber,status,clientid,channelid,outchannelid,createdate from b_order_shipped where createdate between '" + startTime + "' and '" + endTime + "' order by pkid desc";
        log.info("查询" + COMPANY + "b_order_shipped表，sql为：{}", sql);
        list = jdbcTemplate.queryForList(sql);
        for (Map<String, Object> tempMap : list) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("systemnumber", StringUtil.changeObjStr(tempMap.get("systemnumber")));       //系统单号
            jsonObject.put("waybillnumber", StringUtil.changeObjStr(tempMap.get("waybillnumber")));          //运单号
            jsonObject.put("tracknumber", StringUtil.changeObjStr(tempMap.get("tracknumber")));         //转单号
            jsonObject.put("newtracknumber", StringUtil.changeObjStr(tempMap.get("newtracknumber")));    //换单号
            jsonObject.put("labelnumber", StringUtil.changeObjStr(tempMap.get("labelnumber")));       //标签单号
            jsonObject.put("customernumber1", StringUtil.changeObjStr(tempMap.get("customernumber1")));    //客户参考号1
            jsonObject.put("billnumber", StringUtil.changeObjStr(tempMap.get("billnumber")));       //提单号
            jsonObject.put("fbanumber", StringUtil.changeObjStr(tempMap.get("fbanumber")));       //FBA ID
            jsonObject.put("agentnumber", StringUtil.changeObjStr(tempMap.get("agentnumber")));       //代理单号
            jsonObject.put("channelCode", StringUtil.changeObjStr(tempMap.get("channelid")));       //渠道编码
            jsonObject.put("clientid", StringUtil.changeObjStr(tempMap.get("clientid")));       //客户编码
            jsonObject.put("status", StringUtil.changeObjStr(tempMap.get("status")));       //状态
            jsonObject.put("createDate", StringUtil.changeObjStr(tempMap.get("createdate")));       //制单时间          createdate
            backArr.add(jsonObject);
        }

        return backArr;
    }

    @Override
    public JSONArray synchronizeOrder(String requestToken) {
        return null;
    }

    @Override
    public JSONArray synchronizeBill(String requestToken, String startTime, String endTime) {
        judgeToken(requestToken);

        log.info("请求到" + COMPANY + "同步子单接口！");
        JSONArray backArr = new JSONArray();
        //读取b_order_volume_box表
        String sql = "select customerchildnumber, systemchildnumber, childnumber, masternumber from b_order_volume_box where orderpkid in(" +
                "select pkid from b_order where createdate between '" + startTime + "' and '" + endTime + "' " +
                "union select pkid from b_order_receipt where createdate between '" + startTime + "' and '" + endTime + "' " +
                "union select pkid from b_order_shipped where createdate between '" + startTime + "' and '" + endTime + "') and masternumber is not null " +
                "and childnumber is not null";
        log.info("获取到" + COMPANY + "子单表，sql为：{}", sql);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        for (Map<String, Object> tempMap : list) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("childnumber", StringUtil.changeObjStr(tempMap.get("childnumber")));
            jsonObject.put("masternumber", StringUtil.changeObjStr(tempMap.get("masternumber")));
            backArr.add(jsonObject);
        }

        return backArr;
    }

    @Override
    public JSONArray synchronizeClientLevel(String requestToken) {
        judgeToken(requestToken);

        Company company = getCompany(requestToken);

        log.info("请求到" + COMPANY + "同步客户等级接口！");
        //读取order表
        String sql = "select gradeid,gradename  from d_client_grade where companycode='" + company.getCompanyCode() + "'";
        log.info("获取到" + COMPANY + "客户等级表，sql为：{}", sql);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        JSONArray backArr = new JSONArray();
        for (Map<String, Object> tempMap : list) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("code", StringUtil.changeObjStr(tempMap.get("gradeid")));                 //客户等级
            jsonObject.put("name", StringUtil.changeObjStr(tempMap.get("gradename")));               //客户名称
            backArr.add(jsonObject);
        }

        return backArr;
    }
}
