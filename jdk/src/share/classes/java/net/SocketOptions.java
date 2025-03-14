/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.Native;
import java.net.Socket;
import java.net.Socket;

/**
 * 提供设置/获取socket选项方法的接口。该接口被{@link java.net.SocketImpl}和
 * {@link java.net.DatagramSocketImpl}所实现。上述两个的子类应该覆盖该接口的
 * 方法以支持它们自己的选项。
 *
 * <p>该接口中的指定选项的方法和常量仅用于实现类。如果你不想实现上述子类，则无需直接
 * 调用这些方法。在{@link java.net.Socket}、{@link java.net.ServerSocket}、
 * {@link java.net.DatagramSocket}和{@link java.net.MulticastSocket}中，
 * 设置或取消这些选项的方法都是类型安全的。
 *
 * @author David Brown
 */
public interface SocketOptions {

    /**
     * Enable/disable the option specified by <I>optID</I>.  If the option
     * is to be enabled, and it takes an option-specific "value",  this is
     * passed in <I>value</I>.  The actual type of value is option-specific,
     * and it is an error to pass something that isn't of the expected type:
     * <BR><PRE>
     * SocketImpl s;
     * ...
     * s.setOption(SO_LINGER, new Integer(10));
     *    // OK - set SO_LINGER w/ timeout of 10 sec.
     * s.setOption(SO_LINGER, new Double(10));
     *    // ERROR - expects java.lang.Integer
     *</PRE>
     * If the requested option is binary, it can be set using this method by
     * a java.lang.Boolean:
     * <BR><PRE>
     * s.setOption(TCP_NODELAY, new Boolean(true));
     *    // OK - enables TCP_NODELAY, a binary option
     * </PRE>
     * <BR>
     * Any option can be disabled using this method with a Boolean(false):
     * <BR><PRE>
     * s.setOption(TCP_NODELAY, new Boolean(false));
     *    // OK - disables TCP_NODELAY
     * s.setOption(SO_LINGER, new Boolean(false));
     *    // OK - disables SO_LINGER
     * </PRE>
     * <BR>
     * For an option that has a notion of on and off, and requires
     * a non-boolean parameter, setting its value to anything other than
     * <I>Boolean(false)</I> implicitly enables it.
     * <BR>
     * Throws SocketException if the option is unrecognized,
     * the socket is closed, or some low-level error occurred
     * <BR>
     * @param optID identifies the option
     * @param value the parameter of the socket option
     * @throws SocketException if the option is unrecognized,
     * the socket is closed, or some low-level error occurred
     * @see #getOption(int)
     */
    public void
        setOption(int optID, Object value) throws SocketException;

    /**
     * Fetch the value of an option.
     * Binary options will return java.lang.Boolean(true)
     * if enabled, java.lang.Boolean(false) if disabled, e.g.:
     * <BR><PRE>
     * SocketImpl s;
     * ...
     * Boolean noDelay = (Boolean)(s.getOption(TCP_NODELAY));
     * if (noDelay.booleanValue()) {
     *     // true if TCP_NODELAY is enabled...
     * ...
     * }
     * </PRE>
     * <P>
     * For options that take a particular type as a parameter,
     * getOption(int) will return the parameter's value, else
     * it will return java.lang.Boolean(false):
     * <PRE>
     * Object o = s.getOption(SO_LINGER);
     * if (o instanceof Integer) {
     *     System.out.print("Linger time is " + ((Integer)o).intValue());
     * } else {
     *   // the true type of o is java.lang.Boolean(false);
     * }
     * </PRE>
     *
     * @param optID an {@code int} identifying the option to fetch
     * @return the value of the option
     * @throws SocketException if the socket is closed
     * @throws SocketException if <I>optID</I> is unknown along the
     *         protocol stack (including the SocketImpl)
     * @see #setOption(int, java.lang.Object)
     */
    public Object getOption(int optID) throws SocketException;

    /**
     * 由Java支持的BSD风格的选项
     */

