package com.xihe.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xihe.entity.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author gzy
 * @Date 2024/8/29 18:17
 */
@RestController
public class K5RegisterController {
    private static final Logger log = LoggerFactory.getLogger(K5RegisterController.class);
    //公共token
    private final String commonToken = "bc2HsFK6TyPRXYY";

    @GetMapping(value = "/test")
    public Result<String> test() {
        return Result.success("测试成功");
    }

    @Operation(summary = "获取所请求K5的公司信息")
    @PostMapping(value = "/getCompany")
    public Result<Company> register(String requestToken) {
        if (!requestToken.equals(commonToken)) {
            return Result.failure(198, "请检查传递的参数！");
        }
//        String url = "jdbc:jtds:sqlserver://localhost:1433;databaseName=k5new;user=sa;password=qwe123!";//sa身份连接
        String url = "jdbc:jtds:sqlserver://localhost:1433/k5new";
        String user = "sa";
        String password = "qwe123!";

        // Declare the JDBC objects.
        Connection con = null;
        PreparedStatement pre = null;
        ResultSet rs = null;

        try {
            // Establish the connection.
            System.out.println("begin.");
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
//            con = DriverManager.getConnection(url);
            con = DriverManager.getConnection(url, user, password);
            System.out.println("end.");

            //添加client表
            String sql = "select companyid,shortname,url from company";
            log.info("获取公司表数据，sql为：{}", sql);
            pre = con.prepareStatement(sql);
            rs = pre.executeQuery();
            if (rs.next()) {
                Company company = new Company();
                company.setCompanyid(rs.getString("companyid"));
                company.setShortname(rs.getString("shortname"));
                company.setUrl(rs.getString("url"));
                return Result.success(company);
            }
        } catch (Exception e) {
            log.info("添加客户报错：{}", e);
//            e.printStackTrace();
        } finally {
            if (rs != null)
                try {
                    rs.close();
                } catch (Exception e) {
                }
            if (pre != null)
                try {
                    pre.close();
                } catch (Exception e) {
                }
            if (con != null)
                try {
                    con.close();
                } catch (Exception e) {
                }
        }

        return Result.failure(198, "获取所请求K5的公司信息失败！");
    }

    @Operation(summary = "客户注册")
    @PostMapping(value = "/addRegister")
    public Result<String> register(@Valid Register register, String requestToken) {
        if (!requestToken.equals(commonToken)) {
            return Result.failure(198, "请检查传递的参数！");
        }
//        String url = "jdbc:jtds:sqlserver://localhost:1433;databaseName=k5new;user=sa;password=qwe123!";//sa身份连接
        String url = "jdbc:jtds:sqlserver://localhost:1433/k5new";
        String user = "sa";
        String password = "qwe123!";

        // Declare the JDBC objects.
        Connection con = null;
        PreparedStatement pre = null;
        ResultSet rs = null;

        try {
            // Establish the connection.
            System.out.println("begin.");
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
//            con = DriverManager.getConnection(url);
            con = DriverManager.getConnection(url, user, password);
            System.out.println("end.");

            //添加client表
            String sql = "insert into client(clientid,clientname,clienttype,rectypeid,creditday,corpid,status,opdate,editdate,modidate,editor) values(?,?,1,1,0,'',0,GETDATE(),GETDATE(),GETDATE(),'管理员')";
            log.info("添加客户表，sql为：{}", sql);
            pre = con.prepareStatement(sql);
            pre.setString(1, register.getUserId());
            pre.setString(2, register.getClientName());
            pre.executeUpdate();
            //添加c_api表
            sql = "insert into c_api(clientid,token,editor,editdate) values(?,?,'管理员',GETDATE())";
            log.info("添加客户API表，sql为：{}", sql);
            pre = con.prepareStatement(sql);
            pre.setString(1, register.getUserId());
            pre.setString(2, register.getToken());
            pre.executeUpdate();
            //添加c_user表
            sql = "insert into c_user(userid,password,clientid,editor,editdate) values(?,?,?,?,GETDATE())";
            log.info("添加客户用户表，sql为：{}", sql);
            pre = con.prepareStatement(sql);
            pre.setString(1, register.getUserId());
            pre.setString(2, register.getPassWord());
            pre.setString(3, register.getUserId());
            pre.setString(4, "管理员");
            pre.executeUpdate();
            //添加c_role_module表
            sql = "insert into c_role_module select '" + register.getUserId() + "',moduleid from c_role_module where userid=?";
            log.info("添加客户权限表，sql为：{}", sql);
            pre = con.prepareStatement(sql);
            pre.setString(1, "TestAccount");
            pre.executeUpdate();
        } catch (Exception e) {
            log.info("添加客户报错：{}", e);
//            e.printStackTrace();
        } finally {
            if (rs != null)
                try {
                    rs.close();
                } catch (Exception e) {
                }
            if (pre != null)
                try {
                    pre.close();
                } catch (Exception e) {
                }
            if (con != null)
                try {
                    con.close();
                } catch (Exception e) {
                }
        }

        HttpResponse<String> response = Unirest.post("http://localhost:8880/PostInterfaceService?method=registeredClient").asString();
        String result = response.getBody();
        log.info("调用K5返回：{}", result);
        return Result.success("测试成功");
    }

