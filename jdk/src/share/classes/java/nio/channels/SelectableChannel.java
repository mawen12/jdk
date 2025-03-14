/*
 * Copyright (c) 2000, 2008, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.channels.spi.SelectorProvider;


/**
 * 可以通过{@link java.nio.channels.Selector}进行多路复用的通道。
 *
 * <p>为了与selector一起使用，必须首先通过{@link #register(Selector, int, Object)}
 * 方法来注册该类的实例。该方法返回一个新的{@link java.nio.channels.SelectionKey}对象，
 * 代表channel和selector的注册。
 *
 * <p>一旦在selector上注册，channel将保留注册直到取消注册。这涉及到释放selector分配
 * 给channel的所有资源。
 *
 * <p>channel不能直接取消注册，相反，必须通过调用代表channel和selector的注册的
 * {@link java.nio.channels.SelectionKey#cancel()}方法。取消一个键要求
 * 在selector的下一次选择操作期间取消注册该channel。当channel关闭时，所有的键
 * 都会被隐式取消，无论是通过调用其{@link java.nio.channels.Channel#close()}
 * 还是通过中断channel上I/O操作中阻塞的线程。
 *
 * <p>如果selector本身被关闭，然后channel将被取消注册。代表其注册的key将失效，
 * 不再延迟。
 *
 * <p>一个channel最多可以向任何特定selector注册一次。
 *
 * <p>可以通过调用{@link #isRegistered()}方法来检查
 * 一个channel是否在一个或多个selector中注册。
 *
 * <p>可选择的channel可以被多线程安全的使用。
 *
 * <h2>阻塞模式</h2>
 *
 * 一个可选择的channel可以处于阻塞或非阻塞模式。在阻塞模式下，
 * 在channel上调用的每一个I/O操作将阻塞直到其完成。在非阻塞
 * 模式下，I/O操作将永远不会阻塞，并且可能传输比请求的更少的
 * 字节，甚至可能根本不传输字节。可以通过调用{@link #isBlocking()}
 * 方法来检查是否处于阻塞模式。
 *
 * <p>新创建的可选择的channel总是处于阻塞模式下。非阻塞模式与
 * 基于selector的多路复用结合使用时最有用。channel在向selector
 * 注册前必须处于非阻塞模式。直到取消注册后才可能返回阻塞模式。
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 *
 * @see SelectionKey
 * @see Selector
 */

public abstract class SelectableChannel extends AbstractInterruptibleChannel implements Channel {

    /**
     * 初始化该类的实例
     */
    protected SelectableChannel() { }

    /**
     * @return  创建该channel的provider
     */
    public abstract SelectorProvider provider();

    /**
     * Returns an <a href="SelectionKey.html#opsets">operation set</a>
     * identifying this channel's supported operations.  The bits that are set
     * in this integer value denote exactly the operations that are valid for
     * this channel.  This method always returns the same value for a given
     * concrete channel class.
     *
     * @return  The valid-operation set
     */
    public abstract int validOps();

    // Internal state:
    //   keySet, may be empty but is never null, typ. a tiny array
    //   boolean isRegistered, protected by key set
    //   regLock, lock object to prevent duplicate registrations
    //   boolean isBlocking, protected by regLock

    /**
     * Tells whether or not this channel is currently registered with any
     * selectors.  A newly-created channel is not registered.
     *
     * <p> Due to the inherent delay between key cancellation and channel
     * deregistration, a channel may remain registered for some time after all
     * of its keys have been cancelled.  A channel may also remain registered
     * for some time after it is closed.  </p>
     *
     * @return <tt>true</tt> if, and only if, this channel is registered
     */
    public abstract boolean isRegistered();
    //
    // sync(keySet) { return isRegistered; }

    /**
     * Retrieves the key representing the channel's registration with the given
     * selector.
     *
     * @param   sel
     *          The selector
     *
     * @return  The key returned when this channel was last registered with the
     *          given selector, or <tt>null</tt> if this channel is not
     *          currently registered with that selector
     */
    public abstract SelectionKey keyFor(Selector sel);
    //
    // sync(keySet) { return findKey(sel); }

