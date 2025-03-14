/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

import java.io.Closeable;
import java.lang.Throwable;

/**
 * 一个对象可能一直持有资源（例如文件或socket句柄）直到它被关闭。声明在try-with-resources
 * 块中的对象，在退出该块时会自动调用{@link AutoCloseable#close()}方法。这种构造可确保
 * 及时释放，避免可能发生的资源耗尽异常和错误。
 *
 * <p>对于实现了{@link AutoCloseable}的基类，即使其所有子类或实例都不会持有可释放的资源的这种
 * 情况是可能且常见的。对于必须完全通用地操作的代码，或者当已知{@link AutoCloseable}实例需要
 * 释放资源时，建议使用try-with-resources。然而，当使用支持基于I/O或非基于I/O形式的
 * {@link java.util.stream.Stream}等工具时，在使用非基于I/O的形式时，通常不需要
 * try-with-resources块。
 *
 * @author Josh Bloch
 * @since 1.7
 */
public interface AutoCloseable {
    /**
     * 关闭该资源，放弃任何底层资源。对于由try-with-resources语句管理的对象，
     * 该方法会被自动调用。
     *
     * <p>虽然接口方法被声明抛出{@link java.lang.Exception}，但是强烈鼓励
     * 实现者声明{@code close}方法的具体实现以抛出更具体的异常，或者如果close
     * 操作不能失败，则根本不抛出任何异常。
     *
     * <p>实现者必须注意关闭操作可能失败。强烈建议在抛出异常前，放弃底层资源，并
     * 在内部标记该资源已被关闭。{@code close}方法不太可能被调用多次，因此这可
     * 确保及时释放资源。此外，它还减少了当资源包装或被另一个资源包装时可能出现的
     * 问题。
     *
     * <p>强烈建议该接口的实现者不要让{@code close}方法抛出
     * {@link java.lang.InterruptedException}.此异常与线程的中断状态交互，
     * 如果{@code InterruptedException}被
     * {@link java.lang.Throwable#addSuppressed(java.lang.Throwable)}
     * 所抑制，则可能会发生运行时不当行为。
     *
     * 更通用的来说，如果抑制异常可能会导致问题，则{@code Autoclosable.close}
     * 不应该抛出该异常。
     *
     * <p>请注意，不同于{@link java.io.Closeable#close()}方法，该方法不要求
     * 幂等。换句话说，多次调用该方法可能导致一些可见的副作用，而不像{@code Closeable.close}
     * 方法多次调用没有任何副作用。
     *
     * 然而，强烈建议实现该接口时，确保{@code close}方法是幂等的。
     *
     * @throws Exception 如果资源不能被关闭
     */
    void close() throws Exception;
}
