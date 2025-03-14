/*
 * Copyright (c) 2000, 2004, Oracle and/or its affiliates. All rights reserved.
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

import java.io.Closeable;
import java.io.IOException;
import java.lang.Object;
import java.lang.Object;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Set;


/**
 * {@link java.nio.channels.SelectableChannel}对象的多路复用器。
 *
 * <p>可以通过调用{@link #open()}来创建一个selector，这将会使用系统
 * 默认的{@link java.nio.channels.spi.SelectorProvider}来创建
 * 一个新的selector。也可以调用自定义的selector provider的{@link
 * java.nio.channels.spi.SelectorProvider#openSelector}来创建
 * 一个新的selector。创建后的selector将保持打开直到使用{@link #close()}
 * 关闭。
 *
 * <p>可选择的channel与selector的注册由{@link java.nio.channels.SelectionKey}
 * 代表。一个selector维护三组selection key。
 * <ul>
 *     <li>key set: 包含表示此selector当前channel注册的键。该集合通过{@link #keys()}
 *     方法可以获取。</li>
 *     <li>selected-key set: 是一组键，其中每个键的channel在先前的选择操作期间被检测到
 *     已准备好执行键的兴趣集中标识的至少一个操作。此集合由{@link #selectedKeys()}方法
 *     返回，selected-key set始终是key set的子集。</li>
 *     <li>cancelled-key set：是key已经取消，但是channel尚未取消注册的键集合。该键
 *     不能直接访问，cancelled-key set始终是key set的子集。</li>
 * </ul>
 *
 * <p>在新创建的selector中，这三种键集合都为空。
 *
 * <p>通过channel的{@link java.nio.channels.SelectableChannel#register(java.nio.channels.Selector, int)}
 * 方法注册channel的副作用是将一个键添加到selector的key set中。在选择操作期间，calcelled-key
 * 将从key set中删除，key set本身不可直接修改。
 *
 * <p>当一个键被取消时，无论是通过{@link java.nio.channels.SelectionKey#cancel()}
 * 还是{@link java.nio.channels.Channel#close()}方法，都会导致该键被添加到cancelled-key中。
 * 取消键将导致其channel在下一次选择操作期间被注销，此时该键将从selector中的三个key set中删除。
 *
 * <p>通过选择操作可以将键添加到selected-key set中。通过对该集合调用{@link
 * java.util.Set#remove(java.lang.Object)}方法或者是{@link java.util.Iterator#remove()}
 * 方法来移除键。绝不会以任何其他方式从selected-key set中删除键。
 *
 * <h2>选择</h2>
 *
 * <p>在每次选择操作期间，键可能被添加到selector的selected-key或从其中移除。也可以将键从
 * cancelled-key中移除。通过{@link #select()}、{@link #select(long)}、{@link #selectNow()}
 * 方法执行选择，包括三个步骤：
 * <ol>
 *     <li>calcelled-key 中的每个键都将从其所属的key set中删除，并且channel将被注销，此步骤
 *     calcelled-key 为空。</li>
 *     <li>向底层操作系统查询更新，以确定每个剩余channel是否准备好执行选择操作开始时其键的兴趣集
 *     所标识的任何操作。对于已准备好执行至少一个此类通道的channel，将执行以下两个操作之一：
 *      <ol>
 *          <li>如果channel的键在selected-key set中上不存在，则将其添加到该集合中，
 *          并修改其就绪操作集以准确标识通道现在已报告为就绪的那些操作。之前记录在就绪集
 *          中的任何就绪信息将被丢弃。</li>
 *          <li>否则，如果channel的键已经在selelcted-key set中了，因此其它的就绪操作集
 *          将被修改，以识别任何报告channel已就绪的新操作，之前记录在就绪集中的任何就绪信息
 *          都会保留；换句话说，底层系统返回的就绪集按位分离到键的当前就绪集</li>
 *      </ol>
 *      如果此步骤开始时key set中的的兴趣集均为空，则selected-key和任何键就绪操作均不会更新。
 *     </li>
 *
 *     <li>如果在步骤2的执行中有任何键被添加到cancelled-key中，则它们将按照步骤1进行处理。</li>
 * </ol>
 *
 * <p>选择操作是否阻塞以等待一个或多个channel准备就绪，如果是，则阻塞多长时间，是这三个
 * 选择方法之间的唯一本质的区别。
 *
 * <h2>并发</h2>
 *
 * <p>selector本身可以被多个并发线程安全使用，但是它们的key set却不是。
 *
 * <p>选择操作按顺序在selector本身、key set和selected-key set上同步。
 * 它们还在上述步骤（1）和（3）中对cancelled-key进行同步。
 *
 * <p>
 *
 * <p> Changes made to the interest sets of a selector's keys while a
 * selection operation is in progress have no effect upon that operation; they
 * will be seen by the next selection operation.
 *
 * <p> Keys may be cancelled and channels may be closed at any time.  Hence the
 * presence of a key in one or more of a selector's key sets does not imply
 * that the key is valid or that its channel is open.  Application code should
 * be careful to synchronize and check these conditions as necessary if there
 * is any possibility that another thread will cancel a key or close a channel.
 *
 * <p> A thread blocked in one of the {@link #select()} or {@link
 * #select(long)} methods may be interrupted by some other thread in one of
 * three ways:
 *
 * <ul>
 *
 *   <li><p> By invoking the selector's {@link #wakeup wakeup} method,
 *   </p></li>
 *
 *   <li><p> By invoking the selector's {@link #close close} method, or
 *   </p></li>
 *
 *   <li><p> By invoking the blocked thread's {@link
 *   java.lang.Thread#interrupt() interrupt} method, in which case its
 *   interrupt status will be set and the selector's {@link #wakeup wakeup}
 *   method will be invoked. </p></li>
 *
 * </ul>
 *
 * <p> The {@link #close close} method synchronizes on the selector and all
 * three key sets in the same order as in a selection operation.
 *
 * <a name="ksc"></a>
 *
 * <p> A selector's key and selected-key sets are not, in general, safe for use
 * by multiple concurrent threads.  If such a thread might modify one of these
 * sets directly then access should be controlled by synchronizing on the set
 * itself.  The iterators returned by these sets' {@link
 * java.util.Set#iterator() iterator} methods are <i>fail-fast:</i> If the set
 * is modified after the iterator is created, in any way except by invoking the
 * iterator's own {@link java.util.Iterator#remove() remove} method, then a
 * {@link java.util.ConcurrentModificationException} will be thrown. </p>
 *
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 *
 * @see SelectableChannel
 * @see SelectionKey
 */

