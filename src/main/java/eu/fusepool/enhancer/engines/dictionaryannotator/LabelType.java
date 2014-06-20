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
public enum LabelType {
    LABEL("label"),
    SYNONYM("synonym");
    
    private final String name;
        
    private LabelType(final String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }

    public static LabelType getByName(String name) {
        return LabelType.valueOf(name);
    }
    
    public static LabelType getByValue(String value) {
        for (LabelType e : LabelType.values()) {
            if (e.name.equals(value)) {
                return e;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return name;
    }
}