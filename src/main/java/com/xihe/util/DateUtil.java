package com.xihe.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * @Author gzy
 * @Date 2024/8/14 14:26
 */
public class DateUtil {
    // 静态映射表，用于将中文月份名称映射到数字月份
    private static final Map<String, String> chineseMonthToNumberMap = new HashMap<>();

    static {
        // 初始化映射表
        chineseMonthToNumberMap.put("一月", "01");
        chineseMonthToNumberMap.put("二月", "02");
        chineseMonthToNumberMap.put("三月", "03");
        chineseMonthToNumberMap.put("四月", "04");
        chineseMonthToNumberMap.put("五月", "05");
        chineseMonthToNumberMap.put("六月", "06");
        chineseMonthToNumberMap.put("七月", "07");
        chineseMonthToNumberMap.put("八月", "08");
        chineseMonthToNumberMap.put("九月", "09");
        chineseMonthToNumberMap.put("十月", "10");
        chineseMonthToNumberMap.put("十一月", "11");
        chineseMonthToNumberMap.put("十二月", "12");
    }

    /**
     * 获取上一周的周一
     *
     * @param date 给一个当前时间
     * @return java.util.Date
     * @author gzy
     * @date 2024/8/14 14:29
     */
    public static Date getLastWeekMonday(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(getThisWeekMonday(date));
        calendar.add(Calendar.DATE, -7);
        return calendar.getTime();
    }

    /**
     * 获取当前一周的周一
     *
     * @param date 给一个当前时间
     * @return java.util.Date
     * @author gzy
     * @date 2024/8/14 14:29
     */
    public static Date getThisWeekMonday(Date date) {
        Calendar nowCalendar = Calendar.getInstance();
        nowCalendar.setTime(date);
        //获取当前日期是一个星期的第几天
        int dayWeek = nowCalendar.get(Calendar.DAY_OF_WEEK);
        if (1 == dayWeek) {
            nowCalendar.add(Calendar.DAY_OF_MONTH, -1);
        }
        //设置一个星期的第一天，按中国的习惯一个星期的第一天是星期一
        nowCalendar.setFirstDayOfWeek(Calendar.MONDAY);
        // 获得当前日期是一个星期的第几天
        int day = nowCalendar.get(Calendar.DAY_OF_WEEK);
        //根据日历的规则，给当前日期减去星期几与一个星期第一天的差值
        nowCalendar.add(Calendar.DATE, nowCalendar.getFirstDayOfWeek() - day);
        //把时分秒全部置零
        nowCalendar.set(Calendar.HOUR_OF_DAY, 0);
        nowCalendar.set(Calendar.MINUTE, 0);
        nowCalendar.set(Calendar.SECOND, 0);
        return nowCalendar.getTime();
    }

