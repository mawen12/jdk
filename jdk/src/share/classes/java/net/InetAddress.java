/*
 * Copyright (c) 1995, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.ServiceLoader;
import java.security.AccessController;
import java.io.ObjectStreamException;
import java.io.ObjectStreamField;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;
import sun.security.action.*;
import sun.net.InetAddressCachePolicy;
import sun.net.util.IPAddressUtil;
import sun.net.spi.nameservice.*;

/**
 * 该类代表网络协议(Internet Protocol)地址。
 *
 * <p>IP地址是IP使用的32位或128位无符号数，IP是构建UDP和TCP等协议
 * 的低级协议。IP地址架构由<a
 * href="http://www.ietf.org/rfc/rfc790.txt"><i>RFC&nbsp;790:
 * Assigned Numbers</i></a>, <a
 * href="http://www.ietf.org/rfc/rfc1918.txt"> <i>RFC&nbsp;1918:
 * Address Allocation for Private Internets</i></a>, <a
 * href="http://www.ietf.org/rfc/rfc2365.txt"><i>RFC&nbsp;2365:
 * Administratively Scoped IP Multicast</i></a>, and <a
 * href="http://www.ietf.org/rfc/rfc2373.txt"><i>RFC&nbsp;2373: IP
 * Version 6 Addressing Architecture</i></a>所定义。
 *
 * <p>InetAddress的实例由一个IP地址和可能其对应的主机名组成（取决于它是否用
 * 主机名构造或者是否已经完成反向主机名解析）。
 *
 * <h3>地址类型</h3>
 *
 * <blockquote><table cellspacing=2 summary="单播和多播地址类型描述"></blockquote>
 * <tr><th valign=top><i>单播</i></th>
 *  <td>单个接口的标识符。发送到单播地址的数据包将被传送到该地址标识的接口。
 *
 *  <p>未指定地址 -- 也被称作任意本地地址或通配符地址。据不能将其分配给任何地址。
 *  它表示没有地址，其用途的一个示例是作为绑定的目标，如果服务器主机有多个接口，
 *  它允许服务器在任何接口上接受客户端连接。
 *
 *  <p>未指定地址不能用作IP数据包的目标地址。
 *
 *  <p>回环(Loopback)地址 -- 这是分配给 loopback 接口的地址。发送到此IP地址的
 *  任何内容都会循环并成为本地主机上的IP输入。此地址通常在测试客户端时使用。
 *  </td></tr>
 *
 * <tr><th valign=top><i>多播</i></th>
 *  <td>一组接口的标识符（通常属于不同的节点）。发送到多播地址的数据包将传送到该地址标识
 *  的所有接口</td>
 *
 * <h4>IP地址范围</h4>
 *
 * <p>{@code Link-local}地址旨在用于单个链路上的寻址，以实现自动地址配置、邻居发现，
 * 或不存在路由器等目的。
 *
 * <p>{@code Site-local}地址旨在用于站点内部寻址，无需全局前缀。
 *
 * <p>{@code Global}地址在互联网上是唯一的。
 *
 * <h4>IP地址的文本标识</h4>
 *
 * <p>IP地址的文本标识是特定于地址系列的。
 *
 * <p>对于IPv4地址格式，请参阅<a href="Inet4Address.html#format">Inet4Address#format</a>;
 * 对于IPv6地址格式，请参阅<a href="Inet6Address.html#format">Inet6Address#format</a>
 *
 * <p>此处列举出<a href="doc-files/net-properties.html#Ipv4IPv6">几个系统属性</a>
 * 会影响IPv4和IPv6地址的使用方式。
 *
 * <h4>主机名解析</h4>
 *
 * 主机名到IP的解析是通过结合使用本地计算机配置信息和网络命令服务（如域名系统（DNS）
 * 和网络信息服务（NIS））来实现的。所使用的特定命名服务默认为本地计算机所配置的命令服务。
 * 对于任何主机名，都会返回其对应的IP地址。
 *
 * <p>反向名称解析，表示对于任何IP地址，返回与IP地址关联的主机。
 *
 * <p>{@link InetAddress}类提供将主机名解析为IP地址的方法，反之亦然。
 *
 * <h4>InetAddress缓存</h4>
 *
 * {@code InetAddress}类有一个缓存来存储成功和不成功的主机名解析。
 *
 * <p>默认情况下，安装安全管理器后，为了防止DNS欺骗攻击，主机名正确解析的结果将被永久缓存。
 * 当未安装安全管理器时，默认行为是在有限的（依赖于实现的）时间段内缓存条目。主机名解析失败
 * 的结果会被缓存很短的时间（10秒），以提升性能。
 *
 * <p>如果不希望使用默认行为，则可以将Java安全属性设置为不同的生成时间（TTL）值以进行正向
 * 缓存。同样，系统管理员可以在需要时配置不同的负向缓存TTL值。
 *
 * <p>两个Java安全属性空值用于正向和负向主机名解析缓存的TTL值。
 *
 * <blockquote>
 * <dl>
 *  <dt><b>networkaddress.cache.ttl</b></dt>
 *  <dd>表示从名称服务成功查找名称的缓存策略。该值指定为整数，表示缓存成功查找的秒数。
 *  默认设置是缓存一段特定的时间。
 *
 *      <p>-1值表示永久缓存。
 *  </dd>
 *  <dt><b>networkaddress.cache.negative.ttl</b>(默认：10)</dt>
 *  <dd>表示名称服务中未成功名称查找的缓存策略。该值指定为整数，表示缓存未成功查找失败的秒数
 *
 *      <p>值为0表示永不缓存
 *      值为-1表示永远缓存
 *  </dd>
 *  </dl>
 *  </blockquote>
 *
 * @author  Chris Warth
 * @see     java.net.InetAddress#getByAddress(byte[])
 * @see     java.net.InetAddress#getByAddress(java.lang.String, byte[])
 * @see     java.net.InetAddress#getAllByName(java.lang.String)
 * @see     java.net.InetAddress#getByName(java.lang.String)
 * @see     java.net.InetAddress#getLocalHost()
 * @since JDK1.0
 */
