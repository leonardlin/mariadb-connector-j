package org.mariadb.jdbc.internal.util.scheduler;

/*
MariaDB Client for Java

Copyright (c) 2015 Monty Program Ab.

This library is free software; you can redistribute it and/or modify it under
the terms of the GNU Lesser General Public License as published by the Free
Software Foundation; either version 2.1 of the License, or (at your option)
any later version.

This library is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
for more details.

You should have received a copy of the GNU Lesser General Public License along
with this library; if not, write to Monty Program Ab info@montyprogram.com.

This particular MariaDB Client for Java file is work
derived from a Drizzle-JDBC. Drizzle-JDBC file which is covered by subject to
the following copyright and notice provisions:

Copyright (c) 2009-2011, Marcus Eriksson

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:
Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of the driver nor the names of its contributors may not be
used to endorse or promote products derived from this software without specific
prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS  AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
OF SUCH DAMAGE.
*/

import java.util.concurrent.*;

/**
 * Provider for when ever an internal thread pool is needed.  This can allow library users to
 * override our default pooling behavior with possibly better and faster options.
 */
public class SchedulerServiceProviderHolder {

    /**
     * The default provider will construct a new pool on every request.
     */
    public static SchedulerProvider DEFAULT_PROVIDER = new SchedulerProvider() {
        @Override
        public DynamicSizedSchedulerInterface getScheduler(int minimumThreads) {
            return new DynamicSizedSchedulerImpl(minimumThreads);
        }

        @Override
        public ScheduledThreadPoolExecutor getFixedSizeScheduler(int minimumThreads) {
            return new FixedSizedSchedulerImpl(minimumThreads);
        }

        @Override
        public ScheduledThreadPoolExecutor getTimeoutScheduler() {
            ScheduledThreadPoolExecutor timeoutScheduler = new ScheduledThreadPoolExecutor(1, new MariaDbThreadFactory());
            timeoutScheduler.setRemoveOnCancelPolicy(true);
            return timeoutScheduler;
        }
    };

    private static volatile SchedulerProvider currentProvider = null;

    /**
     * Change the current set scheduler provider.  This provider will be provided in future requests
     * to {@link #getSchedulerProvider()}.
     *
     * @param newProvider New provider to use, or {@code null} to use the default provider
     */
    public static void setSchedulerProvider(SchedulerProvider newProvider) {
        currentProvider = newProvider;
    }

    /**
     * Get the currently set {@link SchedulerProvider} from set invocations via
     * {@link #setSchedulerProvider(SchedulerProvider)}.  If none has been set a default provider
     * will be provided (never a {@code null} result).
     *
     * @return Provider to get scheduler pools from
     */
    public static SchedulerProvider getSchedulerProvider() {
        SchedulerProvider result = currentProvider;
        if (result == null) {
            return DEFAULT_PROVIDER;
        } else {
            return result;
        }
    }

    /**
     * Get a Dinamic sized scheduler directly with the current set provider.
     *
     * @param initialThreadCount Number of threads scheduler is allowed to grow to
     * @return Scheduler capable of providing the needed thread count
     */
    public static DynamicSizedSchedulerInterface getScheduler(int initialThreadCount) {
        return getSchedulerProvider().getScheduler(initialThreadCount);
    }

    /**
     * Get a fixed sized scheduler directly with the current set provider.
     *
     * @param initialThreadCount Number of threads scheduler is allowed to grow to
     * @return Scheduler capable of providing the needed thread count
     */
    public static ScheduledExecutorService getFixedSizeScheduler(int initialThreadCount) {
        return getSchedulerProvider().getFixedSizeScheduler(initialThreadCount);
    }

    /**
     * Get a scheduler to handle timeout.
     *
     * @return Scheduler capable of providing the needed thread count
     */
    public static ScheduledExecutorService getTimeoutScheduler() {
        return getSchedulerProvider().getTimeoutScheduler();
    }

    /**
     * <p>Provider for thread pools which allow scheduling capabilities.  It is expected that the
     * thread pools entire lifecycle (start to stop) is done through the same provider instance.</p>
     */
    public interface SchedulerProvider {
        /**
         * Request to get a scheduler with a minimum number of AVAILABLE threads.
         *
         * @param minimumThreads Minimum number of available threads for the returned scheduler
         * @return A new scheduler that is ready to accept tasks
         */
        public DynamicSizedSchedulerInterface getScheduler(int minimumThreads);

        public ScheduledExecutorService getFixedSizeScheduler(int minimumThreads);

        /**
         * Default Timeout scheduler.
         *
         * This is a one Thread fixed sized scheduler.
         * This specific scheduler is using java 1.7 RemoveOnCancelPolicy, so
         * the task are removed from queue permitting to avoid memory consumption [CONJ-297]
         *
         * @return A new scheduler that is ready to accept tasks
         */
        public ScheduledThreadPoolExecutor getTimeoutScheduler();
    }


}
