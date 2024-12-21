package com.xihe.controller;

import com.alibaba.fastjson2.JSONArray;
import com.xihe.entity.ChannelInfo;
import com.xihe.entity.ClientInfo;
import com.xihe.entity.Company;
import com.xihe.entity.Result;
import com.xihe.services.impl.K5ServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Author gzy
 * @Date 2024/8/29 18:17
 */
@RestController
@RequestMapping("/k5")
public class K5RegisterController {
    //公共token
    public static final String commonToken = "bc2HsFK6TyPRXYY";

    @Resource
    private K5ServiceImpl thirdService;

    @GetMapping(value = "/test")
    public Result<String> test() {
        return Result.success("测试成功");
    }

    @Operation(summary = "获取所请求K5的公司信息")
    @PostMapping(value = "/getCompany")
    public Result<Company> getCompany(String requestToken) {
        return Result.success(thirdService.getCompany(requestToken));
    }

    @Operation(summary = "获取所有的客户等级")
    @PostMapping(value = "/getClientGrade")
    public Result<JSONArray> getClientGrade(String requestToken, String companyCode) {
        return Result.success(thirdService.getClientGrade(requestToken, companyCode));
    }

    @Operation(summary = "客户注册")
    @PostMapping(value = "/addRegister")
    public Result<Integer> addRegister(@Valid ClientInfo clientInfo, String requestToken) {
        return Result.success(thirdService.addRegister(clientInfo, requestToken));
    }

    @Operation(summary = "客户更新")
    @PostMapping(value = "/updateClient")
    public Result<Integer> updateClient(ClientInfo clientInfo, String requestToken) {
        return Result.success(thirdService.updateClient(clientInfo, requestToken));
    }

    @Operation(summary = "客户删除")
    @PostMapping(value = "/deleteClient")
    public Result<Integer> deleteClient(String clientCode, String requestToken) {
        return Result.success(thirdService.deleteClient(clientCode, requestToken));
    }

    @Operation(summary = "获取所有的K5客户")
    @PostMapping(value = "/getAllClientInfo")
    public Result<List<ClientInfo>> getAllClientInfo(String requestToken) {
        return Result.success(thirdService.getAllClientInfo(requestToken));
    }

    @Operation(summary = "获取所有的K5渠道")
    @PostMapping(value = "/getAllChannelInfo")
    public Result<List<ChannelInfo>> getAllChannelInfo(String requestToken) {
        return Result.success(thirdService.getAllChannelInfo(requestToken));
    }

    @Operation(summary = "获取所有的K5起运地")
    @PostMapping(value = "/getAllBaseOrigin")
    public Result<JSONArray> getAllBaseOrigin(String requestToken) {
        return Result.success(thirdService.getAllBaseOrigin(requestToken));
    }

    @Operation(summary = "同步第三方订单")
    @PostMapping(value = "/synchronizeOrder")
    public Result<JSONArray> synchronizeOrder(String requestToken, int type, String startTime, String endTime) {
        return Result.success(thirdService.synchronizeOrder(requestToken, type, startTime, endTime));
    }

    @Operation(summary = "同步第三方接口")
    @PostMapping(value = "/synchronizeOrderInter")
    public Result<JSONArray> synchronizeOrder(String requestToken) {
        return Result.success(thirdService.synchronizeOrder(requestToken));
    }

    @Operation(summary = "同步运单状态")
    @PostMapping(value = "/synchronizeBill")
    public Result<JSONArray> synchronizeBill(String requestToken, String startTime, String endTime) {
        return Result.success(thirdService.synchronizeBill(requestToken, startTime, endTime));
    }

    @Operation(summary = "同步K5客户等级")
    @PostMapping(value = "/synchronizeClientLevel")
    public Result<JSONArray> synchronizeClientLevel(String requestToken) {
        return Result.success(thirdService.synchronizeClientLevel(requestToken));
    }

    @Operation(summary = "获取K5所有的客户附件")
    @PostMapping(value = "/getAllClientAttach")
    public Result<JSONArray> getAllClientAttach(String requestToken) {
        return Result.success(thirdService.getAllClientAttach(requestToken));
    }

    @Operation(summary = "K5上传客户附件")
    @PostMapping(value = "/addClientAttach")
    public Result<Integer> addClientAttach(String requestToken, String attachArr) {
        return Result.success(thirdService.addClientAttach(requestToken, attachArr));
    }

    @Operation(summary = "K5修改客户附件")
    @PostMapping(value = "/updateClientAttach")
    public Result<Integer> updateClientAttach(String requestToken, String attachArr) {
        return Result.success(thirdService.updateClientAttach(requestToken, attachArr));
    }

    @Operation(summary = "K5删除客户附件")
    @PostMapping(value = "/deleteClientAttach")
    public Result<Integer> deleteClientAttach(String requestToken, String attachArr) {
        return Result.success(thirdService.deleteClientAttach(requestToken, attachArr));
    }
}
