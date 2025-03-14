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

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.SocketImplFactory;
import java.nio.channels.ServerSocketChannel;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

/**
 * 该类实现了服务端的sockets。服务端socket等待通过网络传入的请求。
 * 它会基于该请求执行某些操作，然后可能返回一个结果给发送请求的客户端。
 *
 * <p>服务端socket的实际工作由{@link java.net.SocketImpl}的实例
 * 负责执行。一个应用可以修改创建socket实现的的socket工厂，以配置自身
 * 来创建适应本地防火墙的socket。
 *
 * @author  unascribed
 * @see     java.net.SocketImpl
 * @see     java.net.ServerSocket#setSocketFactory(java.net.SocketImplFactory)
 * @see     java.nio.channels.ServerSocketChannel
 * @since   JDK1.0
 */
public
class ServerSocket implements java.io.Closeable {
    //-----------------------------------------
    // 该socket的各种状态
    //-----------------------------------------

    /**
     * 是否已创建
     */
    private boolean created = false;
    /**
     * 是否已绑定
     */
    private boolean bound = false;
    /**
     * 是否已关闭
     */
    private boolean closed = false;
    /**
     * 关闭锁
     */
    private Object closeLock = new Object();

    /**
     * Socket的实现
     */
    private SocketImpl impl;

    /**
     * 是否使用了较旧的SocketImpl
     */
    private boolean oldImpl = false;

    /**
     * 用于创建关联给定SocketImpl的ServerSocket的包私有构造器
     */
    ServerSocket(SocketImpl impl) {
        this.impl = impl;
        // 引用该ServerSocket
        impl.setServerSocket(this);
    }

    /**
     * 创建一个未绑定的server socket
     *
     * @exception IOException 当打开socket是出现IO错误
     * @revised 1.4
     */
    public ServerSocket() throws IOException {
        setImpl();
    }

    /**
     * 创建一个server socket，绑定到指定端口。端口号为{@code 0}
     * 意味着自动分配端口号，通常来自临时端口范围。之后可以通过
     * {@link #getLocalPort()}来获取端口号。
     *
     * <p>传入连接指示（连接请求）的最大队列长度设置为{@code 50}。
     * 如果在队列已满时收到连接指示，则拒绝连接。
     *
     * <p>如果应用指定server socket工厂，则调用工厂的{@link java.net.SocketImplFactory#createSocketImpl()}
     * 来创建Socket实现，否则将创建一个plain socket。
     *
     * <p>如果有安全管理器，则使用{@link port}参数作为其参数调用
     * 其{@link java.lang.SecurityManager#checkListen(int)}方法，
     * 以确保允许该操作，这可能会导致{@link java.net.SocketException}。
     *
     * @param      port  端口号, 或{@code 0}代表使用自动分配的端口号
     *
     * @exception  IOException  如果打开socket时发生I/O异常
     * @exception  SecurityException 如果一个安全管理器存在，且{@link java.lang.SecurityManager#checkListen(int)}
     * 方法不允许该操作
     * @exception  IllegalArgumentException 如果端口号超出了指定的有效端口值范围[0, 65535]
     *
     * @see        java.net.SocketImpl
     * @see        java.net.SocketImplFactory#createSocketImpl()
     * @see        java.net.ServerSocket#setSocketFactory(java.net.SocketImplFactory)
     * @see        SecurityManager#checkListen
     */
    public ServerSocket(int port) throws IOException {
        this(port, 50, null);
    }

