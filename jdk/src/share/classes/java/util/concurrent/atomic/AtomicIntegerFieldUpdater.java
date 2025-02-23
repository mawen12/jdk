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
import java.util.function.IntUnaryOperator;
import java.util.function.IntBinaryOperator;
import sun.misc.Unsafe;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;

/**
 * 基于反射的实用程序，可对指定类的指定{@code volatile int}字段进行原子更新。
 * 此类设计用于原子数据结构，其中同一个节点的多个字段独立地进行原子更新。
 *
 * <p>该类的{@code compareAndSet}方法的包装弱于其它原子类。因为该类
 * 无法确保字段的所有实用都适合原子访问的目的，所以它只能保证相对于同一
 * 更新程序上{@code compareAndSet}和{@code set}的其他调用的原子性。
 *
 * @since 1.5
 * @author Doug Lea
 * @param <T> The type of the object holding the updatable field
 */
public abstract class AtomicIntegerFieldUpdater<T> {
    /**
     * 创建并返回具有给定字段的对象更新器。该类参数需要检查反射类型和通用类型匹配。
     *
     * @param tclass 持有字段的对象的类
     * @param fieldName 要被更新的字段名
     * @param <U> 类的实例类型
     * @return 字段更新器
     * @throws IllegalArgumentException 字段并非一个volatile 数字类型
     * @throws RuntimeException 如果类不包含字段或类型错误，或者根据Java语言
     * 访问控制，调用者无法访问该字段，则会出现基于嵌套反射的异常。
     */
    @CallerSensitive
    public static <U> AtomicIntegerFieldUpdater<U> newUpdater(Class<U> tclass,
                                                              String fieldName) {
        return new AtomicIntegerFieldUpdaterImpl<U>(tclass, fieldName, Reflection.getCallerClass());
    }

    /**
     * 受保护的不执行任何操作的构造函数，供子类使用
     */
    protected AtomicIntegerFieldUpdater() {
    }

    /**
     * 如果当前值{@code ==}预期值，则以原子方式将此更新程序管理的给定对象的字段设置为
     * 给定的更新值。此方法保证相对于对{@code compareAndSet}和{@code set}的其他
     * 调用是原子的，但不一定相对于字段中的其他更改是原子的。
     *
     * @param obj 有条件地设置其字段的对象
     * @param expect 预期值
     * @param update 新值
     * @return {@code true}如果成功
     * @throws ClassCastException 如果对象不是拥有构造函数中建立的字段的类的实例
     */
    public abstract boolean compareAndSet(T obj, int expect, int update);

    /**
     * 如果当前值{@code ==}预期值，则原子地设置被更新器管理的给定对象的字段为更新值。
     * 此方法保证相对于对{@link compareAndSet}和{@code set}的其他调用是原子的，
     * 但不一定相对于字段中的其他更改是原子的。
     *
     * <p><a href="package-summary.html#weakCompareAndSet">可能会意外失败，并且
     * 不提供顺序保证</a>, 因此很少成为{@code compareAndSet}的合适替代方案。
     *
     * @param obj 有条件地设置其字段的对象
     * @param expect 预期值
     * @param update 新值
     * @return {@code true} 如果成功
     * @throws ClassCastException 如果对象不是拥有构造函数中建立的字段的类的实例
     */
    public abstract boolean weakCompareAndSet(T obj, int expect, int update);

    /**
     * 设置由管理器管理的给定对象的字段为给定值。保证此操作对于{@code compareAndSet}
     * 的后续调用充当易失性存储
     *
     * @param obj 要设置字段的对象
     * @param newValue 新值
     */
    public abstract void set(T obj, int newValue);

    /**
     * 最终设置由更新器管理的给定对象的字段为给定值。
     *
     * @param obj 要设置字段的对象
     * @param newValue 新值
     * @since 1.6
     */
    public abstract void lazySet(T obj, int newValue);

    /**
     * 返回由更新器管理的给定对象字段中保存的当前值
     *
     * @param obj 要获取字段的对象
     * @return 当前值
     */
    public abstract int get(T obj);

