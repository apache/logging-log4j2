/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.util;

import java.io.Serializable;
import java.text.DecimalFormat;

import org.apache.logging.log4j.util.Strings;

/**
 *
 */
public class Timer implements Serializable
{
    /**
     * Generated serial version ID.
     */
    private static final long serialVersionUID = 1313460139390794926L;
    private final String m_name;              // The timer's name
    private String m_status;            // The timer's status
    private long m_startTime;           // The start time
    private long m_elapsedTime;         // The elapsed time
    private final int m_iterations;
    private static long NANO_PER_SECOND = 1000000000L;
    private static long NANO_PER_MINUTE = NANO_PER_SECOND * 60;
    private static long NANO_PER_HOUR = NANO_PER_MINUTE * 60;


    /**
     * Constructor.
     * @param name the timer name.
     */
    public Timer(final String name)
    {
        this(name, 0);
    }

    /**
     * Constructor.
     *
     * @param name the timer name.
     */
    public Timer(final String name, final int iterations)
    {
        m_name = name;
        m_startTime = 0;
        m_status = "Stopped";
        m_iterations = (iterations > 0) ? iterations : 0;
    }

    /**
     * Start the timer.
     */
    public void start()
    {
        m_startTime = System.nanoTime();
        m_elapsedTime = 0;
        m_status = "Start";
    }

    /**
     * Stop the timer.
     */
    public void stop()
    {
        m_elapsedTime += System.nanoTime() - m_startTime;
        m_startTime = 0;
        m_status = "Stop";
    }

    /**
     * Pause the timer.
     */
    public void pause()
    {
        m_elapsedTime += System.nanoTime() - m_startTime;
        m_startTime = 0;
        m_status = "Pause";
    }

    /**
     * Resume the timer.
     */
    public void resume()
    {
        m_startTime = System.nanoTime();
        m_status = "Resume";
    }

    /**
     * Accessor for the name.
     * @return the timer's name.
     */
    public String getName()
    {
        return m_name;
    }

    /**
     * Access the elapsed time.
     *
     * @return the elapsed time.
     */
    public long getElapsedTime()
    {
        return m_elapsedTime / 1000000;
    }

    /**
     * Access the elapsed time.
     *
     * @return the elapsed time.
     */
    public long getElapsedNanoTime()
    {
        return m_elapsedTime;
    }

    /**
     * Returns the name of the last operation performed on this timer (Start, Stop, Pause or
     * Resume).
     * @return the string representing the last operation performed.
     */
    public String getStatus()
    {
        return m_status;
    }

    /**
     * Returns the String representation of the timer based upon its current state
     */
    @Override
    public String toString()
    {
        final StringBuilder result = new StringBuilder("Timer ").append(m_name);
        if (m_status.equals("Start"))
        {
            result.append(" started");
        }
        else if (m_status.equals("Pause"))
        {
            result.append(" paused");
        }
        else if (m_status.equals("Resume"))
        {
            result.append(" resumed");
        }
        else if (m_status.equals("Stop"))
        {
            long nanoseconds = m_elapsedTime;
            // Get elapsed hours
            long hours = nanoseconds / NANO_PER_HOUR;
            // Get remaining nanoseconds
            nanoseconds = nanoseconds % NANO_PER_HOUR;
            // Get minutes
            long minutes = nanoseconds / NANO_PER_MINUTE;
            // Get remaining nanoseconds
            nanoseconds = nanoseconds % NANO_PER_MINUTE;
            // Get seconds
            long seconds = nanoseconds / NANO_PER_SECOND;
            // Get remaining nanoseconds
            nanoseconds = nanoseconds % NANO_PER_SECOND;

            String elapsed = Strings.EMPTY;

            if (hours > 0)
            {
                elapsed += hours + " hours ";
            }
            if (minutes > 0 || hours > 0)
            {
                elapsed += minutes + " minutes ";
            }

            DecimalFormat numFormat = null;
            numFormat = new DecimalFormat("#0");
            elapsed += numFormat.format(seconds) + '.';
            numFormat = new DecimalFormat("000000000");
            elapsed += numFormat.format(nanoseconds) + " seconds";
            result.append(" stopped. Elapsed time: ").append(elapsed);
            if (m_iterations > 0)
            {
                nanoseconds = m_elapsedTime / m_iterations;
                // Get elapsed hours
                hours = nanoseconds / NANO_PER_HOUR;
                // Get remaining nanoseconds
                nanoseconds = nanoseconds % NANO_PER_HOUR;
                // Get minutes
                minutes = nanoseconds / NANO_PER_MINUTE;
                // Get remaining nanoseconds
                nanoseconds = nanoseconds % NANO_PER_MINUTE;
                // Get seconds
                seconds = nanoseconds / NANO_PER_SECOND;
                // Get remaining nanoseconds
                nanoseconds = nanoseconds % NANO_PER_SECOND;

                elapsed = Strings.EMPTY;

                if (hours > 0)
                {
                    elapsed += hours + " hours ";
                }
                if (minutes > 0 || hours > 0)
                {
                    elapsed += minutes + " minutes ";
                }

                numFormat = new DecimalFormat("#0");
                elapsed += numFormat.format(seconds) + '.';
                numFormat = new DecimalFormat("000000000");
                elapsed += numFormat.format(nanoseconds) + " seconds";
                result.append(" Average per iteration: ").append(elapsed);
            }
        }
        else
        {
            result.append(' ').append(m_status);
        }
        return result.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Timer)) {
            return false;
        }

        final Timer timer = (Timer) o;

        if (m_elapsedTime != timer.m_elapsedTime) {
            return false;
        }
        if (m_startTime != timer.m_startTime) {
            return false;
        }
        if (m_name != null ? !m_name.equals(timer.m_name) : timer.m_name != null) {
            return false;
        }
        if (m_status != null ? !m_status.equals(timer.m_status) : timer.m_status != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (m_name != null ? m_name.hashCode() : 0);
        result = 29 * result + (m_status != null ? m_status.hashCode() : 0);
        result = 29 * result + (int) (m_startTime ^ (m_startTime >>> 32));
        result = 29 * result + (int) (m_elapsedTime ^ (m_elapsedTime >>> 32));
        return result;
    }

}