    /**
     * 对连接禁用 Nagle 算法。写入网络的数据不会被缓冲，等待先前写入的数据的确认。
     *
     * <p>仅适用于TCP {@link java.net.SocketImpl}
     *
     * @see Socket#setTcpNoDelay
     * @see Socket#getTcpNoDelay
     */

    @Native public final static int TCP_NODELAY = 0x0001;

    /**
     * 获取绑定到socket的本地地址（该选项不能被设置，只能被获取，因为socket是在被
     * 创建时绑定到地址上的，因此本地绑定地址不能被修改）。socket的默认本地地址是
     * INADDR_ANY，表示多宿主机上的任何本地地址。多宿主机可以使用该选项来仅接受
     * 其中一个地址的连接（对于 ServerSocket和DatagramSocket而言），或者向
     * 对等端指定其返回地址（对于socket或DatagramSocket而言）。此选项的参数
     * 是{@link java.net.InetAddress}。
     *
     * <p>该选项必须指定在构造器中。
     *
     * <p>适用于{@link java.net.SocketImpl}和{@link java.net.DatagramSocketImpl}
     *
     * @see Socket#getLocalAddress
     * @see DatagramSocket#getLocalAddress
     */

    @Native public final static int SO_BINDADDR = 0x000F;

    /**
     * 为socket设置 SO_REUSEADDR。仅能在Java中的{@link java.net.MulticastSocket}中使用，
     * 并且它是{@link java.net.MulticastSocket}的默认设置。
     *
     * <p>适用于 {@link java.net.DatagramSocketImpl}
     */
    @Native public final static int SO_REUSEADDR = 0x04;

    /**
     * 为socket设置BOARDCAST，该选项开启或禁用发送广播消息的能力。仅支持datagram socket，
     * 并且仅支持广播消息概念的网络（比如以太网、令牌）等，并且他是{@link java.net.DatagramSocket}
     * 的默认设置。
     */
    @Native public final static int SO_BROADCAST = 0x0020;


    /**
     * 设置发送多播数据库包的传出接口。在具有多个网络的主机上很有用。
     * 应用程序希望使用除系统默认接口之外的其他接口，获取/返回 InetAddress。
     *
     * <p>适用于多播：{@link java.net.DatagramSocketImpl}
     *
     * @see MulticastSocket#setInterface(InetAddress)
     * @see MulticastSocket#getInterface()
     */
    @Native public final static int IP_MULTICAST_IF = 0x10;

    /**
     * 与上一个相同，引用此选项为了使 IP_MULTICAST_IF 的行为保持与以前相同，
     * 同时此新选项可以支持IPv4和IPv6地址设置传出接口。
     *
     * <p>请注意：确保没有与此冲突
     *
     * @see MulticastSocket#setNetworkInterface(NetworkInterface)
     * @see MulticastSocket#getNetworkInterface()
     * @since 1.4
     */
    @Native public final static int IP_MULTICAST_IF2 = 0x1f;

    /**
     * 该选项开启或禁用多播数据包的本地回环。
     *
     * <p>该选项默认被多播socket启用。
     *
     * @since 1.4
     */
    @Native public final static int IP_MULTICAST_LOOP = 0x12;

    /**
     * 该选项设置TCP或UDP socket的IP标头中的服务类型或流量类别字段。
     *
     * @since 1.4
     */
    @Native public final static int IP_TOS = 0x3;

    /**
     * 指定关闭时停留的超时时间。该选项禁用/开启从TCP{@link java.net.Socket#close()}
     * 立即返回。使用非零整数{@code timeout}开启该选项意味着{@link java.net.Socket#close()}
     * 将会阻塞，等待传输和确认所有写入对等端的数据，此时socket将正常关闭。一旦到达超时时间，
     * socket将会被强制关闭，并发出 TCP RST。使用零开启该选项意味着立即强制关闭。
     * 如果特定超时值超过了65535，其会被减少到65535。
     *
     * <p>仅适用于TCP {@link java.net.SocketImpl}
     *
     * @see Socket#setSoLinger
     * @see Socket#getSoLinger
     */
    @Native public final static int SO_LINGER = 0x0080;

