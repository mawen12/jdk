/*
 * Copyright (c) 1995, 2012, Oracle and/or its affiliates. All rights reserved.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileDescriptor;

import sun.net.ConnectionResetException;
import sun.net.NetHooks;
import sun.net.ResourceManager;

/**
 * 默认的socket实现，该实现不会实现任何安全检查。
 *
 * <p>请注意，该类不应该为public。
 *
 * @author  Steven B. Byrne
 */
abstract class AbstractPlainSocketImpl extends SocketImpl
{
    /**
     * 用于 SO_TIMEOUT 的实例变量，毫秒单位
     */
    int timeout;
    /**
     * 流量类别
     */
    private int trafficClass;

    private boolean shut_rd = false;
    private boolean shut_wr = false;

    /**
     * socket输入流
     */
    private SocketInputStream socketInputStream = null;
    /**
     * socket输出流
     */
    private SocketOutputStream socketOutputStream = null;

    /**
     * 使用{@link FileDescriptor}的线程总数
     */
    protected int fdUseCount = 0;

    /**
     * 增加或减少{@link #fdUseCount}时锁定
     */
    protected final Object fdLock = new Object();

    /**
     * 表示文件描述符上有待关闭
     */
    protected boolean closePending = false;

    /**
     * 表示连接重置状态
     */
    private int CONNECTION_NOT_RESET = 0;
    private int CONNECTION_RESET_PENDING = 1;
    private int CONNECTION_RESET = 2;
    private int resetState;
    private final Object resetLock = new Object();

    /**
     * socket是否是stream(TCP)或非stream(UDP)
     */
    protected boolean stream;

