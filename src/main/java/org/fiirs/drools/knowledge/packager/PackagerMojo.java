package org.fiirs.drools.knowledge.packager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.drools.builder.ResourceType;
import org.drools.compiler.DroolsError;
import org.drools.compiler.PackageBuilder;
import org.drools.compiler.PackageBuilderConfiguration;
import org.drools.compiler.PackageBuilderErrors;
import org.drools.core.util.DroolsStreamUtils;
import org.drools.io.Resource;
import org.drools.io.ResourceFactory;

/**
 * Goal which compiles Drools knowledge to a PKG file. A Drools PKG is created
 * from Drools Knowledge resource files found in a supplied JAR file.
 * 
 * @goal package-knowledge
 * @phase package
 * @requiresDependencyResolution runtime
 */
public class PackagerMojo extends AbstractMojo {
	/**
	 * Location of the file.
	 * 
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */
	private File outputDirectory;

	/**
	 * List of JAR files that contain Drools Knowledge resources. All compiles
	 * resources from all JARS will be added to one Drools Package.
	 * 
	 * @parameter expression="${drools.knowledge-jars}"
	 * @required
	 */
	private List<String> knowledgeJars;

	/**
	 * List of pattern Strings used to match resources in
	 * <code>knowledgejars</code>. Two wildcards are supported:
	 * <ul>
	 * <li>** - will match one or more characters in a path String</li>
	 * <li>* - will match one or more characters in a file name or file
	 * extension</li>
	 * </ul>
	 * For example:
	 * 
	 * <pre>
	 *   org/&#42&#42/company/*.drl
	 * </pre>
	 * 
	 * will match
	 * 
	 * <pre>
	 * 	 org/name/company/common.drl
	 * 	 org/name/company/rules.drl
	 *   org/name/test/company/moresules.drl
	 * </pre>
	 * @parameter expression="${drools.pattern-strings}"
	 * @required
	 */
	private List<String> patternStrings;

	/**
	 * The name of the Drools Package.
	 * 
	 * @parameter expression="${drools.package-name}"
	 * @required
	 */
	private String droolsPackageName;

	/**
	 * The path name where the Drools Package file will be created.
	 * 
	 * @parameter expression="${drools.package-file-path}"
	 * @required
	 */
	private String droolsPackageFilePath;

	/**
	 * The file name of the Drools Package file.
	 * 
	 * @parameter expression="${drools.package-file-name}"
	 * @required
	 */
	private String droolsPackageFileName;

	/**
	 * If true the Java code generated by the Drools compiler will be dumped to
	 * #droolsClassDumpDir.
	 * 
	 * @parameter expression="${drools.class.dump}"
	 */
	private boolean droolsClassDump;

	/**
	 * If #droolsClassDump is true the code generated by the Drools compiler
	 * will be dumped to this directory.
	 * 
	 * @parameter expression="${drools.class.dump.dir}"
	 */
	private String droolsClassDumpDir;

	private boolean hasErrors;
	private PackageBuilderErrors packageBuilderErrors;

	/**
	 * @{inherited
	 */
	public void execute() throws MojoExecutionException {
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs();
		}

		// Build the Knowledge entries
		KnowledgeEntryBuilder keb = new KnowledgeEntryBuilder();
		for (String knowledgeJar : knowledgeJars) {
			keb.addJar(knowledgeJar);
		}
		for (String patternString : patternStrings) {
			keb.addPatternStr(patternString);
		}
		KnowledgeEntries ke = null;
		try {
			ke = keb.build();
		} catch (IOException e) {
			throw new MojoExecutionException(e.toString());
		}

		// Process KnowledgeEntries
		if (ke != null) {
			if (ke.getErrors().size() > 0) {
				StringBuilder sb = new StringBuilder();
				for (String s : ke.getErrors()) {
					sb.append(s).append("\n");
				}
				throw new MojoExecutionException(sb.toString());
			}

			PackageBuilderConfiguration pbc = createPackageBuilderConfiguration(ke);
			PackageBuilder pb = createPackageBuilder(ke, pbc);
			persistPackage(pb);
		}

