package su.nightexpress.quests.quest.command;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.commands.Arguments;
import su.nightexpress.nightcore.commands.Commands;
import su.nightexpress.nightcore.commands.command.NightCommand;
import su.nightexpress.nightcore.commands.context.CommandContext;
import su.nightexpress.nightcore.commands.context.ParsedArguments;
import su.nightexpress.quests.QuestsPlaceholders;
import su.nightexpress.quests.QuestsPlugin;
import su.nightexpress.quests.config.Config;
import su.nightexpress.quests.config.Lang;
import su.nightexpress.quests.config.Perms;
import su.nightexpress.quests.quest.QuestManager;
import su.nightexpress.quests.user.QuestUser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class QuestsCommands {

    public static final String DEFAULT_ALIAS = "quests";

    private static final String ARG_PLAYER = "player";
    private static final String ARG_TYPE = "type";
    private static final String ARG_QUEST_DATA_ID = "questid";

    private static QuestsPlugin plugin;
    private static QuestManager manager;
    private static NightCommand command;

    public static void load(@NotNull QuestsPlugin questsPlugin, @NotNull QuestManager questManager) {
        plugin = questsPlugin;
        manager = questManager;

        command = NightCommand.hub(plugin, Config.FEATURES_QUESTS_ALIASES.get(), builder -> builder
            .localized(Lang.COMMAND_QUESTS_NAME)
            .permission(Perms.COMMAND_QUESTS)
            .description(Lang.COMMAND_QUESTS_DESC)
            .branch(Commands.literal("refresh")
                .permission(Perms.COMMAND_QUESTS_REFRESH)
                .description(Lang.COMMAND_QUESTS_REFRESH_DESC)
                .withArguments(Arguments.playerName(ARG_PLAYER), Arguments.string(ARG_TYPE).optional())
                .executes(QuestsCommands::refreshPlayerQuests)
            )
            .branch(Commands.literal("delete_independent")
                .permission(Perms.COMMAND_QUESTS_DELETE)
                .description(Lang.COMMAND_QUESTS_DELETE_INDEPENDENT_DESC)
                .withArguments(Arguments.playerName(ARG_PLAYER), Arguments.string(ARG_QUEST_DATA_ID).suggestions(QuestsCommands::suggestIndependentQuestIds))
                .executes(QuestsCommands::deleteIndependentQuest)
            )
            .branch(Commands.literal("delete_cooperative")
                .permission(Perms.COMMAND_QUESTS_DELETE)
                .description(Lang.COMMAND_QUESTS_DELETE_COOPERATIVE_DESC)
                .withArguments(Arguments.string(ARG_QUEST_DATA_ID).suggestions(QuestsCommands::suggestCooperativeQuestIds))
                .executes(QuestsCommands::deleteCooperativeQuestGlobal)
            )
            .branch(Commands.literal("delete_competitive")
                .permission(Perms.COMMAND_QUESTS_DELETE)
                .description(Lang.COMMAND_QUESTS_DELETE_COMPETITIVE_DESC)
                .withArguments(Arguments.string(ARG_QUEST_DATA_ID).suggestions(QuestsCommands::suggestCompetitiveQuestIds))
                .executes(QuestsCommands::deleteCompetitiveQuestGlobal)
            )
            .branch(Commands.literal("independent")
                .permission(Perms.COMMAND_QUESTS)
                .description(Lang.COMMAND_QUESTS_INDEPENDENT_DESC)
                .executes(QuestsCommands::openIndependentQuests)
            )
            .branch(Commands.literal("cooperative")
                .permission(Perms.COMMAND_QUESTS)
                .description(Lang.COMMAND_QUESTS_COOPERATIVE_DESC)
                .executes(QuestsCommands::openCooperativeQuests)
            )
            .branch(Commands.literal("competitive")
                .permission(Perms.COMMAND_QUESTS)
                .description(Lang.COMMAND_QUESTS_COMPETITIVE_DESC)
                .executes(QuestsCommands::openCompetitiveQuests)
            )
            .branch(Commands.literal("force_complete")
                .permission(Perms.COMMAND_QUESTS_FORCE_COMPLETE)
                .description(Lang.COMMAND_QUESTS_FORCE_COMPLETE_DESC)
                .withArguments(Arguments.playerName(ARG_PLAYER), Arguments.string(ARG_QUEST_DATA_ID).suggestions(QuestsCommands::suggestAllQuestIds))
                .executes(QuestsCommands::forceCompleteQuest)
            )
            .executes(QuestsCommands::openQuests)
        );
        command.register();
    }

    public static void shutdown() {
        command.unregister();
        command = null;
        manager = null;
        plugin = null;
    }

    private static boolean openQuests(@NotNull CommandContext context, @NotNull ParsedArguments arguments) {
        if (!context.isPlayer()) {
            context.errorPlayerOnly();
            return false;
        }

        Player player = context.getPlayerOrThrow();
        manager.openQuests(player);
        return true;
    }

    private static boolean openIndependentQuests(@NotNull CommandContext context, @NotNull ParsedArguments arguments) {
        Player player = context.getPlayerOrThrow();
        manager.openIndependentQuests(player);
        return true;
    }

    private static boolean openCooperativeQuests(@NotNull CommandContext context, @NotNull ParsedArguments arguments) {
        Player player = context.getPlayerOrThrow();
        manager.openCooperativeQuests(player);
        return true;
    }

    private static boolean openCompetitiveQuests(@NotNull CommandContext context, @NotNull ParsedArguments arguments) {
        Player player = context.getPlayerOrThrow();
        manager.openCompetitiveQuests(player);
        return true;
    }

    private static boolean refreshPlayerQuests(@NotNull CommandContext context, @NotNull ParsedArguments arguments) {
        String playerName = arguments.getString(ARG_PLAYER);
        String type = arguments.getString(ARG_TYPE, "all").toLowerCase();
        
        plugin.getUserManager().manageUser(playerName, user -> {
            if (user == null) {
                context.errorBadPlayer();
                return;
            }

            Player player = user.getPlayer();
            if (player == null) {
                // 离线玩家只能重置刷新时间
                user.setNewQuestsDate(0L);
                plugin.getUserManager().save(user);
                context.send(Lang.QUESTS_REFRESHED_ALL_FOR, replacer -> replacer.replace(QuestsPlaceholders.PLAYER_NAME, user.getName()));
                return;
            }
            
            // 根据类型刷新
            switch (type) {
                case "independent":
                case "独立":
                case "个人":
                    manager.refreshIndependentQuests(player);
                    context.send(Lang.QUESTS_REFRESHED_INDEPENDENT_FOR, replacer -> replacer.replace(QuestsPlaceholders.PLAYER_NAME, user.getName()));
                    break;
                case "cooperative":
                case "合作":
                    // 合作任务是全局的，刷新全服
                    manager.forceRefreshCooperativeQuestsGlobal();
                    context.send(Lang.QUESTS_REFRESHED_COOPERATIVE_GLOBAL);
                    break;
                case "competitive":
                case "竞争":
                    // 竞争任务是全局的，刷新全服
                    manager.forceRefreshCompetitiveQuestsGlobal();
                    context.send(Lang.QUESTS_REFRESHED_COMPETITIVE_GLOBAL);
                    break;
                case "all":
                case "全部":
                default:
                    // 刷新所有类型的任务
                    manager.refreshAllQuests(player);
                    context.send(Lang.QUESTS_REFRESHED_ALL_FOR, replacer -> replacer.replace(QuestsPlaceholders.PLAYER_NAME, user.getName()));
                    break;
            }
        });
        return true;
    }
    
    private static boolean deleteIndependentQuest(@NotNull CommandContext context, @NotNull ParsedArguments arguments) {
        String playerName = arguments.getString(ARG_PLAYER);
        String questDataId = arguments.getString(ARG_QUEST_DATA_ID);
        
        plugin.getUserManager().manageUser(playerName, user -> {
            if (user == null) {
                context.errorBadPlayer();
                return;
            }

            Player player = user.getPlayer();
            if (player == null) {
                context.send(Lang.QUESTS_DELETE_FAILED);
                return;
            }
            
            boolean success = manager.deleteIndependentQuest(player, questDataId);
            if (success) {
                context.send(Lang.QUESTS_DELETED_INDEPENDENT, replacer -> replacer
                    .replace(QuestsPlaceholders.PLAYER_NAME, user.getName())
                    .replace(QuestsPlaceholders.QUEST_DATA_ID, questDataId.substring(0, Math.min(8, questDataId.length())))
                );
            } else {
                context.send(Lang.QUESTS_DELETE_FAILED);
            }
        });
        return true;
    }
    
    private static boolean deleteCooperativeQuestGlobal(@NotNull CommandContext context, @NotNull ParsedArguments arguments) {
        String questDataId = arguments.getString(ARG_QUEST_DATA_ID);
        
        boolean success = manager.deleteCooperativeQuestGlobal(questDataId);
        if (success) {
            context.send(Lang.QUESTS_DELETED_COOPERATIVE, replacer -> replacer
                .replace(QuestsPlaceholders.QUEST_DATA_ID, questDataId.substring(0, Math.min(8, questDataId.length())))
            );
        } else {
            context.send(Lang.QUESTS_DELETE_FAILED);
        }
        return true;
    }
    
    private static boolean deleteCompetitiveQuestGlobal(@NotNull CommandContext context, @NotNull ParsedArguments arguments) {
        String questDataId = arguments.getString(ARG_QUEST_DATA_ID);
        
        boolean success = manager.deleteCompetitiveQuestGlobal(questDataId);
        if (success) {
            context.send(Lang.QUESTS_DELETED_COMPETITIVE, replacer -> replacer
                .replace(QuestsPlaceholders.QUEST_DATA_ID, questDataId.substring(0, Math.min(8, questDataId.length())))
            );
        } else {
            context.send(Lang.QUESTS_DELETE_FAILED);
        }
        return true;
    }
    
    private static boolean forceCompleteQuest(@NotNull CommandContext context, @NotNull ParsedArguments arguments) {
        String playerName = arguments.getString(ARG_PLAYER);
        String questDataId = arguments.getString(ARG_QUEST_DATA_ID);
        
        plugin.getUserManager().manageUser(playerName, user -> {
            if (user == null) {
                context.errorBadPlayer();
                return;
            }

            Player player = user.getPlayer();
            if (player == null) {
                context.send(Lang.QUESTS_FORCE_COMPLETE_FAILED);
                return;
            }
            
            boolean success = manager.forceCompleteQuest(player, questDataId);
            if (success) {
                context.send(Lang.QUESTS_FORCE_COMPLETED, replacer -> replacer
                    .replace(QuestsPlaceholders.PLAYER_NAME, user.getName())
                    .replace(QuestsPlaceholders.QUEST_DATA_ID, questDataId.substring(0, Math.min(8, questDataId.length())))
                );
            } else {
                context.send(Lang.QUESTS_FORCE_COMPLETE_NOT_FOUND);
            }
        });
        return true;
    }
    
    // ==================== 任务ID补全方法 ====================
    
    /**
     * 提供所有任务ID的补全（包括独立任务和全局任务）
     */
    @NotNull
    private static List<String> suggestAllQuestIds(@NotNull su.nightexpress.nightcore.commands.argument.ArgumentReader reader, @NotNull CommandContext context) {
        List<String> suggestions = new ArrayList<>();
        
        // 添加独立任务ID
        suggestions.addAll(suggestIndependentQuestIds(reader, context));
        
        // 添加全局任务ID（合作+竞争）
        suggestions.addAll(suggestCooperativeQuestIds(reader, context));
        suggestions.addAll(suggestCompetitiveQuestIds(reader, context));
        
        return suggestions;
    }
    
    /**
     * 提供独立任务ID的补全
     */
    @NotNull
    private static List<String> suggestIndependentQuestIds(@NotNull su.nightexpress.nightcore.commands.argument.ArgumentReader reader, @NotNull CommandContext context) {
        List<String> suggestions = new ArrayList<>();
        
        // 尝试从参数中获取玩家名
        String playerName = context.getArguments().getString(ARG_PLAYER);
        if (playerName == null || playerName.isEmpty()) {
            // 如果没有指定玩家，使用命令执行者
            Player player = context.getPlayer();
            if (player != null) {
                playerName = player.getName();
            }
        }
        
        if (playerName != null) {
            QuestUser user = plugin.getUserManager().getLoaded(playerName);
            if (user != null) {
                suggestions.addAll(user.getQuestDatas().stream()
                    .map(data -> data.getId().toString())
                    .collect(Collectors.toList()));
            }
        }
        
        return suggestions;
    }
    
    /**
     * 提供合作任务ID的补全
     */
    @NotNull
    private static List<String> suggestCooperativeQuestIds(@NotNull su.nightexpress.nightcore.commands.argument.ArgumentReader reader, @NotNull CommandContext context) {
        return manager.getGlobalQuestManager().getAllGlobalQuests().stream()
            .filter(data -> data.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.COOPERATIVE)
            .map(data -> data.getId().toString())
            .collect(Collectors.toList());
    }
    
    /**
     * 提供竞争任务ID的补全
     */
    @NotNull
    private static List<String> suggestCompetitiveQuestIds(@NotNull su.nightexpress.nightcore.commands.argument.ArgumentReader reader, @NotNull CommandContext context) {
        return manager.getGlobalQuestManager().getAllGlobalQuests().stream()
            .filter(data -> data.getQuestType() == su.nightexpress.quests.quest.definition.QuestType.COMPETITIVE)
            .map(data -> data.getId().toString())
            .collect(Collectors.toList());
    }
}