    /**
     * 创建一个server socket，绑定到指定端口号，并指定backlog。
     * 端口号为{@code 0}意味着自动分配端口号，通常来自临时端口范围。
     * 之后可以通过 {@link #getLocalPort()}来获取端口号。
     *
     * <p>传入连接指示（连接请求）的最大队列长度设置为{@code backlog}。
     * 如果在队列已满时收到连接指示，则拒绝连接。
     *
     * <p>如果应用指定server socket工厂，则调用工厂的{@link java.net.SocketImplFactory#createSocketImpl()}
     * 来创建Socket实现，否则将创建一个plain socket。
     *
     * <p>如果有安全管理器，则使用{@link port}参数作为其参数调用
     * 其{@link java.lang.SecurityManager#checkListen(int)}方法，
     * 以确保允许该操作，这可能会导致{@link java.net.SocketException}。
     *
     * <p>{@code backlog}参数是socket上请求的最大待处理连接数。
     * 其确切语义与具体实现相关。具体来说，实现可能会施加最大长度，
     * 或者可以选择完全忽略该参数。提供的值应该大于{@code 0}。
     * 如果小于等于{@code 0}，则将使用特定于实现的默认值。
     *
     * @param      port     端口号, 或{@code 0}代表使用自动分配的端口号
     * @param      backlog  请求的传入连接队列的最大长度
     *
     * @exception  IOException  如果打开socket时发生I/O错误
     * @exception  SecurityException 如果一个安全管理器存在，且{@link java.lang.SecurityManager#checkListen(int)}
     * 方法不允许该操作
     * @exception  IllegalArgumentException 如果端口号超出了指定的有效端口值范围[0, 65535]
     *
     * @see        java.net.SocketImpl
     * @see        java.net.SocketImplFactory#createSocketImpl()
     * @see        java.net.ServerSocket#setSocketFactory(java.net.SocketImplFactory)
     * @see        SecurityManager#checkListen
     */
    public ServerSocket(int port, int backlog) throws IOException {
        this(port, backlog, null);
    }

