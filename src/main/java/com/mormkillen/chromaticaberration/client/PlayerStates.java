package com.mormkillen.chromaticaberration.client;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.StriderEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Set;

/**
 * Player states that can be used as trigger conditions on 1.20.1.
 *
 * Only states that actually exist in this version are listed. Conditions that
 * were added by later versions (spear charge, mace damage, the happy ghast)
 * are intentionally absent instead of being dead entries.
 */
public final class PlayerStates {
    public static final String MOVEMENT = "movement";
    public static final String POSE = "pose";
    public static final String RIDING = "riding";
    public static final String ENVIRONMENT = "environment";

    public static final String WALKING = "walking";
    public static final String SPRINTING = "sprinting";
    public static final String SNEAKING = "sneaking";
    public static final String CRAWLING = "crawling";
    public static final String JUMPING = "jumping";
    public static final String FALLING = "falling";
    public static final String SWIMMING = "swimming";
    public static final String UNDERWATER_MOVEMENT = "underwater_movement";
    public static final String FLOATING = "floating";
    public static final String ELYTRA = "elytra";
    public static final String CREATIVE_FLYING = "creative_flying";
    public static final String SPECTATOR_FLYING = "spectator_flying";
    public static final String CLIMBING = "climbing";
    public static final String VINE_CLIMBING = "vine_climbing";

    public static final String POSE_STANDING = "pose_standing";
    public static final String POSE_CROUCHING = "pose_crouching";
    public static final String POSE_SWIMMING = "pose_swimming";
    public static final String POSE_FALL_FLYING = "pose_fall_flying";
    public static final String POSE_SLEEPING = "pose_sleeping";
    public static final String SPIN_ATTACK = "spin_attack";

    public static final String RIDE_HORSE = "ride_horse";
    public static final String RIDE_PIG = "ride_pig";
    public static final String RIDE_STRIDER = "ride_strider";
    public static final String RIDE_BOAT = "ride_boat";
    public static final String RIDE_MINECART = "ride_minecart";
    public static final String RIDE_CAMEL = "ride_camel";
    public static final String RIDE_OTHER = "ride_other";

    public static final String UNDERWATER = "underwater";
    public static final String IN_LAVA = "in_lava";
    public static final String NAUSEA = "nausea";
    public static final String NIGHT_VISION = "night_vision";
    public static final String WATER_BREATHING = "water_breathing";
    public static final String HURT = "hurt";
    public static final String VIEW_BOBBING = "view_bobbing";

    public static final List<Group> GROUPS = List.of(
            new Group(MOVEMENT, List.of(
                    WALKING, SPRINTING, SNEAKING, CRAWLING, JUMPING, FALLING, SWIMMING,
                    UNDERWATER_MOVEMENT, FLOATING, ELYTRA, CREATIVE_FLYING, SPECTATOR_FLYING,
                    CLIMBING, VINE_CLIMBING)),
            new Group(POSE, List.of(
                    POSE_STANDING, POSE_CROUCHING, POSE_SWIMMING, POSE_FALL_FLYING,
                    POSE_SLEEPING, SPIN_ATTACK)),
            new Group(RIDING, List.of(
                    RIDE_HORSE, RIDE_PIG, RIDE_STRIDER, RIDE_BOAT, RIDE_MINECART,
                    RIDE_CAMEL, RIDE_OTHER)),
            new Group(ENVIRONMENT, List.of(
                    UNDERWATER, IN_LAVA, NAUSEA, NIGHT_VISION, WATER_BREATHING, HURT,
                    VIEW_BOBBING)));

    private PlayerStates() {
    }

    public record Group(String id, List<String> states) {
    }

    public static boolean isValid(String id) {
        if (id == null) {
            return false;
        }
        for (Group group : GROUPS) {
            if (group.states().contains(id)) {
                return true;
            }
        }
        return false;
    }

