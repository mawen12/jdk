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

package java.util.concurrent.atomic;
import java.io.Serializable;

/**
 * 一个或多个变量共同维持最初为零的{@code long}总和。当更新{@link #add(long)}
 * 在线程之间发生争用时，变量集可能会动态增长以减少争用。方法{@link #sum()}（或等效
 * {@link #longValue()}）返回维持总和的变量的当前总和。
 *
 * <p>当多个线程更新用于收集统计数据等目的的公共总和时，此类通常比
 * {@link java.util.concurrent.atomic.AtomicLong}更可取，而不是用于细粒度
 * 同步控制。在低更新争用情况下，这两个类具有相似的特征。但在高争用情况下，此类的预期
 * 吞吐量更高，但空间消耗更高。
 *
 * <p>该类可与{@link java.util.concurrent.ConcurrentHashMap}一起使用，以维护
 * 可扩展的频率图（直方图或多集的形式）。例如，要将计数添加到{@code ConcurrentHashMap<String, LongAdder> freqs}，
 * 如果尚不存在则进行初始化，您可以使用{@code freqs.computeIfAbsent(k -> new LongAdder()).increment()}。
 *
 * <p>该类继承{@link java.lang.Number}，但是并未定义类似于{@code equals}，{@code hashCode}和{@code compareTo}
 * 等方法，因为实例预计会发生变化，所以不能用作集合键。
 *
 * @since 1.8
 * @author Doug Lea
 */
public class LongAdder extends Striped64 implements Serializable {
    private static final long serialVersionUID = 7249069246863182397L;

    /**
     * 创建一个初始和为零的对象
     */
    public LongAdder() {
    }

    /**
     * 添加给定的值
     *
     * @param x 要被添加的值
     */
    public void add(long x) {
        Cell[] as; long b, v; int m; Cell a;
        if ((as = cells) != null || !casBase(b = base, b + x)) {
            boolean uncontended = true;
            if (as == null || (m = as.length - 1) < 0 ||
                (a = as[getProbe() & m]) == null ||
                !(uncontended = a.cas(v = a.value, v + x)))
                longAccumulate(x, null, uncontended);
        }
    }

    /**
     * 与{@link #add(1)}等效
     */
    public void increment() {
        add(1L);
    }

    /**
     * 与{@link #add(-1)}等效
     */
    public void decrement() {
        add(-1L);
    }

    /**
     * 返回当前总和。被返回的值并非一个原子镜像；在没有并发更新的情况下调用会返回
     * 准确的结果，但在计算总和时发生的并发更新可能不会被纳入。
     *
     * @return the sum
     */
    public long sum() {
        Cell[] as = cells; Cell a;
        long sum = base;
        if (as != null) {
            // 遍历所有cells
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    // 累加每个cell.value
                    sum += a.value;
            }
        }
        return sum;
    }

    /**
     * 将保持总和的变量重置为零。此方法可能是创建新的{@link LongAdder}的有用替代方法，
     * 但是只能在没有并发更新时有效。由于此方法本质上是危险的，因此应仅在已知没有线程并发
     * 更新时使用。
     */
    public void reset() {
        Cell[] as = cells; Cell a;
        base = 0L;
        if (as != null) {
            // 遍历所有的cells
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    // 将每个cell.value重置为0
                    a.value = 0L;
            }
        }
    }

    /**
     * 等效于{@link #sum()}后跟{@link #reset()}。该方法可能适用于
     * 多线程计算之间的静止点。如果该方法同时进行更新，则<b>不</b>保证
     * 返回值是重置前的最终值。
     *
     * @return the sum
     */
    public long sumThenReset() {
        Cell[] as = cells; Cell a;
        long sum = base;
        base = 0L;
        if (as != null) {
            // 遍历所有cells
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null) {
                    // 累加每个cell.value
                    sum += a.value;
                    // 将每个cell.value重置为0
                    a.value = 0L;
                }
            }
        }
        return sum;
    }

    /**
     *
     * @return {@link #sum()}的字符串表示
     */
    public String toString() {
        return Long.toString(sum());
    }

    /**
     * 等效于{@link #sum()}
     *
     * @return the sum
     */
    public long longValue() {
        return sum();
    }

    /**
     * 在缩小原始转换后，将{@link #sum()}作为{@code int}返回。
     */
    public int intValue() {
        return (int)sum();
    }

    /**
     * 在缩小原始转换后，将{@link #sum()}作为{@code float}返回。
     */
    public float floatValue() {
        return (float)sum();
    }

    /**
     * 在缩小原始转换后，将{@link #sum()}作为{@code double}返回。
     */
    public double doubleValue() {
        return (double)sum();
    }

    /**
     * Serialization proxy, used to avoid reference to the non-public
     * Striped64 superclass in serialized forms.
     * @serial include
     */
    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 7249069246863182397L;

        /**
         * The current value returned by sum().
         * @serial
         */
        private final long value;

        SerializationProxy(LongAdder a) {
            value = a.sum();
        }

        /**
         * Return a {@code LongAdder} object with initial state
         * held by this proxy.
         *
         * @return a {@code LongAdder} object with initial state
         * held by this proxy.
         */
        private Object readResolve() {
            LongAdder a = new LongAdder();
            a.base = value;
            return a;
        }
    }

    /**
     * Returns a
     * <a href="../../../../serialized-form.html#java.util.concurrent.atomic.LongAdder.SerializationProxy">
     * SerializationProxy</a>
     * representing the state of this instance.
     *
     * @return a {@link SerializationProxy}
     * representing the state of this instance
     */
    private Object writeReplace() {
        return new SerializationProxy(this);
    }

    /**
     * @param s the stream
     * @throws java.io.InvalidObjectException always
     */
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.InvalidObjectException {
        throw new java.io.InvalidObjectException("Proxy required");
    }

}
