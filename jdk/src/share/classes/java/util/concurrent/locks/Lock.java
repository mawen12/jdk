/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.locks;
import java.lang.Thread;
import java.util.concurrent.TimeUnit;

/**
 * {@code Lock}实现提供更多扩展的锁操作而不是通过使用{@code synchronized}
 * 修饰方法或语句。{@code Lock}实现允许更多灵活的结构，可以具有完全不同的属性，
 * 可能支持多个关联的{@code Condition}对象。
 *
 * <p>一个锁是一个用来控制访问被多个线程共享的资源的工具。通常，一个锁提供了对
 * 一个资源的排他访问：同一时间只能有一个线程申请到锁，完全访问到共享资源需要首先
 * 申请到锁。然而，某些锁实现允许并发访问到一个共享资源，例如
 * {@link java.util.concurrent.locks.ReadWriteLock}中的读锁。
 *
 * <p>{@code synchronized}方法或语句的使用提供了访问与每个对象关联的隐式监视器
 * 锁，但强制所有锁获取和释放以块结构的方式进行：当获取读个锁时，必须以相反顺序释放
 * 它们，并且所有锁必须在获取它们的相同词法范围内释放。
 *
 * <p>虽然{@code syncronized}方法和语句的作用域机制使得使用监视器锁变得更容易，
 * 并且有助于避免很多涉及锁的常见编程错误，但在某些情况下，您需要以更灵活地方式使用
 * 锁。例如：某些用于遍历并发访问的数据结构的算法需要使用"hand-over-hand"或"chain
 * locking"，您获取节点A的锁，然后节点B，然后释放A的锁，申请C的锁，然后释放B的锁再
 * 申请D的锁等等。{@code Lock}接口的实现允许使用此类技术，方法是允许在不同范围内
 * 获取和释放锁，并允许以任何顺序获取和释放多个锁，
 *
 * <p>随着灵活性的提高，责任也随之增加。没有块锁定的结构，就无法自动释放使用
 * {@code synchronized}方法和语句的锁。在大部分常见下，应使用以下习惯用法：
 * <pre>{@code
 *  Lock l = ...;
 *  l.lock();
 *  try {
 *      // 通过该锁访问受保护的资源
 *  } finally {
 *      l.unlock();
 *  }
 * }</pre>
 *
 * <p>当获取和释放锁发生在不同范围时，必须小心确保在持有锁期间执行的所有代码都受到
 * try-finally或try-catch的保护，以确保必要时释放锁。
 *
 * <p>{@code Lock}实现通过提供非阻塞获取锁{@link #tryLock()}、尝试获取可中断
 * 的锁{@link #lockInterruptibly()}，以及尝试获取可超时的锁
 * {@link #tryLock(long, TimeUnit)}，提供了比使用{@code synchronized}
 * 方法和语句更多的g功能。
 *
 * <p>一个{@code Lock}类可以提供完全不同于隐式监视器锁的行为和语句。例如顺序保证、
 * 非重入的使用、或死锁检测。如果一个实现提供上述特定语义，则该实现类必须在文档中
 * 注明这些语义。
 *
 * <p>需要注意{@code Lock}实例只是普通对象，它们本身可以用作{@code synchronized}
 * 语句中的目标。获取{@code Lock}实例的监视器锁和调用该实例的任何{@link #lock()}的
 * 方法没有特定的关系。为了避免混淆，建议您永远不要以这种方式使用{@code Lock}实例，
 * 除非是在自己的实现中。
 *
 * <p>除非另有说明，传递任何参数的{@code null}值都将抛出
 * {@link java.lang.NullPointerException}。
 *
 * <h3>内存同步</h3>
 *
 * <p>所有的{@code Lock}实现必须强制执行与内置监视器锁提供的
 * 相同的内存同步语义，具体描述位于
 * <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.4">
 * Java语言规范 (17.4 内存模型)</a>
 * <ul>
 *     <li>一个成功的{@code lock}操作具有与成功的Lock操作相同的内存同步语义</li>
 *     <li>一个成功的{@code unlock}操作具有与成功的Unlock操作相同的内存同步语义</li>
 * </ul>
 *
 * 不成功的加锁和解锁操作，并且可重入的加锁/解锁草走，不要求任何内存同步影响。
 *
 * <h3>实现注意事项</h3>
 *
 * <p>有三种锁获取的形式（可中断、不可中断和定时）可能存在性能特征、顺序保证或
 * 其他实现质量方面不同。此外，在给定的{@code Lock}类可能无法中断正在进行的
 * 锁获取。因此，实现不需要为所有三种形式的锁获取定义完全相同的保证和语义，也
 * 不需要支持中断正在进行的锁获取。实现需要清楚地记录每个锁定方法提供的语义和
 * 保证。它还必须遵守此接口中定义的中断语义，以支持完全中断锁获取：完全中断或
 * 仅在方法入口处中断。
 *
 * <p>由于中断通常意味着取消奥，并且中断检查通常不频繁，因此实现可能倾向于响应
 * 中断而不是正常方法返回。即使可以证明中断是在另一个线程可能已解除线程阻塞之后
 * 发生的，情况也是如此的，实现应该记录此行为。
 *
 * @see ReentrantLock
 * @see Condition
 * @see ReadWriteLock
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface Lock {

    /**
     * 获取锁。
     *
     * <p>如果锁不可用，则当前线程将被禁用，以进行线程调度，并处于休眠状态，
     * 直到获取锁为止。
     *
     * <p><b>实现注意事项</b></p>
     *
     * <p>一个{@code Lock}实现可能可以检测锁的错误使用，例如会导致死锁的
     * 调用，并且可能在这种情况下抛出（未经检查的）异常。该{@code Lock}实现
     * 必须记录这种情况和异常类型。
     */
    void lock();

    /**
     * 获取锁，除非当前线程已被打断{@link Thread#interrupt()}
     *
     * <p>获取锁仅当该锁可用，立刻返回
     *
     * <p>如果锁不可用，则当前线程将被禁用，以进行线程调度，并处于休眠状态，
     * 直到获取锁为止，直到发生以下两种情况的任意一个：
     * <ul>
     *     <li>锁被当前线程获取，或</li>
     *     <li>其他线程中断当前线程{@link Thread#interrupt()}，
     *     支持中断锁的获取</li>
     * </ul>
     *
     * <p>如果当前线程：
     * <ul>
     *     <li>在进入此方法时设置其中断状态</li>
     *     <li>在获取锁时被其他线程中断{@link Thread#interrupt()}，
     *     支持中断锁的获取</li>
     * </ul>
     * 然后将抛出{@link java.lang.InterruptedException}异常，并清理
     * 当前线程的打断状态。
     *
     * <p><b>实现注意事项</b></p>
     *
     * <p>打断锁获取的能力在某些实现也许不可能，如果可能的话也许是一个昂贵的操作。
     * 程序员应该意识到可能存在这种情况。当出现这种情况时{@link Lock}实现应该记录。
     *
     * <p>实现类应该倾向于响应中断而不是正常的方法返回。
     *
     * <p>{@link Lock}实现也许能够检测锁的错误使用，例如会导致死锁的
     * 调用，并且可能在这种情况下抛出（未经检查的）异常。该{@code Lock}实现
     * 必须记录这种情况和异常类型。
     *
     * @throws InterruptedException 如果当前线程在获取锁时被中断（支持中断锁的获取）
     */
    void lockInterruptibly() throws InterruptedException;

    /**
     * 仅在调用时锁处于空闲状态才获取锁。
     *
     * <p>如果锁可用，便获取锁然后立刻返回{@code true}。
     * 如果锁不可用，该方法将立刻返回{@code false}。
     *
     * <p>此方法的典型用法如下：
     * <pre>{@code
     *  Lock lock = ...;
     *  if (lock.tryLock()) {
     *      try {
     *          // 操作受保护状态
     *      } finally {
     *          lock.unlock();
     *      }
     *  }
     *  else {
     *      // 执行替代操作
     *  }
     * }</pre>
     *
     * 上述用法可以确保如果已获取锁，则解锁；如果未获取锁，则不会尝试解锁。
     *
     * @return {@code true} 如果锁已经申请，否则返回 {@code false}
     */
    boolean tryLock();

    /**
     * 如果在给定的等待时间内空间并且当前线程没有被{@linkplain java.lang.Thread#interrupt()}
     * 中断，则获取锁。
     *
     * <p>如果锁可用，方法立刻返回{@code true}。
     * 如果锁不可用，则当前线程将被禁用，以进行线程调度，并处于休眠状态，
     * 直到获取锁为止，直到发生以下两种情况的任意一个：
     * <ul>
     *     <li>锁被当前线程获取，或</li>
     *     <li>其他线程中断当前线程{@link Thread#interrupt()}，
     *     支持中断锁的获取</li>
     * </ul>
     *
     * <p>如果锁被申请，则返回{@code true}。
     *
     * <p>如果当前线程：
     * <ul>
     *     <li>在进入此方法时设置其中断状态</li>
     *     <li>在获取锁时被其他线程中断{@link Thread#interrupt()}，
     *     支持中断锁的获取</li>
     * </ul>
     * 然后将抛出{@link java.lang.InterruptedException}异常，并清理
     * 当前线程的打断状态。
     *
     * <p>如果指定的等待时间已过，则返回值{@code false}。如果时间小于或等于零，
     * 则该方法根本不会等待。
     *
     *
     * <p><b>实现注意事项</b></p>
     *
     * <p>打断锁获取的能力在某些实现也许不可能，如果可能的话也许是一个昂贵的操作。
     * 程序员应该意识到可能存在这种情况。当出现这种情况时{@link Lock}实现应该记录。
     *
     * <p>实现可以倾向于响应中断，而不是正常方法返回，或者报告超时。
     *
     * <p>
     *
     * <p>A {@code Lock} implementation may be able to detect
     * erroneous use of the lock, such as an invocation that would cause
     * deadlock, and may throw an (unchecked) exception in such circumstances.
     * The circumstances and the exception type must be documented by that
     * {@code Lock} implementation.
     *
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the {@code time} argument
     * @return {@code true} if the lock was acquired and {@code false}
     *         if the waiting time elapsed before the lock was acquired
     *
     * @throws InterruptedException if the current thread is interrupted
     *         while acquiring the lock (and interruption of lock
     *         acquisition is supported)
     */
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

    /**
     * Releases the lock.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>A {@code Lock} implementation will usually impose
     * restrictions on which thread can release a lock (typically only the
     * holder of the lock can release it) and may throw
     * an (unchecked) exception if the restriction is violated.
     * Any restrictions and the exception
     * type must be documented by that {@code Lock} implementation.
     */
    void unlock();

    /**
     * Returns a new {@link Condition} instance that is bound to this
     * {@code Lock} instance.
     *
     * <p>Before waiting on the condition the lock must be held by the
     * current thread.
     * A call to {@link Condition#await()} will atomically release the lock
     * before waiting and re-acquire the lock before the wait returns.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The exact operation of the {@link Condition} instance depends on
     * the {@code Lock} implementation and must be documented by that
     * implementation.
     *
     * @return A new {@link Condition} instance for this {@code Lock} instance
     * @throws UnsupportedOperationException if this {@code Lock}
     *         implementation does not support conditions
     */
    Condition newCondition();
}