    @Operation(summary = "客户更新")
    @PostMapping(value = "/updateClient")
    public Result<Integer> updateClient(ClientInfo clientInfo, String requestToken) {
        if (!requestToken.equals(commonToken)) {
            return Result.failure(198, "请检查传递的参数！");
        }
        String url = "jdbc:jtds:sqlserver://localhost:1433/k5new";
        String user = "sa";
        String password = "qwe123!";

        // Declare the JDBC objects.
        Connection con = null;
        PreparedStatement pre = null;
        ResultSet rs = null;

        try {
            // Establish the connection.
            System.out.println("begin.");
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            con = DriverManager.getConnection(url, user, password);
            System.out.println("end.");

            //更新client表
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("update client set ");
            if (clientInfo.getClientid() != null) {
                sqlBuilder.append("clientid='").append(clientInfo.getClientid()).append("',");
            }
            if (clientInfo.getClientname() != null) {
                sqlBuilder.append("clientname='").append(clientInfo.getClientname()).append("',");
            }
            if (clientInfo.getTotalname() != null) {
                sqlBuilder.append("totalname='").append(clientInfo.getTotalname()).append("',");
            }
            if (clientInfo.getClienttype() != null) {
                sqlBuilder.append("clienttype=").append(clientInfo.getClienttype()).append(",");
            }
            if (clientInfo.getLevelid() != null) {
                sqlBuilder.append("levelid=").append(clientInfo.getLevelid()).append(",");
            }
            if (clientInfo.getTel() != null) {
                sqlBuilder.append("tel='").append(clientInfo.getTel()).append("',");
            }
            if (clientInfo.getMobile() != null) {
                sqlBuilder.append("mobile='").append(clientInfo.getMobile()).append("',");
            }
            if (clientInfo.getRegno() != null) {
                sqlBuilder.append("regno='").append(clientInfo.getRegno()).append("',");
            }
            if (clientInfo.getClientaddress() != null) {
                sqlBuilder.append("clientaddress='").append(clientInfo.getClientaddress()).append("',");
            }
            if (clientInfo.getOthertel() != null) {
                sqlBuilder.append("othertel='").append(clientInfo.getOthertel()).append("',");
            }
            if (clientInfo.getPickaddr() != null) {
                sqlBuilder.append("pickaddr='").append(clientInfo.getPickaddr()).append("',");
            }
            if (clientInfo.getRectypeid() != null) {
                sqlBuilder.append("rectypeid=").append(clientInfo.getRectypeid()).append(",");
            }
            if (clientInfo.getHoldway() != null) {
                sqlBuilder.append("holdway=").append(clientInfo.getHoldway()).append(",");
            }
            if (clientInfo.getCredit() != null) {
                sqlBuilder.append("credit=").append(clientInfo.getCredit()).append(",");
            }
            if (clientInfo.getDegreeid() != null) {
                sqlBuilder.append("degreeid='").append(clientInfo.getDegreeid()).append("',");
            }
            if (clientInfo.getSalemanid() != null) {
                sqlBuilder.append("salemanid=").append(clientInfo.getSalemanid()).append(",");
            }
            if (clientInfo.getServiceid() != null) {
                sqlBuilder.append("serviceid=").append(clientInfo.getServiceid()).append(",");
            }
            if (clientInfo.getFinanceid() != null) {
                sqlBuilder.append("financeid=").append(clientInfo.getFinanceid()).append(",");
            }
            if (clientInfo.getConfidential() != null) {
                sqlBuilder.append("confidential='").append(clientInfo.getConfidential()).append("',");
            }
            sqlBuilder.deleteCharAt(sqlBuilder.length() - 1);
            sqlBuilder.append(" where clientid='").append(clientInfo.getClientid()).append("'");
            log.info("更新客户表，sql为：{}", sqlBuilder);
            pre = con.prepareStatement(sqlBuilder.toString());
            int num = pre.executeUpdate();

            if (clientInfo.getUserid() != null && clientInfo.getPassword() != null) {
                //更新c_user表
                String sql = "update c_user set userid=?,password=? where clientid=?";
                log.info("更新客户用户表，sql为：{}", sqlBuilder);
                pre = con.prepareStatement(sql);
                pre.setString(1, clientInfo.getPassword());
                pre.setString(2, clientInfo.getUserid());
                pre.setString(3, clientInfo.getClientid());
                pre.executeUpdate();
            }

            return Result.success(num);
        } catch (Exception e) {
            log.info("更新客户报错：{}", e);
//            e.printStackTrace();
        } finally {
            if (rs != null)
                try {
                    rs.close();
                } catch (Exception ignored) {
                }
            if (pre != null)
                try {
                    pre.close();
                } catch (Exception ignored) {
                }
            if (con != null)
                try {
                    con.close();
                } catch (Exception ignored) {
                }
        }
        return Result.failure(199, "更新客户信息失败！");
    }

