package team.creative.littletiles.common.entity.physic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import team.creative.creativecore.common.level.CreativeLevel;
import team.creative.creativecore.common.level.listener.LevelBoundsListener;
import team.creative.creativecore.common.level.system.BlockUpdateLevelSystem;
import team.creative.creativecore.common.util.math.base.Axis;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.math.box.OBB;
import team.creative.creativecore.common.util.math.collision.CollidingPlane;
import team.creative.creativecore.common.util.math.collision.CollidingPlane.PushCache;
import team.creative.creativecore.common.util.math.collision.CollisionCoordinator;
import team.creative.creativecore.common.util.math.matrix.IVecOrigin;
import team.creative.creativecore.common.util.math.vec.Vec3d;
import team.creative.littletiles.LittleTiles;
import team.creative.littletiles.common.api.block.LittlePhysicBlock;
import team.creative.littletiles.common.entity.INoPushEntity;
import team.creative.littletiles.common.entity.LittleLevelEntity;
import team.creative.littletiles.common.level.LittleAnimationHandlers;

public class LittleLevelEntityPhysic implements LevelBoundsListener {
    
    protected static final Predicate<Entity> noAnimation = x -> !(x.getFirstPassenger() instanceof INoPushEntity);
    
    public final LittleLevelEntity parent;
    
    private OBB orientatedBB;
    private boolean preventPush = false;
    public boolean noCollision = false;
    
    public LittleLevelEntityPhysic(LittleLevelEntity parent) {
        this.parent = parent;
    }
    
    public IVecOrigin getOrigin() {
        return parent.getOrigin();
    }
    
    public OBB getOrientatedBB() {
        return orientatedBB;
    }
    
    public AABB getBB() {
        return getOrigin().getAxisAlignedBox(orientatedBB);
    }
    
    public void ignoreCollision(Runnable run) {
        preventPush = true;
        try {
            run.run();
        } finally {
            preventPush = false;
        }
    }
    
    public boolean shouldPush() {
        return !preventPush;
    }
    
    public Iterable<OBB> collision(AABB box) {
        // TODO Implement
        return null;
    }
    
