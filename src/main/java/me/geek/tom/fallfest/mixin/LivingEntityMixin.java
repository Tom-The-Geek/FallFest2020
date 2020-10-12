package me.geek.tom.fallfest.mixin;

import me.geek.tom.fallfest.block.spawner.CursedSpawnerBlockEntity;
import me.geek.tom.fallfest.component.ModComponents;
import me.geek.tom.fallfest.component.SpawnerMobComponent;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "onDeath", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;setPose(Lnet/minecraft/entity/EntityPose;)V",
            shift = At.Shift.AFTER
    ))
    private void onEntityDeath(DamageSource source, CallbackInfo ci) {
        if (this.world.isClient()) return;

        SpawnerMobComponent component = ModComponents.SPAWNER_MOB.get(this);
        if (component.hasSpawner()) {
            BlockEntity be = this.world.getBlockEntity(component.getSpawnerPos());
            if (be instanceof CursedSpawnerBlockEntity) {
                ((CursedSpawnerBlockEntity) be).onEntityDie((LivingEntity)(Object)this);
            }
        }
    }

}