    /**
     * 为阻塞socket操作设置超时时间。
     *
     * <pre>{@code
     *  ServerSocket.accept();
     *  SocketInputStream.read();
     *  DatagramSocket.receive();
     * }</pre>
     *
     * <p>必须在进入阻塞操作之前设置该选项才能生效，如果超时且操作继续阻塞，
     * 则返回引发{@link java.io.InterruptedIOException}。在这种情况下，
     * socket不会关闭。
     *
     * <p>适用于所有socket：{@link java.net.SocketImpl}，{@link java.net.DatagramSocketImpl}
     *
     * @see Socket#setSoTimeout
     * @see ServerSocket#setSoTimeout
     * @see DatagramSocket#setSoTimeout
     */
    @Native public final static int SO_TIMEOUT = 0x1006;

    /**
     * 设置平台用于传出网络I/O的底层缓存区大小的提示。在set中使用时，
     * 这是应用程序向内核提出的有关通过socket发送数据时使用的缓冲区
     * 大小的建议。在get中使用时，必须返回平台在此socket上发送数据
     * 时实际使用的缓冲区大小。
     *
     * <p>适用于所有socket：{@link java.net.SocketImpl}，{@link java.net.DatagramSocketImpl}
     *
     * @see Socket#setSendBufferSize
     * @see Socket#getSendBufferSize
     * @see DatagramSocket#setSendBufferSize
     * @see DatagramSocket#getSendBufferSize
     */
    @Native public final static int SO_SNDBUF = 0x1001;

    /**
     * 设置平台用于传入网络I/O的底层缓冲区大小的提示。在set中使用时，
     * 这是应用程序向内核提出的有关通过socket接受数据时使用的缓冲区
     * 大小的建议。在get中使用时，必须返回平台在此sockt上接受数据
     * 时实际使用的缓冲区大小。
     *
     * <p>适用于所有socket：{@link java.net.SocketImpl}，{@link java.net.DatagramSocketImpl}
     *
     * @see Socket#setReceiveBufferSize
     * @see Socket#getReceiveBufferSize
     * @see DatagramSocket#setReceiveBufferSize
     * @see DatagramSocket#getReceiveBufferSize
     */
    @Native public final static int SO_RCVBUF = 0x1002;

    /**
     * 当TCP socket设置了keepalive选项，并且2小时内socket上没有双向交换任何数据时
     * （注意：实际值取决于实现），TCP会自动向对等方发送 keepalive 探测。此探测是一个
     * TCP端，对等方必须对其作出响应。
     *
     * <p>预期的响应有以下三种：
     * <ol>
     *     <li></li>
     * </ol>
     */
    /**
     * When the keepalive option is set for a TCP socket and no data
     * has been exchanged across the socket in either direction for
     * 2 hours (NOTE: the actual value is implementation dependent),
     * TCP automatically sends a keepalive probe to the peer. This probe is a
     * TCP segment to which the peer must respond.
     * One of three responses is expected:
     * 1. The peer responds with the expected ACK. The application is not
     *    notified (since everything is OK). TCP will send another probe
     *    following another 2 hours of inactivity.
     * 2. The peer responds with an RST, which tells the local TCP that
     *    the peer host has crashed and rebooted. The socket is closed.
     * 3. There is no response from the peer. The socket is closed.
     *
     * The purpose of this option is to detect if the peer host crashes.
     *
     * Valid only for TCP socket: SocketImpl
     *
     * @see Socket#setKeepAlive
     * @see Socket#getKeepAlive
     */
    @Native public final static int SO_KEEPALIVE = 0x0008;

    /**
     * 当设置了 OOBINLINE 选项，socket上接收的任何TCP紧急数据都将通过
     * socket输入流接收。
     * 当禁用该选项（默认）紧急数据会被静默忽略。
     *
     * @see Socket#setOOBInline
     * @see Socket#getOOBInline
     */
    @Native public final static int SO_OOBINLINE = 0x1003;
}
