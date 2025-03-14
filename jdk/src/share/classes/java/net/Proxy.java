/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.net.Socket;

/**
 * 该类代表代理设置，通常是一种类型（http, socks）和一个socket地址。
 *
 * <p>一个{@link Proxy}是不可变对象。
 *
 * @see     java.net.ProxySelector
 * @author Yingxian Wang
 * @author Jean-Christophe Collet
 * @since   1.5
 */
public class Proxy {

    /**
     * 代表代理类型
     *
     * @since 1.5
     */
    public enum Type {
        /**
         * 代表直接连接，或代理缺失
         */
        DIRECT,
        /**
         * Represents proxy for high level protocols such as HTTP or FTP.
         * 代表高级协议（如：HTTP/FTP）的代理
         */
        HTTP,
        /**
         * 代表Socks（如：V4/V5）的代理
         */
        SOCKS
    };

    private Type type;
    private SocketAddress sa;

    /**
     * 代表{@link Type#DIRECT}连接的代理设置。
     * 基本上告诉协议处理器不要使用任何代理。
     * 例如：用于创建绕过任何其他全局代理设置（例如：Socks）。
     *
     * <p>{@code Socket s = new Socket(Proxy.NO_PROXY);}
     */
    public final static Proxy NO_PROXY = new Proxy();

    /**
     * 创建代表{@link Type#DIRECT}连接的代理
     */
    private Proxy() {
        type = Type.DIRECT;
        sa = null;
    }

    /**
     * 创建一个代表PROXY连接的条目。
     * 某些组合是非法的，例如：对于HTTP和Socks类型，必须提供{@link java.net.SocketAddress}。
     *
     * <p>使用常量{@link Proxy#NO_PROXY}代表直接连接。
     *
     * @param type 代理的类型
     * @param sa 用于代理的Socket地址
     * @throws IllegalArgumentException 当类型和地址不兼容时
     */
    public Proxy(Type type, SocketAddress sa) {
        if ((type == Type.DIRECT) || !(sa instanceof InetSocketAddress))
            throw new IllegalArgumentException("type " + type + " is not compatible with address " + sa);
        this.type = type;
        this.sa = sa;
    }

    /**
     * 返回代理类型
     *
     * @return a Type representing the proxy type
     */
    public Type type() {
        return type;
    }

    /**
     * 返回代理的socket地址，如果是直接连接，则为{@code null}
     *
     * @return a {@code SocketAddress} representing the socket end
     *         point of the proxy
     */
    public SocketAddress address() {
        return sa;
    }

    /**
     * 构造代表代理的字符串表示。
     * 通过调用代理类型的{@link Type#toString()}方法，然后拼接'@',
     * 和其地址的{@link java.net.Socket#toString()}方法。
     *
     * @return  a string representation of this object.
     */
    public String toString() {
        if (type() == Type.DIRECT)
            return "DIRECT";
        return type() + " @ " + address();
    }

    /**
     * 将此对象与指定对象进行比较。
     * 当且仅当参数不为{@code null}并且它代表与此对象相同的代理时，
     * 结果才为{@code true}。
     *
     * <p>
     * 如果SocketAddress和类型相等，则{@link Proxy}的两个实例
     * 代表相同的地址。
     *
     * @param   obj   要比较的对象
     * @return  {@code true} 如果两个对象相等
     * @see java.net.InetSocketAddress#equals(java.lang.Object)
     */
    public final boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Proxy))
            return false;
        Proxy p = (Proxy) obj;
        if (p.type() == type()) {
            if (address() == null) {
                return (p.address() == null);
            } else
                return address().equals(p.address());
        }
        return false;
    }

    /**
     * 返回该代理的哈希码
     *
     * @return  a hash code value for this Proxy.
     */
    public final int hashCode() {
        if (address() == null)
            return type().hashCode();
        return type().hashCode() + address().hashCode();
    }
}
