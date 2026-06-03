package su.nightexpress.quests.task.listener.type;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.quest.data.QuestData;
import su.nightexpress.quests.quest.definition.Quest;
import su.nightexpress.quests.milestone.data.MilestoneData;
import su.nightexpress.quests.milestone.definition.Milestone;
import su.nightexpress.quests.task.adapter.AdapterFamily;
import su.nightexpress.quests.task.TaskManager;
import su.nightexpress.quests.task.TaskType;
import su.nightexpress.quests.task.listener.TaskListener;
import su.nightexpress.quests.user.QuestUser;
import su.nightexpress.quests.util.FilterMatcher;

import java.util.List;
import java.util.Map;

public class EnchantingTaskListener extends TaskListener<Enchantment, AdapterFamily<Enchantment>> {

    public EnchantingTaskListener(@NotNull QuestsPlugin plugin, @NotNull TaskManager manager, @NotNull TaskType<Enchantment, AdapterFamily<Enchantment>> taskType) {
        super(plugin, manager, taskType);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTaskEnchanting(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        if (!this.manager.canDoTasks(player)) return;

        ItemStack item = event.getItem();
        QuestUser user = this.plugin.getUserManager().getOrFetch(player);
        
        event.getEnchantsToAdd().forEach((enchantment, level) -> {
            // 检查任务筛选器
            boolean questFiltered = checkQuestFilters(user, item);
            if (!questFiltered) {
                // 没有通过筛选，不计入任务进度
                return;
            }
            
            // 检查里程碑筛选器
            boolean milestoneFiltered = checkMilestoneFilters(user, item);
            if (!milestoneFiltered) {
                // 没有通过筛选，不计入里程碑进度
                return;
            }
            
            // 通过筛选，正常计入进度
            this.progressQuests(player, enchantment);
        });
    }
    
    /**
     * 检查任务筛选器
     * @return true 如果通过筛选或没有筛选器
     */
    private boolean checkQuestFilters(@NotNull QuestUser user, @NotNull ItemStack item) {
        for (QuestData questData : user.getQuestDatas()) {
            // 独立任务不检查过期（由周期控制）
            if (!questData.isActive()) {
                continue;
            }
            
            Quest quest = this.plugin.questManager().map(qm -> qm.getQuestById(questData.getQuestId())).orElse(null);
            if (quest == null || quest.getType() != this.taskType) {
                continue;
            }
            
            // 检查筛选器
            Map<String, List<String>> filters = quest.getFilters();
            if (filters == null || filters.isEmpty()) {
                return true; // 没有筛选器，通过
            }
            
            List<String> itemFilters = filters.get("Items");
            if (itemFilters == null || itemFilters.isEmpty()) {
                return true; // 没有物品筛选器，通过
            }
            
            // 检查物品是否匹配筛选器
            if (!FilterMatcher.matchesItemFilter(itemFilters, item)) {
                return false; // 不匹配，不通过
            }
        }
        
        return true;
    }
    
    /**
     * 检查里程碑筛选器
     * @return true 如果通过筛选或没有筛选器
     */
    private boolean checkMilestoneFilters(@NotNull QuestUser user, @NotNull ItemStack item) {
        for (MilestoneData milestoneData : user.getMilestoneDatas()) {
            Milestone milestone = this.plugin.milestoneManager().map(mm -> mm.getMilestoneById(milestoneData.getMilestoneId())).orElse(null);
            if (milestone == null || milestone.getType() != this.taskType) {
                continue;
            }
            
            // 检查筛选器
            Map<String, List<String>> filters = milestone.getFilters();
            if (filters == null || filters.isEmpty()) {
                return true; // 没有筛选器，通过
            }
            
            List<String> itemFilters = filters.get("Items");
            if (itemFilters == null || itemFilters.isEmpty()) {
                return true; // 没有物品筛选器，通过
            }
            
            // 检查物品是否匹配筛选器
            if (!FilterMatcher.matchesItemFilter(itemFilters, item)) {
                return false; // 不匹配，不通过
            }
        }
        
        return true;
    }
}
