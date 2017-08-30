package biz.jared;

import biz.jared.domain.Floor;

public class Calc {
    /**
     * 两个楼层的相对距离，可以为负
     *
     * @param src
     * @param target
     * @return src - target
     */
    public static int calcDistance(Floor src, Floor target) {
        return src.getFloorNo() - target.getFloorNo();
    }
}
