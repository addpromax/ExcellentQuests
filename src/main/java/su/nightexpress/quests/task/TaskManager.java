package su.nightexpress.quests.task;

import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.manager.AbstractManager;
import su.nightexpress.nightcore.util.Enums;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.PDCUtil;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.blocktracker.PlayerBlockTracker;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.config.Config;
import su.nightexpress.quests.task.adapter.Adapter;
import su.nightexpress.quests.task.adapter.AdapterFamily;
import su.nightexpress.quests.registry.Registries;
import su.nightexpress.quests.task.listener.TaskGlobalListener;
import su.nightexpress.quests.task.listener.type.*;
import su.nightexpress.quests.task.workstation.WorkstationMode;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

public class TaskManager extends AbstractManager<QuestsPlugin> {

    private static final String PLAYER_BLOCK_MARKER = "player_block_marker";

    private final Set<SpawnReason> artificalMobSpawns;

    private final NamespacedKey stationOwnerKey;
    private final NamespacedKey stationModeKey;
    private final NamespacedKey mobSpawnerKey;

    public TaskManager(@NotNull QuestsPlugin plugin) {
        super(plugin);
        this.artificalMobSpawns = new HashSet<>();
        this.stationOwnerKey = new NamespacedKey(plugin, "workstation.owner_id");
        this.stationModeKey = new NamespacedKey(plugin, "workstation.craft_mode");
        this.mobSpawnerKey = new NamespacedKey(plugin, "spawner_mob");
    }

    @Override
    protected void onLoad() {
        this.plugin.info("TaskManager 开始加载...");
        
        if (!Config.ANTI_ABUSE_COUNT_ARTIFICAL_MOBS.get()) {
            this.artificalMobSpawns.addAll(Lists.modify(Config.ANTI_ABUSE_ARTIFICAL_MOB_SPAWNS.get(), str -> Enums.get(str, SpawnReason.class)));
        }

        this.registerTaskTypes();
        this.registerListeners();
        this.addListener(new TaskGlobalListener(this.plugin, this));
        
        this.plugin.info("TaskManager 加载完成");
    }

    @Override
    protected void onShutdown() {

    }

