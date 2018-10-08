/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xjmdoo.processors.hexdump;

import org.apache.nifi.annotation.behavior.*;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Tags({"hex, convert"})
@CapabilityDescription("A processor that allows the user to convert data to hexadecimal format. If an incoming " +
    "relationship is added, this processor will convert the content of the FlowFile to hexadecimal format.")
@SideEffectFree
@SupportsBatching
public class Hexdump extends AbstractProcessor {

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("Success")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("Failure")
            .build();

    private static final char hex[] = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final Set<Relationship> relationships = new HashSet<>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAILURE);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }

        try {

            session.write(flowFile, (rawIn, outputStream) -> {
                try (final InputStream in = new BufferedInputStream(rawIn)) {
                    while (true) {
                        final int nextByte = in.read();
                        if (nextByte == -1) {
                            return;
                        }

                        outputStream.write(hex[((nextByte >> 4) & 0xF)]);
                        outputStream.write(hex[((nextByte >> 0) & 0xF)]);
                    }
                }
            });

            session.transfer(flowFile, REL_SUCCESS);
        } catch (ProcessException e) {
            session.transfer(flowFile, REL_FAILURE);
        }
    }
}
