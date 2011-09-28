package org.fiirs.drools.knowledge.packager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KnowledgeEntries {
	Map<String, List<String>> entries;
	List<String> errors;
	
	public KnowledgeEntries(KnowledgeEntryBuilder builder) {
		entries = new HashMap<String, List<String>>();
		errors = new ArrayList<String>();
		buildEntries(builder);
	}

	public Map<String, List<String>> getEntries() {
		return entries;
	}
	
	private void buildEntries(KnowledgeEntryBuilder builder) {
		for(String s : builder.getJarFiles()) {
			JarFile jarFile = null;
			try {
				jarFile = new JarFile(s);
				scanJarFile(builder, jarFile);
			} catch (IOException e) {
				errors.add("Error opening file " + s + ": " + e.getMessage());
			} finally {
				if (jarFile != null) {
					try {
						jarFile.close();
					} catch (IOException e) {
						errors.add("Error closing file " + s + ": " + e.getMessage());
					}
				}
			}
		}
	}
	
	private void scanJarFile(KnowledgeEntryBuilder builder, JarFile jarFile) {
		Enumeration<JarEntry> jarFileEntries = jarFile.entries();
		while(jarFileEntries.hasMoreElements()) {
			JarEntry jarEntry = jarFileEntries.nextElement();
			for(String patternString : builder.getPatternStrings()) {
				if(Pattern.matches(convertToRegEx(patternString), jarEntry.getName())) {
					addEntry(jarFile.getName(), jarEntry.getName());					
				}
            }

		}
	}
	
	private void addEntry(String jarName, String entryName) {
		List<String> entryList = entries.get(jarName);
		if(null == entryList) {
			entryList = new ArrayList<String>();
			entries.put(jarName, entryList);
		}
		entryList.add(entryName);
	}
	
	private String convertToRegEx(String s) {
        Pattern p = Pattern.compile(KnowledgeEntryBuilder.WILD_CARD_PATTERN);
        Matcher m = p.matcher(s);

        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            if ("*".equals(m.group())) {
                m.appendReplacement(sb, "[^/]*");
            }
            if ("**".equals(m.group())) {
                m.appendReplacement(sb, ".*");
            }
        }
        m.appendTail(sb);
        return sb.toString();
	}
}