public class InetAddress implements java.io.Serializable {
    /**
     * 指定地址系列：网络协议，版本 4
     *
     * @since 1.4
     */
    static final int IPv4 = 1;

    /**
     * 指定地址系列：网络协议，版本 6
     *
     * @since 1.4
     */
    static final int IPv6 = 2;

    /**
     * 指定地址系列首选项
     */
    static transient boolean preferIPv6Address = false;

    static class InetAddressHolder {

        InetAddressHolder() {}

        InetAddressHolder(String hostName, int address, int family) {
            this.hostName = hostName;
            this.address = address;
            this.family = family;
        }

        void init(String hostName, int family) {
            this.hostName = hostName;
            if (family != -1) {
                this.family = family;
            }
        }

        String hostName;

        String getHostName() {
            return hostName;
        }

        /**
         * 持有32位IPv4地址
         */
        int address;

        int getAddress() {
            return address;
        }

        /**
         * 指定地址系列类型，例如：IPv4地址为1，IPv6地址为2
         */
        int family;

        int getFamily() {
            return family;
        }
    }

    /**
     * 用于存储InetAddress的可序列化字段
     */
    final transient InetAddressHolder holder;

    InetAddressHolder holder() {
        return holder;
    }

    /**
     * 用于存储名称服务提供者
     */
    private static List<NameService> nameServices = null;

    /**
     * 用于存储最佳可用主机名
     */
    private transient String canonicalHostName = null;

    /**
     * 从 JDK 1.0.2 开始使用serialVersionUID实现互操作性
     */
    private static final long serialVersionUID = 3286316764910316507L;

    /*
     * 将网络库加载到运行时，并执行初始化
     */
    static {
        preferIPv6Address = java.security.AccessController.doPrivileged(
            new GetBooleanAction("java.net.preferIPv6Addresses")).booleanValue();
        AccessController.doPrivileged(
            new java.security.PrivilegedAction<Void>() {
                public Void run() {
                    System.loadLibrary("net");
                    return null;
                }
            });
        init();
    }

    /**
     * {@link java.net.Socket#accept()}方法的构造函数。
     * 这将创建一个空的InetAddress，由{@link java.net.Socket#accept()}
     * 方法填充。但是，这个InetAddress不会放入地址缓存中，
     * 因为它不是通过名称创建的。
     */
    InetAddress() {
        holder = new InetAddressHolder();
    }

    /**
     * 使用Inet4Address对象替换反序列化对象
     *
     * @return 反系列化对象的替代对象
     *
     * @throws ObjectStreamException 如果无法创建替换此对象的新对象
     */
    private Object readResolve() throws ObjectStreamException {
        // 将替换反序列化的'this'对象
        return new Inet4Address(holder().getHostName(), holder().getAddress());
    }

    /**
     * 用于检查InetAddress是否为IP多播地址的实用程序
     *
     * @return {@code false} 单播地址
     * @since   JDK1.1
     */
    public boolean isMulticastAddress() {
        return false;
    }

    /**
     * 用于检查InetAddress是否为通配符地址的实用程序。
     *
     *
     * @return {@code false} 非通配符地址
     * @since 1.4
     */
    public boolean isAnyLocalAddress() {
        return false;
    }

    /**
     * 用于检查InetAddress是否为回环地址的实用程序。
     *
     * @return {@code false} 非回环地址
     * @since 1.4
     */
    public boolean isLoopbackAddress() {
        return false;
    }

    /**
     * 用于检查InetAddress是否为link local地址的实用程序
     *
     * @return {@code false} 非link lock单播地址
     * @since 1.4
     */
    public boolean isLinkLocalAddress() {
        return false;
    }

    /**
     * 用于检查InetAddress是否为site local地址的实用程序
     *
     * @return {@code false} 非site local单播地址
     * @since 1.4
     */
    public boolean isSiteLocalAddress() {
        return false;
    }

    /**
     * 用于检查多播地址是否具有global范围的实用程序
     *
     * @return {@code false} 非多播地址，或非global范围
     * @since 1.4
     */
    public boolean isMCGlobal() {
        return false;
    }

    /**
     * 用于检查多播地址是否具有node范围的实用程序
     *
     * @return {@code false} 非多播地址，或非node范围
     * @since 1.4
     */
    public boolean isMCNodeLocal() {
        return false;
    }

    /**
     * 用于检查多播地址是否具有link local范围的实用程序
     *
     * @return {@code false} 非多播地址，或非link local范围
     * @since 1.4
     */
    public boolean isMCLinkLocal() {
        return false;
    }

    /**
     * 用于检查多播地址是否具有site-local范围的实用程序
     *
     * @return a {@code false} 非多播地址，或非site-local范围
     * @since 1.4
     */
    public boolean isMCSiteLocal() {
        return false;
    }

    /**
     * 用于检查多播地址是否具有organization-local范围的实用程序
     *
     * @return a {@code false} 非多播地址，或非organization-local范围
     * @since 1.4
     */
    public boolean isMCOrgLocal() {
        return false;
    }


    /**
     * 测试地址是否可达。
     *
     * Test whether that address is reachable. Best effort is made by the
     * implementation to try to reach the host, but firewalls and server
     * configuration may block requests resulting in a unreachable status
     * while some specific ports may be accessible.
     * A typical implementation will use ICMP ECHO REQUESTs if the
     * privilege can be obtained, otherwise it will try to establish
     * a TCP connection on port 7 (Echo) of the destination host.
     * <p>
     * The timeout value, in milliseconds, indicates the maximum amount of time
     * the try should take. If the operation times out before getting an
     * answer, the host is deemed unreachable. A negative value will result
     * in an IllegalArgumentException being thrown.
     *
     * @param   timeout the time, in milliseconds, before the call aborts
     * @return a {@code boolean} indicating if the address is reachable.
     * @throws IOException if a network error occurs
     * @throws  IllegalArgumentException if {@code timeout} is negative.
     * @since 1.5
     */
    public boolean isReachable(int timeout) throws IOException {
        return isReachable(null, 0 , timeout);
    }

