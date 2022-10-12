package com.learn.qqframe;

import com.learn.qqserver.service.QQserver;

/**
 * 该类创建QQServer，启动后台服务
 */
public class QQFrame {
    public static void main(String[] args) {
        new QQserver();
    }
}
