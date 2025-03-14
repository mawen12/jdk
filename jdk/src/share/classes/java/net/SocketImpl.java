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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileDescriptor;

/**
 * 抽象类{@link SocketImpl}是所有实际实现套接字的类的公共超类。
 * 它被用于创建客户端和服务端的sockets。
 *
 * <p>一个普通socket完全按照描述实现这些方法，而无需尝试穿过
 * 防火墙或代理。即其不支持防火墙或代理。
 *
 * @author  unascribed
 * @since   JDK1.0
 */
public abstract class SocketImpl implements SocketOptions {
    /**
     * 实际的Socket对象
     */
    Socket socket = null;
    /**
     * 服务端Socket对象
     */
    ServerSocket serverSocket = null;

    /**
     * 该Socket的文件描述对象
     */
    protected FileDescriptor fd;

    /**
     * 此socket的远端的IP地址
     */
    protected InetAddress address;

    /**
     * 此socket连接到的远程主机的端口号
     */
    protected int port;

    /**
     * 此socket连接的本地端口号
     */
    protected int localport;

    /**
     * 创建一个stream或dagagram socket
     *
     * @param      stream   如果为{@code true}，则创建一个stream socket，否则创建一个 datagram socket
     * @exception  IOException  如果在创建socket是发生I/O错误
     */
    protected abstract void create(boolean stream) throws IOException;

    /**
     * 将此socket连接到命名主机上的指定端口
     *
     * @param      host   远程主机的名称
     * @param      port   端口
     * @exception  IOException  如果连接到远程主机时发生I/O错误
     */
    protected abstract void connect(String host, int port) throws IOException;

    /**
     * 将此socket连接到指定socket地址上的注定端口
     *
     * @param      address   远程主机的IP地址
     * @param      port      端口
     * @exception  IOException  如果连接到远程主机时发生I/O错误
     */
    protected abstract void connect(InetAddress address, int port) throws IOException;

    /**
     * 将此socket连接到指定主机上的指定端口。
     * 零超时被解释为无限超时。连接将会阻塞，直到连接建立或发生异常。
     *
     * @param      address   远程主机的Socket地址
     * @param     timeout  超时时间，单位为毫秒，如果是0代表无限超时
     * @exception  IOException  如果尝试建立连接时发生I/O异常
     * @since 1.4
     */
    protected abstract void connect(SocketAddress address, int timeout) throws IOException;

    /**
     * 将此socket绑定到指定本地IP地址和端口号
     *
     * @param      host   属于本地接口的IP地址
     * @param      port   端口
     * @exception  IOException  如果绑定socket时发生I/O错误
     */
    protected abstract void bind(InetAddress host, int port) throws IOException;

    /**
     * 将传入连接指示（连接请求）的最大队列长度设置为{@code backlog}参数。
     * 如果在队列已满时达到连接指示，则拒绝连接。
     *
     * @param      backlog   队列的最大长度
     * @exception  IOException  如果创建队列时发生I/O异常
     */
    protected abstract void listen(int backlog) throws IOException;

    /**
     * 接受一个连接
     *
     * @param      s   接受的连接
     * @exception  IOException 如果接受连接时发生I/O异常
     */
    protected abstract void accept(SocketImpl s) throws IOException;

    /**
     * 返回此socket的输入流
     *
     * @return     从该socket读取的流
     * @exception  IOException  如果创建输入流时发生I/O异常
    */
    protected abstract InputStream getInputStream() throws IOException;

    /**
     * 返回此socket的输出流
     *
     * @return     an 用于写入该socket的输入流
     * @exception  IOException  如果创建输出流时发生I/O异常
     */
    protected abstract OutputStream getOutputStream() throws IOException;

    /**
     * 返回可以从此socket可以读取的无阻塞字节数
     *
     * @return     可以从此socket可以读取的无阻塞字节数
     * @exception  IOException  如果确定可用字节数时发生I/O异常
     */
    protected abstract int available() throws IOException;

    /**
     * 关闭该socket
     *
     * @exception  IOException  如果关闭该socket时发生I/O异常
     */
    protected abstract void close() throws IOException;

    /**
     * 将此socket的输入流置于流的末端。发送到此socket的任何数据都将
     * 被确认，然后被默默丢弃。
     *
     * <p>如果在socket上调用此方法后从socket输入流读取，则该流的
     * {@link InputStream#available()}将返回0，而其{@link InputStream#read()}
     * 将返回-1（流结束）。
     *
     * @exception IOException 如果暂停此socket时发生I/O异常
     * @see java.net.Socket#shutdownOutput()
     * @see java.net.Socket#close()
     * @see java.net.Socket#setSoLinger(boolean, int)
     * @since 1.3
     */
    protected void shutdownInput() throws IOException {
      throw new IOException("Method not implemented!");
    }