    /**
     * Test whether that address is reachable. Best effort is made by the
     * implementation to try to reach the host, but firewalls and server
     * configuration may block requests resulting in a unreachable status
     * while some specific ports may be accessible.
     * A typical implementation will use ICMP ECHO REQUESTs if the
     * privilege can be obtained, otherwise it will try to establish
     * a TCP connection on port 7 (Echo) of the destination host.
     * <p>
     * The {@code network interface} and {@code ttl} parameters
     * let the caller specify which network interface the test will go through
     * and the maximum number of hops the packets should go through.
     * A negative value for the {@code ttl} will result in an
     * IllegalArgumentException being thrown.
     * <p>
     * The timeout value, in milliseconds, indicates the maximum amount of time
     * the try should take. If the operation times out before getting an
     * answer, the host is deemed unreachable. A negative value will result
     * in an IllegalArgumentException being thrown.
     *
     * @param   netif   the NetworkInterface through which the
     *                    test will be done, or null for any interface
     * @param   ttl     the maximum numbers of hops to try or 0 for the
     *                  default
     * @param   timeout the time, in milliseconds, before the call aborts
     * @throws  IllegalArgumentException if either {@code timeout}
     *                          or {@code ttl} are negative.
     * @return a {@code boolean}indicating if the address is reachable.
     * @throws IOException if a network error occurs
     * @since 1.5
     */
    public boolean isReachable(NetworkInterface netif, int ttl,
                               int timeout) throws IOException {
        if (ttl < 0)
            throw new IllegalArgumentException("ttl can't be negative");
        if (timeout < 0)
            throw new IllegalArgumentException("timeout can't be negative");

        return impl.isReachable(this, timeout, netif, ttl);
    }

    /**
     * Gets the host name for this IP address.
     *
     * <p>If this InetAddress was created with a host name,
     * this host name will be remembered and returned;
     * otherwise, a reverse name lookup will be performed
     * and the result will be returned based on the system
     * configured name lookup service. If a lookup of the name service
     * is required, call
     * {@link #getCanonicalHostName() getCanonicalHostName}.
     *
     * <p>If there is a security manager, its
     * {@code checkConnect} method is first called
     * with the hostname and {@code -1}
     * as its arguments to see if the operation is allowed.
     * If the operation is not allowed, it will return
     * the textual representation of the IP address.
     *
     * @return  the host name for this IP address, or if the operation
     *    is not allowed by the security check, the textual
     *    representation of the IP address.
     *
     * @see InetAddress#getCanonicalHostName
     * @see SecurityManager#checkConnect
     */
    public String getHostName() {
        return getHostName(true);
    }

    /**
     * Returns the hostname for this address.
     * If the host is equal to null, then this address refers to any
     * of the local machine's available network addresses.
     * this is package private so SocketPermission can make calls into
     * here without a security check.
     *
     * <p>If there is a security manager, this method first
     * calls its {@code checkConnect} method
     * with the hostname and {@code -1}
     * as its arguments to see if the calling code is allowed to know
     * the hostname for this IP address, i.e., to connect to the host.
     * If the operation is not allowed, it will return
     * the textual representation of the IP address.
     *
     * @return  the host name for this IP address, or if the operation
     *    is not allowed by the security check, the textual
     *    representation of the IP address.
     *
     * @param check make security check if true
     *
     * @see SecurityManager#checkConnect
     */
    String getHostName(boolean check) {
        if (holder().getHostName() == null) {
            holder().hostName = InetAddress.getHostFromNameService(this, check);
        }
        return holder().getHostName();
    }

    /**
     * Gets the fully qualified domain name for this IP address.
     * Best effort method, meaning we may not be able to return
     * the FQDN depending on the underlying system configuration.
     *
     * <p>If there is a security manager, this method first
     * calls its {@code checkConnect} method
     * with the hostname and {@code -1}
     * as its arguments to see if the calling code is allowed to know
     * the hostname for this IP address, i.e., to connect to the host.
     * If the operation is not allowed, it will return
     * the textual representation of the IP address.
     *
     * @return  the fully qualified domain name for this IP address,
     *    or if the operation is not allowed by the security check,
     *    the textual representation of the IP address.
     *
     * @see SecurityManager#checkConnect
     *
     * @since 1.4
     */
    public String getCanonicalHostName() {
        if (canonicalHostName == null) {
            canonicalHostName =
                InetAddress.getHostFromNameService(this, true);
        }
        return canonicalHostName;
    }

    /**
     * Returns the hostname for this address.
     *
     * <p>If there is a security manager, this method first
     * calls its {@code checkConnect} method
     * with the hostname and {@code -1}
     * as its arguments to see if the calling code is allowed to know
     * the hostname for this IP address, i.e., to connect to the host.
     * If the operation is not allowed, it will return
     * the textual representation of the IP address.
     *
     * @return  the host name for this IP address, or if the operation
     *    is not allowed by the security check, the textual
     *    representation of the IP address.
     *
     * @param check make security check if true
     *
     * @see SecurityManager#checkConnect
     */
    private static String getHostFromNameService(InetAddress addr, boolean check) {
        String host = null;
        for (NameService nameService : nameServices) {
            try {
                // first lookup the hostname
                host = nameService.getHostByAddr(addr.getAddress());

                /* check to see if calling code is allowed to know
                 * the hostname for this IP address, ie, connect to the host
                 */
                if (check) {
                    SecurityManager sec = System.getSecurityManager();
                    if (sec != null) {
                        sec.checkConnect(host, -1);
                    }
                }

                /* now get all the IP addresses for this hostname,
                 * and make sure one of them matches the original IP
                 * address. We do this to try and prevent spoofing.
                 */

                InetAddress[] arr = InetAddress.getAllByName0(host, check);
                boolean ok = false;

                if(arr != null) {
                    for(int i = 0; !ok && i < arr.length; i++) {
                        ok = addr.equals(arr[i]);
                    }
                }

                //XXX: if it looks a spoof just return the address?
                if (!ok) {
                    host = addr.getHostAddress();
                    return host;
                }

                break;

            } catch (SecurityException e) {
                host = addr.getHostAddress();
                break;
            } catch (UnknownHostException e) {
                host = addr.getHostAddress();
                // let next provider resolve the hostname
            }
        }

        return host;
    }

