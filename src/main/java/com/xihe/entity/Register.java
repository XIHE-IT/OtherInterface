package com.xihe.entity;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author gzy
 * @Date 2024/8/28 16:12
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Register {

    @NotNull(message = "客户账号不能为空")
    @Size(max = 20, message = "客户账号长度不能超过20个字符")
    private String userId;

    @NotNull(message = "客户名称不能为空")
    @Size(max = 20, message = "客户名称长度不能超过20个字符")
    private String clientName;

    @NotNull(message = "客户密码不能为空")
    @Size(max = 20, message = "客户密码长度不能超过20个字符")
    private String passWord;

    @NotNull(message = "token不能为空")
    @Size(max = 50, message = "token长度不能超过50个字符")
    private String token;
}
