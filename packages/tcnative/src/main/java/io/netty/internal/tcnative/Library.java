/*
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.netty.internal.tcnative;

import java.io.File;

public final class Library {

    /* Default library names */
    private static final String [] NAMES = {
        "netty_tcnative",
        "libnetty_tcnative"
    };

    private static final String PROVIDED = "provided";

    /*
     * A handle to the unique Library singleton instance.
     */
    private static Library _instance = null;

    static {
        // Preload all classes that will be used in the OnLoad(...) function of JNI to eliminate the possiblity of a
        // class-loader deadlock. This is a workaround for https://github.com/netty/netty/issues/11209.

        // This needs to match all the classes that are loaded via NETTY_JNI_UTIL_LOAD_CLASS or looked up via
        // NETTY_JNI_UTIL_FIND_CLASS.
        tryLoadClasses(ClassLoader.getSystemClassLoader(),
                // error
                Exception.class, NullPointerException.class, IllegalArgumentException.class, OutOfMemoryError.class,

                // jnilib
                String.class, byte[].class,

                // sslcontext
                SSLTask.class, CertificateCallbackTask.class, CertificateCallback.class, SSLPrivateKeyMethodTask.class,
                SSLPrivateKeyMethodSignTask.class, SSLPrivateKeyMethodDecryptTask.class
                );
    }

    /**
     * Preload the given classes and so ensure the {@link ClassLoader} has these loaded after this method call.
     *
     * @param classLoader   the {@link ClassLoader}
     * @param classes    the classes to load.
     */
    private static void tryLoadClasses(ClassLoader classLoader, Class<?>... classes) {
        for (Class<?> clazz: classes) {
            tryLoadClass(classLoader, clazz.getName());
        }
    }

    private static void tryLoadClass(ClassLoader classLoader, String className) {
        try {
            // Load the class and also ensure we init it which means its linked etc.
            Class.forName(className, true, classLoader);
        } catch (ClassNotFoundException ignore) {
            // Ignore
        } catch (SecurityException ignore) {
            // Ignore
        }
    }

    private Library() throws Exception {
        boolean loaded = false;
        String path = System.getProperty("java.library.path");
        String [] paths = path.split(File.pathSeparator);
        StringBuilder err = new StringBuilder();
        for (int i = 0; i < NAMES.length; i++) {
            try {
                loadLibrary(NAMES[i]);
                loaded = true;
            } catch (ThreadDeath t) {
                throw t;
            } catch (VirtualMachineError t) {
                throw t;
            } catch (Throwable t) {
                String name = System.mapLibraryName(NAMES[i]);
                for (int j = 0; j < paths.length; j++) {
                    File fd = new File(paths[j] , name);
                    if (fd.exists()) {
                        // File exists but failed to load
                        throw new RuntimeException(t);
                    }
                }
                if (i > 0) {
                    err.append(", ");
                }
                err.append(t.getMessage());
            }
            if (loaded) {
                break;
            }
        }
        if (!loaded) {
            throw new UnsatisfiedLinkError(err.toString());
        }
    }

    private Library(String libraryName) {
        if (!PROVIDED.equals(libraryName)) {
            loadLibrary(libraryName);
        }
    }

    private static void loadLibrary(String libraryName) {
        System.loadLibrary(calculatePackagePrefix().replace('.', '_') + libraryName);
    }

    /**
     * The shading prefix added to this class's full name.
     *
     * @throws UnsatisfiedLinkError if the shader used something other than a prefix
     */
    private static String calculatePackagePrefix() {
        String maybeShaded = Library.class.getName();
        // Use ! instead of . to avoid shading utilities from modifying the string
        String expected = "io!netty!internal!tcnative!Library".replace('!', '.');
        if (!maybeShaded.endsWith(expected)) {
            throw new UnsatisfiedLinkError(String.format(
                    "Could not find prefix added to %s to get %s. When shading, only adding a "
                            + "package prefix is supported", expected, maybeShaded));
        }
        return maybeShaded.substring(0, maybeShaded.length() - expected.length());
    }

    /* create global TCN's APR pool
     * This has to be the first call to TCN library.
     */
    private static native boolean initialize0();

    private static native boolean aprHasThreads();

    private static native int aprMajorVersion();

    /* APR_VERSION_STRING */
    private static native String aprVersionString();

    /**
     * Calls {@link #initialize(String, String)} with {@code "provided"} and {@code null}.
     *
     * @return {@code true} if initialization was successful
     * @throws Exception if an error happens during initialization
     */
    public static boolean initialize() throws Exception {
        return initialize(PROVIDED, null);
    }

    /**
     * Setup native library. This is the first method that must be called!
     *
     * @param libraryName the name of the library to load
     * @param engine Support for external a Crypto Device ("engine"), usually
     * @return {@code true} if initialization was successful
     * @throws Exception if an error happens during initialization
     */
    public static boolean initialize(String libraryName, String engine) throws Exception {
        if (_instance == null) {
            _instance = libraryName == null ? new Library() : new Library(libraryName);

            if (aprMajorVersion() < 1) {
                throw new UnsatisfiedLinkError("Unsupported APR Version (" +
                                               aprVersionString() + ")");
            }

            if (!aprHasThreads()) {
                throw new UnsatisfiedLinkError("Missing APR_HAS_THREADS");
            }
        }
        return initialize0() && SSL.initialize(engine) == 0;
    }
}