    /**
     * 获取下一周的周一
     *
     * @param date 给一个当前时间
     * @return java.util.Date
     * @author gzy
     * @date 2024/8/14 14:29
     */
    public static Date getNextWeekMonday(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(getThisWeekMonday(date));
        calendar.add(Calendar.DATE, 7);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
     * 获取下个月第一天
     *
     * @return java.util.Date
     * @author gzy
     * @date 2024/10/11 14:18
     */
    public static Date getNextMonthDay() {
        // 创建一个GregorianCalendar实例，默认使用当前日期和时间
        Calendar calendar = new GregorianCalendar();
        // 获取当前月份
        int currentMonth = calendar.get(Calendar.MONTH);
        // 设置月份为下一个月（注意：Calendar的月份是从0开始的，即0代表1月）
        calendar.set(Calendar.MONTH, currentMonth + 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        // 为了确保是下个月的第一天，我们需要将日设置为1
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTime();
    }

    /**
     * 获取两个时间的时间差
     *
     * @param date1 前面的时间
     * @param date2 后面的时间
     * @return java.lang.Long
     * @author gzy
     * @date 2024/8/14 14:41
     */
    public static Long getDifference(Date date1, Date date2) {
        return date2.getTime() - date1.getTime();
    }

    /**
     * 获取当前时间距离下个月1号的时间
     *
     * @return java.lang.Long
     * @author gzy
     * @date 2024/9/10 11:09
     */
    public static Long getMonthDifference(int monthNum) {
        // 获取当前日期
        LocalDate today = LocalDate.now();

        // 计算下个月的第一天
        LocalDate nextMonthFirst = today.plusMonths(monthNum).withDayOfMonth(1);

        // 转换为Instant（如果需要时间戳的话）
        // 注意：这里我们假设我们想要的是UTC时间的时间戳
        // 如果你想要的是特定时区的时间戳，需要指定ZoneId
        Instant now = Instant.now();
        Instant nextMonthFirstInstant = nextMonthFirst.atStartOfDay(ZoneId.systemDefault()).toInstant();

        // 如果你想要的是特定时区的时间戳，可以这样指定ZoneId
        // Instant nextMonthFirstInstantWithZone = nextMonthFirst.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant();

        // 计算时间戳差值（毫秒）
//        long differenceInMillis = java.time.Duration.between(now, nextMonthFirstInstant).toMillis();

        // 输出结果
//        System.out.println("当前时间戳: " + now.toEpochMilli());
//        System.out.println("下个月一号的时间戳: " + nextMonthFirstInstant.toEpochMilli());
//        System.out.println("时间戳差值（毫秒）: " + differenceInMillis);

        // 如果你想要秒为单位的时间戳差值
        //        System.out.println("时间戳差值（秒）: " + differenceInSeconds);
        return java.time.Duration.between(now, nextMonthFirstInstant).getSeconds();
    }

    /**
     * 将中文月份名称转换为数字月份
     *
     * @param chineseMonth 中文月份名称
     * @return 对应的数字月份，如果转换失败则返回null
     */
    public static String chineseMonthToNumber(String chineseMonth) {
        return chineseMonthToNumberMap.get(chineseMonth);
    }

    /**
     * 根据日期获取是星期几
     *
     * @param date 一个日期
     * @return java.lang.String
     * @author gzy
     * @date 2024/10/25 11:56
     */
    public static String getDayOfWeekString(Date date) {
        // 使用Calendar获取星期几
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        return switch (dayOfWeek) {
            case Calendar.SUNDAY -> "星期日";
            case Calendar.MONDAY -> "星期一";
            case Calendar.TUESDAY -> "星期二";
            case Calendar.WEDNESDAY -> "星期三";
            case Calendar.THURSDAY -> "星期四";
            case Calendar.FRIDAY -> "星期五";
            case Calendar.SATURDAY -> "星期六";
            default -> "未知";
        };
    }

    public static void main(String[] args) {
//        System.out.println(getLastWeekMonday(new Date()));
//        System.out.println(getThisWeekMonday(new Date()));
//        System.out.println(getNextWeekMonday(new Date()));
//        System.out.println(getNextMonthDay());
//        System.out.println(DateUtil.getMonthDifference(2));
//        System.out.println(DateUtil.getNextWeekMonday(new Date()));

//        String time = "2024/10/14";
//        SimpleDateFormat thirdDateFormat = new SimpleDateFormat("yyyy/MM/dd");
//        long tempRateTime = 0;
//        try {
//            tempRateTime = thirdDateFormat.parse(time).getTime();
//        } catch (ParseException e) {
//            throw new RuntimeException(e);
//        }
//        Date nowDate = new Date();
//        Date nextWeekMonday = DateUtil.getNextWeekMonday(nowDate);
//        long nextMondayTime = nextWeekMonday.getTime();
//        if (tempRateTime >= nextMondayTime) {
//            System.out.println(DateUtil.getDifference(new Date(), nextWeekMonday) / 1000 + (5 * 24 * 60 * 60));
//        }
        System.out.println(getDayOfWeekString(new Date()));
    }
}
