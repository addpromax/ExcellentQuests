package su.nightexpress.quests.quest.refresh;

import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.quests.quest.definition.QuestType;

import java.util.*;

/**
 * 任务刷新配置类 - 基于 Cron 表达式
 */
public class QuestRefreshConfig {
    
    private String independentCron;
    private String cooperativeCron;
    private String competitiveCron;
    
    private int independentQuestCount;
    private int cooperativeQuestCount;
    private int competitiveQuestCount;
    
    private boolean separateRefreshEnabled;
    
    // 基于在线人数的动态任务数量配置
    private boolean dynamicQuestCountEnabled;
    private Map<Integer, Integer> cooperativeQuestCountByOnline;
    private Map<Integer, Integer> competitiveQuestCountByOnline;
    
    private long lastIndependentRefresh;
    private long lastCooperativeRefresh;
    private long lastCompetitiveRefresh;
    
    public QuestRefreshConfig() {
        this.independentCron = "0 0 0 * * ?";  // 每天 00:00
        this.cooperativeCron = "0 0 0 * * ?";  // 每天 00:00
        this.competitiveCron = "0 0 0,12 * * ?";  // 每天 00:00 和 12:00
        
        this.independentQuestCount = 5;
        this.cooperativeQuestCount = 2;
        this.competitiveQuestCount = 2;
        
        this.separateRefreshEnabled = true;
        this.dynamicQuestCountEnabled = false;
        
        // 默认配置：根据在线人数阶梯式增加任务数量
        this.cooperativeQuestCountByOnline = new LinkedHashMap<>();
        this.cooperativeQuestCountByOnline.put(1, 2);   // 1-9人：2个任务
        this.cooperativeQuestCountByOnline.put(10, 3);  // 10-19人：3个任务
        this.cooperativeQuestCountByOnline.put(20, 5);  // 20-29人：5个任务
        this.cooperativeQuestCountByOnline.put(30, 7);  // 30+人：7个任务
        
        this.competitiveQuestCountByOnline = new LinkedHashMap<>();
        this.competitiveQuestCountByOnline.put(1, 2);   // 1-9人：2个任务
        this.competitiveQuestCountByOnline.put(10, 3);  // 10-19人：3个任务
        this.competitiveQuestCountByOnline.put(20, 5);  // 20-29人：5个任务
        this.competitiveQuestCountByOnline.put(30, 7);  // 30+人：7个任务
        
        // 初始化为当前时间，避免首次检查时立即刷新
        long currentTime = System.currentTimeMillis();
        this.lastIndependentRefresh = currentTime;
        this.lastCooperativeRefresh = currentTime;
        this.lastCompetitiveRefresh = currentTime;
    }
    
