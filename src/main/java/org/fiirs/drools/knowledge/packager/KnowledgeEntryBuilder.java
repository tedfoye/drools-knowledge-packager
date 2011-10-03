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
