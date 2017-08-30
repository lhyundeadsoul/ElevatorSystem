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
    };

    public abstract Direction opposite();
}
