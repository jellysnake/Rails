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

import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;
import org.terasology.rendering.logic.FloatingTextComponent;

import javax.sound.midi.Track;
import java.util.ArrayList;
import java.util.List;

public abstract class TrackSegment {

    public static final float ARC_SEGMENT_ITERATIONS = 100;

    private CubicBezier[] curves;
    private  float[] argLengths;
    private  float maxDistance;

    public  static class TrackSegmentPair
    {
        int index;
        float t;
        TrackSegment segment;

        public  TrackSegmentPair(float t, TrackSegment segment,int index)
        {
            this.index = index;
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
            maxDistance += distance;
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
                point.add(position);
                rotation.rotate(point);

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
            return  t;
        }
        return  t - argLengths[index -1];
    }

    public  float getMaxDistance()
    {
        return  maxDistance;
    }


    public  Vector3f getPoint(float t)
    {
        TrackSegmentPair pair =  getTrackSegment(t);
        if(pair.segment == null)
            return null;
        return pair.segment.getPoint(pair.segment.getT(pair.index,pair.t));
    }

    public  Vector3f getTangent(float t)
    {
        TrackSegmentPair pair =  getTrackSegment(t);
        if(pair.segment == null)
            return null;
        return pair.segment.getTangent(pair.segment.getT(pair.index,pair.t));
    }

    public  Vector3f getBinormal(float t)
    {
        TrackSegmentPair pair =  getTrackSegment(t);
        if(pair.segment == null)
            return null;
        return pair.segment.getBinormal(pair.segment.getT(pair.index,pair.t));
    }

    public TrackSegmentPair getTrackSegment(float t)
    {
        int index = getIndex(t);
        if(index == argLengths.length)
        {
            return  new TrackSegmentPair(maxDistance-t,getNextSegment(),index);
        }
        else if(index == -1)
        {
            TrackSegment previousSegment = getPreviousSegment();
            return  new TrackSegmentPair(previousSegment.getMaxDistance()+t,getPreviousSegment(),index);
        }
        return  new TrackSegmentPair(t,this,index);

    }


    public abstract TrackSegment getNextSegment();
    public abstract TrackSegment getPreviousSegment();
}
