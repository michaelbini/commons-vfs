/*
 * Copyright 2002, 2003,2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.vfs;

import org.apache.commons.logging.Log;

/**
 * This class is to keep the old logging behaviour (for ant-task) and to be able to
 * correctly use commons-logging.<br>
 * I hope i could remove it sometimes.
 * 
 * @author <a href="mailto:imario@apache.org">Mario Ivankovits</a>
 * @version $Revision: 1.3 $ $Date: 2004/05/21 20:54:34 $
 */
public class VfsLog
{
    // static utility class
    private VfsLog()
    {
    }

    /**
     * warning
     */
    public static void warn(Log vfslog, Log commonslog, String message, Throwable t)
    {
        if (vfslog != null)
        {
            vfslog.warn(message, t);
        }
        else if (commonslog != null)
        {
            commonslog.warn(message, t);
        }
    }

    /**
     * warning
     */
    public static void warn(Log vfslog, Log commonslog, String message)
    {
        if (vfslog != null)
        {
            vfslog.warn(message);
        }
        else if (commonslog != null)
        {
            commonslog.warn(message);
        }
    }

    /**
     * debug
     */
    public static void debug(Log vfslog, Log commonslog, String message)
    {
        if (vfslog != null)
        {
            vfslog.debug(message);
        }
        else if (commonslog != null)
        {
            commonslog.debug(message);
        }
    }
}