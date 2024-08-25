package com.krickert.search.indexer.enhancers;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import com.krickert.search.model.pipe.PipeDocument;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Singleton
public class MessageConverter {

    public List<Message> convertDescriptorsToMessages(Collection<PipeDocument> descriptors) {
        List<Message> messages = new ArrayList<>();

        for (PipeDocument descriptor : descriptors) {
            DynamicMessage message = DynamicMessage.newBuilder(descriptor).build();
            messages.add(message);
        }

        return messages;
    }
}