    /**
     * Returns the raw IP address of this {@code InetAddress}
     * object. The result is in network byte order: the highest order
     * byte of the address is in {@code getAddress()[0]}.
     *
     * @return  the raw IP address of this object.
     */
    public byte[] getAddress() {
        return null;
    }

    /**
     * Returns the IP address string in textual presentation.
     *
     * @return  the raw IP address in a string format.
     * @since   JDK1.0.2
     */
    public String getHostAddress() {
        return null;
     }

    /**
     * Returns a hashcode for this IP address.
     *
     * @return  a hash code value for this IP address.
     */
    public int hashCode() {
        return -1;
    }

    /**
     * Compares this object against the specified object.
     * The result is {@code true} if and only if the argument is
     * not {@code null} and it represents the same IP address as
     * this object.
     * <p>
     * Two instances of {@code InetAddress} represent the same IP
     * address if the length of the byte arrays returned by
     * {@code getAddress} is the same for both, and each of the
     * array components is the same for the byte arrays.
     *
     * @param   obj   the object to compare against.
     * @return  {@code true} if the objects are the same;
     *          {@code false} otherwise.
     * @see     java.net.InetAddress#getAddress()
     */
    public boolean equals(Object obj) {
        return false;
    }

    /**
     * Converts this IP address to a {@code String}. The
     * string returned is of the form: hostname / literal IP
     * address.
     *
     * If the host name is unresolved, no reverse name service lookup
     * is performed. The hostname part will be represented by an empty string.
     *
     * @return  a string representation of this IP address.
     */
    public String toString() {
        String hostName = holder().getHostName();
        return ((hostName != null) ? hostName : "")
            + "/" + getHostAddress();
    }

    /*
     * Cached addresses - our own litle nis, not!
     */
    private static Cache addressCache = new Cache(Cache.Type.Positive);

    private static Cache negativeCache = new Cache(Cache.Type.Negative);

    private static boolean addressCacheInit = false;

    static InetAddress[]    unknown_array; // put THIS in cache

    static InetAddressImpl  impl;

    private static final HashMap<String, Void> lookupTable = new HashMap<>();

    /**
     * Represents a cache entry
     */
    static final class CacheEntry {

        CacheEntry(InetAddress[] addresses, long expiration) {
            this.addresses = addresses;
            this.expiration = expiration;
        }

        InetAddress[] addresses;
        long expiration;
    }

    /**
     * A cache that manages entries based on a policy specified
     * at creation time.
     */
    static final class Cache {
        private LinkedHashMap<String, CacheEntry> cache;
        private Type type;

        enum Type {Positive, Negative};

        /**
         * Create cache
         */
        public Cache(Type type) {
            this.type = type;
            cache = new LinkedHashMap<String, CacheEntry>();
        }

        private int getPolicy() {
            if (type == Type.Positive) {
                return InetAddressCachePolicy.get();
            } else {
                return InetAddressCachePolicy.getNegative();
            }
        }

        /**
         * Add an entry to the cache. If there's already an
         * entry then for this host then the entry will be
         * replaced.
         */
        public Cache put(String host, InetAddress[] addresses) {
            int policy = getPolicy();
            if (policy == InetAddressCachePolicy.NEVER) {
                return this;
            }

            // purge any expired entries

            if (policy != InetAddressCachePolicy.FOREVER) {

                // As we iterate in insertion order we can
                // terminate when a non-expired entry is found.
                LinkedList<String> expired = new LinkedList<>();
                long now = System.currentTimeMillis();
                for (String key : cache.keySet()) {
                    CacheEntry entry = cache.get(key);

                    if (entry.expiration >= 0 && entry.expiration < now) {
                        expired.add(key);
                    } else {
                        break;
                    }
                }

                for (String key : expired) {
                    cache.remove(key);
                }
            }

            // create new entry and add it to the cache
            // -- as a HashMap replaces existing entries we
            //    don't need to explicitly check if there is
            //    already an entry for this host.
            long expiration;
            if (policy == InetAddressCachePolicy.FOREVER) {
                expiration = -1;
            } else {
                expiration = System.currentTimeMillis() + (policy * 1000);
            }
            CacheEntry entry = new CacheEntry(addresses, expiration);
            cache.put(host, entry);
            return this;
        }

        /**
         * Query the cache for the specific host. If found then
         * return its CacheEntry, or null if not found.
         */
        public CacheEntry get(String host) {
            int policy = getPolicy();
            if (policy == InetAddressCachePolicy.NEVER) {
                return null;
            }
            CacheEntry entry = cache.get(host);

            // check if entry has expired
            if (entry != null && policy != InetAddressCachePolicy.FOREVER) {
                if (entry.expiration >= 0 &&
                    entry.expiration < System.currentTimeMillis()) {
                    cache.remove(host);
                    entry = null;
                }
            }

            return entry;
        }
    }

    /*
     * Initialize cache and insert anyLocalAddress into the
     * unknown array with no expiry.
     */
    private static void cacheInitIfNeeded() {
        assert Thread.holdsLock(addressCache);
        if (addressCacheInit) {
            return;
        }
        unknown_array = new InetAddress[1];
        unknown_array[0] = impl.anyLocalAddress();

        addressCache.put(impl.anyLocalAddress().getHostName(),
                         unknown_array);

        addressCacheInit = true;
    }

