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

import java.util.*;

import static com.xihe.init.RedisGlobalEnum.K5SITEKEY;

/**
 * K5具体服务实现类
 *
 * @Author gzy
 * @Date 2024/10/26 11:59
 */
@DS("k5")
@Slf4j
@Service
public class K5ServiceImpl implements ThirdService {
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String COMPANY = "K5";
    private static Company pkCompany = null;
    private static String siteCode = null;

    @Override
    public Company getCompany(String requestToken) {
        judgeToken(requestToken);

        if (null != pkCompany) {
            return pkCompany;
        }

        log.info("请求到" + COMPANY + "公司接口！");
        List<Map<String, Object>> list = jdbcTemplate.queryForList("select companyid,shortname,url from company");
        if (list.isEmpty()) {
            throw new RuntimeException("未在" + COMPANY + "查询到公司数据！");
        }
        Company company = new Company();
        Map<String, Object> tempMap = list.get(0);
        company.setCompanyCode(StringUtil.changeObjStr(tempMap.get("companyid")));
        company.setShortName(StringUtil.changeObjStr(tempMap.get("shortname")));
        company.setUrl(StringUtil.changeObjStr(tempMap.get("url")));
        pkCompany = company;
        return company;
    }

    @Override
    public JSONArray getClientGrade(String requestToken, String companyCode) {
        judgeToken(requestToken);
        String site = getSite(requestToken);

        log.info("请求到" + COMPANY + "客户等级接口！");
        String sql = "select * from clientdegree where corpid='" + site + "' order by pos";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        if (list.isEmpty()) {
            throw new RuntimeException("未在" + COMPANY + "查询到客户等级数据！");
        }
        JSONArray backArr = new JSONArray();
        for (Map<String, Object> tempMap : list) {
            JSONObject tempJson = new JSONObject();
            tempJson.put("gradeid", StringUtil.changeObjInt(tempMap.get("degreeid")));
            tempJson.put("gradename", StringUtil.changeObjStr(tempMap.get("degreename")));
            backArr.add(tempJson);
        }

        return backArr;
    }