    /**
     * 将net库加载到运行时
     */
    static {
        java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction<Void>() {
                public Void run() {
                    System.loadLibrary("net");
                    return null;
                }
            });
    }

    /**
     * 创建一个具有布尔值的socket，该布尔值指定这是一个stream socket或
     * 未连接的UDP socket
     */
    protected synchronized void create(boolean stream) throws IOException {
        this.stream = stream;
        if (!stream) {// 创建 UDP socket
            ResourceManager.beforeUdpCreate();
            // 只有在我们直到能够创建socket后才能创建 fd
            fd = new FileDescriptor();
            try {
                // 创建客户端socket
                socketCreate(false);
            } catch (IOException ioe) {
                ResourceManager.afterUdpClose();
                fd = null;
                throw ioe;
            }
        } else { // 创建 TCP socket
            fd = new FileDescriptor();
            // 创建服务端 socket
            socketCreate(true);
        }
        if (socket != null)
            // 更新客户端socket状态为已创建
            socket.setCreated();
        if (serverSocket != null)
            // 更新服务端socket状态为已创建
            serverSocket.setCreated();
    }

    /**
     * 创建一个socket并将它连接到指定主机的指定端口上。
     *
     * @param host 指定主机
     * @param port 指定端口
     */
    protected void connect(String host, int port) throws UnknownHostException, IOException {
        boolean connected = false;
        try {
            // 解析主机名，获得IP地址
            InetAddress address = InetAddress.getByName(host);
            this.port = port;
            this.address = address;

            // 连接到指定IP地址，并设置超时时间
            connectToAddress(address, port, timeout);
            // 更新为已连接
            connected = true;
        } finally {
            if (!connected) {
                try {
                    // 未能连接成功时，关闭socket
                    close();
                } catch (IOException ioe) {
                    // 不要做任何事情，如果连接抛出异常，然后它将会被传递到调用栈中。
                }
            }
        }
    }

    /**
     * 创建一个socket并将它连接到指定地址的指定端口上
     *
     * @param address IP地址
     * @param port 指定端口
     */
    protected void connect(InetAddress address, int port) throws IOException {
        this.port = port;
        this.address = address;

        try {
            // 连接到指定IP
            connectToAddress(address, port, timeout);
            return;
        } catch (IOException e) {
            // 有任何失败，则抛出关闭并抛出异常
            close();
            throw e;
        }
    }

    /**
     * 创建一个socket，并将其连接到指定地址的指定端口上
     *
     * @param address IP地址
     * @param timeout 以毫秒为单位的超时时间，或零表示无超时
     * @throws IOException 如果连接失败时
     * @throws  IllegalArgumentException 如果地址为空，或该socket不支持SockerAdress的子类
     * @since 1.4
     */
    protected void connect(SocketAddress address, int timeout) throws IOException {
        boolean connected = false;
        try {
            // 地址必须为IP协议地址
            if (address == null || !(address instanceof InetSocketAddress))
                throw new IllegalArgumentException("unsupported address type");
            InetSocketAddress addr = (InetSocketAddress) address;
            // 如果主机名未被转换为IP地址，则报错
            if (addr.isUnresolved())
                throw new UnknownHostException(addr.getHostName());
            this.port = addr.getPort();
            this.address = addr.getAddress();

            // 在指定时间内连接到指定地址和端口
            connectToAddress(this.address, port, timeout);
            // 更新为已连接状态
            connected = true;
        } finally {
            if (!connected) {
                try {
                    // 未能连接，则关闭socket
                    close();
                } catch (IOException ioe) {
                    // 不要做任何事情，如果连接抛出异常，然后它将会被传递到调用栈中。
                }
            }
        }
    }

    /**
     * 在指定时间内连接到指定IP地址和指定端口
     *
     * @param address IP地址
     * @param port 端口
     * @param timeout 超时时间
     * @throws IOException 连接时发生异常
     */
    private void connectToAddress(InetAddress address, int port, int timeout) throws IOException {
        if (address.isAnyLocalAddress()) {
            // 对于本地地址，则使用本地IP进行连接
            doConnect(InetAddress.getLocalHost(), port, timeout);
        } else {
            doConnect(address, port, timeout);
        }
    }

    /**
     * 设置选项，支持的选项有：
     * <ul>
     *     <li>SO_LINGER: 启用后，close(2)或shutdown(2)将不会返回，直到socket的
     *     所有排队消息都已成功发送或已达到停留超时。否则，调用将立即返回，并在后台完成
     *     关闭。当socket作为exit(2)的一部分关闭时，它始终停留在后台。</li>
     *     <li>SO_TIMEOUT:</li>
     *     <li>IP_TOS:</li>
     *     <li>SO_BINDADDR:</li>
     *     <li>TCP_NODELAY:</li>
     *     <li>SO_SNDBUF:</li>
     *     <li>SO_RCVBUF:</li>
     *     <li>SO_KEEPALIVE: 启用面向连接的socket上的保持活动消息发送</li>
     *     <li>SO_OOBINLINE:</li>
     *     <li>SO_RESUEADDR:</li>
     * </ul>
     *
     *
     * @param opt
     * @param val
     * @throws SocketException
     */
    public void setOption(int opt, Object val) throws SocketException {
        if (isClosedOrPending()) {
            throw new SocketException("Socket Closed");
        }
        boolean on = true;
        switch (opt) {
            /* check type safety b4 going native.  These should never
             * fail, since only java.Socket* has access to
             * PlainSocketImpl.setOption().
             */
        case SO_LINGER:
            if (val == null || (!(val instanceof Integer) && !(val instanceof Boolean)))
                throw new SocketException("Bad parameter for option");
            if (val instanceof Boolean) {
                /* true only if disabling - enabling should be Integer */
                on = false;
            }
            break;
        case SO_TIMEOUT:
            if (val == null || (!(val instanceof Integer)))
                throw new SocketException("Bad parameter for SO_TIMEOUT");
            int tmp = ((Integer) val).intValue();
            if (tmp < 0)
                throw new IllegalArgumentException("timeout < 0");
            timeout = tmp;
            break;
        case IP_TOS:
             if (val == null || !(val instanceof Integer)) {
                 throw new SocketException("bad argument for IP_TOS");
             }
             trafficClass = ((Integer)val).intValue();
             break;
        case SO_BINDADDR:
            throw new SocketException("Cannot re-bind socket");
        case TCP_NODELAY:
            if (val == null || !(val instanceof Boolean))
                throw new SocketException("bad parameter for TCP_NODELAY");
            on = ((Boolean)val).booleanValue();
            break;
        case SO_SNDBUF:
        case SO_RCVBUF:
            if (val == null || !(val instanceof Integer) ||
                !(((Integer)val).intValue() > 0)) {
                throw new SocketException("bad parameter for SO_SNDBUF " +
                                          "or SO_RCVBUF");
            }
            break;
        case SO_KEEPALIVE:
            if (val == null || !(val instanceof Boolean))
                throw new SocketException("bad parameter for SO_KEEPALIVE");
            on = ((Boolean)val).booleanValue();
            break;
        case SO_OOBINLINE:
            if (val == null || !(val instanceof Boolean))
                throw new SocketException("bad parameter for SO_OOBINLINE");
            on = ((Boolean)val).booleanValue();
            break;
        case SO_REUSEADDR:
            if (val == null || !(val instanceof Boolean))
                throw new SocketException("bad parameter for SO_REUSEADDR");
            on = ((Boolean)val).booleanValue();
            break;
        default:
            throw new SocketException("unrecognized TCP option: " + opt);
        }
        socketSetOption(opt, on, val);
    }
    public Object getOption(int opt) throws SocketException {
        if (isClosedOrPending()) {
            throw new SocketException("Socket Closed");
        }
        if (opt == SO_TIMEOUT) {
            return new Integer(timeout);
        }
        int ret = 0;
        /*
         * The native socketGetOption() knows about 3 options.
         * The 32 bit value it returns will be interpreted according
         * to what we're asking.  A return of -1 means it understands
         * the option but its turned off.  It will raise a SocketException
         * if "opt" isn't one it understands.
         */

        switch (opt) {
        case TCP_NODELAY:
            ret = socketGetOption(opt, null);
            return Boolean.valueOf(ret != -1);
        case SO_OOBINLINE:
            ret = socketGetOption(opt, null);
            return Boolean.valueOf(ret != -1);
        case SO_LINGER:
            ret = socketGetOption(opt, null);
            return (ret == -1) ? Boolean.FALSE: (Object)(new Integer(ret));
        case SO_REUSEADDR:
            ret = socketGetOption(opt, null);
            return Boolean.valueOf(ret != -1);
        case SO_BINDADDR:
            InetAddressContainer in = new InetAddressContainer();
            ret = socketGetOption(opt, in);
            return in.addr;
        case SO_SNDBUF:
        case SO_RCVBUF:
            ret = socketGetOption(opt, null);
            return new Integer(ret);
        case IP_TOS:
            ret = socketGetOption(opt, null);
            if (ret == -1) { // ipv6 tos
                return new Integer(trafficClass);
            } else {
                return new Integer(ret);
            }
        case SO_KEEPALIVE:
            ret = socketGetOption(opt, null);
            return Boolean.valueOf(ret != -1);
        // should never get here
        default:
            return null;
        }
    }

    /**
     * 连接操作的主力。多次尝试与给定的 <host, port> 建立连接。如果不成功，
     * 则抛出{@link IOException}以指示出错的原因。
     *
     * @param address IP地址
     * @param port 端口
     * @param timeout 超时时间
     * @throws IOException 连接时发生
     */
    synchronized void doConnect(InetAddress address, int port, int timeout) throws IOException {
        // 在连接之前加锁
        synchronized (fdLock) {
            if (!closePending && (socket == null || !socket.isBound())) {
                NetHooks.beforeTcpConnect(fd, address, port);
            }
        }
        try {
            // 获取并自增fdUseCount
            acquireFD();
            try {
                // 建立连接
                socketConnect(address, port, timeout);
                // 在 poll/select 期间socket可能被关闭了
                synchronized (fdLock) {
                    if (closePending) {
                        throw new SocketException ("Socket closed");
                    }
                }
                // 如果有一个指向socket的引用，然后便将 created, bound和connected设置为true
                // 这通常在 Socket#connect() 中完成，但 Socket 的一些子类可能会直接调用
                // impl#connect()。
                if (socket != null) {
                    socket.setBound();
                    socket.setConnected();
                }
            } finally {
                // 释放
                releaseFD();
            }
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    /**
     * Binds the socket to the specified address of the specified local port.
     * @param address the address
     * @param lport the port
     */
    protected synchronized void bind(InetAddress address, int lport)
        throws IOException
    {
       synchronized (fdLock) {
            if (!closePending && (socket == null || !socket.isBound())) {
                NetHooks.beforeTcpBind(fd, address, lport);
            }
        }
        socketBind(address, lport);
        if (socket != null)
            socket.setBound();
        if (serverSocket != null)
            serverSocket.setBound();
    }

    /**
     * Listens, for a specified amount of time, for connections.
     * @param count the amount of time to listen for connections
     */
    protected synchronized void listen(int count) throws IOException {
        socketListen(count);
    }

    /**
     * Accepts connections.
     * @param s the connection
     */
    protected void accept(SocketImpl s) throws IOException {
        acquireFD();
        try {
            socketAccept(s);
        } finally {
            releaseFD();
        }
    }

    /**
     * Gets an InputStream for this socket.
     */
    protected synchronized InputStream getInputStream() throws IOException {
        synchronized (fdLock) {
            if (isClosedOrPending())
                throw new IOException("Socket Closed");
            if (shut_rd)
                throw new IOException("Socket input is shutdown");
            if (socketInputStream == null)
                socketInputStream = new SocketInputStream(this);
        }
        return socketInputStream;
    }

    void setInputStream(SocketInputStream in) {
        socketInputStream = in;
    }

    /**
     * Gets an OutputStream for this socket.
     */
    protected synchronized OutputStream getOutputStream() throws IOException {
        synchronized (fdLock) {
            if (isClosedOrPending())
                throw new IOException("Socket Closed");
            if (shut_wr)
                throw new IOException("Socket output is shutdown");
            if (socketOutputStream == null)
                socketOutputStream = new SocketOutputStream(this);
        }
        return socketOutputStream;
    }

    void setFileDescriptor(FileDescriptor fd) {
        this.fd = fd;
    }

    void setAddress(InetAddress address) {
        this.address = address;
    }

    void setPort(int port) {
        this.port = port;
    }

    void setLocalPort(int localport) {
        this.localport = localport;
    }

    /**
     * Returns the number of bytes that can be read without blocking.
     */
    protected synchronized int available() throws IOException {
        if (isClosedOrPending()) {
            throw new IOException("Stream closed.");
        }

        /*
         * If connection has been reset or shut down for input, then return 0
         * to indicate there are no buffered bytes.
         */
        if (isConnectionReset() || shut_rd) {
            return 0;
        }

        /*
         * If no bytes available and we were previously notified
         * of a connection reset then we move to the reset state.
         *
         * If are notified of a connection reset then check
         * again if there are bytes buffered on the socket.
         */
        int n = 0;
        try {
            n = socketAvailable();
            if (n == 0 && isConnectionResetPending()) {
                setConnectionReset();
            }
        } catch (ConnectionResetException exc1) {
            setConnectionResetPending();
            try {
                n = socketAvailable();
                if (n == 0) {
                    setConnectionReset();
                }
            } catch (ConnectionResetException exc2) {
            }
        }
        return n;
    }

    /**
     * Closes the socket.
     */
    protected void close() throws IOException {
        synchronized(fdLock) {
            if (fd != null) {
                if (!stream) {
                    ResourceManager.afterUdpClose();
                }
                if (fdUseCount == 0) {
                    if (closePending) {
                        return;
                    }
                    closePending = true;
                    /*
                     * We close the FileDescriptor in two-steps - first the
                     * "pre-close" which closes the socket but doesn't
                     * release the underlying file descriptor. This operation
                     * may be lengthy due to untransmitted data and a long
                     * linger interval. Once the pre-close is done we do the
                     * actual socket to release the fd.
                     */
                    try {
                        socketPreClose();
                    } finally {
                        socketClose();
                    }
                    fd = null;
                    return;
                } else {
                    /*
                     * If a thread has acquired the fd and a close
                     * isn't pending then use a deferred close.
                     * Also decrement fdUseCount to signal the last
                     * thread that releases the fd to close it.
                     */
                    if (!closePending) {
                        closePending = true;
                        fdUseCount--;
                        socketPreClose();
                    }
                }
            }
        }
    }

    void reset() throws IOException {
        if (fd != null) {
            socketClose();
        }
        fd = null;
        super.reset();
    }


    /**
     * Shutdown read-half of the socket connection;
     */
    protected void shutdownInput() throws IOException {
      if (fd != null) {
          socketShutdown(SHUT_RD);
          if (socketInputStream != null) {
              socketInputStream.setEOF(true);
          }
          shut_rd = true;
      }
    }

    /**
     * Shutdown write-half of the socket connection;
     */
    protected void shutdownOutput() throws IOException {
      if (fd != null) {
          socketShutdown(SHUT_WR);
          shut_wr = true;
      }
    }

    protected boolean supportsUrgentData () {
        return true;
    }

    protected void sendUrgentData (int data) throws IOException {
        if (fd == null) {
            throw new IOException("Socket Closed");
        }
        socketSendUrgentData (data);
    }

    /**
     * Cleans up if the user forgets to close it.
     */
    protected void finalize() throws IOException {
        close();
    }

    /**
     * 申请并返回用于该实现的文件描述符
     * @return 需要相应的 releaseFD 来释放文件描述符
     */
    FileDescriptor acquireFD() {
        synchronized (fdLock) {
            fdUseCount++;
            return fd;
        }
    }

    /**
     * 释放此实现的文件描述符，
     *
     * 如果使用总数变为-1，此socket已被关闭。
     */
    void releaseFD() {
        synchronized (fdLock) {
            fdUseCount--;
            if (fdUseCount == -1) {
                if (fd != null) {
                    try {
                        socketClose();
                    } catch (IOException e) {
                    } finally {
                        fd = null;
                    }
                }
            }
        }
    }

    /**
     * @return 检查连接是否重置
     */
    public boolean isConnectionReset() {
        synchronized (resetLock) {
            return (resetState == CONNECTION_RESET);
        }
    }

    /**
     * @return 检查连接是否重置待处理
     */
    public boolean isConnectionResetPending() {
        synchronized (resetLock) {
            return (resetState == CONNECTION_RESET_PENDING);
        }
    }

    /**
     * 设置连接为已重置
     */
    public void setConnectionReset() {
        synchronized (resetLock) {
            resetState = CONNECTION_RESET;
        }
    }

    /**
     * 设置连接为重置待处理
     */
    public void setConnectionResetPending() {
        synchronized (resetLock) {
            if (resetState == CONNECTION_NOT_RESET) {
                resetState = CONNECTION_RESET_PENDING;
            }
        }

    }

    /**
     * @return 如果已关闭或者关闭待定，则返回 true
     */
    public boolean isClosedOrPending() {
        // 对fdLock加锁以确保我们在关闭过程中等待
        synchronized (fdLock) {
            if (closePending || (fd == null)) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * @return SO_IIMEOUT的当前值
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * 通过复制文件描述符来预关闭socket，这使得可以在不释放文件描述符的情况下
     * 关闭socket。
     *
     * @throws IOException 预关闭时发生异常
     */
    private void socketPreClose() throws IOException {
        socketClose0(true);
    }

    /**
     * 关闭此socker，并释放文件描述符
     *
     * @throws IOException 关闭时发生错误
     */
    protected void socketClose() throws IOException {
        socketClose0(false);
    }

    abstract void socketCreate(boolean isServer) throws IOException;

    abstract void socketConnect(InetAddress address, int port, int timeout) throws IOException;

    abstract void socketBind(InetAddress address, int port) throws IOException;

    abstract void socketListen(int count) throws IOException;

    abstract void socketAccept(SocketImpl s) throws IOException;

    abstract int socketAvailable() throws IOException;

    abstract void socketClose0(boolean useDeferredClose) throws IOException;

    abstract void socketShutdown(int howto) throws IOException;

    abstract void socketSetOption(int cmd, boolean on, Object value) throws SocketException;

    abstract int socketGetOption(int opt, Object iaContainerObj) throws SocketException;

    abstract void socketSendUrgentData(int data) throws IOException;

    public final static int SHUT_RD = 0;
    public final static int SHUT_WR = 1;
}
