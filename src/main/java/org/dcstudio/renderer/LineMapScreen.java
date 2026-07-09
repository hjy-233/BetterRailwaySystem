package org.dcstudio.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
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

// 原生线路图界面，支持搜索、缩放、拖拽、图例和悬浮详情。
public final class LineMapScreen extends Screen {
    private static final float LAYOUT_GRID_SIZE = 30.0f;
    private static final float MIN_SCALE = 0.3f;
    private static final float MAX_SCALE = 4.0f;

    private final OpenLineMapPayload payload;
    private final GraphLayout graphLayout;

    private TextFieldWidget searchField;
    private LegendListWidget legendList;
    private ButtonWidget backButton;

    private String searchQuery = "";
    private float offsetX;
    private float offsetY;
    private float scale = 1.0f;
    private boolean viewInitialized;
    private boolean dragging;
    private double dragStartMouseX;
    private double dragStartMouseY;
    private float dragStartOffsetX;
    private float dragStartOffsetY;

    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int graphLeft;
    private int graphTop;
    private int graphWidth;
    private int graphHeight;
    private int legendLeft;
    private int legendTop;
    private int legendWidth;
    private int legendHeight;

    public LineMapScreen(OpenLineMapPayload payload) {
        super(payload.worldMap()
                ? Text.translatable("screen.betterrailwaysystem.world_map")
                : Text.translatable("screen.betterrailwaysystem.line_map"));
        this.payload = payload;
        this.graphLayout = payload.worldMap() ? betterrailwaysystem$buildWorldLayout(payload) : betterrailwaysystem$buildSingleLineLayout(payload);
    }

