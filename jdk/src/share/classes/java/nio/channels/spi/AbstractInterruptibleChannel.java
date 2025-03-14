/*
 * Copyright (c) 2000, 2010, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.channels.spi;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import sun.nio.ch.Interruptible;


/**
 * 用于{@link java.nio.channels.InterruptibleChannel}的基本实现类。
 *
 * <p>该类封装了实现channel的异步关闭和中断所需的低级机制。具体的channel类
 * 必须在调用可能无限期阻塞的I/O操作之前和之后分别调用{@link #begin()}
 * 和{@link #end(boolean)}方法。为了确保{@link #end(boolean)}方法
 * 总是被调用，这些方法应该用在try-finally块中。
 *
 * <pre>{@code
 *  try {
 *      begin();
 *      completed = ...; // 执行阻塞I/O操作
 *      return ...; // 返回结果
 *  } finally {
 *      end(completed);
 *  }
 * }</pre>
 *
 * <p>{@link #end(boolean)}方法中的{@code completed}参数告知方法
 * I/O操作是否真的完成，也就是说，它是否产生了对调用者可见的效果。例如：在
 * 读取字节的操作中，当且仅当一些字节实际被传输到调用者的目标缓冲区中时，
 * 此参数才应该为true。
 *
 * <p>具体的channel类还必须实现{@link #implCloseChannel()}方法，以便
 * 如果在另一个线程在channel上的本机I/O操作中被阻塞时调用该方法，则该操作将
 * 立刻返回，要么抛出异常，要么正常返回。如果线程被阻塞或其阻塞的channel被
 * 异步关闭，该channel的{@link #end(boolean)}将抛出相应的异常。
 *
 * <p>该类执行实现{@link java.nio.channels.Channel}规范所需的同步。
 * {@link #implCloseChannel()}方法的实现无需与可能试图关闭channel的
 * 其他线程同步。
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */
public abstract class AbstractInterruptibleChannel implements Channel, InterruptibleChannel {

    private final Object closeLock = new Object();
    private volatile boolean open = true;

    /**
     * 初始化类的实例
     */
    protected AbstractInterruptibleChannel() { }

    /**
     * 关闭该channel。
     *
     * <p>如果channel已经关闭，该方法将立即返回。否则会标记channel为已关闭，
     * 然后调用{@link #implCloseChannel()}方法来完成关闭操作。
     *
     * @throws  IOException 如果发生I/O异常
     */
    public final void close() throws IOException {
        synchronized (closeLock) {
            if (!open)
                return;
            open = false;
            implCloseChannel();
        }
    }

    /**
     * 关闭该channel。
     *
     * <p>该方法被{@link #end(boolean)}方法调用来执行channel关闭的实际工作。
     * 仅当channel还未被关闭时才会调用该方法，该方法不会被调用多次。
     *
     * <p>此方法的实现必须安排在该channel上的I/O操作中被阻塞的任何其他线程
     * 立即返回，要么抛出异常，要么正常返回。
     *
     * @throws  IOException 如果当关闭chanel时发生I/O异常
     */
    protected abstract void implCloseChannel() throws IOException;

    public final boolean isOpen() {
        return open;
    }


    // -- 中断机制 --

    private Interruptible interruptor;
    private volatile Thread interrupted;

    /**
     * 标记可能会导致无限期阻塞的I/O操作正在开始。
     *
     * <p>该方法应该和{@link #end(boolean)}成对调用，使用try-finally块，
     * 就像类描述中的实例，以实现对channel的异步关闭和打断。
     */
    protected final void begin() {
        if (interruptor == null) {
            interruptor = new Interruptible() {
                    public void interrupt(Thread target) {
                        synchronized (closeLock) {
                            if (!open)
                                return;
                            open = false;
                            interrupted = target;
                            try {
                                AbstractInterruptibleChannel.this.implCloseChannel();
                            } catch (IOException x) { }
                        }
                    }};
        }
        blockedOn(interruptor);
        Thread me = Thread.currentThread();
        if (me.isInterrupted())
            interruptor.interrupt(me);
    }

    /**
     * 标记可能会导致无限期阻塞的I/O操作已结束。
     *
     * <p>该方法应该和{@link #begin()}成对调用，使用try-finally块，
     * 就像类描述中的实例，以实现对channel的异步关闭和打断。
     *
     * @param  completed 仅当I/O操作成功时，并产生了对调用者可见的效果，返回true
     *
     * @throws  AsynchronousCloseException channel已被异步关闭
     *
     * @throws  ClosedByInterruptException 阻塞在I/O操作上的线程已被打断
     */
    protected final void end(boolean completed)
        throws AsynchronousCloseException
    {
        blockedOn(null);
        Thread interrupted = this.interrupted;
        if (interrupted != null && interrupted == Thread.currentThread()) {
            interrupted = null;
            throw new ClosedByInterruptException();
        }
        if (!completed && !open)
            throw new AsynchronousCloseException();
    }


    // -- sun.misc.SharedSecrets --
    static void blockedOn(Interruptible intr) {         // package-private
        sun.misc.SharedSecrets.getJavaLangAccess().blockedOn(Thread.currentThread(),
                                                             intr);
    }
}
