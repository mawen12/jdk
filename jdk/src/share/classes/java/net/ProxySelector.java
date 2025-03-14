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

import java.io.IOException;
import java.util.List;
import sun.security.util.SecurityConstants;

/**
 * 选择连接到URL引用的网络资源时要使用的代理服务器（如果有）。
 * 代理选择器是一个此类的具体子类，通过调用{@link #setDefault(java.net.ProxySelector)}
 * 来进行注册。可以通过调用{@link #getDefault()}
 * 来获取注册的代理选择器。
 *
 * <p>当注册了一个代理选择器，例如{@link java.net.URLConnection}
 * 的子类应该为每一个URL请求均调用{@link #select(URI)}方法，以便
 * 代理选择器可以决定是否应使用直接连接或代理连接。{@link #select(URI)}
 * 方法返回一个具有首选连接方法的集合迭代器。
 *
 * <p>如果无法与代理服务器（例如HTTP或SOCKS）建立连接，则调用者应调用
 * {@link #connectFailed(URI, SocketAddress, IOException)}方法
 * 来通知代理选择器该代理服务器不可达。
 *
 * <p>默认的代理选择器强制执行与代理设置相关的<a href="doc-files/net-properties.html#Proxies">
 * 系统属性集合</a>
 *
 * @author Yingxian Wang
 * @author Jean-Christophe Collet
 * @since 1.5
 */
public abstract class ProxySelector {
    /**
     * 系统范围的代理选择器，用于在连接到URL引用的远程对象时
     * 选择要使用的代理服务器（如果有）
     *
     * @see #setDefault(ProxySelector)
     */
    private static ProxySelector theProxySelector;

    static {
        try {
            // 使用系统默认的代理选择器
            Class<?> c = Class.forName("sun.net.spi.DefaultProxySelector");
            if (c != null && ProxySelector.class.isAssignableFrom(c)) {
                theProxySelector = (ProxySelector) c.newInstance();
            }
        } catch (Exception e) {
            theProxySelector = null;
        }
    }

    /**
     * 获取系统范围的代理选择器
     *
     * @throws  SecurityException 如果安全管理器已被初始化并且它拒绝{@link java.net.NetPermission}
     * @see #setDefault(ProxySelector)
     * @return the system-wide {@code ProxySelector}
     * @since 1.5
     */
    public static ProxySelector getDefault() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(SecurityConstants.GET_PROXYSELECTOR_PERMISSION);
        }
        return theProxySelector;
    }

    /**
     * 设置或取消设置系统范围的代理选择器
     *
     * 请注意：非标准协议处理器可能忽略该设置
     *
     * @param ps HTTP代理选择器，如果为{@code null}则取消设置代理选择器
     *
     * @throws  SecurityException 如果安全管理器已被初始化并且它拒绝
     *
     * @see #getDefault()
     * @since 1.5
     */
    public static void setDefault(ProxySelector ps) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(SecurityConstants.SET_PROXYSELECTOR_PERMISSION);
        }
        theProxySelector = ps;
    }

    /**
     * 根据访问资源的协议和访问资源的目标地址选择所有适用的代理。
     *
     * URI的格式定义如下：
     * <ul>
     *     <li>用于http连接的http URI</li>
     *     <li>用于https连接的https URI</li>
     *     <li>用于tcp客户端sockets连接的{@code socket://host:port}</li>
     * </ul>
     *
     * @param   uri 需要连接才能访问的URI
     *
     * @return  代理列表。列表中的每个元素都是{@link java.net.Proxy}类型。
     * 当没有代理可用时，该列表将包含一个{@link java.net.Proxy}类型的元素，
     * 代表直接连接。
     * @throws IllegalArgumentException 如果参数为空
     */
    public abstract List<Proxy> select(URI uri);

    /**
     * 调用以指示连接无法与代理服务器建立连接。该方法的实现可以暂时删除代理
     * 或重新排序{@link #select(URI)}返回的代理序列，使用地址和尝试连接
     * 时捕获的IOException。
     *
     * @param   uri Socket地址处的代理无法处理的URI
     * @param   sa 代理服务其的socket地址
     *
     * @param   ioe 当连接失败时抛出IOException
     * @throws IllegalArgumentException 如果任一参数为空
     */
    public abstract void connectFailed(URI uri, SocketAddress sa, IOException ioe);
}
