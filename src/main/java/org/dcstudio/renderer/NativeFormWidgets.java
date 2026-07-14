package org.dcstudio.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.text.Text;

import java.util.List;

// 为原生配置界面提供滚动表单和常用行组件。
final class NativeFormWidgets {
    private NativeFormWidgets() {
    }

    static final int ROW_HEIGHT = 28;

    static FormListWidget createFormList(MinecraftClient client, int left, int top, int width, int height, int rowWidth) {
        return new FormListWidget(client, left, top, width, height, rowWidth);
    }

    static final class FormListWidget extends ElementListWidget<RowEntry> {
        private final int left;
        private final int rowWidth;

        private FormListWidget(MinecraftClient client, int left, int top, int width, int height, int rowWidth) {
            super(client, width, height, top, ROW_HEIGHT);
            this.left = left;
            this.rowWidth = rowWidth;
            setX(left);
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
            return left + rowWidth + 8;
        }

        @Override
        public int getRowLeft() {
            return left;
        }

        public void setEntries(List<RowEntry> entries) {
            replaceEntries(entries);
            refreshScroll();
        }
    }

    abstract static class RowEntry extends ElementListWidget.Entry<RowEntry> {
        @Override
        public List<? extends Element> children() {
            return List.of();
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return List.of();
        }
    }

    static final class LabeledWidgetEntry extends RowEntry {
        private final Text label;
        private final ClickableWidget widget;
        private final int labelWidth;

        LabeledWidgetEntry(Text label, ClickableWidget widget, int labelWidth) {
            this.label = label;
            this.widget = widget;
            this.labelWidth = labelWidth;
        }

        @Override
        public List<? extends Element> children() {
            return List.of(widget);
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return List.of(widget);
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int textY = y + (entryHeight - 8) / 2;
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, label, x + 4, textY, 0xFFFFFF);
            int widgetX = x + labelWidth;
            int widgetY = y + 4;
            widget.setDimensionsAndPosition(Math.max(40, entryWidth - labelWidth - 4), 20, widgetX, widgetY);
            widget.render(context, mouseX, mouseY, tickDelta);
        }
    }

    static final class LabeledDualWidgetEntry extends RowEntry {
        private final Text label;
        private final ClickableWidget fieldWidget;
        private final ClickableWidget buttonWidget;
        private final int labelWidth;
        private final int buttonWidth;

        LabeledDualWidgetEntry(Text label, ClickableWidget fieldWidget, ClickableWidget buttonWidget, int labelWidth, int buttonWidth) {
            this.label = label;
            this.fieldWidget = fieldWidget;
            this.buttonWidget = buttonWidget;
            this.labelWidth = labelWidth;
            this.buttonWidth = buttonWidth;
        }

        @Override
        public List<? extends Element> children() {
            return List.of(fieldWidget, buttonWidget);
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return List.of(fieldWidget, buttonWidget);
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int textY = y + (entryHeight - 8) / 2;
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, label, x + 4, textY, 0xFFFFFF);
            int widgetY = y + 4;
            int fieldX = x + labelWidth;
            int gap = 6;
            int fieldWidth = Math.max(40, entryWidth - labelWidth - buttonWidth - gap - 4);
            fieldWidget.setDimensionsAndPosition(fieldWidth, 20, fieldX, widgetY);
            buttonWidget.setDimensionsAndPosition(buttonWidth, 20, fieldX + fieldWidth + gap, widgetY);
            fieldWidget.render(context, mouseX, mouseY, tickDelta);
            buttonWidget.render(context, mouseX, mouseY, tickDelta);
        }
    }

    static final class FullWidthWidgetEntry extends RowEntry {
        private final ClickableWidget widget;

        FullWidthWidgetEntry(ClickableWidget widget) {
            this.widget = widget;
        }

        @Override
        public List<? extends Element> children() {
            return List.of(widget);
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return List.of(widget);
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            widget.setDimensionsAndPosition(Math.max(40, entryWidth - 8), 20, x + 4, y + 4);
            widget.render(context, mouseX, mouseY, tickDelta);
        }
    }

    static final class HintEntry extends RowEntry {
        private final Text text;

        HintEntry(Text text) {
            this.text = text;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, text, x + 4, y + 8, 0xFFA0A0A0);
        }
    }
}