    /**
     * 创建一个server socket，绑定到指定端口，指定backlog，并绑定到本地IP地址。
     * {@code bindAddr}参数可用于多宿主主机上的Server Socket，该主机仅接受
     * 对某个地址的连接请求。端口号应该在[0, 65535]。端口号为{@code 0}意味着
     * 自动分配端口号，通常来自临时端口范围。之后可以通过 {@link #getLocalPort()}
     * 来获取端口号。
     *
     * <p>如果有安全管理器，则使用{@link port}参数作为其参数调用
     * 其{@link java.lang.SecurityManager#checkListen(int)}方法，
     * 以确保允许该操作，这可能会导致{@link java.net.SocketException}。
     *
     * <p>{@code backlog}参数是socket上请求的最大待处理连接数。
     * 其确切语义与具体实现相关。具体来说，实现可能会施加最大长度，
     * 或者可以选择完全忽略该参数。提供的值应该大于{@code 0}。
     * 如果小于等于{@code 0}，则将使用特定于实现的默认值。
     *
     * @param port  端口号, 或{@code 0}代表使用自动分配的端口号
     * @param backlog 请求的传入连接队列的最大长度
     * @param bindAddr 该服务将绑定的本地ip地址
     *
     * @throws  SecurityException 如果一个安全管理器存在，且{@link java.lang.SecurityManager#checkListen(int)}
     *
     * @throws  IOException 如果打开socket时发生I/O错误
     * @exception  IllegalArgumentException 如果端口号超出了指定的有效端口值范围[0, 65535]
     *
     * @see SocketOptions
     * @see SocketImpl
     * @see SecurityManager#checkListen
     * @since   JDK1.1
     */
    public ServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
        // 设置为系统默认类型的SocketImpl
        setImpl();
        // 检查端口是否在[0, 65535]，否则抛出异常
        if (port < 0 || port > 0xFFFF)
            throw new IllegalArgumentException(
                       "Port value out of range: " + port);
        // 默认为50
        if (backlog < 1)
          backlog = 50;
        try {
            // 绑定到本地ip和端口，并设置backlog
            bind(new InetSocketAddress(bindAddr, port), backlog);
        } catch(SecurityException e) {
            // 安全管理器存在，且{@code checkListen}报错
            close();
            throw e;
        } catch(IOException e) {
            // 打开socket报错
            close();
            throw e;
        }
    }

    /**
     * Get the {@code SocketImpl} attached to this socket, creating
     * it if necessary.
     *
     * @return  the {@code SocketImpl} attached to that ServerSocket.
     * @throws SocketException if creation fails.
     * @since 1.4
     */
    SocketImpl getImpl() throws SocketException {
        if (!created)
            createImpl();
        return impl;
    }

    private void checkOldImpl() {
        if (impl == null)
            return;
        // SocketImpl.connect() is a protected method, therefore we need to use
        // getDeclaredMethod, therefore we need permission to access the member
        try {
            AccessController.doPrivileged(
                new PrivilegedExceptionAction<Void>() {
                    public Void run() throws NoSuchMethodException {
                        impl.getClass().getDeclaredMethod("connect",
                                                          SocketAddress.class,
                                                          int.class);
                        return null;
                    }
                });
        } catch (java.security.PrivilegedActionException e) {
            oldImpl = true;
        }
    }

    private void setImpl() {
        if (factory != null) {
            // 存在SocketImplFactory，则使用工厂来创建SocketImpl实例
            impl = factory.createSocketImpl();
            checkOldImpl();
        } else {
            // 无需在此处调用checkOldImpl()，我们知道这是最新的SocketImpl
            impl = new SocksSocketImpl();
        }
        if (impl != null)
            // 创建成功后，引用该ServerSocket
            impl.setServerSocket(this);
    }

    /**
     * 创建socket实现
     *
     * @throws IOException 如果创建失败
     * @since 1.4
     */
    void createImpl() throws SocketException {
        if (impl == null)
            // 使用系统默认类型的SocketImpl
            setImpl();
        try {
            // 更新为已创建
            impl.create(true);
            // 同步更新已创建
            created = true;
        } catch (IOException e) {
            // 创建失败，抛出socket异常
            throw new SocketException(e.getMessage());
        }
    }

    /**
     *
     * Binds the {@code ServerSocket} to a specific address
     * (IP address and port number).
     * <p>
     * If the address is {@code null}, then the system will pick up
     * an ephemeral port and a valid local address to bind the socket.
     * <p>
     * @param   endpoint        The IP address and port number to bind to.
     * @throws  IOException if the bind operation fails, or if the socket
     *                     is already bound.
     * @throws  SecurityException       if a {@code SecurityManager} is present and
     * its {@code checkListen} method doesn't allow the operation.
     * @throws  IllegalArgumentException if endpoint is a
     *          SocketAddress subclass not supported by this socket
     * @since 1.4
     */
    public void bind(SocketAddress endpoint) throws IOException {
        bind(endpoint, 50);
    }

    /**
     *
     * Binds the {@code ServerSocket} to a specific address
     * (IP address and port number).
     * <p>
     * If the address is {@code null}, then the system will pick up
     * an ephemeral port and a valid local address to bind the socket.
     * <P>
     * The {@code backlog} argument is the requested maximum number of
     * pending connections on the socket. Its exact semantics are implementation
     * specific. In particular, an implementation may impose a maximum length
     * or may choose to ignore the parameter altogther. The value provided
     * should be greater than {@code 0}. If it is less than or equal to
     * {@code 0}, then an implementation specific default will be used.
     * @param   endpoint        The IP address and port number to bind to.
     * @param   backlog         requested maximum length of the queue of
     *                          incoming connections.
     * @throws  IOException if the bind operation fails, or if the socket
     *                     is already bound.
     * @throws  SecurityException       if a {@code SecurityManager} is present and
     * its {@code checkListen} method doesn't allow the operation.
     * @throws  IllegalArgumentException if endpoint is a
     *          SocketAddress subclass not supported by this socket
     * @since 1.4
     */
    public void bind(SocketAddress endpoint, int backlog) throws IOException {
        if (isClosed())
            throw new SocketException("Socket is closed");
        if (!oldImpl && isBound())
            throw new SocketException("Already bound");
        if (endpoint == null)
            endpoint = new InetSocketAddress(0);
        if (!(endpoint instanceof InetSocketAddress))
            throw new IllegalArgumentException("Unsupported address type");
        InetSocketAddress epoint = (InetSocketAddress) endpoint;
        if (epoint.isUnresolved())
            throw new SocketException("Unresolved address");
        if (backlog < 1)
          backlog = 50;
        try {
            SecurityManager security = System.getSecurityManager();
            if (security != null)
                security.checkListen(epoint.getPort());
            getImpl().bind(epoint.getAddress(), epoint.getPort());
            getImpl().listen(backlog);
            bound = true;
        } catch(SecurityException e) {
            bound = false;
            throw e;
        } catch(IOException e) {
            bound = false;
            throw e;
        }
    }

    /**
     * Returns the local address of this server socket.
     * <p>
     * If the socket was bound prior to being {@link #close closed},
     * then this method will continue to return the local address
     * after the socket is closed.
     * <p>
     * If there is a security manager set, its {@code checkConnect} method is
     * called with the local address and {@code -1} as its arguments to see
     * if the operation is allowed. If the operation is not allowed,
     * the {@link InetAddress#getLoopbackAddress loopback} address is returned.
     *
     * @return  the address to which this socket is bound,
     *          or the loopback address if denied by the security manager,
     *          or {@code null} if the socket is unbound.
     *
     * @see SecurityManager#checkConnect
     */
    public InetAddress getInetAddress() {
        if (!isBound())
            return null;
        try {
            InetAddress in = getImpl().getInetAddress();
            SecurityManager sm = System.getSecurityManager();
            if (sm != null)
                sm.checkConnect(in.getHostAddress(), -1);
            return in;
        } catch (SecurityException e) {
            return InetAddress.getLoopbackAddress();
        } catch (SocketException e) {
            // nothing
            // If we're bound, the impl has been created
            // so we shouldn't get here
        }
        return null;
    }

    /**
     * Returns the port number on which this socket is listening.
     * <p>
     * If the socket was bound prior to being {@link #close closed},
     * then this method will continue to return the port number
     * after the socket is closed.
     *
     * @return  the port number to which this socket is listening or
     *          -1 if the socket is not bound yet.
     */
    public int getLocalPort() {
        if (!isBound())
            return -1;
        try {
            return getImpl().getLocalPort();
        } catch (SocketException e) {
            // nothing
            // If we're bound, the impl has been created
            // so we shouldn't get here
        }
        return -1;
    }

    /**
     * Returns the address of the endpoint this socket is bound to.
     * <p>
     * If the socket was bound prior to being {@link #close closed},
     * then this method will continue to return the address of the endpoint
     * after the socket is closed.
     * <p>
     * If there is a security manager set, its {@code checkConnect} method is
     * called with the local address and {@code -1} as its arguments to see
     * if the operation is allowed. If the operation is not allowed,
     * a {@code SocketAddress} representing the
     * {@link InetAddress#getLoopbackAddress loopback} address and the local
     * port to which the socket is bound is returned.
     *
     * @return a {@code SocketAddress} representing the local endpoint of
     *         this socket, or a {@code SocketAddress} representing the
     *         loopback address if denied by the security manager,
     *         or {@code null} if the socket is not bound yet.
     *
     * @see #getInetAddress()
     * @see #getLocalPort()
     * @see #bind(SocketAddress)
     * @see SecurityManager#checkConnect
     * @since 1.4
     */

    public SocketAddress getLocalSocketAddress() {
        if (!isBound())
            return null;
        return new InetSocketAddress(getInetAddress(), getLocalPort());
    }

    /**
     * Listens for a connection to be made to this socket and accepts
     * it. The method blocks until a connection is made.
     *
     * <p>A new Socket {@code s} is created and, if there
     * is a security manager,
     * the security manager's {@code checkAccept} method is called
     * with {@code s.getInetAddress().getHostAddress()} and
     * {@code s.getPort()}
     * as its arguments to ensure the operation is allowed.
     * This could result in a SecurityException.
     *
     * @exception  IOException  if an I/O error occurs when waiting for a
     *               connection.
     * @exception  SecurityException  if a security manager exists and its
     *             {@code checkAccept} method doesn't allow the operation.
     * @exception  SocketTimeoutException if a timeout was previously set with setSoTimeout and
     *             the timeout has been reached.
     * @exception  java.nio.channels.IllegalBlockingModeException
     *             if this socket has an associated channel, the channel is in
     *             non-blocking mode, and there is no connection ready to be
     *             accepted
     *
     * @return the new Socket
     * @see SecurityManager#checkAccept
     * @revised 1.4
     * @spec JSR-51
     */
    public Socket accept() throws IOException {
        if (isClosed())
            throw new SocketException("Socket is closed");
        if (!isBound())
            throw new SocketException("Socket is not bound yet");
        Socket s = new Socket((SocketImpl) null);
        implAccept(s);
        return s;
    }

    /**
     * Subclasses of ServerSocket use this method to override accept()
     * to return their own subclass of socket.  So a FooServerSocket
     * will typically hand this method an <i>empty</i> FooSocket.  On
     * return from implAccept the FooSocket will be connected to a client.
     *
     * @param s the Socket
     * @throws java.nio.channels.IllegalBlockingModeException
     *         if this socket has an associated channel,
     *         and the channel is in non-blocking mode
     * @throws IOException if an I/O error occurs when waiting
     * for a connection.
     * @since   JDK1.1
     * @revised 1.4
     * @spec JSR-51
     */
    protected final void implAccept(Socket s) throws IOException {
        SocketImpl si = null;
        try {
            if (s.impl == null)
              s.setImpl();
            else {
                s.impl.reset();
            }
            si = s.impl;
            s.impl = null;
            si.address = new InetAddress();
            si.fd = new FileDescriptor();
            getImpl().accept(si);

            SecurityManager security = System.getSecurityManager();
            if (security != null) {
                security.checkAccept(si.getInetAddress().getHostAddress(),
                                     si.getPort());
            }
        } catch (IOException e) {
            if (si != null)
                si.reset();
            s.impl = si;
            throw e;
        } catch (SecurityException e) {
            if (si != null)
                si.reset();
            s.impl = si;
            throw e;
        }
        s.impl = si;
        s.postAccept();
    }

    /**
     * Closes this socket.
     *
     * Any thread currently blocked in {@link #accept()} will throw
     * a {@link SocketException}.
     *
     * <p> If this socket has an associated channel then the channel is closed
     * as well.
     *
     * @exception  IOException  if an I/O error occurs when closing the socket.
     * @revised 1.4
     * @spec JSR-51
     */
    public void close() throws IOException {
        synchronized(closeLock) {
            if (isClosed())
                return;
            if (created)
                impl.close();
            closed = true;
        }
    }

    /**
     * Returns the unique {@link java.nio.channels.ServerSocketChannel} object
     * associated with this socket, if any.
     *
     * <p> A server socket will have a channel if, and only if, the channel
     * itself was created via the {@link
     * java.nio.channels.ServerSocketChannel#open ServerSocketChannel.open}
     * method.
     *
     * @return  the server-socket channel associated with this socket,
     *          or {@code null} if this socket was not created
     *          for a channel
     *
     * @since 1.4
     * @spec JSR-51
     */
    public ServerSocketChannel getChannel() {
        return null;
    }

    /**
     * Returns the binding state of the ServerSocket.
     *
     * @return true if the ServerSocket successfully bound to an address
     * @since 1.4
     */
    public boolean isBound() {
        // Before 1.3 ServerSockets were always bound during creation
        return bound || oldImpl;
    }

    /**
     * Returns the closed state of the ServerSocket.
     *
     * @return true if the socket has been closed
     * @since 1.4
     */
    public boolean isClosed() {
        synchronized(closeLock) {
            return closed;
        }
    }

    /**
     * Enable/disable {@link SocketOptions#SO_TIMEOUT SO_TIMEOUT} with the
     * specified timeout, in milliseconds.  With this option set to a non-zero
     * timeout, a call to accept() for this ServerSocket
     * will block for only this amount of time.  If the timeout expires,
     * a <B>java.net.SocketTimeoutException</B> is raised, though the
     * ServerSocket is still valid.  The option <B>must</B> be enabled
     * prior to entering the blocking operation to have effect.  The
     * timeout must be {@code > 0}.
     * A timeout of zero is interpreted as an infinite timeout.
     * @param timeout the specified timeout, in milliseconds
     * @exception SocketException if there is an error in
     * the underlying protocol, such as a TCP error.
     * @since   JDK1.1
     * @see #getSoTimeout()
     */
    public synchronized void setSoTimeout(int timeout) throws SocketException {
        if (isClosed())
            throw new SocketException("Socket is closed");
        getImpl().setOption(SocketOptions.SO_TIMEOUT, new Integer(timeout));
    }

    /**
     * Retrieve setting for {@link SocketOptions#SO_TIMEOUT SO_TIMEOUT}.
     * 0 returns implies that the option is disabled (i.e., timeout of infinity).
     * @return the {@link SocketOptions#SO_TIMEOUT SO_TIMEOUT} value
     * @exception IOException if an I/O error occurs
     * @since   JDK1.1
     * @see #setSoTimeout(int)
     */
    public synchronized int getSoTimeout() throws IOException {
        if (isClosed())
            throw new SocketException("Socket is closed");
        Object o = getImpl().getOption(SocketOptions.SO_TIMEOUT);
        /* extra type safety */
        if (o instanceof Integer) {
            return ((Integer) o).intValue();
        } else {
            return 0;
        }
    }

    /**
     * Enable/disable the {@link SocketOptions#SO_REUSEADDR SO_REUSEADDR}
     * socket option.
     * <p>
     * When a TCP connection is closed the connection may remain
     * in a timeout state for a period of time after the connection
     * is closed (typically known as the {@code TIME_WAIT} state
     * or {@code 2MSL} wait state).
     * For applications using a well known socket address or port
     * it may not be possible to bind a socket to the required
     * {@code SocketAddress} if there is a connection in the
     * timeout state involving the socket address or port.
     * <p>
     * Enabling {@link SocketOptions#SO_REUSEADDR SO_REUSEADDR} prior to
     * binding the socket using {@link #bind(SocketAddress)} allows the socket
     * to be bound even though a previous connection is in a timeout state.
     * <p>
     * When a {@code ServerSocket} is created the initial setting
     * of {@link SocketOptions#SO_REUSEADDR SO_REUSEADDR} is not defined.
     * Applications can use {@link #getReuseAddress()} to determine the initial
     * setting of {@link SocketOptions#SO_REUSEADDR SO_REUSEADDR}.
     * <p>
     * The behaviour when {@link SocketOptions#SO_REUSEADDR SO_REUSEADDR} is
     * enabled or disabled after a socket is bound (See {@link #isBound()})
     * is not defined.
     *
     * @param on  whether to enable or disable the socket option
     * @exception SocketException if an error occurs enabling or
     *            disabling the {@link SocketOptions#SO_REUSEADDR SO_REUSEADDR}
     *            socket option, or the socket is closed.
     * @since 1.4
     * @see #getReuseAddress()
     * @see #bind(SocketAddress)
     * @see #isBound()
     * @see #isClosed()
     */
    public void setReuseAddress(boolean on) throws SocketException {
        if (isClosed())
            throw new SocketException("Socket is closed");
        getImpl().setOption(SocketOptions.SO_REUSEADDR, Boolean.valueOf(on));
    }

    /**
     * Tests if {@link SocketOptions#SO_REUSEADDR SO_REUSEADDR} is enabled.
     *
     * @return a {@code boolean} indicating whether or not
     *         {@link SocketOptions#SO_REUSEADDR SO_REUSEADDR} is enabled.
     * @exception SocketException if there is an error
     * in the underlying protocol, such as a TCP error.
     * @since   1.4
     * @see #setReuseAddress(boolean)
     */
    public boolean getReuseAddress() throws SocketException {
        if (isClosed())
            throw new SocketException("Socket is closed");
        return ((Boolean) (getImpl().getOption(SocketOptions.SO_REUSEADDR))).booleanValue();
    }

    /**
     * Returns the implementation address and implementation port of
     * this socket as a {@code String}.
     * <p>
     * If there is a security manager set, its {@code checkConnect} method is
     * called with the local address and {@code -1} as its arguments to see
     * if the operation is allowed. If the operation is not allowed,
     * an {@code InetAddress} representing the
     * {@link InetAddress#getLoopbackAddress loopback} address is returned as
     * the implementation address.
     *
     * @return  a string representation of this socket.
     */
    public String toString() {
        if (!isBound())
            return "ServerSocket[unbound]";
        InetAddress in;
        if (System.getSecurityManager() != null)
            in = InetAddress.getLoopbackAddress();
        else
            in = impl.getInetAddress();
        return "ServerSocket[addr=" + in +
                ",localport=" + impl.getLocalPort()  + "]";
    }

    void setBound() {
        bound = true;
    }

    void setCreated() {
        created = true;
    }

    /**
     * 用于所有服务端sockets的工厂
     */
    private static SocketImplFactory factory = null;

    /**
     * Sets the server socket implementation factory for the
     * application. The factory can be specified only once.
     * <p>
     * When an application creates a new server socket, the socket
     * implementation factory's {@code createSocketImpl} method is
     * called to create the actual socket implementation.
     * <p>
     * Passing {@code null} to the method is a no-op unless the factory
     * was already set.
     * <p>
     * If there is a security manager, this method first calls
     * the security manager's {@code checkSetFactory} method
     * to ensure the operation is allowed.
     * This could result in a SecurityException.
     *
     * @param      fac   the desired factory.
     * @exception  IOException  if an I/O error occurs when setting the
     *               socket factory.
     * @exception  SocketException  if the factory has already been defined.
     * @exception  SecurityException  if a security manager exists and its
     *             {@code checkSetFactory} method doesn't allow the operation.
     * @see        java.net.SocketImplFactory#createSocketImpl()
     * @see        SecurityManager#checkSetFactory
     */
    public static synchronized void setSocketFactory(SocketImplFactory fac) throws IOException {
        if (factory != null) {
            throw new SocketException("factory already defined");
        }
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkSetFactory();
        }
        factory = fac;
    }

    /**
     * Sets a default proposed value for the
     * {@link SocketOptions#SO_RCVBUF SO_RCVBUF} option for sockets
     * accepted from this {@code ServerSocket}. The value actually set
     * in the accepted socket must be determined by calling
     * {@link Socket#getReceiveBufferSize()} after the socket
     * is returned by {@link #accept()}.
     * <p>
     * The value of {@link SocketOptions#SO_RCVBUF SO_RCVBUF} is used both to
     * set the size of the internal socket receive buffer, and to set the size
     * of the TCP receive window that is advertized to the remote peer.
     * <p>
     * It is possible to change the value subsequently, by calling
     * {@link Socket#setReceiveBufferSize(int)}. However, if the application
     * wishes to allow a receive window larger than 64K bytes, as defined by RFC1323
     * then the proposed value must be set in the ServerSocket <B>before</B>
     * it is bound to a local address. This implies, that the ServerSocket must be
     * created with the no-argument constructor, then setReceiveBufferSize() must
     * be called and lastly the ServerSocket is bound to an address by calling bind().
     * <p>
     * Failure to do this will not cause an error, and the buffer size may be set to the
     * requested value but the TCP receive window in sockets accepted from
     * this ServerSocket will be no larger than 64K bytes.
     *
     * @exception SocketException if there is an error
     * in the underlying protocol, such as a TCP error.
     *
     * @param size the size to which to set the receive buffer
     * size. This value must be greater than 0.
     *
     * @exception IllegalArgumentException if the
     * value is 0 or is negative.
     *
     * @since 1.4
     * @see #getReceiveBufferSize
     */
     public synchronized void setReceiveBufferSize (int size) throws SocketException {
        if (!(size > 0)) {
            throw new IllegalArgumentException("negative receive size");
        }
        if (isClosed())
            throw new SocketException("Socket is closed");
        getImpl().setOption(SocketOptions.SO_RCVBUF, new Integer(size));
    }

    /**
     * Gets the value of the {@link SocketOptions#SO_RCVBUF SO_RCVBUF} option
     * for this {@code ServerSocket}, that is the proposed buffer size that
     * will be used for Sockets accepted from this {@code ServerSocket}.
     *
     * <p>Note, the value actually set in the accepted socket is determined by
     * calling {@link Socket#getReceiveBufferSize()}.
     * @return the value of the {@link SocketOptions#SO_RCVBUF SO_RCVBUF}
     *         option for this {@code Socket}.
     * @exception SocketException if there is an error
     *            in the underlying protocol, such as a TCP error.
     * @see #setReceiveBufferSize(int)
     * @since 1.4
     */
    public synchronized int getReceiveBufferSize()
    throws SocketException{
        if (isClosed())
            throw new SocketException("Socket is closed");
        int result = 0;
        Object o = getImpl().getOption(SocketOptions.SO_RCVBUF);
        if (o instanceof Integer) {
            result = ((Integer)o).intValue();
        }
        return result;
    }

    /**
     * Sets performance preferences for this ServerSocket.
     *
     * <p> Sockets use the TCP/IP protocol by default.  Some implementations
     * may offer alternative protocols which have different performance
     * characteristics than TCP/IP.  This method allows the application to
     * express its own preferences as to how these tradeoffs should be made
     * when the implementation chooses from the available protocols.
     *
     * <p> Performance preferences are described by three integers
     * whose values indicate the relative importance of short connection time,
     * low latency, and high bandwidth.  The absolute values of the integers
     * are irrelevant; in order to choose a protocol the values are simply
     * compared, with larger values indicating stronger preferences.  If the
     * application prefers short connection time over both low latency and high
     * bandwidth, for example, then it could invoke this method with the values
     * {@code (1, 0, 0)}.  If the application prefers high bandwidth above low
     * latency, and low latency above short connection time, then it could
     * invoke this method with the values {@code (0, 1, 2)}.
     *
     * <p> Invoking this method after this socket has been bound
     * will have no effect. This implies that in order to use this capability
     * requires the socket to be created with the no-argument constructor.
     *
     * @param  connectionTime
     *         An {@code int} expressing the relative importance of a short
     *         connection time
     *
     * @param  latency
     *         An {@code int} expressing the relative importance of low
     *         latency
     *
     * @param  bandwidth
     *         An {@code int} expressing the relative importance of high
     *         bandwidth
     *
     * @since 1.5
     */
    public void setPerformancePreferences(int connectionTime,
                                          int latency,
                                          int bandwidth)
    {
        /* Not implemented yet */
    }

}
