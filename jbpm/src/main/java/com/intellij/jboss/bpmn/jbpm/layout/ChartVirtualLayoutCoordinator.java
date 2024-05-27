package com.intellij.jboss.bpmn.jbpm.layout;

import com.intellij.jboss.bpmn.jbpm.render.size.ChartNodeSizeEnhancer;
import com.intellij.openapi.command.undo.DocumentReference;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChartVirtualLayoutCoordinator implements ChartLayoutCoordinator {
  @NotNull final Map<String, NodeLayout> nodeLayouts;
  @NotNull final Map<Pair<String, String>, List<Point>> edgeLayouts;
  private final boolean persistAutoGeneratedLayout;
  @NotNull private final ChartLayoutCoordinator chartLayoutCoordinator;

  public ChartVirtualLayoutCoordinator(@NotNull Project project,
                                       DocumentReference[] references,
                                       boolean persistAutoGeneratedLayout,
                                       @NotNull ChartLayoutCoordinator coordinator) {
    nodeLayouts = new MapUndoableWrapper<>(
      UndoManager.getInstance(project),
      references,
      new HashMap<>());
    edgeLayouts = new MapUndoableWrapper<>(
      UndoManager.getInstance(project),
      references,
      new HashMap<>());
    this.persistAutoGeneratedLayout = persistAutoGeneratedLayout;
    chartLayoutCoordinator = coordinator;
  }

  @Nullable
  @Override
  public NodeLayout getNodeLayout(@NotNull String nodeId, ChartNodeSizeEnhancer enhancer) {
    NodeLayout virtualLayout = nodeLayouts.get(nodeId);
    return virtualLayout != null ? virtualLayout : chartLayoutCoordinator.getNodeLayout(nodeId, enhancer);
  }

  @Nullable
  public Runnable getCreateNodeLayoutAction(@NotNull final String nodeId,
                                            @NotNull final NodeLayout layout,
                                            @Nullable ChartNodeSizeEnhancer enhancer,
                                            boolean isAutoGenerated) {
    if (isAutoGenerated && !persistAutoGeneratedLayout) {
      return () -> nodeLayouts.put(nodeId, layout);
    }
    return chartLayoutCoordinator.getChangeNodeLayoutAction(nodeId, layout, enhancer);
  }

  @Nullable
  @Override
  public Runnable getChangeNodeLayoutAction(@NotNull final String nodeId,
                                            @NotNull NodeLayout layout,
                                            @Nullable ChartNodeSizeEnhancer enhancer) {
    NodeLayout virtualLayout = nodeLayouts.get(nodeId);
    if (virtualLayout == null) {
      return chartLayoutCoordinator.getChangeNodeLayoutAction(nodeId, layout, enhancer);
    }
    if (!virtualLayout.equals(layout)) {
      final Runnable action = chartLayoutCoordinator.getChangeNodeLayoutAction(nodeId, layout, enhancer);
      return () -> {
        nodeLayouts.remove(nodeId);
        if (action != null) {
          action.run();
        }
      };
    }
    return null;
  }

  @Nullable
  @Override
  public List<Point> getEdgePoints(@NotNull String sourceNodeId, @NotNull String targetNodeId) {
    List<Point> points = chartLayoutCoordinator.getEdgePoints(sourceNodeId, targetNodeId);
    return points == null ? edgeLayouts.get(Pair.create(sourceNodeId, targetNodeId)) : points;
  }

  @Nullable
  @Override
  public Runnable getChangeEdgePointsAction(@NotNull String sourceNodeId,
                                            @NotNull String targetNodeId,
                                            @NotNull List<Point> points) {
    return chartLayoutCoordinator.getChangeEdgePointsAction(sourceNodeId, targetNodeId, points);
  }
}
