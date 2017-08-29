package biz.jared;

import biz.jared.domain.Floor;

public class Calc {
    public static int calcAbsDistance(Floor src, Floor target){
        return Math.abs(src.getFloorNo() - target.getFloorNo());
    }
}
