/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.restassured.internal.bytecode;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;

/**
 * Fails if any compiled main class of this module references Groovy. This module must not depend on Groovy.
 * <p>
 * Self-contained so it can be copied to another module: change {@link #ANCHOR} to a main class of that module.
 */
class NoGroovyBytecodeTest {

    /**
     * Any class from this module's main sources. Its location is the directory that gets scanned (target/classes).
     */
    private static final Class<?> ANCHOR = RestAssured.class;

    private static final String[] FORBIDDEN = {"groovy/", "org/codehaus/groovy/", "org/apache/groovy/"};

    @Test
    void main_classes_do_not_reference_groovy() throws Exception {
        Path classesDir = Paths.get(ANCHOR.getProtectionDomain().getCodeSource().getLocation().toURI());
        List<Path> classFiles;
        try (Stream<Path> files = Files.walk(classesDir)) {
            classFiles = files.filter(p -> p.toString().endsWith(".class")).collect(Collectors.toList());
        }

        List<String> violations = new ArrayList<>();
        for (Path classFile : classFiles) {
            List<String> groovyReferences = utf8Constants(Files.readAllBytes(classFile)).stream()
                    .filter(NoGroovyBytecodeTest::isGroovyReference)
                    .collect(Collectors.toList());
            if (!groovyReferences.isEmpty()) {
                violations.add(classesDir.relativize(classFile) + " references " + groovyReferences);
            }
        }

        assertThat("class files scanned in " + classesDir, classFiles.size(), greaterThan(0));
        assertThat(violations, empty());
    }

    private static boolean isGroovyReference(String constant) {
        for (String forbidden : FORBIDDEN) {
            if (constant.contains(forbidden)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the CONSTANT_Utf8 entries of a class file's constant pool. These hold every class name, descriptor
     * and generic signature the class refers to (JVMS 4.4).
     */
    private static List<String> utf8Constants(byte[] classBytes) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(classBytes));
        if (in.readInt() != 0xCAFEBABE) {
            throw new IOException("Not a class file");
        }
        in.readUnsignedShort(); // minor version
        in.readUnsignedShort(); // major version
        int constantPoolCount = in.readUnsignedShort();
        List<String> utf8 = new ArrayList<>();
        for (int i = 1; i < constantPoolCount; i++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
                case 1: // Utf8
                    utf8.add(in.readUTF());
                    break;
                case 7: // Class
                case 8: // String
                case 16: // MethodType
                case 19: // Module
                case 20: // Package
                    in.skipBytes(2);
                    break;
                case 15: // MethodHandle
                    in.skipBytes(3);
                    break;
                case 3: // Integer
                case 4: // Float
                case 9: // Fieldref
                case 10: // Methodref
                case 11: // InterfaceMethodref
                case 12: // NameAndType
                case 17: // Dynamic
                case 18: // InvokeDynamic
                    in.skipBytes(4);
                    break;
                case 5: // Long
                case 6: // Double
                    in.skipBytes(8);
                    i++; // takes two constant pool slots
                    break;
                default:
                    throw new IOException("Unknown constant pool tag " + tag);
            }
        }
        return utf8;
    }
}
