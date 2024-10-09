package com.xihe.controller;

import com.alibaba.fastjson2.JSONArray;
import com.fasterxml.jackson.databind.JsonNode;
import com.xihe.entity.CheckHome;
import com.xihe.entity.Result;
import com.xihe.services.CityPostService;
import com.xihe.services.impl.FedexFuel;
import com.xihe.services.impl.FuelImpl;
import com.xihe.services.impl.UpsFuel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author gzy
 * @Date 2024/8/15 15:34
 */
@Controller
@RequestMapping("/cityPost")
@Tag(name = "偏远")
public class CityPostController {
    @Resource
    UpsFuel upsFuel;
    @Resource
    FedexFuel fedexFuel;
    @Resource
    FuelImpl fuel;
    @Resource
    CityPostService cityPostService;

    @Operation(summary = "根据省/州判断地址中是否存在,若存在则切割地址组")
    @PostMapping(value = "/interceptAddress")
    @ResponseBody
    public Result<JsonNode> interceptAddress(@Parameter(description = "全部的地址") String address) {
        return Result.success(cityPostService.interceptAddress(address));
    }

    @Operation(summary = "校验私宅（0 未分类，1 商业，2 住宅，3 混合）")
    @PostMapping("/checkHome")
    @ResponseBody
    public Result<Integer> checkHome(@Valid @RequestBody CheckHome checkHome) {
        if ("ups".equals(checkHome.getType())) {
            return Result.success(upsFuel.getCheckHome(checkHome));
        } else if ("fedex".equals(checkHome.getType())) {
            return Result.success(fedexFuel.getCheckHome(checkHome));
        }

        return null;
    }

    @Operation(summary = "所有最新燃油查询")
    @GetMapping
    @ResponseBody
    public Result<JSONArray> getAllFuel() {
        return Result.success(fuel.getAllFuel());
    }

    @Operation(summary = "根据燃油类型获取燃油")
    @GetMapping("/getFuel/{fuelKey}")
    @ResponseBody
    public JsonNode getFuel(@PathVariable String fuelKey) {
        return fuel.getCommonFuel(fuelKey);
    }

    @RequestMapping("/index")
    public String CommercialResidence() {
        return "index";
    }

    @RequestMapping("/fuel")
    public String fuel() {
        return "fuel";
    }

    @GetMapping("/test")
    @ResponseBody
    public Result<String> test() {
        return Result.success("test");
    }

    @Operation(summary = "模糊查询仓库代码")
    @GetMapping("/getStatCodeList/{str}")
    @ResponseBody
    public Result<List<String>> getStatCodeList(@PathVariable String str) {
        return Result.success(cityPostService.getStatCodeList(str));
    }

    @Operation(summary = "根据仓库代码查询 地址+城市+省州二字码+邮编")
    @GetMapping("/getCountryCombinationInfoList/{statCode}")
    @ResponseBody
    public Result<List<String>> getCountryCombinationInfoList(@PathVariable String statCode) {
        return Result.success(cityPostService.getCountryCombinationInfoList(statCode));
    }

}
