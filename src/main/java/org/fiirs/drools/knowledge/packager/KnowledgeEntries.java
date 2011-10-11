/**
 * Copyright 2011 Ted Foye
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fiirs.drools.knowledge.packager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KnowledgeEntries {
	private List<KnowledgeJar> knowledgeJars;
	private List<String> errors;

	public KnowledgeEntries(KnowledgeEntryBuilder builder) throws IOException {
		knowledgeJars = new ArrayList<KnowledgeJar>();
		errors = new ArrayList<String>();
		buildEntries(builder);
	}

	public List<KnowledgeJar> getKnowledgeJars() {
		return knowledgeJars;
	}
	
	public List<String> getErrors() {
		return errors;
	}

	private void buildEntries(KnowledgeEntryBuilder builder) throws IOException {
		for (KnowledgeJar kJar : builder.getKJars()) {
			JarFile jarFile = null;
			try {
				jarFile = new JarFile(kJar.getKnowledgeJarName());
				scanJarFile(jarFile, kJar.getPatternStrings());
			} finally {
				if (jarFile != null) {
					jarFile.close();
				}
			}
		}
	}

	private void scanJarFile(JarFile jarFile, List<String> patternStrings) {
		Set<String> alreadyProcessed = new HashSet<String>();
		
		List<String> jarEntries = new ArrayList<String>();
		for(String patternString : patternStrings) {
			Enumeration<JarEntry> jarFileEntries = jarFile.entries();
			while (jarFileEntries.hasMoreElements()) {
				JarEntry jarEntry = jarFileEntries.nextElement();
				if (Pattern.matches(convertToRegEx(patternString), jarEntry.getName())) {
					if(!alreadyProcessed.contains(jarEntry.getName())) {
						jarEntries.add(jarEntry.getName());
						alreadyProcessed.add(jarEntry.getName());
					}
				}
			}
		}
		if(jarEntries.size() > 0) {
			KnowledgeJar kJar = new KnowledgeJar();
			kJar.setKnowledgeJarName(jarFile.getName());
			kJar.setPatternStrings(jarEntries);
			knowledgeJars.add(kJar);
		}
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
