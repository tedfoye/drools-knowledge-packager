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

import java.util.ArrayList;
import java.util.List;

public class KnowledgeJar {
    private String knowledgeJarName;
	private List<String> patternStrings;
	
	public KnowledgeJar() {
		patternStrings = new ArrayList<String>();
	}
	
	public String getKnowledgeJarName() {
		return knowledgeJarName;
	}
	
	public void setKnowledgeJarName(String knowledgeJarName) {
		this.knowledgeJarName = knowledgeJarName;
	}
	
	public List<String> getPatternStrings() {
		return patternStrings;
	}
	
	public void setPatternStrings(List<String> patternStrings) {
		this.patternStrings = patternStrings;
	}
	
	public void setPatternString(String patternString) {
		patternStrings.add(patternString);
	}
}
