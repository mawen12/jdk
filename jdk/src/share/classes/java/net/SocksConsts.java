/*
 * Copyright (c) 2000, 2001, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package java.net;

/**
 * 被 SOCKS 协议实现使用的常量
 */
interface SocksConsts {
    /**
     * 版本4
     */
    static final int PROTO_VERS4                = 4;
    /**
     * 版本5
     */
    static final int PROTO_VERS         = 5;
    /**
     * 默认端口
     */
    static final int DEFAULT_PORT               = 1080;

    /**
     * 没有身份认证
     */
    static final int NO_AUTH            = 0;
    static final int GSSAPI             = 1;
    /**
     * 用户+密码身份认证
     */
    static final int USER_PASSW         = 2;
    static final int NO_METHODS         = -1;

    static final int CONNECT            = 1;
    static final int BIND                       = 2;
    static final int UDP_ASSOC          = 3;

    /**
     * IPv4地址
     */
    static final int IPV4                       = 1;
    /**
     * 域名
     */
    static final int DOMAIN_NAME                = 3;
    /**
     * IPv6地址
     */
    static final int IPV6                       = 4;

    /**
     * 请求成功
     */
    static final int REQUEST_OK         = 0;
    /**
     * 一般故障
     */
    static final int GENERAL_FAILURE    = 1;
    /**
     * 不允许
     */
    static final int NOT_ALLOWED                = 2;
    /**
     * 网络不可达
     */
    static final int NET_UNREACHABLE    = 3;
    /**
     * 主机不可达
     */
    static final int HOST_UNREACHABLE   = 4;
    /**
     * 连接被拒绝
     */
    static final int CONN_REFUSED               = 5;
    /**
     * TTL过期
     */
    static final int TTL_EXPIRED                = 6;
    /**
     * 不支持命令
     */
    static final int CMD_NOT_SUPPORTED  = 7;
    /**
     * 地址类型不支持
     */
    static final int ADDR_TYPE_NOT_SUP  = 8;
}
