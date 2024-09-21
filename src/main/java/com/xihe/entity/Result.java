package com.xihe.entity;

import org.springframework.http.HttpStatus;

import java.io.Serializable;

public class Result<T> implements Serializable {
    //返回结果key，200成功，198失败，500异常
    private int code;
    //返回提示信息
    private String msg;
    //返回的数据
    private T data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> failure() {
        return failure(HttpStatus.BAD_REQUEST.value(),HttpStatus.BAD_REQUEST.getReasonPhrase());
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase(), data);
    }

    public static <T> Result<T> failure(int errorCode, String errorMsg) {
        return failure(errorCode, errorMsg, null);
    }

    public static <T> Result<T> failure(int code, String errorMsg, T data) {
        return new Result<>(code, errorMsg, data);
    }

    public Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

}