    /**
     * 禁用此socket的输入流。
     * 对于TCP socket，任何先前写入的数据都将被发送，然后遵循TCP的
     * 正常连接中止序列。
     *
     * <p>如果在socket上调用{@link #shutdownOutput()}后写入
     * socket输出流，则该流将抛出IOException。
     *
     * @exception IOException 如果暂停此socket时发生I/O异常
     * @see java.net.Socket#shutdownInput()
     * @see java.net.Socket#close()
     * @see java.net.Socket#setSoLinger(boolean, int)
     * @since 1.3
     */
    protected void shutdownOutput() throws IOException {
      throw new IOException("Method not implemented!");
    }

    /**
     * 返回socket上{@link #fd}字段的值
     *
     * @return  socket上{@link #fd}字段的值
     * @see     java.net.SocketImpl#fd
     */
    protected FileDescriptor getFileDescriptor() {
        return fd;
    }

    /**
     * 返回socket上{@link #address}字段的值
     *
     * @return  socket上{@link #address}字段的值
     * @see     java.net.SocketImpl#address
     */
    protected InetAddress getInetAddress() {
        return address;
    }

    /**
     * 返回socket上的{@link #port}字段的值
     *
     * @return  socket上的{@link #port}字段的值
     * @see     java.net.SocketImpl#port
     */
    protected int getPort() {
        return port;
    }

    /**
     * 返回此socket是否支持发送紧急数据。默认情况下，除非在子类中
     * 重写该方法，否则返回false。
     *
     * @return  {@code true}如果支持紧急数
     * @see     java.net.SocketImpl#address
     * @since 1.4
     */
    protected boolean supportsUrgentData () {
        return false; // must be overridden in sub-class
    }

    /**
     * 在此socket上发送一个字节的紧急数据。
     * 需要发送的字节是参数的的低八位
     *
     * @param data 要被发送的字节数
     * @exception IOException 如果发送数据时发生异常
     * @since 1.4
     */
    protected abstract void sendUrgentData (int data) throws IOException;

    /**
     * 返回此socket的{@link #localport}字段的值
     *
     * @return  此socket的{@link #localport}字段的值
     * @see     java.net.SocketImpl#localport
     */
    protected int getLocalPort() {
        return localport;
    }

    void setSocket(Socket soc) {
        this.socket = soc;
    }

    Socket getSocket() {
        return socket;
    }

    void setServerSocket(ServerSocket soc) {
        this.serverSocket = soc;
    }

    ServerSocket getServerSocket() {
        return serverSocket;
    }

    /**
     * 以字符串返回将此socket的地址和端口
     *
     * @return  a string representation of this socket.
     */
    public String toString() {
        return "Socket[addr=" + getInetAddress() +
            ",port=" + getPort() + ",localport=" + getLocalPort()  + "]";
    }

    void reset() throws IOException {
        address = null;
        port = 0;
        localport = 0;
    }

    /**
     * 设置该socket的性能偏好。
     *
     * <p>sockets默认使用TCP/IP协议。某些实现可能提供与TCP/IP具有不同性能特征
     * 的替代协议。该方法允许应用程序表达自己的偏好，即在从可用协议中进行选择时应
     * 任何进行这些权衡。
     *
     * <p>性能偏好由三个整数描述：其值表示短连接时间、低延迟和高带宽的相对重要性。
     * 整数的绝对值无关紧要；为了选择协议，只需比较这些值，值越大表示越好越强。
     * 负值表示优先级低于正值。例如：如果应用程序更喜欢短链接时间而不是低延迟和
     * 高带宽，那么它可以使用值{@code (1, 0, 0)}调用此方法。应用程序更喜欢高带宽
     * 而不是低延迟，并且更喜欢低延迟而不是短链接时间，那么它可以使用值{@code (0, 1, 2)}
     * 调用此方法。
     *
     * 默认情况下，该方法不做任何事情，除非子类覆盖它。
     *
     * @param  connectionTime 表示短连接时间的相对重要性
     * @param  latency 表示低延迟的相对重要性
     * @param  bandwidth 表示高带宽的相对重要性
     *
     * @since 1.5
     */
    protected void setPerformancePreferences(int connectionTime,
                                          int latency,
                                          int bandwidth)
    {
        /* Not implemented yet */
    }
}
