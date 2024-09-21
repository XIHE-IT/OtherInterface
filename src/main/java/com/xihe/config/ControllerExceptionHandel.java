package com.xihe.config;

import com.xihe.entity.Result;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class ControllerExceptionHandel {

    @ExceptionHandler(ValidationException.class)
    public Result<String> handleException(ValidationException e) {
//        e.printStackTrace();
        log.error("参数校验发生异常:{}", e.getMessage());
        return Result.failure(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }

    @ExceptionHandler(value = NullPointerException.class)
    public Result<String> exceptionHandler(NullPointerException e) {
//        e.printStackTrace();
        log.error("空指针异常:{}", e.getMessage());
        return Result.failure(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
    }

    @ExceptionHandler(value = {Exception.class, RuntimeException.class})
    public Result<String> exceptionHandler(Exception e) {
//        e.printStackTrace();
        log.error("运行时发生异常:{}", e.getMessage());
        return Result.failure(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public Result<String> handException(AsyncRequestTimeoutException e) {
//        e.printStackTrace();
        log.error("运行时超时异常:{}", e.getMessage());
        return Result.failure(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }

//    @ExceptionHandler(value = Exception.class)
//    public ModelAndView defaultErrorHandler(HttpServletRequest req, HttpServletResponse resp, Exception e) {
//
//        log.error("------------------>捕捉到全局异常", e);
//
//        Enumeration<String> headerNames = req.getHeaderNames();
//        while (headerNames.hasMoreElements()) {
//            String key = (String) headerNames.nextElement();
//            String value = req.getHeader(key);
//            log.info("key {} value {}",key,value);
//        }
//
//        String contentType = req.getHeader("Content-Type");
//        if ((contentType!=null&&contentType.contains("application/json"))||(req.getHeader("X-Requested-With")!= null
//                && req.getHeader("X-Requested-With").contains("XMLHttpRequest") )) {
//            //如果是接口类型的调用,就需要返回json数据
//            try {
//                resp.setCharacterEncoding("utf-8");
//                PrintWriter writer = resp.getWriter();
//                writer.write(e.toString());
//                writer.flush();
//            } catch (IOException i) {
//                i.printStackTrace();
//            }
//            return null;
//        }
//
//        ModelAndView mav = new ModelAndView();
//        mav.addObject("exception", e);
//        mav.addObject("message", e.getMessage());
//        mav.addObject("url", req.getRequestURL());
//        mav.setViewName("error/500");
//        return mav;
//    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public Map handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex){
        BindingResult bindingResult = ex.getBindingResult();
        List<FieldError> errors = bindingResult.getFieldErrors();
        //初始化错误信息大小
        Map<String,String> errorMessage = new HashMap<>(errors.size());
        for (FieldError error: errors) {
            log.error(error.toString());
            errorMessage.put(error.getField(), error.getDefaultMessage());
        }
        Map m=new HashMap();
        m.put("error",errorMessage.toString());
        return m;
    }
}