public abstract class Selector implements Closeable {

    /**
     * Initializes a new instance of this class.
     */
    protected Selector() { }

    /**
     * Opens a selector.
     *
     * <p> The new selector is created by invoking the {@link
     * java.nio.channels.spi.SelectorProvider#openSelector openSelector} method
     * of the system-wide default {@link
     * java.nio.channels.spi.SelectorProvider} object.  </p>
     *
     * @return  A new selector
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    public static Selector open() throws IOException {
        return SelectorProvider.provider().openSelector();
    }

    /**
     * Tells whether or not this selector is open.
     *
     * @return <tt>true</tt> if, and only if, this selector is open
     */
    public abstract boolean isOpen();

    /**
     * Returns the provider that created this channel.
     *
     * @return  The provider that created this channel
     */
    public abstract SelectorProvider provider();

    /**
     * Returns this selector's key set.
     *
     * <p> The key set is not directly modifiable.  A key is removed only after
     * it has been cancelled and its channel has been deregistered.  Any
     * attempt to modify the key set will cause an {@link
     * UnsupportedOperationException} to be thrown.
     *
     * <p> The key set is <a href="#ksc">not thread-safe</a>. </p>
     *
     * @return  This selector's key set
     *
     * @throws  ClosedSelectorException
     *          If this selector is closed
     */
    public abstract Set<SelectionKey> keys();

