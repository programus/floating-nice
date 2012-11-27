package org.programus.android.floatings;

public interface Constants {
	String GLOBAL_KEY = "Global.Key";
	String ENABLED = "/enabled";
	String X = "x";
	String Y = "y";
	String OR = "isPortrait";
	
	int[] angles = {0, 90, 180, 270};
	int[][] offsets = {
			{-1, 0},
			{0, -1},
			{1, 0},
			{0, 1}
	};
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