    private void registerListeners() {
        // 从注册表获取任务类型并注册监听器
        // 这样在 reload 时也能正确注册监听器（即使注册表已冻结）
        TaskType<?, ?> placeBlockType = Registries.TASK_TYPE.byKey(TaskTypeId.PLACE_BLOCK);
        if (placeBlockType != null) {
            this.addListener(new BlockPlaceTaskListener(this.plugin, this, (TaskType) placeBlockType));
        }
        
        TaskType<?, ?> breakBlockType = Registries.TASK_TYPE.byKey(TaskTypeId.BREAK_BLOCK);
        if (breakBlockType != null) {
            this.addListener(new BlockBreakTaskListener(this.plugin, this, (TaskType) breakBlockType));
        }
        
        TaskType<?, ?> breedMobType = Registries.TASK_TYPE.byKey(TaskTypeId.BREED_MOB);
        if (breedMobType != null) {
            this.addListener(new BreedingTaskListener(this.plugin, this, (TaskType) breedMobType));
        }
        
        TaskType<?, ?> brewingType = Registries.TASK_TYPE.byKey(TaskTypeId.BREWING);
        if (brewingType != null) {
            this.addListener(new BrewingTaskListener(this.plugin, this, (TaskType) brewingType));
        }
        
        TaskType<?, ?> cookItemType = Registries.TASK_TYPE.byKey(TaskTypeId.COOK_ITEM);
        if (cookItemType != null) {
            this.addListener(new CookingTaskListener(this.plugin, this, (TaskType) cookItemType));
        }
        
        TaskType<?, ?> craftItemType = Registries.TASK_TYPE.byKey(TaskTypeId.CRAFT_ITEM);
        if (craftItemType != null) {
            this.addListener(new CraftingTaskListener(this.plugin, this, (TaskType) craftItemType));
        }
        
        TaskType<?, ?> enchantingType = Registries.TASK_TYPE.byKey(TaskTypeId.ENCHANTING);
        if (enchantingType != null) {
            this.addListener(new EnchantingTaskListener(this.plugin, this, (TaskType) enchantingType));
        }
        
        TaskType<?, ?> fertilizingType = Registries.TASK_TYPE.byKey(TaskTypeId.FERTILIZING);
        if (fertilizingType != null) {
            this.addListener(new FertilizingTaskListener(this.plugin, this, (TaskType) fertilizingType));
        }
        
        TaskType<?, ?> fishItemType = Registries.TASK_TYPE.byKey(TaskTypeId.FISH_ITEM);
        if (fishItemType != null) {
            this.addListener(new FishingTaskListener(this.plugin, this, (TaskType) fishItemType));
        }
        
        TaskType<?, ?> forgeItemType = Registries.TASK_TYPE.byKey(TaskTypeId.FORGE_ITEM);
        if (forgeItemType != null) {
            this.addListener(new ForgingTaskListener(this.plugin, this, (TaskType) forgeItemType));
        }
        
        TaskType<?, ?> blockLootType = Registries.TASK_TYPE.byKey(TaskTypeId.BLOCK_LOOT);
        if (blockLootType != null) {
            this.addListener(new BlockDropTaskListener(this.plugin, this, (TaskType) blockLootType));
        }
        
        TaskType<?, ?> mobLootType = Registries.TASK_TYPE.byKey(TaskTypeId.MOB_LOOT);
        if (mobLootType != null) {
            this.addListener(new MobDropTaskListener(this.plugin, this, (TaskType) mobLootType));
        }
        
        TaskType<?, ?> grindstoneType = Registries.TASK_TYPE.byKey(TaskTypeId.GRINDSTONE_ITEM);
        if (grindstoneType != null) {
            this.addListener(new GrindstoneTaskListener(this.plugin, this, (TaskType) grindstoneType));
        }
        
        TaskType<?, ?> killMobType = Registries.TASK_TYPE.byKey(TaskTypeId.KILL_MOB);
        if (killMobType != null) {
            this.addListener(new KillingTaskListener(this.plugin, this, (TaskType) killMobType));
        }
        
        TaskType<?, ?> milkMobType = Registries.TASK_TYPE.byKey(TaskTypeId.MILK_MOB);
        if (milkMobType != null) {
            this.addListener(new MilkingTaskListener(this.plugin, this, (TaskType) milkMobType));
        }
        
        TaskType<?, ?> shearMobType = Registries.TASK_TYPE.byKey(TaskTypeId.SHEAR_MOB);
        if (shearMobType != null) {
            this.addListener(new ShearingTaskListener(this.plugin, this, (TaskType) shearMobType));
        }
        
        TaskType<?, ?> tameMobType = Registries.TASK_TYPE.byKey(TaskTypeId.TAME_MOB);
        if (tameMobType != null) {
            this.addListener(new TamingTaskListener(this.plugin, this, (TaskType) tameMobType));
        }
        
        // 新增的任务类型
        TaskType<?, ?> shootArrowType = Registries.TASK_TYPE.byKey(TaskTypeId.SHOOT_ARROW);
        if (shootArrowType != null) {
            this.addListener(new ShootArrowTaskListener(this.plugin, this, (TaskType) shootArrowType));
        }
        
        TaskType<?, ?> shootTargetType = Registries.TASK_TYPE.byKey(TaskTypeId.SHOOT_TARGET);
        if (shootTargetType != null) {
            this.addListener(new ShootTargetTaskListener(this.plugin, this, (TaskType) shootTargetType));
        }
        
        TaskType<?, ?> throwSnowballType = Registries.TASK_TYPE.byKey(TaskTypeId.THROW_SNOWBALL);
        if (throwSnowballType != null) {
            this.addListener(new ThrowProjectileTaskListener(this.plugin, this, (TaskType) throwSnowballType));
        }
        
        TaskType<?, ?> throwEggType = Registries.TASK_TYPE.byKey(TaskTypeId.THROW_EGG);
        if (throwEggType != null) {
            this.addListener(new ThrowProjectileTaskListener(this.plugin, this, (TaskType) throwEggType));
        }
        
        TaskType<?, ?> throwEnderpearlType = Registries.TASK_TYPE.byKey(TaskTypeId.THROW_ENDERPEARL);
        if (throwEnderpearlType != null) {
            this.addListener(new ThrowProjectileTaskListener(this.plugin, this, (TaskType) throwEnderpearlType));
        }
        
        TaskType<?, ?> itemPickupType = Registries.TASK_TYPE.byKey(TaskTypeId.ITEM_PICKUP);
        if (itemPickupType != null) {
            this.addListener(new ItemPickupTaskListener(this.plugin, this, (TaskType) itemPickupType));
        }
        
        TaskType<?, ?> itemDropType = Registries.TASK_TYPE.byKey(TaskTypeId.ITEM_DROP);
        if (itemDropType != null) {
            this.addListener(new ItemDropTaskListener(this.plugin, this, (TaskType) itemDropType));
        }
        
        TaskType<?, ?> itemBreakType = Registries.TASK_TYPE.byKey(TaskTypeId.ITEM_BREAK);
        if (itemBreakType != null) {
            this.addListener(new ItemBreakTaskListener(this.plugin, this, (TaskType) itemBreakType));
        }
        
        TaskType<?, ?> eatItemType = Registries.TASK_TYPE.byKey(TaskTypeId.EAT_ITEM);
        if (eatItemType != null) {
            this.addListener(new EatItemTaskListener(this.plugin, this, (TaskType) eatItemType));
        }
        
        TaskType<?, ?> drinkPotionType = Registries.TASK_TYPE.byKey(TaskTypeId.DRINK_POTION);
        if (drinkPotionType != null) {
            this.addListener(new EatItemTaskListener(this.plugin, this, (TaskType) drinkPotionType));
        }
        
        TaskType<?, ?> villagerTradeType = Registries.TASK_TYPE.byKey(TaskTypeId.VILLAGER_TRADE);
        if (villagerTradeType != null) {
            this.addListener(new VillagerTradeTaskListener(this.plugin, this, (TaskType) villagerTradeType));
        }
        
        TaskType<?, ?> useAnvilType = Registries.TASK_TYPE.byKey(TaskTypeId.USE_ANVIL);
        if (useAnvilType != null) {
            this.addListener(new UseAnvilTaskListener(this.plugin, this, (TaskType) useAnvilType));
        }
        
        TaskType<?, ?> useBedType = Registries.TASK_TYPE.byKey(TaskTypeId.USE_BED);
        if (useBedType != null) {
            this.addListener(new UseBedTaskListener(this.plugin, this, (TaskType) useBedType));
        }
        
        TaskType<?, ?> useHoeType = Registries.TASK_TYPE.byKey(TaskTypeId.USE_HOE);
        if (useHoeType != null) {
            this.addListener(new UseHoeTaskListener(this.plugin, this, (TaskType) useHoeType));
        }
        
        TaskType<?, ?> placeLavaType = Registries.TASK_TYPE.byKey(TaskTypeId.PLACE_LAVA);
        if (placeLavaType != null) {
            this.addListener(new PlaceBucketTaskListener(this.plugin, this, (TaskType) placeLavaType));
        }
        
        TaskType<?, ?> placeWaterType = Registries.TASK_TYPE.byKey(TaskTypeId.PLACE_WATER);
        if (placeWaterType != null) {
            this.addListener(new PlaceBucketTaskListener(this.plugin, this, (TaskType) placeWaterType));
        }
        
        TaskType<?, ?> launchFireworkType = Registries.TASK_TYPE.byKey(TaskTypeId.LAUNCH_FIREWORK);
        if (launchFireworkType != null) {
            this.addListener(new LaunchFireworkTaskListener(this.plugin, this, (TaskType) launchFireworkType));
        }
        
        TaskType<?, ?> playMusicDiscType = Registries.TASK_TYPE.byKey(TaskTypeId.PLAY_MUSIC_DISC);
        if (playMusicDiscType != null) {
            this.addListener(new PlayMusicDiscTaskListener(this.plugin, this, (TaskType) playMusicDiscType));
        }
        
        TaskType<?, ?> playerDeathType = Registries.TASK_TYPE.byKey(TaskTypeId.PLAYER_DEATH);
        if (playerDeathType != null) {
            this.addListener(new PlayerDeathTaskListener(this.plugin, this, (TaskType) playerDeathType));
        }
        
        TaskType<?, ?> playerJoinType = Registries.TASK_TYPE.byKey(TaskTypeId.PLAYER_JOIN);
        if (playerJoinType != null) {
            this.addListener(new PlayerJoinTaskListener(this.plugin, this, (TaskType) playerJoinType));
        }
        
        TaskType<?, ?> useRiptideType = Registries.TASK_TYPE.byKey(TaskTypeId.USE_RIPTIDE);
        if (useRiptideType != null) {
            this.addListener(new UseRiptideTaskListener(this.plugin, this, (TaskType) useRiptideType));
        }
        
        TaskType<?, ?> winRaidType = Registries.TASK_TYPE.byKey(TaskTypeId.WIN_RAID);
        if (winRaidType != null) {
            this.addListener(new WinRaidTaskListener(this.plugin, this, (TaskType) winRaidType));
        }
        
        TaskType<?, ?> editBookType = Registries.TASK_TYPE.byKey(TaskTypeId.EDIT_BOOK);
        if (editBookType != null) {
            this.addListener(new EditBookTaskListener(this.plugin, this, (TaskType) editBookType));
        }
        
        TaskType<?, ?> completeAdvancementType = Registries.TASK_TYPE.byKey(TaskTypeId.COMPLETE_ADVANCEMENT);
        if (completeAdvancementType != null) {
            this.addListener(new CompleteAdvancementTaskListener(this.plugin, this, (TaskType) completeAdvancementType));
        }
    }