    /**
     * Returns this selector's selected-key set.
     *
     * <p> Keys may be removed from, but not directly added to, the
     * selected-key set.  Any attempt to add an object to the key set will
     * cause an {@link UnsupportedOperationException} to be thrown.
     *
     * <p> The selected-key set is <a href="#ksc">not thread-safe</a>. </p>
     *
     * @return  This selector's selected-key set
     *
     * @throws  ClosedSelectorException
     *          If this selector is closed
     */
    public abstract Set<SelectionKey> selectedKeys();

    /**
     * Selects a set of keys whose corresponding channels are ready for I/O
     * operations.
     *
     * <p> This method performs a non-blocking <a href="#selop">selection
     * operation</a>.  If no channels have become selectable since the previous
     * selection operation then this method immediately returns zero.
     *
     * <p> Invoking this method clears the effect of any previous invocations
     * of the {@link #wakeup wakeup} method.  </p>
     *
     * @return  The number of keys, possibly zero, whose ready-operation sets
     *          were updated by the selection operation
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  ClosedSelectorException
     *          If this selector is closed
     */
    public abstract int selectNow() throws IOException;

    /**
     * Selects a set of keys whose corresponding channels are ready for I/O
     * operations.
     *
     * <p> This method performs a blocking <a href="#selop">selection
     * operation</a>.  It returns only after at least one channel is selected,
     * this selector's {@link #wakeup wakeup} method is invoked, the current
     * thread is interrupted, or the given timeout period expires, whichever
     * comes first.
     *
     * <p> This method does not offer real-time guarantees: It schedules the
     * timeout as if by invoking the {@link Object#wait(long)} method. </p>
     *
     * @param  timeout  If positive, block for up to <tt>timeout</tt>
     *                  milliseconds, more or less, while waiting for a
     *                  channel to become ready; if zero, block indefinitely;
     *                  must not be negative
     *
     * @return  The number of keys, possibly zero,
     *          whose ready-operation sets were updated
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  ClosedSelectorException
     *          If this selector is closed
     *
     * @throws  IllegalArgumentException
     *          If the value of the timeout argument is negative
     */
    public abstract int select(long timeout)
        throws IOException;

    /**
     * Selects a set of keys whose corresponding channels are ready for I/O
     * operations.
     *
     * <p> This method performs a blocking <a href="#selop">selection
     * operation</a>.  It returns only after at least one channel is selected,
     * this selector's {@link #wakeup wakeup} method is invoked, or the current
     * thread is interrupted, whichever comes first.  </p>
     *
     * @return  The number of keys, possibly zero,
     *          whose ready-operation sets were updated
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  ClosedSelectorException
     *          If this selector is closed
     */
    public abstract int select() throws IOException;

    /**
     * Causes the first selection operation that has not yet returned to return
     * immediately.
     *
     * <p> If another thread is currently blocked in an invocation of the
     * {@link #select()} or {@link #select(long)} methods then that invocation
     * will return immediately.  If no selection operation is currently in
     * progress then the next invocation of one of these methods will return
     * immediately unless the {@link #selectNow()} method is invoked in the
     * meantime.  In any case the value returned by that invocation may be
     * non-zero.  Subsequent invocations of the {@link #select()} or {@link
     * #select(long)} methods will block as usual unless this method is invoked
     * again in the meantime.
     *
     * <p> Invoking this method more than once between two successive selection
     * operations has the same effect as invoking it just once.  </p>
     *
     * @return  This selector
     */
    public abstract Selector wakeup();

    /**
     * Closes this selector.
     *
     * <p> If a thread is currently blocked in one of this selector's selection
     * methods then it is interrupted as if by invoking the selector's {@link
     * #wakeup wakeup} method.
     *
     * <p> Any uncancelled keys still associated with this selector are
     * invalidated, their channels are deregistered, and any other resources
     * associated with this selector are released.
     *
     * <p> If this selector is already closed then invoking this method has no
     * effect.
     *
     * <p> After a selector is closed, any further attempt to use it, except by
     * invoking this method or the {@link #wakeup wakeup} method, will cause a
     * {@link ClosedSelectorException} to be thrown. </p>
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    public abstract void close() throws IOException;

}
