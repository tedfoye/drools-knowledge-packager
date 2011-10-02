package org.fiirs.drools.knowledge.packager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class KnowledgeEntryBuilder {
	public static String WILD_CARD_PATTERN = "(\\*\\*)|(\\*)";
	private List<String> jarFiles;
	private List<String> patternStrings;
	
	public KnowledgeEntryBuilder() {
		jarFiles = new ArrayList<String>();
		patternStrings = new ArrayList<String>();
	}
	
	public KnowledgeEntryBuilder addJar(String jarFile) {
		jarFiles.add(jarFile);
		return this;
	}
	
	public KnowledgeEntryBuilder addPatternStr(String patternString) {
		patternStrings.add(patternString);
		return this;
	}
	
	
	public List<String> getJarFiles() {
		return jarFiles;
	}

	public List<String> getPatternStrings() {
		return patternStrings;
	}

	public KnowledgeEntries build() throws IOException {
		return new KnowledgeEntries(this);
	}	
}