    private void registerTaskTypes() {
        // 原有的任务类型
        Registries.registerTaskType(TaskTypeId.PLACE_BLOCK, AdapterFamily.BLOCK, taskType -> {});
        Registries.registerTaskType(TaskTypeId.BREAK_BLOCK, AdapterFamily.BLOCK, type -> {});
        Registries.registerTaskType(TaskTypeId.BREED_MOB, AdapterFamily.ENTITY, taskType -> {});
        Registries.registerTaskType(TaskTypeId.BREWING, AdapterFamily.ITEM, taskType -> {});
        Registries.registerTaskType(TaskTypeId.COOK_ITEM, AdapterFamily.ITEM, taskType -> {});
        Registries.registerTaskType(TaskTypeId.CRAFT_ITEM, AdapterFamily.ITEM, taskType -> {});
        Registries.registerTaskType(TaskTypeId.ENCHANTING, AdapterFamily.ENCHANTMENT, taskType -> {});
        Registries.registerTaskType(TaskTypeId.FERTILIZING, AdapterFamily.BLOCK_STATE, type -> {});
        Registries.registerTaskType(TaskTypeId.FISH_ITEM, AdapterFamily.ITEM, type -> {});
        Registries.registerTaskType(TaskTypeId.FORGE_ITEM, AdapterFamily.ITEM, type -> {});
        Registries.registerTaskType(TaskTypeId.BLOCK_LOOT, AdapterFamily.ITEM, type -> {});
        Registries.registerTaskType(TaskTypeId.MOB_LOOT, AdapterFamily.ITEM, type -> {});
        Registries.registerTaskType(TaskTypeId.GRINDSTONE_ITEM, AdapterFamily.ITEM, type -> {});
        Registries.registerTaskType(TaskTypeId.KILL_MOB, AdapterFamily.ENTITY, type -> {});
        Registries.registerTaskType(TaskTypeId.MILK_MOB, AdapterFamily.ENTITY, type -> {});
        Registries.registerTaskType(TaskTypeId.SHEAR_MOB, AdapterFamily.ENTITY, type -> {});
        Registries.registerTaskType(TaskTypeId.TAME_MOB, AdapterFamily.ENTITY, type -> {});
        
        // 新增：射击相关任务类型
        Registries.registerTaskType(TaskTypeId.SHOOT_ARROW, AdapterFamily.CUSTOM, type -> {});
        Registries.registerTaskType(TaskTypeId.SHOOT_TARGET, AdapterFamily.BLOCK, type -> {});
        
        // 新增：投掷物相关任务类型
        Registries.registerTaskType(TaskTypeId.THROW_SNOWBALL, AdapterFamily.CUSTOM, type -> {});
        Registries.registerTaskType(TaskTypeId.THROW_EGG, AdapterFamily.CUSTOM, type -> {});
        Registries.registerTaskType(TaskTypeId.THROW_ENDERPEARL, AdapterFamily.CUSTOM, type -> {});
        
        // 新增：物品相关任务类型
        Registries.registerTaskType(TaskTypeId.ITEM_PICKUP, AdapterFamily.ITEM, type -> {});
        Registries.registerTaskType(TaskTypeId.ITEM_DROP, AdapterFamily.ITEM, type -> {});
        Registries.registerTaskType(TaskTypeId.ITEM_BREAK, AdapterFamily.ITEM, type -> {});
        Registries.registerTaskType(TaskTypeId.EAT_ITEM, AdapterFamily.ITEM, type -> {});
        Registries.registerTaskType(TaskTypeId.DRINK_POTION, AdapterFamily.ITEM, type -> {});
        
        // 新增：交互相关任务类型
        Registries.registerTaskType(TaskTypeId.VILLAGER_TRADE, AdapterFamily.ITEM, type -> {});
        Registries.registerTaskType(TaskTypeId.USE_ANVIL, AdapterFamily.ITEM, type -> {});
        Registries.registerTaskType(TaskTypeId.USE_BED, AdapterFamily.BLOCK, type -> {});
        Registries.registerTaskType(TaskTypeId.USE_HOE, AdapterFamily.BLOCK, type -> {});
        
        // 新增：桶类相关任务类型
        Registries.registerTaskType(TaskTypeId.PLACE_LAVA, AdapterFamily.ITEM, type -> {});
        Registries.registerTaskType(TaskTypeId.PLACE_WATER, AdapterFamily.ITEM, type -> {});
        
        // 新增：其他任务类型
        Registries.registerTaskType(TaskTypeId.LAUNCH_FIREWORK, AdapterFamily.ITEM, type -> {});
        Registries.registerTaskType(TaskTypeId.PLAY_MUSIC_DISC, AdapterFamily.ITEM, type -> {});
        Registries.registerTaskType(TaskTypeId.PLAYER_DEATH, AdapterFamily.CUSTOM, type -> {});
        Registries.registerTaskType(TaskTypeId.PLAYER_JOIN, AdapterFamily.CUSTOM, type -> {});
        Registries.registerTaskType(TaskTypeId.USE_RIPTIDE, AdapterFamily.ITEM, type -> {});
        Registries.registerTaskType(TaskTypeId.WIN_RAID, AdapterFamily.CUSTOM, type -> {});
        Registries.registerTaskType(TaskTypeId.EDIT_BOOK, AdapterFamily.ITEM, type -> {});
        Registries.registerTaskType(TaskTypeId.COMPLETE_ADVANCEMENT, AdapterFamily.CUSTOM, type -> {});
    }

