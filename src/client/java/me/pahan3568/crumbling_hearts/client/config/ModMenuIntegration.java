package me.pahan3568.crumbling_hearts.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.gui.DrawContext;

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
        private static final int HEADER_HEIGHT = 45;
        private static final int FOOTER_HEIGHT = 30;
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
        private int scrollableHeight;
        private int lastWidgetY; // Добавляем поле для отслеживания Y-координаты последнего виджета


        protected ConfigScreen(Screen parent) {
            super(Text.translatable("config.crumbling_hearts.title"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            ModConfig config = ModConfig.getInstance();
            int currentY = START_Y;
            this.scrollableHeight = this.height - HEADER_HEIGHT - FOOTER_HEIGHT; //Вычисляем высоту скролла
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
            lastWidgetY = currentY; // Обновляем lastWidgetY

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
            lastWidgetY = currentY;// Обновляем lastWidgetY

            // Слайдеры
            this.particlesSlider = addSlider(currentY, "particles", config.getParticlesPerHeart(), 1, 128);
            currentY += SPACING;
            lastWidgetY = currentY;// Обновляем lastWidgetY

            this.gravitySlider = addSlider(currentY, "gravity", config.getGravityStrength(), 0.0f, 1.0f);
            currentY += SPACING;
            lastWidgetY = currentY;// Обновляем lastWidgetY

            this.velocitySlider = addSlider(currentY, "velocity", config.getInitialVelocity(), 0.1f, 5.0f);
            currentY += SPACING;
            lastWidgetY = currentY;// Обновляем lastWidgetY

            this.fadeSpeedSlider = addSlider(currentY, "fade_speed", config.getFadeSpeed(), 0.1f, 3.0f);
            lastWidgetY = currentY;// Обновляем lastWidgetY
            // Обновляем contentHeight
            this.contentHeight = lastWidgetY + fadeSpeedSlider.getHeight(); //Вычесляем contentHeight по Y послденего виджета


            // Кнопки внизу экрана
            this.saveButton = ButtonWidget.builder(Text.translatable("gui.done"), button -> {
                if (validateAndSave()) {
                    this.client.setScreen(this.parent);
                }
            }).dimensions(this.width / 2 - 100, this.height - 30, 95, 20).build();


            this.resetButton = ButtonWidget.builder(Text.translatable("controls.reset"), button -> resetToDefaults())
                    .dimensions(this.width / 2 + 5, this.height - 30, 95, 20).build();

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
            this.renderBackground(context);

            // Рендерим заголовок
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("config.crumbling_hearts.title"),
                    this.width / 2, 15,
                    0xFFFFFF);
            MatrixStack matrices = context.getMatrices();
            int maxScroll = Math.max(0, contentHeight - scrollableHeight);
            int scrollOffset = -(int) (maxScroll * scrollPosition);

            // Ограничиваем область отрисовки
            enableScissor(0, HEADER_HEIGHT, this.width, scrollableHeight);

            // Применяем скролл
            matrices.push();
            matrices.translate(0, scrollOffset, 0);

            // Рендерим элементы
            for (Element child : this.children()) {
                if (!(child == resetButton || child == saveButton)) {
                    if (child instanceof Drawable drawable) {
                        ((Drawable)child).render(context, mouseX, mouseY - scrollOffset, delta);
                    }
                }
            }

            // Отрисовка цветовых превью
            renderColorPreview(context, normalColorField);
            renderColorPreview(context, extraColorField);

            // Рендерим лейблы
            renderLabels(context);

            matrices.pop();

            disableScissor();

            // Рендерим кнопки
            resetButton.render(context, mouseX, mouseY, delta);
            saveButton.render(context, mouseX, mouseY, delta);

            // Рендерим скроллбар
            if (maxScroll > 0) {
                renderScrollbar(context, maxScroll, scrollableHeight);
            }

            // Сообщение об ошибке
            if (errorMessage != null) {
                context.drawCenteredTextWithShadow(this.textRenderer,
                        errorMessage,
                        this.width / 2, this.height - 45,
                        0xFF0000);
            }
        }

        private void enableScissor(int x, int y, int width, int height) {
            double scale = this.client.getWindow().getScaleFactor();
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor((int) (x * scale), (int) (this.client.getWindow().getFramebufferHeight() - (y + height) * scale),
                    (int) (width * scale), (int) (height * scale));
        }

        private void disableScissor() {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }

        private void renderLabels(DrawContext context) {
            int currentY = START_Y - LABEL_OFFSET;
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

        private void drawLabel(DrawContext context, String translationKey, int y) {
            context.drawTextWithShadow(this.textRenderer, Text.translatable(translationKey), this.width / 2 - 100, y, 0xFFFFFF);
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
            config.setParticlesPerHeart((int) particlesSlider.getCurrentValue());
            config.setGravityStrength(gravitySlider.getCurrentValue());
            config.setInitialVelocity(velocitySlider.getCurrentValue());
            config.setFadeSpeed(fadeSpeedSlider.getCurrentValue());

            return true;
        }

        private CustomSliderWidget addSlider(int y, String key, float value, float min, float max) {
            CustomSliderWidget slider = new CustomSliderWidget(
                    this.width / 2 - 100, y,
                    200, 20,
                    key, value, min, max
            );
            return slider;
        }

        private void resetToDefaults() {
            ModConfig config = ModConfig.getInstance();
            normalColorField.setText("#FF0000");
            extraColorField.setText("#FFFF00");
            ((CustomSliderWidget) particlesSlider).setValueFromFloat(64);
            ((CustomSliderWidget) particlesSlider).applyValue();
            ((CustomSliderWidget) particlesSlider).updateMessage();
            ((CustomSliderWidget) gravitySlider).setValueFromFloat(0.04f);
            ((CustomSliderWidget) gravitySlider).applyValue();
            ((CustomSliderWidget) gravitySlider).updateMessage();
            ((CustomSliderWidget) velocitySlider).setValueFromFloat(1.0f);
            ((CustomSliderWidget) velocitySlider).applyValue();
            ((CustomSliderWidget) velocitySlider).updateMessage();
            ((CustomSliderWidget) fadeSpeedSlider).setValueFromFloat(1.0f);
            ((CustomSliderWidget) fadeSpeedSlider).applyValue();
            ((CustomSliderWidget) fadeSpeedSlider).updateMessage();
        }

        private void renderColorPreview(DrawContext context, TextFieldWidget field) {
            int x = field.getX() + field.getWidth() + 10;
            int y = field.getY();
            int color;

            try {
                String colorText = field.getText();
                if (HEX_PATTERN.matcher(colorText).matches()) {
                    color = Integer.parseInt(colorText.substring(1), 16) | 0xFF000000;
                } else {
                    color = 0xFF888888;
                }
            } catch (Exception e) {
                color = 0xFF888888;
            }
            //Используем DrawContext
            context.fill(x - 1, y - 1, x + COLOR_PREVIEW_SIZE + 1, y + COLOR_PREVIEW_SIZE + 1, 0xFF000000);
            context.fill(x, y, x + COLOR_PREVIEW_SIZE, y + COLOR_PREVIEW_SIZE, color);
        }

        private void renderScrollbar(DrawContext context, int maxScroll, int scrollableHeight) {
            int scrollBarHeight = Math.max(32, scrollableHeight * scrollableHeight / contentHeight);
            int scrollBarY = HEADER_HEIGHT + (int) ((scrollableHeight - scrollBarHeight) * scrollPosition);
            //Используем DrawContext
            context.fill(this.width - 10, HEADER_HEIGHT, this.width - 4, this.height - FOOTER_HEIGHT, 0x33FFFFFF); //отступы сверху и снизу
            context.fill(this.width - 9, scrollBarY, this.width - 5, scrollBarY + scrollBarHeight, 0xFFAAAAAA);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            // Проверяем клик по скроллбару
            if (button == 0 && mouseX >= this.width - 10 && mouseX <= this.width - 4
                    && mouseY >= HEADER_HEIGHT && mouseY <= this.height - FOOTER_HEIGHT) {
                isDragging = true;
                updateScrollFromMouse(mouseY);
                return true;
            }

            int maxScroll = Math.max(0, contentHeight - scrollableHeight);
            int scrollOffset = (int) (maxScroll * scrollPosition);

            // Проверяем клики по кнопкам отдельно, не учитывая смещение
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

            int maxScroll = Math.max(0, contentHeight - (this.height - SCISSOR_PADDING - 40));
            int scrollOffset = (int) (maxScroll * scrollPosition);

            // Для остальных элементов учитываем скролл
            return super.mouseDragged(mouseX, mouseY + scrollOffset, button, deltaX, deltaY);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
            // Ограничиваем скорость скроллинга
            double scrollAmount = Math.max(-0.25, Math.min(0.25, amount * 0.1));
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
