/*
 * Copyright 2014 Gabor.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.fusepool.enhancer.engines.dictionaryannotator;

/**
 *
 * @author Gabor
 */
public class Concept {
    public String labelText;
    public LabelType labelType;
    public String uri;
    public String type;

    public Concept() {

    }
    
    public Concept(String labelText, String labelType, String uri) {
        this.labelText = labelText;
        this.labelType = LabelType.getByValue(labelType);
        this.uri = uri;
    }
    
    public Concept(String labelText, String labelType, String uri, String type) {
        this.labelText = labelText;
        this.labelType = LabelType.getByValue(labelType);
        this.uri = uri;
        this.type = type;
    }
    
    public Boolean IsLabel(){
        if(LabelType.LABEL.toString().equals(labelType.toString())){
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Concept{" + "labelText=" + labelText + ", labelType=" + labelType + ", uri=" + uri + ", type=" + type + '}';
    }
    
    
}
