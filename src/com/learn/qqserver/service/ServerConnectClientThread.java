package com.learn.qqserver.service;

import com.learn.common.Message;
import com.learn.common.MessageType;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;

/**
 * 该类的一个对象和某个客户端保持通讯
 */
public class ServerConnectClientThread extends Thread{
    private Socket socket;
    private String userId;//连接到服务端的用户Id

    public ServerConnectClientThread(Socket socket, String userId) {
        this.socket = socket;
        this.userId = userId;
    }

    public Socket getSocket() {
        return socket;
    }

    @Override
    public void run() {//这里线程处于run状态，可以发送/接收消息
        while (true){
            System.out.println("服务端和客户端 " + userId + "保持通信，读取数据....");
            try {
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                Message m = (Message)ois.readObject();
                //后面会使用这个message,根据message的类型做相应的业务处理
                if(m.getMesType().equals(MessageType.MESSAGE_GET_ONLINE_FRIEND)){
                    //客户端要在线用户列表
                    /*
                    在线用户列表形式 100 100 紫霞仙子
                     */
                    System.out.println(m.getSender() + "要在线用户列表");
                    String onlineUser = ManageClientThreads.getOnlineUser();
                    //返回Message
                    //创建一个Message对象，返回给客户端
                    Message message = new Message();
                    message.setMesType(MessageType.MESSAGE_RET_ONLINE_FRIEND);
                    message.setContent(onlineUser);
                    message.setGetter(m.getSender());
                    //写入到数据通道,返回给客户端
                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    oos.writeObject(message);
                    

                }else if(m.getMesType().equals(MessageType.MESSAGE_CLIENT_EXIT)){//客户端退出
                    System.out.println(m.getSender() + "退出");
                    //将这个客户端的线程,从集合里删除
                    ManageClientThreads.removeServerConnectClientThread(m.getSender());
                    socket.close();//关闭连接
                    break;//退出while循环
                }else if(m.getMesType().equals(MessageType.MESSAGE_COMM_MES)){
                    //根据message获取getter id，然后再得到对应的线程
                    ServerConnectClientThread serverConnectClientThread = ManageClientThreads.getServerConnectClientThread(m.getGetter());
                    //得到对应socket的对象输出流，将message对象转发给指定的客户端
                    if(serverConnectClientThread != null) {
                        ObjectOutputStream oos = new ObjectOutputStream(serverConnectClientThread.socket.getOutputStream());
                        oos.writeObject(m);//转发，提示如果客户不在线，可以保持到数据库，这样就可以实现离线留言
                    }else {
                        QQserver.addOffLineDb(m.getGetter(),m);
                    }
                }else if(m.getMesType().equals(MessageType.MESSAGE_TO_ALL_MES)){
                    //需要遍历管理线程的集合，把所有的线程的socket得到，然后把message进行转发
                    HashMap<String, ServerConnectClientThread> hm = ManageClientThreads.getHm();

                    Iterator<String> iterator = hm.keySet().iterator();
                    while (iterator.hasNext()){
                        //取出在线用户的id
                        String onLineUserId = iterator.next();
                        if(!onLineUserId.equals(m.getSender())){//排除群发消息的这个用户
                            //进行转发message
                            ObjectOutputStream oos = new ObjectOutputStream
                                    (hm.get(onLineUserId).socket.getOutputStream());
                            oos.writeObject(m);

                        }
                    }
                }else if(m.getMesType().equals(MessageType.MESSAGE_FILE_MES)){
                    //根据getterId获取到对应的线程，将message转发
                  ServerConnectClientThread serverConnectClientThread =
                          ManageClientThreads.getServerConnectClientThread(m.getGetter());
                  if(serverConnectClientThread != null) {
                      ObjectOutputStream oos = new ObjectOutputStream
                              (serverConnectClientThread.socket.getOutputStream());
                      //转发
                      System.out.println("开始转发");
                      oos.writeObject(m);
                  }else {
                     QQserver.addOffLineDb(m.getGetter(),m);
                  }

                }
                else {
                    System.out.println("其他类型暂时不处理");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
