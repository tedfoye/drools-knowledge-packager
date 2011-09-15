package org.fiirs.drools.knowledge.packager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.drools.builder.ResourceType;
import org.drools.compiler.PackageBuilder;
import org.drools.core.util.DroolsStreamUtils;
import org.drools.io.ResourceFactory;

/**
 * Goal which compiles Drools knowledge to a PKG file.  A Drools PKG is created from Drools Knowledge resource files
 * found in a supplied JAR file. 
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
	 * Location and name of the Drools Knowledge JAR file.  This JAR file contains Knowledge resources
	 * for one Drools Package.
	 * 
	 * @parameter expression="${drools.knowledge-jar}"
	 * @required
	 */
	private File knowledgeJar;

	/**
	 * List of Knowledge resource file names, with extensions, to exclude from the Knowledge Package.
	 * </br>e.g.
	 * <pre>
	 * {@code
	 * <exclusions>
	 *     <exclude>myflow-1.bpmn</exclude>
	 *     <exclude>myflow-2.bpmn</exclude>
	 * </exclusions>
	 * }      
	 * </pre>
	 * @parameter
	 */
	private List<String> exclusions;

	/**
	 * The name of the Drools Package.
	 * @parameter expression="${drools.package-name}"
	 * @required
	 */
	private String droolsPackageName;
	
	/**
	 * The path name where the Drools Package file will be created.
	 * @parameter expression="${drools.package-file-path}"
	 * @required
	 */
	private String droolsPackageFilePath;
	
	/**
	 * The file name of the Drools Package file.
	 * @parameter expression="${drools.package-file-name}"
	 * @required
	 */
	private String droolsPackageFileName;
		
	/**
	 * @{inherited}
	 */
	public void execute() throws MojoExecutionException {
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs();
		}
		if(knowledgeJar.exists()) {
			try {
				JarFile jarFile = new JarFile(knowledgeJar.getCanonicalPath());
				List<JarEntry> entries = loadEntries(jarFile);
				createDroolsPackage(jarFile, entries);
			} catch (IOException e) {
				Log log = getLog();
				log.error(e.getMessage());
			}
		}
	}
	
	private List<JarEntry> loadEntries(JarFile jarFile) throws IOException {
		List<JarEntry> entryList = new ArrayList<JarEntry>();
		entryList.addAll(loadEntriesByType(jarFile, ".package", null));
		entryList.addAll(loadEntriesByType(jarFile, ".function", null));
		entryList.addAll(loadEntriesByType(jarFile, ".model.drl", null));
		entryList.addAll(loadEntriesByType(jarFile, ".drl", ".model.drl"));
		entryList.addAll(loadEntriesByType(jarFile, ".bpmn", null));		
		return entryList;
	}
			
	private List<JarEntry> loadEntriesByType(JarFile jarFile, String ext, String excludeExt) {
		List<JarEntry> entryList = new ArrayList<JarEntry>();		
		Enumeration<JarEntry> entries = jarFile.entries();
		while(entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			String name = entry.getName();
			
			// Skip .guvnorinfo directories
			if(name.contains(".guvnorinfo")) {
				continue;
			}
			
			// Skip entries that end with "excludeExt"
			if(null != excludeExt && name.contains(excludeExt)) {
				continue;
			}
			
			// Skip files in the exclusions list
			if(exclusions != null) {
				boolean exclusionFound = false;
				for(String exclusion : exclusions) {
					if(name.endsWith(exclusion)) {
						exclusionFound = true;
						break;
					}
				}
				if(exclusionFound) {
					continue;
				}
			}
			
			// Add the Jar Entry to the list
			if(name.endsWith(ext)) {
				entryList.add(entry);
			}
		}
		return entryList;
	}
	
	private void createDroolsPackage(JarFile jarFile, List<JarEntry> entries) throws IOException {
    	PackageBuilder pbuilder = new PackageBuilder();
    	pbuilder.getPackageBuilderConfiguration().setDefaultPackageName(droolsPackageName);
    	
    	for(JarEntry entry : entries) {
    		ResourceType resourceType = ResourceType.DRL;
    		if(entry.getName().endsWith(".bpmn")) {
    			resourceType = ResourceType.BPMN2;
    		}
    		String s = readEntry(jarFile, entry);
    		pbuilder.addKnowledgeResource(ResourceFactory.newByteArrayResource(s.getBytes()), resourceType, null);
    	}
    	
    	org.drools.rule.Package pkg = pbuilder.getPackage();

    	File file = new File(droolsPackageFilePath);
    	file.mkdirs();
    	String resource = droolsPackageFilePath;
    	if(!droolsPackageFilePath.endsWith(File.separator)) {
    		resource += File.separator;
    	}
    	file = new File(resource + droolsPackageFileName);
		FileOutputStream fos = new FileOutputStream(file);
		DroolsStreamUtils.streamOut(fos, pkg);
		fos.flush();    		
		fos.close();
	}
	
	private String readEntry(JarFile jarFile, JarEntry entry) throws IOException  {
		InputStream is = jarFile.getInputStream(entry);
		byte[] buffer = new byte[2048];
		StringBuffer sb = new StringBuffer();
		int len = is.read(buffer);
		while(len > -1) {
			sb.append(new String(buffer, 0, len));
			len = is.read(buffer);
		}
		return sb.toString();
	}
}
