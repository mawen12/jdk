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
import java.util.function.LongBinaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 一个本地包类，包含支持64位值动态条带化的类的通用表示和机制。
 * 该类扩展了{@link java.lang.Number}，因此具体子类必须
 * 公开这样做。
 */
@SuppressWarnings("serial")
abstract class Striped64 extends Number {
    /*
     * 该类维护了一个懒初始化的原子更新变量表，以及一个额外的{@code base}字段，
     * 表的大小是2的次幂。索引使用屏蔽的每个线程哈希码。此类中的几乎所有声明都是包
     * 私有的，可由子类直接访问。
     *
     * 表的条目属于{@link Cell}；填充通过{@link sum.misc.Contended}来减少
     * 缓存争用。填充对于大多数Atmoic来说都是多余的。因为它们通常不规则的分散在
     * 内存中，因此不会相互干扰太多。但是驻留在数组中的Atomic对象将倾向于彼此相邻
     * 放置，因此如果没有这种预防措施，它们通常会共享缓存行（对性能产生巨大的负面影响）。
     *
     * 部分原因是因为Cell相对较大，因此我们避免创建它们，直到需要它们为止。当没有争用时，
     * 所有更新都对基本字段进行。当第一个争用时（<code>base</code>更新时CAS失败），
     * 表的大小初始化为2。在进一步争用时，表大小将加倍，直到达到最接近的2的次幂，
     * 大于或等于CPUS的数量。表槽位保持空，直到需要它们为止。
     *
     * 单个的自旋锁（当cells繁忙时）用于初始化和调整表大小，以及用新单元填充插槽。
     * 此处无需阻塞锁；当锁不可用时，线程将尝试其他的插槽或<code>base</code>字段。
     * 在这些重试期间，争用会增加，局部性会降低，但仍然比其他方法更好。
     *
     * 通过{@link ThreadLocalRandom}维护的线程探测字段用作每个线程的哈希码。
     * 我们让它们保持未初始化状态，即为零（如果它们以这种方式出现），直到它们在槽位
     * 0处发生争用，然后将它们初始化为通常不会与其他值发生冲突的值。执行更新操作时，
     * CAS失败会指示争用和/或表冲突。发生冲突时，如果表大小小于容量，则将其大小翻倍，
     * 除非其他线程持有锁。如果哈希槽为空，并且锁可用，则创建一个新的单元。否则，如果
     * 槽位存在，则尝试CAS。重试通过”双重哈希“进行，使用二级哈希（Marsaglia XorShift）
     * 尝试找到空闲槽位。
     *
     * 表大小有上限，因为当线程数多过CPU时，假设每个线程都绑定到一个CPU，就会存在一个
     * 完美的哈希函数将线程映射到插槽，从而消除冲突。当我们达到容量时，我们会通过随机改变
     * 冲突线程的哈希码来搜索此映射。由于搜索是随机的，并且冲突只能通过CAS故障得知，
     * 因此收敛速度可能很慢，并且由于线程通常不会永远绑定到CPU，因此可能根本不会发生冲突。
     * 但是，尽管存在这些限制，在这些情况下观察到的争用率通常很低。
     *
     * 当曾经散列到单元格的线程终止时，以及在将表加倍导致没有线程的扩展掩码下散列到单元格
     * 的情况下，单元格可能会变得未使用。我们不会尝试检测或删除此类单元格，因为对于长期运行
     * 的实例，观察到的争用级别将再次出现，因此最终将再次需要这些单元格；对于短暂的单元格，
     * 这并不重要。
     */

