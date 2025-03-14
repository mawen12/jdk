/*
 * Copyright (c) 2000, 2003, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.Closeable;


/**
 * I/O操作的纽带。
 *
 * <p>一个channel代表与实体的已打开的连接，例如硬件设备、文件、网络socket，或是能够执行
 * 一个或多个不同的I/O操作的程序组件，例如读和写。
 *
 * <p>一个channel可以打开或关闭。一旦创建后便已打开，一旦关闭就是已关闭。一旦关闭后，任何
 * 尝试调用其I/O操作都将导致抛出{@link java.nio.channels.ClosedChannelException}。
 * 可以通过调用其{@link #isOpen()}方法来测试channel是否打开。
 *
 * <p>一般而言，通道旨在保证多线程访问的安全，如扩展和实现此接口的接口和类的规范中所述。
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */

public interface Channel extends Closeable {

    /**
     * 测试channel是否打开
     *
     * @return true 代表已打开
     */
    public boolean isOpen();

    /**
     * 关闭该channel。
     *
     * <p>在关闭后，任何尝试调用其I/O操作都会导致抛出{@link ClosedChannelException}异常。
     *
     * <p>在关闭后，再次调用该方法不会产生任何异常。
     *
     * <p>该方法可以在任何时候被调用。如果其他线程已经调用它了，但是另外一个线程也调用该方法时会阻塞，
     * 直到第一个调用已完成，后续调用不会产生任何影响。
     *
     * @throws  IOException  如果发生I/O异常
     */
    public void close() throws IOException;

}
