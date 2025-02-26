package me.pahan3568.crumbling_hearts.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Drawable;
import net.minecraft.text.Text;
import java.util.regex.Pattern;

public class ModMenuIntegration implements ModMenuApi {
    private static final Pattern HEX_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new ConfigScreen(parent);
    }

    private static class ConfigScreen extends Screen {
        private static final int SPACING = 40;
        private static final int START_Y = 60;
        private static final int LABEL_OFFSET = 15;
        private static final int COLOR_PREVIEW_SIZE = 20;
        private static final int BOTTOM_PADDING = 5;
        private static final int HEADER_HEIGHT = 45;
        private static final int SCISSOR_PADDING = 45;

        private final Screen parent;
        private TextFieldWidget normalColorField;
        private TextFieldWidget extraColorField;
        private CustomSliderWidget particlesSlider;
        private CustomSliderWidget gravitySlider;
        private CustomSliderWidget velocitySlider;
        private CustomSliderWidget fadeSpeedSlider;
        private Text errorMessage = null;
        private ButtonWidget resetButton;
        private double scrollPosition = 0;
        private boolean isDragging = false;
        private int contentHeight;
        private ButtonWidget saveButton;

        protected ConfigScreen(Screen parent) {
            super(Text.translatable("config.crumbling_hearts.title"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            ModConfig config = ModConfig.getInstance();
            int currentY = START_Y;

            // Цвет обычных сердец
            this.normalColorField = new TextFieldWidget(
                this.textRenderer, 
                this.width / 2 - 100, currentY, 
                200, 20, 
                Text.translatable("config.crumbling_hearts.option.normal_heart_color")
            );
            normalColorField.setText(config.getNormalHeartColorString());
            normalColorField.setMaxLength(7);
            currentY += SPACING;

            // Цвет сердец поглощения
            this.extraColorField = new TextFieldWidget(
                this.textRenderer, 
                this.width / 2 - 100, currentY, 
                200, 20, 
                Text.translatable("config.crumbling_hearts.option.extra_heart_color")
            );
            extraColorField.setText(config.getExtraHeartColorString());
            extraColorField.setMaxLength(7);
            currentY += SPACING;

            // Слайдеры
            this.particlesSlider = addSlider(currentY, "particles", config.getParticlesPerHeart(), 1, 128);
            currentY += SPACING;
            
            this.gravitySlider = addSlider(currentY, "gravity", config.getGravityStrength(), 0.0f, 1.0f);
            currentY += SPACING;
            
            this.velocitySlider = addSlider(currentY, "velocity", config.getInitialVelocity(), 0.1f, 5.0f);
            currentY += SPACING;
            
            this.fadeSpeedSlider = addSlider(currentY, "fade_speed", config.getFadeSpeed(), 0.1f, 3.0f);

            // Обновляем contentHeight
            this.contentHeight = fadeSpeedSlider.getY() + fadeSpeedSlider.getHeight();

            // Кнопки внизу экрана
            this.saveButton = ButtonWidget.builder(
                Text.translatable("gui.done"),
                button -> {
                    if (validateAndSave()) {
                        this.client.setScreen(this.parent);
                    }
                }
            )
            .dimensions(this.width / 2 - 100, this.height - 25, 95, 20)
            .build();

            this.resetButton = ButtonWidget.builder(
                Text.translatable("controls.reset"),
                button -> resetToDefaults()
            )
            .dimensions(this.width / 2 + 5, this.height - 25, 95, 20)
            .build();

            addDrawableChild(normalColorField);
            addDrawableChild(extraColorField);
            addDrawableChild(particlesSlider);
            addDrawableChild(gravitySlider);
            addDrawableChild(velocitySlider);
            addDrawableChild(fadeSpeedSlider);
            addDrawableChild(saveButton);
            addDrawableChild(resetButton);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            this.renderBackground(context, mouseX, mouseY, delta);
            
            // Рендерим заголовок отдельно (вне области скроллинга)
            context.drawCenteredTextWithShadow(this.textRenderer, 
                Text.translatable("config.crumbling_hearts.title"),
                this.width / 2, 15,
                0xFFFFFF);
            
            int maxScroll = Math.max(0, contentHeight - (this.height - SCISSOR_PADDING));
            int scrollOffset = -(int)(maxScroll * scrollPosition);
            
            // Применяем скролл только к контенту
            context.enableScissor(0, HEADER_HEIGHT, this.width, this.height - 35);
            context.getMatrices().push();
            context.getMatrices().translate(0, scrollOffset, 0);
            
            // Рендерим скроллируемые элементы
            for (Element child : this.children()) {
                if (!(child == resetButton || child == saveButton)) {
                    if (child instanceof Drawable drawable) {
                        drawable.render(context, mouseX, mouseY - scrollOffset, delta);
                    }
                }
            }

            // Отрисовка цветовых превью
            renderColorPreview(context, normalColorField);
            renderColorPreview(context, extraColorField);

            // Тексты
            renderLabels(context);

            context.getMatrices().pop();
            context.disableScissor();

            // Рендерим кнопки и скроллбар поверх всего
            resetButton.render(context, mouseX, mouseY, delta);
            saveButton.render(context, mouseX, mouseY, delta);

            if (maxScroll > 0) {
                renderScrollbar(context, maxScroll);
            }

            // Сообщение об ошибке
            if (errorMessage != null) {
                context.drawCenteredTextWithShadow(this.textRenderer, 
                    errorMessage,
                    this.width / 2, this.height - 45,
                    0xFF0000);
            }
        }

        private void renderLabels(DrawContext context) {
            int currentY = START_Y - LABEL_OFFSET;

            // Названия полей
            drawLabel(context, "config.crumbling_hearts.option.normal_heart_color", currentY);
            currentY += SPACING;
            
            drawLabel(context, "config.crumbling_hearts.option.extra_heart_color", currentY);
            currentY += SPACING;
            
            drawLabel(context, "config.crumbling_hearts.option.particles", currentY);
            currentY += SPACING;
            
            drawLabel(context, "config.crumbling_hearts.option.gravity", currentY);
            currentY += SPACING;
            
            drawLabel(context, "config.crumbling_hearts.option.velocity", currentY);
            currentY += SPACING;
            
            drawLabel(context, "config.crumbling_hearts.option.fade_speed", currentY);
        }

        private void drawLabel(net.minecraft.client.gui.DrawContext context, String translationKey, int y) {
            context.drawTextWithShadow(this.textRenderer, 
                Text.translatable(translationKey),
                this.width / 2 - 100, y, 
                0xFFFFFF);
        }

        private boolean validateAndSave() {
            String normalColor = normalColorField.getText();
            String extraColor = extraColorField.getText();

            if (!HEX_PATTERN.matcher(normalColor).matches() || !HEX_PATTERN.matcher(extraColor).matches()) {
                errorMessage = Text.translatable("config.crumbling_hearts.error.invalid_color");
                return false;
            }

            ModConfig config = ModConfig.getInstance();
            
            // Сохраняем цвета
            config.setNormalHeartColor(normalColor);
            config.setExtraHeartColor(extraColor);

            // Сохраняем значения слайдеров
            config.setParticlesPerHeart((int)particlesSlider.getCurrentValue());
            config.setGravityStrength(gravitySlider.getCurrentValue());
            config.setInitialVelocity(velocitySlider.getCurrentValue());
            config.setFadeSpeed(fadeSpeedSlider.getCurrentValue());

            return true;
        }

        private CustomSliderWidget addSlider(int y, String key, float value, float min, float max) {
            return new CustomSliderWidget(
                this.width / 2 - 100, y,
                200, 20,
                key, value, min, max
            );
        }

        private void resetToDefaults() {
            ModConfig config = ModConfig.getInstance();
            normalColorField.setText("#FF0000");
            extraColorField.setText("#FFFF00");
            ((CustomSliderWidget)particlesSlider).setValueFromFloat(64);
            ((CustomSliderWidget)gravitySlider).setValueFromFloat(0.04f);
            ((CustomSliderWidget)velocitySlider).setValueFromFloat(1.0f);
            ((CustomSliderWidget)fadeSpeedSlider).setValueFromFloat(1.0f);
        }

        private void renderColorPreview(net.minecraft.client.gui.DrawContext context, TextFieldWidget field) {
            int x = field.getX() + field.getWidth() + 10;
            int y = field.getY();
            int color;

            try {
                String colorText = field.getText();
                if (HEX_PATTERN.matcher(colorText).matches()) {
                    color = Integer.parseInt(colorText.substring(1), 16) | 0xFF000000;
                } else {
                    color = 0xFF888888; // Серый цвет для некорректного значения
                }
            } catch (Exception e) {
                color = 0xFF888888;
            }

            // Рамка
            context.fill(x - 1, y - 1, x + COLOR_PREVIEW_SIZE + 1, y + COLOR_PREVIEW_SIZE + 1, 0xFF000000);
            // Цвет
            context.fill(x, y, x + COLOR_PREVIEW_SIZE, y + COLOR_PREVIEW_SIZE, color);
        }

        private void renderScrollbar(DrawContext context, int maxScroll) {
            int scrollBarHeight = Math.max(32, (this.height - 80) * (this.height - 80) / contentHeight);
            int scrollBarY = 40 + (int)((this.height - 80 - scrollBarHeight) * scrollPosition);
            
            // Фон скроллбара
            context.fill(this.width - 10, 40, this.width - 4, this.height - 40, 0x33FFFFFF);
            // Ползунок
            context.fill(this.width - 9, scrollBarY, this.width - 5, scrollBarY + scrollBarHeight, 0xFFAAAAAA);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            // Проверяем клик по скроллбару
            if (button == 0 && mouseX >= this.width - 10 && mouseX <= this.width - 4 
                && mouseY >= 40 && mouseY <= this.height - 40) {
                isDragging = true;
                updateScrollFromMouse(mouseY);
                return true;
            }

            // Применяем смещение к координатам мыши для скроллируемых элементов
            int maxScroll = Math.max(0, contentHeight - (this.height - 120));
            int scrollOffset = (int)(maxScroll * scrollPosition);
            
            // Проверяем клики по кнопкам отдельно
            if (resetButton.mouseClicked(mouseX, mouseY, button)) return true;
            if (saveButton.mouseClicked(mouseX, mouseY, button)) return true;

            // Для остальных элементов учитываем скролл
            return super.mouseClicked(mouseX, mouseY + scrollOffset, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            if (isDragging) {
                updateScrollFromMouse(mouseY);
                return true;
            }

            // Применяем смещение к координатам мыши для скроллируемых элементов
            int maxScroll = Math.max(0, contentHeight - (this.height - 120));
            int scrollOffset = (int)(maxScroll * scrollPosition);
            
            return super.mouseDragged(mouseX, mouseY + scrollOffset, button, deltaX, deltaY);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
            // Ограничиваем скорость скроллинга
            double scrollAmount = Math.max(-0.25, Math.min(0.25, verticalAmount * 0.1));
            updateScroll(scrollPosition - scrollAmount);
            return true;
        }

        private void updateScrollFromMouse(double mouseY) {
            double scrollableHeight = this.height - 80;
            scrollPosition = (mouseY - 40) / scrollableHeight;
            updateScroll(scrollPosition);
        }

        private void updateScroll(double newPosition) {
            scrollPosition = Math.max(0.0, Math.min(1.0, newPosition));
        }
    }

    private static class CustomSliderWidget extends SliderWidget {
        private final String key;
        private final float min;
        private final float max;
        private final String translationKey;

        public CustomSliderWidget(int x, int y, int width, int height, String key, float value, float min, float max) {
            super(x, y, width, height, Text.empty(), (value - min) / (max - min));
            this.key = key;
            this.min = min;
            this.max = max;
            this.translationKey = "config.crumbling_hearts.option." + key;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            String valueStr = key.equals("particles") 
                ? String.format("%d", (int)(min + (max - min) * (float)value))
                : String.format("%.2f", min + (max - min) * (float)value);
            setMessage(Text.translatable(translationKey).append(": " + valueStr));
        }

        @Override
        protected void applyValue() {
            ModConfig config = ModConfig.getInstance();
            float actualValue = min + (max - min) * (float)value;
            if (key.equals("particles")) {
                config.setParticlesPerHeart((int)actualValue);
            } else if (key.equals("gravity")) {
                config.setGravityStrength(actualValue);
            } else if (key.equals("velocity")) {
                config.setInitialVelocity(actualValue);
            } else if (key.equals("fade_speed")) {
                config.setFadeSpeed(actualValue);
            }
        }

        public void setValueFromFloat(float newValue) {
            this.value = (newValue - min) / (max - min);
            updateMessage();
        }

        public float getCurrentValue() {
            return min + (max - min) * (float)value;
        }
    }
} 