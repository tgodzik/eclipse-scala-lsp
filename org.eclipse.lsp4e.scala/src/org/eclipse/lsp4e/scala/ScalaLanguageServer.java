/*******************************************************************************
 * Copyright (c) 2017 Rogue Wave Software Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Michał Niewrzał (Rogue Wave Software Inc.) - initial implementation
 *******************************************************************************/
package org.eclipse.lsp4e.scala;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.lsp4e.LanguageServerPlugin;
import org.eclipse.lsp4e.server.ProcessStreamConnectionProvider;
import org.eclipse.lsp4e.server.StreamConnectionProvider;

import org.osgi.framework.Bundle;

public class ScalaLanguageServer implements StreamConnectionProvider {
	private @Nullable Process mainProcess;

	@Override
	public void start() {
		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.command("java", "-jar", "./coursier", "fetch", "-p", "--ttl", "Inf",
				"org.scalameta:metals_2.12:0.7.0", "-r", "bintray:scalacenter/releases", "-r", "sonatype:public", "-r",
				"sonatype:snapshots", "-p");
		processBuilder.environment().put("COURSIER_NO_TERM", "true");
		StringBuffer buffer = new StringBuffer();
		String metalsClasspath = "";
		try {
			Process process = processBuilder.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				buffer.append(line);
			}
			metalsClasspath = buffer.toString();	

		} catch (IOException e) {
			e.printStackTrace();
		}
		ProcessBuilder processBuilder2 = new ProcessBuilder();
		processBuilder2.command("java", 
				"-Dmetals.input-box=on", "-Dmetals.client=eclipse", 
				"-Xss4m", "-Xms100m", "-classpath", metalsClasspath, "scala.meta.metals.Main");
		try {
			processBuilder2.redirectError(ProcessBuilder.Redirect.INHERIT);
			mainProcess = processBuilder2.start();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
		Process p = mainProcess;
		if (p != null) {
			p.destroy();
		}
	}

	@Override
	public String toString() {
		return "Scala Language Server" + super.toString();
	}

	@Override
	public @Nullable InputStream getInputStream() {
		Process p = mainProcess;
		return p == null ? null : p.getInputStream();
	}

	@Override
	public @Nullable InputStream getErrorStream() {
		Process p = mainProcess;
		return p == null ? null : p.getErrorStream();
	}

	@Override
	public @Nullable OutputStream getOutputStream() {
		Process p = mainProcess;
		return p == null ? null : p.getOutputStream();
	}

}
