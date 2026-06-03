package su.nightexpress.quests.quest.menu;

import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.quest.definition.QuestType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 统一的Lore模板管理器
 * 支持条件显示，根据任务类型动态生成Lore
 */
public class LoreTemplateManager {
    
    private final QuestsPlugin plugin;
    private List<String> questLoreTemplate;
    private String questNameFormat;
    private Map<String, String> questNameFormatByStatus;
    
    public LoreTemplateManager(@NotNull QuestsPlugin plugin) {
        this.plugin = plugin;
        this.load();
    }
    
    /**
     * 加载模板配置
     */
    public void load() {
        FileConfig config = FileConfig.loadOrExtract(this.plugin, "lore_template.yml");
        
        this.questLoreTemplate = ConfigValue.create("Quest_Lore", 
            Arrays.asList(
                "[ALL] <gray>%quest_description%",
                "[ALL] ",
                "[ALL] <gold>任务目标:",
                "[ALL] %objectives%",
                "[ALL] ",
                "[ALL] <gold>进度: <white>%progress%<gold>%",
                "[ALL] <gold>任务奖励: <yellow>%rewards_inline%"
            )
        ).read(config);
        
        this.questNameFormat = ConfigValue.create("Quest_Name_Format", "<gold>%quest_name%").read(config);
        
        this.questNameFormatByStatus = new HashMap<>();
        if (config.contains("Quest_Name_Format_By_Status")) {
            for (String key : config.getSection("Quest_Name_Format_By_Status")) {
                String format = config.getString("Quest_Name_Format_By_Status." + key);
                if (format != null) {
                    this.questNameFormatByStatus.put(key.toUpperCase(), format);
                }
            }
        }
    }
    
    /**
     * 根据任务类型生成Lore
     * @param questType 任务类型
     * @return 过滤后的Lore模板列表
     */
    @NotNull
    public List<String> generateQuestLore(@NotNull QuestType questType) {
        return generateQuestLore(questType, null);
    }
    
    /**
     * 根据任务类型和状态生成Lore
     * @param questType 任务类型
     * @param status 任务状态（可选）
     * @return 过滤后的Lore模板列表
     */
    @NotNull
    public List<String> generateQuestLore(@NotNull QuestType questType, String status) {
        List<String> result = new ArrayList<>();
        
        for (String line : this.questLoreTemplate) {
            if (shouldDisplayLine(line, questType, status)) {
                // 移除条件标签，只保留实际内容
                String content = removeConditionTag(line);
                result.add(content);
            }
        }
        
        return result;
    }
    
    /**
     * 判断某一行是否应该显示
     * @param line 模板行
     * @param questType 任务类型
     * @param status 任务状态
     * @return 是否显示
     */
    private boolean shouldDisplayLine(@NotNull String line, @NotNull QuestType questType, String status) {
        // 如果没有条件标签，默认显示
        if (!line.contains("[")) {
            return true;
        }
        
        // 提取条件标签
        String condition = extractCondition(line);
        if (condition == null || condition.isEmpty()) {
            return true;
        }
        
        // [ALL] 总是显示
        if (condition.equals("ALL")) {
            return true;
        }
        
        // 多条件支持（用逗号分隔）
        String[] conditions = condition.split(",");
        for (String cond : conditions) {
            cond = cond.trim().toUpperCase();
            
            // 检查任务类型匹配
            if (matchesQuestType(cond, questType)) {
                return true;
            }
            
            // 检查状态匹配
            if (status != null && cond.equals(status.toUpperCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查条件是否匹配任务类型
     */
    private boolean matchesQuestType(@NotNull String condition, @NotNull QuestType questType) {
        return switch (questType) {
            case INDEPENDENT -> condition.equals("INDEPENDENT");
            case COOPERATIVE -> condition.equals("COOPERATIVE");
            case COMPETITIVE -> condition.equals("COMPETITIVE");
        };
    }
    
    /**
     * 提取条件标签
     * 例如: "[ALL] 内容" -> "ALL"
     * 例如: "[COOPERATIVE,COMPETITIVE] 内容" -> "COOPERATIVE,COMPETITIVE"
     */
    private String extractCondition(@NotNull String line) {
        int start = line.indexOf('[');
        int end = line.indexOf(']');
        
        if (start >= 0 && end > start) {
            return line.substring(start + 1, end).trim();
        }
        
        return null;
    }
    
    /**
     * 移除条件标签，只保留内容
     * 例如: "[ALL] <gray>描述" -> "<gray>描述"
     */
    @NotNull
    private String removeConditionTag(@NotNull String line) {
        int end = line.indexOf(']');
        if (end >= 0 && end < line.length() - 1) {
            return line.substring(end + 1).trim();
        }
        return line;
    }
    
    /**
     * 获取任务名称格式
     * @param status 任务状态（可选）
     * @return 格式化字符串
     */
    @NotNull
    public String getQuestNameFormat(String status) {
        if (status != null) {
            String format = this.questNameFormatByStatus.get(status.toUpperCase());
            if (format != null) {
                return format;
            }
        }
        return this.questNameFormat;
    }
    
    /**
     * 获取原始模板（用于调试）
     */
    @NotNull
    public List<String> getRawTemplate() {
        return new ArrayList<>(this.questLoreTemplate);
    }
}

