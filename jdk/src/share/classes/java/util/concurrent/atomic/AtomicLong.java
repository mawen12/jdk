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
import java.util.function.LongUnaryOperator;
import java.util.function.LongBinaryOperator;
import sun.misc.Unsafe;

/**
 * 可以被原子更新的{@code long}值。查看{@link java.util.concurrent.atomic}
 * 包规范获取关于原子变量的属性的描述。{@code AtomicLong}可以在应用中被用于原子更新
 * 标识，但是不能被用作替代{@link java.lang.Long}。然后，该类继承{@link java.lang.Number}
 * 来允许处理基于数字的类别的工具和实用程序进行统一访问。
 *
 * @since 1.5
 * @author Doug Lea
 */
public class AtomicLong extends Number implements java.io.Serializable {
    private static final long serialVersionUID = 1927816293512124184L;

    /**
     * 设置使用{@link Unsafe#compareAndSwapLong}来更新
     */
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    /**
     * {@link #value}的offset
     */
    private static final long valueOffset;

    /**
     * 记录底层JVM是否支持无锁compareAndSwap长整型。虽然Unsafe#compareAndSwapLong
     * 方法在任何一种情况下都有效，但某些构造应在Java级别处理，以避免锁定用户可见的锁。
     */
    static final boolean VM_SUPPORTS_LONG_CAS = VMSupportsCS8();

    /**
     * 返回底层JVM是否支持无锁的Long类型<code>CompareAndSet</code>。
     * 该方法仅调用一次，并被缓存在{@link #VM_SUPPORTS_LONG_CAS}中。
     */
    private static native boolean VMSupportsCS8();

    static {
        try {
            // 获取对于该对象的属性{@link #value}的相对偏移量
            valueOffset = unsafe.objectFieldOffset(AtomicLong.class.getDeclaredField("value"));
        } catch (Exception ex) { throw new Error(ex); }
    }

    /**
     * 保存实际值
     */
    private volatile long value;

    /**
     * 使用给定的值创建一个新的实例
     *
     * @param initialValue 初始值
     */
    public AtomicLong(long initialValue) {
        value = initialValue;
    }

    /**
     * 使用初始为0的值创建一个新的实例
     */
    public AtomicLong() {
    }

    /**
     * @return 当前值
     */
    public final long get() {
        return value;
    }

    /**
     * 更新为给定值
     *
     * @param newValue 新值
     */
    public final void set(long newValue) {
        value = newValue;
    }

    /**
     * 最终更新为给定值
     *
     * @param newValue 新值
     * @since 1.6
     */
    public final void lazySet(long newValue) {
        unsafe.putOrderedLong(this, valueOffset, newValue);
    }

    /**
     * 原子地设置给定值并返回旧值
     *
     * @param newValue 新值
     * @return 旧值
     */
    public final long getAndSet(long newValue) {
        return unsafe.getAndSetLong(this, valueOffset, newValue);
    }

    /**
     * 如果当前值等于预期值，那么就原子地设置该值为给定值。
     *
     * @param expect 期待值
     * @param update 新值
     * @return {@code true} 更新成功. {@code false} 更新失败，
     * 由于当前值与{@code expect}不相等。
     */
    public final boolean compareAndSet(long expect, long update) {
        return unsafe.compareAndSwapLong(this, valueOffset, expect, update);
    }

    /**
     * 如果当前值等于预期值，那么就原子地设置该值为给定值。
     *
     * <p><a href="package-summary.html#weakCompareAndSet">可能会意外失败，并且
     * 不提供顺序保证</a>, 因此很少成为{@code compareAndSet}的合适替代方案。
     *
     * @param expect 预期值
     * @param update 新值
     * @return {@code true} 更新成功. {@code false} 更新失败，
     * 由于当前值与{@code expect}不相等。
     */
    public final boolean weakCompareAndSet(long expect, long update) {
        return unsafe.compareAndSwapLong(this, valueOffset, expect, update);
    }

    /**
     * 原子地将当前值+1
     *
     * @return 之前值
     */
    public final long getAndIncrement() {
        return unsafe.getAndAddLong(this, valueOffset, 1L);
    }

    /**
     * 原子地将当前值-1
     *
     * @return 之前值
     */
    public final long getAndDecrement() {
        return unsafe.getAndAddLong(this, valueOffset, -1L);
    }

    /**
     * 以原子方式将给定值添加到当前值
     *
     * @param delta 要增加的值
     * @return 之前的值
     */
    public final long getAndAdd(long delta) {
        return unsafe.getAndAddLong(this, valueOffset, delta);
    }

