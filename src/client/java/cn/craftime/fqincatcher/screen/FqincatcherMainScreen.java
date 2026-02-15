package cn.craftime.fqincatcher.screen;

import cn.craftime.fqincatcher.store.FqincatcherMapProfile;
import cn.craftime.fqincatcher.store.FqincatcherMapStore;
import cn.craftime.fqincatcher.copy.FqincatcherCopyController;
import cn.craftime.fqincatcher.seed.FqincatcherSeedCrackerController;
import cn.craftime.fqincatcher.world.FqincatcherWorldShell;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;

public final class FqincatcherMainScreen extends Screen {
    private final Screen parent;
    private FqincatcherMapStore store;

    private MapListWidget listWidget;
    private ButtonWidget createButton;
    private ButtonWidget deleteButton;

    private ButtonWidget crackSeedButton;
    private TextFieldWidget seedField;
    private ButtonWidget saveSeedButton;

    private ButtonWidget generateMapButton;
    private ButtonWidget startCopyButton;
    private ButtonWidget pauseCopyButton;

    public FqincatcherMainScreen(Screen parent) {
        super(Text.literal("FQinCatcher"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.store = FqincatcherMapStore.get();

        int left = 10;
        int top = 30;
        int listWidth = Math.min(220, this.width / 2 - 20);
        int bottomBarHeight = 90;
        int listBottom = this.height - bottomBarHeight - 10;

        this.listWidget = new MapListWidget(this.client, listWidth, listBottom - top, top, 24);
        this.listWidget.setX(left);
        this.addSelectableChild(this.listWidget);

        int sideX = left + listWidth + 10;
        int sideW = 110;
        this.createButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("新建地图"), b -> onCreate())
                .dimensions(sideX, top, sideW, 20)
                .build());
        this.deleteButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("删除地图"), b -> onDelete())
                .dimensions(sideX, top + 24, sideW, 20)
                .build());

        int bottomTop = listBottom + 10;
        int rowH = 20;
        int gap = 6;

        int seedRowY = bottomTop;
        this.crackSeedButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("服务器种子破解"), b -> onCrackSeed())
                .dimensions(left, seedRowY, 110, rowH)
                .build());
        this.seedField = this.addDrawableChild(new TextFieldWidget(this.textRenderer, left + 110 + gap, seedRowY, 160, rowH, Text.literal("服务器种子")));
        this.seedField.setMaxLength(32);
        this.saveSeedButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("保存"), b -> onSaveSeed())
                .dimensions(left + 110 + gap + 160 + gap, seedRowY, 60, rowH)
                .build());

        int copyRowY = bottomTop + rowH + gap;
        this.generateMapButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("生成地图"), b -> onGenerateMap())
                .dimensions(left, copyRowY, 90, rowH)
                .build());
        this.startCopyButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("开始复制"), b -> onStartCopy())
                .dimensions(left + 90 + gap, copyRowY, 90, rowH)
                .build());
        this.pauseCopyButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("暂停复制"), b -> onPauseCopy())
                .dimensions(left + (90 + gap) * 2, copyRowY, 90, rowH)
                .build());

        refreshList();
        refreshSelectionUi();
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
        this.listWidget.renderWidget(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }

    private void refreshList() {
        List<FqincatcherMapProfile> maps = this.store.list();
        this.listWidget.setMaps(this, maps);
        this.store.selected().ifPresentOrElse(selected -> this.listWidget.selectById(selected.id), () -> this.listWidget.setSelected(null));
    }

    private void refreshSelectionUi() {
        Optional<FqincatcherMapProfile> selected = this.store.selected();
        boolean hasSelection = selected.isPresent();
        this.deleteButton.active = hasSelection;
        this.crackSeedButton.active = hasSelection;
        this.seedField.setEditable(hasSelection);
        this.saveSeedButton.active = hasSelection;
        this.generateMapButton.active = hasSelection;
        this.startCopyButton.active = hasSelection;
        this.pauseCopyButton.active = hasSelection;

        String seed = selected.map(m -> m.seed == null ? "" : Long.toString(m.seed)).orElse("");
        if (!this.seedField.getText().equals(seed)) {
            this.seedField.setText(seed);
        }
    }

    private void onCreate() {
        int index = this.store.list().size() + 1;
        this.store.create("新地图 " + index);
        refreshList();
        refreshSelectionUi();
        chat("已新建地图");
    }

    private void onDelete() {
        Optional<FqincatcherMapProfile> selected = this.store.selected();
        if (selected.isEmpty()) {
            chat("请先选中一个地图");
            return;
        }
        String id = selected.get().id;
        this.store.deleteWorldFolderIfExists(id);
        this.store.delete(id);
        refreshList();
        refreshSelectionUi();
        chat("已删除地图");
    }

    private void onCrackSeed() {
        FqincatcherSeedCrackerController.start();
    }

    private void onSaveSeed() {
        Optional<FqincatcherMapProfile> selected = this.store.selected();
        if (selected.isEmpty()) {
            chat("请先选中一个地图");
            return;
        }
        String raw = this.seedField.getText().trim();
        if (raw.isEmpty()) {
            this.store.setSeed(selected.get().id, null);
            chat("已清空种子");
            refreshSelectionUi();
            return;
        }
        try {
            long seed = Long.parseLong(raw);
            this.store.setSeed(selected.get().id, seed);
            chat("已保存种子: " + seed);
        } catch (NumberFormatException e) {
            chat("种子格式不正确，请输入整数");
        }
        refreshSelectionUi();
    }

    private void onGenerateMap() {
        FqincatcherWorldShell.generateForSelected();
    }

    private void onStartCopy() {
        FqincatcherCopyController.start();
    }

    private void onPauseCopy() {
        FqincatcherCopyController.pause();
    }

    private void onSelect(FqincatcherMapProfile map) {
        this.store.select(map.id);
        refreshSelectionUi();
    }

    private void chat(String message) {
        MinecraftClient client = this.client;
        if (client == null || client.player == null) {
            return;
        }
        client.player.sendMessage(Text.literal("[FQinCatcher] " + message), false);
    }

    private static final class MapListWidget extends AlwaysSelectedEntryListWidget<MapListWidget.MapEntry> {
        MapListWidget(MinecraftClient client, int width, int height, int y, int itemHeight) {
            super(client, width, height, y, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return this.width;
        }

        @Override
        protected int getScrollbarX() {
            return this.getX() + this.width - 6;
        }

        void setMaps(FqincatcherMainScreen screen, List<FqincatcherMapProfile> maps) {
            this.clearEntries();
            for (FqincatcherMapProfile map : maps) {
                this.addEntry(new MapEntry(screen, map));
            }
        }

        void selectById(String mapId) {
            if (mapId == null) {
                this.setSelected(null);
                return;
            }
            for (MapEntry entry : this.children()) {
                if (entry.map.id.equals(mapId)) {
                    this.setSelected(entry);
                    return;
                }
            }
            this.setSelected(null);
        }

        private static final class MapEntry extends AlwaysSelectedEntryListWidget.Entry<MapEntry> {
            private final FqincatcherMainScreen screen;
            private final FqincatcherMapProfile map;

            private MapEntry(FqincatcherMainScreen screen, FqincatcherMapProfile map) {
                this.screen = screen;
                this.map = map;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                String name = map.name == null ? "未命名地图" : map.name;
                String seed = map.seed == null ? "-" : Long.toString(map.seed);
                String line2 = "种子: " + seed;
                context.drawTextWithShadow(screen.textRenderer, Text.literal(name), x + 4, y + 4, 0xFFFFFF);
                context.drawTextWithShadow(screen.textRenderer, Text.literal(line2), x + 4, y + 4 + 10, 0xA0A0A0);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    screen.onSelect(this.map);
                    screen.listWidget.setSelected(this);
                    return true;
                }
                return false;
            }

            @Override
            public Text getNarration() {
                String name = map.name == null ? "未命名地图" : map.name;
                return Text.literal(name);
            }
        }
    }
}

