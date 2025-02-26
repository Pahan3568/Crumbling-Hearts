package me.pahan3568.crumbling_hearts.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import me.pahan3568.crumbling_hearts.client.config.ModConfig;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Crumbling_heartsClient implements ClientModInitializer {
    private static final List<HeartParticle> particles = new ArrayList<>();
    private static final int MAX_PARTICLES = 512; // Максимальное количество частиц
    private static float lastHealth = -1;
    private static float lastAbsorption = -1;

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (MinecraftClient.getInstance().player == null) {
                return;
            }
            drawContext.getMatrices().push();
            drawContext.getMatrices().translate(0, 0, 1000);
            renderHeartParticles(drawContext);
            drawContext.getMatrices().pop();
        });


        // Загружаем конфиг при старте
        ModConfig.getInstance();
    }

    private void renderHeartParticles(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;

        if (player == null) return;
        if (client.options.hudHidden) return;

        float currentHealth = player.getHealth();
        float currentAbsorption = player.getAbsorptionAmount();

        // Инициализация при первом запуске
        if (lastHealth < 0) {
            lastHealth = currentHealth;
            lastAbsorption = currentAbsorption;
            return;
        }

        int baseX = client.getWindow().getScaledWidth() / 2 - 91;
        int baseY = client.getWindow().getScaledHeight() - 39;
        ModConfig config = ModConfig.getInstance();

        // Проверяем урон по обычному здоровью
        if (currentHealth < lastHealth) {
            float damageTaken = lastHealth - currentHealth;
            int heartsLost = MathHelper.ceil(damageTaken / 2.0f);
            int startHeart = MathHelper.ceil(lastHealth / 2.0f) - heartsLost;

            int availableParticles = MAX_PARTICLES - particles.size();
            int particlesPerDamagedHeart = Math.max(1,
                    Math.min(
                            config.getParticlesPerHeart(),
                            availableParticles / heartsLost
                    )
            );

            for (int i = 0; i < heartsLost; i++) {
                int heartIndex = startHeart + i;
                // Minecraft накладывает сердца каждые 2 ряда (после 20 сердец)
                int visualRow = (heartIndex / 10) % 2;
                int stackLevel = heartIndex / 20;
                int col = heartIndex % 10;

                int heartX = baseX + col * 8;
                int heartY = baseY - stackLevel * 6 - visualRow * 3;

                createParticlesForHeart(heartX, heartY, config.getNormalHeartColor(), particlesPerDamagedHeart, config);
            }
        }

        // Проверяем урон по поглощению
        if (currentAbsorption < lastAbsorption) {
            float absorptionDamage = lastAbsorption - currentAbsorption;
            int absorptionHeartsLost = MathHelper.ceil(absorptionDamage / 2.0f);
            int maxHealth = MathHelper.ceil(player.getMaxHealth() / 2.0f);
            int startAbsorptionHeart = maxHealth + MathHelper.ceil(lastAbsorption / 2.0f) - absorptionHeartsLost;

            int availableParticles = MAX_PARTICLES - particles.size();
            int particlesPerDamagedHeart = Math.max(1,
                    Math.min(
                            config.getParticlesPerHeart(),
                            availableParticles / absorptionHeartsLost
                    )
            );

            for (int i = 0; i < absorptionHeartsLost; i++) {
                int heartIndex = startAbsorptionHeart + i;
                int visualRow = (heartIndex / 10) % 2;
                int stackLevel = heartIndex / 20;
                int col = heartIndex % 10;

                int heartX = baseX + col * 8;
                int heartY = baseY - stackLevel * 6 - visualRow * 3;

                createParticlesForHeart(heartX, heartY, config.getExtraHeartColor(), particlesPerDamagedHeart, config);
            }
        }

        lastHealth = currentHealth;
        lastAbsorption = currentAbsorption;

        // Обновляем существующие частицы
        Iterator<HeartParticle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            HeartParticle particle = iterator.next();
            particle.update();
            particle.render(context);

            if (particle.isDead()) {
                iterator.remove();
            }
        }
    }

    private void createParticlesForHeart(int heartX, int heartY, int color, int particleCount, ModConfig config) {
        for (int j = 0; j < particleCount; j++) {
            particles.add(new HeartParticle(
                    heartX + (int)(Math.random() * 8),
                    heartY + (int)(Math.random() * 8),
                    (float)(Math.random() * config.getInitialVelocity() - config.getInitialVelocity()/2),
                    (float)(-Math.random() * config.getInitialVelocity()),
                    color
            ));
        }
    }

    private static class HeartParticle {
        private float x, y;
        private final float velocityX;
        private float velocityY;
        private float lifetime;
        private final int size;
        private final float maxLifetime;
        private final int color;

        public HeartParticle(float x, float y, float velocityX, float velocityY, int color) {
            this.x = x;
            this.y = y;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.color = color;
            this.size = (int)(Math.random() * 2) + 1;
            this.maxLifetime = (float)((Math.random() * 20) + 40);
            this.lifetime = this.maxLifetime;
        }

        public void update() {
            x += velocityX;
            y += velocityY;
            velocityY += ModConfig.getInstance().getGravityStrength();
            lifetime -= ModConfig.getInstance().getFadeSpeed();
        }

        public void render(DrawContext context) {
            float alpha = Math.min(lifetime / maxLifetime, 1.0f);
            int alpha8 = Math.max(64, (int)(alpha * 255));

            context.fill(
                    (int) x,
                    (int) y,
                    (int) x + size,
                    (int) y + size,
                    (alpha8 << 24) | (color & 0xFFFFFF)
            );
        }

        public boolean isDead() {
            return lifetime <= 0;
        }
    }
}
