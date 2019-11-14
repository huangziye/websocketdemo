package com.zjca.ws;

import org.apache.tomcat.websocket.WsSession;
import org.springframework.stereotype.Component;

import javax.lang.model.element.VariableElement;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: ziye_huang
 * @date: 2019/11/13
 */
@ServerEndpoint(value = "/websocket/{userId}")
@Component
public class WebSocketServer {

    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static int onlineCount = 0;
    //concurrent包的线程安全Map，用来存放每个客户端对应的MyWebSocket对象。
    private static ConcurrentHashMap<String, WebSocketServer> websocketMap = new ConcurrentHashMap<String, WebSocketServer>();

    //与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;

    // 客户端ID
    private String userId = "";

    /**
     * 群发自定义消息
     */
    public static void sendInfo(String message, @PathParam("userId") String userId) throws IOException {
        System.out.println("推送消息到窗口" + userId + "，推送内容:" + message);
        //这里可以设定只推送给这个sid的，为null则全部推送
        for (Map.Entry<String, WebSocketServer> entry : websocketMap.entrySet()) {
            try {
                String uid = entry.getKey();
                WebSocketServer wss = entry.getValue();
                if (userId == null) {
                    wss.sendMessage(message);
                } else if (uid.equals(userId)) {
                    wss.sendMessage(message);
                }
            } catch (IOException e) {
                continue;
            }
        }
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        WebSocketServer.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        WebSocketServer.onlineCount--;
    }

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        List<String> userIds = session.getRequestParameterMap().get("userId");
        if (!userIds.isEmpty()) {
            userId = userIds.get(0);
        }
        websocketMap.put(this.userId, this);
        System.out.println("websocketList->" + websocketMap.toString());
        addOnlineCount();           //在线数加1
        System.out.println("有新窗口开始监听:" + this.userId + ",当前在线人数为" + getOnlineCount());
        this.userId = this.userId;
        try {
            sendMessage("连接成功");
        } catch (IOException e) {
            System.out.println("websocket IO异常");
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        if (websocketMap.get(this.userId) != null) {
            websocketMap.remove(this.userId);
            subOnlineCount();           //在线数减1
            System.out.println("有一连接关闭！当前在线人数为" + getOnlineCount());
        }
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("来自客户端的消息:" + message);
        //群发消息
        for (Map.Entry<String, WebSocketServer> entry : websocketMap.entrySet()) {
            try {
                System.out.println(session.getId());
                String uid = entry.getKey();
                WebSocketServer wss = entry.getValue();
                if (userId == null) {
                    wss.sendMessage(message);
                } else if (uid.equals(userId)) {
                    wss.sendMessage(message);
                }
            } catch (IOException e) {
                continue;
            }
        }
    }

    /**
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("发生错误");
        error.printStackTrace();
    }

    /**
     * 实现服务器主动推送
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }
}