    @Override
    public void getAllSite(String requestToken) {
        judgeToken(requestToken);

        log.info("请求到" + COMPANY + "所有站点接口！");
        Company company = getCompany(requestToken);
        if (null == company) {
            throw new RuntimeException("未在" + COMPANY + "找到公司！");
        }
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(K5SITEKEY.getValue()))) {
            return;
        }

        String sql = "select branchid,branchname,type from branch where corpid='" + company.getCompanyCode() + "' order by type desc";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        if (list.isEmpty()) {
            throw new RuntimeException("未在" + COMPANY + "查询到站点数据！");
        }

        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
        for (Map<String, Object> tempMap : list) {
            JSONObject tempJson = new JSONObject();
            String branchId = StringUtil.changeObjStr(tempMap.get("branchid"));
            tempJson.put("code", branchId);
            tempJson.put("name", StringUtil.changeObjStr(tempMap.get("branchname")));
            int flag = StringUtil.changeObjInt(tempMap.get("type"));
            tempJson.put("defaultFlag", flag);
            if (1 == flag) {
                siteCode = branchId;
            }
            hashOperations.put(K5SITEKEY.getValue(), branchId, tempJson.toJSONString());
        }

        if (null == siteCode && !list.isEmpty()) {
            siteCode = list.get(0).get("branchid").toString();
        }
    }

    @Override
    public String getSite(String requestToken, String siteCode) {
        judgeToken(requestToken);

        log.info("请求到" + COMPANY + "查询具体站点接口！");
        getAllSite(requestToken);
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
        String str = hashOperations.get(K5SITEKEY.getValue(), siteCode);
        if (null == str) {
            return null;
        }

        return JSONObject.parseObject(str).getString("code");
    }

    @Override
    public String getSite(String requestToken) {
        judgeToken(requestToken);

        log.info("请求到" + COMPANY + "默认站点接口！");
        if (null != siteCode) {
            return siteCode;
        }

        getAllSite(requestToken);

        return siteCode;
    }

    @Override
    public Integer addRegister(ClientInfo clientInfo, String requestToken) {
        judgeToken(requestToken);

        Company company = getCompany(requestToken);

        //校验
        String sql = "select 1 from c_user where clientid='" + clientInfo.getClientid() + "'";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        if (!list.isEmpty()) {
            throw new RuntimeException("K5用户表已存在客户编码为：" + clientInfo.getClientid() + "的客户，请检查！");
        }
        sql = "select 1 from client where clientname='" + clientInfo.getClientname() + "'";
        list = jdbcTemplate.queryForList(sql);
        if (!list.isEmpty()) {
            throw new RuntimeException("K5客户表已存在客户简称为：" + clientInfo.getClientname() + "的客户，请检查！");
        }
        sql = "select 1 from client where clientid='" + clientInfo.getClientid() + "'";
        list = jdbcTemplate.queryForList(sql);
        if (!list.isEmpty()) {
            throw new RuntimeException("K5客户表已存在客户编码为：" + clientInfo.getClientid() + "的客户，请检查！");
        }
        sql = "select 1 from c_role_module where userid='" + clientInfo.getClientid() + "'";
        list = jdbcTemplate.queryForList(sql);
        if (!list.isEmpty()) {
            throw new RuntimeException("K5用户权限表已存在客户编码为：" + clientInfo.getClientid() + "的权限，请检查！");
        }

        log.info("请求到" + COMPANY + "注册客户接口！");
        String[] sqlArr = new String[4];
        int status = clientInfo.getStatus();
        if (0 == status) {
            status = 1;
        } else {
            status = 0;
        }
        String degreeid = clientInfo.getDegreeid();
        if (!StringUtils.hasText(degreeid)) {
            degreeid = "0";
        }

        //添加client表
        sqlArr[0] = "insert into client(clientid,clientname,totalname,clienttype,rectypeid,creditday,corpid,status,opdate,editdate,modidate,editor,degreeid,holdway,levelid,tel,mobile,salemanid,serviceid,financeid) values('" + clientInfo.getClientid() + "','" + clientInfo.getClientname() + "','" + clientInfo.getTotalname() + "',1,1,0,'" + company.getCompanyCode() + "'," + status + ",GETDATE(),GETDATE(),GETDATE(),'管理员'," + degreeid + ",0,0,'" + clientInfo.getTel() + "','" + clientInfo.getMobile() + "','" + clientInfo.getSalemanid() + "','" + clientInfo.getServiceid() + "','" + clientInfo.getFinanceid() + "')";
        log.info("添加到" + COMPANY + "客户表，sql为：{}", sqlArr[0]);
        //添加c_api表
        sqlArr[1] = "insert into c_api(clientid,token,editor,editdate) values('" + clientInfo.getClientid() + "','" + clientInfo.getToken() + "','管理员',GETDATE())";
        log.info("添加" + COMPANY + "客户API表，sql为：{}", sqlArr[1]);
        //添加c_user表
        sqlArr[2] = "insert into c_user(userid,password,clientid,editor,editdate) values('" + clientInfo.getClientid() + "','" + clientInfo.getPassword() + "','" + clientInfo.getClientid() + "','管理员',GETDATE())";
        log.info("添加客户用户表，sql为：{}", sqlArr[2]);
        //添加c_role_module表
        sqlArr[3] = "insert into c_role_module select '" + clientInfo.getClientid() + "',moduleid from c_role_module where userid='TestAccount'";
        log.info("添加" + COMPANY + "客户权限表，sql为：{}", sqlArr[3]);

        jdbcTemplate.batchUpdate(sqlArr);

        HttpResponse<String> response = Unirest.post(getCompany(requestToken).getUrl() + "/PostInterfaceService?method=registeredClient").asString();
        String result = response.getBody();
        log.info("调用" + COMPANY + "返回：{}", result);

        return 1;
    }

    @Override
    public Integer addClientAttach(String requestToken, String tempAttachArr) {
        judgeToken(requestToken);

        log.info("请求添加{}客户附件，请求参数为：{}！", COMPANY, tempAttachArr);
        JSONArray attachArr = JSONArray.parse(tempAttachArr);
        List<String> sqlList = new ArrayList<>();
        for (int i = 0; i < attachArr.size(); i++) {
            JSONObject tempJson = attachArr.getJSONObject(i);
            String attachName = tempJson.getString("name");
            int index = tempJson.getString("path").lastIndexOf("/") + 1;
            String name = tempJson.getString("path").substring(index);
            String path = "sysstyle/clientAttach/" + name;
            //先把文件放入K5客户文件夹
            tempJson.put("name", name);
            tempJson.put("path", "C:\\K5\\pk\\sysstyle\\clientAttach\\");
            saveFile(tempJson);
            sqlList.add("insert into clientattach(clientid,attachname,attachpath) values('" + tempJson.getString("clientId") + "','" + attachName + "','" + path + "')");
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
            sqlList.add("update clientattach set attachpath='" + tempJson.getString("path") + "') where clientid='" + tempJson.getString("clientId") + "' and attachname='" + tempJson.getString("name") + "'");
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
            String path = "C:\\K5\\pk\\sysstyle\\clientAttach\\" + name;
            IOUtil.deleteFile(path);

            sqlList.add("delete from clientattach where clientid='" + tempJson.getString("clientId") + "' and attachname='" + attachName + "'");
        }
        jdbcTemplate.batchUpdate(sqlList.toArray(new String[0]));
        log.info("请求{}删除客户附件，执行SQL为：{}！", COMPANY, sqlList);

        return attachArr.size();
    }

    @Override
    public JSONArray getAllClientAttach(String requestToken) {
        judgeToken(requestToken);

        log.info("获取{}所有的客户附件！", COMPANY);
        String sql = "select clientid,attachname,attachpath from clientattach where clientid is not null and attachpath<>'' order by clientId,serialid";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);

        return fileListChangeArr(list, "clientid", "attachname", "attachpath");
    }

    @Override
    public Integer updateClient(ClientInfo clientInfo, String requestToken) {
        judgeToken(requestToken);

//        String site = getSite(requestToken);

        int rectypeid = 0;
        if (null != clientInfo.getRectype()) {
            rectypeid = clientInfo.getRectype();
        }
        int status = clientInfo.getStatus();
        if (0 == status) {
            status = 1;
        } else {
            status = 0;
        }
        String degreeid = clientInfo.getDegreeid();
        if (!StringUtils.hasText(degreeid)) {
            degreeid = "0";
        }
        String salemanid = clientInfo.getSalemanid();
        String serviceid = clientInfo.getServiceid();
        String financeid = clientInfo.getFinanceid();

        log.info("请求到" + COMPANY + "修改客户接口！");
//        String sql = "update client set status=" + status + ",clientname='" + clientInfo.getClientname() + "',totalname='" + clientInfo.getTotalname() + "',tel='" + clientInfo.getTel() + "',mobile='" + clientInfo.getMobile() + "',rectypeid=" + rectypeid + ",creditday=" + clientInfo.getCredit() + ",editdate=GETDATE(),modidate=GETDATE(),editor='" + clientInfo.getClientname() + "',degreeid=" + clientInfo.getDegreeid() + ",holdway=" + clientInfo.getHoldway() + ",levelid=" + clientInfo.getLevelid() + ",regno='" + clientInfo.getRegno() + "',clientaddress='" + clientInfo.getClientaddress() + "',othertel='" + clientInfo.getOthertel() + "',pickaddr='" + clientInfo.getPickaddr() + "',salemanid=" + clientInfo.getSalemanid() + ",serviceid=" + clientInfo.getServiceid() + ",financeid=" + clientInfo.getFinanceid() + ",confidential='" + clientInfo.getConfidential() + "' where clientid='" + clientInfo.getClientid() + "'";
        String sql = "update client set status=" + status + ",clientname='" + clientInfo.getClientname() + "',totalname='" + clientInfo.getTotalname() + "',tel='" + clientInfo.getTel() + "',mobile='" + clientInfo.getMobile() + "',rectypeid=" + rectypeid + ",creditday=" + clientInfo.getCredit() + ",editdate=GETDATE(),modidate=GETDATE(),editor='" + clientInfo.getClientname() + "',degreeid=" + degreeid + ",holdway=" + clientInfo.getHoldway() + ",regno='" + clientInfo.getRegno() + "',clientaddress='" + clientInfo.getClientaddress() + "',othertel='" + clientInfo.getOthertel() + "',pickaddr='" + clientInfo.getPickaddr() + "'";
        if (null != salemanid) {
            sql += ",salemanid = '" + salemanid + "'";
        }
        if (null != serviceid) {
            sql += ",serviceid = '" + serviceid + "'";
        }
        if (null != financeid) {
            sql += ",financeid = '" + financeid + "'";
        }
        sql += " where clientid='" + clientInfo.getClientid() + "'";
        log.info("到" + COMPANY + "修改客户表，sql为：{}", sql);
        int num = jdbcTemplate.update(sql);

        if (clientInfo.getUserid() != null && clientInfo.getPassword() != null) {
            //更新c_user表
            sql = "update c_user set userid='" + clientInfo.getUserid() + "',password='" + clientInfo.getPassword() + "' where clientid='" + clientInfo.getClientid() + "'";
            log.info("更新客户用户表，sql为：{}", sql);
            jdbcTemplate.update(sql);
        }

        return num;
    }

    @Override
    public Integer deleteClient(String clientCode, String requestToken) {
        judgeToken(requestToken);

        log.info("请求到" + COMPANY + "删除客户接口！");
        //更新client表
        String sql = "delete from client where clientid in(";
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
        int num = jdbcTemplate.update(sql);

        //更新c_user表
        sql = "delete from c_user where clientid in(" + clientBuilder + ")";
        log.info("删除客户用户表，sql为：{}", sql);
        jdbcTemplate.update(sql);

        //更新c_api表
        sql = "delete from c_api where clientid in(" + clientBuilder + ")";
        log.info("删除客户API表，sql为：{}", sql);
        jdbcTemplate.update(sql);

        //更新c_role_module表
        sql = "delete from c_role_module where userid in(" + clientBuilder + ")";
        log.info("删除客户权限表，sql为：{}", sql);
        jdbcTemplate.update(sql);

        return num;
    }

    @Override
    public List<ClientInfo> getAllClientInfo(String requestToken) {
        judgeToken(requestToken);

        log.info("请求到" + COMPANY + "获取所有客户接口！");
        //读取client表
        String sql = "select clientid,clientname,totalname,clienttype,degreeid,levelid,tel,mobile,regno,clientaddress,othertel,pickaddr,rectypeid,holdway,credit,salemanid,serviceid,financeid,confidential,status,note,filePathName from client";
        log.info("获取到" + COMPANY + "客户表，sql为：{}", sql);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        List<ClientInfo> clientInfoList = new ArrayList<>();
        for (Map<String, Object> tempMap : list) {
            ClientInfo clientInfo = new ClientInfo();
            clientInfo.setClientid(StringUtil.changeObjStr(tempMap.get("clientid")));
            clientInfo.setClientname(StringUtil.changeObjStr(tempMap.get("clientname")));
            clientInfo.setTotalname(StringUtil.changeObjStr(tempMap.get("totalname")));
            clientInfo.setClienttype(StringUtil.changeObjInt(tempMap.get("clienttype")));
            clientInfo.setLevelid(StringUtil.changeObjInt(tempMap.get("levelid")));         //客户等级
            clientInfo.setDegreeid(StringUtil.changeObjStr(tempMap.get("degreeid")));       //默认报价等级
            clientInfo.setTel(StringUtil.changeObjStr(tempMap.get("tel")));
            clientInfo.setMobile(StringUtil.changeObjStr(tempMap.get("mobile")));
            clientInfo.setRegno(StringUtil.changeObjStr(tempMap.get("regno")));
            clientInfo.setClientaddress(StringUtil.changeObjStr(tempMap.get("clientaddress")));
            clientInfo.setOthertel(StringUtil.changeObjStr(tempMap.get("othertel")));
            clientInfo.setPickaddr(StringUtil.changeObjStr(tempMap.get("pickaddr")));
            clientInfo.setRectype(StringUtil.changeObjInt(tempMap.get("rectypeid")));
            clientInfo.setHoldway(StringUtil.changeObjInt(tempMap.get("holdway")));
            clientInfo.setCredit(StringUtil.changeObjBig(tempMap.get("credit")));
            clientInfo.setSalemanid(StringUtil.changeObjStr(tempMap.get("salemanid")));
            clientInfo.setServiceid(StringUtil.changeObjStr(tempMap.get("serviceid")));
            clientInfo.setFinanceid(StringUtil.changeObjStr(tempMap.get("financeid")));
            clientInfo.setConfidential(StringUtil.changeObjStr(tempMap.get("confidential")));
            clientInfo.setStatus(StringUtil.changeObjInt(tempMap.get("status")));
            clientInfo.setNote(StringUtil.changeObjStr(tempMap.get("note")));
            clientInfo.setFilePathName(StringUtil.changeObjStr(tempMap.get("filePathName")));
            clientInfoList.add(clientInfo);
        }

        sql = "select userid,password,clientid from c_user";
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

        sql = "select clientid,token from c_api";
        log.info("获取API--Token表，sql为：{}", sql);
        list = jdbcTemplate.queryForList(sql);
        for (Map<String, Object> tempMap : list) {
            String clientid = StringUtil.changeObjStr(tempMap.get("clientid"));
            String token = StringUtil.changeObjStr(tempMap.get("token"));
            for (ClientInfo clientInfo : clientInfoList) {
                if (clientInfo.getClientid().equals(clientid)) {
                    clientInfo.setToken(token);
                    break;
                }
            }
        }

        return clientInfoList;
    }

    @Override
    public Integer addChannelInfo(String requestToken, JSONObject channelInfo) {
        String siteCode = getSite(requestToken);
        String sql = "insert into channel(channelid,channelcode,channelname,logisticid,mcalid,channeltypeid,type,corpid,channelenname," +
                "ifmaking,origin,createdate,docman) values('" + channelInfo.getString("channelid") + "','" + channelInfo.getString("channelcode") + "'," +
                "'" + channelInfo.getString("channelname") + "','" + channelInfo.getString("logisticid") + "'," +
                "'" + channelInfo.getString("mcalid") + "','" + channelInfo.getString("channeltypeid") + "'," +
                "'" + channelInfo.getString("type") + "','" + siteCode + "','" + channelInfo.getString("channelenname") + "')";
        jdbcTemplate.update(sql);
        return 0;
    }

    @Override
    public List<ChannelInfo> getAllChannelInfo(String requestToken) {
        judgeToken(requestToken);

        log.info("请求到" + COMPANY + "获取所有渠道接口！");
        //读取d_channel表
        String sql = "select channelid,channelcode,channelname,channelenname,ifmaking,origin,type,ifstop from channel";
        log.info("获取到" + COMPANY + "渠道表，sql为：{}", sql);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        List<ChannelInfo> channelInfoList = new ArrayList<>();
        for (Map<String, Object> tempMap : list) {
            ChannelInfo channelInfo = new ChannelInfo();
            channelInfo.setChannelid(StringUtil.changeObjStr(tempMap.get("channelid")));
            channelInfo.setChannelCode(StringUtil.changeObjStr(tempMap.get("channelcode")));
            channelInfo.setChannelname(StringUtil.changeObjStr(tempMap.get("channelname")));
            channelInfo.setChannelCnName(StringUtil.changeObjStr(tempMap.get("channelname")));
            channelInfo.setChannelEnName(StringUtil.changeObjStr(tempMap.get("channelenname")));
            channelInfo.setIfmaking(StringUtil.changeObjInt(tempMap.get("ifmaking")));
            channelInfo.setOrigin(StringUtil.changeObjStr(tempMap.get("origin")));
            channelInfo.setType(StringUtil.changeObjInt(tempMap.get("type")));    //1:入仓出仓渠道、2：仅入仓渠道、0：仅出仓渠道
            channelInfo.setIfStop(StringUtil.changeObjInt(tempMap.get("ifstop")));  //0:启用、1：停用、2：停用并报价有效期无效
            channelInfoList.add(channelInfo);
        }

        sql = "select clientid,channelid from channelclient order by clientid";
        log.info("获取渠道可用客户表，sql为：{}", sql);
        list = jdbcTemplate.queryForList(sql);
        Map<String, String> map = new HashMap<>();
        for (Map<String, Object> tempMap : list) {
            String clientid = StringUtil.changeObjStr(tempMap.get("clientid"));
            String channelid = StringUtil.changeObjStr(tempMap.get("channelid"));
            if (tempMap.containsKey(clientid)) {
                map.put(clientid, map.get(clientid) + "," + channelid);
                continue;
            }
            map.put(clientid, channelid);
        }
        for (ChannelInfo channelInfo : channelInfoList) {
            String clientArr = map.get(channelInfo.getChannelid());
            channelInfo.setClientArr(clientArr);
        }

        return channelInfoList;
    }

    @Override
    public JSONArray getAllBaseOrigin(String requestToken) {
        judgeToken(requestToken);

        log.info("请求到" + COMPANY + "获取所有起运地接口！");
        //读取basedataitem表
        String sql = "select itemcode,itemname from basedataitem where type=180 order by pos";
        log.info("获取到" + COMPANY + "起运地表，sql为：{}", sql);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        JSONArray backArr = new JSONArray();
        for (Map<String, Object> tempMap : list) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", StringUtil.changeObjStr(tempMap.get("itemcode")));
            jsonObject.put("name", StringUtil.changeObjStr(tempMap.get("itemname")));
            backArr.add(jsonObject);
        }

        return backArr;
    }

    @Override
    public JSONArray synchronizeOrder(String requestToken, int type, String startTime, String endTime) {
        judgeToken(requestToken);

        log.info("请求到" + COMPANY + "同步订单接口！");
        JSONArray backArr = new JSONArray();
        if (0 == type) {
            //读取order表
            String sql = "select serialid,billid,refernumb,corpbillid,labelbillid,referenceid,domesticno,channelid,clientid,flag,editdate from c_order where modidate between '" + startTime + "' and '" + endTime + "' order by serialid desc";
            log.info("获取到" + COMPANY + "订单表，sql为：{}", sql);
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
            for (Map<String, Object> tempMap : list) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("serialid", StringUtil.changeObjStr(tempMap.get("serialid")));
                jsonObject.put("billid", StringUtil.changeObjStr(tempMap.get("billid")));               //服务商单号 oms_order_trans_track_number
                jsonObject.put("refernumb", StringUtil.changeObjStr(tempMap.get("refernumb")));         //客户订单号 oms_order_customer_number
                jsonObject.put("corpbillid", StringUtil.changeObjStr(tempMap.get("corpbillid")));       //订单号（K5系统单号）oms_order_third_system_number
                jsonObject.put("labelbillid", StringUtil.changeObjStr(tempMap.get("labelbillid")));     //标签单号  oms_order_lable_billid
                jsonObject.put("referenceid", StringUtil.changeObjStr(tempMap.get("referenceid")));     //FBA ID    子单表
                jsonObject.put("domesticno", StringUtil.changeObjStr(tempMap.get("domesticno")));       //国内快递单号    oms_order_billid
                jsonObject.put("channelCode", StringUtil.changeObjStr(tempMap.get("channelid")));       //渠道编码      oms_order_channel_code
                jsonObject.put("clientid", StringUtil.changeObjStr(tempMap.get("clientid")));       //客户编码          oms_order_client_code
                jsonObject.put("flag", StringUtil.changeObjInt(tempMap.get("flag")));       //状态                    oms_order_status
                jsonObject.put("diff", 0);       //区分订单、制单
                jsonObject.put("createDate", StringUtil.changeObjStr(tempMap.get("editdate")));       //制单时间          editdate
                backArr.add(jsonObject);
            }
        } else {
            //读取e_order表
            String sql = "select billid,refernumb,corpbillid,shipmentid,childTrackingNumber,clientid,flag,channelid,editdate from e_order where flag in(2,3,4,5) and isNull(ifdelete,0) = 0 and editdate between '" + startTime + "' and '" + endTime + "' order by serialid desc";
            log.info("获取到" + COMPANY + "制单表，sql为：{}", sql);
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
            for (Map<String, Object> tempMap : list) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("billid", StringUtil.changeObjStr(tempMap.get("billid")));               //转单号   oms_order_agent_number
                jsonObject.put("refernumb", StringUtil.changeObjStr(tempMap.get("refernumb")));         //客户参考号 oms_order_customer_number
                jsonObject.put("corpbillid", StringUtil.changeObjStr(tempMap.get("corpbillid")));       //内部订单号 oms_order_third_system_number
                jsonObject.put("shipmentid", StringUtil.changeObjStr(tempMap.get("shipmentid")));     //短单号     oms_order_billid
                jsonObject.put("childTrackingNumber", StringUtil.changeObjStr(tempMap.get("childTrackingNumber")));     //子单号   子单表
                jsonObject.put("clientid", StringUtil.changeObjStr(tempMap.get("clientid")));       //客户编码          oms_order_client_code
                jsonObject.put("channelCode", StringUtil.changeObjStr(tempMap.get("channelid")));       //渠道编码        oms_order_channel_code
                jsonObject.put("createDate", StringUtil.changeObjStr(tempMap.get("editdate")));       //制单时间          editdate
                //转换制单状态为OMS系统状态
                int flag = StringUtil.changeObjInt(tempMap.get("flag"));
                if (3 == flag) {
                    flag = 15;
                } else if (4 == flag) {
                    flag = 16;
                } else if (5 == flag) {
                    flag = 17;
                }
                jsonObject.put("flag", flag);       //状态                     oms_order_status
                jsonObject.put("diff", 1);       //区分订单、制单
                backArr.add(jsonObject);
            }
        }

        return backArr;
    }

    @Override
    public JSONArray synchronizeOrder(String requestToken) {
        log.info("请求到" + COMPANY + "实时同步订单接口！");
        JSONArray backArr = new JSONArray();
        //读取order_copy表
        String sql = "select top 5 serialid,sid,clientid,channelid,billid,refernumb,corpbillid,labelbillid,referenceid,domesticno,type,flag,status,update_date from order_copy order by serialid";
        log.info("获取到" + COMPANY + "订单表，sql为：{}", sql);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        StringBuilder sBuilder = new StringBuilder();
        for (Map<String, Object> tempMap : list) {
            JSONObject jsonObject = new JSONObject();
            int serialid = StringUtil.changeObjInt(tempMap.get("serialid"));
            sBuilder.append(serialid).append(",");
            jsonObject.put("serialid", serialid);                                                   //自动生成主键
            jsonObject.put("sid", StringUtil.changeObjStr(tempMap.get("sid")));                     //c_order和e_order的主键
            jsonObject.put("channelCode", StringUtil.changeObjStr(tempMap.get("channelid")));       //渠道编码
            jsonObject.put("clientid", StringUtil.changeObjStr(tempMap.get("clientid")));           //客户编码
            jsonObject.put("billid", StringUtil.changeObjStr(tempMap.get("billid")));               //订单（服务商单号）、制单（转单号）
            jsonObject.put("refernumb", StringUtil.changeObjStr(tempMap.get("refernumb")));         //客户订单号
            jsonObject.put("corpbillid", StringUtil.changeObjStr(tempMap.get("corpbillid")));       //系统单号（订单号）
            jsonObject.put("labelbillid", StringUtil.changeObjStr(tempMap.get("labelbillid")));     //订单（标签单号）
            jsonObject.put("referenceid", StringUtil.changeObjStr(tempMap.get("referenceid")));     //制单（短单号）
            jsonObject.put("domesticno", StringUtil.changeObjStr(tempMap.get("domesticno")));       //订单（国内快递单号）、制单（子单号）
            int diff = StringUtil.changeObjInt(tempMap.get("type"));
            jsonObject.put("diff", diff);                   //0 快递，1 制单
            //转换制单状态为OMS系统状态
            int flag = StringUtil.changeObjInt(tempMap.get("flag"));
            if (1 == diff) {
                if (3 == flag) {
                    flag = 15;
                } else if (4 == flag) {
                    flag = 16;
                } else if (5 == flag) {
                    flag = 17;
                }
            }

            jsonObject.put("flag", flag);                   //状态
            jsonObject.put("status", StringUtil.changeObjInt(tempMap.get("status")));               //0 添加，1 修改，2 删除
            backArr.add(jsonObject);
        }

        if (!sBuilder.isEmpty()) {
            sBuilder.delete(sBuilder.length() - 1, sBuilder.length());
            //删除获取到的数据
            sql = "delete from order_copy where serialid in(" + sBuilder + ")";
            jdbcTemplate.update(sql);
        }

        return backArr;
    }

    @Override
    public JSONArray synchronizeBill(String requestToken, String startTime, String endTime) {
        judgeToken(requestToken);

        log.info("请求到" + COMPANY + "同步运单接口！");
        JSONArray backArr = new JSONArray();
        //读取order表
        String sql = "select refernumb,transbillid,status from billstatus where indate between '" + startTime + "' and '" + endTime + "' order by sid desc";
        log.info("获取到" + COMPANY + "运单表，sql为：{}", sql);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        for (Map<String, Object> tempMap : list) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("refernumb", StringUtil.changeObjStr(tempMap.get("refernumb")));
            jsonObject.put("transbillid", StringUtil.changeObjStr(tempMap.get("transbillid")));
            jsonObject.put("status", StringUtil.changeObjInt(tempMap.get("status")));       //状态                    oms_order_status
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
        String sql = "select degreeid,degreename  from clientdegree where corpid='" + company.getCompanyCode() + "'";
        log.info("获取到" + COMPANY + "客户等级表，sql为：{}", sql);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        JSONArray backArr = new JSONArray();
        for (Map<String, Object> tempMap : list) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("code", StringUtil.changeObjStr(tempMap.get("degreeid")));                 //客户等级
            jsonObject.put("name", StringUtil.changeObjStr(tempMap.get("degreename")));               //客户名称
            backArr.add(jsonObject);
        }

        return backArr;
    }
}