    @Override
    protected void init() {
        super.init();
        betterrailwaysystem$layoutBounds();
        clearChildren();

        int searchWidth = Math.max(160, graphWidth - 110);
        searchField = addDrawableChild(new TextFieldWidget(textRenderer, graphLeft + 72, panelTop + 30, searchWidth, 20, Text.empty()));
        searchField.setMaxLength(128);
        searchField.setText(searchQuery);
        searchField.setChangedListener(this::betterrailwaysystem$setSearchQuery);

        legendList = addDrawableChild(new LegendListWidget(client, legendLeft, legendTop, legendWidth, legendHeight, betterrailwaysystem$buildLegendItems()));

        backButton = addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), button -> close())
                .dimensions(panelLeft + panelWidth - 100, panelTop + panelHeight - 24, 90, 20)
                .build());

        if (!viewInitialized) {
            betterrailwaysystem$fitToBounds(32.0f);
            viewInitialized = true;
        } else {
            betterrailwaysystem$clampOffsets();
        }
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        String search = searchField == null ? searchQuery : searchField.getText();
        super.resize(client, width, height);
        betterrailwaysystem$setSearchQuery(search);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(null);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (betterrailwaysystem$isInsideGraph(mouseX, mouseY) && (button == 0 || button == 2)) {
            dragging = true;
            dragStartMouseX = mouseX;
            dragStartMouseY = mouseY;
            dragStartOffsetX = offsetX;
            dragStartOffsetY = offsetY;
            setFocused(null);
            return true;
        }
        return handled;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            float scaled = Math.max(1.0E-4f, scale);
            offsetX = dragStartOffsetX + (float) ((dragStartMouseX - mouseX) / scaled);
            offsetY = dragStartOffsetY + (float) ((dragStartMouseY - mouseY) / scaled);
            betterrailwaysystem$clampOffsets();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (betterrailwaysystem$isInsideGraph(mouseX, mouseY)) {
            float newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale + (float) verticalAmount * 0.1f));
            if (Math.abs(newScale - scale) < 1.0E-4f) {
                return true;
            }
            float localX = (float) (mouseX - graphLeft);
            float localY = (float) (mouseY - graphTop);
            offsetX += localX / scale - localX / newScale;
            offsetY += localY / scale - localY / newScale;
            scale = newScale;
            betterrailwaysystem$clampOffsets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        betterrailwaysystem$drawPanel(context);
        List<Text> hoveredTooltip = betterrailwaysystem$renderGraph(context, mouseX, mouseY);
        super.render(context, mouseX, mouseY, delta);
        if (hoveredTooltip != null && !hoveredTooltip.isEmpty()) {
            context.drawTooltip(textRenderer, hoveredTooltip, mouseX + 8, mouseY + 8);
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    private void betterrailwaysystem$layoutBounds() {
        int maxPanelWidth = payload.worldMap() ? 980 : 920;
        int maxPanelHeight = 620;
        panelWidth = Math.max(360, Math.min(width - 24, maxPanelWidth));
        panelHeight = Math.max(280, Math.min(height - 24, maxPanelHeight));
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;

        legendWidth = Math.max(118, Math.min(150, panelWidth / 5));
        legendLeft = panelLeft + panelWidth - legendWidth - 10;
        legendTop = panelTop + 56;
        legendHeight = panelHeight - 90;

        graphLeft = panelLeft + 10;
        graphTop = panelTop + 56;
        graphWidth = Math.max(180, legendLeft - graphLeft - 10);
        graphHeight = panelHeight - 90;
    }

    private void betterrailwaysystem$drawPanel(DrawContext context) {
        context.fill(panelLeft - 1, panelTop - 1, panelLeft + panelWidth + 1, panelTop + panelHeight + 1, 0xFF000000);
        context.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xE01A1A1A);
        context.drawBorder(panelLeft, panelTop, panelWidth, panelHeight, 0xFF6F6F6F);

        Text titleText = payload.worldMap()
                ? Text.translatable("screen.betterrailwaysystem.world_map")
                : Text.literal(payload.title().isBlank() ? "-" : payload.title());
        int titleColor = payload.worldMap() ? 0xFFEBCB8B : (0xFF000000 | payload.titleColor());
        context.drawTextWithShadow(textRenderer, titleText, panelLeft + 10, panelTop + 10, titleColor);

        if (!payload.currentStation().isBlank()) {
            context.drawTextWithShadow(textRenderer, Text.literal(payload.currentStation()), panelLeft + 10, panelTop + 22, 0xFFFFFFFF);
        }

        context.drawTextWithShadow(textRenderer, Text.translatable("screen.betterrailwaysystem.search_station"), graphLeft, panelTop + 36, 0xFFFFFFFF);

        context.fill(graphLeft - 1, graphTop - 1, graphLeft + graphWidth + 1, graphTop + graphHeight + 1, 0xFF000000);
        context.fill(graphLeft, graphTop, graphLeft + graphWidth, graphTop + graphHeight, 0xAA111111);

        context.fill(legendLeft - 1, legendTop - 1, legendLeft + legendWidth + 1, legendTop + legendHeight + 1, 0xFF000000);
        context.fill(legendLeft, legendTop, legendLeft + legendWidth, legendTop + legendHeight, 0x66101010);
    }

    private List<Text> betterrailwaysystem$renderGraph(DrawContext context, int mouseX, int mouseY) {
        context.enableScissor(graphLeft, graphTop, graphLeft + graphWidth, graphTop + graphHeight);
        betterrailwaysystem$drawGrid(context);

        float lineWidth = betterrailwaysystem$getLineWidth(graphWidth, graphHeight);
        for (RenderedSegment segment : graphLayout.segments()) {
            for (int index = 1; index < segment.points().size(); index++) {
                Vector2f start = segment.points().get(index - 1);
                Vector2f end = segment.points().get(index);
                float drawX1 = graphLeft + (start.x - offsetX) * scale;
                float drawY1 = graphTop + (start.y - offsetY) * scale;
                float drawX2 = graphLeft + (end.x - offsetX) * scale;
                float drawY2 = graphTop + (end.y - offsetY) * scale;
                betterrailwaysystem$drawLine(context, drawX1, drawY1, drawX2, drawY2, 0xFF000000 | segment.color(), lineWidth);
            }
        }

        for (RenderedCityLabel cityLabel : graphLayout.cityLabels()) {
            int labelX = Math.round(graphLeft + (cityLabel.x() - offsetX) * scale);
            int labelY = Math.round(graphTop + (cityLabel.y() - offsetY) * scale);
            int labelWidth = Math.round(cityLabel.width());
            context.fill(labelX - labelWidth / 2 - 4, labelY - 3, labelX + labelWidth / 2 + 4, labelY + 10, 0xAA000000);
            context.drawCenteredTextWithShadow(textRenderer, cityLabel.cityName(), labelX, labelY, 0xFFEBCB8B);
        }

        List<Text> hoveredTooltip = null;
        for (RenderedNode node : graphLayout.nodes()) {
            boolean searchMatch = betterrailwaysystem$matchesSearch(node);
            int size = node.transfer() ? 10 : 8;
            if (node.current()) {
                size = 12;
            }
            if (searchMatch) {
                size += 2;
            }
            int centerX = Math.round(graphLeft + (node.x() - offsetX) * scale);
            int centerY = Math.round(graphTop + (node.y() - offsetY) * scale);
            int x = centerX - size / 2;
            int y = centerY - size / 2;
            int fillColor = node.transfer() ? 0xFFFFFFFF : 0xFF000000 | node.color();
            int borderColor = searchMatch ? 0xFF6BD7FF : (node.current() ? 0xFFFFFF55 : 0xFF222222);
            int textColor = searchMatch ? 0xFF6BD7FF : (node.current() ? 0xFFFFFF55 : 0xFFFFFFFF);
            context.fill(x, y, x + size, y + size, fillColor);
            context.drawBorder(x, y, size, size, borderColor);
            context.drawTextWithShadow(textRenderer, node.stationName(), x + size + 4, y - 1, textColor);
            if (mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size) {
                hoveredTooltip = node.tooltipLines();
            }
        }

        context.disableScissor();
        return hoveredTooltip;
    }

    private void betterrailwaysystem$drawGrid(DrawContext context) {
        float worldGrid = 64.0f;
        float visibleWorldWidth = graphWidth / Math.max(1.0E-4f, scale);
        float visibleWorldHeight = graphHeight / Math.max(1.0E-4f, scale);
        float startWorldX = (float) Math.floor(offsetX / worldGrid) * worldGrid;
        float startWorldY = (float) Math.floor(offsetY / worldGrid) * worldGrid;
        float endWorldX = offsetX + visibleWorldWidth;
        float endWorldY = offsetY + visibleWorldHeight;

        for (float worldX = startWorldX; worldX <= endWorldX + worldGrid; worldX += worldGrid) {
            int drawX = Math.round(graphLeft + (worldX - offsetX) * scale);
            context.fill(drawX, graphTop, drawX + 1, graphTop + graphHeight, 0x22333333);
        }
        for (float worldY = startWorldY; worldY <= endWorldY + worldGrid; worldY += worldGrid) {
            int drawY = Math.round(graphTop + (worldY - offsetY) * scale);
            context.fill(graphLeft, drawY, graphLeft + graphWidth, drawY + 1, 0x22333333);
        }
    }

    private void betterrailwaysystem$drawLine(DrawContext context, float x1, float y1, float x2, float y2, int color, float thickness) {
        if (Math.abs(x1 - x2) < 0.5f) {
            int left = Math.round(x1 - thickness / 2.0f);
            int top = Math.round(Math.min(y1, y2) - thickness / 2.0f);
            int right = Math.round(x1 + thickness / 2.0f + 1.0f);
            int bottom = Math.round(Math.max(y1, y2) + thickness / 2.0f + 1.0f);
            context.fill(left, top, right, bottom, color);
            return;
        }
        if (Math.abs(y1 - y2) < 0.5f) {
            int left = Math.round(Math.min(x1, x2) - thickness / 2.0f);
            int top = Math.round(y1 - thickness / 2.0f);
            int right = Math.round(Math.max(x1, x2) + thickness / 2.0f + 1.0f);
            int bottom = Math.round(y1 + thickness / 2.0f + 1.0f);
            context.fill(left, top, right, bottom, color);
            return;
        }

        float dx = x2 - x1;
        float dy = y2 - y1;
        int steps = Math.max(1, Math.round(Math.max(Math.abs(dx), Math.abs(dy))));
        int half = Math.max(1, Math.round(thickness / 2.0f));
        for (int step = 0; step <= steps; step++) {
            float progress = step / (float) steps;
            int drawX = Math.round(x1 + dx * progress);
            int drawY = Math.round(y1 + dy * progress);
            context.fill(drawX - half, drawY - half, drawX + half + 1, drawY + half + 1, color);
        }
    }

    private boolean betterrailwaysystem$isInsideGraph(double mouseX, double mouseY) {
        return mouseX >= graphLeft && mouseX < graphLeft + graphWidth
                && mouseY >= graphTop && mouseY < graphTop + graphHeight;
    }

    private void betterrailwaysystem$setSearchQuery(String query) {
        searchQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        RenderedNode firstMatch = betterrailwaysystem$findFirstSearchMatch();
        if (firstMatch != null) {
            if (scale < 0.9f) {
                scale = 0.9f;
            }
            betterrailwaysystem$centerOn(firstMatch.x(), firstMatch.y());
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

    private void betterrailwaysystem$fitToBounds(float padding) {
        float graphWorldWidth = Math.max(1.0f, graphLayout.maxX() - graphLayout.minX());
        float graphWorldHeight = Math.max(1.0f, graphLayout.maxY() - graphLayout.minY());
        float availableWidth = Math.max(1.0f, graphWidth - padding * 2.0f);
        float availableHeight = Math.max(1.0f, graphHeight - padding * 2.0f);
        scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, Math.min(availableWidth / graphWorldWidth, availableHeight / graphWorldHeight)));
        float worldViewWidth = graphWidth / scale;
        float worldViewHeight = graphHeight / scale;
        offsetX = graphLayout.minX() - Math.max(0.0f, (worldViewWidth - graphWorldWidth) / 2.0f);
        offsetY = graphLayout.minY() - Math.max(0.0f, (worldViewHeight - graphWorldHeight) / 2.0f);
        betterrailwaysystem$clampOffsets();
    }

    private void betterrailwaysystem$centerOn(float x, float y) {
        offsetX = x - graphWidth / (2.0f * scale);
        offsetY = y - graphHeight / (2.0f * scale);
        betterrailwaysystem$clampOffsets();
    }

    private void betterrailwaysystem$clampOffsets() {
        float worldViewWidth = Math.max(1.0f, graphWidth / Math.max(1.0E-4f, scale));
        float worldViewHeight = Math.max(1.0f, graphHeight / Math.max(1.0E-4f, scale));
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

    private float betterrailwaysystem$getLineWidth(int width, int height) {
        float minSize = Math.max(1.0f, Math.min(width, height));
        return Math.max(1.4f, Math.min(4.6f, minSize / 170.0f));
    }

    private List<LegendItem> betterrailwaysystem$buildLegendItems() {
        List<LegendItem> items = new ArrayList<>();
        Map<String, List<OpenLineMapPayload.LineEntry>> grouped = new LinkedHashMap<>();
        for (OpenLineMapPayload.LineEntry line : payload.lines()) {
            grouped.computeIfAbsent(line.cityName().isBlank() ? "-" : line.cityName(), ignored -> new ArrayList<>()).add(line);
        }

        if (payload.worldMap()) {
            grouped.forEach((cityName, lines) -> {
                items.add(LegendItem.header(cityName));
                for (OpenLineMapPayload.LineEntry line : lines) {
                    items.add(LegendItem.line(betterrailwaysystem$formatLineLabel(line), line.lineColor()));
                }
            });
            return items;
        }

        if (!payload.lines().isEmpty()) {
            OpenLineMapPayload.LineEntry line = payload.lines().getFirst();
            if (!line.cityName().isBlank()) {
                items.add(LegendItem.header(line.cityName()));
            }
            items.add(LegendItem.line(betterrailwaysystem$formatLineLabel(line), line.lineColor()));
        }
        return items;
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
                accumulator.add(44 + stationIndex * 52, fallbackY, line.lineColor(), betterrailwaysystem$formatLineLabel(line), station.pos());
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

    private static String betterrailwaysystem$formatLineLabel(OpenLineMapPayload.LineEntry line) {
        if (line.direction() == null || line.direction().isBlank()) {
            return line.lineId();
        }
        return line.lineId() + " (" + Text.translatable("screen.betterrailwaysystem.direction." + line.direction()).getString() + ")";
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

    private record LegendItem(String label, int color, boolean header) {
        private static LegendItem header(String label) {
            return new LegendItem(label, 0, true);
        }

        private static LegendItem line(String label, int color) {
            return new LegendItem(label, color, false);
        }
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

    private final class LegendListWidget extends ElementListWidget<LegendEntry> {
        private final int left;
        private final int rowWidth;

        private LegendListWidget(MinecraftClient client, int left, int top, int width, int height, List<LegendItem> items) {
            super(client, width, height, top, 16);
            this.left = left;
            this.rowWidth = width - 10;
            setX(left);
            for (LegendItem item : items) {
                addEntry(new LegendEntry(item));
            }
        }

        @Override
        protected void drawMenuListBackground(DrawContext context) {
        }

        @Override
        protected void drawHeaderAndFooterSeparators(DrawContext context) {
        }

        @Override
        protected void renderDecorations(DrawContext context, int mouseX, int mouseY) {
        }

        @Override
        public int getRowWidth() {
            return rowWidth;
        }

        @Override
        protected int getScrollbarX() {
            return left + rowWidth + 4;
        }

        @Override
        public int getRowLeft() {
            return left + 2;
        }
    }

    private final class LegendEntry extends ElementListWidget.Entry<LegendEntry> {
        private final LegendItem item;

        private LegendEntry(LegendItem item) {
            this.item = item;
        }

        @Override
        public List<? extends Element> children() {
            return List.of();
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return List.of();
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            if (item.header()) {
                context.drawTextWithShadow(textRenderer, item.label(), x, y + 4, 0xFFEBCB8B);
                return;
            }
            context.fill(x, y + 3, x + 10, y + 13, 0xFF000000 | item.color());
            context.drawBorder(x, y + 3, 10, 10, 0xFF222222);
            context.drawTextWithShadow(textRenderer, item.label(), x + 14, y + 4, 0xFFFFFFFF);
        }
    }
}
