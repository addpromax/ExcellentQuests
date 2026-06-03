package su.nightexpress.quests.task.listener.type;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.task.TaskManager;
import su.nightexpress.quests.task.TaskType;
import su.nightexpress.quests.task.adapter.AdapterFamily;
import su.nightexpress.quests.task.listener.TaskListener;

/**
 * 使用激流任务监听器
 */
public class UseRiptideTaskListener extends TaskListener<ItemStack, AdapterFamily<ItemStack>> {

    public UseRiptideTaskListener(@NotNull QuestsPlugin plugin, @NotNull TaskManager manager, @NotNull TaskType<ItemStack, AdapterFamily<ItemStack>> taskType) {
        super(plugin, manager, taskType);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident)) return;
        
        Trident trident = (Trident) event.getEntity();
        if (!(trident.getShooter() instanceof Player)) return;

        Player player = (Player) trident.getShooter();
        if (!this.manager.canDoTasks(player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getEnchantmentLevel(Enchantment.RIPTIDE) > 0) {
            this.progressQuests(player, item);
        }
    }
}

