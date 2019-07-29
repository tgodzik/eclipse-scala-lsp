package org.eclipse.lsp4e.scala;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.lsp4e.server.StreamConnectionProvider;


public class ScalaLanguageServer implements StreamConnectionProvider {
	private @Nullable Process mainProcess;

	@Override
	public void start() {
		ProcessBuilder processBuilder = new ProcessBuilder();
		URL url = Platform.getBundle(Activator.PLUGIN_ID).getEntry("coursier");
		String coursierFile = "";
		try {
			coursierFile = FileLocator.toFileURL(url).getPath();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		// TODO this should be added to settings
		String metalsVersion = "0.7.0";
		processBuilder.command("java", "-jar", coursierFile, "fetch", "-p", "--ttl", "Inf",
				"org.scalameta:metals_2.12:" + metalsVersion, "-r", "bintray:scalacenter/releases", "-r", "sonatype:public", "-r",
				"sonatype:snapshots", "-p");
		processBuilder.environment().put("COURSIER_NO_TERM", "true");
		StringBuffer buffer = new StringBuffer();
		String metalsClasspath = "";
		processBuilder.redirectErrorStream(true);
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
		processBuilder2.command("java", "-Dmetals.input-box=on", "-Dmetals.client=eclipse", "-Xss4m", "-Xms100m",
				"-classpath", metalsClasspath, "scala.meta.metals.Main");
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
