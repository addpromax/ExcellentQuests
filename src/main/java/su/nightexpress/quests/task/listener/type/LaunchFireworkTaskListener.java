package su.nightexpress.quests.task.listener.type;

import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.task.TaskManager;
import su.nightexpress.quests.task.TaskType;
import su.nightexpress.quests.task.adapter.AdapterFamily;
import su.nightexpress.quests.task.listener.TaskListener;

/**
 * 发射烟花任务监听器
 */
public class LaunchFireworkTaskListener extends TaskListener<ItemStack, AdapterFamily<ItemStack>> {

    public LaunchFireworkTaskListener(@NotNull QuestsPlugin plugin, @NotNull TaskManager manager, @NotNull TaskType<ItemStack, AdapterFamily<ItemStack>> taskType) {
        super(plugin, manager, taskType);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof Firework)) return;

        Firework firework = (Firework) event.getEntity();
        if (!(firework.getShooter() instanceof Player)) return;

        Player player = (Player) firework.getShooter();
        if (!this.manager.canDoTasks(player)) return;

        ItemStack fireworkItem = new ItemStack(org.bukkit.Material.FIREWORK_ROCKET);
        this.progressQuests(player, fireworkItem);
    }
}

