package com.dtilt.pipeline.spring;

import com.dtilt.pipeline.event.PipelineEvent;
import com.dtilt.pipeline.listener.PipelineListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Listener di test che raccoglie gli eventi ricevuti. */
final class RecordingListener<C> implements PipelineListener<C> {

    final List<PipelineEvent<? extends C>> events = new CopyOnWriteArrayList<>();

    @Override
    public void onEvent(PipelineEvent<? extends C> event) {
        events.add(event);
    }
}
