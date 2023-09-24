/*
 * Copyright 2014-2016 the libsecp256k1 contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bitcoin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import static java.io.File.createTempFile;
import static java.lang.System.getProperty;
import static java.lang.Thread.currentThread;
import static java.util.Locale.ENGLISH;
import static java.util.Objects.requireNonNull;

import java.util.logging.Logger;

/**
 * This class holds the context reference used in native methods
 * to handle ECDSA operations.
 */
public class Secp256k1Context {
	private static final Logger logger = Logger.getLogger(Secp256k1Context.class.getName());

	private static final boolean enabled; // true if the library is loaded
	private static final long context; // ref to pointer to context obj

	static { // static initializer
		boolean isEnabled = true;
		long contextRef = -1;

		final String libToLoad;

		final String arch = getProperty("os.arch");
		System.out.println(arch);
		final boolean arch64 = "x64".equals(arch) || "amd64".equals(arch) || "x86_64".equals(arch);
		final boolean archArm64 = "aarch64".equals(arch);

		final String os = getProperty("os.name");
		System.out.println(os);
		final boolean linux = os.toLowerCase(ENGLISH).startsWith("linux");
		System.out.println(linux);
		final boolean osx = os.startsWith("Mac OS X");
		final boolean windows = os.startsWith("Windows");

		try {
			if (arch64 && linux) {
				libToLoad = extract("coop/rchain/secp256k1-native-linux-x86_64.so");
			} else if (arch64 && osx) {
				libToLoad = extract("coop/rchain/secp256k1-native-osx-x86_64.dylib");
			} else if (archArm64 && linux) {
				// libToLoad = extract("coop/rchain/secp256k1-native-linux-x86_64.so");
				libToLoad = System.getenv("HOME") + "/.firefly/libsecp256k1.so";
			} else if (archArm64 && osx) {
				// libToLoad = extract("coop/firefly/secp256k1-native-osx-arm64.dylib");
				// libToLoad =
				// "/Users/spreston/Downloads/linux-aarch64/libsecp256k1.so";
				libToLoad = System.getenv("HOME") + "/.firefly/libsecp256k1.0.dylib";
			} else if (arch64 && windows) {
				libToLoad = extract("coop/rchain/secp256k1-native-windows-x86_64.dll");
			} else {
				throw new RuntimeException("No secp256k1-native library to extract");
			}
			System.load(libToLoad);
			contextRef = secp256k1_init_context();
		} catch (UnsatisfiedLinkError e) {
			logger.severe("UnsatisfiedLinkError: " + e.toString());
			isEnabled = false;
		} catch (IOException e) {
			logger.severe("IOException: " + e.toString());
			isEnabled = false;
		} catch (NullPointerException e) {
			logger.severe("Null pointer exception: " + e.toString());
			isEnabled = false;
		}
		enabled = isEnabled;
		context = contextRef;
	}

	@SuppressWarnings("PMD.AssignmentInOperand")
	private static String extract(final String name) throws IOException {
		final String suffix = name.substring(name.lastIndexOf('.'));
		final File file;
		try {
			file = createTempFile("secp256k1-native-library-", suffix);
			file.deleteOnExit();
			final ClassLoader cl = currentThread().getContextClassLoader();
			try (InputStream in = cl.getResourceAsStream(name);
					OutputStream out = Files.newOutputStream(file.toPath())) {
				requireNonNull(in, "Classpath resource not found");
				int bytes;
				final byte[] buffer = new byte[4_096];
				while (-1 != (bytes = in.read(buffer))) {
					out.write(buffer, 0, bytes);
				}
			}
			return file.getAbsolutePath();
		} catch (final IOException e) {
			throw new IOException("Failed to extract " + name, e);
		}
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static long getContext() {
		if (!enabled)
			return -1; // sanity check
		return context;
	}

	private static native long secp256k1_init_context();
}
