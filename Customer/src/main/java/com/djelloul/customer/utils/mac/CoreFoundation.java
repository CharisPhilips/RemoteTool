/*
 * Copyright 2015-2019 OpenIndex.de.
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
package com.djelloul.customer.utils.mac;

import com.sun.jna.Library;
import com.sun.jna.Structure;

/**
 * Definition (incomplete) of the Core Foundation framework.
 *
 * @author Djelloul
 * @see <a href="https://developer.apple.com/documentation/corefoundation">Core Foundation</a>
 */
@SuppressWarnings("unused")
public interface CoreFoundation extends Library {
    String JNA_LIBRARY_NAME = "CoreFoundation";

    /**
     * Releases a Core Foundation object.
     *
     * @param cf A CFType object to release. This value must not be NULL.
     * @see <a href="https://developer.apple.com/documentation/corefoundation/1521153-cfrelease">CFRelease</a>
     */
    void CFRelease(CFTypeRef cf);

    /**
     * An untyped "generic" reference to any Core Foundation object.
     *
     * @see <a href="https://developer.apple.com/documentation/corefoundation/cftyperef">CFTypeRef</a>
     */
    class CFTypeRef extends Structure {
    }
}
