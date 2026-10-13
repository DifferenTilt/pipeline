package io.github.differentilt.pipeline.spring;

import io.github.differentilt.pipeline.event.PipelineEvent;
import io.github.differentilt.pipeline.listener.PipelineListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Test listener that collects the received events. */
final class RecordingListener<C> implements PipelineListener<C> {

    final List<PipelineEvent<? extends C>> events = new CopyOnWriteArrayList<>();

    @Override
    public void onEvent(PipelineEvent<? extends C> event) {
        events.add(event);
    }
}