    @Operation(summary = "客户删除")
    @PostMapping(value = "/deleteClient")
    public Result<Integer> deleteClient(String clientCode, String requestToken) {
        if (!requestToken.equals(commonToken)) {
            return Result.failure(198, "请检查传递的参数！");
        }
        String url = "jdbc:jtds:sqlserver://localhost:1433/k5new";
        String user = "sa";
        String password = "qwe123!";

        // Declare the JDBC objects.
        Connection con = null;
        PreparedStatement pre = null;
        ResultSet rs = null;

        try {
            // Establish the connection.
            System.out.println("begin.");
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            con = DriverManager.getConnection(url, user, password);
            System.out.println("end.");

            //更新client表
            String sql = "delete from client where clientid in(";
            StringBuilder clientBuilder = new StringBuilder();
            String[] clientCodes = clientCode.split(",");
            if (clientCodes.length == 0) {
                return Result.failure(199, "请检查传递的客户编码参数！");
            }
            for (String code : clientCodes) {
                clientBuilder.append(code).append(",");
            }
            clientBuilder.delete(clientBuilder.length() - 1, clientBuilder.length());
            sql += clientBuilder + ")";
            log.info("删除客户表，sql为：{}", sql);
            pre = con.prepareStatement(sql);
            int num = pre.executeUpdate();

            //更新c_user表
            sql = "delete from c_user where clientid in(" + clientBuilder + ")";
            log.info("删除客户用户表，sql为：{}", sql);
            pre = con.prepareStatement(sql);
            pre.executeUpdate();

            //更新c_api表
            sql = "delete from c_api where clientid in(" + clientBuilder + ")";
            log.info("删除客户API表，sql为：{}", sql);
            pre = con.prepareStatement(sql);
            pre.executeUpdate();

            //更新c_role_module表
            sql = "delete from c_role_module where userid in(" + clientBuilder + ")";
            log.info("删除客户权限表，sql为：{}", sql);
            pre = con.prepareStatement(sql);
            pre.executeUpdate();

            return Result.success(num);
        } catch (Exception e) {
            log.info("更新客户报错：{}", e);
//            e.printStackTrace();
        } finally {
            if (rs != null)
                try {
                    rs.close();
                } catch (Exception ignored) {
                }
            if (pre != null)
                try {
                    pre.close();
                } catch (Exception ignored) {
                }
            if (con != null)
                try {
                    con.close();
                } catch (Exception ignored) {
                }
        }
        return Result.failure(199, "更新客户信息失败！");
    }

