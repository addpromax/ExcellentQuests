package su.nightexpress.quests.data.serialize;

import com.google.gson.*;
import su.nightexpress.quests.quest.data.GlobalQuestData;
import su.nightexpress.quests.quest.data.QuestCounter;
import su.nightexpress.quests.quest.definition.QuestType;

import java.lang.reflect.Type;
import java.util.*;

public class GlobalQuestDataSerializer implements JsonSerializer<GlobalQuestData>, JsonDeserializer<GlobalQuestData> {

    @Override
    public GlobalQuestData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = json.getAsJsonObject();

        UUID id = UUID.fromString(object.get("id").getAsString());
        String questId = object.get("questId").getAsString();
        QuestType questType = QuestType.fromString(object.get("questType").getAsString());

        Map<String, QuestCounter> objectives = new HashMap<>();
        JsonObject objectivesRoot = object.get("objectives").getAsJsonObject();
        objectivesRoot.asMap().forEach((fullName, counterJson) -> {
            QuestCounter counter = context.deserialize(counterJson, QuestCounter.class);
            objectives.put(fullName, counter);
        });

        int maxCompletionCount = object.get("maxCompletionCount").getAsInt();
        
        Set<UUID> completedPlayers = new HashSet<>();
        JsonArray playersArray = object.getAsJsonArray("completedPlayers");
        playersArray.forEach(element -> {
            completedPlayers.add(UUID.fromString(element.getAsString()));
        });
        
        int maxOnlinePlayerCount = object.get("maxOnlinePlayerCount").getAsInt();
        
        // 读取玩家贡献度
        Map<UUID, Integer> playerContributions = new HashMap<>();
        if (object.has("playerContributions")) {
            JsonObject contributionsObj = object.getAsJsonObject("playerContributions");
            contributionsObj.asMap().forEach((playerIdStr, contributionElement) -> {
                try {
                    UUID playerId = UUID.fromString(playerIdStr);
                    int contribution = contributionElement.getAsInt();
                    playerContributions.put(playerId, contribution);
                } catch (Exception ignored) {
                }
            });
        }
        
        boolean active = object.get("active").getAsBoolean();
        long expireDate = object.get("expireDate").getAsLong();
        long createDate = object.get("createDate").getAsLong();
        
        // 读取 needsObjectiveInitialization 字段（如果不存在则默认为 false）
        boolean needsObjectiveInitialization = object.has("needsObjectiveInitialization") ? 
            object.get("needsObjectiveInitialization").getAsBoolean() : false;

        GlobalQuestData globalData = new GlobalQuestData(id, questId, questType, objectives, maxCompletionCount, 
            completedPlayers, maxOnlinePlayerCount, playerContributions, active, expireDate, createDate);
        globalData.setNeedsObjectiveInitialization(needsObjectiveInitialization);
        return globalData;
    }

    @Override
    public JsonElement serialize(GlobalQuestData data, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject object = new JsonObject();

        object.addProperty("id", data.getId().toString());
        object.addProperty("questId", data.getQuestId());
        object.addProperty("questType", data.getQuestType().name());

        JsonObject objectivesRoot = new JsonObject();
        data.getObjectiveCounterMap().forEach((fullName, counter) -> {
            objectivesRoot.add(fullName, context.serialize(counter, QuestCounter.class));
        });

        object.add("objectives", objectivesRoot);
        object.addProperty("maxCompletionCount", data.getMaxCompletionCount());
        
        JsonArray playersArray = new JsonArray();
        data.getCompletedPlayers().forEach(playerId -> {
            playersArray.add(playerId.toString());
        });
        object.add("completedPlayers", playersArray);
        
        object.addProperty("maxOnlinePlayerCount", data.getMaxOnlinePlayerCount());
        
        // 保存玩家贡献度
        JsonObject contributionsObj = new JsonObject();
        data.getPlayerContributions().forEach((playerId, contribution) -> {
            contributionsObj.addProperty(playerId.toString(), contribution);
        });
        object.add("playerContributions", contributionsObj);
        
        object.addProperty("active", data.isActive());
        object.addProperty("expireDate", data.getExpireDate());
        object.addProperty("createDate", data.getCreateDate());
        object.addProperty("needsObjectiveInitialization", data.needsObjectiveInitialization());

        return object;
    }
}
