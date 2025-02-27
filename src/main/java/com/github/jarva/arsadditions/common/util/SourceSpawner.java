package com.github.jarva.arsadditions.common.util;

import com.github.jarva.arsadditions.common.block.tile.SourceSpawnerTile;
import com.github.jarva.arsadditions.common.recipe.SourceSpawnerRecipe;
import com.github.jarva.arsadditions.common.util.codec.TagModifier;
import com.github.jarva.arsadditions.datagen.tags.EntityTypeTagDatagen;
import com.github.jarva.arsadditions.setup.registry.AddonBlockRegistry;
import com.github.jarva.arsadditions.setup.registry.AddonRecipeRegistry;
import com.github.jarva.arsadditions.setup.registry.ModifyTagRegistry;
import com.hollingsworth.arsnouveau.common.block.tile.MobJarTile;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SourceSpawner extends BaseSpawner {
    private final BlockEntity blockEntity;
    public boolean disabled = false;
    public WeightedRandomList<WeightedEntry.Wrapper<SpawnData>> spawnPotentials = WeightedRandomList.create();
    public HashMap<EntityType<?>, SourceSpawnerRecipe> recipes = new HashMap<>();
    public short spawnDelay = 20;
    public short minSpawnDelay = 200;
    public short maxSpawnDelay = 800;
    public short spawnCount = 4;
    public short maxNearbyEntities = 6;
    public short requiredPlayerRange = 16;
    public short spawnRange = 4;

    public SourceSpawner(SourceSpawnerTile sourceSpawnerTile) {
        this.blockEntity = sourceSpawnerTile;
    }

    @Override
    public void load(@Nullable Level level, BlockPos pos, CompoundTag tag) {
        this.spawnDelay = tag.getShort("Delay");

        if (tag.contains("MinSpawnDelay", Tag.TAG_ANY_NUMERIC)) {
            this.minSpawnDelay = tag.getShort("MinSpawnDelay");
            this.maxSpawnDelay = tag.getShort("MaxSpawnDelay");
            this.spawnCount = tag.getShort("SpawnCount");
        }

        if (tag.contains("MaxNearbyEntities", Tag.TAG_ANY_NUMERIC)) {
            this.maxNearbyEntities = tag.getShort("MaxNearbyEntities");
            this.requiredPlayerRange = tag.getShort("RequiredPlayerRange");
        }

        if (tag.contains("SpawnRange", Tag.TAG_ANY_NUMERIC)) {
            this.spawnRange = tag.getShort("SpawnRange");
        }

        this.disabled = tag.getBoolean("Disabled");
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putShort("Delay", this.spawnDelay);
        tag.putShort("MinSpawnDelay", this.minSpawnDelay);
        tag.putShort("MaxSpawnDelay", this.maxSpawnDelay);
        tag.putShort("SpawnCount", this.spawnCount);
        tag.putShort("MaxNearbyEntities", this.maxNearbyEntities);
        tag.putShort("RequiredPlayerRange", this.requiredPlayerRange);
        tag.putShort("SpawnRange", this.spawnRange);
        tag.putBoolean("Disabled", this.disabled);
        return tag;
    }

    @Override
    public void broadcastEvent(Level level, BlockPos pos, int eventId) {
        level.blockEvent(pos, AddonBlockRegistry.SOURCE_SPAWNER.get(), eventId, 0);
    }

    private void delay(Level level, BlockPos pos) {
        RandomSource randomSource = level.getRandom();
        if (this.maxSpawnDelay <= this.minSpawnDelay) {
            this.spawnDelay = this.minSpawnDelay;
        } else {
            this.spawnDelay = (short) (this.minSpawnDelay + randomSource.nextInt(this.maxSpawnDelay - this.minSpawnDelay));
        }
    }

    private static final ModifyTagRegistry.AppendTag APPEND_SOURCE_SPAWNER_TAG = new ModifyTagRegistry.AppendTag(Map.of("Tags", StringTag.valueOf("ars_additions:source_spawner")));

    private SimpleWeightedRandomList<SpawnData> getEntities(ServerLevel level, BlockPos pos) {
        SimpleWeightedRandomList.Builder<SpawnData> builder = SimpleWeightedRandomList.builder();

        for(BlockPos b : BlockPos.withinManhattan(pos, 10, 10, 10)){
            if(level.getBlockEntity(b) instanceof MobJarTile mobJarTile && mobJarTile.getEntity() instanceof LivingEntity livingEntity){
                EntityType<?> type = livingEntity.getType();
                if (type.is(EntityTypeTagDatagen.SOURCE_SPAWNER_DENYLIST)) {
                    continue;
                }

                CompoundTag entityTag = new CompoundTag();
                entityTag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());

                 getRecipe(livingEntity).ifPresent(recipe -> {
                    recipes.put(type, recipe);
                    recipe.tag_modifiers().ifPresent(modifiers -> {
                        livingEntity.saveAsPassenger(entityTag);
                        for (TagModifier modifier : modifiers) {
                            modifier.modify(entityTag);
                        }
                    });
                });

                entityTag.remove("UUID");
                APPEND_SOURCE_SPAWNER_TAG.modify(entityTag);
                builder.add(new SpawnData(entityTag, Optional.empty(), Optional.empty()), 1);
            }
        }

        return builder.build();
    }

    public void removeEntry(WeightedEntry.Wrapper<SpawnData> entry) {
        List<WeightedEntry.Wrapper<SpawnData>> potentials = new ArrayList<>(this.spawnPotentials.unwrap());
        potentials.remove(entry);
        this.spawnPotentials = WeightedRandomList.create(potentials);
    }

    public Optional<SourceSpawnerRecipe> getRecipe(Entity entity) {
        return AddonRecipeRegistry.SOURCE_SPAWNER_REGISTRY.getRecipes().stream().filter(r -> r.value().isMatch(entity.getType())).findFirst().map(RecipeHolder::value);
    }

    public int calculateSource(Entity entity) {
        EntityType<?> type = entity.getType();
        SourceSpawnerRecipe recipe = recipes.get(type);
        if (recipe != null && recipe.source().isPresent()) {
            return recipe.source().get();
        }

        int source = 200;
        if (type.is(EntityTypeTagDatagen.BOSSES)) {
            source += 10_000;
        }

        if (type.getCategory().isPersistent()) {
            source += 1_000;
        }

        if (entity instanceof Mob mob) {
            source += (int) (mob.getMaxHealth() * 50);
        }

        return source;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos) {
        if (!this.isNearPlayer(level, pos)) return;

        if (this.spawnDelay == -1) {
            this.delay(level, pos);
            return;
        }

        if (this.spawnDelay > 0) {
            --this.spawnDelay;
            return;
        }

        if (disabled) {
            return;
        }

        this.spawnPotentials = getEntities(level, pos);

        if (this.spawnPotentials.isEmpty()) {
            this.delay(level, pos);
            return;
        }

        RandomSource randomSource = level.getRandom();
        int count = this.spawnCount;
        while (count > 0) {
            int total = this.spawnPotentials.unwrap().size();
            if (total == 0) {
                delay(level, pos);
                return;
            }

            WeightedEntry.Wrapper<SpawnData> wrappedSpawnData = this.spawnPotentials.getRandom(randomSource).orElse(null);
            if (wrappedSpawnData == null) {
                this.delay(level, pos);
                return;
            }
            SpawnData spawnData = wrappedSpawnData.data();

            CompoundTag entityTag = spawnData.getEntityToSpawn();
            Optional<EntityType<?>> entityTypeOptional = EntityType.by(entityTag);
            if (entityTypeOptional.isEmpty()) {
                removeEntry(wrappedSpawnData);
                continue;
            }
            EntityType<?> entityType = entityTypeOptional.get();

            double x = pos.getX() + (randomSource.nextDouble() - randomSource.nextDouble()) * this.spawnRange + 0.5;
            double y = pos.getY() + randomSource.nextInt(3) - 1;
            double z = pos.getZ() + (randomSource.nextDouble() - randomSource.nextDouble()) * this.spawnRange + 0.5;

            if (!level.noCollision(entityType.getSpawnAABB(x, y, z))) continue;

            if (!entityType.getCategory().isFriendly() && level.getDifficulty() == Difficulty.PEACEFUL) {
                removeEntry(wrappedSpawnData);
                continue;
            }

            Entity entity = EntityType.loadEntityRecursive(entityTag, level, (arg) -> {
                arg.moveTo(x, y, z, arg.getYRot(), arg.getXRot());
                return arg;
            });

            if (entity == null) {
                removeEntry(wrappedSpawnData);
                continue;
            }

            int nearby = level.getEntitiesOfClass(entity.getClass(), new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1).inflate(this.spawnRange)).size();
            if (nearby >= this.maxNearbyEntities) {
                removeEntry(wrappedSpawnData);
                continue;
            }

            entity.moveTo(entity.getX(), entity.getY(), entity.getZ(), randomSource.nextFloat() * 360, 0.0F);

            if (!level.tryAddFreshEntityWithPassengers(entity)) {
                removeEntry(wrappedSpawnData);
                continue;
            }

            int neededSource = calculateSource(entity);
            boolean tookSource = AddonSourceUtil.takeSource(pos, level, 5, neededSource);
            if (!tookSource) {
                entity.remove(Entity.RemovalReason.DISCARDED);
                removeEntry(wrappedSpawnData);
                continue;
            }

            level.levelEvent(2004, pos, 0);
            level.gameEvent(entity, GameEvent.ENTITY_PLACE, pos);
            if (entity instanceof Mob mob) {
                mob.spawnAnim();
            }

            count--;
        }

        if (count < this.spawnCount) {
            this.delay(level, pos);
        }
    }

    private boolean isNearPlayer(Level level, BlockPos pos) {
        return level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, this.requiredPlayerRange);
    }

    @Override
    public void clientTick(Level level, BlockPos pos) {
        if (this.isNearPlayer(level, pos) && !this.disabled) {
            RandomSource randomSource = level.getRandom();
            double x = pos.getX() + randomSource.nextDouble();
            double y = pos.getY() + randomSource.nextDouble();
            double z = pos.getZ() + randomSource.nextDouble();
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
            level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);

            if (this.spawnDelay > 0) {
                --this.spawnDelay;
            }
        }
    }

    @Override
    public @Nullable Either<BlockEntity, Entity> getOwner() {
        return Either.left(blockEntity);
    }
}
