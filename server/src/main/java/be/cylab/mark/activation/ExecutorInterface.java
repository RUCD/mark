/*
 * The MIT License
 *
 * Copyright 2019 tibo.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package be.cylab.mark.activation;

import java.util.Map;

/**
 * The actual detection jobs can be executed by any platform that implements
 * this interface. We currently use Apache Ignite.
 * @author tibo
 */
public interface ExecutorInterface {

    /**
     * Run this job.
     * @param job
     */
    void submit(Runnable job);

    /**
     * Stop.
     * @return
     * @throws InterruptedException if the thread was killed while we were
     * stopping.
     */
    boolean shutdown() throws InterruptedException;

    /**
     * Get the status of the backend executor.
     *
     * Typical fields of the Map include:
     * - executor.job.executed
     * - executor.job.running
     * - executor.job.waiting
     * - executor.job.waittime
     * - executor.job.executetime
     * - executor.nodes
     * - executor.cpus
     * - executor.parallelism
     *
     * @return
     */
    Map<String, Object> getStatus();
}
