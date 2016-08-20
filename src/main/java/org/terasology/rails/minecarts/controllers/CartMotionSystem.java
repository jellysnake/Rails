/*
 * Copyright 2016 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rails.minecarts.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.core.logic.door.DoorSystem;
import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.location.LocationComponent;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.physics.HitResult;
import org.terasology.physics.Physics;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.rails.minecarts.blocks.RailBlockTrackSegment;
import org.terasology.rails.minecarts.blocks.RailBlockTrackSegmentSystem;
import org.terasology.rails.minecarts.blocks.RailComponent;
import org.terasology.rails.minecarts.components.RailVehicleComponent;
import org.terasology.rails.tracks.TrackSegment;
import org.terasology.registry.In;
import org.terasology.rendering.logic.MeshComponent;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;

/**
 * Created by michaelpollind on 8/16/16.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class CartMotionSystem extends BaseComponentSystem implements UpdateSubscriberSystem {

    private static final Logger logger = LoggerFactory.getLogger(CartMotionSystem.class);

    @In
    private Time time;
    @In
    private EntityManager entityManager;
    @In
    private WorldProvider worldProvider;
    @In
    private Physics physics;
    @In
    private LocalPlayer localPlayer;
    @In
    private InventoryManager inventoryManager;

    @In
    private BlockEntityRegistry blockEntityRegistry;

    @In
    private RailBlockTrackSegmentSystem railBlockTrackSegment;



    @Override
    public void update(float delta) {
        for (EntityRef railVehicle : entityManager.getEntitiesWith(RailVehicleComponent.class,RigidBodyComponent.class)) {
            RailVehicleComponent railVehicleComponent = railVehicle.getComponent(RailVehicleComponent.class);

                updateCart(railVehicle);
        }
    }

    public  void  updateCart(EntityRef railVehicle)
    {

        LocationComponent location = railVehicle.getComponent(LocationComponent.class);
        RigidBodyComponent rigidBody = railVehicle.getComponent(RigidBodyComponent.class);
        RailVehicleComponent railVehicleComponent = railVehicle.getComponent(RailVehicleComponent.class);

        if(railVehicleComponent.currentSegment == null)
        {
            HitResult hit =  physics.rayTrace(location.getWorldPosition(), Vector3f.down(), 1.2f, StandardCollisionGroup.DEFAULT, StandardCollisionGroup.WORLD);
            if(hit == null || hit.getBlockPosition() == null)
                return;

            EntityRef ref= blockEntityRegistry.getBlockEntityAt(hit.getBlockPosition());
            Block block = worldProvider.getBlock(hit.getBlockPosition());

            if(ref.hasComponent(RailComponent.class))
            {
                railVehicleComponent.currentSegment = ref;

                RailBlockTrackSegment segment = railBlockTrackSegment.getSegment(block.getURI());
                railVehicleComponent.t = segment.getNearestT(hit.getHitPoint(),hit.getBlockPosition().toVector3f(),segment.getRotation().getQuat4f());
                railVehicleComponent.headingVelocity = segment.getTangent(railVehicleComponent.t,segment.getRotation().getQuat4f(),ref);
                railVehicle.saveComponent(railVehicleComponent);

            }

        }

        repositionAxis(railVehicle);


    }

    public  void repositionAxis(EntityRef railVehicle)
    {
        LocationComponent location = railVehicle.getComponent(LocationComponent.class);
        RailVehicleComponent railVehicleComponent = railVehicle.getComponent(RailVehicleComponent.class);

        if(railVehicleComponent.currentSegment != null)
        {

            BlockComponent blockComponent = railVehicleComponent.currentSegment.getComponent(BlockComponent.class);
            if(blockComponent == null)
            {
                railVehicleComponent.currentSegment = null;
                railVehicle.saveComponent(railVehicleComponent);
                return;
            }

            RailBlockTrackSegment segment = railBlockTrackSegment.getSegment(blockComponent.getBlock().getURI());

            Vector3f position = segment.getPoint(railVehicleComponent.t,blockComponent.getPosition().toVector3f(),segment.getRotation().getQuat4f(),railVehicleComponent.currentSegment);
            MeshComponent mesh = railVehicle.getComponent(MeshComponent.class);
            position.y = mesh.mesh.getAABB().getMax().y/2.0f + position.y + .05f;

            Vector3f normal =  segment.getNormal(railVehicleComponent.t,segment.getRotation().getQuat4f(),railVehicleComponent.currentSegment);
            Vector3f tangent = segment.getTangent(railVehicleComponent.t,segment.getRotation().getQuat4f(),railVehicleComponent.currentSegment);

            location.setWorldPosition(position);
            location.setLocalRotation(new Quat4f(normal,(float) Math.acos(Vector3f.north().dot(tangent))));


            TrackSegment.TrackSegmentPair proceedingPair;
            float headingMagnitude =  railVehicleComponent.headingVelocity.length();
            Vector3f normalizedHeading = railVehicleComponent.headingVelocity.normalize();

            float tangentrotation =  new Quat4f().shortestArcQuat(tangent,normalizedHeading).getAngle();
            float inverseTangentRoation = new Quat4f().shortestArcQuat(new Vector3f(tangent).invert(),normalizedHeading).getAngle();
            if( Math.abs(tangentrotation) < Math.abs(inverseTangentRoation)) {
                railVehicleComponent.headingVelocity.set(tangent).mul(headingMagnitude);
                proceedingPair = segment.getTrackSegment(railVehicleComponent.t + headingMagnitude * time.getGameDelta(), railVehicleComponent.currentSegment);
            }
            else {
                railVehicleComponent.headingVelocity.set(tangent).invert().mul(headingMagnitude);
                proceedingPair = segment.getTrackSegment(railVehicleComponent.t - headingMagnitude * time.getGameDelta(), railVehicleComponent.currentSegment);
            }
            if(proceedingPair == null)
                return;

            railVehicleComponent.t = proceedingPair.t;
            railVehicleComponent.currentSegment = proceedingPair.association;

            railVehicle.saveComponent(railVehicleComponent);
            railVehicle.saveComponent(location);

        }
    }



    private  void  findTrackToAttachTo(EntityRef ref)
    {

    }

}
