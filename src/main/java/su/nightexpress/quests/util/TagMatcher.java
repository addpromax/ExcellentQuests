package su.nightexpress.quests.util;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minecraft Tags 标签匹配工具类
 * 支持在任务目标中使用标签，如 #minecraft:logs, #minecraft:planks 等
 */
public class TagMatcher {
    
    private static final String TAG_PREFIX = "#";
    // 使用线程安全的 ConcurrentHashMap
    private static final Map<String, Tag<Material>> MATERIAL_TAG_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Tag<EntityType>> ENTITY_TAG_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 检查字符串是否是标签格式（以 # 开头）
     */
    public static boolean isTag(@NotNull String name) {
        return name.startsWith(TAG_PREFIX);
    }
    
    /**
     * 从标签名称中移除 # 前缀
     */
    @NotNull
    public static String removeTagPrefix(@NotNull String tagName) {
        if (isTag(tagName)) {
            return tagName.substring(TAG_PREFIX.length());
        }
        return tagName;
    }
    
    /**
     * 检查物品是否匹配给定的材料标签
     * 
     * @param fullName 目标名称（可能是具体物品或标签）
     * @param material 要检查的材料
     * @return true 如果匹配
     */
    public static boolean matchesMaterial(@NotNull String fullName, @NotNull Material material) {
        // 如果不是标签，进行普通匹配
        if (!isTag(fullName)) {
            return matchesNormalMaterial(fullName, material);
        }
        
        // 是标签，检查材料是否属于该标签
        String tagKey = removeTagPrefix(fullName);
        Tag<Material> tag = getMaterialTag(tagKey);
        
        if (tag != null) {
            return tag.isTagged(material);
        }
        
        return false;
    }
    
    /**
     * 检查方块是否匹配给定的标签
     */
    public static boolean matchesBlock(@NotNull String fullName, @NotNull Block block) {
        return matchesMaterial(fullName, block.getType());
    }
    
    /**
     * 检查物品堆是否匹配给定的标签
     */
    public static boolean matchesItemStack(@NotNull String fullName, @NotNull ItemStack itemStack) {
        return matchesMaterial(fullName, itemStack.getType());
    }
    
    /**
     * 检查实体是否匹配给定的标签
     * 
     * @param fullName 目标名称（可能是具体实体或标签）
     * @param entity 要检查的实体
     * @return true 如果匹配
     */
    public static boolean matchesEntity(@NotNull String fullName, @NotNull Entity entity) {
        // 如果不是标签，进行普通匹配
        if (!isTag(fullName)) {
            return matchesNormalEntity(fullName, entity);
        }
        
        // 是标签，检查实体是否属于该标签
        String tagKey = removeTagPrefix(fullName);
        Tag<EntityType> tag = getEntityTag(tagKey);
        
        if (tag != null) {
            return tag.isTagged(entity.getType());
        }
        
        return false;
    }
    
    /**
     * 普通材料匹配（非标签）
     */
    private static boolean matchesNormalMaterial(@NotNull String fullName, @NotNull Material material) {
        // 精确匹配（带命名空间）
        String materialKey = material.getKey().toString();
        if (materialKey.equalsIgnoreCase(fullName)) {
            return true;
        }
        
        // 不带命名空间匹配
        if (fullName.contains(":")) {
            String valueOnly = fullName.substring(fullName.indexOf(':') + 1);
            return material.name().equalsIgnoreCase(valueOnly);
        } else {
            return material.name().equalsIgnoreCase(fullName);
        }
    }
    
    /**
     * 普通实体匹配（非标签）
     */
    private static boolean matchesNormalEntity(@NotNull String fullName, @NotNull Entity entity) {
        EntityType type = entity.getType();
        
        // 精确匹配（带命名空间）
        String entityKey = type.getKey().toString();
        if (entityKey.equalsIgnoreCase(fullName)) {
            return true;
        }
        
        // 不带命名空间匹配
        if (fullName.contains(":")) {
            String valueOnly = fullName.substring(fullName.indexOf(':') + 1);
            return type.name().equalsIgnoreCase(valueOnly);
        } else {
            return type.name().equalsIgnoreCase(fullName);
        }
    }
    