    /** True when at least one of the selected conditions is currently active. */
    public static boolean matches(MinecraftClient client, Set<String> selected) {
        if (selected == null || selected.isEmpty()) {
            return false;
        }
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return false;
        }
        for (String id : selected) {
            if (isActive(client, player, id)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isActive(MinecraftClient client, ClientPlayerEntity player, String id) {
        Vec3d velocity = player.getVelocity();
        double horizontal = Math.hypot(velocity.x, velocity.z);
        EntityPose pose = player.getPose();
        Entity vehicle = player.getVehicle();

        return switch (id) {
            case WALKING -> player.isOnGround() && horizontal > 0.06
                    && !player.isSprinting() && !player.isSneaking();
            case SPRINTING -> player.isSprinting();
            case SNEAKING -> player.isSneaking();
            case CRAWLING -> player.isCrawling();
            case JUMPING -> !player.isOnGround() && velocity.y > 0.0 && !player.isFallFlying();
            case FALLING -> !player.isOnGround() && velocity.y < -0.1
                    && !player.isFallFlying() && !player.isSwimming();
            case SWIMMING -> player.isSwimming();
            case UNDERWATER_MOVEMENT -> player.isSubmergedInWater() && horizontal > 0.02;
            case FLOATING -> player.isTouchingWater() && !player.isSubmergedInWater();
            case ELYTRA -> player.isFallFlying();
            case CREATIVE_FLYING -> player.isCreative() && player.getAbilities().flying;
            case SPECTATOR_FLYING -> player.isSpectator();
            case CLIMBING -> player.isClimbing() && player.getWorld()
                    .getBlockState(BlockPos.ofFloored(player.getPos())).isOf(Blocks.LADDER);
            case VINE_CLIMBING -> player.isClimbing() && isVine(player);
            case POSE_STANDING -> pose == EntityPose.STANDING;
            case POSE_CROUCHING -> pose == EntityPose.CROUCHING;
            case POSE_SWIMMING -> pose == EntityPose.SWIMMING;
            case POSE_FALL_FLYING -> pose == EntityPose.FALL_FLYING;
            case POSE_SLEEPING -> pose == EntityPose.SLEEPING;
            case SPIN_ATTACK -> player.isUsingRiptide();
            case RIDE_HORSE -> vehicle instanceof AbstractHorseEntity && !(vehicle instanceof CamelEntity);
            case RIDE_PIG -> vehicle instanceof PigEntity;
            case RIDE_STRIDER -> vehicle instanceof StriderEntity;
            case RIDE_BOAT -> vehicle instanceof BoatEntity;
            case RIDE_MINECART -> vehicle instanceof AbstractMinecartEntity;
            case RIDE_CAMEL -> vehicle instanceof CamelEntity;
            case RIDE_OTHER -> vehicle != null && !(vehicle instanceof AbstractHorseEntity)
                    && !(vehicle instanceof PigEntity) && !(vehicle instanceof StriderEntity)
                    && !(vehicle instanceof BoatEntity) && !(vehicle instanceof AbstractMinecartEntity);
            case UNDERWATER -> player.isSubmergedInWater();
            case IN_LAVA -> player.isInLava();
            case NAUSEA -> player.hasStatusEffect(StatusEffects.NAUSEA);
            case NIGHT_VISION -> player.hasStatusEffect(StatusEffects.NIGHT_VISION);
            case WATER_BREATHING -> player.hasStatusEffect(StatusEffects.WATER_BREATHING);
            case HURT -> player.hurtTime > 0;
            case VIEW_BOBBING -> client.options.getBobView().getValue() && horizontal > 0.02;
            default -> false;
        };
    }

    private static boolean isVine(ClientPlayerEntity player) {
        return player.getWorld().getBlockState(BlockPos.ofFloored(player.getPos())).isOf(Blocks.VINE)
                || player.getWorld().getBlockState(BlockPos.ofFloored(player.getPos())).isOf(Blocks.WEEPING_VINES)
                || player.getWorld().getBlockState(BlockPos.ofFloored(player.getPos())).isOf(Blocks.TWISTING_VINES);
    }
}