    @Override
    public void rescan(CreativeLevel level, BlockUpdateLevelSystem system, Iterable<BlockPos> possible) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        
        for (BlockPos pos : possible) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof LittlePhysicBlock) {
                LittlePhysicBlock phy = (LittlePhysicBlock) state.getBlock();
                minX = Math.min(minX, phy.bound(level, pos, Facing.WEST));
                minY = Math.min(minX, phy.bound(level, pos, Facing.DOWN));
                minZ = Math.min(minX, phy.bound(level, pos, Facing.NORTH));
                maxX = Math.max(maxX, phy.bound(level, pos, Facing.EAST));
                maxY = Math.max(maxX, phy.bound(level, pos, Facing.UP));
                maxZ = Math.max(maxX, phy.bound(level, pos, Facing.SOUTH));
            } else {
                minX = Math.min(minX, pos.getX());
                minY = Math.min(minX, pos.getY());
                minZ = Math.min(minX, pos.getZ());
                maxX = Math.max(maxX, pos.getX() + 1);
                maxY = Math.max(maxX, pos.getY() + 1);
                maxZ = Math.max(maxX, pos.getZ() + 1);
            }
        }
        orientatedBB = new OBB(parent.getOrigin(), minX, minY, minZ, maxX, maxY, maxZ);
    }
    
    @Override
    public void rescan(CreativeLevel level, BlockUpdateLevelSystem system, Facing facing, Iterable<BlockPos> possible, int boundary) {
        double value = facing.positive ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        for (BlockPos pos : possible) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof LittlePhysicBlock)
                value = facing.positive ? Math.max(value, ((LittlePhysicBlock) state.getBlock()).bound(level, pos, facing)) : Math
                        .min(value, ((LittlePhysicBlock) state.getBlock()).bound(level, pos, facing));
            else
                value = facing.positive ? Math.max(value, pos.get(facing.axis.toVanilla()) + 1) : Math.min(value, pos.get(facing.axis.toVanilla()));
            
            if (value == boundary)
                break;
        }
        orientatedBB = orientatedBB.set(facing, value);
    }
    
    public void moveAndRotateAnimation(double x, double y, double z, double rotX, double rotY, double rotZ) {
        if (x == 0 && y == 0 && z == 0 && rotX == 0 && rotY == 0 && rotZ == 0)
            return;
        
        CollisionCoordinator coordinator = new CollisionCoordinator(x, y, z, rotX, rotY, rotZ, getOrigin(), getOrigin());
        if (LittleTiles.CONFIG.general.enableAnimationCollision)
            moveAndRotateAnimation(coordinator);
        coordinator.move();
    }
    
    public void moveAndRotateAnimation(CollisionCoordinator coordinator) {
        if (preventPush)
            return;
        
        noCollision = true;
        
        Level level = parent.getRealLevel();
        List<Entity> entities = level.getEntities(parent, coordinator.computeSurroundingBox(orientatedBB), noAnimation);
        List<OBB> worldCollisionBoxes = Collections.EMPTY_LIST;
        if (!entities.isEmpty()) {
            
            // PHASE ONE
            // Gather all affected boxes
            List<AABB> surroundingBoxes = new ArrayList<>(worldCollisionBoxes.size());
            for (OBB box : worldCollisionBoxes) {
                if (box.cache == null)
                    box.buildCache();
                box.cache.reset();
                
                surroundingBoxes.add(coordinator.computeSurroundingBox(box));
            }
            
            // PHASE TWO
            // Move entities by their center
            PushCache[] caches = new PushCache[entities.size()];
            for (int j = 0; j < entities.size(); j++) {
                Entity entity = entities.get(j);
                
                AABB entityBB = entity.getBoundingBox();
                Vec3d center = new Vec3d(entityBB.minX + (entityBB.maxX - entityBB.minX) * 0.5D, entityBB.minY + (entityBB.maxY - entityBB.minY) * 0.5D, entityBB.minZ + (entityBB.maxZ - entityBB.minZ) * 0.5D);
                double radius = center.distanceSqr(entityBB.minX, entityBB.minY, entityBB.minZ);
                
                Double t = null;
                OBB pushingBox = null;
                //Facing facing;
                
                checking_all_boxes: for (int i = 0; i < surroundingBoxes.size(); i++) {
                    if (surroundingBoxes.get(i).intersects(entityBB)) {
                        // Check for earliest hit
                        OBB box = worldCollisionBoxes.get(i);
                        
                        if (!box.cache.isCached())
                            box.cache.planes = CollidingPlane.getPlanes(box, box.cache, coordinator);
                        
                        // Binary search
                        for (CollidingPlane plane : box.cache.planes) {
                            Double tempT = plane.binarySearch(t, entityBB, radius, center, coordinator);
                            if (tempT != null) {
                                t = tempT;
                                pushingBox = box;
                                //facing = plane.facing;
                                if (t == 0)
                                    break checking_all_boxes;
                            }
                        }
                    }
                }
                
                // Applying found t
                if (t != null) {
                    PushCache cache = new PushCache();
                    //cache.facing = facing;
                    
                    Vec3d newCenter = new Vec3d(center);
                    coordinator.transform(newCenter, 1 - t);
                    
                    cache.pushBox = pushingBox;
                    cache.entityBox = entityBB.move(newCenter.x - center.x, newCenter.y - center.y, newCenter.z - center.z);
                    caches[j] = cache;
                }
            }
            
            coordinator.move();
            
            // PHASE THREE
            for (int i = 0; i < entities.size(); i++) {
                Entity entity = entities.get(i);
                PushCache cache = caches[i];
                
                if (cache == null) {
                    cache = new PushCache();
                    cache.entityBox = entity.getBoundingBox();
                }
                
                cache.entityBoxOrientated = coordinator.origin.getOrientatedBox(cache.entityBox);
                Vec3d center = new Vec3d(cache.entityBox.minX + (cache.entityBox.maxX - cache.entityBox.minX) * 0.5D, cache.entityBox.minY + (cache.entityBox.maxY - cache.entityBox.minY) * 0.5D, cache.entityBox.minZ + (cache.entityBox.maxZ - cache.entityBox.minZ) * 0.5D);
                coordinator.origin.transformPointToFakeWorld(center);
                
                Axis one = null;
                Axis two = null;
                
                boolean ignoreOne = false;
                Boolean positiveOne = null;
                boolean ignoreTwo = false;
                Boolean positiveTwo = null;
                
                double maxVolume = 0;
                
                List<OBB> intersecting = new ArrayList<>();
                List<Facing> intersectingFacing = new ArrayList<>();
                
                for (OBB box : worldCollisionBoxes) {
                    if (box == cache.pushBox || box.intersects(cache.entityBoxOrientated)) {
                        //box.cache.planes = CollidingPlane.getPlanes(box, box.cache, coordinator);
                        
                        Facing facing = CollidingPlane.getDirection(coordinator, box, center);
                        if (facing == null || (!coordinator.hasRotation && (!coordinator.hasTranslation || (coordinator.translation.get(facing.axis) > 0) != facing.positive)))
                            continue;
                        
                        double intersectingVolume = box.getIntersectionVolume(cache.entityBoxOrientated);
                        
                        if (maxVolume == 0 || intersectingVolume > maxVolume) {
                            maxVolume = intersectingVolume;
                            cache.facing = facing;
                        }
                        
                        intersecting.add(box);
                        intersectingFacing.add(facing);
                    }
                }
                
                if (intersecting.isEmpty())
                    continue;
                
                one = cache.facing.one();
                two = cache.facing.two();
                
                positiveOne = null;
                positiveTwo = null;
                
                for (Facing facing : intersectingFacing) {
                    
                    if (!ignoreOne && facing.axis == one) {
                        if (positiveOne == null)
                            positiveOne = facing.positive;
                        else if (facing.positive != positiveOne)
                            ignoreOne = true;
                    } else if (!ignoreTwo && facing.axis == two) {
                        if (positiveTwo == null)
                            positiveTwo = facing.positive;
                        else if (facing.positive != positiveTwo)
                            ignoreTwo = true;
                    }
                    
                    if (ignoreOne && ignoreTwo)
                        break;
                }
                
                // Now things are ready. Go through all intersecting ones and push the box out
                Vec3d pushVec = new Vec3d();
                pushVec.set(cache.facing.axis, cache.facing.offset());
                if (!ignoreOne && positiveOne != null)
                    pushVec.set(one, positiveOne ? 1 : -1);
                if (!ignoreTwo && positiveTwo != null)
                    pushVec.set(two, positiveTwo ? 1 : -1);
                
                double scale = 0;
                
                for (int j = 0; j < intersecting.size(); j++) {
                    Facing facing = intersectingFacing.get(j);
                    
                    if ((ignoreOne && facing.axis == one) || (ignoreTwo && facing.axis == two))
                        continue;
                    
                    scale = intersecting.get(j).getPushOutScale(scale, cache.entityBoxOrientated, pushVec);
                }
                
                boolean collidedHorizontally = entity.horizontalCollision;
                boolean collidedVertically = entity.verticalCollision;
                boolean onGround = entity.isOnGround();
                
                AABB originalBox = entity.getBoundingBox();
                
                Vec3d rotatedVec = new Vec3d(pushVec);
                coordinator.origin.rotation().transform(rotatedVec);
                
                double moveX = cache.entityBox.minX - originalBox.minX + rotatedVec.x * scale;
                double moveY = cache.entityBox.minY - originalBox.minY + rotatedVec.y * scale;
                double moveZ = cache.entityBox.minZ - originalBox.minZ + rotatedVec.z * scale;
                
                entity.move(MoverType.SELF, new Vec3(moveX, moveY, moveZ));
                
                if (entity instanceof ServerPlayer)
                    LittleAnimationHandlers.setPushedByDoor((ServerPlayer) entity);
                
                if (LittleTiles.CONFIG.general.enableCollisionMotion)
                    entity.getDeltaMovement().add(moveX, moveY, moveZ);
                
                if (moveX != 0 || moveZ != 0)
                    collidedHorizontally = true;
                if (moveY != 0) {
                    collidedVertically = true;
                    onGround = true;
                }
                
                entity.horizontalCollision = collidedHorizontally;
                entity.verticalCollision = collidedVertically;
                entity.setOnGround(onGround);
                
            }
            
            for (OBB box : worldCollisionBoxes)
                box.cache.reset();
        }
        
        /*for (int i = 0; i < fakeWorld.loadedEntityList.size(); i++) {
            Entity entity = fakeWorld.loadedEntityList.get(i);
            if (entity instanceof EntityAnimation) {
                coordinator.reset(((EntityAnimation) entity).origin);
                ((EntityAnimation) entity).moveAndRotateAnimation(coordinator);
            } TODO Implement moving children
        }*/
        
        noCollision = false;
    }
    
    public void updateBoundingBox() {
        if (orientatedBB == null || parent.getFakeLevel() == null)
            return;
        
        if (parent.getOrigin().hasChanged() || parent.getOrigin().hasChanged()) {
            parent.markOriginChange();
            parent.setBoundingBox(parent.getOrigin().getAxisAlignedBox(orientatedBB));
            parent.resetOriginChange();
        }
    }
}
