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
package org.terasology.rails.minecarts.blocks;

import jdk.nashorn.internal.runtime.QuotedStringTokenizer;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.math.Rotation;
import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.rails.minecarts.components.PathDescriptorComponent;
import org.terasology.rails.tracks.CubicBezier;
import org.terasology.rails.tracks.TrackSegment;
import org.terasology.registry.In;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockUri;
import org.terasology.world.generation.World;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by michaelpollind on 8/16/16.
 */
public class RailBlockTrackSegment extends TrackSegment {

    private  Rotation rotation;
    private  Side start;
    private  Side end;


    WorldProvider worldProvider;
    EntityManager entityManager;

    public void SetProviders(WorldProvider worldProvider,EntityManager entityManager)
    {
        this.worldProvider = worldProvider;
        this.entityManager = entityManager;
    }

    public RailBlockTrackSegment(CubicBezier[] curves, PathDescriptorComponent.Descriptor descriptor, Rotation rotation) {
        super(curves);
        this.rotation = rotation;
        this.start = rotation.rotate(descriptor.start);
        this.end = rotation.rotate(descriptor.end);
    }

    @Override
    public boolean invertSegment(TrackSegment previous, TrackSegment next) {
        if(((RailBlockTrackSegment)previous).end.reverse() == ((RailBlockTrackSegment)next).start)
            return false;
        return  true;
    }

    @Override
    public TrackSegment getNextSegment(EntityRef ref) {
        Vector3i blockPosition = ref.getComponent(BlockComponent.class).getPosition().add(end.getVector3i());
        Block b = worldProvider.getBlock(blockPosition);

        return ((RailsUpdatesFamily)b.getBlockFamily()).getRailSegment(b.getURI());
    }

    @Override
    public TrackSegment getPreviousSegment(EntityRef ref) {
        Vector3i blockPosition = ref.getComponent(BlockComponent.class).getPosition().add(start.getVector3i());
        Block b = worldProvider.getBlock(blockPosition);

        return ((RailsUpdatesFamily)b.getBlockFamily()).getRailSegment(b.getURI());
    }
}
