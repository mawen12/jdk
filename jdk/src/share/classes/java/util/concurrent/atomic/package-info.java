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

/**
 * 一个小型类的工具包，支持对单个变量进行无锁（lock-free）线程安全编程。
 * 本质上，此包中的类将{@code volatile}值、字段和数组元素的概念扩展为
 * 还提供以下形式的原子条件更新操作：
 * <pre>{@code
 *  boolean compareAndSet(expectedValue, updateValue);;
 * }</pre>
 *
 * <p>该方法（不同类的参数类型不同）如果变量持有{@code expectedValue}，
 * 则自动将其设置为{@code updateValue}，成功时报告{@code true}。
 * 此包中的类还包含获取和无条件设置值的方法，以及下文描述的较弱的条件原子更新操作
 * {@code weakCompareAndSet}。
 *
 * <p>这些方法规范使实现能够使用当代处理器上可用的高效机器级原子指令。
 * 但在某些平台上，支持可能需要某种形式的内部锁定。因此，这些方法不能
 * 严格保证不阻塞，线程可能会在执行操作之前暂时阻塞。
 *
 * <p>这些类的实例：
 * <ul>
 *     <li>{@link java.util.concurrent.atomic.AtomicBoolean}</li>
 *     <li>{@link java.util.concurrent.atomic.AtomicInteger}</li>
 *     <li>{@link java.util.concurrent.atomic.AtomicLong}</li>
 *     <li>{@link java.util.concurrent.atomic.AtomicReference}</li>
 * </ul>
 * 每一个都提供对相应类型的单个变量的访问和更新。每个类还为该类型提供适当的实用方法。
 * 例如：类{@code AtomicLong}和{@code AtomicInteger}提供原子的自增方法。
 * 一个应用示例是生成序列号：
 * <pre>{@code
 *  class Sequencer {
 *      private final AtomicLong sequenceNumber = new AtomicLong(0);
 *
 *      public long next() {
 *          return sequenceNumber.getAndIncrement();
 *      }
 *  }
 * }</pre>
 *
 * <p>定义新的实用函数很简单，例如：getAndIncrement，它可以原子地将函数应用于值。
 * 例如：给定一些转换：
 * <pre>{@code
 *  long transform(long input)
 * }</pre>
 * 按如下方式编写你的单元方法：
 * <pre>{@code
 *  long getAndTransform(AtomicLong var) {
 *      long prev, next;
 *      do {
 *          prev = var.get();
 *          next = transform(prev);
 *      } while (!var.compareAndSet(prev, next));
 *      return prev;
 *  }
 * }</pre>
 *
 * <p>原子访问和更新的记忆效应通常遵循volatile的规则，如<a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.4">
 * Java语言规范 (17.4 内存模型)</a>中所述。
 * <ul>
 *     <li>{@code get}具有读取{@code volatile}变量的记忆效应</li>
 *     <li>{@code set}具有写入（分配）{@code volatile}变量的记忆效应</li>
 *     <li>{@code lazySet}具有写入（分配）{@code volatile}变量的内存效果，
 *     但它允许对后续（但不是之前）内存操作进行重新排序，而这些操作本身不会对普通的
 *     非{@code volatile}写入施加重新排序的约束。在其他使用上下文中，{@code lazySet}
 *     可能适用于为了垃圾回收将不再访问的引用清空的清空。</li>
 *     <li>{@code weakCompareAndSet}原子地读取和有条件地写入变量，但不会创建任何
 *     先行发生顺序，因此对于除{@code weakCompareAndSet}的目标之外的任何变量的
 *     先前或后续读取和写入不提供任何保证。</li>
 *     <li>{@code compareAndSet}和其他读取和更新操作（例如{@code getAndIncrement}）
 *     都具有读取和写入{@code volatile}变量的记忆效应。</li>
 * </ul>
 *
 * <p>除了表示单个值的类之外，此包还包含Updater类，可用于获取对任何选定类的任何选定
 * {@code volatile}字段的{@code compareAndSet}操作。
 * <ul>
 *     <li>{@link java.util.concurrent.atomic.AtomicReferenceFieldUpdater}</li>
 *     <li>{@link java.util.concurrent.atomic.AtomicIntegerFieldUpdater}</li>
 *     <li>{@link java.util.concurrent.atomic.AtomicLongFieldUpdater}</li>
 *     <li>基于反射的实用程序，提供对相关字段类型的访问</li>
 * </ul>
 * 上述这些主要用于原子数据结构，其中同一个节点的多个{@code volatile}字段（例如：树节点的链接）
 * 独立地接受原子更新。这些类在如何以及何时实用原子更新方面提供了更大的灵活性，但代价是基于反射的
 * 设置更麻烦、实用不太方便以及保证较弱。
 *
 * <p>此包中还提供了对于数组更新的原子操作。
 * <ul>
 *     <li>{@link java.util.concurrent.atomic.AtomicIntegerArray}</li>
 *     <li>{@link java.util.concurrent.atomic.AtomicLongArray}</li>
 *     <li>{@link java.util.concurrent.atomic.AtomicReferenceArray}</li>
 * </ul>
 * 这些类进一步将原子操作支持扩展到这些类型的数组。这些类还因其数据元素提供
 * {@code volatile}访问语义而引人注目，而普通数据不支持这种访问语义。
 *
 * <p>原子类还支持方法{@code weakCompareAndSet}，但其适用性有限。
 * 在某些平台上，弱版本可能比{@code compareAndSet}更有效，但不同之处在于，
 * 对{@code weakCompareAndSet}方法的任何给定调用都可能返回{@code false}虚假
 * （即没有明显原因）。返回{@code false}仅表示可以根据需要重试该操作，依赖于
 * 保证当变量保持{@code expectedValue}并且没有其他线程也尝试设置变量时重复调用
 * 最终会成功。（这种虚假失败可能是由于内存争用效应造成的，与预期值和当前值是否相等
 * 无关）。此外，{@code strongCompareAndSet}不提供通常需要同步控制的排序保证。
 * 但是，当此类更新与程序的其他先行发生顺序无关时，该方法可能对更新计数器和统计信息有用。
 * 当线程看到由{@code weakCompareAndSet}引起的原子变量更新时，它不一定会看到在
 * {@code weakCompareAndSet}之前发生的任何其他变量的更新。例如，在更新性能统计
 * 数据时，这可能是可以接受的，但在其他情况下很少见。
 *
 * <p>{@link java.util.concurrent.atomic.AtomicMarkableReference}类将
 * 单个布尔值与引用关联。例如：此位可能在数据结构内部使用，表示所引用的对象在逻辑上
 * 已被删除。
 *
 * <p>{@link java.util.concurrent.atomic.AtomicStampedReference}类将
 * 单个int值与引用关联。例如：这可用于表示与一系列更新相对应的版本号。
 *
 * <p>原子类主要设计为实现非阻塞结构和相关基础结构类的构建块。{@code compareAndSet}
 * 方法不是锁定的一般替代品。它仅使用于对象的关键更新仅限于单个变量的情况。
 *
 * <p>原子类不是{@code java.lang.Integer}和相关类的通用替代品。它们不定义诸如
 * {@code equals}, {@code hashCode}和{@code compareTo}之类的方法。（由于
 * 原子变量预计会发生变异，因此它们不适合用作哈希表键）。此外，仅为预期应用中常用的
 * 类型提供类。例如，没有用于表示{@code byte}的原子类。在那些不太常见的情况下，
 * 如果您想要这样做，可以使用{@code AtomicInteger}来保存{@code byte}值。并进行
 * 适当的转换。
 *
 * <p>用户可以使用以下的类并进行转换分别来持有Float和Double。
 * <ul>
 *     <li>{@link java.lang.Float#floatToRawIntBits}</li>
 *     <li>{@link java.lang.Float#intBitsToFloat}</li>
 *     <li>{@link java.lang.Double#doubleToRawLongBits}</li>
 *     <li>{@link java.lang.Double#longBitsToDouble}</li>
 * </ul>
 *
 * @since 1.5
 */
package java.util.concurrent.atomic;
