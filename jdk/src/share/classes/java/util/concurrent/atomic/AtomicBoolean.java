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
import sun.misc.Unsafe;

/**
 * 可以被原子更新的{@code boolean}值。查看{@link java.util.concurrent.atomic}
 * 包规范获取关于原子变量的属性的描述。{@code AtomicBoolean}可以在应用中被用于原子更新
 * 标识，但不能被用作{@link java.lang.Boolean}的替代。
 *
 * <p>使用场景：
 * <ul>
 *     <li>对于某些核心组件的启动，只需要一次，但是可能存在多个线程去触发</li>
 * </ul>
 *
 * @since 1.5
 * @author Doug Lea
 */
public class AtomicBoolean implements java.io.Serializable {
    private static final long serialVersionUID = 4654671469794556979L;

    /**
     * 设置为使用Unsafe#compareAndSwapInt进行更新
     */
    private static final Unsafe unsafe = Unsafe.getUnsafe();

    /**
     * {@link #value}的offset
     */
    private static final long valueOffset;

    static {
        try {
            // 获取AtomicBoolean#value的字段偏移量
            valueOffset = unsafe.objectFieldOffset(AtomicBoolean.class.getDeclaredField("value"));
        } catch (Exception ex) { throw new Error(ex); }
    }

    /**
     * 底层使用的值，使用该值作为boolean的底层标识
     * <ul>
     *     <li>0: false</li>
     *     <li>0: true</li>
     * </ul>
     */
    private volatile int value;

    /**
     * 使用给定的初始化的值创建一个新的{@code AtomicBoolean}
     *
     * @param initialValue 初始值
     */
    public AtomicBoolean(boolean initialValue) {
        value = initialValue ? 1 : 0;
    }

    /**
     * 使用初始化值{@code false}创建一个新的{@code AtomicBoolean}
     */
    public AtomicBoolean() {
    }

    /**
     * @return 当前值
     */
    public final boolean get() {
        return value != 0;
    }

    /**
     * 如果当前值和预期值相等时，原子地将值设置为给定值。
     *
     * @param expect 期望值
     * @param update 新的值
     * @return {@code true} 修改成功. {@code false}标明实际值与预期值不相等
     */
    public final boolean compareAndSet(boolean expect, boolean update) {
        // 将上层的boolean转换为底层的int
        int e = expect ? 1 : 0;
        int u = update ? 1 : 0;
        return unsafe.compareAndSwapInt(this, valueOffset, e, u);
    }

    /**
     * 如果当前值和预期值相等时，原子地将值设置为给定值。
     *
     * <p><a href="package-summary.html#weakCompareAndSet">
     * 可能会错误的失败并且不提供排序保证，因此很少会成为{@code compareAndSet}
     * 的替代方案。
     *
     * @param expect 期望值
     * @param update 新的值
     * @return {@code true} 修改成功. {@code false}标明实际值与预期值不相等
     */
    public boolean weakCompareAndSet(boolean expect, boolean update) {
        int e = expect ? 1 : 0;
        int u = update ? 1 : 0;
        return unsafe.compareAndSwapInt(this, valueOffset, e, u);
    }

    /**
     * 无条件的设置给定值
     *
     * @param newValue 新的值
     */
    public final void set(boolean newValue) {
        value = newValue ? 1 : 0;
    }

    /**
     * 最终设置为给定值
     *
     * @param newValue 新的值
     * @since 1.6
     */
    public final void lazySet(boolean newValue) {
        // 将上层的boolean转换为底层的int
        int v = newValue ? 1 : 0;
        unsafe.putOrderedInt(this, valueOffset, v);
    }

    /**
     * 原子地设置给定值并返回之前的值
     *
     * @param newValue 新的值
     * @return 之前的值
     */
    public final boolean getAndSet(boolean newValue) {
        boolean prev;
        // 无限循环确保了操作必然成功
        do {
            // 先获取之前的值
            prev = get();
            // 在执行cas操作
        } while (!compareAndSet(prev, newValue));
        // 返回之前的值
        return prev;
    }

    /**
     * Returns the String representation of the current value.
     * @return the String representation of the current value
     */
    public String toString() {
        return Boolean.toString(get());
    }

}
