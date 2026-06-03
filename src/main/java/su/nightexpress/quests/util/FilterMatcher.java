package su.nightexpress.quests.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * 任务筛选匹配工具类
 * 用于解析和匹配任务目标中的筛选条件
 */
public class FilterMatcher {
    
    private static final String FILTER_SEPARATOR = "\\|";
    private static final String FILTER_LIST_SEPARATOR = ",";
    
    /**
     * 从完整的目标字符串中提取基础目标名称
     * 例如: "minecraft:sharpness|minecraft:diamond_sword" -> "minecraft:sharpness"
     */
    @NotNull
    public static String extractObjectiveName(@NotNull String fullObjective) {
        String[] parts = fullObjective.split(FILTER_SEPARATOR, 2);
        return parts[0].trim();
    }
    
    /**
     * 从完整的目标字符串中提取筛选条件列表
     * 例如: "minecraft:sharpness|minecraft:diamond_sword,minecraft:netherite_sword" 
     *       -> ["minecraft:diamond_sword", "minecraft:netherite_sword"]
     */
    @Nullable
    public static List<String> extractFilters(@NotNull String fullObjective) {
        String[] parts = fullObjective.split(FILTER_SEPARATOR, 2);
        if (parts.length < 2) {
            return null; // 没有筛选条件
        }
        
        String filtersStr = parts[1].trim();
        if (filtersStr.isEmpty()) {
            return null;
        }
        
        return Arrays.asList(filtersStr.split(FILTER_LIST_SEPARATOR));
    }
    
    /**
     * 检查物品是否匹配筛选条件
     * 
     * @param filters 筛选条件列表
     * @param item 要检查的物品
     * @return true 如果匹配
     */
    public static boolean matchesItemFilter(@Nullable List<String> filters, @NotNull ItemStack item) {
        if (filters == null || filters.isEmpty()) {
            return true; // 没有筛选条件，全部通过
        }
        
        for (String filter : filters) {
            String trimmedFilter = filter.trim();
            
            if (TagMatcher.isTag(trimmedFilter)) {
                // 标签匹配
                if (TagMatcher.matchesItemStack(trimmedFilter, item)) {
                    return true;
                }
            } else {
                // 普通匹配
                if (matchesMaterial(trimmedFilter, item.getType())) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 检查药水是否匹配筛选条件
     * 
     * @param filters 筛选条件列表
     * @param potion 要检查的药水
     * @return true 如果匹配
     */
    public static boolean matchesPotionFilter(@Nullable List<String> filters, @NotNull ItemStack potion) {
        if (filters == null || filters.isEmpty()) {
            return true; // 没有筛选条件，全部通过
        }
        
        if (!(potion.getItemMeta() instanceof PotionMeta potionMeta)) {
            return false;
        }
        
        Material potionType = potion.getType();
        PotionType basePotionType = potionMeta.getBasePotionType();
        
        for (String filter : filters) {
            String trimmedFilter = filter.trim();
            
            // 解析筛选条件
            // 格式1: minecraft:potion{Potion:"minecraft:strength"}
            // 格式2: minecraft:potion (任意普通药水)
            // 格式3: minecraft:splash_potion (任意喷溅药水)
            
            if (trimmedFilter.contains("{")) {
                // 带药水效果的筛选
                if (matchesPotionWithEffect(trimmedFilter, potionType, basePotionType)) {
                    return true;
                }
            } else {
                // 只匹配药水类型，不管效果
                if (matchesPotionType(trimmedFilter, potionType)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 检查材料是否匹配筛选条件（用于锻造任务）
     * 
     * @param filters 筛选条件列表
     * @param materials 锻造台中的材料列表
     * @return true 如果匹配
     */
    public static boolean matchesMaterialsFilter(@Nullable List<String> filters, @NotNull List<ItemStack> materials) {
        if (filters == null || filters.isEmpty()) {
            return true; // 没有筛选条件，全部通过
        }
        
        // 检查是否所有筛选条件都在材料列表中
        for (String filter : filters) {
            String trimmedFilter = filter.trim();
            boolean found = false;
            
            for (ItemStack material : materials) {
                if (material == null || material.getType().isAir()) {
                    continue;
                }
                
                if (TagMatcher.isTag(trimmedFilter)) {
                    if (TagMatcher.matchesItemStack(trimmedFilter, material)) {
                        found = true;
                        break;
                    }
                } else {
                    if (matchesMaterial(trimmedFilter, material.getType())) {
                        found = true;
                        break;
                    }
                }
            }
            
            if (!found) {
                return false; // 有筛选条件未满足
            }
        }
        
        return true; // 所有筛选条件都满足
    }
    
    /**
     * 检查材料类型是否匹配
     */
    private static boolean matchesMaterial(@NotNull String filter, @NotNull Material material) {
        String materialKey = material.getKey().toString();
        
        // 精确匹配
        if (materialKey.equalsIgnoreCase(filter)) {
            return true;
        }
        
        // 不带命名空间匹配
        if (filter.contains(":")) {
            String valueOnly = filter.substring(filter.indexOf(':') + 1);
            return material.name().equalsIgnoreCase(valueOnly);
        } else {
            return material.name().equalsIgnoreCase(filter);
        }
    }
    
    /**
     * 检查药水类型和效果是否匹配
     * 格式: minecraft:potion{Potion:"minecraft:strength"}
     */
    private static boolean matchesPotionWithEffect(@NotNull String filter, @NotNull Material potionType, @NotNull PotionType basePotionType) {
        try {
            // 提取药水类型
            int braceIndex = filter.indexOf('{');
            if (braceIndex == -1) {
                return false;
            }
            
            String typeStr = filter.substring(0, braceIndex).trim();
            if (!matchesPotionType(typeStr, potionType)) {
                return false;
            }
            
            // 提取药水效果
            int startQuote = filter.indexOf('"');
            int endQuote = filter.lastIndexOf('"');
            if (startQuote == -1 || endQuote == -1 || startQuote >= endQuote) {
                return false;
            }
            
            String effectStr = filter.substring(startQuote + 1, endQuote).trim();
            
            // 移除命名空间前缀
            String effectName = effectStr.contains(":") ? effectStr.substring(effectStr.indexOf(':') + 1) : effectStr;
            
            // 匹配药水效果
            return basePotionType.name().equalsIgnoreCase(effectName);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查药水类型是否匹配（不检查效果）
     */
    private static boolean matchesPotionType(@NotNull String filter, @NotNull Material potionType) {
        String typeKey = potionType.getKey().toString();
        
        // 精确匹配
        if (typeKey.equalsIgnoreCase(filter)) {
            return true;
        }
        
        // 不带命名空间匹配
        if (filter.contains(":")) {
            String valueOnly = filter.substring(filter.indexOf(':') + 1);
            return potionType.name().equalsIgnoreCase(valueOnly);
        } else {
            return potionType.name().equalsIgnoreCase(filter);
        }
    }
}
