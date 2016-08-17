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
package org.terasology.rails.tracks;

import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;
import org.terasology.rendering.logic.FloatingTextComponent;
import org.terasology.world.WorldProvider;

import javax.sound.midi.Track;
import java.util.ArrayList;
import java.util.List;

public abstract class TrackSegment {

    public static final float ARC_SEGMENT_ITERATIONS = 100;

    private CubicBezier[] curves;
    private  float[] argLengths;

    public static class TrackSegmentPair
    {
        float t;
        TrackSegment segment;

        public  TrackSegmentPair(float t, TrackSegment segment)
        {
            this.t = t;
            this.segment = segment;
        }
    }



    public TrackSegment(CubicBezier[] curves) {
        this.curves = curves;
        this.argLengths = new float[this.curves.length];
        CalculateLengths();
    }

    public  void  CalculateLengths()
    {

        float distance = 0f;
        for (int x = 0; x < curves.length; x++)
        {
            Vector3f previous = curves[x].getPoint(0);
            for(int y = 0; y <= ARC_SEGMENT_ITERATIONS; y++)
            {
                Vector3f current = curves[x].getPoint(y/ARC_SEGMENT_ITERATIONS);
                distance += current.distance(previous);
                previous = current;
            }
            this.argLengths[x] = distance;
        }
    }

    public  float getNearestT(Vector3f pos, Vector3f position, Quat4f rotation)
    {
        float result = 0;
        float closest = Float.MAX_VALUE;

        for (int x = 0; x < curves.length; x++)
        {
            for(int y = 0; y <= ARC_SEGMENT_ITERATIONS; y++)
            {
                Vector3f point = curves[x].getPoint(y/ARC_SEGMENT_ITERATIONS);
                rotation.rotate(point,point);
                point.add(position);

                if(point.distance(pos) < closest)
                    result = y/ARC_SEGMENT_ITERATIONS;
            }
        }
        return  result;
    }

    private  int getIndex(float t)
    {
        float distance = 0;
        if(t < 0)
            return  -1;
        for(int x = 0; x < argLengths.length; x++)
        {
            if(t < argLengths[x])
            {
                return x;
            }
        }
        return argLengths.length ;
    }

    private  float getT(int index,float t)
    {
        if(index -1 < 0)
        {
            return  (t/argLengths[0]);
        }
        return  ((t - argLengths[index -1])/(argLengths[index -1]-argLengths[index]));
    }


    public  float getMaxDistance()
    {
        return  argLengths[argLengths.length -1];
    }


    public  Vector3f getPoint(float t,EntityRef ref,WorldProvider worldProvider, EntityManager entityManager)
    {
        TrackSegmentPair pair =  getTrackSegment(t,ref,  worldProvider,  entityManager);
        if(pair.segment == null)
            return null;
        int index= pair.segment.getIndex(pair.t);
        return pair.segment.curves[index].getPoint(pair.segment.getT(index,(pair.t)) );
    }


    public Vector3f getPoint(float t,Vector3f position,Quat4f rotation,EntityRef ref,WorldProvider worldProvider, EntityManager entityManager)
    {
        return rotation.rotate(getPoint(t,ref,  worldProvider,  entityManager)).add(position);
    }

    public  Vector3f getTangent(float t,EntityRef ref,WorldProvider worldProvider, EntityManager entityManager)
    {
        TrackSegmentPair pair =  getTrackSegment(t,ref,  worldProvider,  entityManager);
        if(pair.segment == null)
            return null;
        int index= pair.segment.getIndex(pair.t);
        return pair.segment.curves[index].getTangent(pair.segment.getT(index,pair.t));
    }

    public Vector3f getTangent(float t,Quat4f rotation,EntityRef ref,WorldProvider worldProvider, EntityManager entityManager)
    {
        return rotation.rotate(getTangent(t,ref,  worldProvider,  entityManager));
    }

    public  Vector3f getBinormal(float t,EntityRef ref,WorldProvider worldProvider, EntityManager entityManager)
    {
        TrackSegmentPair pair =  getTrackSegment(t,ref,  worldProvider,  entityManager);
        if(pair.segment == null)
            return null;
        int index= pair.segment.getIndex(pair.t);
        return pair.segment.curves[index].getBinormal(pair.segment.getT(index,pair.t));
    }

    public Vector3f getBinormal(float t,Quat4f rotation,EntityRef ref,WorldProvider worldProvider, EntityManager entityManager)
    {
        return rotation.rotate(getBinormal(t,ref,worldProvider,entityManager));
    }

    public TrackSegmentPair getTrackSegment(float t, EntityRef ref, WorldProvider worldProvider, EntityManager entityManager)
    {

        TrackSegment previous = this.getPreviousSegment(ref,  worldProvider,  entityManager);
        TrackSegment next = this.getNextSegment(ref,  worldProvider,  entityManager);

        int index = getIndex(t);

        if(index == argLengths.length)
        {
            float result = this.getMaxDistance()-t;
            if(invertSegment(this,next,  worldProvider,  entityManager))
                result = next.getMaxDistance() - result;

            return  new TrackSegmentPair(result,next);
        }
        else if(index == -1)
        {
            float result = previous.getMaxDistance()+t;
            if(invertSegment(previous,this,  worldProvider,  entityManager))
                result = previous.getMaxDistance() - result;

            return  new TrackSegmentPair(result,previous);
        }
        return  new TrackSegmentPair(t,this);
    }
    public  abstract  boolean invertSegment(TrackSegment previous,TrackSegment next, WorldProvider worldProvider, EntityManager entityManager);
    public abstract TrackSegment getNextSegment(EntityRef ref, WorldProvider worldProvider, EntityManager entityManager);
    public abstract TrackSegment getPreviousSegment(EntityRef ref, WorldProvider worldProvider, EntityManager entityManager);
}