    /*
     * Cache the given hostname and addresses.
     */
    private static void cacheAddresses(String hostname,
                                       InetAddress[] addresses,
                                       boolean success) {
        hostname = hostname.toLowerCase();
        synchronized (addressCache) {
            cacheInitIfNeeded();
            if (success) {
                addressCache.put(hostname, addresses);
            } else {
                negativeCache.put(hostname, addresses);
            }
        }
    }

    /*
     * Lookup hostname in cache (positive & negative cache). If
     * found return addresses, null if not found.
     */
    private static InetAddress[] getCachedAddresses(String hostname) {
        hostname = hostname.toLowerCase();

        // search both positive & negative caches

        synchronized (addressCache) {
            cacheInitIfNeeded();

            CacheEntry entry = addressCache.get(hostname);
            if (entry == null) {
                entry = negativeCache.get(hostname);
            }

            if (entry != null) {
                return entry.addresses;
            }
        }

        // not found
        return null;
    }

    private static NameService createNSProvider(String provider) {
        if (provider == null)
            return null;

        NameService nameService = null;
        if (provider.equals("default")) {
            // initialize the default name service
            nameService = new NameService() {
                public InetAddress[] lookupAllHostAddr(String host)
                    throws UnknownHostException {
                    return impl.lookupAllHostAddr(host);
                }
                public String getHostByAddr(byte[] addr)
                    throws UnknownHostException {
                    return impl.getHostByAddr(addr);
                }
            };
        } else {
            final String providerName = provider;
            try {
                nameService = java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedExceptionAction<NameService>() {
                        public NameService run() {
                            Iterator<NameServiceDescriptor> itr =
                                ServiceLoader.load(NameServiceDescriptor.class)
                                    .iterator();
                            while (itr.hasNext()) {
                                NameServiceDescriptor nsd = itr.next();
                                if (providerName.
                                    equalsIgnoreCase(nsd.getType()+","
                                        +nsd.getProviderName())) {
                                    try {
                                        return nsd.createNameService();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        System.err.println(
                                            "Cannot create name service:"
                                             +providerName+": " + e);
                                    }
                                }
                            }

                            return null;
                        }
                    }
                );
            } catch (java.security.PrivilegedActionException e) {
            }
        }