    @Operation(summary = "获取所有的K5客户")
    @PostMapping(value = "/getAllClientInfo")
    public Result<List<ClientInfo>> getAllClientInfo(String requestToken) {
        if (!requestToken.equals(commonToken)) {
            return Result.failure(198, "请检查传递的参数！");
        }
//        String url = "jdbc:jtds:sqlserver://localhost:1433;databaseName=k5new;user=sa;password=qwe123!";//sa身份连接
        String url = "jdbc:jtds:sqlserver://localhost:1433/k5new";
        String user = "sa";
        String password = "qwe123!";

        // Declare the JDBC objects.
        Connection con = null;
        PreparedStatement pre = null;
        ResultSet rs = null;

        try {
            // Establish the connection.
            System.out.println("begin.");
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            con = DriverManager.getConnection(url, user, password);
            System.out.println("end.");

            List<ClientInfo> list = new ArrayList<>();
            //读取client表
            String sql = "select clientid,clientname,totalname,clienttype,levelid,tel,mobile,regno,clientaddress,othertel,pickaddr,rectypeid,holdway,credit,degreeid,salemanid,serviceid,financeid,confidential,status,note,filePathName from client";
            log.info("获取客户表，sql为：{}", sql);
            pre = con.prepareStatement(sql);
            rs = pre.executeQuery();
            while (rs.next()) {
                ClientInfo clientInfo = new ClientInfo();
                clientInfo.setClientid(rs.getString("clientid"));
                clientInfo.setClientname(rs.getString("clientname"));
                clientInfo.setTotalname(rs.getString("totalname"));
                clientInfo.setClienttype(rs.getInt("clienttype"));
                clientInfo.setLevelid(rs.getInt("levelid"));
                clientInfo.setTel(rs.getString("tel"));
                clientInfo.setMobile(rs.getString("mobile"));
                clientInfo.setRegno(rs.getString("regno"));
                clientInfo.setClientaddress(rs.getString("clientaddress"));
                clientInfo.setOthertel(rs.getString("othertel"));
                clientInfo.setPickaddr(rs.getString("pickaddr"));
                clientInfo.setRectypeid(rs.getInt("rectypeid"));
                clientInfo.setHoldway(rs.getInt("holdway"));
                clientInfo.setCredit(rs.getBigDecimal("credit"));
                clientInfo.setDegreeid(rs.getString("degreeid"));
                clientInfo.setSalemanid(rs.getString("salemanid"));
                clientInfo.setServiceid(rs.getString("serviceid"));
                clientInfo.setFinanceid(rs.getString("financeid"));
                clientInfo.setConfidential(rs.getString("confidential"));
                clientInfo.setStatus(rs.getInt("status"));
                clientInfo.setNote(rs.getString("note"));
                clientInfo.setFilePathName(rs.getString("filePathName"));
                list.add(clientInfo);
            }

            sql = "select userid,password,clientid from c_user";
            log.info("获取客户用户表，sql为：{}", sql);
            pre = con.prepareStatement(sql);
            rs = pre.executeQuery();
            while (rs.next()) {
                String clientid = rs.getString("clientid");
                for (ClientInfo clientInfo : list) {
                    if (clientInfo.getClientid().equals(clientid) && !StringUtils.hasText(clientInfo.getUserid())) {
                        clientInfo.setUserid(rs.getString("userid"));
                        clientInfo.setPassword(rs.getString("password"));
                        break;
                    }
                }
            }

            sql = "select clientid,token from c_api";
            log.info("获取API--Token表，sql为：{}", sql);
            pre = con.prepareStatement(sql);
            rs = pre.executeQuery();
            while (rs.next()) {
                String clientid = rs.getString("clientid");
                String token = rs.getString("token");
                for (ClientInfo clientInfo : list) {
                    if (clientInfo.getClientid().equals(clientid)) {
                        clientInfo.setToken(token);
                        break;
                    }
                }
            }
            return Result.success(list);
        } catch (Exception e) {
            log.info("获取客户报错：{}", e);
//            e.printStackTrace();
        } finally {
            if (rs != null)
                try {
                    rs.close();
                } catch (Exception ignored) {
                }
            if (pre != null)
                try {
                    pre.close();
                } catch (Exception ignored) {
                }
            if (con != null)
                try {
                    con.close();
                } catch (Exception ignored) {
                }
        }
        return Result.failure(199, "获取客户信息失败！");
    }

