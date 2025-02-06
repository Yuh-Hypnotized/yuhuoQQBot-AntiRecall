package com.example;

import com.google.gson.Gson;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.Listener;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageRecallEvent.GroupRecall;
import net.mamoe.mirai.message.data.MessageChain;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;

public final class AntiRecall extends JavaPlugin {
    public static final AntiRecall INSTANCE = new AntiRecall();

    private AntiRecall() {
        super(new JvmPluginDescriptionBuilder("com.example.antiRecall", "0.1.0")
                .name("antiRecall")
                .author("Yuh_Hypnotized")

                .build());
    }

    private List<messageInfo> messageCache = new ArrayList<>();

    @Override
    public void onEnable() {
        getLogger().info("Plugin loaded!");

        Config config = loadConfig();
        if (config == null) {
            getLogger().error("Failed to load config file!");
            return;
        }

        List<Long> whitelistedGroupID = config.whitelistedGroupID;
        List<Long> adminID = config.adminID;


        Listener<Event> listener = GlobalEventChannel.INSTANCE.subscribeAlways(Event.class,
                event -> {
            if (event instanceof GroupRecall) {
                GroupRecall groupRecall = (GroupRecall) event;
                if (config.showRecallMsg == true && whitelistedGroupID.contains(groupRecall.getGroup().getId())) {

                    int recalledMessageID = groupRecall.getMessageIds()[0];
                    for (messageInfo m : messageCache) {
                        if (m.messageID == recalledMessageID)

                            groupRecall.getGroup().sendMessage(groupRecall.getAuthor().getNick().toString() + "("
                                    + groupRecall.getAuthorId() + ") 撤回了一条消息: \n"
                                    + m.messageContent.toString().replaceAll("\\[mirai:source:ids=\\[\\d+\\]," +
                                            " internalIds=\\[\\d+\\], from group \\d+ to \\d+ at \\d+\\]", "")
                                    .trim());
                        messageCache.remove(m);
                        break;
                    }
                }
            }
            else if (event instanceof GroupMessageEvent) {
                GroupMessageEvent groupMessageEvent = (GroupMessageEvent) event;

                if (whitelistedGroupID.contains(groupMessageEvent.getGroup().getId())) {
                    MessageChain message = groupMessageEvent.getMessage();
                    int messageID = groupMessageEvent.getSource().getIds()[0];
                    long groupID = groupMessageEvent.getGroup().getId();
                    long userID = groupMessageEvent.getSender().getId();
                    messageCache.add(new messageInfo(messageID, message));

                    String messageString = message.contentToString().trim(); // 去除前后空格

                    if (messageString.startsWith("/")) {
                        // 分割命令和参数（例如 "/help" → ["help"], "/anti-recall toggle" → ["anti-recall", "toggle"]）
                        String[] commandParts = messageString.substring(1).split("\\s+"); // 按空格分割

                        if (commandParts.length == 0) {
                            return; // 空命令
                        }
                        String mainCommand = commandParts[0].toLowerCase(); // 统一小写处理

                        switch (mainCommand) {
                            case "anti-recall":
                                if (commandParts.length >= 2 && commandParts[1].equalsIgnoreCase("toggle")) {
                                    if (adminID.contains(userID)) {
                                        config.showRecallMsg = !config.showRecallMsg;
                                        updateConfig(config);
                                        groupMessageEvent.getGroup().sendMessage(
                                                config.showRecallMsg ? "防撤回已开启" : "防撤回已关闭"
                                        );
                                    } else {
                                        groupMessageEvent.getGroup().sendMessage("您没有管理员权限！");
                                    }
                                }
                                break;

                            default:
                                // 未知命令，可忽略或提示
                                break;
                        }
                    }
                }


            }
                });

    }

    private Config loadConfig() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.json")) {
            if (input == null) {
                getLogger().error("Config file not found!");
                return null;
            }
            InputStreamReader reader = new InputStreamReader(input);
            return new Gson().fromJson(reader, Config.class);
        }
        catch (Exception e) {
            getLogger().error("Failed to load config file!", e);
            return null;
        }
    }
    private void updateConfig(Config newConfig) {
        ObjectMapper objectMapper = new ObjectMapper();
        try{
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File("config.json"),
                    newConfig);
        }
        catch (Exception e) {
            getLogger().error("Failed to save config file!", e);
        }
    }
}