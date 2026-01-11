package epilogue.util;

import com.google.common.base.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import java.util.List;

public final class RayCastUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static Vec3 getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float)Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float)Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3(f1 * f2, f3, f * f2);
    }

    public static RayCastResult rayCast(RotationUtil.RotationVec rotation, double distance) {
        return rayCast(rotation, distance, 0.0f);
    }

    public static boolean inView(Entity entity) {
        int renderDistance = 16 * mc.gameSettings.renderDistanceChunks;
        RotationUtil.RotationVec rotation = calculateRotationToEntity(entity);


        if (Math.abs(RotationUtil.wrapAngleDiff(mc.thePlayer.rotationYaw, rotation.x)) > mc.gameSettings.fovSetting) {
            return false;
        }

        if (entity.getDistanceToEntity(mc.thePlayer) > 100.0 || !(entity instanceof net.minecraft.entity.player.EntityPlayer)) {
            RayCastResult result = rayCast(rotation, renderDistance, 0.2f);
            return result != null && result.typeOfHit == RayCastResult.Type.ENTITY;
        }


        AxisAlignedBB boundingBox = entity.getEntityBoundingBox();
        double width = boundingBox.maxX - boundingBox.minX;
        double height = boundingBox.maxY - boundingBox.minY;
        double depth = boundingBox.maxZ - boundingBox.minZ;

        for (double yOffset = 1.0; yOffset >= -1.0; yOffset -= 0.5) {
            for (double xOffset = 1.0; xOffset >= -1.0; xOffset -= 1.0) {
                for (double zOffset = 1.0; zOffset >= -1.0; zOffset -= 1.0) {
                    double scanX = entity.posX + width * xOffset * 0.5;
                    double scanY = entity.posY + height * yOffset * 0.5;
                    double scanZ = entity.posZ + depth * zOffset * 0.5;

                    RotationUtil.RotationVec scanRotation = calculateRotationTo(scanX, scanY, scanZ);
                    RayCastResult result = rayCast(scanRotation, renderDistance, 0.2f);

                    if (result != null && result.typeOfHit == RayCastResult.Type.ENTITY) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static RayCastResult rayCast(RotationUtil.RotationVec rotation, double distance, float expandSize) {
        return rayCast(rotation, distance, expandSize, mc.thePlayer);
    }

    public static RayCastResult rayCast(RotationUtil.RotationVec rotation, double distance, float expandSize, Entity sourceEntity) {
        if (sourceEntity != null && mc.theWorld != null) {
            float partialTicks = 1.0f;


            MovingObjectPosition blockHit = rayTraceCustom(sourceEntity, rotation.x, rotation.y, distance);
            double maxDistance = distance;

            Vec3 eyePos = sourceEntity.getPositionEyes(partialTicks);

            if (blockHit != null && blockHit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                maxDistance = blockHit.hitVec.distanceTo(eyePos);
            }


            Vec3 lookVec = getVectorForRotation(rotation.y, rotation.x);
            Vec3 endPos = eyePos.addVector(lookVec.xCoord * distance, lookVec.yCoord * distance, lookVec.zCoord * distance);

            Entity hitEntity = null;
            Vec3 hitVec = null;


            Predicate<Entity> predicate = new Predicate<Entity>() {
                @Override
                public boolean apply(Entity entity) {
                    return entity.canBeCollidedWith();
                }
            };


            List<Entity> entities = mc.theWorld.getEntitiesInAABBexcluding(
                    sourceEntity,
                    sourceEntity.getEntityBoundingBox().addCoord(
                            lookVec.xCoord * distance,
                            lookVec.yCoord * distance,
                            lookVec.zCoord * distance
                    ).expand(1.0, 1.0, 1.0),
                    predicate
            );

            double currentDistance = maxDistance;

            for (Entity entity : entities) {
                float entityExpand = entity.getCollisionBorderSize() + expandSize;
                AxisAlignedBB expandedBB = entity.getEntityBoundingBox().expand(entityExpand, entityExpand, entityExpand);

                MovingObjectPosition entityHit = expandedBB.calculateIntercept(eyePos, endPos);

                if (expandedBB.isVecInside(eyePos)) {
                    if (currentDistance >= 0.0) {
                        hitEntity = entity;
                        hitVec = entityHit == null ? eyePos : entityHit.hitVec;
                        currentDistance = 0.0;
                    }
                    continue;
                }

                if (entityHit != null) {
                    double distanceToHit = eyePos.distanceTo(entityHit.hitVec);
                    if (distanceToHit < currentDistance || currentDistance == 0.0) {
                        hitEntity = entity;
                        hitVec = entityHit.hitVec;
                        currentDistance = distanceToHit;
                    }
                }
            }

            if (hitEntity != null && (currentDistance < maxDistance || blockHit == null)) {
                return new RayCastResult(hitEntity, hitVec);
            }

            if (blockHit != null) {

                return new RayCastResult(
                        blockHit.hitVec,
                        blockHit.sideHit,
                        blockHit.getBlockPos()
                );
            }
        }

        return null;
    }

    private static MovingObjectPosition rayTraceCustom(Entity entity, float yaw, float pitch, double distance) {
        Vec3 eyePos = entity.getPositionEyes(1.0f);

        Vec3 lookVec = getVectorForRotation(pitch, yaw);
        Vec3 targetPos = eyePos.addVector(lookVec.xCoord * distance, lookVec.yCoord * distance, lookVec.zCoord * distance);
        return entity.worldObj.rayTraceBlocks(eyePos, targetPos);
    }

    public static boolean overBlock(RotationUtil.RotationVec rotation, net.minecraft.util.EnumFacing side,
                                    net.minecraft.util.BlockPos pos, boolean checkSide) {
        RayCastResult hit = rayCast(rotation, 4.5);
        if (hit == null || hit.hitVec == null) {
            return false;
        }
        return hit.getBlockPos() != null && hit.getBlockPos().equals(pos) && (!checkSide || hit.sideHit == side);
    }

    private static RotationUtil.RotationVec calculateRotationToEntity(Entity entity) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 entityPos = new Vec3(
                entity.posX,
                entity.posY + entity.getEyeHeight(),
                entity.posZ
        );

        double deltaX = entityPos.xCoord - eyePos.xCoord;
        double deltaY = entityPos.yCoord - eyePos.yCoord;
        double deltaZ = entityPos.zCoord - eyePos.zCoord;

        double horizontalDist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        float yaw = (float)(Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float)(-(Math.atan2(deltaY, horizontalDist) * 180.0 / Math.PI));

        return new RotationUtil.RotationVec(yaw, pitch);
    }

    private static RotationUtil.RotationVec calculateRotationTo(double x, double y, double z) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);

        double deltaX = x - eyePos.xCoord;
        double deltaY = y - eyePos.yCoord;
        double deltaZ = z - eyePos.zCoord;

        double horizontalDist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        float yaw = (float)(Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float)(-(Math.atan2(deltaY, horizontalDist) * 180.0 / Math.PI));

        return new RotationUtil.RotationVec(yaw, pitch);
    }


    public static class RayCastResult {
        public enum Type {
            MISS,
            BLOCK,
            ENTITY
        }

        public Type typeOfHit;
        public Vec3 hitVec;
        public Entity entityHit;
        public net.minecraft.util.EnumFacing sideHit;
        private net.minecraft.util.BlockPos blockPos;


        public RayCastResult(Vec3 hitVec, Type type) {
            this.hitVec = hitVec;
            this.typeOfHit = type;
        }


        public RayCastResult(Entity entity, Vec3 hitVec) {
            this.entityHit = entity;
            this.hitVec = hitVec;
            this.typeOfHit = Type.ENTITY;
        }


        public RayCastResult(Vec3 hitVec, net.minecraft.util.EnumFacing sideHit, net.minecraft.util.BlockPos blockPos) {
            this.hitVec = hitVec;
            this.sideHit = sideHit;
            this.blockPos = blockPos;
            this.typeOfHit = Type.BLOCK;
        }


        public RayCastResult(Vec3 hitVec, net.minecraft.util.EnumFacing sideHit, Type type) {
            this.hitVec = hitVec;
            this.sideHit = sideHit;
            this.typeOfHit = type;
        }

        public net.minecraft.util.BlockPos getBlockPos() {
            return this.blockPos;
        }

        public void setBlockPos(net.minecraft.util.BlockPos blockPos) {
            this.blockPos = blockPos;
        }
    }
}