    @Operation(summary = "获取所有的K5渠道")
    @PostMapping(value = "/getAllChannelInfo")
    public Result<List<ChannelInfo>> getAllChannelInfo(String requestToken) {
        if (!requestToken.equals(commonToken)) {
            return Result.failure(198, "请检查传递的参数！");
        }
//        String url = "jdbc:jtds:sqlserver://localhost:1433;databaseName=k5new;user=sa;password=qwe123!";//sa身份连接
        String url = "jdbc:jtds:sqlserver://localhost:1433/k5new";
        String user = "sa";
        String password = "qwe123!";

        // Declare the JDBC objects.
        Connection con = null;
        PreparedStatement pre = null;
        ResultSet rs = null;

        try {
            // Establish the connection.
            System.out.println("begin.");
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            con = DriverManager.getConnection(url, user, password);
            System.out.println("end.");

            List<ChannelInfo> list = new ArrayList<>();
            //读取client表
            String sql = "select channelid,channelname,channelenname,ifmaking,origin from channel";
            log.info("获取渠道表，sql为：{}", sql);
            pre = con.prepareStatement(sql);
            rs = pre.executeQuery();
            while (rs.next()) {
                ChannelInfo channelInfo = new ChannelInfo();
                channelInfo.setChannelid(rs.getString("channelid"));
                channelInfo.setChannelname(rs.getString("channelname"));
                channelInfo.setChannelenname(rs.getString("channelenname"));
                channelInfo.setIfmaking(rs.getInt("ifmaking"));
                channelInfo.setOrigin(rs.getString("origin"));
                list.add(channelInfo);
            }

            Map<String, String> map = new HashMap<>();
            sql = "select clientid,channelid from channelclient order by clientid";
            log.info("获取渠道可用客户表，sql为：{}", sql);
            pre = con.prepareStatement(sql);
            rs = pre.executeQuery();
            while (rs.next()) {
                String clientid = rs.getString("clientid");
                String channelid = rs.getString("channelid");
                if (map.containsKey(clientid)) {
                    map.put(clientid, map.get(clientid) + "," + channelid);
                    continue;
                }
                map.put(clientid, channelid);
            }
            for (ChannelInfo channelInfo : list) {
                String clientArr = map.get(channelInfo.getChannelid());
                channelInfo.setClientArr(clientArr);
            }
            return Result.success(list);
        } catch (Exception e) {
            log.info("更新渠道报错：{}", e);
//            e.printStackTrace();
        } finally {
            if (rs != null)
                try {
                    rs.close();
                } catch (Exception ignored) {
                }
            if (pre != null)
                try {
                    pre.close();
                } catch (Exception ignored) {
                }
            if (con != null)
                try {
                    con.close();
                } catch (Exception ignored) {
                }
        }
        return Result.failure(199, "获取渠道信息失败！");
    }

    @Operation(summary = "获取所有的K5起运地")
    @PostMapping(value = "/getAllBaseOrigin")
    public Result<JSONArray> getAllBaseOrigin(String requestToken) {
        if (!requestToken.equals(commonToken)) {
            return Result.failure(198, "请检查传递的参数！");
        }
        String url = "jdbc:jtds:sqlserver://localhost:1433/k5new";
        String user = "sa";
        String password = "qwe123!";

        // Declare the JDBC objects.
        Connection con = null;
        PreparedStatement pre = null;
        ResultSet rs = null;

        try {
            // Establish the connection.
            System.out.println("begin.");
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            con = DriverManager.getConnection(url, user, password);
            System.out.println("end.");

            JSONArray backArr = new JSONArray();
            //读取client表
            String sql = "select itemcode,itemname from basedataitem where type=180 order by pos";
            log.info("获取起运地表，sql为：{}", sql);
            pre = con.prepareStatement(sql);
            rs = pre.executeQuery();
            while (rs.next()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", rs.getString("itemcode"));
                jsonObject.put("name", rs.getString("itemname"));
                backArr.add(jsonObject);
            }

            return Result.success(backArr);
        } catch (Exception e) {
            log.info("获取起运地报错：{}", e);
//            e.printStackTrace();
        } finally {
            if (rs != null)
                try {
                    rs.close();
                } catch (Exception ignored) {
                }
            if (pre != null)
                try {
                    pre.close();
                } catch (Exception ignored) {
                }
            if (con != null)
                try {
                    con.close();
                } catch (Exception ignored) {
                }
        }
        return Result.failure(199, "获取起运地信息失败！");
    }
}