        return nameService;
    }

    static {
        // create the impl
        impl = InetAddressImplFactory.create();

        // get name service if provided and requested
        String provider = null;;
        String propPrefix = "sun.net.spi.nameservice.provider.";
        int n = 1;
        nameServices = new ArrayList<NameService>();
        provider = AccessController.doPrivileged(
                new GetPropertyAction(propPrefix + n));
        while (provider != null) {
            NameService ns = createNSProvider(provider);
            if (ns != null)
                nameServices.add(ns);

            n++;
            provider = AccessController.doPrivileged(
                    new GetPropertyAction(propPrefix + n));
        }

        // if not designate any name services provider,
        // create a default one
        if (nameServices.size() == 0) {
            NameService ns = createNSProvider("default");
            nameServices.add(ns);
        }
    }

    /**
     * Creates an InetAddress based on the provided host name and IP address.
     * No name service is checked for the validity of the address.
     *
     * <p> The host name can either be a machine name, such as
     * "{@code java.sun.com}", or a textual representation of its IP
     * address.
     * <p> No validity checking is done on the host name either.
     *
     * <p> If addr specifies an IPv4 address an instance of Inet4Address
     * will be returned; otherwise, an instance of Inet6Address
     * will be returned.
     *
     * <p> IPv4 address byte array must be 4 bytes long and IPv6 byte array
     * must be 16 bytes long
     *
     * @param host the specified host
     * @param addr the raw IP address in network byte order
     * @return  an InetAddress object created from the raw IP address.
     * @exception  UnknownHostException  if IP address is of illegal length
     * @since 1.4
     */
    public static InetAddress getByAddress(String host, byte[] addr)
        throws UnknownHostException {
        if (host != null && host.length() > 0 && host.charAt(0) == '[') {
            if (host.charAt(host.length()-1) == ']') {
                host = host.substring(1, host.length() -1);
            }
        }
        if (addr != null) {
            if (addr.length == Inet4Address.INADDRSZ) {
                return new Inet4Address(host, addr);
            } else if (addr.length == Inet6Address.INADDRSZ) {
                byte[] newAddr
                    = IPAddressUtil.convertFromIPv4MappedAddress(addr);
                if (newAddr != null) {
                    return new Inet4Address(host, newAddr);
                } else {
                    return new Inet6Address(host, addr);
                }
            }
        }
        throw new UnknownHostException("addr is of illegal length");
    }


    /**
     * Determines the IP address of a host, given the host's name.
     *
     * <p> The host name can either be a machine name, such as
     * "{@code java.sun.com}", or a textual representation of its
     * IP address. If a literal IP address is supplied, only the
     * validity of the address format is checked.
     *
     * <p> For {@code host} specified in literal IPv6 address,
     * either the form defined in RFC 2732 or the literal IPv6 address
     * format defined in RFC 2373 is accepted. IPv6 scoped addresses are also
     * supported. See <a href="Inet6Address.html#scoped">here</a> for a description of IPv6
     * scoped addresses.
     *
     * <p> If the host is {@code null} then an {@code InetAddress}
     * representing an address of the loopback interface is returned.
     * See <a href="http://www.ietf.org/rfc/rfc3330.txt">RFC&nbsp;3330</a>
     * section&nbsp;2 and <a href="http://www.ietf.org/rfc/rfc2373.txt">RFC&nbsp;2373</a>
     * section&nbsp;2.5.3. </p>
     *
     * @param      host   the specified host, or {@code null}.
     * @return     an IP address for the given host name.
     * @exception  UnknownHostException  if no IP address for the
     *               {@code host} could be found, or if a scope_id was specified
     *               for a global IPv6 address.
     * @exception  SecurityException if a security manager exists
     *             and its checkConnect method doesn't allow the operation
     */
    public static InetAddress getByName(String host)
        throws UnknownHostException {
        return InetAddress.getAllByName(host)[0];
    }

    // called from deployment cache manager
    private static InetAddress getByName(String host, InetAddress reqAddr)
        throws UnknownHostException {
        return InetAddress.getAllByName(host, reqAddr)[0];
    }

    /**
     * Given the name of a host, returns an array of its IP addresses,
     * based on the configured name service on the system.
     *
     * <p> The host name can either be a machine name, such as
     * "{@code java.sun.com}", or a textual representation of its IP
     * address. If a literal IP address is supplied, only the
     * validity of the address format is checked.
     *
     * <p> For {@code host} specified in <i>literal IPv6 address</i>,
     * either the form defined in RFC 2732 or the literal IPv6 address
     * format defined in RFC 2373 is accepted. A literal IPv6 address may
     * also be qualified by appending a scoped zone identifier or scope_id.
     * The syntax and usage of scope_ids is described
     * <a href="Inet6Address.html#scoped">here</a>.
     * <p> If the host is {@code null} then an {@code InetAddress}
     * representing an address of the loopback interface is returned.
     * See <a href="http://www.ietf.org/rfc/rfc3330.txt">RFC&nbsp;3330</a>
     * section&nbsp;2 and <a href="http://www.ietf.org/rfc/rfc2373.txt">RFC&nbsp;2373</a>
     * section&nbsp;2.5.3. </p>
     *
     * <p> If there is a security manager and {@code host} is not
     * null and {@code host.length() } is not equal to zero, the
     * security manager's
     * {@code checkConnect} method is called
     * with the hostname and {@code -1}
     * as its arguments to see if the operation is allowed.
     *
     * @param      host   the name of the host, or {@code null}.
     * @return     an array of all the IP addresses for a given host name.
     *
     * @exception  UnknownHostException  if no IP address for the
     *               {@code host} could be found, or if a scope_id was specified
     *               for a global IPv6 address.
     * @exception  SecurityException  if a security manager exists and its
     *               {@code checkConnect} method doesn't allow the operation.
     *
     * @see SecurityManager#checkConnect
     */
    public static InetAddress[] getAllByName(String host)
        throws UnknownHostException {
        return getAllByName(host, null);
    }

    private static InetAddress[] getAllByName(String host, InetAddress reqAddr)
        throws UnknownHostException {

        if (host == null || host.length() == 0) {
            InetAddress[] ret = new InetAddress[1];
            ret[0] = impl.loopbackAddress();
            return ret;
        }

        boolean ipv6Expected = false;
        if (host.charAt(0) == '[') {
            // This is supposed to be an IPv6 literal
            if (host.length() > 2 && host.charAt(host.length()-1) == ']') {
                host = host.substring(1, host.length() -1);
                ipv6Expected = true;
            } else {
                // This was supposed to be a IPv6 address, but it's not!
                throw new UnknownHostException(host + ": invalid IPv6 address");
            }
        }

        // if host is an IP address, we won't do further lookup
        if (Character.digit(host.charAt(0), 16) != -1
            || (host.charAt(0) == ':')) {
            byte[] addr = null;
            int numericZone = -1;
            String ifname = null;
            // see if it is IPv4 address
            addr = IPAddressUtil.textToNumericFormatV4(host);
            if (addr == null) {
                // This is supposed to be an IPv6 literal
                // Check if a numeric or string zone id is present
                int pos;
                if ((pos=host.indexOf ("%")) != -1) {
                    numericZone = checkNumericZone (host);
                    if (numericZone == -1) { /* remainder of string must be an ifname */
                        ifname = host.substring (pos+1);
                    }
                }
                if ((addr = IPAddressUtil.textToNumericFormatV6(host)) == null && host.contains(":")) {
                    throw new UnknownHostException(host + ": invalid IPv6 address");
                }
            } else if (ipv6Expected) {
                // Means an IPv4 litteral between brackets!
                throw new UnknownHostException("["+host+"]");
            }
            InetAddress[] ret = new InetAddress[1];
            if(addr != null) {
                if (addr.length == Inet4Address.INADDRSZ) {
                    ret[0] = new Inet4Address(null, addr);
                } else {
                    if (ifname != null) {
                        ret[0] = new Inet6Address(null, addr, ifname);
                    } else {
                        ret[0] = new Inet6Address(null, addr, numericZone);
                    }
                }
                return ret;
            }
        } else if (ipv6Expected) {
            // We were expecting an IPv6 Litteral, but got something else
            throw new UnknownHostException("["+host+"]");
        }
        return getAllByName0(host, reqAddr, true);
    }

    /**
     * Returns the loopback address.
     * <p>
     * The InetAddress returned will represent the IPv4
     * loopback address, 127.0.0.1, or the IPv6 loopback
     * address, ::1. The IPv4 loopback address returned
     * is only one of many in the form 127.*.*.*
     *
     * @return  the InetAddress loopback instance.
     * @since 1.7
     */
    public static InetAddress getLoopbackAddress() {
        return impl.loopbackAddress();
    }


    /**
     * check if the literal address string has %nn appended
     * returns -1 if not, or the numeric value otherwise.
     *
     * %nn may also be a string that represents the displayName of
     * a currently available NetworkInterface.
     */
    private static int checkNumericZone (String s) throws UnknownHostException {
        int percent = s.indexOf ('%');
        int slen = s.length();
        int digit, zone=0;
        if (percent == -1) {
            return -1;
        }
        for (int i=percent+1; i<slen; i++) {
            char c = s.charAt(i);
            if (c == ']') {
                if (i == percent+1) {
                    /* empty per-cent field */
                    return -1;
                }
                break;
            }
            if ((digit = Character.digit (c, 10)) < 0) {
                return -1;
            }
            zone = (zone * 10) + digit;
        }
        return zone;
    }

    private static InetAddress[] getAllByName0 (String host)
        throws UnknownHostException
    {
        return getAllByName0(host, true);
    }

    /**
     * package private so SocketPermission can call it
     */
    static InetAddress[] getAllByName0 (String host, boolean check)
        throws UnknownHostException  {
        return getAllByName0 (host, null, check);
    }

    private static InetAddress[] getAllByName0 (String host, InetAddress reqAddr, boolean check)
        throws UnknownHostException  {

        /* If it gets here it is presumed to be a hostname */
        /* Cache.get can return: null, unknownAddress, or InetAddress[] */

        /* make sure the connection to the host is allowed, before we
         * give out a hostname
         */
        if (check) {
            SecurityManager security = System.getSecurityManager();
            if (security != null) {
                security.checkConnect(host, -1);
            }
        }

        InetAddress[] addresses = getCachedAddresses(host);

        /* If no entry in cache, then do the host lookup */
        if (addresses == null) {
            addresses = getAddressesFromNameService(host, reqAddr);
        }

        if (addresses == unknown_array)
            throw new UnknownHostException(host);

        return addresses.clone();
    }

    private static InetAddress[] getAddressesFromNameService(String host, InetAddress reqAddr)
        throws UnknownHostException
    {
        InetAddress[] addresses = null;
        boolean success = false;
        UnknownHostException ex = null;

        // Check whether the host is in the lookupTable.
        // 1) If the host isn't in the lookupTable when
        //    checkLookupTable() is called, checkLookupTable()
        //    would add the host in the lookupTable and
        //    return null. So we will do the lookup.
        // 2) If the host is in the lookupTable when
        //    checkLookupTable() is called, the current thread
        //    would be blocked until the host is removed
        //    from the lookupTable. Then this thread
        //    should try to look up the addressCache.
        //     i) if it found the addresses in the
        //        addressCache, checkLookupTable()  would
        //        return the addresses.
        //     ii) if it didn't find the addresses in the
        //         addressCache for any reason,
        //         it should add the host in the
        //         lookupTable and return null so the
        //         following code would do  a lookup itself.
        if ((addresses = checkLookupTable(host)) == null) {
            try {
                // This is the first thread which looks up the addresses
                // this host or the cache entry for this host has been
                // expired so this thread should do the lookup.
                for (NameService nameService : nameServices) {
                    try {
                        /*
                         * Do not put the call to lookup() inside the
                         * constructor.  if you do you will still be
                         * allocating space when the lookup fails.
                         */

                        addresses = nameService.lookupAllHostAddr(host);
                        success = true;
                        break;
                    } catch (UnknownHostException uhe) {
                        if (host.equalsIgnoreCase("localhost")) {
                            InetAddress[] local = new InetAddress[] { impl.loopbackAddress() };
                            addresses = local;
                            success = true;
                            break;
                        }
                        else {
                            addresses = unknown_array;
                            success = false;
                            ex = uhe;
                        }
                    }
                }

                // More to do?
                if (reqAddr != null && addresses.length > 1 && !addresses[0].equals(reqAddr)) {
                    // Find it?
                    int i = 1;
                    for (; i < addresses.length; i++) {
                        if (addresses[i].equals(reqAddr)) {
                            break;
                        }
                    }
                    // Rotate
                    if (i < addresses.length) {
                        InetAddress tmp, tmp2 = reqAddr;
                        for (int j = 0; j < i; j++) {
                            tmp = addresses[j];
                            addresses[j] = tmp2;
                            tmp2 = tmp;
                        }
                        addresses[i] = tmp2;
                    }
                }
                // Cache the address.
                cacheAddresses(host, addresses, success);

                if (!success && ex != null)
                    throw ex;

            } finally {
                // Delete host from the lookupTable and notify
                // all threads waiting on the lookupTable monitor.
                updateLookupTable(host);
            }
        }

        return addresses;
    }


    private static InetAddress[] checkLookupTable(String host) {
        synchronized (lookupTable) {
            // If the host isn't in the lookupTable, add it in the
            // lookuptable and return null. The caller should do
            // the lookup.
            if (lookupTable.containsKey(host) == false) {
                lookupTable.put(host, null);
                return null;
            }

            // If the host is in the lookupTable, it means that another
            // thread is trying to look up the addresses of this host.
            // This thread should wait.
            while (lookupTable.containsKey(host)) {
                try {
                    lookupTable.wait();
                } catch (InterruptedException e) {
                }
            }
        }

        // The other thread has finished looking up the addresses of
        // the host. This thread should retry to get the addresses
        // from the addressCache. If it doesn't get the addresses from
        // the cache, it will try to look up the addresses itself.
        InetAddress[] addresses = getCachedAddresses(host);
        if (addresses == null) {
            synchronized (lookupTable) {
                lookupTable.put(host, null);
                return null;
            }
        }

        return addresses;
    }

    private static void updateLookupTable(String host) {
        synchronized (lookupTable) {
            lookupTable.remove(host);
            lookupTable.notifyAll();
        }
    }

    /**
     * Returns an {@code InetAddress} object given the raw IP address .
     * The argument is in network byte order: the highest order
     * byte of the address is in {@code getAddress()[0]}.
     *
     * <p> This method doesn't block, i.e. no reverse name service lookup
     * is performed.
     *
     * <p> IPv4 address byte array must be 4 bytes long and IPv6 byte array
     * must be 16 bytes long
     *
     * @param addr the raw IP address in network byte order
     * @return  an InetAddress object created from the raw IP address.
     * @exception  UnknownHostException  if IP address is of illegal length
     * @since 1.4
     */
    public static InetAddress getByAddress(byte[] addr)
        throws UnknownHostException {
        return getByAddress(null, addr);
    }

    private static InetAddress cachedLocalHost = null;
    private static long cacheTime = 0;
    private static final long maxCacheTime = 5000L;
    private static final Object cacheLock = new Object();

    /**
     * Returns the address of the local host. This is achieved by retrieving
     * the name of the host from the system, then resolving that name into
     * an {@code InetAddress}.
     *
     * <P>Note: The resolved address may be cached for a short period of time.
     * </P>
     *
     * <p>If there is a security manager, its
     * {@code checkConnect} method is called
     * with the local host name and {@code -1}
     * as its arguments to see if the operation is allowed.
     * If the operation is not allowed, an InetAddress representing
     * the loopback address is returned.
     *
     * @return     the address of the local host.
     *
     * @exception  UnknownHostException  if the local host name could not
     *             be resolved into an address.
     *
     * @see SecurityManager#checkConnect
     * @see java.net.InetAddress#getByName(java.lang.String)
     */
    public static InetAddress getLocalHost() throws UnknownHostException {

        SecurityManager security = System.getSecurityManager();
        try {
            String local = impl.getLocalHostName();

            if (security != null) {
                security.checkConnect(local, -1);
            }

            if (local.equals("localhost")) {
                return impl.loopbackAddress();
            }

            InetAddress ret = null;
            synchronized (cacheLock) {
                long now = System.currentTimeMillis();
                if (cachedLocalHost != null) {
                    if ((now - cacheTime) < maxCacheTime) // Less than 5s old?
                        ret = cachedLocalHost;
                    else
                        cachedLocalHost = null;
                }

                // we are calling getAddressesFromNameService directly
                // to avoid getting localHost from cache
                if (ret == null) {
                    InetAddress[] localAddrs;
                    try {
                        localAddrs =
                            InetAddress.getAddressesFromNameService(local, null);
                    } catch (UnknownHostException uhe) {
                        // Rethrow with a more informative error message.
                        UnknownHostException uhe2 =
                            new UnknownHostException(local + ": " +
                                                     uhe.getMessage());
                        uhe2.initCause(uhe);
                        throw uhe2;
                    }
                    cachedLocalHost = localAddrs[0];
                    cacheTime = now;
                    ret = localAddrs[0];
                }
            }
            return ret;
        } catch (java.lang.SecurityException e) {
            return impl.loopbackAddress();
        }
    }

    /**
     * Perform class load-time initializations.
     */
    private static native void init();


    /*
     * Returns the InetAddress representing anyLocalAddress
     * (typically 0.0.0.0 or ::0)
     */
    static InetAddress anyLocalAddress() {
        return impl.anyLocalAddress();
    }

    /*
     * Load and instantiate an underlying impl class
     */
    static InetAddressImpl loadImpl(String implName) {
        Object impl = null;

        /*
         * Property "impl.prefix" will be prepended to the classname
         * of the implementation object we instantiate, to which we
         * delegate the real work (like native methods).  This
         * property can vary across implementations of the java.
         * classes.  The default is an empty String "".
         */
        String prefix = AccessController.doPrivileged(
                      new GetPropertyAction("impl.prefix", ""));
        try {
            impl = Class.forName("java.net." + prefix + implName).newInstance();
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: java.net." + prefix +
                               implName + ":\ncheck impl.prefix property " +
                               "in your properties file.");
        } catch (InstantiationException e) {
            System.err.println("Could not instantiate: java.net." + prefix +
                               implName + ":\ncheck impl.prefix property " +
                               "in your properties file.");
        } catch (IllegalAccessException e) {
            System.err.println("Cannot access class: java.net." + prefix +
                               implName + ":\ncheck impl.prefix property " +
                               "in your properties file.");
        }

        if (impl == null) {
            try {
                impl = Class.forName(implName).newInstance();
            } catch (Exception e) {
                throw new Error("System property impl.prefix incorrect");
            }
        }

        return (InetAddressImpl) impl;
    }

    private void readObjectNoData (ObjectInputStream s) throws
                         IOException, ClassNotFoundException {
        if (getClass().getClassLoader() != null) {
            throw new SecurityException ("invalid address type");
        }
    }

    private static final long FIELDS_OFFSET;
    private static final sun.misc.Unsafe UNSAFE;

    static {
        try {
            sun.misc.Unsafe unsafe = sun.misc.Unsafe.getUnsafe();
            FIELDS_OFFSET = unsafe.objectFieldOffset(
                InetAddress.class.getDeclaredField("holder")
            );
            UNSAFE = unsafe;
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    private void readObject (ObjectInputStream s) throws
                         IOException, ClassNotFoundException {
        if (getClass().getClassLoader() != null) {
            throw new SecurityException ("invalid address type");
        }
        GetField gf = s.readFields();
        String host = (String)gf.get("hostName", null);
        int address= gf.get("address", 0);
        int family= gf.get("family", 0);
        InetAddressHolder h = new InetAddressHolder(host, address, family);
        UNSAFE.putObject(this, FIELDS_OFFSET, h);
    }

    /* needed because the serializable fields no longer exist */

    /**
     * @serialField hostName String
     * @serialField address int
     * @serialField family int
     */
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("hostName", String.class),
        new ObjectStreamField("address", int.class),
        new ObjectStreamField("family", int.class),
    };

    private void writeObject (ObjectOutputStream s) throws
                        IOException {
        if (getClass().getClassLoader() != null) {
            throw new SecurityException ("invalid address type");
        }
        PutField pf = s.putFields();
        pf.put("hostName", holder().getHostName());
        pf.put("address", holder().getAddress());
        pf.put("family", holder().getFamily());
        s.writeFields();
    }
}

/*
 * Simple factory to create the impl
 */
class InetAddressImplFactory {

    static InetAddressImpl create() {
        return InetAddress.loadImpl(isIPv6Supported() ?
                                    "Inet6AddressImpl" : "Inet4AddressImpl");
    }

    static native boolean isIPv6Supported();
}
