package su.nightexpress.quests.util;

import org.jetbrains.annotations.NotNull;
import su.nightexpress.quests.quest.definition.QuestPeriod;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Calendar;

/**
 * 任务周期时间工具类
 */
public class PeriodUtil {
    
    /**
     * 获取指定周期的下一个重置时间
     * @param period 任务周期
     * @return 下一个重置时间的时间戳（毫秒）
     */
    public static long getNextResetTime(@NotNull QuestPeriod period) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextReset;
        
        switch (period) {
            case DAILY:
                // 下一天的凌晨0点
                nextReset = now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                break;
                
            case WEEKLY:
                // 下周一的凌晨0点
                nextReset = now.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
                break;
                
            case MONTHLY:
                // 下个月1号的凌晨0点
                nextReset = now.plusMonths(1)
                    .withDayOfMonth(1)
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
                break;
                
            case SEASONAL:
                // 赛季任务不自动重置，由战斗通行证系统控制
                // 返回一个很远的未来时间
                nextReset = now.plusYears(100);
                break;
                
            default:
                nextReset = now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                break;
        }
        
        return nextReset.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    
    /**
     * 检查是否需要重置任务
     * @param lastResetTime 上次重置时间
     * @param period 任务周期
     * @return 是否需要重置
     */
    public static boolean shouldReset(long lastResetTime, @NotNull QuestPeriod period) {
        if (lastResetTime <= 0) {
            return true;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // 如果当前时间已经超过了上次重置时间，说明需要重置
        return currentTime >= lastResetTime;
    }
    
    /**
     * 获取当前周期的开始时间
     * @param period 任务周期
     * @return 周期开始时间的时间戳（毫秒）
     */
    public static long getPeriodStartTime(@NotNull QuestPeriod period) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStart;
        
        switch (period) {
            case DAILY:
                // 今天的凌晨0点
                periodStart = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
                break;
                
            case WEEKLY:
                // 本周一的凌晨0点
                periodStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
                break;
                
            case MONTHLY:
                // 本月1号的凌晨0点
                periodStart = now.withDayOfMonth(1)
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
                break;
                
            case SEASONAL:
                // 赛季任务返回0，由战斗通行证系统控制
                return 0;
                
            default:
                periodStart = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
                break;
        }
        
        return periodStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    
    /**
     * 获取周期的显示文本
     * @param period 任务周期
     * @return 显示文本
     */
    @NotNull
    public static String getPeriodDisplayText(@NotNull QuestPeriod period) {
        switch (period) {
            case DAILY:
                return "每日";
            case WEEKLY:
                return "每周";
            case MONTHLY:
                return "每月";
            case SEASONAL:
                return "赛季";
            default:
                return "未知";
        }
    }
    
    /**
     * 获取距离下次重置的剩余时间（秒）
     * @param period 任务周期
     * @return 剩余秒数
     */
    public static long getTimeUntilReset(@NotNull QuestPeriod period) {
        long nextReset = getNextResetTime(period);
        long current = System.currentTimeMillis();
        return Math.max(0, (nextReset - current) / 1000);
    }
}