    public boolean canDoTasks(@NotNull Player player) {
        return player.getGameMode() != GameMode.CREATIVE && canDoTasksInVehicle(player);
    }

    public static boolean canDoTasksInVehicle(@NotNull Player player) {
        if (Config.ANTI_ABUSE_COUNT_IN_VEHICLES.get()) return true;

        Entity vehicle = player.getVehicle();
        return vehicle == null || vehicle instanceof LivingEntity;
    }

    public <O, A extends AdapterFamily<O>> void progressQuests(@NotNull Player player, @NotNull TaskType<O, A> taskType, @NotNull O entity, int amount) {
        Adapter<?, O> adapter = taskType.getAdapterFamily().getAdapterFor(entity);
        if (adapter == null) {
            return;
        }

        String fullName = adapter.toFullNameOfEntity(entity);
        if (fullName == null) {
            return;
        }

        this.plugin.milestoneManager().ifPresent(milestoneManager -> milestoneManager.progressMilestones(player, taskType, fullName, amount));
        this.plugin.questManager().ifPresent(questManager -> questManager.progressQuests(player, taskType, fullName, amount));
    }

    public boolean isArtificalSpawn(@NotNull SpawnReason reason) {
        return this.artificalMobSpawns.contains(reason);
    }

    public boolean isSpawnerMob(@NotNull Entity entity) {
        return PDCUtil.getBoolean(entity, this.mobSpawnerKey).isPresent();
    }

