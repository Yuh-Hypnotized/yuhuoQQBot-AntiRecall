package com.example;

import net.mamoe.mirai.message.data.MessageChain;

public class messageInfo {
    public int messageID;
    public MessageChain messageContent;
    public messageInfo(int id, MessageChain content) {
        this.messageID = id;
        this.messageContent = content;
    }
}
