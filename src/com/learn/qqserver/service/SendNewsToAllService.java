package com.learn.qqserver.service;

import com.learn.common.Message;
import com.learn.common.MessageType;
import com.learn.utils.Utility;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

public class SendNewsToAllService implements Runnable{
    private Scanner scanner = new Scanner(System.in);

    @Override
    public void run() {
        //为了可以推送多次，用while循环
        while (true) {
            System.out.println("请输入服务器要推送的新闻(输入exit表示退出推送服务）");
            String news = Utility.readString(100);
            if("exit".equals(news)){
                break;
            }
            //构建消息，群发消息
            Message message = new Message();
            message.setSender("服务器");
            message.setContent(news);
            message.setMesType(MessageType.MESSAGE_TO_ALL_MES);

            message.setSendTime(new Date().toString());
            System.out.println("服务器推送消息给所有人 说" + news);

            //遍历当前所有的通信线程，得到socket，并发送message
            HashMap<String, ServerConnectClientThread> hm = ManageClientThreads.getHm();
            Iterator<String> iterator = hm.keySet().iterator();
            while (iterator.hasNext()) {
                String onlineUserId = iterator.next();
                try {
                    ObjectOutputStream oos =
                            new ObjectOutputStream(hm.get(onlineUserId).getSocket().getOutputStream());
                    oos.writeObject(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