    public void markSpawnerMob(@NotNull Entity entity, boolean flag) {
        PDCUtil.set(entity, this.mobSpawnerKey, flag);
    }

    public boolean isPlayerBlock(@NotNull Block block) {
        return block.hasMetadata(PLAYER_BLOCK_MARKER) || PlayerBlockTracker.isTracked(block);
    }

    public void markPlayerBlock(@NotNull Block block, boolean flag) {
        if (flag) {
            block.setMetadata(PLAYER_BLOCK_MARKER, new FixedMetadataValue(this.plugin, true));
        }
        else {
            block.removeMetadata(PLAYER_BLOCK_MARKER, this.plugin);
        }
    }

    public void setWorkstationOwnerId(@NotNull TileState station, @NotNull UUID uuid) {
        PDCUtil.set(station, this.stationOwnerKey, uuid);
    }

    @Nullable
    public UUID getWorkstationOwnerId(@NotNull TileState station) {
        return PDCUtil.getUUID(station, this.stationOwnerKey).orElse(null);
    }

    @Nullable
    public Player getWorkstationOwner(@NotNull TileState station) {
        UUID uuid = getWorkstationOwnerId(station);
        return uuid == null ? null : Players.getPlayer(uuid);
    }

    public void setWorkstationMode(@NotNull TileState station, @NotNull WorkstationMode mode) {
        PDCUtil.set(station, this.stationModeKey, mode.getId());
    }

    @Nullable
    public WorkstationMode getWorkstationMode(@NotNull TileState station) {
        return PDCUtil.getInt(station, this.stationModeKey).map(WorkstationMode::byId).orElse(null);
    }
}