    public static QuestRefreshConfig load(@NotNull FileConfig config) {
        QuestRefreshConfig refreshConfig = new QuestRefreshConfig();
        
        refreshConfig.independentCron = config.getString("QuestRefresh.IndependentQuests.Cron", "0 0 0 * * ?");
        refreshConfig.cooperativeCron = config.getString("QuestRefresh.CooperativeQuests.Cron", "0 0 0 * * ?");
        refreshConfig.competitiveCron = config.getString("QuestRefresh.CompetitiveQuests.Cron", "0 0 0,12 * * ?");
        
        refreshConfig.independentQuestCount = config.getInt("QuestRefresh.IndependentQuests.Count", 5);
        refreshConfig.cooperativeQuestCount = config.getInt("QuestRefresh.CooperativeQuests.Count", 2);
        refreshConfig.competitiveQuestCount = config.getInt("QuestRefresh.CompetitiveQuests.Count", 2);
        
        refreshConfig.separateRefreshEnabled = config.getBoolean("QuestRefresh.SeparateRefreshEnabled", true);
        
        // 加载动态任务数量配置
        refreshConfig.dynamicQuestCountEnabled = config.getBoolean("QuestRefresh.DynamicQuestCount.Enabled", false);
        
        // 加载合作任务的在线人数阈值配置
        refreshConfig.cooperativeQuestCountByOnline = new LinkedHashMap<>();
        if (config.contains("QuestRefresh.DynamicQuestCount.CooperativeQuests")) {
            var section = config.getConfigurationSection("QuestRefresh.DynamicQuestCount.CooperativeQuests");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    try {
                        int onlineCount = Integer.parseInt(key);
                        int questCount = section.getInt(key);
                        refreshConfig.cooperativeQuestCountByOnline.put(onlineCount, questCount);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        
        // 如果配置为空，使用默认值
        if (refreshConfig.cooperativeQuestCountByOnline.isEmpty()) {
            refreshConfig.cooperativeQuestCountByOnline.put(1, 2);
            refreshConfig.cooperativeQuestCountByOnline.put(10, 3);
            refreshConfig.cooperativeQuestCountByOnline.put(20, 5);
            refreshConfig.cooperativeQuestCountByOnline.put(30, 7);
        }
        
        // 加载竞争任务的在线人数阈值配置
        refreshConfig.competitiveQuestCountByOnline = new LinkedHashMap<>();
        if (config.contains("QuestRefresh.DynamicQuestCount.CompetitiveQuests")) {
            var section = config.getConfigurationSection("QuestRefresh.DynamicQuestCount.CompetitiveQuests");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    try {
                        int onlineCount = Integer.parseInt(key);
                        int questCount = section.getInt(key);
                        refreshConfig.competitiveQuestCountByOnline.put(onlineCount, questCount);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        
        // 如果配置为空，使用默认值
        if (refreshConfig.competitiveQuestCountByOnline.isEmpty()) {
            refreshConfig.competitiveQuestCountByOnline.put(1, 2);
            refreshConfig.competitiveQuestCountByOnline.put(10, 3);
            refreshConfig.competitiveQuestCountByOnline.put(20, 5);
            refreshConfig.competitiveQuestCountByOnline.put(30, 7);
        }
        
        return refreshConfig;
    }
    
    public void save(@NotNull FileConfig config) {
        config.set("QuestRefresh.IndependentQuests.Cron", independentCron);
        config.set("QuestRefresh.IndependentQuests.Count", independentQuestCount);
        
        config.set("QuestRefresh.CooperativeQuests.Cron", cooperativeCron);
        config.set("QuestRefresh.CooperativeQuests.Count", cooperativeQuestCount);
        
        config.set("QuestRefresh.CompetitiveQuests.Cron", competitiveCron);
        config.set("QuestRefresh.CompetitiveQuests.Count", competitiveQuestCount);
        
        config.set("QuestRefresh.SeparateRefreshEnabled", separateRefreshEnabled);
        
        // 保存动态任务数量配置
        config.set("QuestRefresh.DynamicQuestCount.Enabled", dynamicQuestCountEnabled);
        
        for (Map.Entry<Integer, Integer> entry : cooperativeQuestCountByOnline.entrySet()) {
            config.set("QuestRefresh.DynamicQuestCount.CooperativeQuests." + entry.getKey(), entry.getValue());
        }
        
        for (Map.Entry<Integer, Integer> entry : competitiveQuestCountByOnline.entrySet()) {
            config.set("QuestRefresh.DynamicQuestCount.CompetitiveQuests." + entry.getKey(), entry.getValue());
        }
    }
    
    public boolean shouldRefresh(@NotNull QuestType questType) {
        if (!separateRefreshEnabled && questType != QuestType.INDEPENDENT) {
            return false;
        }
        
        String cron = getCron(questType);
        long lastRefresh = getLastRefresh(questType);
        
        return CronUtil.shouldExecute(cron, lastRefresh);
    }
    
    public void markRefreshed(@NotNull QuestType questType) {
        long currentTime = System.currentTimeMillis();
        
        switch (questType) {
            case INDEPENDENT:
                this.lastIndependentRefresh = currentTime;
                break;
            case COOPERATIVE:
                this.lastCooperativeRefresh = currentTime;
                break;
            case COMPETITIVE:
                this.lastCompetitiveRefresh = currentTime;
                break;
        }
    }
    
    public void markAllRefreshed() {
        long currentTime = System.currentTimeMillis();
        this.lastIndependentRefresh = currentTime;
        this.lastCooperativeRefresh = currentTime;
        this.lastCompetitiveRefresh = currentTime;
    }
    
    public int getQuestCount(@NotNull QuestType questType) {
        switch (questType) {
            case INDEPENDENT:
                return independentQuestCount;
            case COOPERATIVE:
                return cooperativeQuestCount;
            case COMPETITIVE:
                return competitiveQuestCount;
            default:
                return 0;
        }
    }
    
    /**
     * 根据在线人数获取任务数量（动态计算）
     * @param questType 任务类型
     * @param maxOnlinePlayers 当日最高在线人数
     * @return 任务数量
     */
    public int getQuestCountByOnline(@NotNull QuestType questType, int maxOnlinePlayers) {
        // 如果未启用动态数量，返回固定值
        if (!dynamicQuestCountEnabled) {
            return getQuestCount(questType);
        }
        
        Map<Integer, Integer> thresholdMap;
        switch (questType) {
            case COOPERATIVE:
                thresholdMap = cooperativeQuestCountByOnline;
                break;
            case COMPETITIVE:
                thresholdMap = competitiveQuestCountByOnline;
                break;
            default:
                return getQuestCount(questType);
        }
        
        // 根据在线人数查找对应的任务数量
        // 阈值从小到大排序，找到最后一个小于等于当前在线人数的阈值
        int questCount = getQuestCount(questType); // 默认值
        
        List<Integer> thresholds = new ArrayList<>(thresholdMap.keySet());
        Collections.sort(thresholds);
        
        for (int threshold : thresholds) {
            if (maxOnlinePlayers >= threshold) {
                questCount = thresholdMap.get(threshold);
            } else {
                break;
            }
        }
        
        return questCount;
    }
    
    public String getCron(@NotNull QuestType questType) {
        switch (questType) {
            case INDEPENDENT:
                return independentCron;
            case COOPERATIVE:
                return cooperativeCron;
            case COMPETITIVE:
                return competitiveCron;
            default:
                return "0 0 0 * * ?";
        }
    }
    
    public long getLastRefresh(@NotNull QuestType questType) {
        switch (questType) {
            case INDEPENDENT:
                return lastIndependentRefresh;
            case COOPERATIVE:
                return lastCooperativeRefresh;
            case COMPETITIVE:
                return lastCompetitiveRefresh;
            default:
                return 0L;
        }
    }
    
    public long getNextRefreshTime(@NotNull QuestType questType) {
        String cron = getCron(questType);
        return CronUtil.getNextExecutionTime(cron);
    }
    
    @NotNull
    public String getNextRefreshTimeString(@NotNull QuestType questType) {
        long nextTime = getNextRefreshTime(questType);
        long seconds = (nextTime - System.currentTimeMillis()) / 1000;
        
        if (seconds <= 0) {
            return "即将刷新";
        }
        
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        
        if (days > 0) {
            return String.format("%d天%d小时", days, hours);
        } else if (hours > 0) {
            return String.format("%d小时%d分钟", hours, minutes);
        } else {
            return String.format("%d分钟", minutes);
        }
    }
    
    // Getters and Setters
    
    public String getIndependentCron() {
        return independentCron;
    }
    
    public void setIndependentCron(String independentCron) {
        this.independentCron = independentCron;
    }
    
    public String getCooperativeCron() {
        return cooperativeCron;
    }
    
    public void setCooperativeCron(String cooperativeCron) {
        this.cooperativeCron = cooperativeCron;
    }
    
    public String getCompetitiveCron() {
        return competitiveCron;
    }
    
    public void setCompetitiveCron(String competitiveCron) {
        this.competitiveCron = competitiveCron;
    }
    
    public int getIndependentQuestCount() {
        return independentQuestCount;
    }
    
    public void setIndependentQuestCount(int independentQuestCount) {
        this.independentQuestCount = Math.max(0, independentQuestCount);
    }
    
    public int getCooperativeQuestCount() {
        return cooperativeQuestCount;
    }
    
    public void setCooperativeQuestCount(int cooperativeQuestCount) {
        this.cooperativeQuestCount = Math.max(0, cooperativeQuestCount);
    }
    
    public int getCompetitiveQuestCount() {
        return competitiveQuestCount;
    }
    
    public void setCompetitiveQuestCount(int competitiveQuestCount) {
        this.competitiveQuestCount = Math.max(0, competitiveQuestCount);
    }
    
    public boolean isSeparateRefreshEnabled() {
        return separateRefreshEnabled;
    }
    
    public void setSeparateRefreshEnabled(boolean separateRefreshEnabled) {
        this.separateRefreshEnabled = separateRefreshEnabled;
    }
    
    public void setLastIndependentRefresh(long lastIndependentRefresh) {
        this.lastIndependentRefresh = lastIndependentRefresh;
    }
    
    public void setLastCooperativeRefresh(long lastCooperativeRefresh) {
        this.lastCooperativeRefresh = lastCooperativeRefresh;
    }
    
    public void setLastCompetitiveRefresh(long lastCompetitiveRefresh) {
        this.lastCompetitiveRefresh = lastCompetitiveRefresh;
    }
    
    public boolean isDynamicQuestCountEnabled() {
        return dynamicQuestCountEnabled;
    }
    
    public void setDynamicQuestCountEnabled(boolean dynamicQuestCountEnabled) {
        this.dynamicQuestCountEnabled = dynamicQuestCountEnabled;
    }
    
    public Map<Integer, Integer> getCooperativeQuestCountByOnline() {
        return cooperativeQuestCountByOnline;
    }
    
    public Map<Integer, Integer> getCompetitiveQuestCountByOnline() {
        return competitiveQuestCountByOnline;
    }
}
