package cn.potatocake.lucene.spatial;

/**
 * 图形关系枚举
 * <p>
 * @author Potato-Cake（potato_cake@163.com）
 */
public enum SpatialRelation {

    WITHIN((byte) '0'),// 被包含
    INTERSECTS((byte) '1'),// 相交
    CONTAINS((byte) '2'),// 包含
    DISJOINT((byte) '3');// 脱离

    private final byte b;

    private SpatialRelation(byte _b) {
        b = _b;
    }

    public byte getByte() {
        return b;
    }
}
