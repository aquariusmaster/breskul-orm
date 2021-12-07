package com.anderb.breskulorm.action;

import com.anderb.breskulorm.exception.OrmException;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class ActionQueue {
    private static final LinkedHashMap<Class<? extends Action>, Function<ActionQueue, List<? extends Action>>> EXECUTABLE_LISTS_MAP;

    static {
        EXECUTABLE_LISTS_MAP = new LinkedHashMap<>(3);

        EXECUTABLE_LISTS_MAP.put(
                InsertAction.class,
                (actionQueue) -> actionQueue.insertions
        );
        EXECUTABLE_LISTS_MAP.put(
                UpdateAction.class,
                (actionQueue) -> actionQueue.updates
        );
        EXECUTABLE_LISTS_MAP.put(
                DeleteAction.class,
                (actionQueue) -> actionQueue.deletions
        );
    }

    private List<InsertAction> insertions;
    private List<DeleteAction> deletions;
    private List<UpdateAction> updates;

    public void addAction(InsertAction action) {
        if (insertions == null) {
            insertions = new LinkedList<>();
        }
        insertions.add(action);
    }

    public void addAction(UpdateAction action) {
        if (updates == null) {
            updates = new LinkedList<>();
        }
        updates.add(action);
    }

    public void addAction(DeleteAction action) {
        if (deletions == null) {
            deletions = new LinkedList<>();
        }
        deletions.add(action);
    }

    public void executeActions() throws OrmException {
        EXECUTABLE_LISTS_MAP.forEach((k, actionQueueProvider) -> {
            var l = actionQueueProvider.apply(this);
            if (l != null && !l.isEmpty()) {
                executeActions(l);
            }
        });
    }

    public void executeActions(List<? extends Action> list) throws OrmException {
        for (Action action : list) {
            action.execute();
        }
        list.clear();
    }

    /**
     * Execute {@link Action} immediately
     *
     * @param executable
     * @param <E>
     */
    public <E extends Action> void execute(E action) {
        action.execute();
    }


    public List<InsertAction> getInsertions() {
        return insertions;
    }

    public List<DeleteAction> getDeletions() {
        return deletions;
    }

    public List<UpdateAction> getUpdates() {
        return updates;
    }

}