    /**
     * 以原子方式将当前值+1，并返回更新后的值
     *
     * @return 更新后的值
     */
    public final long incrementAndGet() {
        return unsafe.getAndAddLong(this, valueOffset, 1L) + 1L;
    }

    /**
     * 以原子方式将当前值-1，并返回更新后的值
     *
     * @return 更新后的值
     */
    public final long decrementAndGet() {
        return unsafe.getAndAddLong(this, valueOffset, -1L) - 1L;
    }

    /**
     * 以原子方式将给定值添加到当前值，并返回更新后的值
     *
     * @param delta 要添加的值
     * @return 更新后的值
     */
    public final long addAndGet(long delta) {
        return unsafe.getAndAddLong(this, valueOffset, delta) + delta;
    }

    /**
     * 原子地用给定函数计算的结果更新当前值，并返回之前的值。
     * 该函数应该没有副作用，因为当尝试更新由于线程之间的争用而失败时，
     * 可以重新应用该函数。
     *
     * @param updateFunction 没有副作用的函数
     * @return 更新前的值
     * @since 1.8
     */
    public final long getAndUpdate(LongUnaryOperator updateFunction) {
        long prev, next;
        do {
            prev = get();
            next = updateFunction.applyAsLong(prev);
        } while (!compareAndSet(prev, next));
        return prev;
    }

    /**
     * 原子地应用给定函数计算的结果更新当前值，并返回更新后的值。
     * 该函数应该没有副作用，因为当尝试更新由于线程之间的争用而失败时，
     * 可以重新应用该函数。
     *
     * @param updateFunction 没有副作用的函数
     * @return 更新后的值
     * @since 1.8
     */
    public final long updateAndGet(LongUnaryOperator updateFunction) {
        long prev, next;
        do {
            prev = get();
            next = updateFunction.applyAsLong(prev);
        } while (!compareAndSet(prev, next));
        return next;
    }

    /**
     * 原子地应用通过给定函数根据当前和给定值计算的结果更新当前值，并返回之前的值。
     * 该函数应该没有副作用，因为当尝试更新由于线程之间的争用而失败时，
     * 可以重新应用该函数。该函数以当前值作为其第一个参数，以给定的更新作为第二个参数。
     *
     * @param x 更新值
     * @param accumulatorFunction 带有两个参数的没有副作用的函数
     * @return 之前的值
     * @since 1.8
     */
    public final long getAndAccumulate(long x, LongBinaryOperator accumulatorFunction) {
        long prev, next;
        do {
            prev = get();
            next = accumulatorFunction.applyAsLong(prev, x);
        } while (!compareAndSet(prev, next));
        return prev;
    }

    /**
     * 原子地应用通过给定函数根据当前和给定值计算的结果更新当前值，并返回更新后的值。
     * 该函数应该没有副作用，因为当尝试更新由于线程之间的争用而失败时，
     * 可以重新应用该函数。该函数以当前值作为其第一个参数，以给定的更新作为第二个参数。
     *
     * @param x 更新值
     * @param accumulatorFunction 带有两个参数的没有副作用的函数
     * @return 更新后的值
     * @since 1.8
     */
    public final long accumulateAndGet(long x, LongBinaryOperator accumulatorFunction) {
        long prev, next;
        do {
            prev = get();
            next = accumulatorFunction.applyAsLong(prev, x);
        } while (!compareAndSet(prev, next));
        return next;
    }

    /**
     * Returns the String representation of the current value.
     * @return the String representation of the current value
     */
    public String toString() {
        return Long.toString(get());
    }

    /**
     * Returns the value of this {@code AtomicLong} as an {@code int}
     * after a narrowing primitive conversion.
     * @jls 5.1.3 Narrowing Primitive Conversions
     */
    public int intValue() {
        return (int)get();
    }

    /**
     * Returns the value of this {@code AtomicLong} as a {@code long}.
     */
    public long longValue() {
        return get();
    }

    /**
     * Returns the value of this {@code AtomicLong} as a {@code float}
     * after a widening primitive conversion.
     * @jls 5.1.2 Widening Primitive Conversions
     */
    public float floatValue() {
        return (float)get();
    }

    /**
     * Returns the value of this {@code AtomicLong} as a {@code double}
     * after a widening primitive conversion.
     * @jls 5.1.2 Widening Primitive Conversions
     */
    public double doubleValue() {
        return (double)get();
    }

}