    /**
     * 原子地设置由更新器管理的给定对象的字段为给定值，并返回旧值。
     *
     * @param obj 要获取和设置字段的对象
     * @param newValue 新值
     * @return 旧值
     */
    public int getAndSet(T obj, int newValue) {
        int prev;
        do {
            prev = get(obj);
        } while (!compareAndSet(obj, prev, newValue));
        return prev;
    }

    /**
     * 原子地自增由更新器管理的给定对象的字段，并返回旧值。
     *
     * @param obj 要获取和设置字段的对象
     * @return 旧值
     */
    public int getAndIncrement(T obj) {
        int prev, next;
        do {
            prev = get(obj);
            next = prev + 1;
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    /**
     * 原子地自减由更新器管理的给定对象的字段，并返回旧值。
     *
     * @param obj 要获取和设置字段的对象
     * @return 旧值
     */
    public int getAndDecrement(T obj) {
        int prev, next;
        do {
            prev = get(obj);
            next = prev - 1;
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    /**
     * 原子地在由更新器管理的给定对象的字段基础上增加给定值，并返回旧值。
     *
     * @param obj 要获取和设置字段的对象
     * @param delta 要增加的值
     * @return 旧值
     */
    public int getAndAdd(T obj, int delta) {
        int prev, next;
        do {
            prev = get(obj);
            next = prev + delta;
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    /**
     * 原子地自增由更新器管理的给定对象的字段，并返回更新后的值。
     *
     * @param obj 要获取和设置字段的对象
     * @return 更新后的值
     */
    public int incrementAndGet(T obj) {
        int prev, next;
        do {
            prev = get(obj);
            next = prev + 1;
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    /**
     * 原子地自减由更新器管理的给定对象的字段，并返回更新后的值。
     *
     * @param obj 要获取和设置字段的对象
     * @return 更新后的值
     */
    public int decrementAndGet(T obj) {
        int prev, next;
        do {
            prev = get(obj);
            next = prev - 1;
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    /**
     * 原子地在由更新器管理的给定对象的字段基础上增加给定值，并返回更新后的值。
     *
     * @param obj 要获取和设置字段的对象
     * @param delta 要增加的值
     * @return 更新后的值
     */
    public int addAndGet(T obj, int delta) {
        int prev, next;
        do {
            prev = get(obj);
            next = prev + delta;
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    /**
     * Atomically updates the field of the given object managed by this updater
     * with the results of applying the given function, returning the previous
     * value. The function should be side-effect-free, since it may be
     * re-applied when attempted updates fail due to contention among threads.
     *
     * @param obj An object whose field to get and set
     * @param updateFunction a side-effect-free function
     * @return the previous value
     * @since 1.8
     */
    public final int getAndUpdate(T obj, IntUnaryOperator updateFunction) {
        int prev, next;
        do {
            prev = get(obj);
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    /**
     * Atomically updates the field of the given object managed by this updater
     * with the results of applying the given function, returning the updated
     * value. The function should be side-effect-free, since it may be
     * re-applied when attempted updates fail due to contention among threads.
     *
     * @param obj An object whose field to get and set
     * @param updateFunction a side-effect-free function
     * @return the updated value
     * @since 1.8
     */
    public final int updateAndGet(T obj, IntUnaryOperator updateFunction) {
        int prev, next;
        do {
            prev = get(obj);
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    /**
     * Atomically updates the field of the given object managed by this
     * updater with the results of applying the given function to the
     * current and given values, returning the previous value. The
     * function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.  The
     * function is applied with the current value as its first argument,
     * and the given update as the second argument.
     *
     * @param obj An object whose field to get and set
     * @param x the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the previous value
     * @since 1.8
     */
    public final int getAndAccumulate(T obj, int x,
                                      IntBinaryOperator accumulatorFunction) {
        int prev, next;
        do {
            prev = get(obj);
            next = accumulatorFunction.applyAsInt(prev, x);
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    /**
     * Atomically updates the field of the given object managed by this
     * updater with the results of applying the given function to the
     * current and given values, returning the updated value. The
     * function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.  The
     * function is applied with the current value as its first argument,
     * and the given update as the second argument.
     *
     * @param obj An object whose field to get and set
     * @param x the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the updated value
     * @since 1.8
     */
    public final int accumulateAndGet(T obj, int x,
                                      IntBinaryOperator accumulatorFunction) {
        int prev, next;
        do {
            prev = get(obj);
            next = accumulatorFunction.applyAsInt(prev, x);
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    /**
     * Standard hotspot implementation using intrinsics
     */
    private static class AtomicIntegerFieldUpdaterImpl<T>
            extends AtomicIntegerFieldUpdater<T> {
        private static final Unsafe unsafe = Unsafe.getUnsafe();
        private final long offset;
        private final Class<T> tclass;
        private final Class<?> cclass;

        AtomicIntegerFieldUpdaterImpl(final Class<T> tclass,
                                      final String fieldName,
                                      final Class<?> caller) {
            final Field field;
            final int modifiers;
            try {
                field = AccessController.doPrivileged(
                    new PrivilegedExceptionAction<Field>() {
                        public Field run() throws NoSuchFieldException {
                            return tclass.getDeclaredField(fieldName);
                        }
                    });
                modifiers = field.getModifiers();
                sun.reflect.misc.ReflectUtil.ensureMemberAccess(
                    caller, tclass, null, modifiers);
                ClassLoader cl = tclass.getClassLoader();
                ClassLoader ccl = caller.getClassLoader();
                if ((ccl != null) && (ccl != cl) &&
                    ((cl == null) || !isAncestor(cl, ccl))) {
                  sun.reflect.misc.ReflectUtil.checkPackageAccess(tclass);
                }
            } catch (PrivilegedActionException pae) {
                throw new RuntimeException(pae.getException());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            Class<?> fieldt = field.getType();
            if (fieldt != int.class)
                throw new IllegalArgumentException("Must be integer type");

            if (!Modifier.isVolatile(modifiers))
                throw new IllegalArgumentException("Must be volatile type");

            this.cclass = (Modifier.isProtected(modifiers) &&
                           caller != tclass) ? caller : null;
            this.tclass = tclass;
            offset = unsafe.objectFieldOffset(field);
        }

        /**
         * Returns true if the second classloader can be found in the first
         * classloader's delegation chain.
         * Equivalent to the inaccessible: first.isAncestor(second).
         */
        private static boolean isAncestor(ClassLoader first, ClassLoader second) {
            ClassLoader acl = first;
            do {
                acl = acl.getParent();
                if (second == acl) {
                    return true;
                }
            } while (acl != null);
            return false;
        }

        private void fullCheck(T obj) {
            if (!tclass.isInstance(obj))
                throw new ClassCastException();
            if (cclass != null)
                ensureProtectedAccess(obj);
        }

        public boolean compareAndSet(T obj, int expect, int update) {
            if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
            return unsafe.compareAndSwapInt(obj, offset, expect, update);
        }

        public boolean weakCompareAndSet(T obj, int expect, int update) {
            if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
            return unsafe.compareAndSwapInt(obj, offset, expect, update);
        }

        public void set(T obj, int newValue) {
            if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
            unsafe.putIntVolatile(obj, offset, newValue);
        }

        public void lazySet(T obj, int newValue) {
            if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
            unsafe.putOrderedInt(obj, offset, newValue);
        }

        public final int get(T obj) {
            if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
            return unsafe.getIntVolatile(obj, offset);
        }

        public int getAndSet(T obj, int newValue) {
            if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
            return unsafe.getAndSetInt(obj, offset, newValue);
        }

        public int getAndIncrement(T obj) {
            return getAndAdd(obj, 1);
        }

        public int getAndDecrement(T obj) {
            return getAndAdd(obj, -1);
        }

        public int getAndAdd(T obj, int delta) {
            if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
            return unsafe.getAndAddInt(obj, offset, delta);
        }

        public int incrementAndGet(T obj) {
            return getAndAdd(obj, 1) + 1;
        }

        public int decrementAndGet(T obj) {
             return getAndAdd(obj, -1) - 1;
        }

        public int addAndGet(T obj, int delta) {
            return getAndAdd(obj, delta) + delta;
        }

        private void ensureProtectedAccess(T obj) {
            if (cclass.isInstance(obj)) {
                return;
            }
            throw new RuntimeException(
                new IllegalAccessException("Class " +
                    cclass.getName() +
                    " can not access a protected member of class " +
                    tclass.getName() +
                    " using an instance of " +
                    obj.getClass().getName()
                )
            );
        }
    }
}
