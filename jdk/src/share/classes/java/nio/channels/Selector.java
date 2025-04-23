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
import java.lang.Object;
import java.lang.Thread;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.SelectorProvider;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
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
 * <p>在选择进行时，同时对对selector中感兴趣键的进行操作没有任何印象，
 * 因为这些修改会在下一次选择操作可见。
 *
 * <p>键可以在任何时候被取消，channel可以在任何被关闭。因此在一个或多个
 * selector中存在的键并不意味着它是合法或它的channel是打开的。应用代码
 * 应该在必要的时候小心同步和检查这些条件，因为其他线程有可能取消一个键或
 * 关闭channel。
 *
 * <p>阻塞在{@link #select()}或是{@link #select(long)}方法上的线程
 * 有可能被被其他线程打断，以下是三种打断方式：
 *
 * <ul>
 *     <li>通过调用selector的{@link #wakeup()}方法</li>
 *     <li>通过调用selector的{@link #close()}方法</li>
 *     <li>通过调用阻塞线程的{@link java.lang.Thread#interrupt()}方法，
 *     这将设置线程的打断状态，并调用selector的{@link #wakeup()}方法。</li>
 * </ul>
 *
 * <p>selector中的方法{@link #close()}是同步的，在一个选择操作中所有三种
 * 键集合都是以相同顺序。
 *
 * <p>在多个并发线程中，seletor的键和selected-key set并不是安全的。例如一个
 * 线程可能编辑其中一种集合然后访问，这个操作应该被set本身进行同步控制。返回这些
 * 集合的{@link java.util.Set#iterator()}方法是快速失败：如果集合在iterator
 * 创建后被修改，而不是通过{@link java.util.Iterator#remove()}方法，则会抛出
 * {@link java.util.ConcurrentModificationException}。
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
     * 打开一个selector
     *
     * <p>通过系统默认的{@link java.nio.channels.spi.SelectorProvider}调用
     * {@link java.nio.channels.spi.SelectorProvider#openSelector()}方法
     * 来创建一个新的{@link Selector}。
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
     * 返回selector是否已打开。
     *
     * @return <tt>true</tt> if, and only if, this selector is open
     */
    public abstract boolean isOpen();

    /**
     * 返回创建该channel的provider
     *
     * @return  The provider that created this channel
     */
    public abstract SelectorProvider provider();

    /**
     * 返回selector的key集合
     *
     * <p>该key集合不允许直接编辑。key仅在被取消或其channel被注销时才移除。
     * 任何尝试编辑该key集合都将导致{@link UnsupportedOperationException}。
     *
     * <p>该key集合不是线程安全的。
     *
     * @return  This selector's key set
     *
     * @throws  ClosedSelectorException
     *          If this selector is closed
     */
    public abstract Set<SelectionKey> keys();

    /**
     * 返回selector的selected-key集合。
     *
     * <p>key可以从此集合移除，但不能直接添加。任何尝试向其添加的操作
     * 都会导致{@link UnsupportedOperationException}。
     *
     * <p>该key集合不是线程安全的。
     *
     * @return  This selector's selected-key set
     *
     * @throws  ClosedSelectorException
     *          If this selector is closed
     */
    public abstract Set<SelectionKey> selectedKeys();

    /**
     * 返回相关channel已经为I/O操作准备好的key集合的数量。
     *
     * <p>该方法执行<b>非阻塞</b>的选择操作。如果从上次选择操作后
     * 还没有任何channel变为可选择该方法会立即返回0.
     *
     * <p>调用该方法将清除任何之前调用{@link #wakeup()}方法所产生的影响。
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
     * 返回channel已准备好的，且对应key在当前selector中的数量。
     *
     * <p>该方法执行阻塞选择操作。它仅在以下情况之一发生时才会返回：
     * <ol>
     *      <li>至少一个channel可选择</li>
     *      <li>其他线程调用了{@link #wakeup()}</li>
     *      <li>当前线程被打断</li>
     *      <li>给定时间超时</li>
     * </ol>
     *
     * <p>该方法不会提供实时性保证：因为其底层通过调用{@link
     * java.lang.Object#wait(long)}来实现超时的。
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
     * 返回channel已准备好的，且对应key在当前selector中的数量。
     *
     * <p>该方法执行阻塞选择操作。它仅在以下情况之一发生时才会返回：
     * <ol>
     *     <li>至少一个channel可选择</li>
     *     <li>其他线程调用了{@link #wakeup()}</li>
     *     <li>当前线程被打断</li>
     * </ol>
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
     * 使得尚未返回的第一个选择操作立即返回。
     *
     * <p>如果另一个线程当前正阻塞在{@link #select(long)}
     * 或{@link #select()}方法上，调用后会导致其立即返回。
     * 如果当前没有选择操作阻塞，上述方法的下次调用将立即返回
     * 除非同时调用了{@link #selectNow()}方法。在任何情况下，
     * 该方法返回值可能为非0。后续对于{@link #select()}或
     * {@link #select(long)}方法调用将正常阻塞，除非同时
     * 调用该方法。
     *
     * <p>在两次成功选择操作之间多次调用该方法不会产生副作用。
     *
     * @return  This selector
     */
    public abstract Selector wakeup();

    /**
     * 关闭该selector。
     *
     * <p>如果一个线程当前正阻塞在selector的选择方法上，
     * 它将被打断，仿佛调用了{@link Selector#wakeup()}。
     *
     * <p>任何与该selector关联的已取消的键将被非法化。
     * 它们的channel将被注销，与selector有关的任何其他
     * 资源将被释放。
     *
     * <p>如果selector已关闭，再次调用不会产生任何影响。
     *
     * <p>在selector关闭后，调用除{@link #close()}和
     * {@link #wakeup()}以外的任何方法都会导致{@link
     * java.nio.channels.ClosedChannelException}。
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    public abstract void close() throws IOException;

}
