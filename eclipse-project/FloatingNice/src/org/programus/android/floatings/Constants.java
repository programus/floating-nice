package org.programus.android.floatings;

/**
 * An interface to put the constants in
 * @author programus
 *
 */
public interface Constants {
    /** global key */
	String GLOBAL_KEY = "Global.Key";
	/** enable key suffix */
	String ENABLED = "/enabled";
	/** key for x */
	String X = "x";
	/** key for y */
	String Y = "y";
	/** key for orientation */
	String OR = "isPortrait";
	
	/** key for flag of start by program */
	String SELF_START = "SelfStart";
	
	/** the angles for the dirs */
	int[] angles = {0, 90, 180, 270};
	/** position offsets for the buttons */
	int[][] offsets = {
			{-1, 0},
			{0, -1},
			{1, 0},
			{0, 1}
	};
	/** position offsets for the clipboard */
	int[][][][] xoffsets = {
			//  ov, margin, tv
			{
			    {
    				{1, 0, -1},
    				{0, -1, -1}
			    },
			    {
    				{0, -1, -1},
    				{1, 0, -1}
			    }
			},
			{
			    {},
			    {
    				{1, 1, 0},
    				{1, 0, -1}
			    },
			    {
    				{0, 0, 0},
    				{0, -1, -1}
			    }
			},
			{
			    {},
			    {},
			    {
    				{0, 0, 0},
    				{1, 1, 0}
			    },
			    {
    				{1, 1, 0},
    				{0, 0, 0}
			    }
			},
			{
			    {
    				{1, 0, -1},
    				{1, 1, 0}
			    },
			    {},
			    {},
			    {
    				{0, -1, -1},
    				{0, 0, 0}
			    }
			}
	};
}
