package su.nightexpress.quests.quest.refresh;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;

/**
 * Cron 表达式工具类
 * 支持标准 Cron 格式：秒 分 时 日 月 星期
 */
public class CronUtil {
    
    public static boolean shouldExecute(@NotNull String cron, long lastExecutionTime) {
        long nextTime = getNextExecutionTime(cron, lastExecutionTime);
        long currentTime = System.currentTimeMillis();
        return currentTime >= nextTime;
    }
    
    public static long getNextExecutionTime(@NotNull String cron) {
        return getNextExecutionTime(cron, System.currentTimeMillis());
    }
    
    public static long getNextExecutionTime(@NotNull String cron, long fromTime) {
        try {
            String[] parts = cron.trim().split("\\s+");
            if (parts.length != 6) {
                return fromTime + 86400000L; // 默认1天后
            }
            
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(fromTime);
            cal.add(Calendar.MINUTE, 1);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            
            for (int i = 0; i < 1000; i++) {
                if (matches(cron, cal)) {
                    return cal.getTimeInMillis();
                }
                cal.add(Calendar.MINUTE, 1);
            }
            
            return fromTime + 86400000L;
        } catch (Exception e) {
            return fromTime + 86400000L;
        }
    }
    
    private static boolean matches(@NotNull String cron, @NotNull Calendar cal) {
        String[] parts = cron.split("\\s+");
        
        int second = cal.get(Calendar.SECOND);
        int minute = cal.get(Calendar.MINUTE);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH) + 1;
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
        
        return matchPart(parts[0], second, 0, 59) &&
               matchPart(parts[1], minute, 0, 59) &&
               matchPart(parts[2], hour, 0, 23) &&
               matchPart(parts[3], day, 1, 31) &&
               matchPart(parts[4], month, 1, 12) &&
               matchPart(parts[5], dayOfWeek, 0, 6);
    }
    
    private static boolean matchPart(@NotNull String part, int value, int min, int max) {
        if (part.equals("*") || part.equals("?")) {
            return true;
        }
        
        if (part.contains(",")) {
            String[] values = part.split(",");
            for (String v : values) {
                if (matchPart(v, value, min, max)) {
                    return true;
                }
            }
            return false;
        }
        
        if (part.contains("-")) {
            String[] range = part.split("-");
            int start = Integer.parseInt(range[0]);
            int end = Integer.parseInt(range[1]);
            return value >= start && value <= end;
        }
        
        if (part.contains("/")) {
            String[] step = part.split("/");
            int start = step[0].equals("*") ? min : Integer.parseInt(step[0]);
            int interval = Integer.parseInt(step[1]);
            return (value - start) % interval == 0 && value >= start;
        }
        
        try {
            return value == Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

