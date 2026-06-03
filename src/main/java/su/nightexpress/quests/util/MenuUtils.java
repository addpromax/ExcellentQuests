package su.nightexpress.quests.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.text.night.NightMessage;
import su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers;
import su.nightexpress.quests.config.Config;
import su.nightexpress.quests.config.Lang;
import su.nightexpress.quests.quest.definition.QuestXPReward;
import su.nightexpress.quests.reward.Reward;
import su.nightexpress.quests.task.adapter.Adapter;
import su.nightexpress.quests.task.TaskType;
import su.nightexpress.quests.milestone.data.MilestoneData;
import su.nightexpress.quests.quest.data.QuestData;
import su.nightexpress.quests.milestone.definition.Milestone;
import su.nightexpress.quests.quest.definition.Quest;
import su.nightexpress.quests.milestone.definition.MilestoneObjective;

import java.util.*;

import static su.nightexpress.quests.QuestsPlaceholders.*;

public class MenuUtils {

    @NotNull
    public static String buildProgressBar(double percent) {
        int length = Config.UI_PROGRESS_BAR_LENGTH.get();
        int filled = Math.clamp((int) Math.ceil(length * percent), 0, length);

        String colorFill = Config.UI_PROGRESS_BAR_COLOR_FILL.get();
        String colorEmpty = Config.UI_PROGRESS_BAR_COLOR_EMPTY.get();
        String point = Config.UI_PROGRESS_BAR_CHAR.get();

        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < length; index++) {
            String color = filled > index ? colorFill : colorEmpty;
            builder.append(TagWrappers.COLOR.with(color).wrap(point));
        }
        return builder.toString();
    }

    @NotNull
    public static List<String> formatObjectives(@NotNull Milestone milestone, @NotNull MilestoneData data, int level) {
        List<String> list = new ArrayList<>();
        TaskType<?, ?> type = milestone.getType();

        // 如果有分组，按组显示累加进度
        if (!milestone.getObjectiveGroups().isEmpty()) {
            for (su.nightexpress.quests.milestone.definition.ObjectiveGroup group : milestone.getObjectiveGroups()) {
                int required = group.getRequiredAmount(level);
                if (required <= 0) continue;
                
                // 累加组内所有成员的进度
                int current = 0;
                if (data.isLevelCompleted(level)) {
                    current = required;
                } else {
                    for (String member : group.getMembers()) {
                        current += data.getObjectiveProgress(member);
                    }
                }
                
                // 使用组的第一个成员获取本地化名称（作为组的代表）
                String firstMember = group.getMembers().isEmpty() ? group.getGroupName() : group.getMembers().get(0);
                Adapter<?, ?> adapter = type.getAdapterFamily().getAdapterForName(firstMember);
                
                // 使用组名或物品类型名作为显示名称
                String displayName = adapter == null ? group.getGroupName() : getGroupDisplayName(adapter, firstMember, group);
                
                list.add(Lang.UI_MILESTONES_MILESTONE_OBJECTIVE.text()
                    .replace(GENERIC_NAME, displayName)
                    .replace(GENERIC_CURRENT, NumberUtil.format(current))
                    .replace(GENERIC_REQUIRED, NumberUtil.format(required)));
            }
        } else {
            // 没有分组，使用原来的逻辑：显示每个独立目标
            milestone.getObjectiveTable().getEntryMap().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                String fullName = entry.getKey();
                MilestoneObjective objective = entry.getValue();

                int required = objective.getAmount(level);
                int current = data.isLevelCompleted(level) ? required : data.getObjectiveProgress(fullName);

                list.add(formatObjective(type, fullName, current, required));
            });
        }

        return list;
    }
    
    /**
     * 获取组的显示名称
     * 策略：使用第一个成员的本地化名称，并去掉具体类型前缀
     * 例如：橡木木板 -> 木板
     */
    @NotNull
    private static String getGroupDisplayName(@NotNull Adapter<?, ?> adapter, @NotNull String firstMember, 
                                               @NotNull su.nightexpress.quests.milestone.definition.ObjectiveGroup group) {
        String localizedName = adapter.getLocalizedName(firstMember);
        
        // 如果本地化名称为 null，返回组名
        if (localizedName == null) {
            return group.getGroupName();
        }
        
        // 尝试去掉常见的前缀（如"橡木"、"白桦"等）
        // 保留后缀（如"木板"、"原木"等）
        String[] commonPrefixes = {
            "橡木", "白桦", "云杉", "丛林", "金合欢", "深色橡木", "绯红", "诡异", "樱花", "红树",
            "Oak", "Birch", "Spruce", "Jungle", "Acacia", "Dark Oak", "Crimson", "Warped", "Cherry", "Mangrove"
        };
        
        for (String prefix : commonPrefixes) {
            if (localizedName.startsWith(prefix)) {
                // 移除前缀，返回后缀部分
                String suffix = localizedName.substring(prefix.length()).trim();
                if (!suffix.isEmpty()) {
                    return suffix;
                }
            }
        }
        
        // 如果没有匹配的前缀，返回组名
        return group.getGroupName();
    }

    @NotNull
    public static List<String> formatObjectives(@NotNull Quest quest, @NotNull QuestData data) {
        List<String> list = new ArrayList<>();
        TaskType<?, ?> type = quest.getType();

        data.getObjectiveCounterMap().forEach((fullName, counter) -> {
            int required = counter.getRequired();
            int current = counter.getCompleted();

            list.add(formatObjective(type, fullName, current, required));
        });

        /*quest.getObjectiveTable().getEntryMap().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            String fullName = entry.getKey();

            int required = data.getRequired(fullName);
            int current = data.getCurrent(fullName);

            list.add(formatObjective(type, fullName, current, required));
        });*/

        return list;
    }

    @NotNull
    private static String formatObjective(@NotNull TaskType<?, ?> type, @NotNull String fullName, int current, int required) {
        String name = getDisplayName(type, fullName);

        return Lang.UI_MILESTONES_MILESTONE_OBJECTIVE.text()
            .replace(GENERIC_NAME, String.valueOf(name))
            .replace(GENERIC_CURRENT, NumberUtil.format(current))
            .replace(GENERIC_REQUIRED, NumberUtil.format(required));
    }
    
    /**
     * 获取目标的显示名称
     * 支持通配符和标签特殊处理
     */
    @NotNull
    private static String getDisplayName(@NotNull TaskType<?, ?> type, @NotNull String fullName) {
        // 检查是否是通配符
        if (fullName.endsWith(":*") || fullName.endsWith(":all")) {
            return getWildcardDisplayName(type);
        }
        
        // 检查是否是标签（以 # 开头）
        if (TagMatcher.isTag(fullName)) {
            return getTagDisplayName(fullName);
        }
        
        // 检查是否是 vanilla_string 类型（这些通常需要特殊显示名称）
        if (fullName.startsWith("vanilla_string:")) {
            String key = fullName.substring("vanilla_string:".length());
            return getVanillaStringDisplayName(key);
        }
        
        // 检查是否是特殊物品（如附魔台、酿造台等）
        String specialName = getSpecialItemDisplayName(fullName);
        if (specialName != null) {
            return specialName;
        }
        
        // 普通目标，使用适配器获取本地化名称
        Adapter<?, ?> adapter = type.getAdapterFamily().getAdapterForName(fullName);
        if (adapter == null) {
            return fullName;
        }
        
        String localizedName = adapter.getLocalizedName(fullName);
        return localizedName != null ? localizedName : fullName;
    }
    
    /**
     * 获取 vanilla_string 类型的显示名称
     */
    @NotNull
    private static String getVanillaStringDisplayName(@NotNull String key) {
        return switch (key) {
            case "join" -> Lang.VANILLA_STRING_JOIN.text();
            case "death" -> Lang.VANILLA_STRING_DEATH.text();
            case "advancement" -> Lang.VANILLA_STRING_ADVANCEMENT.text();
            case "arrow" -> Lang.VANILLA_STRING_ARROW.text();
            case "snowball" -> Lang.VANILLA_STRING_SNOWBALL.text();
            case "egg" -> Lang.VANILLA_STRING_EGG.text();
            case "enderpearl" -> Lang.VANILLA_STRING_ENDERPEARL.text();
            case "raid_victory" -> Lang.VANILLA_STRING_RAID_VICTORY.text();
            case "target" -> Lang.VANILLA_STRING_TARGET.text();
            case "anvil" -> Lang.VANILLA_STRING_ANVIL.text();
            case "bed" -> Lang.VANILLA_STRING_BED.text();
            default -> key; // 如果没有匹配，返回原始键
        };
    }
    
    /**
     * 获取特殊物品/方块的显示名称
     * 用于处理 minecraft:enchanting_table 等特殊情况
     */
    @NotNull
    private static String getSpecialItemDisplayName(@NotNull String fullName) {
        // 移除 minecraft: 前缀
        String key = fullName.startsWith("minecraft:") ? fullName.substring("minecraft:".length()) : fullName;
        
        return switch (key) {
            case "enchanting_table" -> Lang.SPECIAL_ENCHANTING.text();
            case "brewing_stand" -> Lang.SPECIAL_BREWING.text();
            case "firework_rocket" -> Lang.SPECIAL_FIREWORK.text();
            case "music_disc_cat", "music_disc_13", "music_disc_blocks", "music_disc_chirp", 
                 "music_disc_far", "music_disc_mall", "music_disc_mellohi", "music_disc_stal",
                 "music_disc_strad", "music_disc_ward", "music_disc_11", "music_disc_wait",
                 "music_disc_pigstep", "music_disc_otherside", "music_disc_5", "music_disc_relic" 
                 -> Lang.SPECIAL_MUSIC_DISC.text();
            case "lava_bucket" -> Lang.SPECIAL_LAVA_BUCKET.text();
            case "water_bucket" -> Lang.SPECIAL_WATER_BUCKET.text();
            case "trident" -> Lang.SPECIAL_TRIDENT.text();
            case "writable_book", "written_book" -> Lang.SPECIAL_BOOK.text();
            default -> null; // 返回 null 表示没有特殊处理
        };
    }
    
    /**
     * 获取通配符的显示名称
     * 根据任务类型返回友好的名称
     */
    @NotNull
    private static String getWildcardDisplayName(@NotNull TaskType<?, ?> type) {
        String typeId = type.getId();
        
        // 根据任务类型返回对应的显示名称
        return switch (typeId) {
            // 物品相关
            case "item_pickup" -> Lang.WILDCARD_ITEM_PICKUP.text();
            case "item_drop" -> Lang.WILDCARD_ITEM_DROP.text();
            case "item_break" -> Lang.WILDCARD_ITEM_BREAK.text();
            case "enchant_item" -> Lang.WILDCARD_ENCHANT_ITEM.text();
            case "eat_item" -> Lang.WILDCARD_EAT_ITEM.text();
            case "drink_potion" -> Lang.WILDCARD_DRINK_POTION.text();
            case "villager_trade" -> Lang.WILDCARD_VILLAGER_TRADE.text();
            
            // 方块相关
            case "block_break" -> Lang.WILDCARD_BLOCK_BREAK.text();
            case "block_place" -> Lang.WILDCARD_BLOCK_PLACE.text();
            
            // 生物相关
            case "entity_kill" -> Lang.WILDCARD_ENTITY_KILL.text();
            case "entity_breed" -> Lang.WILDCARD_ENTITY_BREED.text();
            case "entity_tame" -> Lang.WILDCARD_ENTITY_TAME.text();
            case "entity_shear" -> Lang.WILDCARD_ENTITY_SHEAR.text();
            
            // 钓鱼相关
            case "fishing" -> Lang.WILDCARD_FISHING.text();
            
            // 玩家动作（通常是 vanilla_string 类型）
            // 这些任务类型使用通配符时也应该显示相应的名称
            case "player_join" -> Lang.VANILLA_STRING_JOIN.text();
            case "player_death" -> Lang.VANILLA_STRING_DEATH.text();
            case "complete_advancement" -> Lang.VANILLA_STRING_ADVANCEMENT.text();
            case "shoot_arrow" -> Lang.VANILLA_STRING_ARROW.text();
            case "throw_snowball" -> Lang.VANILLA_STRING_SNOWBALL.text();
            case "throw_egg" -> Lang.VANILLA_STRING_EGG.text();
            case "throw_enderpearl" -> Lang.VANILLA_STRING_ENDERPEARL.text();
            case "win_raid" -> Lang.VANILLA_STRING_RAID_VICTORY.text();
            
            default -> Lang.WILDCARD_DEFAULT.text();
        };
    }
    
    /**
     * 获取标签的显示名称
     * 支持常用的 Minecraft 标签翻译
     */
    @NotNull
    private static String getTagDisplayName(@NotNull String fullName) {
        // 移除 # 前缀
        String tagKey = TagMatcher.removeTagPrefix(fullName);
        
        // 移除命名空间，只保留标签名称
        String tagName = tagKey.contains(":") ? tagKey.substring(tagKey.indexOf(':') + 1) : tagKey;
        
        // 尝试获取预定义的标签翻译
        String translatedName = getCommonTagTranslation(tagName);
        if (translatedName != null) {
            return translatedName;
        }
        
        // 如果没有预定义翻译，生成友好的显示名称
        // 将下划线替换为空格，并首字母大写
        return formatTagName(tagName);
    }
    
    /**
     * 获取常用标签的翻译
     */
    @Nullable
    private static String getCommonTagTranslation(@NotNull String tagName) {
        return switch (tagName) {
            // 方块/物品标签
            case "logs" -> Lang.TAG_LOGS.text();
            case "planks" -> Lang.TAG_PLANKS.text();
            case "wool" -> Lang.TAG_WOOL.text();
            case "stone_bricks" -> Lang.TAG_STONE_BRICKS.text();
            case "flowers" -> Lang.TAG_FLOWERS.text();
            case "saplings" -> Lang.TAG_SAPLINGS.text();
            case "leaves" -> Lang.TAG_LEAVES.text();
            case "wooden_doors" -> Lang.TAG_WOODEN_DOORS.text();
            case "wooden_stairs" -> Lang.TAG_WOODEN_STAIRS.text();
            case "wooden_slabs" -> Lang.TAG_WOODEN_SLABS.text();
            case "wooden_fences" -> Lang.TAG_WOODEN_FENCES.text();
            case "wooden_pressure_plates" -> Lang.TAG_WOODEN_PRESSURE_PLATES.text();
            case "wooden_buttons" -> Lang.TAG_WOODEN_BUTTONS.text();
            case "wooden_trapdoors" -> Lang.TAG_WOODEN_TRAPDOORS.text();
            case "sand" -> Lang.TAG_SAND.text();
            case "anvil" -> Lang.TAG_ANVIL.text();
            case "rails" -> Lang.TAG_RAILS.text();
            case "beds" -> Lang.TAG_BEDS.text();
            case "banners" -> Lang.TAG_BANNERS.text();
            case "boats" -> Lang.TAG_BOATS.text();
            case "fishes" -> Lang.TAG_FISHES.text();
            case "signs" -> Lang.TAG_SIGNS.text();
            case "music_discs" -> Lang.TAG_MUSIC_DISCS.text();
            case "coals" -> Lang.TAG_COALS.text();
            case "arrows" -> Lang.TAG_ARROWS.text();
            
            // 实体标签
            case "raiders" -> Lang.TAG_RAIDERS.text();
            case "skeletons" -> Lang.TAG_SKELETONS.text();
            case "zombies" -> Lang.TAG_ZOMBIES.text();
            case "impact_projectiles" -> Lang.TAG_IMPACT_PROJECTILES.text();
            case "beehive_inhabitors" -> Lang.TAG_BEEHIVE_INHABITORS.text();
            case "axolotl_always_hostiles" -> Lang.TAG_AXOLOTL_ALWAYS_HOSTILES.text();
            case "axolotl_hunt_targets" -> Lang.TAG_AXOLOTL_HUNT_TARGETS.text();
            case "freeze_immune_entity_types" -> Lang.TAG_FREEZE_IMMUNE_ENTITY_TYPES.text();
            case "freeze_hurts_extra_types" -> Lang.TAG_FREEZE_HURTS_EXTRA_TYPES.text();
            case "frog_food" -> Lang.TAG_FROG_FOOD.text();
            case "powder_snow_walkable_mobs" -> Lang.TAG_POWDER_SNOW_WALKABLE_MOBS.text();
            
            default -> null;
        };
    }
    
    /**
     * 格式化标签名称
     * 将下划线替换为空格，并首字母大写
     */
    @NotNull
    private static String formatTagName(@NotNull String tagName) {
        String[] words = tagName.split("_");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                if (result.length() > 0) {
                    result.append(" ");
                }
                // 首字母大写
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
            }
        }
        
        return result.toString();
    }

    @NotNull
    public static List<String> formatRewards(@NotNull List<Reward> rewards, int units, int level, double scale) {
        List<String> result = new ArrayList<>();
        
        rewards.stream()
            .sorted(Comparator.comparing(reward -> NightMessage.stripTags(reward.getName(units, level, scale))))
            .forEach(reward -> {
                // 添加奖励名称
                result.add(Lang.UI_ENTRY_REWARD.text().replace(GENERIC_NAME, reward.getName(units, level, scale)));
                
                // 添加奖励描述（lore）
                List<String> lore = reward.getLore(units, level, scale);
                if (!lore.isEmpty()) {
                    lore.forEach(line -> result.add("  " + line));  // 缩进显示
                }
            });
        
        return result;
    }

    @NotNull
    public static List<String> formatBattlePassRewards(@NotNull QuestXPReward reward, double unitsWorth) {
        return Lists.newList(
            Lang.UI_ENTRY_REWARD_BATTLE_PASS_XP.text().replace(GENERIC_XP, NumberUtil.format(reward.getXP(unitsWorth)))
        );
    }
}