    /**
     * AtomicLong 的填充变体，仅支持原始访问和CAS。
     *
     * JVM 内在函数注意事项：如果提供了CAS，则可以在此处使用仅发布形式的CAS。
     */
    @sun.misc.Contended static final class Cell {
        /**
         * 多线程修改可见
         */
        volatile long value;
        Cell(long x) { value = x; }

        /**
         * CAS操作
         *
         * @param cmp 预期值
         * @param val 新的值
         * @return CAS操作结果
         */
        final boolean cas(long cmp, long val) {
            return UNSAFE.compareAndSwapLong(this, valueOffset, cmp, val);
        }

        // 不安全机制
        private static final sun.misc.Unsafe UNSAFE;
        private static final long valueOffset;
        static {
            try {
                // UNSAFE对象
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> ak = Cell.class;
                // 获取{@link #value}变量的内存地址
                valueOffset = UNSAFE.objectFieldOffset
                    (ak.getDeclaredField("value"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /**
     * CPU的数量，限制表的大小
     */
    static final int NCPU = Runtime.getRuntime().availableProcessors();

    /**
     * 单元格表，非空时，大小为2的次幂。
     */
    transient volatile Cell[] cells;

    /**
     * 基值，主要用于没有争用的情况，但也作为表初始化争用期间的后备。
     * 通过CAS更新
     */
    transient volatile long base;

    /**
     * 调整大小和创建单元格时使用自旋锁（通过CAS锁定）
     */
    transient volatile int cellsBusy;

    /**
     * 包私有的默认构造器
     */
    Striped64() {
    }

    /**
     * CAS {@link #base}字段
     */
    final boolean casBase(long cmp, long val) {
        return UNSAFE.compareAndSwapLong(this, BASE, cmp, val);
    }

    /**
     * CAS {@link #cellsBusy}字段，从0到1来申请锁
     */
    final boolean casCellsBusy() {
        return UNSAFE.compareAndSwapInt(this, CELLSBUSY, 0, 1);
    }

    /**
     * 返回当前线程的探测值。
     * 由于包限制，从{@link ThreadLocalRandom}复制。
     */
    static final int getProbe() {
        return UNSAFE.getInt(Thread.currentThread(), PROBE);
    }

    /**
     * 伪随机地推进和记录给定线程的给定探测值。
     * 由于包限制，从{@link ThreadLocalRandom}复制。
     */
    static final int advanceProbe(int probe) {
        probe ^= probe << 13;   // xorshift
        probe ^= probe >>> 17;
        probe ^= probe << 5;
        UNSAFE.putInt(Thread.currentThread(), PROBE, probe);
        return probe;
    }

    /**
     * 处理涉及初始化、调整大小、创建新单元格和/或争用的更新情况。
     * 请参阅上文了解解释。此方法存在乐观重试常见的非模块化问题，
     * 依赖于重新检查的读取集。
     *
     * @param x 值
     * @param fn 更新函数，或添加时为null。（此约定避免了在LongAdder中额外的字段或函数）
     * @param wasUncontended false 如果CAS在调用前失败
     */
    final void longAccumulate(long x, LongBinaryOperator fn,
                              boolean wasUncontended) {
        int h;
        if ((h = getProbe()) == 0) {
            // 强制初始化
            ThreadLocalRandom.current();
            h = getProbe();
            wasUncontended = true;
        }
        // 如果最后一个插槽不为空，则为true
        boolean collide = false;
        for (;;) {
            Cell[] as; Cell a; int n; long v;
            if ((as = cells) != null && (n = as.length) > 0) {
                if ((a = as[(n - 1) & h]) == null) { // 最后一个插槽为null时
                    if (cellsBusy == 0) {       // 尝试链接新的Cell
                        Cell r = new Cell(x);   // 乐观地创造新的Cell
                        if (cellsBusy == 0 && casCellsBusy()) { //执行CAS操作，用于加锁，进入代表加锁成功
                            boolean created = false;
                            try {               // 重新检查上锁情况
                                Cell[] rs; int m, j;
                                if ((rs = cells) != null && (m = rs.length) > 0 && rs[j = (m - 1) & h] == null) { // 确保和上锁前的场景保持一致
                                    rs[j] = r; // 将新建的Cell添加到末尾
                                    created = true; // 更新创建标识
                                }
                            } finally {
                                cellsBusy = 0; // 释放锁
                            }
                            if (created) // 跳出
                                break;
                            continue;           // 插槽现在非空
                        }
                    }
                    collide = false;
                }
                else if (!wasUncontended)       // CAS已知失败
                    wasUncontended = true;      // 重新哈希后继续
                else if (a.cas(v = a.value, ((fn == null) ? v + x : fn.applyAsLong(v, x)))) // 对当前Cell执行CAS操作
                    break;
                else if (n >= NCPU || cells != as)
                    collide = false;            // 达到最大尺寸或陈旧
                else if (!collide)
                    collide = true;
                else if (cellsBusy == 0 && casCellsBusy()) { // CAS操作，加锁
                    try {
                        if (cells == as) {      // 扩展表，除非过时
                            Cell[] rs = new Cell[n << 1]; //扩展为原来2倍
                            for (int i = 0; i < n; ++i) //
                                rs[i] = as[i];
                            cells = rs;
                        }
                    } finally {
                        cellsBusy = 0; // 释放锁
                    }
                    collide = false;
                    continue;                   // Retry with expanded table
                }
                h = advanceProbe(h);
            }
            else if (cellsBusy == 0 && cells == as && casCellsBusy()) { // 加锁
                boolean init = false;
                try {                           // 初始化表
                    if (cells == as) {
                        Cell[] rs = new Cell[2];
                        rs[h & 1] = new Cell(x);
                        cells = rs;
                        init = true;
                    }
                } finally {
                    cellsBusy = 0;
                }
                if (init)
                    break;
            }
            else if (casBase(v = base, ((fn == null) ? v + x :
                                        fn.applyAsLong(v, x))))
                break;                          // 回退到使用base
        }
    }

    /**
     * Same as longAccumulate, but injecting long/double conversions
     * in too many places to sensibly merge with long version, given
     * the low-overhead requirements of this class. So must instead be
     * maintained by copy/paste/adapt.
     */
    final void doubleAccumulate(double x, DoubleBinaryOperator fn,
                                boolean wasUncontended) {
        int h;
        if ((h = getProbe()) == 0) {
            ThreadLocalRandom.current(); // force initialization
            h = getProbe();
            wasUncontended = true;
        }
        boolean collide = false;                // True if last slot nonempty
        for (;;) {
            Cell[] as; Cell a; int n; long v;
            if ((as = cells) != null && (n = as.length) > 0) {
                if ((a = as[(n - 1) & h]) == null) {
                    if (cellsBusy == 0) {       // Try to attach new Cell
                        Cell r = new Cell(Double.doubleToRawLongBits(x));
                        if (cellsBusy == 0 && casCellsBusy()) {
                            boolean created = false;
                            try {               // Recheck under lock
                                Cell[] rs; int m, j;
                                if ((rs = cells) != null &&
                                    (m = rs.length) > 0 &&
                                    rs[j = (m - 1) & h] == null) {
                                    rs[j] = r;
                                    created = true;
                                }
                            } finally {
                                cellsBusy = 0;
                            }
                            if (created)
                                break;
                            continue;           // Slot is now non-empty
                        }
                    }
                    collide = false;
                }
                else if (!wasUncontended)       // CAS already known to fail
                    wasUncontended = true;      // Continue after rehash
                else if (a.cas(v = a.value,
                               ((fn == null) ?
                                Double.doubleToRawLongBits
                                (Double.longBitsToDouble(v) + x) :
                                Double.doubleToRawLongBits
                                (fn.applyAsDouble
                                 (Double.longBitsToDouble(v), x)))))
                    break;
                else if (n >= NCPU || cells != as)
                    collide = false;            // At max size or stale
                else if (!collide)
                    collide = true;
                else if (cellsBusy == 0 && casCellsBusy()) {
                    try {
                        if (cells == as) {      // Expand table unless stale
                            Cell[] rs = new Cell[n << 1];
                            for (int i = 0; i < n; ++i)
                                rs[i] = as[i];
                            cells = rs;
                        }
                    } finally {
                        cellsBusy = 0;
                    }
                    collide = false;
                    continue;                   // Retry with expanded table
                }
                h = advanceProbe(h);
            }
            else if (cellsBusy == 0 && cells == as && casCellsBusy()) {
                boolean init = false;
                try {                           // Initialize table
                    if (cells == as) {
                        Cell[] rs = new Cell[2];
                        rs[h & 1] = new Cell(Double.doubleToRawLongBits(x));
                        cells = rs;
                        init = true;
                    }
                } finally {
                    cellsBusy = 0;
                }
                if (init)
                    break;
            }
            else if (casBase(v = base,
                             ((fn == null) ?
                              Double.doubleToRawLongBits
                              (Double.longBitsToDouble(v) + x) :
                              Double.doubleToRawLongBits
                              (fn.applyAsDouble
                               (Double.longBitsToDouble(v), x)))))
                break;                          // Fall back on using base
        }
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long BASE;
    private static final long CELLSBUSY;
    private static final long PROBE;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> sk = Striped64.class;
            BASE = UNSAFE.objectFieldOffset
                (sk.getDeclaredField("base"));
            CELLSBUSY = UNSAFE.objectFieldOffset
                (sk.getDeclaredField("cellsBusy"));
            Class<?> tk = Thread.class;
            PROBE = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("threadLocalRandomProbe"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}
