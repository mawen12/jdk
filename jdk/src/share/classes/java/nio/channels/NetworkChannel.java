/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.Object;
import java.lang.Object;
import java.net.SocketOption;
import java.net.SocketAddress;
import java.util.Set;
import java.io.IOException;

/**
 * 代表网络socket的channel。
 *
 * <p>实现了该接口的channel代表是网络socket的channel。
 * {@link #bind(SocketAddress)}方法被用于绑定socket到本地{@link java.net.SocketAddress}，
 * {@link #getLocalAddress()}将返回socket绑定的地址。
 * {@link #setOption(SocketOption, java.lang.Object)}用于设置socket选项。
 * {@link #getOption(SocketOption)}用于获取socket选项。
 * 该接口的实现应该指定其支持的socket选项。
 *
 * <p>{@link #bind(SocketAddress)}和{@link #setOption(SocketOption, java.lang.Object)}
 * 方法没有返回值，但是被指定返回调用它们的channel。这允许链式方法调用，此接口的实现
 * 应该专门化返回类型，以便可以链接实现类上的方法调用。
 *
 * @since 1.7
 */

public interface NetworkChannel extends Channel {
    /**
     * 将channel的socket绑定到本地地址。
     *
     * <p>该方法用于在socket和本地地址间建立联系。一旦建立好联系，socket便会保持绑定
     * 直到channel被关闭。如果参数{@code local}为null，socket将被绑定到自动分配的地址。
     *
     * @param   local 用于socket绑定的地址，如果为{@code null}，将会自动分配一个地址
     *                给socket去绑定。
     *
     * @return  This channel
     *
     * @throws  AlreadyBoundException 已经有socket绑定了。

     * @throws  UnsupportedAddressTypeException 给定的地址类型不支持
     *
     * @throws  ClosedChannelException channel已经关闭
     *
     * @throws  IOException 如果发生某些I/O错误
     *
     * @throws  SecurityException 如果安装了安全管理器并且它拒绝未指定的权限。此接口的实现
     *                            应该指定任何所需的权限。
     *
     * @see #getLocalAddress
     */
    NetworkChannel bind(SocketAddress local) throws IOException;

    /**
     * 返回channel中的socket绑定的本地地址。
     *
     * <p>当channel绑定到网络协议socket地址({@link java.net.InetSocketAddress})时，
     * 此方法的返回值类型为{@link java.net.InetSocketAddress}。
     *
     * @return  channel中的socket绑定的本地地址，如果未绑定，则返回{@code null}。
     *
     * @throws  ClosedChannelException 如果channel已关闭
     *
     * @throws  IOException 如果发生I/O异常
     */
    SocketAddress getLocalAddress() throws IOException;

    /**
     * 设置socket选项的值
     *
     * @param   <T> socket选项值的类型
     * @param   name socket选项名称
     * @param   value socket选项的值，对于某些选项来说，{@code null}是可被接受的
     *
     * @return  This channel
     *
     * @throws  UnsupportedOperationException channel不支持该socket选项
     * @throws  IllegalArgumentException 对于socket选项来说是非法值
     * @throws  ClosedChannelException channel已关闭
     * @throws  IOException 如果发生I/O异常
     *
     * @see java.net.StandardSocketOptions
     */
    <T> NetworkChannel setOption(SocketOption<T> name, T value) throws IOException;

    /**
     * 返回socket选项的值
     *
     * @param   <T> socket选项值的类型
     * @param   name socket选项
     *
     * @return  value socket选项的值，对于某些选项来说，{@code null}是可被接受的
     *
     * @throws  UnsupportedOperationException channel不支持该socket选项
     * @throws  ClosedChannelException channel已关闭
     * @throws  IOException 如果发生I/O异常
     *
     * @see java.net.StandardSocketOptions
     */
    <T> T getOption(SocketOption<T> name) throws IOException;

    /**
     * 返回该channel支持的socket选项的集合。
     *
     * <p>该方法不受channel关闭的影响，即使已经关闭了，也会返回选项集合
     *
     * @return  该channel支持的socket选项的集合
     */
    Set<SocketOption<?>> supportedOptions();
}