    /**
     * 获取材料标签
     */
    @Nullable
    private static Tag<Material> getMaterialTag(@NotNull String tagKey) {
        // 先检查缓存
        if (MATERIAL_TAG_CACHE.containsKey(tagKey)) {
            return MATERIAL_TAG_CACHE.get(tagKey);
        }
        
        // 尝试解析命名空间键
        NamespacedKey key = parseNamespacedKey(tagKey);
        if (key == null) {
            MATERIAL_TAG_CACHE.put(tagKey, null);
            return null;
        }
        
        // 尝试获取 Bukkit 预定义的标签
        Tag<Material> tag = getBukkitMaterialTag(key);
        
        // 缓存结果（即使是null）
        MATERIAL_TAG_CACHE.put(tagKey, tag);
        return tag;
    }
    
    /**
     * 获取实体标签
     */
    @Nullable
    private static Tag<EntityType> getEntityTag(@NotNull String tagKey) {
        // 先检查缓存
        if (ENTITY_TAG_CACHE.containsKey(tagKey)) {
            return ENTITY_TAG_CACHE.get(tagKey);
        }
        
        // 尝试解析命名空间键
        NamespacedKey key = parseNamespacedKey(tagKey);
        if (key == null) {
            ENTITY_TAG_CACHE.put(tagKey, null);
            return null;
        }
        
        // 尝试获取 Bukkit 预定义的标签
        Tag<EntityType> tag = getBukkitEntityTag(key);
        
        // 缓存结果（即使是null）
        ENTITY_TAG_CACHE.put(tagKey, tag);
        return tag;
    }
    
    /**
     * 解析命名空间键
     */
    @Nullable
    private static NamespacedKey parseNamespacedKey(@NotNull String tagKey) {
        try {
            if (tagKey.contains(":")) {
                String[] parts = tagKey.split(":", 2);
                return new NamespacedKey(parts[0], parts[1]);
            } else {
                // 默认使用 minecraft 命名空间
                return NamespacedKey.minecraft(tagKey);
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取 Bukkit 预定义的材料标签
     * 使用反射访问 Tag 类中的所有静态字段
     */
    @Nullable
    private static Tag<Material> getBukkitMaterialTag(@NotNull NamespacedKey key) {
        try {
            // 遍历 Tag 类中的所有公共静态字段
            for (java.lang.reflect.Field field : Tag.class.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && 
                    java.lang.reflect.Modifier.isPublic(field.getModifiers())) {
                    
                    Object value = field.get(null);
                    
                    // 检查是否是 Tag<Material>
                    if (value instanceof Tag) {
                        Tag<?> tag = (Tag<?>) value;
                        
                        // 检查标签的键是否匹配
                        if (tag instanceof Keyed) {
                            Keyed keyedTag = (Keyed) tag;
                            if (keyedTag.getKey().equals(key)) {
                                // 验证是否是 Material 标签
                                try {
                                    @SuppressWarnings("unchecked")
                                    Tag<Material> materialTag = (Tag<Material>) tag;
                                    return materialTag;
                                } catch (ClassCastException ignored) {
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 反射失败，忽略
        }
        
        return null;
    }
    
    /**
     * 获取 Bukkit 预定义的实体标签
     */
    @Nullable
    private static Tag<EntityType> getBukkitEntityTag(@NotNull NamespacedKey key) {
        try {
            // 遍历 Tag 类中的所有公共静态字段
            for (java.lang.reflect.Field field : Tag.class.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && 
                    java.lang.reflect.Modifier.isPublic(field.getModifiers())) {
                    
                    Object value = field.get(null);
                    
                    // 检查是否是 Tag<EntityType>
                    if (value instanceof Tag) {
                        Tag<?> tag = (Tag<?>) value;
                        
                        // 检查标签的键是否匹配
                        if (tag instanceof Keyed) {
                            Keyed keyedTag = (Keyed) tag;
                            if (keyedTag.getKey().equals(key)) {
                                // 验证是否是 EntityType 标签
                                try {
                                    @SuppressWarnings("unchecked")
                                    Tag<EntityType> entityTag = (Tag<EntityType>) tag;
                                    return entityTag;
                                } catch (ClassCastException ignored) {
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 反射失败，忽略
        }
        
        return null;
    }
    
    /**
     * 清除缓存（用于重载配置时）
     */
    public static void clearCache() {
        MATERIAL_TAG_CACHE.clear();
        ENTITY_TAG_CACHE.clear();
    }
}
