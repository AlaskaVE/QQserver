package com.learn.qqserver.service;

import com.learn.common.Message;
import com.learn.common.MessageType;
import com.learn.common.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 这是服务端，在监听9999，等待客户端连接，并保持通信
 */
public class QQserver {
    private ServerSocket ss = null;
    //创建一个集合，存放多个用户，如果是这些用户登陆，就认为是合法的
    //这里也可以使用ConcurrentHashMap，可以处理并发的集合
    //HashMap 没有处理线程安全，因此在多线程情况下是不安全的
    //ConcurrentHashMap 处理的线程安全，即线程同步处理，在多线程情况下是安全的
    private static ConcurrentHashMap<String,User> validUsers = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, ArrayList<Message>> offLineDb = new ConcurrentHashMap<>();


    public static void addOffLineDb(String getter,Message message){
        ArrayList<Message> messages = new ArrayList<>();
        messages.add(message);
        offLineDb.put(getter,messages);
    }

    static {//在静态代码块，初始化validUsers

        validUsers.put("100",new User("100","123456"));
        validUsers.put("200",new User("200","123456"));
        validUsers.put("300",new User("300","123456"));
        validUsers.put("至尊宝",new User("至尊宝","123456"));
        validUsers.put("紫霞仙子",new User("紫霞仙子","123456"));
        validUsers.put("菩提老祖",new User("菩提老祖","123456"));

    }

    //验证用户是否有效的方法(过关斩将)
    public boolean checkUser(String userId,String pwd){
        User user = validUsers.get(userId);
        if(user == null){//如果user为空，说明userId不存在集合中
            return false;
        }
        if(!user.getPasswd().equals(pwd)){
            return false;
        }
        return true;
    }
    public QQserver(){
        //注意：端口可以写在配置文件中
        try {
            System.out.println("服务器在9999端口监听....");
            //启动推送服务
            new Thread(new SendNewsToAllService()).start();

            ss  = new ServerSocket(9999);
            while (true) {//当和某个客户端建立连接后，会继续监听，所以用while
                Socket socket = ss.accept();//如果没有客户端连接，就会阻塞在这里
                //得到socket关联的对象输入流
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                //得到socket关联的输出流
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                User u = (User) ois.readObject();//读取客户端发送的User对象
                //创建一个Message对象，准备回复客户端
                Message message = new Message();
                //验证
                if(checkUser(u.getUserId(),u.getPasswd())){//登陆成功
                    message.setMesType(MessageType.MESSAGE_LOGIN_SUCCEED);
                    //将Message对象回复
                    oos.writeObject(message);
                    //创建一个线程，和客户端保持通信，该线程需要持有socket
                    ServerConnectClientThread serverConnectClientThread = new ServerConnectClientThread(socket, u.getUserId());
                    //启动线程
                    serverConnectClientThread.start();
                    //把该线程对象放入到一个集合中进行管理
                    ManageClientThreads.addClientThread(u.getUserId(),serverConnectClientThread);

                    Iterator<String> iterator = offLineDb.keySet().iterator();
                    while (iterator.hasNext()){
                       String key = iterator.next();
                        if(u.getUserId().equals(key)){
                            ArrayList<Message> messages = new ArrayList<>();
                            messages = offLineDb.get(key);
                            for (int i = 0; i < messages.size(); i++) {

                                ObjectOutputStream oos2 = null;
                                try {
                                    oos2 = new ObjectOutputStream(socket.getOutputStream());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                oos2.writeObject(messages.get(i));

                            }
                            offLineDb.remove(key);

                        }

                    }

                }else {//登陆失败
                    System.out.println("用户 id = " + u.getUserId() + "验证失败");
                   message.setMesType(MessageType.MESSAGE_LOGIN_FAIL);
                   oos.writeObject(message);
                   socket.close();
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //如果服务器端退出了while，说明服务器不再监听，因此关闭ServerSocket
            try {
                ss.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