    /**
     * Registers this channel with the given selector, returning a selection
     * key.
     *
     * <p> If this channel is currently registered with the given selector then
     * the selection key representing that registration is returned.  The key's
     * interest set will have been changed to <tt>ops</tt>, as if by invoking
     * the {@link SelectionKey#interestOps(int) interestOps(int)} method.  If
     * the <tt>att</tt> argument is not <tt>null</tt> then the key's attachment
     * will have been set to that value.  A {@link CancelledKeyException} will
     * be thrown if the key has already been cancelled.
     *
     * <p> Otherwise this channel has not yet been registered with the given
     * selector, so it is registered and the resulting new key is returned.
     * The key's initial interest set will be <tt>ops</tt> and its attachment
     * will be <tt>att</tt>.
     *
     * <p> This method may be invoked at any time.  If this method is invoked
     * while another invocation of this method or of the {@link
     * #configureBlocking(boolean) configureBlocking} method is in progress
     * then it will first block until the other operation is complete.  This
     * method will then synchronize on the selector's key set and therefore may
     * block if invoked concurrently with another registration or selection
     * operation involving the same selector. </p>
     *
     * <p> If this channel is closed while this operation is in progress then
     * the key returned by this method will have been cancelled and will
     * therefore be invalid. </p>
     *
     * @param  sel
     *         The selector with which this channel is to be registered
     *
     * @param  ops
     *         The interest set for the resulting key
     *
     * @param  att
     *         The attachment for the resulting key; may be <tt>null</tt>
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  ClosedSelectorException
     *          If the selector is closed
     *
     * @throws  IllegalBlockingModeException
     *          If this channel is in blocking mode
     *
     * @throws  IllegalSelectorException
     *          If this channel was not created by the same provider
     *          as the given selector
     *
     * @throws  CancelledKeyException
     *          If this channel is currently registered with the given selector
     *          but the corresponding key has already been cancelled
     *
     * @throws  IllegalArgumentException
     *          If a bit in the <tt>ops</tt> set does not correspond to an
     *          operation that is supported by this channel, that is, if
     *          {@code set & ~validOps() != 0}
     *
     * @return  A key representing the registration of this channel with
     *          the given selector
     */
    public abstract SelectionKey register(Selector sel, int ops, Object att)
        throws ClosedChannelException;
    //
    // sync(regLock) {
    //   sync(keySet) { look for selector }
    //   if (channel found) { set interest ops -- may block in selector;
    //                        return key; }
    //   create new key -- may block somewhere in selector;
    //   sync(keySet) { add key; }
    //   attach(attachment);
    //   return key;
    // }

    /**
     * Registers this channel with the given selector, returning a selection
     * key.
     *
     * <p> An invocation of this convenience method of the form
     *
     * <blockquote><tt>sc.register(sel, ops)</tt></blockquote>
     *
     * behaves in exactly the same way as the invocation
     *
     * <blockquote><tt>sc.{@link
     * #register(java.nio.channels.Selector,int,java.lang.Object)
     * register}(sel, ops, null)</tt></blockquote>
     *
     * @param  sel
     *         The selector with which this channel is to be registered
     *
     * @param  ops
     *         The interest set for the resulting key
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  ClosedSelectorException
     *          If the selector is closed
     *
     * @throws  IllegalBlockingModeException
     *          If this channel is in blocking mode
     *
     * @throws  IllegalSelectorException
     *          If this channel was not created by the same provider
     *          as the given selector
     *
     * @throws  CancelledKeyException
     *          If this channel is currently registered with the given selector
     *          but the corresponding key has already been cancelled
     *
     * @throws  IllegalArgumentException
     *          If a bit in <tt>ops</tt> does not correspond to an operation
     *          that is supported by this channel, that is, if {@code set &
     *          ~validOps() != 0}
     *
     * @return  A key representing the registration of this channel with
     *          the given selector
     */
    public final SelectionKey register(Selector sel, int ops)
        throws ClosedChannelException
    {
        return register(sel, ops, null);
    }

    /**
     * Adjusts this channel's blocking mode.
     *
     * <p> If this channel is registered with one or more selectors then an
     * attempt to place it into blocking mode will cause an {@link
     * IllegalBlockingModeException} to be thrown.
     *
     * <p> This method may be invoked at any time.  The new blocking mode will
     * only affect I/O operations that are initiated after this method returns.
     * For some implementations this may require blocking until all pending I/O
     * operations are complete.
     *
     * <p> If this method is invoked while another invocation of this method or
     * of the {@link #register(Selector, int) register} method is in progress
     * then it will first block until the other operation is complete. </p>
     *
     * @param  block  If <tt>true</tt> then this channel will be placed in
     *                blocking mode; if <tt>false</tt> then it will be placed
     *                non-blocking mode
     *
     * @return  This selectable channel
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  IllegalBlockingModeException
     *          If <tt>block</tt> is <tt>true</tt> and this channel is
     *          registered with one or more selectors
     *
     * @throws IOException
     *         If an I/O error occurs
     */
    public abstract SelectableChannel configureBlocking(boolean block)
        throws IOException;
    //
    // sync(regLock) {
    //   sync(keySet) { throw IBME if block && isRegistered; }
    //   change mode;
    // }

    /**
     * Tells whether or not every I/O operation on this channel will block
     * until it completes.  A newly-created channel is always in blocking mode.
     *
     * <p> If this channel is closed then the value returned by this method is
     * not specified. </p>
     *
     * @return <tt>true</tt> if, and only if, this channel is in blocking mode
     */
    public abstract boolean isBlocking();

    /**
     * Retrieves the object upon which the {@link #configureBlocking
     * configureBlocking} and {@link #register register} methods synchronize.
     * This is often useful in the implementation of adaptors that require a
     * specific blocking mode to be maintained for a short period of time.
     *
     * @return  The blocking-mode lock object
     */
    public abstract Object blockingLock();

}
