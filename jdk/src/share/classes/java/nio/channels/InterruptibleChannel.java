/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 */

package java.nio.channels;

import java.io.IOException;
import java.lang.Thread;
import java.nio.channels.Channel;


/**
 * 可以被异步关闭和打断的channel。
 *
 * <p>实现该接口的channel是可异步关闭的：如果线程在一个可打断的channel上的
 * I/O操作中被阻塞，那么另一个线程可以调用channel的{@link #close()}方法。
 * 这会导致被阻塞的线程收到一个{@link java.nio.channels.AsynchronousCloseException}异常。
 *
 * <p>实现该接口的channel同时也是可打断的：如果线程在一个可打断的channel上的
 * I/O操作中被阻塞，那么另一个线程可以调用阻塞线程的{@link java.lang.Thread#interrupt()}
 * 方法，这回导致被阻塞的线程收到一个{@link java.nio.channels.ClosedByInterruptException}
 * 异常，然后将设置被阻塞线程的打断状态。
 *
 * <p>如果线程的阻塞状态已经被设置，然后该线程在channel上调用阻塞I/O操作，
 * 然后该channel将被关闭，并且线程会立即收到{@link java.nio.channels.ClosedByInterruptException}异常，
 * 线程的打断状态将被保留。
 *
 * <p>当且仅当实现此接口时，channel才支持异步关闭和打断。如有必要，可以在运行时
 * 通过{@code instanceof}运算符进行测试。
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */

public interface InterruptibleChannel extends Channel {

    /**
     * 关闭channel。
     *
     * <p>任何在该channel上的I/O操作被阻塞的线程都会收到一个
     * {@link java.nio.channels.AsynchronousCloseException}异常。
     *
     * <p>该方法的行为与{@link java.nio.channels.Channel#close()}
     * 接口指定的行为完全一致。
     *
     * @throws  IOException  如果发生I/O错误
     */
    public void close() throws IOException;

}
