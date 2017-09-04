package biz.jared.domain.enumeration;

public enum Direction {
    UP {
        @Override
        public Direction opposite() {
            return DOWN;
        }
    },
    DOWN {
        @Override
        public Direction opposite() {
            return UP;
        }
    },
    SAME {
        @Override
        public Direction opposite() {
            return SAME;
        }
    };

    public abstract Direction opposite();
}
