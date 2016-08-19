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

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.rails.minecarts.components.RailVehicleComponent;
import org.terasology.world.block.Block;

/**
 * Created by michaelpollind on 8/16/16.
 */
public class CartMotion {

    public Vector3f prevPosition = new Vector3f();
    public Vector3f currentPosition = new Vector3f();

    public Vector3i currentSegment;
    public Vector3i previousSegment;
    public  float t;

}