		if (hasErrors) {
			StringBuilder sb = new StringBuilder();
			for (DroolsError droolsError : packageBuilderErrors.getErrors()) {
				sb.append(droolsError.getMessage());
				sb.append("\n");
			}
			throw new MojoExecutionException(sb.toString());
		}
	}

	private PackageBuilder createPackageBuilder(KnowledgeEntries ke,
			PackageBuilderConfiguration pbc) throws MojoExecutionException {
		PackageBuilder pb = new PackageBuilder(pbc);
		pb.getPackageBuilderConfiguration().setDefaultPackageName(
				droolsPackageName);

		// Add all entries to the Package Builder, skip
		// drools.packagebuilder.conf
		try {
			for (Entry<String, List<String>> mapEntry : ke.getEntries()
					.entrySet()) {
				JarFile jarFile = new JarFile(mapEntry.getKey());
				getLog().info("JAR file: " + mapEntry.getKey());
				
				for (String name : mapEntry.getValue()) {
					if (name.endsWith("drools.packagebuilder.conf")) {
						continue;
					}
					getLog().info("    processing " + name);
					
					ResourceType resourceType = ResourceType.DRL;
					if (name.endsWith(".bpmn")) {
						resourceType = ResourceType.BPMN2;
					}
					JarEntry jarEntry = jarFile.getJarEntry(name);
					String knowledge = readEntryAsString(jarFile, jarEntry);

					Resource res = ResourceFactory
							.newByteArrayResource(knowledge.getBytes());
					pb.addKnowledgeResource(res, resourceType, null);
				}
				jarFile.close();
			}
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage());
		}
		return pb;
	}

	private void persistPackage(PackageBuilder pb)
			throws MojoExecutionException {
		// Get the package and check for errors
		org.drools.rule.Package pkg = pb.getPackage();
		hasErrors = pb.hasErrors();
		if (hasErrors) {
			packageBuilderErrors = pb.getErrors();
		}

		// Persist the package file only if there are no errors
		if (!hasErrors) {
			File file = new File(droolsPackageFilePath);
			file.mkdirs();
			String resource = droolsPackageFilePath;
			if (!droolsPackageFilePath.endsWith(File.separator)) {
				resource += File.separator;
			}
			file = new File(resource + droolsPackageFileName);

			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(file);
				DroolsStreamUtils.streamOut(fos, pkg);
			} catch (IOException e) {
				throw new MojoExecutionException(e.getMessage());
			} finally {
				if (fos != null) {
					try {
						fos.flush();
						fos.close();
					} catch (IOException e) {
						throw new MojoExecutionException(e.getMessage());
					}
				}
			}

		}
	}

	// Get properties from "drools.packagebuilder.conf"
	private PackageBuilderConfiguration createPackageBuilderConfiguration(
			KnowledgeEntries kEntries) throws MojoExecutionException {
		PackageBuilderConfiguration pbc = new PackageBuilderConfiguration();

		String pbcJarName = null;
		String pbcFileName = null;
		for (Entry<String, List<String>> mapEntry : kEntries.getEntries()
				.entrySet()) {
			for (String s : mapEntry.getValue()) {
				if (s.endsWith("drools.packagebuilder.conf")) {
					pbcJarName = mapEntry.getKey();
					pbcFileName = s;
					break;
				}
			}
			if (null != pbcJarName && null != pbcFileName) {
				break;
			}
		}

		try {
			if (null != pbcJarName && null != pbcFileName) {
				JarFile jarFile = new JarFile(pbcJarName);
				ZipEntry ze = jarFile.getEntry(pbcFileName);
				if (ze != null) {
					InputStream is = jarFile.getInputStream(ze);
					Properties props = new Properties();
					props.load(is);
					for (Map.Entry<Object, Object> propEntry : props.entrySet()) {
						String key = (String) propEntry.getKey();
						String value = (String) propEntry.getValue();
						pbc.setProperty(key, value);
					}
				}
			}
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage());
		}
		configureClassDump(pbc);
		return pbc;
	}

	// Dump the generated code?
	private void configureClassDump(PackageBuilderConfiguration pbc) {
		if (droolsClassDump && null != droolsClassDumpDir
				&& droolsClassDumpDir.length() > 0) {
			File file = new File(droolsClassDumpDir);
			file.mkdirs();
			pbc.setProperty("drools.dump.dir", droolsClassDumpDir);
		}
	}

	private String readEntryAsString(JarFile jarFile, JarEntry entry)
			throws IOException {
		InputStream is = jarFile.getInputStream(entry);
		byte[] buffer = new byte[2048];
		StringBuffer sb = new StringBuffer();
		int len = is.read(buffer);
		while (len > -1) {
			sb.append(new String(buffer, 0, len));
			len = is.read(buffer);
		}
		return sb.toString();
	}
}
