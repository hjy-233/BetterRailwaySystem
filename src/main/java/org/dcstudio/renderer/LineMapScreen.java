package org.dcstudio.renderer;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.LayoutStyle;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.dcstudio.network.OpenLineMapPayload;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Consumer;

// 使用 LDLib2 绘制带缩放、拖拽和搜索的线路图。
public final class LineMapScreen extends ModularUIScreen {
    private static final float LAYOUT_GRID_SIZE = 30.0f;

    public LineMapScreen(OpenLineMapPayload payload) {
        super(betterrailwaysystem$createUi(payload), Text.translatable("screen.betterrailwaysystem.line_map"));
    }

    private static ModularUI betterrailwaysystem$createUi(OpenLineMapPayload payload) {
        GraphLayout graphLayout = payload.worldMap() ? betterrailwaysystem$buildWorldLayout(payload) : betterrailwaysystem$buildSingleLineLayout(payload);
        GraphElement graphElement = betterrailwaysystem$layout(new GraphElement(graphLayout), layout -> {
            layout.widthPercent(100);
            layout.flex(1);
            layout.minWidth(0);
            layout.minHeight(0);
        });
        TextField searchField = betterrailwaysystem$layout(new TextField()
                .setAnyString()
                .setText("")
                .setTextResponder(graphElement::setSearchQuery), layout -> {
            layout.flex(1);
            layout.height(18);
            layout.minWidth(0);
        });

        UIElement legend = betterrailwaysystem$buildLegend(payload);

        Label titleLabel = new Label();
        titleLabel.setText(payload.worldMap()
                ? Text.translatable("screen.betterrailwaysystem.line_map")
                : Text.literal(payload.title().isBlank() ? "-" : payload.title()));
        titleLabel.textStyle(textStyle -> textStyle.fontSize(18));
        titleLabel.layout(layout -> layout.height(22));

        Label subtitleLabel = new Label();
        subtitleLabel.setText(payload.currentStation().isBlank() ? Text.literal("") : Text.literal(payload.currentStation()));
        subtitleLabel.layout(layout -> layout.height(16));
        subtitleLabel.setDisplay(!payload.currentStation().isBlank());

        UIElement searchRow = new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.height(18);
                    layout.flexDirection(dev.vfyjxf.taffy.style.FlexDirection.ROW);
                    layout.alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER);
                    layout.gapAll(6);
                })
                .addChildren(
                        betterrailwaysystem$label("screen.betterrailwaysystem.search_station", 10, 54),
                        searchField
                );

        UIElement leftPanel = new UIElement()
                .layout(layout -> {
                    layout.flex(1);
                    layout.minWidth(0);
                    layout.minHeight(0);
                    layout.flexDirection(dev.vfyjxf.taffy.style.FlexDirection.COLUMN);
                    layout.gapAll(6);
                })
                .addChildren(
                        searchRow,
                        betterrailwaysystem$layout(new UIElement()
                                .style(style -> style.backgroundTexture(new ColorRectTexture(0x66000000)))
                                .addChild(graphElement), layout -> {
                            layout.flex(1);
                            layout.minWidth(0);
                            layout.minHeight(0);
                            layout.paddingAll(4);
                        })
                );

        UIElement body = new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.flex(1);
                    layout.minHeight(0);
                    layout.flexDirection(dev.vfyjxf.taffy.style.FlexDirection.ROW);
                    layout.gapAll(8);
                })
                .addChildren(leftPanel, legend);

        Button closeButton = betterrailwaysystem$layout(new Button()
                .setText(Text.translatable("gui.back"))
                .setOnClick(event -> MinecraftClient.getInstance().setScreen(null)), layout -> {
            layout.height(20);
            layout.widthPercent(100);
        });

        UIElement panel = new UIElement()
                .layout(layout -> {
                    layout.widthPercent(90);
                    layout.maxWidth(760);
                    layout.minWidth(380);
                    layout.heightPercent(86);
                    layout.maxHeight(500);
                    layout.minHeight(280);
                    layout.paddingAll(8);
                    layout.gapAll(8);
                    layout.flexDirection(dev.vfyjxf.taffy.style.FlexDirection.COLUMN);
                })
                .style(style -> style.backgroundTexture(Sprites.BORDER))
                .addChildren(titleLabel, subtitleLabel, body, closeButton);

        UIElement root = new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.heightPercent(100);
                    layout.justifyContent(dev.vfyjxf.taffy.style.AlignContent.CENTER);
                    layout.alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER);
                })
                .addChild(panel);
        return new ModularUI(UI.of(root));
    }

    private static GraphLayout betterrailwaysystem$buildSingleLineLayout(OpenLineMapPayload payload) {
        if (payload.lines().isEmpty()) {
            return new GraphLayout(0, 0, 1, 1, List.of(), List.of(), List.of());
        }
        OpenLineMapPayload.LineEntry line = payload.lines().getFirst();
        CityLayoutData cityLayout = betterrailwaysystem$buildCityLayout(
                line.cityName(),
                payload.currentStation(),
                List.of(line),
                !line.cityName().isBlank()
        );
        return betterrailwaysystem$translateToGraphLayout(List.of(cityLayout));
    }

    private static GraphLayout betterrailwaysystem$buildWorldLayout(OpenLineMapPayload payload) {
        Map<String, List<OpenLineMapPayload.LineEntry>> cities = new LinkedHashMap<>();
        for (OpenLineMapPayload.LineEntry line : payload.lines()) {
            cities.computeIfAbsent(line.cityName().isBlank() ? "-" : line.cityName(), ignored -> new ArrayList<>()).add(line);
        }

        List<CityLayoutData> cityLayouts = new ArrayList<>();
        for (Map.Entry<String, List<OpenLineMapPayload.LineEntry>> cityEntry : cities.entrySet()) {
            cityLayouts.add(betterrailwaysystem$buildCityLayout(cityEntry.getKey(), payload.currentStation(), cityEntry.getValue(), true));
        }
        return betterrailwaysystem$translateToGraphLayout(cityLayouts);
    }

    private static GraphLayout betterrailwaysystem$translateToGraphLayout(List<CityLayoutData> cityLayouts) {
        if (cityLayouts.isEmpty()) {
            return new GraphLayout(0, 0, 1, 1, List.of(), List.of(), List.of());
        }

        int columns = Math.max(1, (int) Math.ceil(Math.sqrt(cityLayouts.size())));
        List<RenderedSegment> segments = new ArrayList<>();
        List<RenderedNode> nodes = new ArrayList<>();
        List<RenderedCityLabel> cityLabels = new ArrayList<>();
        float currentY = 32;
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (int rowStart = 0; rowStart < cityLayouts.size(); rowStart += columns) {
            float currentX = 32;
            float rowHeight = 0;
            for (int index = rowStart; index < Math.min(cityLayouts.size(), rowStart + columns); index++) {
                CityLayoutData cityLayout = cityLayouts.get(index);
                float translateX = currentX - cityLayout.minX();
                float translateY = currentY - cityLayout.minY();

                for (RenderedSegment segment : cityLayout.segments()) {
                    segments.add(segment.translate(translateX, translateY));
                }
                for (RenderedNode node : cityLayout.nodes()) {
                    RenderedNode translated = node.translate(translateX, translateY);
                    nodes.add(translated);
                    minX = Math.min(minX, translated.x());
                    minY = Math.min(minY, translated.y());
                    maxX = Math.max(maxX, translated.x());
                    maxY = Math.max(maxY, translated.y());
                }
                for (RenderedCityLabel label : cityLayout.cityLabels()) {
                    RenderedCityLabel translated = label.translate(translateX, translateY);
                    cityLabels.add(translated);
                    minX = Math.min(minX, translated.x() - translated.width() / 2.0f);
                    minY = Math.min(minY, translated.y());
                    maxX = Math.max(maxX, translated.x() + translated.width() / 2.0f);
                    maxY = Math.max(maxY, translated.y() + 12);
                }

                currentX += cityLayout.width() + 64;
                rowHeight = Math.max(rowHeight, cityLayout.height());
            }
            currentY += rowHeight + 72;
        }

        if (nodes.isEmpty()) {
            minX = 0;
            minY = 0;
            maxX = 1;
            maxY = 1;
        }
        return new GraphLayout(minX, minY, maxX, maxY, segments, nodes, cityLabels);
    }

    private static CityLayoutData betterrailwaysystem$buildCityLayout(String cityName, String currentStation, List<OpenLineMapPayload.LineEntry> lines, boolean showCityLabel) {
        Map<String, NodeAccumulator> accumulators = new LinkedHashMap<>();
        List<LinePath> linePaths = new ArrayList<>();

        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            OpenLineMapPayload.LineEntry line = lines.get(lineIndex);
            float fallbackY = 52 + lineIndex * 34;
            List<String> stationKeys = new ArrayList<>();
            for (int stationIndex = 0; stationIndex < line.stations().size(); stationIndex++) {
                OpenLineMapPayload.StationEntry station = line.stations().get(stationIndex);
                String nodeKey = cityName + "|" + station.stationName();
                NodeAccumulator accumulator = accumulators.computeIfAbsent(nodeKey, ignored -> new NodeAccumulator(cityName, station.stationName()));
                accumulator.add(44 + stationIndex * 52, fallbackY, line.lineColor(), line.lineId(), station.pos());
                stationKeys.add(nodeKey);
            }
            linePaths.add(new LinePath(line.lineColor(), stationKeys));
        }

        Map<String, PositionedNode> positionedNodes = betterrailwaysystem$positionCityNodes(accumulators);
        List<RenderedNode> nodes = new ArrayList<>();
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (Map.Entry<String, PositionedNode> entry : positionedNodes.entrySet()) {
            PositionedNode positionedNode = entry.getValue();
            boolean current = !currentStation.isBlank() && currentStation.equals(positionedNode.stationName());
            RenderedNode node = new RenderedNode(
                    positionedNode.x(),
                    positionedNode.y(),
                    positionedNode.stationName(),
                    positionedNode.cityName(),
                    positionedNode.transfer(),
                    current,
                    positionedNode.lineColor(),
                    positionedNode.lineIds(),
                    positionedNode.pos(),
                    betterrailwaysystem$tooltipFor(positionedNode.cityName(), positionedNode.lineIds(), positionedNode.pos(), current, positionedNode.transfer())
            );
            nodes.add(node);
            minX = Math.min(minX, node.x());
            minY = Math.min(minY, node.y());
            maxX = Math.max(maxX, node.x());
            maxY = Math.max(maxY, node.y());
        }

        List<RenderedSegment> segments = betterrailwaysystem$buildSegments(positionedNodes, linePaths);
        for (RenderedSegment segment : segments) {
            minX = Math.min(minX, segment.minX());
            minY = Math.min(minY, segment.minY());
            maxX = Math.max(maxX, segment.maxX());
            maxY = Math.max(maxY, segment.maxY());
        }

        List<RenderedCityLabel> cityLabels = new ArrayList<>();
        if (showCityLabel && !cityName.isBlank()) {
            float labelWidth = Math.max(42, cityName.length() * 7.5f);
            float labelX = (minX + maxX) / 2.0f;
            float labelY = minY - 22;
            cityLabels.add(new RenderedCityLabel(labelX, labelY, cityName, labelWidth));
            minX = Math.min(minX, labelX - labelWidth / 2.0f);
            minY = Math.min(minY, labelY);
            maxX = Math.max(maxX, labelX + labelWidth / 2.0f);
        }

        if (nodes.isEmpty()) {
            minX = 0;
            minY = 0;
            maxX = 1;
            maxY = 1;
        }
        return new CityLayoutData(minX, minY, maxX, maxY, segments, nodes, cityLabels);
    }

    private static Map<String, PositionedNode> betterrailwaysystem$positionCityNodes(Map<String, NodeAccumulator> accumulators) {
        Map<String, PositionedNode> positioned = new LinkedHashMap<>();
        boolean hasRealPositions = accumulators.values().stream().filter(NodeAccumulator::hasRealPosition).count() >= 2;
        if (hasRealPositions) {
            float minRealX = Float.MAX_VALUE;
            float minRealY = Float.MAX_VALUE;
            float maxRealX = -Float.MAX_VALUE;
            float maxRealY = -Float.MAX_VALUE;
            for (NodeAccumulator accumulator : accumulators.values()) {
                if (!accumulator.hasRealPosition()) {
                    continue;
                }
                minRealX = Math.min(minRealX, accumulator.averageRealX());
                minRealY = Math.min(minRealY, accumulator.averageRealY());
                maxRealX = Math.max(maxRealX, accumulator.averageRealX());
                maxRealY = Math.max(maxRealY, accumulator.averageRealY());
            }
            float rangeX = Math.max(1.0f, maxRealX - minRealX);
            float rangeY = Math.max(1.0f, maxRealY - minRealY);
            float scale = Math.max(0.18f, Math.min(1.4f, 260.0f / Math.max(80.0f, Math.max(rangeX, rangeY))));
            float gridSize = 30.0f;
            Map<Long, Integer> occupiedCells = new LinkedHashMap<>();
            for (Map.Entry<String, NodeAccumulator> entry : accumulators.entrySet()) {
                NodeAccumulator accumulator = entry.getValue();
                float rawX = accumulator.hasRealPosition()
                        ? 36 + (accumulator.averageRealX() - minRealX) * scale
                        : accumulator.averageFallbackX();
                float rawY = accumulator.hasRealPosition()
                        ? 44 + (accumulator.averageRealY() - minRealY) * scale
                        : accumulator.averageFallbackY();
                float x = betterrailwaysystem$snapToGrid(rawX, gridSize);
                float y = betterrailwaysystem$snapToGrid(rawY, gridSize);
                long cellKey = betterrailwaysystem$gridKey(x, y, gridSize);
                while (occupiedCells.containsKey(cellKey)) {
                    y += gridSize;
                    cellKey = betterrailwaysystem$gridKey(x, y, gridSize);
                }
                occupiedCells.put(cellKey, 1);
                positioned.put(entry.getKey(), new PositionedNode(
                        x,
                        y,
                        accumulator.stationName,
                        accumulator.cityName,
                        accumulator.lineIds.size() > 1,
                        accumulator.lineColor,
                        List.copyOf(accumulator.lineIds),
                        accumulator.pos
                ));
            }
            return positioned;
        }

        for (Map.Entry<String, NodeAccumulator> entry : accumulators.entrySet()) {
            NodeAccumulator accumulator = entry.getValue();
            positioned.put(entry.getKey(), new PositionedNode(
                    accumulator.averageFallbackX(),
                    accumulator.averageFallbackY(),
                    accumulator.stationName,
                    accumulator.cityName,
                    accumulator.lineIds.size() > 1,
                    accumulator.lineColor,
                    List.copyOf(accumulator.lineIds),
                    accumulator.pos
            ));
        }
        return positioned;
    }

    private static float betterrailwaysystem$snapToGrid(float value, float gridSize) {
        return Math.round(value / gridSize) * gridSize;
    }

    private static long betterrailwaysystem$gridKey(float x, float y, float gridSize) {
        int gridX = Math.round(x / gridSize);
        int gridY = Math.round(y / gridSize);
        return (((long) gridX) << 32) ^ (gridY & 0xffffffffL);
    }

    private static List<RenderedSegment> betterrailwaysystem$buildSegments(Map<String, PositionedNode> nodesByKey, List<LinePath> linePaths) {
        Map<EdgeKey, List<SegmentSeed>> groupedSegments = new LinkedHashMap<>();
        for (LinePath linePath : linePaths) {
            for (int index = 1; index < linePath.stationKeys().size(); index++) {
                String previousKey = linePath.stationKeys().get(index - 1);
                String currentKey = linePath.stationKeys().get(index);
                if (previousKey.equals(currentKey)) {
                    continue;
                }
                groupedSegments.computeIfAbsent(EdgeKey.of(previousKey, currentKey), ignored -> new ArrayList<>())
                        .add(new SegmentSeed(previousKey, currentKey, linePath.color()));
            }
        }

        List<RenderedSegment> segments = new ArrayList<>();
        List<PositionedNode> allNodes = new ArrayList<>(nodesByKey.values());
        Map<CorridorKey, Integer> corridorUsage = new LinkedHashMap<>();
        for (List<SegmentSeed> group : groupedSegments.values()) {
            for (SegmentSeed seed : group) {
                PositionedNode from = nodesByKey.get(seed.fromKey());
                PositionedNode to = nodesByKey.get(seed.toKey());
                if (from == null || to == null) {
                    continue;
                }
                List<Vector2f> route = betterrailwaysystem$routeBetweenNodes(from, to, allNodes, corridorUsage);
                betterrailwaysystem$registerCorridors(route, corridorUsage);
                segments.add(new RenderedSegment(seed.color(), route));
            }
        }
        return segments;
    }

    private static List<Vector2f> betterrailwaysystem$routeBetweenNodes(
            PositionedNode from,
            PositionedNode to,
            List<PositionedNode> allNodes,
            Map<CorridorKey, Integer> corridorUsage
    ) {
        if (betterrailwaysystem$canUseGridRouting(from) && betterrailwaysystem$canUseGridRouting(to)) {
            List<Vector2f> route = betterrailwaysystem$findGridRoute(from, to, allNodes, corridorUsage);
            if (route != null) {
                return route;
            }
        }
        return betterrailwaysystem$fallbackRouteBetweenNodes(from, to, allNodes, corridorUsage);
    }

    private static List<Vector2f> betterrailwaysystem$fallbackRouteBetweenNodes(
            PositionedNode from,
            PositionedNode to,
            List<PositionedNode> allNodes,
            Map<CorridorKey, Integer> corridorUsage
    ) {
        List<List<Vector2f>> candidates = new ArrayList<>();
        candidates.add(betterrailwaysystem$cleanupRoute(List.of(
                new Vector2f(from.x(), from.y()),
                new Vector2f(to.x(), to.y())
        )));

        if (Math.abs(from.x() - to.x()) < 0.01f) {
            float detour = 18.0f;
            candidates.add(betterrailwaysystem$cleanupRoute(List.of(
                    new Vector2f(from.x(), from.y()),
                    new Vector2f(from.x() + detour, from.y()),
                    new Vector2f(from.x() + detour, to.y()),
                    new Vector2f(to.x(), to.y())
            )));
            candidates.add(betterrailwaysystem$cleanupRoute(List.of(
                    new Vector2f(from.x(), from.y()),
                    new Vector2f(from.x() - detour, from.y()),
                    new Vector2f(from.x() - detour, to.y()),
                    new Vector2f(to.x(), to.y())
            )));
        } else if (Math.abs(from.y() - to.y()) < 0.01f) {
            float detour = 18.0f;
            candidates.add(betterrailwaysystem$cleanupRoute(List.of(
                    new Vector2f(from.x(), from.y()),
                    new Vector2f(from.x(), from.y() + detour),
                    new Vector2f(to.x(), from.y() + detour),
                    new Vector2f(to.x(), to.y())
            )));
            candidates.add(betterrailwaysystem$cleanupRoute(List.of(
                    new Vector2f(from.x(), from.y()),
                    new Vector2f(from.x(), from.y() - detour),
                    new Vector2f(to.x(), from.y() - detour),
                    new Vector2f(to.x(), to.y())
            )));
        } else {
            candidates.add(betterrailwaysystem$cleanupRoute(List.of(
                    new Vector2f(from.x(), from.y()),
                    new Vector2f(from.x(), from.y() + 18.0f),
                    new Vector2f(to.x(), from.y() + 18.0f),
                    new Vector2f(to.x(), to.y())
            )));
            candidates.add(betterrailwaysystem$cleanupRoute(List.of(
                    new Vector2f(from.x(), from.y()),
                    new Vector2f(from.x() + 18.0f, from.y()),
                    new Vector2f(from.x() + 18.0f, to.y()),
                    new Vector2f(to.x(), to.y())
            )));
            candidates.add(betterrailwaysystem$cleanupRoute(List.of(
                    new Vector2f(from.x(), from.y()),
                    new Vector2f(to.x(), from.y()),
                    new Vector2f(to.x(), to.y())
            )));
            candidates.add(betterrailwaysystem$cleanupRoute(List.of(
                    new Vector2f(from.x(), from.y()),
                    new Vector2f(from.x(), to.y()),
                    new Vector2f(to.x(), to.y())
            )));
        }

        List<Vector2f> best = candidates.getFirst();
        float bestScore = Float.MAX_VALUE;
        for (List<Vector2f> candidate : candidates) {
            float score = betterrailwaysystem$routeScore(candidate, from, to, allNodes, corridorUsage);
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private static List<Vector2f> betterrailwaysystem$findGridRoute(
            PositionedNode from,
            PositionedNode to,
            List<PositionedNode> allNodes,
            Map<CorridorKey, Integer> corridorUsage
    ) {
        GridPoint start = GridPoint.of(from);
        GridPoint end = GridPoint.of(to);
        if (start.equals(end)) {
            return List.of(new Vector2f(from.x(), from.y()), new Vector2f(to.x(), to.y()));
        }

        Set<GridPoint> blockedNodes = new HashSet<>();
        for (PositionedNode node : allNodes) {
            if (node == from || node == to || !betterrailwaysystem$canUseGridRouting(node)) {
                continue;
            }
            blockedNodes.add(GridPoint.of(node));
        }

        int minGridX = Math.min(start.x(), end.x());
        int maxGridX = Math.max(start.x(), end.x());
        int minGridY = Math.min(start.y(), end.y());
        int maxGridY = Math.max(start.y(), end.y());
        for (PositionedNode node : allNodes) {
            if (!betterrailwaysystem$canUseGridRouting(node)) {
                continue;
            }
            minGridX = Math.min(minGridX, Math.round(node.x()));
            maxGridX = Math.max(maxGridX, Math.round(node.x()));
            minGridY = Math.min(minGridY, Math.round(node.y()));
            maxGridY = Math.max(maxGridY, Math.round(node.y()));
        }

        int padding = Math.round(LAYOUT_GRID_SIZE * 4.0f);
        minGridX -= padding;
        maxGridX += padding;
        minGridY -= padding;
        maxGridY += padding;

        PriorityQueue<PathNode> open = new PriorityQueue<>((first, second) -> Float.compare(first.priority(), second.priority()));
        Map<RouteState, Float> bestCosts = new HashMap<>();
        Map<RouteState, RouteState> previous = new HashMap<>();
        RouteState startState = new RouteState(start, -1);
        bestCosts.put(startState, 0.0f);
        open.add(new PathNode(startState, 0.0f, betterrailwaysystem$manhattan(start, end)));

        while (!open.isEmpty()) {
            PathNode currentNode = open.poll();
            RouteState current = currentNode.state();
            float knownCost = bestCosts.getOrDefault(current, Float.MAX_VALUE);
            if (currentNode.cost() > knownCost + 0.001f) {
                continue;
            }
            if (current.point().equals(end)) {
                return betterrailwaysystem$reconstructGridRoute(current, previous, from, to);
            }

            for (int direction = 0; direction < 4; direction++) {
                GridPoint nextPoint = betterrailwaysystem$move(current.point(), direction);
                if (nextPoint.x() < minGridX || nextPoint.x() > maxGridX || nextPoint.y() < minGridY || nextPoint.y() > maxGridY) {
                    continue;
                }
                if (blockedNodes.contains(nextPoint)) {
                    continue;
                }

                float stepCost = 1.0f;
                if (current.direction() != -1 && current.direction() != direction) {
                    stepCost += 0.6f;
                }
                CorridorKey corridorKey = CorridorKey.of(current.point(), nextPoint);
                stepCost += corridorUsage.getOrDefault(corridorKey, 0) * 6.0f;
                stepCost += betterrailwaysystem$nodeProximityPenalty(nextPoint, blockedNodes);

                RouteState nextState = new RouteState(nextPoint, direction);
                float nextCost = currentNode.cost() + stepCost;
                if (nextCost >= bestCosts.getOrDefault(nextState, Float.MAX_VALUE) - 0.001f) {
                    continue;
                }
                bestCosts.put(nextState, nextCost);
                previous.put(nextState, current);
                float priority = nextCost + betterrailwaysystem$manhattan(nextPoint, end);
                open.add(new PathNode(nextState, nextCost, priority));
            }
        }
        return null;
    }

    private static boolean betterrailwaysystem$canUseGridRouting(PositionedNode node) {
        float gridX = node.x() / LAYOUT_GRID_SIZE;
        float gridY = node.y() / LAYOUT_GRID_SIZE;
        return Math.abs(gridX - Math.round(gridX)) < 0.05f && Math.abs(gridY - Math.round(gridY)) < 0.05f;
    }

    private static float betterrailwaysystem$nodeProximityPenalty(GridPoint point, Set<GridPoint> blockedNodes) {
        float penalty = 0.0f;
        for (GridPoint blockedNode : blockedNodes) {
            int distance = Math.abs(blockedNode.x() - point.x()) + Math.abs(blockedNode.y() - point.y());
            if (distance == 0) {
                return 1000.0f;
            }
            if (distance <= LAYOUT_GRID_SIZE) {
                penalty += 2.5f;
            }
        }
        return penalty;
    }

    private static float betterrailwaysystem$manhattan(GridPoint first, GridPoint second) {
        return (Math.abs(first.x() - second.x()) + Math.abs(first.y() - second.y())) / LAYOUT_GRID_SIZE;
    }

    private static GridPoint betterrailwaysystem$move(GridPoint point, int direction) {
        int step = Math.round(LAYOUT_GRID_SIZE);
        return switch (direction) {
            case 0 -> new GridPoint(point.x() + step, point.y());
            case 1 -> new GridPoint(point.x() - step, point.y());
            case 2 -> new GridPoint(point.x(), point.y() + step);
            default -> new GridPoint(point.x(), point.y() - step);
        };
    }

    private static List<Vector2f> betterrailwaysystem$reconstructGridRoute(
            RouteState endState,
            Map<RouteState, RouteState> previous,
            PositionedNode from,
            PositionedNode to
    ) {
        List<GridPoint> reversedPoints = new ArrayList<>();
        RouteState current = endState;
        while (current != null) {
            reversedPoints.add(current.point());
            current = previous.get(current);
        }

        List<Vector2f> points = new ArrayList<>(reversedPoints.size());
        for (int index = reversedPoints.size() - 1; index >= 0; index--) {
            GridPoint point = reversedPoints.get(index);
            points.add(new Vector2f(point.x(), point.y()));
        }
        if (!points.isEmpty()) {
            points.set(0, new Vector2f(from.x(), from.y()));
            points.set(points.size() - 1, new Vector2f(to.x(), to.y()));
        }
        return betterrailwaysystem$cleanupRoute(points);
    }

    private static List<Vector2f> betterrailwaysystem$cleanupRoute(List<Vector2f> points) {
        List<Vector2f> cleaned = new ArrayList<>();
        for (Vector2f point : points) {
            if (cleaned.isEmpty()) {
                cleaned.add(new Vector2f(point));
                continue;
            }
            Vector2f previous = cleaned.get(cleaned.size() - 1);
            if (Math.abs(previous.x - point.x) < 0.01f && Math.abs(previous.y - point.y) < 0.01f) {
                continue;
            }
            cleaned.add(new Vector2f(point));
        }
        int index = 1;
        while (index < cleaned.size() - 1) {
            Vector2f before = cleaned.get(index - 1);
            Vector2f current = cleaned.get(index);
            Vector2f after = cleaned.get(index + 1);
            boolean sameVertical = Math.abs(before.x - current.x) < 0.01f && Math.abs(current.x - after.x) < 0.01f;
            boolean sameHorizontal = Math.abs(before.y - current.y) < 0.01f && Math.abs(current.y - after.y) < 0.01f;
            if (sameVertical || sameHorizontal) {
                cleaned.remove(index);
                continue;
            }
            index++;
        }
        return cleaned;
    }

    private static float betterrailwaysystem$routeScore(
            List<Vector2f> route,
            PositionedNode from,
            PositionedNode to,
            List<PositionedNode> allNodes,
            Map<CorridorKey, Integer> corridorUsage
    ) {
        float score = route.size() * 3.0f;
        for (int index = 1; index < route.size(); index++) {
            Vector2f start = route.get(index - 1);
            Vector2f end = route.get(index);
            CorridorKey corridorKey = CorridorKey.of(start, end);
            score += corridorUsage.getOrDefault(corridorKey, 0) * 8.0f;
            for (PositionedNode node : allNodes) {
                if (node == from || node == to) {
                    continue;
                }
                if (betterrailwaysystem$segmentHitsNode(start, end, node)) {
                    score += 1000.0f;
                }
            }
        }
        return score;
    }

    private static boolean betterrailwaysystem$segmentHitsNode(Vector2f start, Vector2f end, PositionedNode node) {
        float threshold = 10.0f;
        float minX = Math.min(start.x, end.x) - threshold;
        float maxX = Math.max(start.x, end.x) + threshold;
        float minY = Math.min(start.y, end.y) - threshold;
        float maxY = Math.max(start.y, end.y) + threshold;
        if (node.x() < minX || node.x() > maxX || node.y() < minY || node.y() > maxY) {
            return false;
        }
        if (Math.abs(start.x - end.x) < 0.01f) {
            return Math.abs(node.x() - start.x) <= threshold
                    && node.y() > Math.min(start.y, end.y) + 2.0f
                    && node.y() < Math.max(start.y, end.y) - 2.0f;
        }
        if (Math.abs(start.y - end.y) < 0.01f) {
            return Math.abs(node.y() - start.y) <= threshold
                    && node.x() > Math.min(start.x, end.x) + 2.0f
                    && node.x() < Math.max(start.x, end.x) - 2.0f;
        }
        float dx = end.x - start.x;
        float dy = end.y - start.y;
        float lengthSquared = dx * dx + dy * dy;
        if (lengthSquared < 1.0E-4f) {
            return false;
        }
        float projection = ((node.x() - start.x) * dx + (node.y() - start.y) * dy) / lengthSquared;
        if (projection <= 0.05f || projection >= 0.95f) {
            return false;
        }
        float projectedX = start.x + dx * projection;
        float projectedY = start.y + dy * projection;
        float distanceX = node.x() - projectedX;
        float distanceY = node.y() - projectedY;
        return distanceX * distanceX + distanceY * distanceY <= threshold * threshold;
    }

    private static void betterrailwaysystem$registerCorridors(List<Vector2f> route, Map<CorridorKey, Integer> corridorUsage) {
        for (int index = 1; index < route.size(); index++) {
            CorridorKey corridorKey = CorridorKey.of(route.get(index - 1), route.get(index));
            corridorUsage.put(corridorKey, corridorUsage.getOrDefault(corridorKey, 0) + 1);
        }
    }

    private static UIElement betterrailwaysystem$buildLegend(OpenLineMapPayload payload) {
        UIElement legendRows = new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.flexDirection(dev.vfyjxf.taffy.style.FlexDirection.COLUMN);
                    layout.gapAll(4);
                });

        Map<String, List<OpenLineMapPayload.LineEntry>> grouped = new LinkedHashMap<>();
        for (OpenLineMapPayload.LineEntry line : payload.lines()) {
            grouped.computeIfAbsent(line.cityName().isBlank() ? "-" : line.cityName(), ignored -> new ArrayList<>()).add(line);
        }

        if (payload.worldMap()) {
            grouped.forEach((cityName, lines) -> {
                legendRows.addChild(betterrailwaysystem$label(Text.literal(cityName), 10, 14));
                for (OpenLineMapPayload.LineEntry line : lines) {
                    legendRows.addChild(betterrailwaysystem$legendRow(line.lineId(), line.lineColor()));
                }
            });
        } else if (!payload.lines().isEmpty()) {
            OpenLineMapPayload.LineEntry line = payload.lines().getFirst();
            if (!line.cityName().isBlank()) {
                legendRows.addChild(betterrailwaysystem$label(Text.literal(line.cityName()), 10, 14));
            }
            legendRows.addChild(betterrailwaysystem$legendRow(line.lineId(), line.lineColor()));
        }

        ScrollerView legendScroller = betterrailwaysystem$layout(new ScrollerView()
                .addScrollViewChild(legendRows), layout -> {
            layout.widthPercent(100);
            layout.flex(1);
            layout.minHeight(0);
        });

        return betterrailwaysystem$layout(new UIElement()
                .style(style -> style.backgroundTexture(new ColorRectTexture(0x66000000)))
                .addChild(legendScroller), layout -> {
            layout.width(110);
            layout.minWidth(110);
            layout.maxWidth(110);
            layout.minHeight(0);
            layout.paddingAll(4);
        });
    }

    private static UIElement betterrailwaysystem$legendRow(String lineId, int color) {
        Label label = new Label();
        label.setText(Text.literal(lineId));
        label.textStyle(textStyle -> textStyle.fontSize(9));
        return betterrailwaysystem$layout(new UIElement()
                .addChildren(
                        betterrailwaysystem$layout(new UIElement()
                                .style(style -> style.backgroundTexture(new ColorRectTexture(0xFF000000 | color))), layout -> {
                            layout.width(10);
                            layout.minWidth(10);
                            layout.maxWidth(10);
                            layout.height(10);
                        }),
                        betterrailwaysystem$layout(label, layout -> {
                            layout.flex(1);
                            layout.height(12);
                        })
                ), layout -> {
            layout.widthPercent(100);
            layout.flexDirection(dev.vfyjxf.taffy.style.FlexDirection.ROW);
            layout.alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER);
            layout.gapAll(4);
            layout.height(12);
        });
    }

    private static Label betterrailwaysystem$label(String key, int fontSize, int height) {
        return betterrailwaysystem$label(Text.translatable(key), fontSize, height);
    }

    private static Label betterrailwaysystem$label(Text text, int fontSize, int height) {
        Label label = new Label();
        label.setText(text);
        label.textStyle(textStyle -> textStyle.fontSize(fontSize));
        label.layout(layout -> layout.height(height));
        return label;
    }

    private static List<Text> betterrailwaysystem$tooltipFor(String cityName, List<String> lineIds, BlockPos pos, boolean current, boolean transfer) {
        List<Text> lines = new ArrayList<>();
        lines.add(Text.translatable("screen.betterrailwaysystem.station_city", cityName.isBlank() ? "-" : cityName));
        lines.add(Text.translatable("screen.betterrailwaysystem.station_lines", String.join(", ", lineIds)));
        lines.add(Text.translatable("screen.betterrailwaysystem.station_pos", pos.getX(), pos.getY(), pos.getZ()));
        if (transfer) {
            lines.add(Text.translatable("screen.betterrailwaysystem.station_transfer"));
        }
        if (current) {
            lines.add(Text.translatable("screen.betterrailwaysystem.station_current"));
        }
        return lines;
    }

    private static <T extends UIElement> T betterrailwaysystem$layout(T element, Consumer<LayoutStyle> consumer) {
        element.layout(consumer);
        return element;
    }

    private record GraphLayout(
            float minX,
            float minY,
            float maxX,
            float maxY,
            List<RenderedSegment> segments,
            List<RenderedNode> nodes,
            List<RenderedCityLabel> cityLabels
    ) {
    }

    private record CityLayoutData(
            float minX,
            float minY,
            float maxX,
            float maxY,
            List<RenderedSegment> segments,
            List<RenderedNode> nodes,
            List<RenderedCityLabel> cityLabels
    ) {
        private float width() {
            return Math.max(1, maxX - minX);
        }

        private float height() {
            return Math.max(1, maxY - minY);
        }
    }

    private record EdgeKey(String a, String b) {
        private static EdgeKey of(String first, String second) {
            return first.compareTo(second) <= 0 ? new EdgeKey(first, second) : new EdgeKey(second, first);
        }
    }

    private record CorridorKey(int x1, int y1, int x2, int y2) {
        private static CorridorKey of(Vector2f start, Vector2f end) {
            int startX = Math.round(start.x);
            int startY = Math.round(start.y);
            int endX = Math.round(end.x);
            int endY = Math.round(end.y);
            if (startX < endX || (startX == endX && startY <= endY)) {
                return new CorridorKey(startX, startY, endX, endY);
            }
            return new CorridorKey(endX, endY, startX, startY);
        }

        private static CorridorKey of(GridPoint start, GridPoint end) {
            if (start.x() < end.x() || (start.x() == end.x() && start.y() <= end.y())) {
                return new CorridorKey(start.x(), start.y(), end.x(), end.y());
            }
            return new CorridorKey(end.x(), end.y(), start.x(), start.y());
        }
    }

    private record GridPoint(int x, int y) {
        private static GridPoint of(PositionedNode node) {
            return new GridPoint(Math.round(node.x()), Math.round(node.y()));
        }
    }

    private record RouteState(GridPoint point, int direction) {
    }

    private record PathNode(RouteState state, float cost, float priority) {
    }

    private record SegmentSeed(String fromKey, String toKey, int color) {
    }

    private record RenderedSegment(int color, List<Vector2f> points) {
        private float minX() {
            float min = Float.MAX_VALUE;
            for (Vector2f point : points) {
                min = Math.min(min, point.x);
            }
            return min;
        }

        private float minY() {
            float min = Float.MAX_VALUE;
            for (Vector2f point : points) {
                min = Math.min(min, point.y);
            }
            return min;
        }

        private float maxX() {
            float max = -Float.MAX_VALUE;
            for (Vector2f point : points) {
                max = Math.max(max, point.x);
            }
            return max;
        }

        private float maxY() {
            float max = -Float.MAX_VALUE;
            for (Vector2f point : points) {
                max = Math.max(max, point.y);
            }
            return max;
        }

        private RenderedSegment translate(float translateX, float translateY) {
            List<Vector2f> translatedPoints = new ArrayList<>(points.size());
            for (Vector2f point : points) {
                translatedPoints.add(new Vector2f(point.x + translateX, point.y + translateY));
            }
            return new RenderedSegment(color, List.copyOf(translatedPoints));
        }
    }

    private record RenderedNode(
            float x,
            float y,
            String stationName,
            String cityName,
            boolean transfer,
            boolean current,
            int color,
            List<String> lineIds,
            BlockPos pos,
            List<Text> tooltipLines
    ) {
        private RenderedNode translate(float translateX, float translateY) {
            return new RenderedNode(
                    x + translateX,
                    y + translateY,
                    stationName,
                    cityName,
                    transfer,
                    current,
                    color,
                    lineIds,
                    pos,
                    tooltipLines
            );
        }
    }

    private record RenderedCityLabel(
            float x,
            float y,
            String cityName,
            float width
    ) {
        private RenderedCityLabel translate(float translateX, float translateY) {
            return new RenderedCityLabel(x + translateX, y + translateY, cityName, width);
        }
    }

    private record PositionedNode(
            float x,
            float y,
            String stationName,
            String cityName,
            boolean transfer,
            int lineColor,
            List<String> lineIds,
            BlockPos pos
    ) {
    }

    private record LinePath(int color, List<String> stationKeys) {
    }

    private record DragOffset(float startOffsetX, float startOffsetY) {
    }

    private static final class NodeAccumulator {
        private final String cityName;
        private final String stationName;
        private float fallbackXSum;
        private float fallbackYSum;
        private int fallbackCount;
        private float realXSum;
        private float realYSum;
        private int realCount;
        private int lineColor;
        private BlockPos pos = BlockPos.ORIGIN;
        private final LinkedHashSet<String> lineIds = new LinkedHashSet<>();

        private NodeAccumulator(String cityName, String stationName) {
            this.cityName = cityName;
            this.stationName = stationName;
        }

        private void add(float fallbackX, float fallbackY, int color, String lineId, BlockPos pos) {
            fallbackXSum += fallbackX;
            fallbackYSum += fallbackY;
            fallbackCount++;
            lineColor = color;
            lineIds.add(lineId);
            if (pos != null && !BlockPos.ORIGIN.equals(pos)) {
                realXSum += pos.getX();
                realYSum += -pos.getZ();
                realCount++;
                if (BlockPos.ORIGIN.equals(this.pos)) {
                    this.pos = pos;
                }
            }
        }

        private boolean hasRealPosition() {
            return realCount > 0;
        }

        private float averageFallbackX() {
            return fallbackCount == 0 ? 0 : fallbackXSum / fallbackCount;
        }

        private float averageFallbackY() {
            return fallbackCount == 0 ? 0 : fallbackYSum / fallbackCount;
        }

        private float averageRealX() {
            return realCount == 0 ? averageFallbackX() : realXSum / realCount;
        }

        private float averageRealY() {
            return realCount == 0 ? averageFallbackY() : realYSum / realCount;
        }
    }

    private static final class GraphElement extends UIElement {
        private static final float MIN_SCALE = 0.3f;
        private static final float MAX_SCALE = 4.0f;

        private final GraphLayout graphLayout;
        private float offsetX;
        private float offsetY;
        private float scale = 1.0f;
        private boolean viewInitialized;
        private String searchQuery = "";

        private GraphElement(GraphLayout graphLayout) {
            this.graphLayout = graphLayout;
            setOverflowVisible(false);
            style(style -> style.backgroundTexture(new ColorRectTexture(0xAA101010)));
            addEventListener(UIEvents.MOUSE_DOWN, this::betterrailwaysystem$onMouseDown);
            addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::betterrailwaysystem$onDragSourceUpdate);
            addEventListener(UIEvents.MOUSE_WHEEL, this::betterrailwaysystem$onMouseWheel);
        }

        public void setSearchQuery(String searchQuery) {
            String normalized = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
            if (normalized.equals(this.searchQuery)) {
                return;
            }
            this.searchQuery = normalized;
            RenderedNode firstMatch = betterrailwaysystem$findFirstSearchMatch();
            if (firstMatch != null) {
                if (scale < 0.9f) {
                    scale = 0.9f;
                }
                betterrailwaysystem$centerOn(firstMatch.x(), firstMatch.y());
            }
        }

        @Override
        protected void onLayoutChanged() {
            super.onLayoutChanged();
            if (!viewInitialized && getContentWidth() > 1 && getContentHeight() > 1) {
                betterrailwaysystem$fitToBounds(32);
                viewInitialized = true;
            } else if (viewInitialized) {
                betterrailwaysystem$clampOffsets();
            }
        }

        private void betterrailwaysystem$fitToBounds(float padding) {
            float graphWidth = Math.max(1.0f, graphLayout.maxX() - graphLayout.minX());
            float graphHeight = Math.max(1.0f, graphLayout.maxY() - graphLayout.minY());
            float availableWidth = Math.max(1.0f, getContentWidth() - padding * 2.0f);
            float availableHeight = Math.max(1.0f, getContentHeight() - padding * 2.0f);
            scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, Math.min(availableWidth / graphWidth, availableHeight / graphHeight)));
            float worldViewWidth = getContentWidth() / scale;
            float worldViewHeight = getContentHeight() / scale;
            offsetX = graphLayout.minX() - Math.max(0, (worldViewWidth - graphWidth) / 2.0f);
            offsetY = graphLayout.minY() - Math.max(0, (worldViewHeight - graphHeight) / 2.0f);
            betterrailwaysystem$clampOffsets();
        }

        private void betterrailwaysystem$centerOn(float x, float y) {
            offsetX = x - getContentWidth() / (2.0f * scale);
            offsetY = y - getContentHeight() / (2.0f * scale);
            betterrailwaysystem$clampOffsets();
        }

        private void betterrailwaysystem$clampOffsets() {
            float worldViewWidth = Math.max(1.0f, getContentWidth() / Math.max(1.0E-4f, scale));
            float worldViewHeight = Math.max(1.0f, getContentHeight() / Math.max(1.0E-4f, scale));
            float padding = 36.0f / Math.max(1.0E-4f, scale);
            float minOffsetX = graphLayout.minX() - padding;
            float minOffsetY = graphLayout.minY() - padding;
            float maxOffsetX = graphLayout.maxX() - worldViewWidth + padding;
            float maxOffsetY = graphLayout.maxY() - worldViewHeight + padding;
            if (minOffsetX > maxOffsetX) {
                offsetX = (graphLayout.minX() + graphLayout.maxX() - worldViewWidth) / 2.0f;
            } else {
                offsetX = Math.max(minOffsetX, Math.min(maxOffsetX, offsetX));
            }
            if (minOffsetY > maxOffsetY) {
                offsetY = (graphLayout.minY() + graphLayout.maxY() - worldViewHeight) / 2.0f;
            } else {
                offsetY = Math.max(minOffsetY, Math.min(maxOffsetY, offsetY));
            }
        }

        private RenderedNode betterrailwaysystem$findFirstSearchMatch() {
            if (searchQuery.isBlank()) {
                return null;
            }
            for (RenderedNode node : graphLayout.nodes()) {
                if (node.stationName().toLowerCase(Locale.ROOT).contains(searchQuery)) {
                    return node;
                }
            }
            return null;
        }

        private boolean betterrailwaysystem$matchesSearch(RenderedNode node) {
            return !searchQuery.isBlank() && node.stationName().toLowerCase(Locale.ROOT).contains(searchQuery);
        }

        private void betterrailwaysystem$onMouseDown(UIEvent event) {
            if (!isSelfOrChildHover() || !isMouseOverContent(event.x, event.y)) {
                return;
            }
            if (event.button == 0 || event.button == 2) {
                startDrag(new DragOffset(offsetX, offsetY), null);
                event.stopPropagation();
            }
        }

        private void betterrailwaysystem$onDragSourceUpdate(UIEvent event) {
            if (!(event.dragHandler.draggingObject instanceof DragOffset dragOffset)) {
                return;
            }
            float scaled = Math.max(1.0E-4f, scale);
            float localX = event.x - getContentX();
            float localY = event.y - getContentY();
            float startLocalX = event.dragStartX - getContentX();
            float startLocalY = event.dragStartY - getContentY();
            offsetX = dragOffset.startOffsetX() + (startLocalX - localX) / scaled;
            offsetY = dragOffset.startOffsetY() + (startLocalY - localY) / scaled;
            betterrailwaysystem$clampOffsets();
        }

        private void betterrailwaysystem$onMouseWheel(UIEvent event) {
            if (!isSelfOrChildHover() || !isMouseOverContent(event.x, event.y)) {
                return;
            }
            float newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale + event.deltaY * 0.1f));
            if (Math.abs(newScale - scale) < 1.0E-4f) {
                return;
            }
            float localX = event.x - getContentX();
            float localY = event.y - getContentY();
            offsetX += localX / scale - localX / newScale;
            offsetY += localY / scale - localY / newScale;
            scale = newScale;
            betterrailwaysystem$clampOffsets();
            event.stopPropagation();
        }

        @Override
        public void drawBackgroundAdditional(GUIContext context) {
            super.drawBackgroundAdditional(context);
            int baseX = Math.round(getContentX());
            int baseY = Math.round(getContentY());
            int width = Math.round(getContentWidth());
            int height = Math.round(getContentHeight());
            context.graphics.fill(baseX, baseY, baseX + width, baseY + height, 0xAA111111);
            context.graphics.enableScissor(baseX, baseY, baseX + width, baseY + height);
            betterrailwaysystem$drawGrid(context, baseX, baseY, width, height);

            List<Text> hoveredTooltip = null;

            for (RenderedSegment segment : graphLayout.segments()) {
                List<Vector2f> points = new ArrayList<>(segment.points().size());
                for (Vector2f point : segment.points()) {
                    points.add(new Vector2f(
                            baseX + (point.x - offsetX) * scale,
                            baseY + (point.y - offsetY) * scale
                    ));
                }
                DrawerHelper.drawLines(
                        context.graphics,
                        points,
                        0xFF000000 | segment.color(),
                        0xFF000000 | segment.color(),
                        betterrailwaysystem$getLineWidth(width, height)
                );
            }

            for (RenderedCityLabel cityLabel : graphLayout.cityLabels()) {
                int labelX = Math.round(baseX + (cityLabel.x() - offsetX) * scale);
                int labelY = Math.round(baseY + (cityLabel.y() - offsetY) * scale);
                int labelWidth = Math.round(cityLabel.width());
                context.graphics.fill(labelX - labelWidth / 2 - 4, labelY - 3, labelX + labelWidth / 2 + 4, labelY + 10, 0xAA000000);
                context.graphics.drawCenteredTextWithShadow(context.mc.textRenderer, cityLabel.cityName(), labelX, labelY, 0xFFEBCB8B);
            }

            for (RenderedNode node : graphLayout.nodes()) {
                boolean searchMatch = betterrailwaysystem$matchesSearch(node);
                int size = node.transfer() ? 10 : 8;
                if (node.current()) {
                    size = 12;
                }
                if (searchMatch) {
                    size += 2;
                }
                int centerX = Math.round(baseX + (node.x() - offsetX) * scale);
                int centerY = Math.round(baseY + (node.y() - offsetY) * scale);
                int x = centerX - size / 2;
                int y = centerY - size / 2;
                int fillColor = node.transfer() ? 0xFFFFFFFF : 0xFF000000 | node.color();
                int borderColor = searchMatch ? 0xFF6BD7FF : (node.current() ? 0xFFFFFF55 : 0xFF222222);
                context.graphics.fill(x, y, x + size, y + size, fillColor);
                context.graphics.drawBorder(x, y, size, size, borderColor);
                context.graphics.drawTextWithShadow(
                        context.mc.textRenderer,
                        node.stationName(),
                        x + size + 4,
                        y - 1,
                        searchMatch ? 0xFF6BD7FF : (node.current() ? 0xFFFFFF55 : 0xFFFFFFFF)
                );
                if (context.mouseX >= x && context.mouseX <= x + size && context.mouseY >= y && context.mouseY <= y + size) {
                    hoveredTooltip = node.tooltipLines();
                }
            }

            context.graphics.disableScissor();
            if (hoveredTooltip != null && !hoveredTooltip.isEmpty()) {
                context.graphics.drawTooltip(context.mc.textRenderer, hoveredTooltip, context.mouseX + 8, context.mouseY + 8);
            }
        }

        private void betterrailwaysystem$drawGrid(GUIContext context, int baseX, int baseY, int width, int height) {
            float worldGrid = 64.0f;
            float visibleWorldWidth = width / Math.max(1.0E-4f, scale);
            float visibleWorldHeight = height / Math.max(1.0E-4f, scale);
            float startWorldX = (float) Math.floor(offsetX / worldGrid) * worldGrid;
            float startWorldY = (float) Math.floor(offsetY / worldGrid) * worldGrid;
            float endWorldX = offsetX + visibleWorldWidth;
            float endWorldY = offsetY + visibleWorldHeight;

            for (float worldX = startWorldX; worldX <= endWorldX + worldGrid; worldX += worldGrid) {
                int drawX = Math.round(baseX + (worldX - offsetX) * scale);
                context.graphics.fill(drawX, baseY, drawX + 1, baseY + height, 0x22333333);
            }
            for (float worldY = startWorldY; worldY <= endWorldY + worldGrid; worldY += worldGrid) {
                int drawY = Math.round(baseY + (worldY - offsetY) * scale);
                context.graphics.fill(baseX, drawY, baseX + width, drawY + 1, 0x22333333);
            }
        }

        private float betterrailwaysystem$getLineWidth(int width, int height) {
            float minSize = Math.max(1.0f, Math.min(width, height));
            return Math.max(1.4f, Math.min(4.6f, minSize / 170.0f));
        }
    }
}
