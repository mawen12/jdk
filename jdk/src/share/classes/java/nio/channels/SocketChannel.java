/*
 * Copyright (c) 2000, 2011, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.channels;

import java.io.IOException;
import java.lang.Object;
import java.net.Socket;
import java.net.SocketOption;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * 用于面向stream(TCP)链接socket的可选择的channel。
 *
 * <p>调用该类中的{@code #open}来创建一个新的socket channel。
 * 需要注意无法创建任意的，预先存在的socket。新常见的socket channel
 * 处于{@code open}，但尚未连接的状态。如果此时在该socket channel
 * 执行I/O操作，其会抛出{@link NotYetConnectedException}异常。
 * 调用{@link #connect(SocketAddress)}可以使socket channel
 * 状态变为{@code connected}。一旦建立连接，socket channel将
 * 保持该状态，直到其被关闭。通过{@link #isConnected()}方法可以
 * 检查socket channel是否已连接。
 *
 * <p>socket channel支持非阻塞连接，当一个socket channel建立完成后，
 * 通过{@link #connect(SocketAddress)}方法启动建立到远程socket channel
 * 的过程，以便稍后通过{@link #finishConnect()}方法完成。通过{@link
 * #isConnectionPending()}方法可以检查是否正在建立连接。
 *
 * <p>socket channel支持异步关闭，类似于{@code Channel#close}类中的异步关闭。
 * 如果输入端的socket channel被一个线程停止，同时另一个线程阻塞在socket channel
 * 的读操作上，此时处于阻塞状态的读操作线程将立即完成，但不会读到任何字节，而是返回-1.
 * 如果输出端的socket channel被一个线程停止，同时另外一个线程阻塞在socket channel
 * 的写操作上，此时处于阻塞状态的写操作线程将会收到{@code AsynchronousCloseException}.
 *
 * <p>通过{@link #setOption(SocketOption, java.lang.Object)}可以配置socket选项。
 * socket channel支持以下选项：
 * <table border summary="Socket 选项">
 *   <tr>
 *       <th>选项名</th>
 *       <th>描述</th>
 *   </tr>
 *   <tr>
 *       <td>{@link StandardSocketOptions#SO_SNDBUF}</td>
 *       <td>socket发送缓冲区大小</td>
 *   </tr>
 *   <tr>
 *       <td>{@link StandardSocketOptions#SO_RCVBUF}</td>
 *       <td>socket接受缓冲区大小</td>
 *   </tr>
 *   <tr>
 *       <td>{@link StandardSocketOptions#SO_KEEPALIVE}</td>
 *       <td>保持连接活跃时长</td>
 *   </tr>
 *   <tr>
 *       <td>{@link StandardSocketOptions#SO_REUSEADDR}</td>
 *       <td>复用地址</td>
 *   </tr>
 *   <tr>
 *       <td>{@link StandardSocketOptions#SO_LINGER}</td>
 *       <td>如果存在数据，则在关闭时停留（阻塞模式下）</td>
 *   </tr>
 *   <tr>
 *       <td>{@link StandardSocketOptions#TCP_NODELAY}</td>
 *       <td>禁用Nagle算法</td>
 *   </tr>
 * </table>
 *
 * 额外选项（实现特定）也可能支持。
 *
 * <p>socket channel是线程安全的。它支持并发读写，仅在同一时间支持最多
 * 一个线程写，最多一个线程读。{@link #connect(SocketAddress)}和
 * {@link #finishConnect()}方法相互同步。当这两个方法的任意一个在处理，
 * 尝试在该socket channel上的读/写操作都会被阻塞，直到方法完成。
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */

public abstract class SocketChannel
    extends AbstractSelectableChannel
    implements ByteChannel, ScatteringByteChannel, GatheringByteChannel, NetworkChannel
{

    /**
     * Initializes a new instance of this class.
     *
     * @param  provider
     *         The provider that created this channel
     */
    protected SocketChannel(SelectorProvider provider) {
        super(provider);
    }

    /**
     * Opens a socket channel.
     *
     * <p> The new channel is created by invoking the {@link
     * java.nio.channels.spi.SelectorProvider#openSocketChannel
     * openSocketChannel} method of the system-wide default {@link
     * java.nio.channels.spi.SelectorProvider} object.  </p>
     *
     * @return  A new socket channel
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    public static SocketChannel open() throws IOException {
        return SelectorProvider.provider().openSocketChannel();
    }

    /**
     * Opens a socket channel and connects it to a remote address.
     *
     * <p> This convenience method works as if by invoking the {@link #open()}
     * method, invoking the {@link #connect(SocketAddress) connect} method upon
     * the resulting socket channel, passing it <tt>remote</tt>, and then
     * returning that channel.  </p>
     *
     * @param  remote
     *         The remote address to which the new channel is to be connected
     *
     * @return  A new, and connected, socket channel
     *
     * @throws  AsynchronousCloseException
     *          If another thread closes this channel
     *          while the connect operation is in progress
     *
     * @throws  ClosedByInterruptException
     *          If another thread interrupts the current thread
     *          while the connect operation is in progress, thereby
     *          closing the channel and setting the current thread's
     *          interrupt status
     *
     * @throws  UnresolvedAddressException
     *          If the given remote address is not fully resolved
     *
     * @throws  UnsupportedAddressTypeException
     *          If the type of the given remote address is not supported
     *
     * @throws  SecurityException
     *          If a security manager has been installed
     *          and it does not permit access to the given remote endpoint
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public static SocketChannel open(SocketAddress remote)
        throws IOException
    {
        SocketChannel sc = open();
        try {
            sc.connect(remote);
        } catch (Throwable x) {
            try {
                sc.close();
            } catch (Throwable suppressed) {
                x.addSuppressed(suppressed);
            }
            throw x;
        }
        assert sc.isConnected();
        return sc;
    }

    /**
     * Returns an operation set identifying this channel's supported
     * operations.
     *
     * <p> Socket channels support connecting, reading, and writing, so this
     * method returns <tt>(</tt>{@link SelectionKey#OP_CONNECT}
     * <tt>|</tt>&nbsp;{@link SelectionKey#OP_READ} <tt>|</tt>&nbsp;{@link
     * SelectionKey#OP_WRITE}<tt>)</tt>.  </p>
     *
     * @return  The valid-operation set
     */
    public final int validOps() {
        return (SelectionKey.OP_READ
                | SelectionKey.OP_WRITE
                | SelectionKey.OP_CONNECT);
    }


    // -- Socket-specific operations --

    /**
     * @throws  ConnectionPendingException
     *          If a non-blocking connect operation is already in progress on
     *          this channel
     * @throws  AlreadyBoundException               {@inheritDoc}
     * @throws  UnsupportedAddressTypeException     {@inheritDoc}
     * @throws  ClosedChannelException              {@inheritDoc}
     * @throws  IOException                         {@inheritDoc}
     *
     * @since 1.7
     */
    @Override
    public abstract SocketChannel bind(SocketAddress local)
        throws IOException;

    /**
     * @throws  UnsupportedOperationException           {@inheritDoc}
     * @throws  IllegalArgumentException                {@inheritDoc}
     * @throws  ClosedChannelException                  {@inheritDoc}
     * @throws  IOException                             {@inheritDoc}
     *
     * @since 1.7
     */
    @Override
    public abstract <T> SocketChannel setOption(SocketOption<T> name, T value)
        throws IOException;

    /**
     * Shutdown the connection for reading without closing the channel.
     *
     * <p> Once shutdown for reading then further reads on the channel will
     * return {@code -1}, the end-of-stream indication. If the input side of the
     * connection is already shutdown then invoking this method has no effect.
     *
     * @return  The channel
     *
     * @throws  NotYetConnectedException
     *          If this channel is not yet connected
     * @throws  ClosedChannelException
     *          If this channel is closed
     * @throws  IOException
     *          If some other I/O error occurs
     *
     * @since 1.7
     */
    public abstract SocketChannel shutdownInput() throws IOException;

    /**
     * Shutdown the connection for writing without closing the channel.
     *
     * <p> Once shutdown for writing then further attempts to write to the
     * channel will throw {@link ClosedChannelException}. If the output side of
     * the connection is already shutdown then invoking this method has no
     * effect.
     *
     * @return  The channel
     *
     * @throws  NotYetConnectedException
     *          If this channel is not yet connected
     * @throws  ClosedChannelException
     *          If this channel is closed
     * @throws  IOException
     *          If some other I/O error occurs
     *
     * @since 1.7
     */
    public abstract SocketChannel shutdownOutput() throws IOException;

    /**
     * Retrieves a socket associated with this channel.
     *
     * <p> The returned object will not declare any public methods that are not
     * declared in the {@link java.net.Socket} class.  </p>
     *
     * @return  A socket associated with this channel
     */
    public abstract Socket socket();

    /**
     * Tells whether or not this channel's network socket is connected.
     *
     * @return  <tt>true</tt> if, and only if, this channel's network socket
     *          is {@link #isOpen open} and connected
     */
    public abstract boolean isConnected();

    /**
     * Tells whether or not a connection operation is in progress on this
     * channel.
     *
     * @return  <tt>true</tt> if, and only if, a connection operation has been
     *          initiated on this channel but not yet completed by invoking the
     *          {@link #finishConnect finishConnect} method
     */
    public abstract boolean isConnectionPending();

    /**
     * Connects this channel's socket.
     *
     * <p> If this channel is in non-blocking mode then an invocation of this
     * method initiates a non-blocking connection operation.  If the connection
     * is established immediately, as can happen with a local connection, then
     * this method returns <tt>true</tt>.  Otherwise this method returns
     * <tt>false</tt> and the connection operation must later be completed by
     * invoking the {@link #finishConnect finishConnect} method.
     *
     * <p> If this channel is in blocking mode then an invocation of this
     * method will block until the connection is established or an I/O error
     * occurs.
     *
     * <p> This method performs exactly the same security checks as the {@link
     * java.net.Socket} class.  That is, if a security manager has been
     * installed then this method verifies that its {@link
     * java.lang.SecurityManager#checkConnect checkConnect} method permits
     * connecting to the address and port number of the given remote endpoint.
     *
     * <p> This method may be invoked at any time.  If a read or write
     * operation upon this channel is invoked while an invocation of this
     * method is in progress then that operation will first block until this
     * invocation is complete.  If a connection attempt is initiated but fails,
     * that is, if an invocation of this method throws a checked exception,
     * then the channel will be closed.  </p>
     *
     * @param  remote
     *         The remote address to which this channel is to be connected
     *
     * @return  <tt>true</tt> if a connection was established,
     *          <tt>false</tt> if this channel is in non-blocking mode
     *          and the connection operation is in progress
     *
     * @throws  AlreadyConnectedException
     *          If this channel is already connected
     *
     * @throws  ConnectionPendingException
     *          If a non-blocking connection operation is already in progress
     *          on this channel
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  AsynchronousCloseException
     *          If another thread closes this channel
     *          while the connect operation is in progress
     *
     * @throws  ClosedByInterruptException
     *          If another thread interrupts the current thread
     *          while the connect operation is in progress, thereby
     *          closing the channel and setting the current thread's
     *          interrupt status
     *
     * @throws  UnresolvedAddressException
     *          If the given remote address is not fully resolved
     *
     * @throws  UnsupportedAddressTypeException
     *          If the type of the given remote address is not supported
     *
     * @throws  SecurityException
     *          If a security manager has been installed
     *          and it does not permit access to the given remote endpoint
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public abstract boolean connect(SocketAddress remote) throws IOException;

    /**
     * Finishes the process of connecting a socket channel.
     *
     * <p> A non-blocking connection operation is initiated by placing a socket
     * channel in non-blocking mode and then invoking its {@link #connect
     * connect} method.  Once the connection is established, or the attempt has
     * failed, the socket channel will become connectable and this method may
     * be invoked to complete the connection sequence.  If the connection
     * operation failed then invoking this method will cause an appropriate
     * {@link java.io.IOException} to be thrown.
     *
     * <p> If this channel is already connected then this method will not block
     * and will immediately return <tt>true</tt>.  If this channel is in
     * non-blocking mode then this method will return <tt>false</tt> if the
     * connection process is not yet complete.  If this channel is in blocking
     * mode then this method will block until the connection either completes
     * or fails, and will always either return <tt>true</tt> or throw a checked
     * exception describing the failure.
     *
     * <p> This method may be invoked at any time.  If a read or write
     * operation upon this channel is invoked while an invocation of this
     * method is in progress then that operation will first block until this
     * invocation is complete.  If a connection attempt fails, that is, if an
     * invocation of this method throws a checked exception, then the channel
     * will be closed.  </p>
     *
     * @return  <tt>true</tt> if, and only if, this channel's socket is now
     *          connected
     *
     * @throws  NoConnectionPendingException
     *          If this channel is not connected and a connection operation
     *          has not been initiated
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  AsynchronousCloseException
     *          If another thread closes this channel
     *          while the connect operation is in progress
     *
     * @throws  ClosedByInterruptException
     *          If another thread interrupts the current thread
     *          while the connect operation is in progress, thereby
     *          closing the channel and setting the current thread's
     *          interrupt status
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public abstract boolean finishConnect() throws IOException;

    /**
     * Returns the remote address to which this channel's socket is connected.
     *
     * <p> Where the channel is bound and connected to an Internet Protocol
     * socket address then the return value from this method is of type {@link
     * java.net.InetSocketAddress}.
     *
     * @return  The remote address; {@code null} if the channel's socket is not
     *          connected
     *
     * @throws  ClosedChannelException
     *          If the channel is closed
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @since 1.7
     */
    public abstract SocketAddress getRemoteAddress() throws IOException;

    // -- ByteChannel operations --

    /**
     * @throws  NotYetConnectedException
     *          If this channel is not yet connected
     */
    public abstract int read(ByteBuffer dst) throws IOException;

    /**
     * @throws  NotYetConnectedException
     *          If this channel is not yet connected
     */
    public abstract long read(ByteBuffer[] dsts, int offset, int length)
        throws IOException;

    /**
     * @throws  NotYetConnectedException
     *          If this channel is not yet connected
     */
    public final long read(ByteBuffer[] dsts) throws IOException {
        return read(dsts, 0, dsts.length);
    }

    /**
     * @throws  NotYetConnectedException
     *          If this channel is not yet connected
     */
    public abstract int write(ByteBuffer src) throws IOException;

    /**
     * @throws  NotYetConnectedException
     *          If this channel is not yet connected
     */
    public abstract long write(ByteBuffer[] srcs, int offset, int length)
        throws IOException;

    /**
     * @throws  NotYetConnectedException
     *          If this channel is not yet connected
     */
    public final long write(ByteBuffer[] srcs) throws IOException {
        return write(srcs, 0, srcs.length);
    }

    /**
     * {@inheritDoc}
     * <p>
     * If there is a security manager set, its {@code checkConnect} method is
     * called with the local address and {@code -1} as its arguments to see
     * if the operation is allowed. If the operation is not allowed,
     * a {@code SocketAddress} representing the
     * {@link java.net.InetAddress#getLoopbackAddress loopback} address and the
     * local port of the channel's socket is returned.
     *
     * @return  The {@code SocketAddress} that the socket is bound to, or the
     *          {@code SocketAddress} representing the loopback address if
     *          denied by the security manager, or {@code null} if the
     *          channel's socket is not bound
     *
     * @throws  ClosedChannelException     {@inheritDoc}
     * @throws  IOException                {@inheritDoc}
     */
    @Override
    public abstract SocketAddress getLocalAddress() throws IOException;

}
